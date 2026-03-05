(ns migrations.2025-08-03-add-safe-separation-distance-calculator
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]
            [string-utils.interface :refer [->kebab]]))

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

(def length-dim-uuid (d/q '[:find ?uuid .
                            :where
                            [?e :dimension/name "Length"]
                            [?e :bp/uuid ?uuid]] (d/db conn)))

(def feet-uuid (sm/name->uuid conn :unit/name "Feet (ft)"))
(def meters-uuid (sm/name->uuid conn :unit/name "Meters (m)"))

(def area-dim-uuid (d/q '[:find ?uuid .
                          :where
                          [?e :dimension/name "Area"]
                          [?e :bp/uuid ?uuid]] (d/db conn)))

(def acres-uuid (sm/name->uuid conn :unit/name "Acres (ac)"))
(def hectares-uuid (sm/name->uuid conn :unit/name "Hectares (ha)"))

(def safety-condition-color-tag-payload
  [{:db/id                   -100
    :tag-set/name            "Safety Conditions"
    :tag-set/translation-key "behaveplus:tags:safety-conditions"
    :tag-set/color?          true
    :tag-set/tags
    [{:db/id               -101
      :tag/name            "Low"
      :tag/color           "#FFFFFF"
      :tag/translation-key "behaveplus:tags:safety-conditions:low"
      :tag/order           0}
     {:db/id               -102
      :tag/name            "Moderate"
      :tag/color           "#FFFF00"
      :tag/translation-key "behaveplus:tags:safety-conditions:moderate"
      :tag/order           1}
     {:db/id               -103
      :tag/name            "Extreme"
      :tag/color           "#FF0000"
      :tag/translation-key "behaveplus:tags:safety-conditions:extreme"
      :tag/order           2}]}])

(def missing-vars-payload
  [{:variable/name      "Burning Condition"
    :variable/bp6-label "Burning Condition"
    :variable/bp6-code  "vBurningCondition"
    :variable/kind      :discrete
    :variable/list
    {:list/name    "Burning Condition"
     :list/options [{:list-option/name            "Low"
                     :list-option/value           "0"
                     :list-option/translation-key "behaveplus:list-option:burning-condition:low"}
                    {:list-option/name            "Moderate"
                     :list-option/value           "1"
                     :list-option/translation-key "behaveplus:list-option:burning-condition:moderate"}
                    {:list-option/name            "Extreme"
                     :list-option/value           "2"
                     :list-option/translation-key "behaveplus:list-option:burning-condition:extreme"}]}}

   {:variable/name      "Safety Condition"
    :variable/bp6-label "Safety Condition"
    :variable/bp6-code  "vSafetyCondition"
    :variable/kind      :discrete
    :variable/list
    {:list/name          "Safety Condition"
     :list/color-tag-set (sm/t-key->eid conn "behaveplus:tags:safety-conditions")
     :list/options       [{:list-option/name            "Low"
                           :list-option/value           "0"
                           :list-option/translation-key "behaveplus:list-option:safety-condition:low"
                           :list-option/color-tag-ref   (sm/t-key->eid conn "behaveplus:tags:safety-conditions:low")}
                          {:list-option/name            "Moderate"
                           :list-option/value           "1"
                           :list-option/translation-key "behaveplus:list-option:safety-condition:moderate"
                           :list-option/color-tag-ref   (sm/t-key->eid conn "behaveplus:tags:safety-conditions:moderate")}
                          {:list-option/name            "Extreme"
                           :list-option/value           "2"
                           :list-option/translation-key "behaveplus:list-option:safety-condition:extreme"
                           :list-option/color-tag-ref   (sm/t-key->eid conn "behaveplus:tags:safety-conditions:extreme")}]}}

   {:variable/name      "Slope Class"
    :variable/bp6-label "Slope Class"
    :variable/bp6-code  "vSlopeClass"
    :variable/kind      :discrete
    :variable/list
    {:list/name    "Slope Class"
     :list/options [{:list-option/name            "Flat"
                     :list-option/value           "0"
                     :list-option/translation-key "behaveplus:list-option:slope-class:low"}
                    {:list-option/name            "Moderate"
                     :list-option/value           "1"
                     :list-option/translation-key "behaveplus:list-option:slope-class:moderate"}
                    {:list-option/name            "Steep"
                     :list-option/value           "2"
                     :list-option/translation-key "behaveplus:list-option:slope-class:steep"}]}}

   {:variable/name      "Wind Speed Class"
    :variable/bp6-label "Wind Speed Class"
    :variable/bp6-code  "vWindSpeedClass"
    :variable/kind      :discrete
    :variable/list
    {:list/name    "Wind Speed Class"
     :list/options [{:list-option/name            "Light"
                     :list-option/value           "0"
                     :list-option/translation-key "behaveplus:list-option:wind-speed-class:light"}
                    {:list-option/name            "Moderate"
                     :list-option/value           "1"
                     :list-option/translation-key "behaveplus:list-option:wind-speed-class:moderate"}
                    {:list-option/name            "High"
                     :list-option/value           "2"
                     :list-option/translation-key "behaveplus:list-option:wind-speed-class:high"}]}}

   {:variable/name              "Vegetation Height"
    :variable/bp6-label         "Vegetation Height"
    :variable/bp6-code          "vVegetationHeight"
    :variable/kind              :continuous
    :variable/dimension-uuid    length-dim-uuid
    :variable/native-unit-uuid  feet-uuid
    :variable/native-decimals   1
    :variable/english-unit-uuid feet-uuid
    :variable/english-decimals  1
    :variable/metric-unit-uuid  meters-uuid
    :variable/metric-decimals   1}

   {:variable/name              "Safe Separation Distance"
    :variable/bp6-label         "Safe Separation Distance"
    :variable/bp6-code          "vSafeSeparationDistance"
    :variable/kind              :continuous
    :variable/dimension-uuid    length-dim-uuid
    :variable/native-unit-uuid  feet-uuid
    :variable/native-decimals   1
    :variable/english-unit-uuid feet-uuid
    :variable/english-decimals  1
    :variable/metric-unit-uuid  meters-uuid
    :variable/metric-decimals   1}

   {:variable/name              "Safety Zone Size"
    :variable/bp6-label         "Safety Zone Size"
    :variable/bp6-code          "vSafetyZoneSize"
    :variable/kind              :continuous
    :variable/dimension-uuid    area-dim-uuid
    :variable/native-unit-uuid  acres-uuid
    :variable/native-decimals   1
    :variable/english-unit-uuid acres-uuid
    :variable/english-decimals  1
    :variable/metric-unit-uuid  hectares-uuid
    :variable/metric-decimals   1}])

;; ===========================================================================================================
;; 2. Import CPP Functions
;; ===========================================================================================================

(add-export-file-to-conn "./cms-exports/safeSeparationDistanceCalculator.edn" conn)

;; ===========================================================================================================
;; 3. Create Relative Humidity Tool
;; ===========================================================================================================

(def bp-app
  (d/q '[:find ?e .
         :where [?e :application/name "BehavePlus"]]
       (d/db conn)))

(def global-namespace (sm/cpp-ns->uuid conn "global"))
(def ssd-calc-class (sm/cpp-class->uuid conn "global" "SafeSeparationDistanceCalculator"))

(defn ssd-fn-uuid [fn-name]
  (sm/cpp-fn->uuid conn "global" "SafeSeparationDistanceCalculator" fn-name))

(defn ssd-param-uuid [fn-name p-name]
  (sm/cpp-param->uuid conn "global" "SafeSeparationDistanceCalculator" fn-name p-name))

(defn ssd-t-key [v-name]
  (str "behaveplus:safe-separation-distance-calculator:" (->kebab v-name)))

(defn ssd-h-key [v-name]
  (str (ssd-t-key v-name) ":help"))

(def ssd-calc-payload
  [{:db/id -100
    :application/_tools   bp-app
    :tool/name            "Safe Separation Distance"
    :tool/lib-ns          "behave.lib.safe-separation-distance-calculator"
    :tool/translation-key "behaveplus:safe-separation-distance-calculator"
    :tool/help-key        "behaveplus:safe-separation-distance-calculator:help"
    :tool/subtools
    [{:db/id -101
      :subtool/name               "Safe Separation Distance"
      :subtool/cpp-namespace-uuid global-namespace
      :subtool/cpp-class-uuid     ssd-calc-class
      :subtool/cpp-function-uuid  (ssd-fn-uuid "calculate")
      :subtool/translation-key    "behaveplus:safe-separation-distance-calculator:safe-separation-distance-calculator"
      :subtool/help-key           "behaveplus:safe-separation-distance-calculator:safe-separation-distance-calculator:help"
      :subtool/variables
      [{:db/id                               -1
        :subtool-variable/io                 :input
        :subtool-variable/order              0
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "setBurningCondition")
        :subtool-variable/translation-key    (ssd-t-key "Burning Condition")
        :subtool-variable/help-key           (ssd-h-key "Burning Condition")}

       {:db/id                               -2
        :subtool-variable/io                 :input
        :subtool-variable/order              0
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "setSlopeClass")
        :subtool-variable/translation-key    (ssd-t-key "Slope Class")
        :subtool-variable/help-key           (ssd-h-key "Slope Class")}

       {:db/id                               -3
        :subtool-variable/io                 :input
        :subtool-variable/order              0
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "setSpeedClass")
        :subtool-variable/translation-key    (ssd-t-key "Wind Speed Class")
        :subtool-variable/help-key           (ssd-h-key "Wind Speed Class")}

       {:db/id                               -4
        :subtool-variable/io                 :input
        :subtool-variable/order              3
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "setVegetationHeight")
        :subtool-variable/cpp-parameter-uuid (ssd-param-uuid "setVegetationHeight" "height")
        :subtool-variable/translation-key    (ssd-t-key "Vegetation Height")
        :subtool-variable/help-key           (ssd-h-key "Vegetation Height")}

       {:db/id                               -5
        :subtool-variable/io                 :output
        :subtool-variable/order              4
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "getSafeSeparationDistance")
        :subtool-variable/translation-key    (ssd-t-key "Safe Separation Distance")
        :subtool-variable/help-key           (ssd-h-key "Safe Separation Distance")}

       {:db/id                               -6
        :subtool-variable/io                 :output
        :subtool-variable/order              5
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "getSafetyZoneSize")
        :subtool-variable/translation-key    (ssd-t-key "Safety Zone Size")
        :subtool-variable/help-key           (ssd-t-key "Safety Zone Size")}

       {:db/id                               -7
        :subtool-variable/io                 :output
        :subtool-variable/order              6
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "getSafetyCondition")
        :subtool-variable/translation-key    (ssd-t-key "Safety Condition")
        :subtool-variable/help-key           (ssd-t-key "Safety Condition")}]}]}])

(def subtool-variables-payload
  [{:db/id                      (sm/bp6-code->variable-eid conn "vBurningCondition")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key "Burning Condition"))}
   {:db/id                      (sm/bp6-code->variable-eid conn "vSlopeClass")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key "Slope Class"))}
   {:db/id                      (sm/bp6-code->variable-eid conn "vWindSpeedClass")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key "Wind Speed Class"))}
   {:db/id                      (sm/bp6-code->variable-eid conn "vVegetationHeight")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key "Vegetation Height"))}
   {:db/id                      (sm/bp6-code->variable-eid conn "vSafeSeparationDistance")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key "Safe Separation Distance"))}
   {:db/id                      (sm/bp6-code->variable-eid conn "vSafetyZoneSize")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key "Safety Zone Size"))}
   {:db/id                      (sm/bp6-code->variable-eid conn "vSafetyCondition")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key "Safety Condition"))}])

(def translations-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:tags:safety-conditions"                                                  "Safety Conditions"
    "behaveplus:tags:safety-conditions:low"                                              "Low"
    "behaveplus:tags:safety-conditions:moderate"                                         "Moderate"
    "behaveplus:tags:safety-conditions:extreme"                                          "Extreme"
    "behaveplus:list-option:burning-condition:low"                                       "Low"
    "behaveplus:list-option:burning-condition:moderate"                                  "Moderate"
    "behaveplus:list-option:burning-condition:extreme"                                   "Extreme"
    "behaveplus:list-option:safety-condition:low"                                        "Low"
    "behaveplus:list-option:safety-condition:moderate"                                   "Moderate"
    "behaveplus:list-option:safety-condition:extreme"                                    "Extreme"
    "behaveplus:list-option:slope-class:low"                                             "Low"
    "behaveplus:list-option:slope-class:moderate"                                        "Moderate"
    "behaveplus:list-option:slope-class:steep"                                           "Steep"
    "behaveplus:list-option:wind-speed-class:light"                                      "Light"
    "behaveplus:list-option:wind-speed-class:moderate"                                   "Moderate"
    "behaveplus:list-option:wind-speed-class:high"                                       "High"
    "behaveplus:safe-separation-distance-calculator"                                     "Safe Separation Distance Calculator"
    "behaveplus:safe-separation-distance-calculator:safe-separation-distance-calculator" "Safe Separation Distance Calculator"
    (ssd-t-key "Burning Condition")                                                      "Burning Condition"
    (ssd-t-key "Slope Class")                                                            "Slope Class"
    (ssd-t-key "Wind Speed Class")                                                       "Wind Speed Class"
    (ssd-t-key "Vegetation Height")                                                      "Vegetation Height"
    (ssd-t-key "Safe Separation Distance")                                               "Safe Separation Distance"
    (ssd-t-key "Safety Zone Size")                                                       "Safety Zone Size"
    (ssd-t-key "Safety Condition")                                                       "Safety Condition"}))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(comment
  (def tx-data-1 (d/transact conn (sm/postwalk-insert safety-condition-color-tag-payload)))
  (def tx-data-2 (d/transact conn (sm/postwalk-insert missing-vars-payload)))
  (def tx-data-3 (d/transact conn (sm/postwalk-insert ssd-calc-payload)))
  (def tx-data-4 (d/transact conn subtool-variables-payload))
  (def tx-data-5 (d/transact conn translations-payload)))

tx-data-5
(comment
  (do (sm/rollback-tx! conn @tx-data-5)
      (sm/rollback-tx! conn @tx-data-4)
      (sm/rollback-tx! conn @tx-data-3)
      (sm/rollback-tx! conn @tx-data-2)
      (sm/rollback-tx! conn @tx-data-1)))
