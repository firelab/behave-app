(ns migrations.2025-01-27-add-list-option-color-tags
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def standard
  #{"1" "2" "3" "101" "102" "103" "104" "105" "106" "107" "108" "109" "121" "122" "123" "124" "4" "5" "6" "7" "141"
    "142" "143" "144" "145" "146" "147" "148" "149" "161" "162" "163" "164" "165" "8" "9" "10" "181" "182" "183"
    "184" "185" "186" "187" "188" "189" "11" "12" "13" "201" "202" "203" "204" "91" "92" "93" "98" "99"})

(def mediterranean
  #{"110" "111" "155" "156" "157" "158" "159" "166" "167" "168" "169" "170" "171" "172"
    "190" "191" "192" "193"})

(def chapparal-and-coastal-sage-shrub
  #{"150" "151" "152" "153" "154"})

(def list-options
  (:list/options (d/entity (d/db conn) (sm/name->eid conn :list/name "SurfaceFuelModels"))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id           (sm/name->eid conn :list/name "SurfaceFuelModels")
    :list/color-tags (sm/postwalk-insert
                      [{:color-tag/id              :standard
                        :color-tag/translation-key "behaveplus:list:surfacefuelmodels:color-tag:standard"}
                       {:color-tag/id              :mediterranean
                        :color-tag/translation-key "behaveplus:list:surfacefuelmodels:color-tag:mediterranean"}
                       {:color-tag/id              :chapparal-and-coastal-sage-shrub
                        :color-tag/translation-key "behaveplus:list:surfacefuelmodels:color-tag:chaparral-and-coastal-sage-shrub"}])}])

(def grass
  #{"110" "111"})

(def shrub
  #{"150" "151" "152" "153" "154" "155" "156" "157" "158" "159"})

(def timber-understory
  #{"166" "167" "168" "169" "170" "171" "172"})

(def timber-litter
  #{"190" "191" "192" "193"})



(def add-new-translations-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:list:surfacefuelmodels:color-tag:standard"                         "Standard Fuel Models (Anderson, Scott and Burgan)"
    "behaveplus:list:surfacefuelmodels:color-tag:mediterranean"                    "Mediterranean Fuel Models (Fernandes et al; Portugal)"
    "behaveplus:list:surfacefuelmodels:color-tag:chaparral-and-coastal-sage-shrub" "Chapparral & Coastal Sage Shrub (Weise, Southern CA)"}))

(def add-mising-tags-payload
  (concat (->> list-options
               (filter #(contains? grass (:list-option/value %)))
               (map (fn [{eid :db/id}]
                      {:db/id            eid
                       :list-option/tags [:Grass]})))
          (->> list-options
               (filter #(contains? shrub (:list-option/value %)))
               (map (fn [{eid :db/id}]
                      {:db/id            eid
                       :list-option/tags [:Shrub]})))
          (->> list-options
               (filter #(contains? timber-understory (:list-option/value %)))
               (map (fn [{eid :db/id}]
                      {:db/id            eid
                       :list-option/tags [:Timber-Understory]})))
          (->> list-options
               (filter #(contains? timber-litter (:list-option/value %)))
               (map (fn [{eid :db/id}]
                      {:db/id            eid
                       :list-option/tags [:Timber-Litter]})))))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (do
    (def tx-data (d/transact conn (concat payload add-new-translations-payload add-mising-tags-payload)))

    ;; (sm/t-key->eid conn "behaveplus:list:surfacefuelmodels:color-tag:standard")

    (def standard-list-options-payload
      (->> list-options
           (filter #(contains? standard (:list-option/value %)))
           (map (fn [{eid :db/id}]
                  {:db/id                 eid
                   :list-option/hide?     false
                   :list-option/color-tag (sm/t-key->eid conn "behaveplus:list:surfacefuelmodels:color-tag:standard")}))))

    (def mediterranean-list-options-payload
      (->> list-options
           (filter #(contains? mediterranean (:list-option/value %)))
           (map (fn [{eid :db/id}]
                  {:db/id                 eid
                   :list-option/hide?     false
                   :list-option/color-tag (sm/t-key->eid conn "behaveplus:list:surfacefuelmodels:color-tag:mediterranean")}))))

    (def chapparal-and-coastal-sage-shrub-payload
      (->> list-options
           (filter #(contains? chapparal-and-coastal-sage-shrub (:list-option/value %)))
           (map (fn [{eid :db/id}]
                  {:db/id                 eid
                   :list-option/hide?     false
                   :list-option/color-tag (sm/t-key->eid conn "behaveplus:list:surfacefuelmodels:color-tag:chaparral-and-coastal-sage-shrub")}))))

    (def tx-data-2 (d/transact conn (concat standard-list-options-payload
                                            mediterranean-list-options-payload
                                            chapparal-and-coastal-sage-shrub-payload))))
  )

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-data-2)
    (sm/rollback-tx! conn @tx-data)))
