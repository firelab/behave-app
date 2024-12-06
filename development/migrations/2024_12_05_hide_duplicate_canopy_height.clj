(ns migrations.2024-12-05-hide-duplicate-canopy-height
  (:require
   [schema-migrate.interface :as sm]
   [datomic.api :as d]
   [behave-cms.store :refer [default-conn]]
   [behave-cms.server :as cms]))

;;; Overview

;; 1. Adds a new hidden Canopy Height Group under:
;; Crown > Spot (Input) > Canopy Fuel
;;
;; 2. Links new Canopy Height group under

;; 3. Adds condiditionals to only show only the Spot Canopy Height when
;;    Fire Behavior Inputs are not selected

#_{:clj-kondo/ignore [:missing-docstring]}
(do

  (cms/init-db!)
 
  (def conn (default-conn))
  (def db (d/db conn))

  (def canopy-fuel-input-canopy-height-var
    "behaveplus:crown:input:canopy_fuel:canopy_height:canopy_height")

  (def spot-input-canopy-fuel-group
    "behaveplus:crown:input:spotting:canopy_fuel")

  (def spot-input-canopy-height-var
    "behaveplus:crown:input:spotting:canopy_fuel:canopy_height:canopy_height")

  (def link-payload
    (sm/->link
     (sm/t-key->eid db canopy-fuel-input-canopy-height-var)
     (sm/t-key->eid db spot-input-canopy-height-var)))

  (def conditionals
    (-> (mapv (fn [t-key]
                {:conditional/group-variable-uuid (sm/t-key->uuid conn t-key)
                 :conditional/type                :group-variable
                 :conditional/operator            :equal
                 :conditional/values              ["false"]})
              ["behaveplus:crown:output:fire_type:fire_behavior:rate_of_spread"
               "behaveplus:crown:output:fire_type:fire_behavior:flame_length"
               "behaveplus:crown:output:fire_type:fire_behavior:active-crown-fireline-intensity"
               "behaveplus:crown:output:fire_type:active_or_independent_crown_fire:active_ratio"
               "behaveplus:crown:output:fire_type:active_or_independent_crown_fire:critical_crown_rate_of_spread"
               "behaveplus:crown:output:fire_type:transition_to_crown_fire:critical_surface_fireline_intensity"
               "behaveplus:crown:output:fire_type:transition_to_crown_fire:critical_surface_flame_length"
               "behaveplus:crown:output:fire_type:transition_to_crown_fire:critical_surface_rate_of_spread"
               "behaveplus:crown:output:fire_type:transition_to_crown_fire:transition_ratio"
               "behaveplus:crown:output:size:fire_area:fire_area"
               "behaveplus:crown:output:size:fire_perimeter:fire_perimeter"
               "behaveplus:crown:output:size:length_to_width_ratio"
               "behaveplus:crown:output:size:spread_distance:spread_distance"])
        sm/postwalk-insert))

  (def conditionals-payload
    {:db/id                       (sm/t-key->eid db spot-input-canopy-fuel-group)
     :group/conditionals-operator :and
     :group/conditionals          conditionals})

  (def payload [link-payload conditionals-payload])

  )

;;; Transact

(comment
  (def tx-data (d/transact conn payload)))

;;; Rollback

(comment
  (sm/rollback-tx! conn @tx-data))
