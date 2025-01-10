(ns migrations.2025-01-10-remove-obsolete-mortality-submodule-scorch-height
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Move these group variables from "Scorch Height (input)" into "Scorch (input)"
;; surface:
;;   - Wind Adjustment Factor
;;   - Wind Measured At
;;   - wind speed
;; 3. Remove these group variables from  "Scorch Height (input)" submodule.
;; 2. Delete the "Scorch Height (input)" submodule

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
(def groups-in-scorch-height-input-submodule
  (->> (sm/t-key->entity conn "behaveplus:mortality:compute-scorch-height")
       d/touch
       :submodule/groups
       (map (fn [group]
              [(:group/name group) (:db/id group)]))
       (into {})))

#_{"Wind Speed"                4611681620380886477,
   "Wind Measured at"          4611681620380886479,
   "Surface Fire Flame Length" 4611681620380881112,
   "Midflame Wind Speed"       4611681620380881113,
   "Wind Adjustment Factor"    4611681620380886481,
   "Air Temperature"           4611681620380881111}

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [; Add these gruops into "Scorch (input)" submodule
   {:db/id            (sm/t-key->eid conn "behaveplus:mortality:input:scorch")
    :submodule/groups [(groups-in-scorch-height-input-submodule "Wind Speed")
                       (groups-in-scorch-height-input-submodule "Wind Measured at")
                       (groups-in-scorch-height-input-submodule "Wind Adjustment Factor")]}

   ;; Remove these groups from the obselete "Scorch Height (input)" submodule
   ;; Necessary because when we delete the submodule it will also delete groups, which we do not want to.
   [:db/retract (sm/t-key->eid conn "behaveplus:mortality:compute-scorch-height")
    :submodule/groups (groups-in-scorch-height-input-submodule "Wind Speed")]

   [:db/retract (sm/t-key->eid conn "behaveplus:mortality:compute-scorch-height")
    :submodule/groups (groups-in-scorch-height-input-submodule "Wind Measured at")]

   [:db/retract (sm/t-key->eid conn "behaveplus:mortality:compute-scorch-height")
    :submodule/groups (groups-in-scorch-height-input-submodule "Wind Adjustment Factor")]

   ;; Delete "Scorch Height (input)" submodule
   [:db/retractEntity (sm/t-key->eid conn "behaveplus:mortality:compute-scorch-height")]])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
