(ns migrations.2024-11-14-add-probability-of-ignition-to-crown
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def conditionals
  (mapv (fn [t-key]
          {:conditional/group-variable-uuid (sm/t-key->uuid conn t-key)
           :conditional/type                :group-variable
           :conditional/operator            :equal
           :conditional/values              ["true"]})
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
         "behaveplus:crown:output:size:spread_distance:spread_distance"]))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id (sm/t-key->eid conn "behaveplus:crown:fire-behavior")
    :submodule/groups
    (sm/postwalk-insert
     [{:group/order                  1
       :group/name                   "Ignition"
       :group/translation-key        "behaveplus:crown:output:ignition"
       :group/result-translation-key "behaveplus:crown:result:ignition"
       :group/group-variables        [{:variable/_group-variables             (sm/name->eid conn :variable/name "Probability of Ignition")
                                       :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
                                       :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGIgnite")
                                       :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getFirebrandIgnitionProbability")
                                       :group-variable/translation-key        "behaveplus:crown:output:ignition:probability_of_ignition"
                                       :group-variable/result-translation-key "behaveplus:crown:result:ignition:probability_of_ignition"}]}])}

   ;; retract conditionals
   [:db/retractEntity 4611681620380887356] ;10-hr
   [:db/retractEntity 4611681620380887359] ;100-hr
   [:db/retractEntity 4611681620380887362] ;Live Woody

   ;; add conditionals to surface fuel moistures
   {:db/id              (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:by-size-class:10-h-fuel-moisture")
    :group/conditionals (sm/postwalk-insert conditionals)}
   {:db/id              (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:by-size-class:100-h-fuel-moisture")
    :group/conditionals (sm/postwalk-insert conditionals)}
   {:db/id              (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture:by-size-class:live-woody-fuel-moisture")
    :group/conditionals (sm/postwalk-insert conditionals)}

   ;; Add missing order attribute crown > fire behavior
   {:db/id       (sm/t-key->eid conn "behaveplus:crown:output:fire_type:fire_behavior")
    :group/order 0}

   ])

#_{:clj-kondo/ignore [:missing-docstring]}
(def translation-payload
  (sm/build-translations-payload conn {"behaveplus:crown:output:ignition"                         "Ignition"
                                       "behaveplus:crown:output:ignition:probability_of_ignition" "Probability of Ignition"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def final-payload
  (concat payload
          translation-payload))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (do (def tx-data (d/transact conn final-payload))

      (def update-surface-submodule-conditionals
        [;; enable surface > weather submodule when probability of iginition is enabled in crown.
         {:db/id                           (sm/t-key->eid conn "behaveplus:surface:input:weather")
          :submodule/conditionals-operator :or
          :submodule/conditionals          [(sm/postwalk-insert
                                             {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:crown:output:ignition:probability_of_ignition")
                                              :conditional/type                :group-variable
                                              :conditional/operator            :equal
                                              :conditional/values              ["true"]})]}

         ;; enable surface > fuel moisture submodule when probability of iginition is enabled in crown.
         {:db/id                  (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture")
          :submodule/conditionals [(sm/postwalk-insert
                                    {:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:crown:output:ignition:probability_of_ignition")
                                     :conditional/type                :group-variable
                                     :conditional/operator            :equal
                                     :conditional/values              ["true"]})]}])

      (def tx-data-2 (d/transact conn update-surface-submodule-conditionals))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-data-2)
    (sm/rollback-tx! conn @tx-data)))
