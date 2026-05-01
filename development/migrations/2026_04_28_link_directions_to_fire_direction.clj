(ns migrations.2026-04-28-link-directions-to-fire-direction
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [clojure.string           :as str]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Create "Directions" color tag-set with Heading/Flanking/Backing tags and hex colors.
;; 2. Seed an English translation for the new tag-set key only; per-direction keys already exist.
;; 3. Attach :list/color-tag-set to the existing FireDirection list.
;; 4. Add :list-option/color-tag-ref to each FireDirection list-option (matched by :list-option/name).
;; 5. Backfill :group-variable/direction-ref from existing :group-variable/direction keywords,
;;    resolving against FireDirection options by :list-option/name.

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Step 1 — color tag-set + tags
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def directions-color-tag-payload
  (sm/postwalk-insert
   [{:db/id                                                                         -100
     :tag-set/name                                                                  "Directions"
     :tag-set/translation-key                                                       "behave:tag-set:directions"
     :tag-set/color?                                                                true
     :tag-set/tags
     [{:db/id               -101
       :tag/name            "Heading"
       :tag/color           "#13486a"
       :tag/translation-key "behave:list-option:list-option:firedirection:heading"
       :tag/order           0}
      {:db/id               -102
       :tag/name            "Flanking"
       :tag/color           "#4a7086"
       :tag/translation-key "behave:list-option:list-option:firedirection:flanking"
       :tag/order           1}
      {:db/id               -103
       :tag/name            "Backing"
       :tag/color           "#8297a3"
       :tag/translation-key "behave:list-option:list-option:firedirection:backing"
       :tag/order           2}]}]))

;; ===========================================================================================================
;; Step 2 — translation for the new tag-set (per-direction keys already exist)
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   {"behave:tag-set:directions" "Directions"}))

;; ===========================================================================================================
;; Step 3 + 4 — attach tag-set and color-tag-ref to FireDirection list and its options
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def fire-direction-eid
  (d/q '[:find ?l .
         :where [?l :list/name "FireDirection"]]
       (d/db conn)))

(def ^:private tag-name->tempid
  {"heading"  -101
   "flanking" -102
   "backing"  -103})

#_{:clj-kondo/ignore [:missing-docstring]}
(def fire-direction-options
  (d/q '[:find ?opt ?name
         :in $ ?l
         :where
         [?l :list/options ?opt]
         [?opt :list-option/name ?name]]
       (d/db conn)
       fire-direction-eid))

#_{:clj-kondo/ignore [:missing-docstring]}
(def attach-payload
  (concat
   [[:db/add fire-direction-eid :list/color-tag-set -100]]
   (for [[opt-eid opt-name] fire-direction-options
         :let               [lc-name (str/lower-case opt-name)
                             tag-id  (tag-name->tempid lc-name)]
         :when              tag-id]
     [:db/add opt-eid :list-option/color-tag-ref tag-id])))

;; ===========================================================================================================
;; Step 5 — backfill :group-variable/direction-ref from legacy :group-variable/direction keywords
;; ===========================================================================================================

(def ^:private name->option-eid
  (into {} (for [[opt-eid opt-name] fire-direction-options]
             [(str/lower-case opt-name) opt-eid])))

#_{:clj-kondo/ignore [:missing-docstring]}
(def gv-pairs
  (d/q '[:find ?gv ?dir
         :where [?gv :group-variable/direction ?dir]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def backfill-payload
  (vec (for [[gv-eid dir-kw] gv-pairs
             :let            [opt-eid (name->option-eid (name dir-kw))]
             :when           opt-eid]
         [:db/add gv-eid :group-variable/direction-ref opt-eid])))

(def payload
  (concat directions-color-tag-payload
          translations-payload
          attach-payload
          backfill-payload))

;; ===========================================================================================================
;; Transact
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx1))
