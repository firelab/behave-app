(ns migrations.2024-11-19-update-crown-fire-type-conditionals
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

(def t-keys-to-process
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload [{:db/id                        4611681620380882254
               :action/conditionals          (mapv (fn [t-key]
                                                     {:conditional/group-variable-uuid (sm/t-key->uuid conn t-key)
                                                      :conditional/type                :group-variable
                                                      :conditional/operator            :equal
                                                      :conditional/values              ["true"]}) t-keys-to-process)
               :action/conditionals-operator :or}])

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
