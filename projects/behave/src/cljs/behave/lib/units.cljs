(ns behave.lib.units
  (:require
   [clojure.string :as str]
   [behave.lib.enums :as enum]
   [map-utils.interface :refer [index-by]]
   [string-utils.interface :refer [camel->kebab]]))

(def ^:private dimensions
  ["AreaUnits"
   "BasalAreaUnits"
   "DensityUnits"
   "FirelineIntensityUnits"
   "FractionUnits"
   "HeatOfCombustionUnits"
   "HeatPerUnitAreaUnits"
   "HeatSinkUnits"
   "HeatSourceAndReactionIntensityUnits"
   "LengthUnits"
   "LoadingUnits"
   "PressureUnits"
   "SlopeUnits"
   "SpeedUnits"
   "SurfaceAreaToVolumeUnits"
   "TemperatureUnits"
   "TimeUnits"])

(def ^:private dimension-conversions
  (into {} (map (fn [unit]
                  [(-> unit (str/replace "Units" "") (camel->kebab) (keyword))
                   {:to-fn   (aget js/Module unit "prototype" "toBaseUnits") 
                    :from-fn (aget js/Module unit "prototype" "fromBaseUnits")}])
                dimensions)))

(def ^:private unitless
  [{:short "#"}
   {:short "%" :system "english" :enum enum/cover-units :dimension :fraction :unit "Percent"}
   {:short "deg" :system "english" :enum enum/slope-units :dimension :slope :unit "Degrees"}
   {:short "fraction" :system "english" :enum enum/cover-units :dimension :fraction :unit "Fraction"}
   {:short "points"} ; FIXME Contain Fire Points
   {:short "ratio"}])

(def ^:private english-units
  [{:short "Btu/ft/s"    :system "english" :enum enum/fireline-intensity-units      :dimension :fireline-intensity      :unit "BtusPerFootPerSecond"}
   {:short "Btu/ft/min"  :system "english" :enum enum/fireline-intensity-units      :dimension :fireline-intensity      :unit "BtusPerFootPerMinute"}
   {:short "Btu/ft2"     :system "english" :enum enum/heat-unit-per-unit-area-units :dimension :heat-unit-per-unit-area :unit "BtusPerSquareFoot"}
   {:short "Btu/ft2/min" :system "english" :enum enum/heat-source-reaction-units    :dimension :heat-source-reaction    :unit "BtusPerSquareFootPerMinute"}
   {:short "Btu/ft2/sec" :system "english" :enum enum/heat-source-reaction-units    :dimension :heat-source-reaction    :unit "BtusPerSquareFootPerSecond"}
   {:short "Btu/ft3"     :system "english" :enum enum/heat-sink-units               :dimension :heat-sink               :unit "BtusPerCubicFoot"}
   {:short "Btu/lb"      :system "english" :enum enum/heat-combustion-units         :dimension :heat-combustion         :unit "BtusPerPound"}
   {:short "ac"          :system "english" :enum enum/area-units                    :dimension :area                    :unit "Acres"}
   {:short "ch"          :system "english" :enum enum/length-units                  :dimension :length                  :unit "Chains"}
   {:short "ch/h"        :system "english" :enum enum/speed-units                   :dimension :speed                   :unit "ChainsPerHour"}
   {:short "ft"          :system "english" :enum enum/length-units                  :dimension :length                  :unit "Feet"}
   {:short "ft/min"      :system "english" :enum enum/speed-units                   :dimension :speed                   :unit "FeetPerMinute"}
   {:short "ft2"         :system "english" :enum enum/area-units                    :dimension :area                    :unit "SquareFeet"}
   {:short "ft2/ac"      :system "english" :enum enum/basal-area-units              :dimension :basal-area              :unit "SquareFeetPerAcre"}
   {:short "ft2/ft3"     :system "english" :enum enum/surface-area-to-volume-units  :dimension :surface-area-to-volume  :unit "SquareFeetOverCubicFeet"}
   {:short "in"          :system "english" :enum enum/length-units                  :dimension :length                  :unit "Inches"}
   {:short "lb/ft3"      :system "english" :enum enum/density-units                 :dimension :density                 :unit "PoundsPerCubicFoot"}
   {:short "lbs/ft3"     :system "english" :enum enum/density-units                 :dimension :density                 :unit "PoundsPerCubicFoot"}
   {:short "mi"          :system "english" :enum enum/length-units                  :dimension :length                  :unit "Miles"}
   {:short "mi/h"        :system "english" :enum enum/speed-units                   :dimension :speed                   :unit "MilesPerHour"}
   {:short "ms"} ; FIXME
   {:short "oF"          :system "english" :enum enum/temperature-units             :dimension :temperature-units       :unit "Fahrenheit"}
   {:short "per ac"} ; FIXME Tree Count
   {:short "ton/ac"      :system "english" :enum enum/loading-units                 :dimension :loading-units           :unit "TonnesPerAcre"}
   {:short "psi"         :system "english" :enum enum/pressure-units                :dimension :pressure-units          :unit "PoundPerSquareInch"}])

(def ^:private metric-units
  [{:short "cm"       :system "metric" :enum enum/length-units                  :dimension :length                  :unit "Centimeters"}
   {:short "ha"       :system "metric" :enum enum/area-units                    :dimension :area                    :unit "Hectares"}
   {:short "kJ/kg"    :system "metric" :enum enum/heat-combustion-units         :dimension :heat-combustion         :unit "KilojoulesPerKilogram"}
   {:short "kJ/m2"    :system "metric" :enum enum/heat-unit-per-unit-area-units :dimension :heat-unit-per-unit-area :unit "KilojoulesPerSquareMeter"}
   {:short "kJ/m3"    :system "metric" :enum enum/heat-sink-units               :dimension :heat-sink               :unit "KilojoulesPerCubicMeter"}
   {:short "kW/m"     :system "metric" :enum enum/fireline-intensity-units      :dimension :fireline-intensity      :unit "KilowattsPerMeter"}
   {:short "kW/m2"    :system "metric" :enum enum/heat-source-reaction-units    :dimension :heat-source-reaction    :unit "KilowattsPerSquareMeter"}
   {:short "kg/m3"    :system "metric" :enum enum/density-units                 :dimension :density                 :unit "KilogramsPerCubicMeter"}
   {:short "km"       :system "metric" :enum enum/length-units                  :dimension :length                  :unit "Kilometers"}
   {:short "km/h"     :system "metric" :enum enum/speed-units                   :dimension :speed                   :unit "KilometersPerHour"}
   {:short "m"        :system "metric" :enum enum/length-units                  :dimension :length                  :unit "Meters"}
   {:short "m/h"      :system "metric" :enum enum/speed-units                   :dimension :speed                   :unit "MetersPerHour"} ; FIXME
   {:short "m/min"    :system "metric" :enum enum/speed-units                   :dimension :speed                   :unit "MetersPerMinute"}
   {:short "m2"       :system "metric" :enum enum/area-units                    :dimension :area                    :unit "SquareMeters"}
   {:short "m2/ha"    :system "metric" :enum enum/basal-area-units              :dimension :basal-area              :unit "SquareMetersPerHectare"}
   {:short "m2/m3"    :system "metric" :enum enum/surface-area-to-volume-units  :dimension :surface-area-to-volume  :unit "SquareMetersOverCubicMeters"}
   {:short "mm"       :system "metric" :enum enum/length-units                  :dimension :length                  :unit "Millimeters"}
   {:short "oC"       :system "metric" :enum enum/temperature-units             :dimension :temperature             :unit "Celsius"}
   {:short "per ha"} ; FIXME Tree Density
   {:short "tonne/ha" :system "metric" :enum enum/loading-units                 :dimension :loading                 :unit "TonnesPerHectare"}
   {:short "Pa"       :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "Pascal"}
   {:short "hPa"      :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "HectoPascal"}
   {:short "kPa"      :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "KiloPascal"}
   {:short "MPa"      :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "MegaPascal"}
   {:short "GPa"      :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "GigaPascal"}
   {:short "bar"      :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "Bar"}
   {:short "at"       :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "TechnicalAtmosphere"}
   {:short "atm"      :system "metric" :enum enum/pressure-units                :dimension :pressure                :unit "Atmosphere"}])

(def ^:private time-units
  [{:short "s"     :system "time" :enum enum/time-units :dimension :time :unit "Seconds"}
   {:short "min"   :system "time" :enum enum/time-units :dimension :time :unit "Minutes"}
   {:short "h"     :system "time" :enum enum/time-units :dimension :time :unit "Hours"}
   {:short "days"  :system "time" :enum enum/time-units :dimension :time :unit "Days"}
   {:short "years" :system "time" :enum enum/time-units :dimension :time :unit "Years"}])

(def ^:private all-units
  (index-by :short
            (concat unitless
                    english-units
                    metric-units
                    time-units)))

;;; Public

(defn get-unit
  "Acquire the unit member value for a given `short-hand` unit code."
  [short-hand]
  (let [unit (get all-units short-hand)]
    (get (:enum unit) (:unit unit))))

(defn convert
  "Conversion `value`in `dimension` `from` units `to` new units."
  ([value from to]
   (convert value (get-in all-units [from :dimension]) from to))
  ([value dimension from to]
   (let [{:keys [to-fn from-fn]} (get dimension-conversions dimension)
         to                      (if (string? to) (get-unit to) to)
         from                    (if (string? from) (get-unit from) from)]
     (-> value
         (to-fn from)
         (from-fn to)))))
