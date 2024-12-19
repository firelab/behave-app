(ns migrations.2024-12-13-add-waf-sub-conditionals
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

(def db-hist (d/history (d/db conn)))

(def waf-conditional-eid 4611681620380881131)

(d/touch (d/entity (d/db conn) 4611681620380881131))

(defn t-key->conditional [t-key]
  {:conditional/group-variable-uuid (:bp/uuid
                                     (sm/t-key->entity conn t-key))
   :conditional/type                :group-variable
   :conditional/operator            :equal
   :conditional/values              "true"})

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id                        waf-conditional-eid
    :conditional/sub-conditionals (sm/postwalk-insert
                                   (map t-key->conditional
                                        ["behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading"
                                         "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:heading_backing_flanking"
                                         "behaveplus:surface:output:fire_behavior:surface_fire:direction_mode:direction_of_interest"
                                         "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire"
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
                                         "behaveplus:crown:output:size:length_to_width_ratio:length_to_width_ratio"
                                         "behaveplus:crown:output:size:spread_distance:spread_distance"]))
    :conditional/sub-conditional-operator :or}])

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
