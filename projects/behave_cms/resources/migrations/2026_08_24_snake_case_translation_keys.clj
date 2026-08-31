(ns migrations.2026-08-24-snake-case-translation-keys
  (:require [clojure.string           :as str]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Normalize every translation key in the VMS db to snake_case "all the way": collapse any run of
;; hyphens to a single underscore (`-+` -> `_`). This touches two stores that must stay in lockstep:
;;   1. the `:translation/key` dictionary entities, and
;;   2. every per-entity reference attr whose name ends in "translation-key"
;;      (`:group/translation-key`, `:group/result-translation-key`, `:list-option/export-translation-key`,
;;       `:list-option/english-units-translation-key`, ... — discovered dynamically from the schema).
;;
;; HELP KEYS ARE LEFT UNTOUCHED. Help content is tied to a separate system, so any key ending in
;; ":help" (and the `*/help-key` attrs, which never end in "translation-key") is skipped entirely.
;;
;; UNIQUENESS + DUPLICATE ENTITIES: `:translation/key` and every UNIQUE `*/translation-key` attr are
;; `:db.unique/identity`. Some entities exist twice — an old kebab copy and a new snake copy (e.g. the
;; detached Torching Trees subtrees) — whose keys collapse to the same snake value, which would throw a
;; unique-conflict. This migration GUARDED-AUTO-DEDUPES: for each conflict it retracts EVERY copy that is
;; detached from the module tree (`attached?` walk below) and keeps the single attached one, if any (a
;; conflict with zero attached copies — junk on both sides — is fully retracted). Guard: a conflict with
;; >= 2 ATTACHED copies is UNRESOLVABLE (we never delete a live entity) — it changes NOTHING and throws an
;; enriched `ex-info`. That case is the Burning Pile input/output result-key clash, fixed beforehand by
;; 2026_08_23_fix_burning_pile_result_keys (io-preserving distinct result keys). `cms/init-db!` runs this
;; migration before a REPL `conn` exists, so the report is baked into the thrown ex-data (each entity
;; annotated with :attached?/:children/:referenced-by/etc.).
;;
;; Dictionary (`:translation/key`) duplicates whose translation TEXT is identical are pure redundancy and
;; are auto-merged (retract the duplicate). Duplicates whose text DIFFERS fail-fast (no text is dropped).

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

(defn- translation-keys-with-hyphens
  "Every translation key still containing a '-', EXCLUDING help keys (which stay kebab).
   Scans the :translation/key dictionary and every */translation-key reference attr.
   Returns a seq of {:attr <attr> :eid <eid> :key <string>}; empty => all snake_case."
  [db]
  (let [key-attrs (->> (d/q '[:find [?ident ...] :where [?a :db/ident ?ident]] db)
                       (filter #(str/ends-with? (name %) "translation-key"))
                       (cons :translation/key))]
    (for [attr  key-attrs
          [e v] (d/q '[:find ?e ?v :in $ ?a :where [?e ?a ?v]] db attr)
          :when (and (string? v)
                     (str/includes? v "-")
                     (not (str/ends-with? v ":help")))]
      {:attr attr :eid e :key v})))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn ->snake-key
  "Collapse hyphen runs to a single underscore. Help keys are returned unchanged."
  [k]
  (if (str/ends-with? k ":help")
    k
    (str/replace k #"-+" "_")))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn translation-key-attrs
  "All schema attrs whose name ends in \"translation-key\" (excludes `*/help-key`)."
  [db]
  (->> (d/q '[:find [?ident ...] :where [?a :db/ident ?ident]] db)
       (filter #(str/ends-with? (name %) "translation-key"))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- name-attr
  "The `<entity>/name` attr for a `<entity>/…-translation-key` attr, for report context."
  [attr]
  (keyword (namespace attr) "name"))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- unique-attr?
  "True when `attr` is `:db.unique/identity` in the schema. Only unique attrs can conflict —
   renaming two entities to the same value on a NON-unique attr (e.g. `:submodule/translation-key`,
   `:subtool/translation-key`, `:list/translation-key`) is allowed."
  [db attr]
  (= :db.unique/identity (:db/unique (d/entity db attr))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- attached?
  "True when `eid` is reachable from a submodule by walking parent component-refs UP. A naive
   direct-incoming-ref check is wrong (a child of a detached group still points at its detached
   parent), so we follow the single-parent `:db/isComponent` chain to the top:
   group-variable → group (`:group/_group-variables`), group → parent group (`:group/_children`)
   or submodule (`:submodule/_groups`)."
  [db eid]
  (loop [e    (d/entity db eid)
         seen #{}]
    (cond
      (nil? e)                        false
      (seq (:submodule/_groups e))    true
      (contains? seen (:db/id e))     false
      :else                           (recur (or (first (:group/_children e))
                                                 (first (:group/_group-variables e)))
                                             (conj seen (:db/id e))))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- entity-report
  "Annotate one conflicting entity with the context needed to judge it by hand."
  [db attr eid current-key]
  (let [e       (d/entity db eid)
        ns-attr #(keyword (namespace attr) %)]
    {:eid             eid
     :current-key     current-key
     :name            (get e (name-attr attr))
     :translation-key (get e (ns-attr "translation-key")) ; the entity's own key → reveals input/output role
     :attached?       (attached? db eid)
     :children        (count (:group/children e))
     :group-variables (count (:group/group-variables e))
     ;; attrs on OTHER entities that point at this eid (conditionals, links, actions, parents …) —
     ;; tells us whether it is safe to delete and what role it plays.
     :referenced-by   (frequencies (map #(d/ident db (:a %)) (d/datoms db :vaet eid)))}))

;; ===========================================================================================================
;; Conflict detection
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn reference-conflicts
  "For every UNIQUE `*/translation-key` attr, snake values that >1 entity would claim.
   Each entity is annotated via `entity-report`."
  [db]
  (for [attr        (filter #(unique-attr? db %) (translation-key-attrs db))
        :let        [rows   (d/q '[:find ?e ?v :in $ ?a :where [?e ?a ?v]] db attr)
                     groups (group-by (fn [[_ v]] (->snake-key v)) rows)]
        [new-k grp] groups
        :when       (> (count grp) 1)]
    {:attr     attr
     :new-key  new-k
     :entities (mapv (fn [[e v]] (entity-report db attr e v)) grp)}))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn dictionary-conflicts
  "Snake keys produced from >1 distinct `:translation/key` whose translations DIFFER.
   Returns a seq of {:new-key :entities [{:eid :current-key :text}]}."
  [db]
  (->> (d/q '[:find ?e ?k ?t
              :where
              [?e :translation/key ?k]
              [?e :translation/translation ?t]] db)
       (remove (fn [[_ k]] (str/ends-with? k ":help")))
       (group-by (fn [[_ k]] (->snake-key k)))
       (keep (fn [[new-k grp]]
               (when (and (> (count (distinct (map second grp))) 1)
                          (> (count (distinct (map #(nth % 2) grp))) 1))
                 {:new-key  new-k
                  :entities (for [[e k t] grp] {:eid e :current-key k :text t})})))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- unresolvable?
  "A reference conflict can be auto-resolved by dedup when AT MOST ONE of its entities is attached
   to the module tree: retract the detached ones (all of them if none is attached — pure junk on
   both sides), keep the single attached survivor. It is UNRESOLVABLE only when >= 2 entities are
   attached (we never delete a live entity), which must be fixed by hand first (e.g. the Burning
   Pile input/output result-key clash, handled by 2026_08_23_fix_burning_pile_result_keys)."
  [conflict]
  (>= (count (filter :attached? (:entities conflict))) 2))

;; ===========================================================================================================
;; Rename transactions
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn dictionary-tx
  "Rename `:translation/key` dictionary entries to snake_case. Identical-text duplicates that
   collapse to the same key are merged (retract the redundant entity). Assumes differing-text
   collisions have already been ruled out via `dictionary-conflicts` (fail-fast)."
  [db]
  (let [entries (->> (d/q '[:find ?e ?k :where [?e :translation/key ?k]] db)
                     (remove (fn [[_ k]] (str/ends-with? k ":help")))
                     (map (fn [[e k]] {:e e :k k :new (->snake-key k)})))]
    (->> (group-by :new entries)
         (mapcat
          (fn [[new-k grp]]
            (if (= 1 (count grp))
              (let [{:keys [e k]} (first grp)]
                (when (not= k new-k) [{:db/id e :translation/key new-k}]))
              ;; identical-text duplicate — keep the entity already at new-k, else lowest :db/id
              (let [survivor (or (first (filter #(= (:k %) new-k) grp))
                                 (apply min-key :e grp))
                    dups     (remove #(= (:e %) (:e survivor)) grp)]
                (concat
                 (map (fn [d] [:db.fn/retractEntity (:e d)]) dups)
                 (when (not= (:k survivor) new-k)
                   [{:db/id (:e survivor) :translation/key new-k}]))))))
         (vec))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn references-tx
  "Rename all `*/translation-key` reference-attr values to snake_case, skipping any entity in
   `exclude` (the orphans being retracted)."
  [db exclude]
  (vec
   (for [attr  (translation-key-attrs db)
         [e v] (d/q '[:find ?e ?v :in $ ?a :where [?e ?a ?v]] db attr)
         :let  [nv (->snake-key v)]
         :when (and (not= v nv) (not (contains? exclude e)))]
     {:db/id e attr nv})))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration. The runner calls (payload-fn db) at startup and transacts the result plus a
;; :bp/migration-id marker. Guarded auto-dedup + atomic:
;;   - If any reference conflict is ambiguous (not exactly {one attached, rest detached}) or any
;;     dictionary text conflict exists, throw an enriched report and change nothing.
;;   - Otherwise retract the orphan (detached) duplicate in each conflict pair and apply all renames
;;     (skipping the retracted orphans) in one transaction.
#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [rconf      (reference-conflicts db)
        dconf      (seq (dictionary-conflicts db))
        unresolved (seq (filter unresolvable? rconf))]
    (when (or unresolved dconf)
      (throw (ex-info (str "snake_case translation-key migration: "
                           "resolve these unique-key conflicts by hand before it can apply")
                      {:unresolvable-conflicts (vec unresolved)
                       :dictionary-conflicts   (vec dconf)})))
    (let [orphans (into #{} (comp (mapcat :entities)
                                  (remove :attached?)
                                  (map :eid))
                        rconf)]
      (vec (concat
            (map (fn [eid] [:db.fn/retractEntity eid]) orphans)
            (dictionary-tx db)
            (references-tx db orphans))))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms])
  (cms/init-db!)
  (def conn (behave-cms.store/default-conn))

  ;; Inspect translation-keys with hyphens
  (count (translation-keys-with-hyphens (d/db conn)))  ; just the count — 0 means done

  ;; Inspect what the migration sees. If `payload-fn` throws, its ex-data lists the ambiguous
  ;; conflicts (each entity annotated with :attached?/:children/:group-variables) + dictionary conflicts.
  ;; Both should result in empty lists after the migration.
  (clojure.pprint/pprint (reference-conflicts (d/db conn)))
  (clojure.pprint/pprint (dictionary-conflicts (d/db conn)))

  (translation-keys-with-hyphens (d/db conn))          ; full list
  (count (translation-keys-with-hyphens (d/db conn)))  ; just the count — 0 means done

  ;; Inspect the tx-data (orphan retractions + renames) and apply.
  (payload-fn (d/db conn))
  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
