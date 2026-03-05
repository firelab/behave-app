(ns migrations.2025-08-03-add-relative-humidity-tool
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Add Missing Variables
;; 2. Import CPP Functions
;; 3. Create Relative Humidity Tool


;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; 1. Add Missing Variables
;; ===========================================================================================================

(def temperature-domain (d/q '[:find ?uuid .
                               :where
                               [?e :domain/name "Temperature"]
                               [?e :bp/uuid ?uuid]] (d/db conn)))

(def missing-vars-payload
  [{:db/id                -1
    :variable/name        "Dry Bulb Temperature"
    :variable/bp6-label   "Dry Bulb Temperature"
    :variable/bp6-code    "vWthrDryBulbTemperature"
    :variable/kind        :continuous
    :variable/domain-uuid temperature-domain}
   {:db/id                -2
    :variable/name        "Wet Bulb Depression"
    :variable/bp6-label   "Wet Bulb Depression"
    :variable/bp6-code    "vWthrWetBulbDepression"
    :variable/kind        :continuous
    :variable/domain-uuid temperature-domain}])

;; ===========================================================================================================
;; 2. Import CPP Functions
;; ===========================================================================================================

(add-export-file-to-conn "./cms-exports/relativeHumidity.edn" conn)

;; ===========================================================================================================
;; 3. Create Relative Humidity Tool
;; ===========================================================================================================

(def bp-app
  (d/q '[:find ?e .
         :where [?e :application/name "BehavePlus"]]
       (d/db conn)))

(def global-namespace (sm/cpp-ns->uuid conn "global"))
(def relative-humidity-class (sm/cpp-class->uuid conn "global" "RelativeHumidityTool"))

(def rh-tool-payload
  [{:db/id -100
    :application/_tools   bp-app
    :tool/name            "Relative Humidity"
    :tool/lib-ns          "behave.lib.relative-humidity"
    :tool/translation-key "behaveplus:relative-humidity"
    :tool/help-key        "behaveplus:relative-humidity:help"
    :tool/subtools
    [{:db/id -101
      :subtool/name               "Relative Humidity"
      :subtool/cpp-namespace-uuid global-namespace
      :subtool/cpp-class-uuid     relative-humidity-class
      :subtool/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "RelativeHumidityTool" "calculate")
      :subtool/translation-key    "behaveplus:relative-humidity:relative-humidity"
      :subtool/help-key           "behaveplus:relative-humidity:relative-humidity:help"
      :subtool/variables
      [{:db/id                               -1
        :subtool-variable/io                 :input
        :subtool-variable/order              0
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     relative-humidity-class
        :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "RelativeHumidityTool" "setDryBulbTemperature")
        :subtool-variable/cpp-parameter-uuid (sm/cpp-param->uuid conn "global" "RelativeHumidityTool" "setDryBulbTemperature" "dryBulbTemperature")
        :subtool-variable/translation-key    "behaveplus:relative-humidity:relative-humidity:input:dry-bulb-temperature"
        :subtool-variable/help-key           "behaveplus:relative-humidity:relative-humidity:input:dry-bulb-temperature:help"}

       {:db/id                               -2
        :subtool-variable/io                 :input
        :subtool-variable/order              1
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     relative-humidity-class
        :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "RelativeHumidityTool" "setSiteElevation")
        :subtool-variable/cpp-parameter-uuid (sm/cpp-param->uuid conn "global" "RelativeHumidityTool" "setSiteElevation" "siteElevation")
        :subtool-variable/translation-key    "behaveplus:relative-humidity:relative-humidity:input:site-elevation"
        :subtool-variable/help-key           "behaveplus:relative-humidity:relative-humidity:input:site-elevation:help"}

       {:db/id                               -3
        :subtool-variable/io                 :input
        :subtool-variable/order              2
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     relative-humidity-class
        :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "RelativeHumidityTool" "setWetBulbTemperature")
        :subtool-variable/cpp-parameter-uuid (sm/cpp-param->uuid conn "global" "RelativeHumidityTool" "setWetBulbTemperature" "wetBulbTemperature")
        :subtool-variable/translation-key    "behaveplus:relative-humidity:relative-humidity:input:wet-bulb-temperature"
        :subtool-variable/help-key           "behaveplus:relative-humidity:relative-humidity:input:wet-bulb-temperature:help"}

       {:db/id                               -4
        :subtool-variable/io                 :output
        :subtool-variable/order              3
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     relative-humidity-class
        :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "RelativeHumidityTool" "getRelativeHumidity")
        :subtool-variable/translation-key    "behaveplus:relative-humidity:relative-humidity:output:relative-humidity"
        :subtool-variable/help-key           "behaveplus:relative-humidity:relative-humidity:output:relative-humidity:help"}

       {:db/id                               -5
        :subtool-variable/io                 :output
        :subtool-variable/order              4
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     relative-humidity-class
        :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "RelativeHumidityTool" "getDewPointTemperature")
        :subtool-variable/translation-key    "behaveplus:relative-humidity:relative-humidity:output:dew-point-temperature"
        :subtool-variable/help-key           "behaveplus:relative-humidity:relative-humidity:output:dew-point-temperature:help"}

       {:db/id                               -6
        :subtool-variable/io                 :output
        :subtool-variable/order              5
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     relative-humidity-class
        :subtool-variable/cpp-function-uuid  (sm/cpp-fn->uuid conn "global" "RelativeHumidityTool" "getWetBulbDepression")
        :subtool-variable/translation-key    "behaveplus:relative-humidity:relative-humidity:output:wet-bulb-depression"
        :subtool-variable/help-key           "behaveplus:relative-humidity:relative-humidity:output:wet-bulb-depression:help"}]}]}])

(def subtool-variables-payload
  [{:db/id                      (sm/bp6-code->variable-eid conn "vWthrDryBulbTemperature")
    :variable/subtool-variables (sm/t-key->eid conn "behaveplus:relative-humidity:relative-humidity:input:dry-bulb-temperature")}
   {:db/id                      (sm/bp6-code->variable-eid conn "vSiteElevation")
    :variable/subtool-variables (sm/t-key->eid conn "behaveplus:relative-humidity:relative-humidity:input:site-elevation")}
   {:db/id                      (sm/bp6-code->variable-eid conn "vWthrWetBulbTemp")
    :variable/subtool-variables (sm/t-key->eid conn "behaveplus:relative-humidity:relative-humidity:input:wet-bulb-temperature")}
   {:db/id                      (sm/bp6-code->variable-eid conn "vWthrRelativeHumidity")
    :variable/subtool-variables (sm/t-key->eid conn "behaveplus:relative-humidity:relative-humidity:output:relative-humidity")}
   {:db/id                      (sm/bp6-code->variable-eid conn "vWthrDewPointTemp")
    :variable/subtool-variables (sm/t-key->eid conn "behaveplus:relative-humidity:relative-humidity:output:dew-point-temperature")}
   {:db/id                      (sm/bp6-code->variable-eid conn "vWthrWetBulbDepression")
    :variable/subtool-variables (sm/t-key->eid conn "behaveplus:relative-humidity:relative-humidity:output:wet-bulb-depression")}])

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(comment
  (def tx-data-1 (d/transact conn (sm/postwalk-insert missing-vars-payload)))
  (def tx-data-2 (d/transact conn (sm/postwalk-insert rh-tool-payload)))
  (def tx-data-3 (d/transact conn subtool-variables-payload)))

(comment
  (do (sm/rollback-tx! conn @tx-data-3)
      (sm/rollback-tx! conn @tx-data-2)
      (sm/rollback-tx! conn @tx-data-1)))
