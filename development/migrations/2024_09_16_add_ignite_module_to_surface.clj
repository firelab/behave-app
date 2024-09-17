(ns migrations.2024-09-16-add-ignite-module-to-surface
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

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
                                                              {:db/id                                 -3
                                                               :variable/_group-variables             (sm/name->eid conn :variable/name "Air Temperature")
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
                                                              {:db/id                                 -4
                                                               :variable/_group-variables             (sm/name->eid conn :variable/name "Fuel Shading from the Sun")
                                                               :group-variable/order                  0
                                                               :group-variable/cpp-namespace          (:cpp-namespace cpp-uuids)
                                                               :group-variable/cpp-class              (:cpp-class cpp-uuids)
                                                               :group-variable/cpp-function           (:cpp-function cpp-uuids)
                                                               :group-variable/translation-key        "behaveplus:surface:input:weather:fuel_shading_from_the_sun:fuel_shading_from_the_sun"
                                                               :group-variable/result-translation-key "behaveplus:surface:result:weather:fuel_shading_from_the_sun:fuel_shading_from_the_sun"
                                                               :group-variable/help-key               "behaveplus:surface:input:weather:fuel_shading_from_the_sun:fuel_shading_from_the_sun:help"})]}]}

      {:submodule/name            "Ignite"
       :submodule/io              :output
       :submodule/order           5
       :submodule/translation-key "behaveplus:surface:input:ignite"
       :submodule/groups          [{:group/name            "Ignition Probability"
                                    :group/translation-key "behaveplus:surface:output:ignite:ignition_probability"
                                    :group/group-variables [(let [cpp-uuids (sm/cpp-uuids conn {:cpp-namespace "global"
                                                                                                :cpp-class     "SIGIgnite"
                                                                                                :cpp-function  "getFirebrandIgnitionProbability"})]
                                                              {:db/id                                 -5
                                                               :variable/_group-variables             (sm/name->eid conn :variable/name "Probability of Ignition")
                                                               :group-variable/order                  0
                                                               :group-variable/cpp-namespace          (:cpp-namespace cpp-uuids)
                                                               :group-variable/cpp-class              (:cpp-class cpp-uuids)
                                                               :group-variable/cpp-function           (:cpp-function cpp-uuids)
                                                               :group-variable/translation-key        "behaveplus:surface:output:ignite:ignition_probability:ignition_probability"
                                                               :group-variable/result-translation-key "behaveplus:surface:result:weather:ignition_probability:ignition_probability"
                                                               :group-variable/help-key               "behaveplus:surface:output:ignite:ignition_probability:ignition_probability:help"})]}]}])}

   (sm/->link -1 -2)])

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (do
    #_{:clj-kondo/ignore [:missing-docstring]}
    (def tx-data (d/transact conn payload))

    #_{:clj-kondo/ignore [:missing-docstring]}
    (def add-conditional-payload
      [{:db/id                  (sm/t-key->eid conn "behaveplus:surface:input:weather")
        :submodule/conditionals (sm/postwalk-insert
                                 [{:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:output:ignite:ignition_probability:ignition_probability")
                                   :conditional/type                :group-variable
                                   :conditional/operator            :equal
                                   :conditional/values              ["true"]}])}

       {:db/id                  (sm/t-key->eid conn "behaveplus:surface:input:fuel_moisture")
        :submodule/conditionals (sm/postwalk-insert
                                 [{:conditional/group-variable-uuid (sm/t-key->uuid conn "behaveplus:surface:output:ignite:ignition_probability:ignition_probability")
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
