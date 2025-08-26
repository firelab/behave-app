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

(def area-dim-uuid (d/q '[:find ?uuid .
                       :where
                       [?e :dimension/name "Area"]
                       [?e :bp/uuid ?uuid]] (d/db conn)))

(d/q '[:find ?l ?t-key
       :where [?l :list-option/translation-key ?t-key]] (d/db conn))


(def missing-vars-payload
  [{:variable/name      "Burning Condition"
    :variable/bp6-label "Burning Condition"
    :variable/bp6-code  "vBurningCondition"
    :variable/kind      :discrete
    :variable/list
    {:list/name    "Burning Condition"
     :list/options [{:list-option/name  "Low"
                     :list-option/value 1
                     :list-option/translation-key "behaveplus:list-option:burning-condition:low"}
                    {:list-option/name  "Moderate"
                     :list-option/value 2
                     :list-option/translation-key "behaveplus:list-option:burning-condition:moderate"}
                    {:list-option/name  "Extreme"
                     :list-option/value 3
                     :list-option/translation-key "behaveplus:list-option:burning-condition:extreme"}]}}

   {:variable/name      "Safety Condition"
    :variable/bp6-label "Safety Condition"
    :variable/bp6-code  "vSafetyCondition"
    :variable/kind      :discrete
    :variable/list
    {:list/name    "Safety Condition"
     :list/translation-key ""
     :list/options [{:list-option/name  "Low"
                     :list-option/value 1
                     :list-option/translation-key "behaveplus:list-option:safety-condition:low"}
                    {:list-option/name  "Moderate"
                     :list-option/value 2
                     :list-option/translation-key "behaveplus:list-option:safety-condition:moderate"}
                    {:list-option/name  "Extreme"
                     :list-option/value 3
                     :list-option/translation-key "behaveplus:list-option:safety-condition:extreme"}]}}

   {:variable/name      "Slope Class"
    :variable/bp6-label "Slope Class"
    :variable/bp6-code  "vSlopeClass"
    :variable/kind      :discrete
    :variable/list
    {:list/name    "Slope Class"
     :list/options [{:list-option/name  "Flat"
                     :list-option/value 1
                     :list-option/translation-key "behaveplus:list-option:slope-class:low"}
                    {:list-option/name  "Moderate"
                     :list-option/value 2
                     :list-option/translation-key "behaveplus:list-option:slope-class:moderate"}
                    {:list-option/name  "Steep"
                     :list-option/value 3
                     :list-option/translation-key "behaveplus:list-option:slope-class:steep"}]}}

   {:variable/name      "Wind Speed Class"
    :variable/bp6-label "Wind Speed Class"
    :variable/bp6-code  "vWindSpeedClass"
    :variable/kind      :discrete
    :variable/list
    {:list/name    "Wind Speed Class"
     :list/options [{:list-option/name  "Light"
                     :list-option/value 1
                     :list-option/translation-key "behaveplus:list-option:wind-speed-class:light"}
                    {:list-option/name  "Moderate"
                     :list-option/value 2
                     :list-option/translation-key "behaveplus:list-option:wind-speed-class:moderate"}
                    {:list-option/name  "High"
                     :list-option/value 3
                     :list-option/translation-key "behaveplus:list-option:wind-speed-class:high"}]}}

   {:variable/name           "Vegetation Height"
    :variable/bp6-label      "Vegetation Height"
    :variable/bp6-code       "vVegetationHeight"
    :variable/kind           :continuous
    :variable/dimension-uuid length-dim-uuid}

   {:variable/name           "Safe Separation Distance"
    :variable/bp6-label      "Safe Separation Distance"
    :variable/bp6-code       "vSafeSeparationDistance"
    :variable/kind           :continuous
    :variable/dimension-uuid length-dim-uuid}

   {:variable/name           "Safety Zone Site"
    :variable/bp6-label      "Safety Zone Site"
    :variable/bp6-code       "vSafetyZoneSite"
    :variable/kind           :continuous
    :variable/dimension-uuid area-dim-uuid}])

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
        :subtool-variable/translation-key    (ssd-t-key "Safety Zone Site")
        :subtool-variable/help-key           (ssd-t-key "Safety Zone Site")}

       {:db/id                               -7
        :subtool-variable/io                 :output
        :subtool-variable/order              6
        :subtool-variable/cpp-namespace-uuid global-namespace
        :subtool-variable/cpp-class-uuid     ssd-calc-class
        :subtool-variable/cpp-function-uuid  (ssd-fn-uuid "getSafetyCondition")
        :subtool-variable/translation-key    (ssd-t-key "Condition")
        :subtool-variable/help-key           (ssd-t-key "Condition")}]}]}])

(def subtool-variables-payload
  [{:db/id                      (sm/bp6-code->variable-eid conn "")
    :variable/subtool-variables (sm/t-key->eid conn (ssd-t-key ))}
   {:db/id                      (sm/bp6-code->variable-eid conn "")
    :variable/subtool-variables (sm/t-key->eid conn )}
   {:db/id                      (sm/bp6-code->variable-eid conn "")
    :variable/subtool-variables (sm/t-key->eid conn )}
   {:db/id                      (sm/bp6-code->variable-eid conn "")
    :variable/subtool-variables (sm/t-key->eid conn )}
   {:db/id                      (sm/bp6-code->variable-eid conn "")
    :variable/subtool-variables (sm/t-key->eid conn )}
   {:db/id                      (sm/bp6-code->variable-eid conn "")
    :variable/subtool-variables (sm/t-key->eid conn )}])

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(comment
  (def tx-data-1 (d/transact conn missing-vars-payload))
  (def tx-data-2 (d/transact conn (sm/postwalk-insert rh-tool-payload)))
  (def tx-data-3 (d/transact conn subtool-variables-payload)))

(comment
  (do (sm/rollback-tx! conn @tx-data-3)
      (sm/rollback-tx! conn @tx-data-2)
      (sm/rollback-tx! conn @tx-data-1)))
