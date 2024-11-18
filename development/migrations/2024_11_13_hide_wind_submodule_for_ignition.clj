(ns migrations.2024-11-13-hide-wind-submodule-for-ignition
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Probability of Ignition in Surface does not need the Wind Submodule. This Migration script adds
;; conditionals to the Wind and Slope submodule so that it is only enabled when any of the surface
;; outputs related to fire spread is selected.

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def conditionals
  (-> (mapv (fn [t-key]
              {:conditional/group-variable-uuid (sm/t-key->uuid conn t-key)
               :conditional/type                :group-variable
               :conditional/operator            :equal
               :conditional/values              ["true"]})
            ["behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading"
             "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:direction_of_interest"
             "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking"
             "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread"
             "behaveplus:surface:output:fire_behavior:surface_fire:flame_length"
             "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity"
             "behaveplus:surface:output:spot:maximum_spotting_distance:burning_pile"
             "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire"
             "behaveplus:surface:output:size:surface___fire_size:fire_area"
             "behaveplus:surface:output:size:surface___fire_size:fire_perimeter"
             "behaveplus:surface:output:size:surface___fire_size:length-to-width-ratio"
             "behaveplus:surface:output:size:surface___fire_size:spread-distance"
             "behaveplus:crown:output:fire_type:fire_behavior:rate_of_spread"
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id                           (sm/t-key->eid conn "behaveplus:surface:input:wind_speed")
    :submodule/conditionals-operator :or
    :submodule/conditionals          conditionals}])

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
