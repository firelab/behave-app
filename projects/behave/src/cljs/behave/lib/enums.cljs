(ns behave.lib.enums
  (:require [clojure.string :as str]))

(defonce all-enums (atom {}))

(defn- enum-value [enum-name idx member]
  (let [member (last (str/split member #"::"))
        f      (when (aget js/window "Module")
                 (aget js/Module (str "_emscripten_enum_" enum-name "_" member)))
        value  (if (fn? f) (f) idx)]
    [member value]))

(defn setup-enum [enum-name members]
  (let [member-vals (into {} (map-indexed (partial enum-value enum-name) members))
        enum-name   (first (str/split enum-name #"_"))]
    (swap! all-enums assoc enum-name member-vals)
    member-vals))

(defn enum [enum-name members]
  (if (true? (aget js/window "runtimeInitialized"))
    (setup-enum enum-name members)
    (js/setTimeout #(enum enum-name members) 1000)))

(def area-units
  (enum "AreaUnits_AreaUnitsEnum"
        ["AreaUnits::SquareFeet"
         "AreaUnits::Acres"
         "AreaUnits::Hectares"
         "AreaUnits::SquareMeters"
         "AreaUnits::SquareMiles"
         "AreaUnits::SquareKilometers"]))

(def contain-flank
  (enum "ContainFlank"
        ["LeftFlank"
         "RightFlank"
         "BothFlanks"
         "NeitherFlank"]))

(def contain-status
  (enum "ContainStatus"
        ["Unreported"
         "Reported"
         "Attacked"
         "Contained"
         "Overrun"
         "Exhausted"
         "Overflow"
         "SizeLimitExceeded"
         "TimeLimitExceeded"]))

(def contain-tactic
  (enum "ContainTactic"
        ["HeadAttack"
         "RearAttack"]))

(def cover-units
  (enum "CoverUnits_CoverUnitsEnum"
        ["CoverUnits::Fraction"
         "CoverUnits::Percent"]))

(def density-units
  (enum "DensityUnits_DensityUnitsEnum"
        ["DensityUnits::PoundsPerCubicFoot"
         "DensityUnits::KilogramsPerCubicMeter"]))

(def fireline-intensity-units
  (enum "FirelineIntensityUnits_FirelineIntensityUnitsEnum"
        ["FirelineIntensityUnits::BtusPerFootPerSecond"
         "FirelineIntensityUnits::BtusPerFootPerMinute"
         "FirelineIntensityUnits::KilojoulesPerMeterPerSecond"
         "FirelineIntensityUnits::KilojoulesPerMeterPerMinute"
         "FirelineIntensityUnits::KilowattsPerMeter"]))

(def fire-type
  (enum "FireType_FireTypeEnum"
        ["FireType::Surface"
         "FireType::Torching"
         "FireType::ConditionalCrownFire"
         "FireType::Crowning"]))

(def heat-combustion-units
  (enum "HeatOfCombustionUnits_HeatOfCombustionUnitsEnum"
        ["HeatOfCombustionUnits::BtusPerPound"
         "HeatOfCombustionUnits::KilojoulesPerKilogram"]))

(def heat-sink-units
  (enum "HeatSinkUnits_HeatSinkUnitsEnum"
        ["HeatSinkUnits::BtusPerCubicFoot"
         "HeatSinkUnits::KilojoulesPerCubicMeter"]))

(def heat-source-reaction-units
  (enum "HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum"
        ["HeatSourceAndReactionIntensityUnits::BtusPerSquareFootPerMinute"
         "HeatSourceAndReactionIntensityUnits::BtusPerSquareFootPerSecond"
         "HeatSourceAndReactionIntensityUnits::KilojoulesPerSquareMeterPerSecond"
         "HeatSourceAndReactionIntensityUnits::KilojoulesPerSquareMeterPerMinute"
         "HeatSourceAndReactionIntensityUnits::KilowattsPerSquareMeter"]))

(def heat-unit-per-unit-area-units
  (enum "HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum"
        ["HeatPerUnitAreaUnits::BtusPerSquareFoot"
         "HeatPerUnitAreaUnits::KilojoulesPerSquareMeter"
         "HeatPerUnitAreaUnits::KilowattsPerSquareMeterPerSecond"]))

(def ignition-fuel-bed-type
  (enum "IgnitionFuelBedType"
        ["PonderosaPineLitter"
         "PunkyWoodRottenChunky"
         "PunkyWoodPowderDeep"
         "PunkWoodPowderShallow"
         "LodgepolePineDuff"
         "DouglasFirDuff"
         "HighAltitudeMixed"
         "PeatMoss"]))

(def length-units
  (enum "LengthUnits_LengthUnitsEnum"
        ["LengthUnits::Feet"
         "LengthUnits::Inches"
         "LengthUnits::Centimeters"
         "LengthUnits::Meters"
         "LengthUnits::Chains"
         "LengthUnits::Miles"
         "LengthUnits::Kilometers"]))

(def lightning-charge
  (enum "LightningCharge"
        ["Negative"
         "Positive"
         "Unknown"]))

(def loading-units
  (enum "LoadingUnits_LoadingUnitsEnum"
        ["LoadingUnits::PoundsPerSquareFoot"
         "LoadingUnits::TonsPerAcre"
         "LoadingUnits::TonnesPerHectare"
         "LoadingUnits::KilogramsPerSquareMeter"]))

(def moisture-units
  (enum "MoistureUnits_MoistureUnitsEnum"
        ["MoistureUnits::Fraction"
         "MoistureUnits::Percent"]))

(def probability-units
  (enum "ProbabilityUnits_ProbabilityUnitsEnum"
        ["ProbabilityUnits::Fraction"
         "ProbabilityUnits::Percent"]))

(def slope-units
  (enum "SlopeUnits_SlopeUnitsEnum"
        ["SlopeUnits::Degrees"
         "SlopeUnits::Percent"]))

(def speed-units
  (enum "SpeedUnits_SpeedUnitsEnum"
        ["SpeedUnits::FeetPerMinute"
         "SpeedUnits::ChainsPerHour"
         "SpeedUnits::MetersPerSecond"
         "SpeedUnits::MetersPerMinute"
         "SpeedUnits::MetersPerHour"
         "SpeedUnits::MilesPerHour"
         "SpeedUnits::KilometersPerHour"]))

(def spot-tree-species
  (enum "SpotTreeSpecies"
        ["ENGELMANN_SPRUCE"
         "DOUGLAS_FIR"
         "SUBALPINE_FIR"
         "WESTERN_HEMLOCK"
         "PONDEROSA_PINE"
         "LODGEPOLE_PINE"
         "WESTERN_WHITE_PINE"
         "GRAND_FIR"
         "BALSAM_FIR"
         "SLASH_PINE"
         "LONGLEAF_PINE"
         "POND_PINE"
         "SHORTLEAF_PINE"
         "LOBLOLLY_PINE"]))

(def spot-fire-location
  (enum "SpotFireLocation"
        ["MIDSLOPE_WINDWARD"
         "VALLEY_BOTTOM"
         "MIDSLOPE_LEEWARD"
         "RIDGE_TOP"]))

(def spot-array-constants
  (enum "SpotArrayConstants"
        ["NUM_COLS"
         "NUM_FIREBRAND_ROWS"
         "NUM_SPECIES"]))

(def surface-area-to-volume-units
  (enum "SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum"
        ["SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet"
         "SurfaceAreaToVolumeUnits::SquareMetersOverCubicMeters"
         "SurfaceAreaToVolumeUnits::SquareInchesOverCubicInches"
         "SurfaceAreaToVolumeUnits::SquareCentimetersOverCubicCentimers"]))

(def temperature-units
  (enum "TemperatureUnits_TemperatureUnitsEnum"
        ["TemperatureUnits::Fahrenheit"
         "TemperatureUnits::Celsius"
         "TemperatureUnits::Kelvin"]))

(def time-units
  (enum "TimeUnits_TimeUnitsEnum"
        ["TimeUnits::Minutes"
         "TimeUnits::Seconds"
         "TimeUnits::Hours"]))

(def wind-and-spread-orientation-mode
  (enum "WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum"
        ["WindAndSpreadOrientationMode::RelativeToUpslope"
         "WindAndSpreadOrientationMode::RelativeToNorth"]))

(def wind-height-input-mode
  (enum "WindHeightInputMode_WindHeightInputModeEnum"
        ["WindHeightInputMode::DirectMidflame"
         "WindHeightInputMode::TwentyFoot"
         "WindHeightInputMode::TenMeter"]))
