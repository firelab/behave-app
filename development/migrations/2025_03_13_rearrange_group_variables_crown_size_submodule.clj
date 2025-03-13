(ns migrations.2025-03-13-rearrange-group-variables-crown-size-submodule
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Rearrange group variables in crown size submodule like surface size submodule

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
(def payload
  [(sm/postwalk-insert
    {:db/id                        -1
     :submodule/_groups            (sm/t-key->eid conn "behaveplus:crown:output:size")
     :group/name                   "Crown - Fire Size"
     :group/translation-key        "behaveplus:crown:output:size:crown-fire-size"
     :group/result-translation-key "behaveplus:crown:result:size:crown-fire-size"})

   ;; Move existing gruop variables to the new submodule "Crown Size" from above.
   {:db/id                  (sm/t-key->eid conn "behaveplus:crown:output:size:fire_area:fire_area")
    :group/_group-variables -1
    :group-variable/order   0}

   {:db/id                  (sm/t-key->eid conn "behaveplus:crown:output:size:fire_perimeter:fire_perimeter")
    :group/_group-variables -1
    :group-variable/order   1}

   {:db/id                  (sm/t-key->eid conn "behaveplus:crown:output:size:length_to_width_ratio:length_to_width_ratio")
    :group/_group-variables -1
    :group-variable/order   2}

   {:db/id                  (sm/t-key->eid conn "behaveplus:crown:output:size:spread_distance:spread_distance")
    :group/_group-variables -1
    :group-variable/order   3}

   ;; remove reference to group-variables from current group so that when we delete the old groups, we do not also delete the group-variable. This is because group variables are set as compoenents to groups.
   [:db/retract
    (sm/t-key->eid conn "behaveplus:crown:output:size:fire_area")
    :group/group-variables
    (sm/t-key->eid conn "behaveplus:crown:output:size:fire_area:fire_area")]

   [:db/retract
    (sm/t-key->eid conn "behaveplus:crown:output:size:fire_perimeter")
    :group/group-variables
    (sm/t-key->eid conn "behaveplus:crown:output:size:fire_perimeter:fire_perimeter")]

   [:db/retract
    (sm/t-key->eid conn "behaveplus:crown:output:size:length_to_width_ratio")
    :group/group-variables
    (sm/t-key->eid conn "behaveplus:crown:output:size:length_to_width_ratio:length_to_width_ratio")]

   [:db/retract
    (sm/t-key->eid conn "behaveplus:crown:output:size:spread_distance")
    :group/group-variables
    (sm/t-key->eid conn "behaveplus:crown:output:size:spread_distance:spread_distance")]

   ])

(def translation-payload
  (sm/build-translations-payload conn 100 {"behaveplus:crown:output:size:crown-fire-size" "Size"}))

(def remove-groups-payload
  [[:db/retractEntity
    (sm/t-key->eid conn "behaveplus:crown:output:size:fire_area")]

   [:db/retractEntity
    (sm/t-key->eid conn "behaveplus:crown:output:size:fire_perimeter")]

   [:db/retractEntity
    (sm/t-key->eid conn "behaveplus:crown:output:size:length_to_width_ratio")]

   [:db/retractEntity
    (sm/t-key->eid conn "behaveplus:crown:output:size:spread_distance")]])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (do (def tx-data (d/transact conn (concat payload translation-payload)))
      (def tx-data-2 (d/transact conn remove-groups-payload)))
  )

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-data-2)
    (sm/rollback-tx! conn @tx-data)))
