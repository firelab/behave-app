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
   {:short "%" :system "english" :enum enum/cover-units :unit "Percent"}
   {:short "deg" :system "english" :enum enum/slope-units :unit "Degrees"}
   {:short "fraction" :system "english" :enum enum/cover-units :unit "Fraction"}
   {:short "points"} ; FIXME Contain Fire Points
   {:short "ratio"}
   ])

(def ^:private english-units
  [{:short "Btu/ft/s"    :system "english" :enum enum/fireline-intensity-units      :unit "BtusPerFootPerSecond"}
   {:short "Btu/ft/min"  :system "english" :enum enum/fireline-intensity-units      :unit "BtusPerFootPerMinute"}
   {:short "Btu/ft2"     :system "english" :enum enum/heat-unit-per-unit-area-units :unit "BtusPerSquareFoot"}
   {:short "Btu/ft2/min" :system "english" :enum enum/heat-source-reaction-units    :unit "BtusPerSquareFootPerMinute"}
   {:short "Btu/ft2/sec" :system "english" :enum enum/heat-source-reaction-units    :unit "BtusPerSquareFootPerSecond"}
   {:short "Btu/ft3"     :system "english" :enum enum/heat-sink-units               :unit "BtusPerCubicFoot"}
   {:short "Btu/lb"      :system "english" :enum enum/heat-combustion-units         :unit "BtusPerPound"}
   {:short "ac"          :system "english" :enum enum/area-units                    :unit "Acres"}
   {:short "ch"          :system "english" :enum enum/length-units                  :unit "Chains"}
   {:short "ch/h"        :system "english" :enum enum/speed-units                   :unit "ChainsPerHour"}
   {:short "ft"          :system "english" :enum enum/length-units                  :unit "Feet"}
   {:short "ft-lb/s/ft2"} ; FIXME Power Units
   {:short "ft/min"      :system "english" :enum enum/speed-units                   :unit "FeetPerMinute"}
   {:short "ft2"         :system "english" :enum enum/area-units                    :unit "SquareFeet"}
   {:short "ft2/ac"      :system "english" :enum enum/basal-area                    :unit "SquareFeetPerAcre"}
   {:short "ft2/ft3"     :system "english" :enum enum/surface-area-to-volume-units  :unit "SquareFeetOverCubicFeet"}
   {:short "in"          :system "english" :enum enum/length-units                  :unit "Inches"}
   {:short "lb/ft3"      :system "english" :enum enum/density-units                 :unit "PoundsPerCubicFoot"}
   {:short "lbs/ft3"     :system "english" :enum enum/density-units                 :unit "PoundsPerCubicFoot"}
   {:short "mi"          :system "english" :enum enum/length-units                  :unit "Miles"}
   {:short "mi/h"        :system "english" :enum enum/speed-units                   :unit "MilesPerHour"}
   {:short "ms"} ; FIXME
   {:short "oF"          :system "english" :enum enum/temperature-units             :unit "Fahrenheit"}
   {:short "per ac"} ; FIXME Tree Count
   {:short "ton/ac"      :system "english" :enum enum/loading-units                 :unit "TonnesPerAcre"}
   {:short "psi"         :system "english" :enum enum/pressure-units                :unit "PoundPerSquareInch"}])

(def ^:private metric-units
  [{:short "cm"       :system "metric" :enum enum/length-units                  :unit "centimeters"}
   {:short "ha"       :system "metric" :enum enum/area-units                    :unit "Hectares"}
   {:short "kJ/kg"    :system "metric" :enum enum/heat-combustion-units         :unit "KilojoulesPerKilogram"}
   {:short "kJ/m2"    :system "metric" :enum enum/heat-unit-per-unit-area-units :unit "KilojoulesPerSquareMeter"}
   {:short "kJ/m3"    :system "metric" :enum enum/heat-sink-units               :unit "KilojoulesPerCubicMeter"}
   {:short "kW/m"     :system "metric" :enum enum/fireline-intensity-units      :unit "KilowattsPerMeter"}
   {:short "kW/m2"    :system "metric" :enum enum/heat-source-reaction-units    :unit "KilowattsPerSquareMeter"}
   {:short "kg/m3"    :system "metric" :enum enum/density-units                 :unit "KilogramsPerCubicMeter"}
   {:short "km"       :system "metric" :enum enum/length-units                  :unit "Kilometers"}
   {:short "km/h"     :system "metric" :enum enum/speed-units                   :unit "KilometersPerHour"}
   {:short "m"        :system "metric" :enum enum/length-units                  :unit "Meters"}
   {:short "m/h"      :system "metric" :enum enum/speed-units                   :unit "MetersPerHour"} ; FIXME
   {:short "m/min"    :system "metric" :enum enum/speed-units                   :unit "MetersPerMinute"}
   {:short "m2"       :system "metric" :enum enum/area-units                    :unit "SquareMeters"}
   {:short "m2/ha"    :system "metric" :enum enum/basal-area                    :unit "SquareMetersPerHectare"}
   {:short "m2/m3"    :system "metric" :enum enum/surface-area-to-volume-units  :unit "SquareMetersOverCubicMeters"}
   {:short "mm"       :system "metric" :enum enum/length-units                  :unit "Millimeters"}
   {:short "oC"       :system "metric" :enum enum/temperature-units             :unit "Celsius"}
   {:short "per ha"} ; FIXME Tree Density
   {:short "tonne/ha" :system "metric" :enum enum/loading-units                 :unit "TonnesPerHectare"}
   {:short "Pa"       :system "metric" :enum enum/pressure-units                :unit "Pascal"}
   {:short "kPa"      :system "metric" :enum enum/pressure-units                :unit "KiloPascal"}
   {:short "MPa"      :system "metric" :enum enum/pressure-units                :unit "MegaPascal"}
   {:short "GPa"      :system "metric" :enum enum/pressure-units                :unit "GigaPascal"}
   {:short "bar"      :system "metric" :enum enum/pressure-units                :unit "Bar"}
   {:short "at"       :system "metric" :enum enum/pressure-units                :unit "TechnicalAtmosphere"}
   {:short "atm"      :system "metric" :enum enum/pressure-units                :unit "Atmosphere"}])

(def ^:private time-units
  [{:short "s" :system "time" :enum enum/time-units :unit "Seconds"}
   {:short "min" :system "time" :enum enum/time-units :unit "Minutes"}
   {:short "h" :system "time" :enum enum/time-units :unit "Hours"}
   {:short "days" :system "time" :enum enum/time-units :unit "Days"}
   {:short "years" :system "time" :enum enum/time-units :unit "Years"}])

(def all-units (index-by :short
                         (concat unitless
                                 english-units
                                 metric-units
                                 time-units)))

(defn get-unit [short-hand]
  (let [unit (get all-units short-hand)]
    (get (:enum unit) (:unit unit))))
