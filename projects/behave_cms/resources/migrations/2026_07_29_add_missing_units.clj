(ns migrations.2026-07-29-add-missing-units
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1607 — Add "missing" units from the Variables Spreadsheet in Settings.
;;
;; The C++ layer (behaveUnits.h) already implements nearly every unit listed in
;; the spreadsheet's Unit Preferences tab; the VMS simply has no :unit entity
;; for them, so the Settings unit selectors can never offer them. This
;; migration:
;;
;;   1. Adds the new SpeedUnits::FurlongsPerFortnight enum member (appended to
;;      the C++ enum in behave-mirror as part of this same ticket).
;;   2. Adds the missing :unit entities under their existing dimensions:
;;        Loading                — lb/ft2, kg/m2
;;        Surface Area To Volume — in2/in3, cm2/cm3
;;        Speed                  — m/s, fur/fortnight
;;        Temperature            — K
;;        Heat Per Unit Area     — kW-s/m2
;;        Fireline Intensity     — kJ/m/s, kJ/m/min
;;        Heat Source Reaction   — kJ/m2/s, kJ/m2/min
;;   3. Appends m/s to the "Wind Speed" domain's filtered units (that domain
;;      shows only its filter list per BHP1-1370).
;;   4. Gives "Crown Rate of Spread" a filter of the previously-visible speed
;;      units + m/s, so furlongs/fortnight surfaces only under
;;      "Surface Rate of Spread" (per the spreadsheet).
;;   5. Points the "Fuel & Extinction Moisture" domain at the Fraction
;;      dimension — it had no :domain/dimension-uuid at all, which is why its
;;      Settings row never offered "fraction" (its unit uuids already point at
;;      the Fraction dimension's "%" unit).
;;   6. Re-points "P-G Age of Rough" from Fraction/fraction to Time/years
;;      (numerically a no-op: english = metric = native both before and after,
;;      so no conversion is ever applied), filtered to years only.
;;
;; After this runs, re-export layout.msgpack so the app picks up the new units.

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

(defn- dimension-eid [db dim-name]
  (d/q '[:find ?e . :in $ ?n :where [?e :dimension/name ?n]] db dim-name))

(defn- dimension-uuid [db dim-name]
  (d/q '[:find ?uuid . :in $ ?n
         :where [?e :dimension/name ?n] [?e :bp/uuid ?uuid]]
       db dim-name))

(defn- domain-eid [db domain-name]
  (d/q '[:find ?e . :in $ ?n :where [?e :domain/name ?n]] db domain-name))

(defn- enum-eid [db enum-name]
  (d/q '[:find ?e . :in $ ?n :where [?e :cpp.enum/name ?n]] db enum-name))

(defn- enum-member-uuid [db enum-name member-name]
  (d/q '[:find ?uuid . :in $ ?en ?mn
         :where
         [?e :cpp.enum/name ?en]
         [?e :cpp.enum/enum-member ?m]
         [?m :cpp.enum-member/name ?mn]
         [?m :bp/uuid ?uuid]]
       db enum-name member-name))

(defn- next-enum-member-value [db enum-name]
  (->> (d/q '[:find [?v ...] :in $ ?en
              :where
              [?e :cpp.enum/name ?en]
              [?e :cpp.enum/enum-member ?m]
              [?m :cpp.enum-member/value ?v]]
            db enum-name)
       (apply max)
       (inc)))

(defn- dimension-unit-uuid [db dim-name short-code]
  (d/q '[:find ?uuid . :in $ ?dn ?sc
         :where
         [?d :dimension/name ?dn]
         [?d :dimension/units ?u]
         [?u :unit/short-code ?sc]
         [?u :bp/uuid ?uuid]]
       db dim-name short-code))

(defn- dimension-unit-uuids [db dim-name]
  (d/q '[:find [?uuid ...] :in $ ?dn
         :where
         [?d :dimension/name ?dn]
         [?d :dimension/units ?u]
         [?u :bp/uuid ?uuid]]
       db dim-name))

(defn- add-unit-tx
  "Tx map appending a new unit (with a pre-minted `:bp/uuid`) to a dimension."
  [db dim-name unit]
  {:db/id           (dimension-eid db dim-name)
   :dimension/units (sm/postwalk-insert [unit])})

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def ^:private speed-enum "SpeedUnits_SpeedUnitsEnum")

#_{:clj-kondo/ignore [:missing-docstring :unused-binding]}
(defn payload-fn [db]
  (let [furlong-member-uuid (str (d/squuid))
        ms-unit-uuid        (str (d/squuid))
        furlong-unit-uuid   (str (d/squuid))
        years-unit-uuid     (dimension-unit-uuid db "Time" "years")
        member-uuid         (partial enum-member-uuid db)
        new-unit            (fn [cpp-member-uuid unit-name short-code system]
                              {:bp/uuid                   (str (d/squuid))
                               :unit/name                 unit-name
                               :unit/short-code           short-code
                               :unit/system               system
                               :unit/cpp-enum-member-uuid cpp-member-uuid})]
    (concat
     ;; 1. New SpeedUnits enum member (appended last in C++, matching value)
     [{:db/id                (enum-eid db speed-enum)
       :cpp.enum/enum-member (sm/postwalk-insert
                              [{:bp/uuid               furlong-member-uuid
                                :cpp.enum-member/name  "FurlongsPerFortnight"
                                :cpp.enum-member/value (next-enum-member-value db speed-enum)}])}]

     ;; 2. New units under their existing dimensions
     [(add-unit-tx db "Loading"
                   (new-unit (member-uuid "LoadingUnits_LoadingUnitsEnum" "PoundsPerSquareFoot")
                             "Pounds Per Square Foot (lb/ft2)" "lb/ft2" :english))
      (add-unit-tx db "Loading"
                   (new-unit (member-uuid "LoadingUnits_LoadingUnitsEnum" "KilogramsPerSquareMeter")
                             "Kilograms Per Square Meter (kg/m2)" "kg/m2" :metric))
      (add-unit-tx db "Surface Area To Volume"
                   (new-unit (member-uuid "SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum" "SquareInchesOverCubicInches")
                             "Square Inches Over Cubic Inches (in2/in3)" "in2/in3" :english))
      (add-unit-tx db "Surface Area To Volume"
                   (new-unit (member-uuid "SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum" "SquareCentimetersOverCubicCentimeters")
                             "Square Centimeters Over Cubic Centimeters (cm2/cm3)" "cm2/cm3" :metric))
      (add-unit-tx db "Speed"
                   (assoc (new-unit (member-uuid speed-enum "MetersPerSecond")
                                    "Meters Per Second (m/s)" "m/s" :metric)
                          :bp/uuid ms-unit-uuid))
      (add-unit-tx db "Speed"
                   (assoc (new-unit furlong-member-uuid
                                    "Furlongs Per Fortnight (fur/fortnight)" "fur/fortnight" :english)
                          :bp/uuid furlong-unit-uuid))
      (add-unit-tx db "Temperature"
                   (new-unit (member-uuid "TemperatureUnits_TemperatureUnitsEnum" "Kelvin")
                             "Kelvin (K)" "K" :metric))
      (add-unit-tx db "Heat Per Unit Area"
                   (new-unit (member-uuid "HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum" "KilowattSecondsPerSquareMeter")
                             "Kilowatt Seconds Per Square Meter (kW-s/m2)" "kW-s/m2" :metric))
      (add-unit-tx db "Fireline Intensity"
                   (new-unit (member-uuid "FirelineIntensityUnits_FirelineIntensityUnitsEnum" "KilojoulesPerMeterPerSecond")
                             "Kilojoules Per Meter Per Second (kJ/m/s)" "kJ/m/s" :metric))
      (add-unit-tx db "Fireline Intensity"
                   (new-unit (member-uuid "FirelineIntensityUnits_FirelineIntensityUnitsEnum" "KilojoulesPerMeterPerMinute")
                             "Kilojoules Per Meter Per Minute (kJ/m/min)" "kJ/m/min" :metric))
      (add-unit-tx db "Heat Source Reaction"
                   (new-unit (member-uuid "HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum" "KilojoulesPerSquareMeterPerSecond")
                             "Kilojoules Per Square Meter Per Second (kJ/m2/s)" "kJ/m2/s" :metric))
      (add-unit-tx db "Heat Source Reaction"
                   (new-unit (member-uuid "HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum" "KilojoulesPerSquareMeterPerMinute")
                             "Kilojoules Per Square Meter Per Minute (kJ/m2/min)" "kJ/m2/min" :metric))]

     ;; 3. Wind Speed shows only its filter list — append m/s
     [{:db/id                      (domain-eid db "Wind Speed")
       :domain/filtered-unit-uuids [ms-unit-uuid]}]

     ;; 4. Crown ROS keeps its previously-visible units + m/s (no furlongs)
     [{:db/id                      (domain-eid db "Crown Rate of Spread")
       :domain/filtered-unit-uuids (conj (vec (dimension-unit-uuids db "Speed"))
                                         ms-unit-uuid)}]

     ;; 5. Fuel & Extinction Moisture never had a dimension; its unit uuids
     ;;    already point at the Fraction dimension's "%" unit
     [{:db/id                 (domain-eid db "Fuel & Extinction Moisture")
       :domain/dimension-uuid (dimension-uuid db "Fraction")}]

     ;; 6. P-G Age of Rough: Fraction/fraction -> Time/years (all three unit
     ;;    prefs match before and after, so no value conversion is affected)
     [{:db/id                      (domain-eid db "P-G Age of Rough")
       :domain/dimension-uuid      (dimension-uuid db "Time")
       :domain/english-unit-uuid   years-unit-uuid
       :domain/metric-unit-uuid    years-unit-uuid
       :domain/native-unit-uuid    years-unit-uuid
       :domain/filtered-unit-uuids [years-unit-uuid]}])))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms]
           '[behave-cms.store  :as store])
  (cms/init-db!)

  (def conn (store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
