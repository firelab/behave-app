(ns migrations.2025-10-10-hide-graphs
  (:require [schema-migrate.interface :refer [bp] :as sm]
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def variables-to-hide
  ["behaveplus:crown:output:fire_type:active_or_independent_crown_fire:active_ratio"
   "behaveplus:crown:output:fire_type:active_or_independent_crown_fire:critical_crown_rate_of_spread"
   "behaveplus:crown:output:fire_type:transition_to_crown_fire:transition_ratio"
   "behaveplus:crown:output:fire_type:transition_to_crown_fire:critical_surface_flame_length"
   "behaveplus:crown:output:fire_type:transition_to_crown_fire:critical_surface_fireline_intensity"
   "behaveplus:surface:output:size:surface___fire_size:length-to-width-ratio"
   "behaveplus:surface:output:wind-and-fuel:wind:midflame-eye-level-wind-speed"
   "behaveplus:surface:output:wind-and-fuel:fuel:total-live-fuel-load"
   "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-fuel-load"
   "behaveplus:surface:output:wind-and-fuel:fuel:total-dead-herbaceous-fuel-load"
   "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_perimeter___at_resource_arrival_time"
   "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_area___at_resource_arrival_time"
   "behaveplus:contain:output:fire:containment:contained_area"
   "behaveplus:contain:output:fire:containment:fireline_constructed"
   "behaveplus:contain:output:fire:containment:time_from_report"
   "behaveplus:contain:output:fire:containment:contain_status"])


;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (->> variables-to-hide
       (map (partial sm/t-key->eid conn))
       (map (fn [id] {:db/id id :group-variable/hide-graph? true}))))

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
