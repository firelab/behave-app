(ns migrations.2026-04-09-add-fire-type-color-tag-set
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Create "Fire Type" color tag set with 3 tags: Surface, Torching, Crowning
;; 2. Link the CrownFireType list to the new tag set via :list/color-tag-set
;; 3. Assign :list-option/color-tag-ref to each of the 4 CrownFireType list options:
;;    - "Surface"   → Surface tag
;;    - "Torching"  → Torching tag
;;    - "CondCrown" → Surface tag
;;    - "Crowning"  → Crowning tag

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Creates the Fire Type tag set and links it to the CrownFireType list.
;; The temp id -1 is used to reference the tag set from the list entity within the same transaction.

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (sm/postwalk-insert
   [{:db/id                                                      -1
     :tag-set/name                                               "Fire Type"
     :tag-set/translation-key                                    "behaveplus:tags:fire-type"
     :tag-set/color?                                             true
     :tag-set/tags
     [{:db/id               -2
       :tag/name            "Surface"
       :tag/color           "#95AC3380"
       :tag/translation-key "behaveplus:tags:fire-type:surface"
       :tag/order           0}
      {:db/id               -3
       :tag/name            "Torching"
       :tag/color           "#FFDA0D80"
       :tag/translation-key "behaveplus:tags:fire-type:torching"
       :tag/order           1}
      {:db/id               -4
       :tag/name            "Crowning"
       :tag/color           "#95AC33B2"
       :tag/translation-key "behaveplus:tags:fire-type:crowning"
       :tag/order           2}]}
    {:db/id              (sm/name->eid conn :list/name "CrownFireType")
     :list/color-tag-set -1}]))

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:tags:fire-type"          "Fire Type"
    "behaveplus:tags:fire-type:surface"  "Surface"
    "behaveplus:tags:fire-type:torching" "Torching"
    "behaveplus:tags:fire-type:crowning" "Crowning"
    "behaveplus:color_by"                "Color by"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  ;; Phase 1: create the tag set, link it to the list, and add translations
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data @(d/transact conn (concat payload translations-payload)))

  ;; Phase 2: assign color-tag-ref to each CrownFireType list option.
  ;; Must run AFTER Phase 1 so the tag eids exist for sm/t-key->eid lookups.
  (let [list-options    (:list/options (d/entity (d/db conn) (sm/name->eid conn :list/name "CrownFireType")))
        name->t-key     {"Surface"   "behaveplus:tags:fire-type:surface"
                         "Torching"  "behaveplus:tags:fire-type:torching"
                         "CondCrown" "behaveplus:tags:fire-type:surface"
                         "Crowning"  "behaveplus:tags:fire-type:crowning"}
        options-payload (->> list-options
                             (keep (fn [{eid :db/id opt-name :list-option/name}]
                                     (when-let [t-key (get name->t-key opt-name)]
                                       {:db/id                     eid
                                        :list-option/color-tag-ref (sm/t-key->eid conn t-key)})))
                             vec)]
    #_{:clj-kondo/ignore [:missing-docstring]}
    (def tx-data-2 @(d/transact conn options-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data-2)
  (sm/rollback-tx! conn tx-data))
