(ns migrations.2026-05-05-backfill-missing-order
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Normalize /order values across all sibling sets: for every parent that
;; has a non-dense ordering of its children (missing values, duplicates, or
;; gaps), renumber the children 0..N-1 while preserving their current
;; relative order. Ties (duplicate orders) are broken by :db/id, and
;; children currently missing /order are appended after those that have
;; one (also :db/id-sorted).
;;
;; Only children whose order actually changes are emitted in the payload.
;;
;; Some list-options are shared across multiple parent lists. Shared options
;; that would receive the same order value in every parent are deduped safely.
;; Shared options with conflicting computed orders (different values under
;; different parents) are excluded from the payload entirely and reported via
;; the inspection comment block below. Known conflict at time of authoring:
;;   eid 4611681620380878252 (fuelmodeltype:s) — shared by FuelModelType and
;;   CompassDirection, which is itself a likely data bug to investigate
;;   separately.
;;
;; Counts at time of authoring (~503 total emitted ops):
;;   :list-option/order        — ~346 (after dedupe/skip)
;;   :group-variable/order     —   69
;;   :group/order              —   41
;;   :submodule/order          —   23
;;   :subtool-variable/order   —   17
;;   :tool/order               —    3
;;   :module/order             —    2
;;   :subtool/order            —    2
;; Of these, 282 are entities that previously had no /order at all.

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def order-pairs
  [[:module/order              :application/modules]
   [:submodule/order           :module/submodules]
   [:group/order               :submodule/groups]
   [:group-variable/order      :group/group-variables]
   [:tool/order                :application/tools]
   [:subtool/order             :tool/subtools]
   [:subtool-variable/order    :subtool/variables]
   [:list-option/order         :list/options]
   [:note-category/order       :application/note-categories]
   [:search-table/order        :module/search-tables]
   [:search-table-column/order :search-table/columns]
   [:tag/order                 :tag-set/tags]
   [:pivot-column/order        :pivot-table/columns]])

#_{:clj-kondo/ignore [:missing-docstring]}
(defn build-renumber-payload
  [db parent-ref order-attr]
  (let [parents (d/q [:find '[?p ...] :where ['?p parent-ref]] db)]
    (mapcat
     (fn [pid]
       (let [children    (->> (parent-ref (d/entity db pid)) (map :db/id))
             order-of    (fn [eid] (order-attr (d/entity db eid)))
             orders      (map order-of children)
             needs-fix?  (or (some nil? orders)
                             (not= (sort orders) (range (count children))))
             sorted-eids (sort-by (juxt #(or (order-of %) Long/MAX_VALUE) identity)
                                  children)]
         (when needs-fix?
           (keep-indexed
            (fn [i eid]
              (when (not= i (order-of eid))
                {:db/id eid order-attr i}))
            sorted-eids))))
     parents)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def raw-payload
  (let [db (d/db conn)]
    (vec (mapcat (fn [[oa pr]] (build-renumber-payload db pr oa)) order-pairs))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def conflicting-eids
  (->> raw-payload
       (group-by :db/id)
       (filter (fn [[_ entries]]
                 (> (count (distinct (map #(dissoc % :db/id) entries))) 1)))
       (map first)
       set))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (->> raw-payload
       (remove #(contains? conflicting-eids (:db/id %)))
       distinct
       vec))

;; Inspect conflicting eids before transacting (should be #{4611681620380878252})
(comment
  (let [db (d/db conn)]
    (for [eid conflicting-eids]
      {:eid     eid
       :parents (d/q '[:find ?p ?n
                       :in $ ?c
                       :where [?p :list/options ?c]
                       [(get-else $ ?p :list/name "<no-name>") ?n]]
                     db eid)})))

;; ===========================================================================================================
;; Data fix — CompassDirection erroneously references the FuelModelType "S" option
;; ===========================================================================================================

;; The list-option eid 4611681620380878252 ("S", value "8", translation-key
;; "behave:list-option:list-option:fuelmodeltype:s") belongs to FuelModelType
;; but is also referenced by CompassDirection. Create a new dedicated
;; CompassDirection:S option with the correct translation-key prefix and
;; remove the erroneous reference. The new option is given order 8 — its
;; 0-indexed alphabetical slot between SSE (7 post-renumber) and SSW (9). The
;; renumber pass above resolves any transient duplicates (CompassDirection's
;; existing orders are 1..16) by sorting on (order, :db/id) and producing a
;; final 0..N-1 sequence.
;;
;; Transact this payload BEFORE the renumber payload above so the renumber
;; pass can safely include the (now non-shared) FuelModelType:S option.

#_{:clj-kondo/ignore [:missing-docstring]}
(def compass-direction-eid 4611681620380878243)

#_{:clj-kondo/ignore [:missing-docstring]}
(def fuelmodeltype-s-eid 4611681620380878252)

#_{:clj-kondo/ignore [:missing-docstring]}
(def data-fix-payload
  [{:db/id                       "compass-direction-s"
    :bp/uuid                     (str (java.util.UUID/randomUUID))
    :list-option/name            "S"
    :list-option/value           "8"
    :list-option/order           8
    :list-option/translation-key "behave:list-option:list-option:compassdirection:s"}
   [:db/add     compass-direction-eid :list/options "compass-direction-s"]
   [:db/retract compass-direction-eid :list/options fuelmodeltype-s-eid]])

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-new-translations-payload
  (sm/build-translations-payload conn 100
                                 {"behave:list-option:list-option:compassdirection:s" "S"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

;; Step 1 — fix the erroneous CompassDirection -> fuelmodeltype:s reference.
(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def data-fix-tx-data @(d/transact conn (concat data-fix-payload add-new-translations-payload)))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; Step 2 — re-evaluate `raw-payload`/`conflicting-eids`/`payload` (above) so
;; the renumber pass picks up the cleaned-up DB state, then transact.
(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data)
  (sm/rollback-tx! conn data-fix-tx-data))
