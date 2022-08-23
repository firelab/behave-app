(ns behave.lib.units
  (:require [behave.lib.enums :as enum]))

; TODO Move to map-utils
(defn index-by [k coll]
  (persistent! (reduce
                 (fn [acc cur] (assoc! acc (get cur k) cur))
                 (transient {})
                 coll)))

(def unitless
  [{:short "#"}
   {:short "%" :unit-type enum/cover-units :unit "Percent"}
   {:short "deg" :unit-type enum/slope-units :unit "Degrees"}
   {:short "fraction" :unit-type enum/cover-units :unit "Fraction"}
   {:short "points"} ; FIXME
   {:short "ratio"} ; FIXME
   ])

(def ^:private english-units
  [{:short "Btu/ft/s" :unit-type enum/fireline-intensity-units :unit "BtusPerFootPerSecond"}
   {:short "Btu/ft/min" :unit-type enum/fireline-intensity-units :unit "BtusPerFootPerMinute"}
   {:short "Btu/ft2" :unit-type enum/heat-unit-per-unit-area-units :unit "BtusPerSquareFoot"}
   {:short "Btu/ft2/min" :unit-type enum/heat-source-reaction-units :unit "BtusPerSquareFootPerMinute"}
   {:short "Btu/ft2/sec" :unit-type enum/heat-source-reaction-units :unit "BtusPerSquareFootPerSecond"}
   {:short "Btu/ft3" :unit-type enum/heat-sink-units :unit "BtusPerCubicFoot"}
   {:short "Btu/lb" :unit-type enum/heat-combustion-units :unit "BtusPerPound"}
   {:short "ac" :unit-type enum/area-units :unit "Acres"}
   {:short "ch" :unit-type enum/length-units :unit "Chains"}
   {:short "ch/h" :unit-type enum/speed-units :unit "ChainsPerHour"}
   {:short "ft" :unit-type enum/length-units :unit "Feet"}
   {:short "ft-lb/s/ft2"}
   {:short "ft/min" :unit-type enum/speed-units :unit "FeetPerMinute"}
   {:short "ft2" :unit-type enum/area-units :unit "SquareFeet"}
   {:short "ft2/ac"} ; FIXME
   {:short "ft2/ft3"} ; FIXME
   {:short "in" :unit-type enum/length-units :unit "Inches"}
   {:short "in/mi"} ; FIXME
   {:short "lb/ft3" :unit-type enum/density-units :unit "PoundsPerCubicFoot"}
   {:short "lbs/ft3" :unit-type enum/density-units :unit "PoundsPerCubicFoot"}
   {:short "mi" :unit-type enum/length-units :unit "Miles"}
   {:short "mi/h" :unit-type enum/speed-units :unit "MilesPerHour"}
   {:short "ms"} ; FIXME
   {:short "oF" :unit-type enum/temperature-units  :unit "Fahrenheit"}
   {:short "per ac"} ; FIXME
   {:short "ton/ac"} ; FIXME
   ])

(def ^:private metric-units
  [{:short "cm"} ; FIXME
   {:short "cm/km"} ; FIXME
   {:short "ha" :unit-type enum/area-units :unit "Hectares"}
   {:short "kJ/kg" :unit-type enum/heat-combustion-units :unit "KilojoulesPerKilogram"}
   {:short "kJ/m2"} ; FIXME
   {:short "kJ/m3" :unit-type enum/heat-sink-units :unit "KilojoulesPerCubicMeter"}
   {:short "kW/m" :unit-type enum/fireline-intensity-units :unit "KilowattsPerMeter"}
   {:short "kW/m2" :unit-type enum/heat-source-reaction-units :unit "KilowattsPerSquareMeter"}
   {:short "kg/m3" :unit-type enum/density-units :unit "KilogramsPerCubicMeter"}
   {:short "km" :unit-type enum/length-units :unit "Kilometers"}
   {:short "km/h" :unit-type enum/speed-units :unit "KilometersPerHour"}
   {:short "m" :unit-type enum/length-units :unit "Meters"}
   {:short "m-kg/s/m2"} ; FIXME
   {:short "m/h"} ; FIXME
   {:short "m/min" :unit-type enum/speed-units :unit "MetersPerMinute"}
   {:short "m2" :unit-type enum/area-units :unit "SquareMeters"}
   {:short "m2/ha"} ; FIXME
   {:short "m2/m3" :unit-type enum/surface-area-to-volume-units :unit "SquareMetersOverCubicMeters"}
   {:short "mm"} ; FIXME
   {:short "ms"} ; FIXME
   {:short "oC" :unit-type enum/temperature-units  :unit "Celsius"}
   {:short "per ha"} ; FIXME
   {:short "points"} ; FIXME
   {:short "tonne/ha" :unit-type enum/loading-units :unit "TonnesPerHectare"}])

(def ^:private time-units
  [{:short "s" :unit-type enum/time-units :unit "Seconds"}
   {:short "min" :unit-type enum/time-units :unit "Minutes"}
   {:short "h" :unit-type enum/time-units :unit "Hours"}
   {:short "days"} ; FIXME
   {:short "years"} ; FIXME
   ])

(def all-units (index-by :short
                         (concat unitless
                                 english-units
                                 metric-units
                                 time-units)))

(defn get-unit [short-hand]
  (let [unit (get all-units short-hand)]
    (get (:unit-type unit) (:unit unit))))
