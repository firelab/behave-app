(ns behave.lib.enums
  (:require [clojure.string :as str]))

(defonce all-enums (atom {}))

(defn- enum-value [enum-name idx member]
  (let [member (last (str/split member #"::"))
        f      (when (and (aget js/window "Module")
                          (aget js/window "runtimeInitialized"))
                 (aget js/Module (str "_emscripten_enum_" enum-name "_" member)))
        value  (if (fn? f) (f) idx)]
    [member value]))

(defn enum [enum-name members]
  (let [member-vals (into {} (map-indexed (partial enum-value enum-name) members))
        enum-name   (first (str/split enum-name #"_"))]
    (swap! all-enums assoc enum-name member-vals)
    member-vals))

(def area-units
  (enum "AreaUnits_AreaUnitsEnum"
        ["AreaUnits::SquareFeet"
         "AreaUnits::Acres"
         "AreaUnits::Hectares"
         "AreaUnits::SquareMeters"
         "AreaUnits::SquareMiles"
         "AreaUnits::SquareKilometers"]))

(def beetle-damage
  (enum "BeetleDamage_BeetleDamageEnum"
        ["BeetleDamage::no"
         "BeetleDamage::yes"]))

(def contain-flank
  (enum "ContainFlank_ContainFlankEnum"
        ["ContainFlank::LeftFlank"
         "ContainFlank::RightFlank"
         "ContainFlank::BothFlanks"
         "ContainFlank::NeitherFlank"]))

(def contain-status
  (enum "ContainStatus_ContainStatusEnum"
        ["ContainStatus::Unreported"
         "ContainStatus::Reported"
         "ContainStatus::Attacked"
         "ContainStatus::Contained"
         "ContainStatus::Overrun"
         "ContainStatus::Exhausted"
         "ContainStatus::Overflow"
         "ContainStatus::SizeLimitExceeded"
         "ContainStatus::TimeLimitExceeded"]))

(def contain-tactic
  (enum "ContainTactic_ContainTacticEnum"
        ["ContainTactic::HeadAttack"
         "ContainTactic::RearAttack"]))

(def cover-units
  (enum "CoverUnits_CoverUnitsEnum"
        ["CoverUnits::Fraction"
         "CoverUnits::Percent"]))

(def density-units
  (enum "DensityUnits_DensityUnitsEnum"
        ["DensityUnits::PoundsPerCubicFoot"
         "DensityUnits::KilogramsPerCubicMeter"]))

(def equation-type
  (enum "EquationType::EquationTypeEnum"
        ["EquationType::crown_scorch"
         "EquationType::bole_char"
         "EquationType::crown_damage"]))

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

(def flame-length-or-scorch-height-switch
  (enum "FlameLengthOrScorchHeightSwitch_FlameLengthOrScorchHeightSwitchEnum"
        ["FlameLengthOrScorchHeightSwitch::flame_length"
         "FlameLengthOrScorchHeightSwitch::scorch_height"]))

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
  (enum "IgnitionFuelBedType_IgnitionFuelBedTypeEnum"
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
  (enum "LightningCharge_LightningChargeEnum"
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

(def region-code
  (enum "RegionCode_RegionCodeEnum"
        ["RegionCode::none"
         "RegionCode::interior_west"
         "RegionCode::pacific_east"
         "RegionCode::south_east"
         "RegionCode::north_east"]))

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
  (enum "SpotTreeSpecies_SpotTreeSpeciesEnum"
        ["SpotTreeSpecies::ENGELMANN_SPRUCE"
         "SpotTreeSpecies::DOUGLAS_FIR"
         "SpotTreeSpecies::SUBALPINE_FIR"
         "SpotTreeSpecies::WESTERN_HEMLOCK"
         "SpotTreeSpecies::PONDEROSA_PINE"
         "SpotTreeSpecies::LODGEPOLE_PINE"
         "SpotTreeSpecies::WESTERN_WHITE_PINE"
         "SpotTreeSpecies::GRAND_FIR"
         "SpotTreeSpecies::BALSAM_FIR"
         "SpotTreeSpecies::SLASH_PINE"
         "SpotTreeSpecies::LONGLEAF_PINE"
         "SpotTreeSpecies::POND_PINE"
         "SpotTreeSpecies::SHORTLEAF_PINE"
         "SpotTreeSpecies::LOBLOLLY_PINE"]))

(def spot-fire-location
  (enum "SpotFireLocation_SpotFireLocationEnum"
        ["SpotFireLocation::MIDSLOPE_WINDWARD"
         "SpotFireLocation::VALLEY_BOTTOM"
         "SpotFireLocation::MIDSLOPE_LEEWARD"
         "SpotFireLocation::RIDGE_TOP"]))

(def spot-array-constants
  (enum "SpotArrayConstants_SpotArrayConstantsEnum"
        ["SpotArrayConstants::NUM_COLS"
         "SpotArrayConstants::NUM_FIREBRAND_ROWS"
         "SpotArrayConstants::NUM_SPECIES"]))

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
