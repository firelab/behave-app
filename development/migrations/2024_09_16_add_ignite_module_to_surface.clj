(ns migrations.2024-09-16-add-ignite-module-to-surface
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
;; payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  [{:db/id                 (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire")
    :group/order           18
    :group/group-variables (sm/postwalk-insert
                            [(let [cpp-uuids (sm/cpp-uuids conn {:cpp-namespace "global"
                                                                 :cpp-class     "SIGSurface"
                                                                 :cpp-function  "getMoistureOneHour"})]
                               {:db/id                                 -1
                                :variable/_group-variables             (sm/name->eid conn :variable/name "1-h Fuel Moisture")
                                :group-variable/order                  0
                                :group-variable/cpp-namespace          (:cpp-namespace cpp-uuids)
                                :group-variable/cpp-class              (:cpp-class cpp-uuids)
                                :group-variable/cpp-function           (:cpp-function cpp-uuids)
                                :group-variable/translation-key        "behaveplus:surface:output:fire_behavior:surface_fire:1_h_fuel_moisture"
                                :group-variable/result-translation-key "behaveplus:surface:result:fire_behavior:surface_fire:1_h_fuel_moisture"
                                :group-variable/help-key               "behaveplus:surface:output:fire_behavior:surface_fire:1_h_fuel_moisture:help"
                                :group-variable/conditionally-set?     true
                                :group-variable/hide-result?           true})])}

   {:db/id (sm/t-key->eid conn "behaveplus:surface")
    :module/submodules
    (sm/postwalk-insert
     [{:submodule/name            "Weather"
       :submodule/order           6
       :submodule/io              :input
       :submodule/translation-key "behaveplus:surface:input:weather"
       :submodule/groups          [{:group/name            "1-h Fuel Moisture"
                                    :group/order           0
                                    :group/translation-key "behaveplus:surface:input:weather:1_h_fuel_moisture"
                                    :group/group-variables [(let [cpp-uuids (sm/cpp-uuids conn {:cpp-namespace "global"
                                                                                                :cpp-class     "SIGIgnite"
                                                                                                :cpp-function  "setMoistureOneHour"})]
                                                              {:db/id                                 -2
                                                               :variable/_group-variables             (sm/name->eid conn :variable/name "1-h Fuel Moisture")
                                                               :group-variable/order                  0
                                                               :group-variable/cpp-namespace          (:cpp-namespace cpp-uuids)
                                                               :group-variable/cpp-class              (:cpp-class cpp-uuids)
                                                               :group-variable/cpp-function           (:cpp-function cpp-uuids)
                                                               :group-variable/translation-key        "behaveplus:surface:input:weather:1_h_fuel_moisture:1_h_fuel_moisture"
                                                               :group-variable/result-translation-key "behaveplus:surface:result:weather:1_h_fuel_moisture:1_h_fuel_moisture"
                                                               :group-variable/help-key               "behaveplus:surface:input:weather:1_h_fuel_moisture:1_h_fuel_moisture:help"})]
                                    :group/conditionals    [{:conditional/type     :module
                                                             :conditional/operator :equal
                                                             :conditional/values   #{"surface" "mortality" "contain" "crown"}}]} ;; always hide

                                   {:group/name            "Air Temperature"
                                    :group/order           0
                                    :group/translation-key "behaveplus:surface:input:weather:air_temperature"
                                    :group/group-variables [(let [cpp-uuids (sm/cpp-uuids conn {:cpp-namespace "global"
                                                                                                :cpp-class     "SIGIgnite"
                                                                                                :cpp-function  "setAirTemperature"})]
                                                              {:variable/_group-variables             (sm/name->eid conn :variable/name "Air Temperature")
                                                               :group-variable/order                  0
                                                               :group-variable/cpp-namespace          (:cpp-namespace cpp-uuids)
                                                               :group-variable/cpp-class              (:cpp-class cpp-uuids)
                                                               :group-variable/cpp-function           (:cpp-function cpp-uuids)
                                                               :group-variable/translation-key        "behaveplus:surface:input:weather:air_temperature:air_temperature"
                                                               :group-variable/result-translation-key "behaveplus:surface:result:weather:air_temperature:air_temperature"
                                                               :group-variable/help-key               "behaveplus:surface:input:weather:air_temperature:air_temperature:help"})]}

                                   {:group/name "Fuel Shading from the Sun"
                                    :group/order           0
                                    :group/translation-key "behaveplus:surface:input:weather:fuel_shading_from_the_sun"
                                    :group/group-variables [(let [cpp-uuids (sm/cpp-uuids conn {:cpp-namespace "global"
                                                                                                :cpp-class     "SIGIgnite"
                                                                                                :cpp-function  "setSunShade"}) ]
                                                              {:variable/_group-variables             (sm/name->eid conn :variable/name "Fuel Shading from the Sun")
                                                               :group-variable/order                  0
                                                               :group-variable/cpp-namespace          (:cpp-namespace cpp-uuids)
                                                               :group-variable/cpp-class              (:cpp-class cpp-uuids)
                                                               :group-variable/cpp-function           (:cpp-function cpp-uuids)
                                                               :group-variable/translation-key        "behaveplus:surface:input:weather:fuel_shading_from_the_sun:fuel_shading_from_the_sun"
                                                               :group-variable/result-translation-key "behaveplus:surface:result:weather:fuel_shading_from_the_sun:fuel_shading_from_the_sun"
                                                               :group-variable/help-key               "behaveplus:surface:input:weather:fuel_shading_from_the_sun:fuel_shading_from_the_sun:help"})]}]}])}

   {:db/id            (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior")
    :submodule/groups (sm/postwalk-insert
                       [{:group/name            "Ignition"
                         :group/order           3
                         :group/translation-key "behaveplus:surface:output:fire_behavior:ignition"
                         :group/group-variables [(let [cpp-uuids (sm/cpp-uuids conn {:cpp-namespace "global"
                                                                                     :cpp-class     "SIGIgnite"
                                                                                     :cpp-function  "getFirebrandIgnitionProbability"})]
                                                   {:group-variable/order                  0
                                                    :variable/_group-variables             (sm/name->eid conn :variable/name "Probability of Ignition")
                                                    :group-variable/cpp-namespace          (:cpp-namespace cpp-uuids)
                                                    :group-variable/cpp-class              (:cpp-class cpp-uuids)
                                                    :group-variable/cpp-function           (:cpp-function cpp-uuids)
                                                    :group-variable/translation-key        "behaveplus:surface:output:fire_behavior:probability_of_ignition:probability_of_ignition"
                                                    :group-variable/result-translation-key "behaveplus:surface:result:fire_behavior:probability_of_ignition:probability_of_ignition"
                                                    :group-variable/help-key               "behaveplus:surface:output:fire_behavior:probability_of_ignition:probability_of_ignition:help"})]}])}

   (sm/->link -1 -2)])

#_{:clj-kondo/ignore [:missing-docstring]}
(def translation-payload
  (sm/build-translations-payload
   conn
   100
   {"behaveplus:surface:output:fire_behavior:surface_fire:1_h_fuel_moisture"                  "1-h Fuel Moisture"
    "behaveplus:surface:input:weather:1_h_fuel_moisture:1_h_fuel_moisture"                    "1-h Fuel Moisture"
    "behaveplus:surface:input:weather"                                                        "Weather"
    "behaveplus:surface:input:weather:air_temperature"                                        "Air Temperature"
    "behaveplus:surface:input:weather:air_temperature:air_temperature"                        "Air Temperature"
    "behaveplus:surface:input:weather:fuel_shading_from_the_sun"                              "Fuel Shading From the Sun"
    "behaveplus:surface:input:weather:fuel_shading_from_the_sun:fuel_shading_from_the_sun"    "Fuel Shading From the Sun"
    "behaveplus:surface:output:fire_behavior:ignition"                                        "Ignition"
    "behaveplus:surface:output:fire_behavior:probability_of_ignition:probability_of_ignition" "Probability of Ignition"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (do
    #_{:clj-kondo/ignore [:missing-docstring]}
    (def tx-data (d/transact conn (concat payload translation-payload)))

    #_{:clj-kondo/ignore [:missing-docstring]}
    (def add-conditional-payload
      [{:db/id                  (sm/t-key->eid conn "behaveplus:surface:input:weather")
        :submodule/conditionals (sm/postwalk-insert
                                 [{:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:probability_of_ignition:probability_of_ignition")
                                   :conditional/type                :group-variable
                                   :conditional/operator            :equal
                                   :conditional/values              ["true"]}])}

       {:db/id                  (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture")
        :submodule/conditionals (sm/postwalk-insert
                                 [{:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:output:fire_behavior:probability_of_ignition:probability_of_ignition")
                                   :conditional/type                :group-variable
                                   :conditional/operator            :equal
                                   :conditional/values              ["true"]}])}])

    #_{:clj-kondo/ignore [:missing-docstring]}
    (def tx-data-2 (d/transact conn add-conditional-payload)))
  )

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-data-2)
    (sm/rollback-tx! conn @tx-data)))
