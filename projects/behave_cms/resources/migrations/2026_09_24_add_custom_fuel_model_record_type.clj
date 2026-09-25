(ns migrations.2026-09-24-add-custom-fuel-model-record-type
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; Seeds the "Custom Fuel Model" record type for BehavePlus.
;;
;; A record type describes a bundle of values the user fills in once and reuses: its fields are bound to the
;; parameters of a single C++ function, which is applied to the engine before a run. This one binds
;; SIGFuelModels::defineCustomFuelModel, whose 28 parameters interleave each value with its units, so the
;; 16 fields below cover only the value positions — the units positions are filled from each field's own
;; unit selection at solve time.
;;
;; Requires the SIGFuelModels class to be in the cpp registry (2026_09_23_import_sig_fuel_models).
;; ===========================================================================================================

(def ^:private translation-key "behaveplus:record_types:custom_fuel_model")

(def ^:private cpp-namespace "global")
(def ^:private cpp-class "SIGFuelModels")
(def ^:private cpp-function "defineCustomFuelModel")

;; Group variables whose fuel model list gains an option per custom fuel model.
(def ^:private target-group-variable-keys
  ["behaveplus:surface:input:fuel_models:standard:fuel_model:fuel_model"
   "behaveplus:surface:input:fuel_models:standard:wind_driven_fuel_model:wind_driven_fuel_model"])

;; ===========================================================================================================
;; Variables
;; ===========================================================================================================

;; Quantities BehavePlus already models. A record field points at the same variable the Surface inputs use,
;; so a fuel load means the same thing — same label, units and bounds — wherever it appears.
(def ^:private reused-variables
  {"fuelLoadOneHour"          "1-h Fuel Load"
   "fuelLoadTenHour"          "10-h Fuel Load"
   "fuelLoadHundredHour"      "100-h Fuel Load"
   "fuelLoadLiveHerbaceous"   "Live Herbaceous Fuel Load"
   "fuelLoadLiveWoody"        "Live Woody Fuel Load"
   "moistureOfExtinctionDead" "Dead Fuel Moisture of Extinction"})

;; Quantities with no existing variable. `Fuel Model Code` is a discrete list of the built-in models and
;; `Fuel Model Number` is the unbounded Surface input, so neither can serve a custom model — hence the
;; dedicated code/name/number variables here.
;;
;; {parameter-name {:nname .. :kind .. :domain .. :minimum .. :maximum ..}}
(def ^:private new-variables
  {"fuelModelNumber"      {:nname   "Custom Fuel Model Number"
                           :kind    :continuous
                           ;; Dimensionless; the engine reserves everything below 220.
                           :minimum 220.0
                           :maximum 256.0}
   "code"                 {:nname "Custom Fuel Model Code" :kind :text}
   "name"                 {:nname "Custom Fuel Model Name" :kind :text}
   "fuelBedDepth"         {:nname   "Custom Fuel Model Fuel Bed Depth"
                           :kind    :continuous
                           :domain  "Surface Fuel Depth"
                           :minimum 0.1
                           :maximum 10.0}
   "heatOfCombustionDead" {:nname   "Dead Fuel Heat of Combustion"
                           :kind    :continuous
                           :domain  "Fuel Heat Content"
                           :minimum 6000.0
                           :maximum 12000.0}
   "heatOfCombustionLive" {:nname   "Live Fuel Heat of Combustion"
                           :kind    :continuous
                           :domain  "Fuel Heat Content"
                           :minimum 6000.0
                           :maximum 12000.0}
   "savrOneHour"          {:nname   "1-h Surface Area to Volume Ratio"
                           :kind    :continuous
                           :domain  "Surface Area/Vol Ratio"
                           :minimum 1.0
                           :maximum 4000.0}
   "savrLiveHerbaceous"   {:nname   "Live Herbaceous Surface Area to Volume Ratio"
                           :kind    :continuous
                           :domain  "Surface Area/Vol Ratio"
                           :minimum 1.0
                           :maximum 4000.0}
   "savrLiveWoody"        {:nname   "Live Woody Surface Area to Volume Ratio"
                           :kind    :continuous
                           :domain  "Surface Area/Vol Ratio"
                           :minimum 1.0
                           :maximum 4000.0}})

;; Field order in the editor, by the C++ parameter each one fills. `isDynamic` is the only field with no
;; variable: a bare bool with no BehavePlus quantity behind it, which renders from its parameter type.
(def ^:private field-order
  ["fuelModelNumber" "code" "name" "fuelBedDepth" "moistureOfExtinctionDead"
   "heatOfCombustionDead" "heatOfCombustionLive"
   "fuelLoadOneHour" "fuelLoadTenHour" "fuelLoadHundredHour"
   "fuelLoadLiveHerbaceous" "fuelLoadLiveWoody"
   "savrOneHour" "savrLiveHerbaceous" "savrLiveWoody"
   "isDynamic"])

(def ^:private key-parameter "fuelModelNumber")

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; New variables are asserted in the same transaction as the fields that point at them, so they carry a
;; string tempid the field can reference. A map value would nest a *copy* of the variable under the field
;; instead of referring to it.
(defn- variable-tempid [param] (str "custom-fuel-model-variable-" param))

(defn- ->variable-payload [db [param {:keys [nname kind domain minimum maximum]}]]
  [param (cond-> (sm/->variable db (cond-> {:nname nname :kind kind}
                                     domain (assoc :domain-uuid (sm/name->uuid db :domain/name domain))))
           :always (assoc :db/id (variable-tempid param))
           minimum (assoc :variable/minimum minimum)
           maximum (assoc :variable/maximum maximum))])

(defn- ->field [db variable-by-param order param]
  (let [variable (get variable-by-param param)]
    (cond-> (sm/->entity
             {:record-field/order         order
              :record-field/source        (if (= param key-parameter) :generated :input)
              :record-field/cpp-parameter (sm/cpp-param->uuid db cpp-namespace cpp-class cpp-function param)})
      (= param key-parameter)
      (assoc :record-field/key? true)

      variable
      (assoc :record-field/variable variable))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (if (d/q '[:find ?e . :in $ ?tk :where [?e :record-type/translation-key ?tk]] db translation-key)
    []
    (let [app-eid      (d/q '[:find ?e . :where [?e :application/name "BehavePlus"]] db)
          new-vars     (into {} (map (partial ->variable-payload db) new-variables))
          variable-ref (merge (zipmap (keys new-vars) (map variable-tempid (keys new-vars)))
                              (into {} (for [[param nname] reused-variables]
                                         [param (sm/name->eid db :variable/name nname)])))
          fields       (vec (map-indexed (partial ->field db variable-ref) field-order))]
      (when (nil? app-eid)
        (throw (ex-info "No BehavePlus application to attach the record type to." {})))
      (concat
       (vals new-vars)
       [{:db/id                    app-eid
         :application/record-types [(sm/->entity
                                     {:record-type/name                   "Custom Fuel Model"
                                      :record-type/translation-key        translation-key
                                      :record-type/lib-ns                 "behave.lib.fuel-models"
                                      :record-type/cpp-namespace          (sm/cpp-ns->uuid db cpp-namespace)
                                      :record-type/cpp-class              (sm/cpp-class->uuid db cpp-namespace cpp-class)
                                      :record-type/cpp-function           (sm/cpp-fn->uuid db cpp-namespace cpp-class cpp-function)
                                      ;; Placeholders are C++ parameter names, which are stable and short.
                                      :record-type/label-template         "{code} — {name}"
                                      :record-type/order                  0
                                      :record-type/fields                 fields
                                      :record-type/target-group-variables (mapv #(sm/t-key->eid db %)
                                                                                target-group-variable-keys)})]}]
       (sm/upsert-translations db "en-US" {translation-key "Custom Fuel Model"})))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
