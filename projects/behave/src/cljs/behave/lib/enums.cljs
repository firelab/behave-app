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

(def aspen-fire-severity
  (enum "AspenFireSeverity_AspenFireSeverityEnum"
        ["AspenFireSeverity::Low"
         "AspenFireSeverity::Moderate"]))

(def area-units
  (enum "AreaUnits_AreaUnitsEnum"
        ["AreaUnits::SquareFeet"
         "AreaUnits::Acres"
         "AreaUnits::Hectares"
         "AreaUnits::SquareMeters"
         "AreaUnits::SquareMiles"
         "AreaUnits::SquareKilometers"]))

(def basal-area-units
  (enum "BasalAreaUnits_BasalAreaUnitsEnum"
        ["BasalAreaUnits::SquareFeetPerAcre"
         "BasalAreaUnits::SquareMetersPerHectare"]))

(def burning-condition
  (enum "BurningCondition_BurningConditionEnum"
        ["BurningCondition::Low"
         "BurningCondition::Moderate"
         "BurningCondition::Extreme"]))

(def beetle-damage
  (enum "BeetleDamage"
        ["BeetleDamage::not_set"
         "BeetleDamage::no"
         "BeetleDamage::yes"]))

(def chaparral-fuel-type
  (enum "ChaparralFuelType_ChaparralFuelTypeEnum"
        ["ChaparralFuelType::NotSet"
         "ChaparralFuelType::Chamise"
         "ChaparralFuelType::MixedBrush"]))

(def chaparral-fuel-load-input-mode
  (enum "ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum"
        ["ChaparralFuelLoadInputMode::DirectFuelLoad"
         "ChaparralFuelLoadInputMode::FuelLoadFromDepthAndChaparralType"]))

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

(def curring-level-units
  (enum "CuringLevelUnits_CuringLevelEnum"
        ["CuringLevelUnits::Fraction"
         "CuringLevelUnits::Percent"]))

(def crown-damage-equation-code
  (enum "CrownDamageEquationCode"
        ["CrownDamageEquationCode::not_set",
         "CrownDamageEquationCode::white_fir",
         "CrownDamageEquationCode::subalpine_fir",
         "CrownDamageEquationCode::incense_cedar",
         "CrownDamageEquationCode::western_larch",
         "CrownDamageEquationCode::whitebark_pine",
         "CrownDamageEquationCode::engelmann_spruce",
         "CrownDamageEquationCode::sugar_pine",
         "CrownDamageEquationCode::red_fir",
         "CrownDamageEquationCode::ponderosa_pine",
         "CrownDamageEquationCode::ponderosa_kill",
         "CrownDamageEquationCode::douglas_fir"]))

(def crown-damage-type
  (enum "CrownDamageType"
        ["CrownDamageType::not_set",
         "CrownDamageType::crown_length",
         "CrownDamageType::crown_volume",
         "CrownDamageType::crown_kill"]))

(def density-units
  (enum "DensityUnits_DensityUnitsEnum"
        ["DensityUnits::PoundsPerCubicFoot"
         "DensityUnits::KilogramsPerCubicMeter"]))

(def equation-type
  (enum "EquationType"
        ["EquationType::not_set",
         "EquationType::crown_scorch",
         "EquationType::bole_char",
         "EquationType::crown_damage"]))

(def fire-severity
  (enum "FireSeverity"
        ["FireSeverity::not_set"
         "FireSeverity::empty"
         "FireSeverity::low"]))

(def fireline-intensity-units
  (enum "FirelineIntensityUnits_FirelineIntensityUnitsEnum"
        ["FirelineIntensityUnits::BtusPerFootPerSecond"
         "FirelineIntensityUnits::BtusPerFootPerMinute"
         "FirelineIntensityUnits::KilojoulesPerMeterPerSecond"
         "FirelineIntensityUnits::KilojoulesPerMeterPerMinute"
         "FirelineIntensityUnits::KilowattsPerMeter"]))

(def fuel-constants
  (enum "FuelConstantsEnum_FuelConstantsEnum"
        ["FuelConstants::MaxLifeStates"
         "FuelConstants::MaxLiveSizeClasses"
         "FuelConstants::MaxDeadSizeClasses"
         "FuelConstants::MaxParticles"
         "FuelConstants::MaxSavrSizeClasses"
         "FuelConstants::MaxFuelModels"]))

(def fuel-life-state
  (enum "FuelLifeState_FuelLifeStateEnum"
        ["FuelLifeState::Dead"
         "FuelLifeState::Live"]))

(def fire-type
  (enum "FireType_FireTypeEnum"
        ["FireType::Surface"
         "FireType::Torching"
         "FireType::ConditionalCrownFire"
         "FireType::Crowning"]))

(def flame-length-or-scorch-height-switch
  (enum "FlameLengthOrScorchHeightSwitch"
        ["FlameLengthOrScorchHeightSwitch::flame_length"
         "FlameLengthOrScorchHeightSwitch::scorch_height"]))

(def fraction-units
  (enum "FractionUnits_FractionUnitsEnum"
        ["FractionUnits::Fraction"
         "FractionUnits::Percent"]))

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
         "LengthUnits::Millimeters"
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

(def moisture-class-input
  (enum "MoistureClassInput_MoistureClassInputEnum"
        ["MoistureClassInput::OneHour"
         "MoistureClassInput::TenHour"
         "MoistureClassInput::HundredHour"
         "MoistureClassInput::LiveHerbaceous"
         "MoistureClassInput::LiveWoody"
         "MoistureClassInput::DeadAggregate"
         "MoistureClassInput::LiveAggregate"]))

(def moisture-input-mode
  (enum "MoistureInputMode_MoistureInputModeEnum"
        ["MoistureInputMode::BySizeClass"
         "MoistureInputMode::AllAggregate"
         "MoistureInputMode::DeadAggregateAndLiveSizeClass"
         "MoistureInputMode::LiveAggregateAndDeadSizeClass"
         "MoistureInputMode::MoistureScenario"]))

(def moisture-units
  (enum "MoistureUnits_MoistureUnitsEnum"
        ["MoistureUnits::Fraction"
         "MoistureUnits::Percent"]))

(def mortality-rate-units
  (enum "MortalityRateUnits_MortalityRateUnitsEnum"
        ["MortalityRateUnits::Fraction"
         "MortalityRateUnits::Percent"]))

(def pressure-units
  (enum "PressureUnits_PressureUnitsEnum"
        ["PressureUnits::Pascal",
         "PressureUnits::HectoPascal",
         "PressureUnits::KiloPascal",
         "PressureUnits::MegaPascal",
         "PressureUnits::GigaPascal",
         "PressureUnits::Bar",
         "PressureUnits::Atmosphere",
         "PressureUnits::TechnicalAtmosphere",
         "PressureUnits::PoundPerSquareInch"]))

(def probability-units
  (enum "ProbabilityUnits_ProbabilityUnitsEnum"
        ["ProbabilityUnits::Fraction"
         "ProbabilityUnits::Percent"]))

(def required-field-names
  (enum "RequiredFieldNames"
        ["RequiredFieldNames::region"
         "RequiredFieldNames::flame_length_or_scorch_height_switch"
         "RequiredFieldNames::flame_length_or_scorch_height_value"
         "RequiredFieldNames::equation_type"
         "RequiredFieldNames::dbh"
         "RequiredFieldNames::tree_height"
         "RequiredFieldNames::crown_ratio"
         "RequiredFieldNames::crown_damage"
         "RequiredFieldNames::cambium_kill_rating"
         "RequiredFieldNames::beetle_damage"
         "RequiredFieldNames::bole_char_height"
         "RequiredFieldNames::bark_thickness"
         "RequiredFieldNames::fire_severity"
         "RequiredFieldNames::num_inputs"]))

(def gacc
  (enum "GACC"
        ["GACC::NotSet"
         "GACC::Alaska"
         "GACC::California"
         "GACC::EasternArea"
         "GACC::GreatBasin"
         "GACC::NorthernRockies"
         "GACC::Northwest"
         "GACC::RockeyMountain"
         "GACC::SouthernArea"
         "GACC::Southwest"]))

(def safety-condition
  (enum "SafetyCondition_SafetyConditionEnum"
        ["SafetyCondition::Low"
         "SafetyCondition::Moderate"
         "SafetyCondition::Extreme"]))

(def slope-class
  (enum "SlopeClass_SlopeClassEnum"
        ["SlopeClass::Flat"
         "SlopeClass::Moderate"
         "SlopeClass::Steep"]))

(def slope-units
  (enum "SlopeUnits_SlopeUnitsEnum"
        ["SlopeUnits::Degrees"
         "SlopeUnits::Percent"]))

(def speed-class
  (enum "SpeedClass_SpeedClassEnum" 
        ["SpeedClass::Light"
         "SpeedClass::Moderate"
         "SpeedClass::High"]))

(def speed-units
  (enum "SpeedUnits_SpeedUnitsEnum"
        ["SpeedUnits::FeetPerMinute"
         "SpeedUnits::ChainsPerHour"
         "SpeedUnits::MetersPerSecond"
         "SpeedUnits::MetersPerMinute"
         "SpeedUnits::MetersPerHour"
         "SpeedUnits::MilesPerHour"
         "SpeedUnits::KilometersPerHour"]))

(def spot-array-constants
  (enum "SpotArrayConstants_SpotArrayConstantsEnum"
        ["SpotArrayConstants::NUM_COLS"
         "SpotArrayConstants::NUM_FIREBRAND_ROWS"
         "SpotArrayConstants::NUM_SPECIES"]))

(def spot-down-wind-canopy-mode
  (enum "SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum"
        ["SpotDownWindCanopyMode::CLOSED"
         "SpotDownWindCanopyMode::OPEN"]))

(def spot-fire-location
  (enum "SpotFireLocation_SpotFireLocationEnum"
        ["SpotFireLocation::MIDSLOPE_WINDWARD"
         "SpotFireLocation::VALLEY_BOTTOM"
         "SpotFireLocation::MIDSLOPE_LEEWARD"
         "SpotFireLocation::RIDGE_TOP"]))

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

(def surface-area-to-volume-units
  (enum "SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum"
        ["SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet"
         "SurfaceAreaToVolumeUnits::SquareMetersOverCubicMeters"
         "SurfaceAreaToVolumeUnits::SquareInchesOverCubicInches"
         "SurfaceAreaToVolumeUnits::SquareCentimetersOverCubicCentimers"]))

(def surface-fire-spread-direction-mode
  (enum "SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum"
        ["SurfaceFireSpreadDirectionMode::FromIgnitionPoint"
         "SurfaceFireSpreadDirectionMode::FromPerimeter"]))

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

(def two-fuel-models-method
  (enum "TwoFuelModelsMethod_TwoFuelModelsMethodEnum"
        ["TwoFuelModelsMethod::NoMethod"
         "TwoFuelModelsMethod::Arithmetic"
         "TwoFuelModelsMethod::Harmonic"
         "TwoFuelModelsMethod::TwoDimensional"]))

(def wind-adjustment-factor-calculation-method
  (enum "WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum"
        ["WindAdjustmentFactorCalculationMethod::UserInput"
         "WindAdjustmentFactorCalculationMethod::UseCrownRatio"
         "WindAdjustmentFactorCalculationMethod::DontUseCrownRatio"]))

(def wind-adjustment-factor-shelter-method
  (enum "WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum"
        ["WindAdjustmentFactorShelterMethod::Unsheltered"
         "WindAdjustmentFactorShelterMethod::Sheltered"]))

(def wind-upslope-alignmentMode
  (enum "WindUpslopeAlignmentMode"
        ["WindUpslopeAlignmentMode::NotAligned"
         "WindUpslopeAlignmentMode::Aligned"]))

(def surface-run-in-direction-of
  (enum "SurfaceRunInDirectionOf"
        ["SurfaceRunInDirectionOf::MaxSpread"
         "SurfaceRunInDirectionOf::DirectionOfInterest"
         "SurfaceRunInDirectionOf::HeadingBackingFlanking"]))

(def crown-fire-calculation-method
  (enum "CrownFireCalculationMethod"
        ["CrownFireCalculationMethod::rothermel"
         "CrownFireCalculationMethod::scott_and_reinhardt"]))

(def wind-and-spread-orientation-mode
  (enum "WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum"
        ["WindAndSpreadOrientationMode::RelativeToUpslope"
         "WindAndSpreadOrientationMode::RelativeToNorth"]))

(def wind-height-input-mode
  (enum "WindHeightInputMode_WindHeightInputModeEnum"
        ["WindHeightInputMode::DirectMidflame"
         "WindHeightInputMode::TwentyFoot"
         "WindHeightInputMode::TenMeter"]))

(def fdfm-tool-aspect-index
  (enum "FDFMToolAspectIndex_AspectIndexEnum"
        ["FDFMToolAspectIndex::NORTH"
         "FDFMToolAspectIndex::EAST"
         "FDFMToolAspectIndex::SOUTH"
         "FDFMToolAspectIndex::WEST"]))

(def fdfm-tool-dry-bulb-index
  (enum "FDFMToolDryBulbIndex_DryBulbIndexEnum"
        ["FDFMToolDryBulbIndex::TEN_TO_TWENTY_NINE_DEGREES_F"
         "FDFMToolDryBulbIndex::THRITY_TO_FOURTY_NINE_DEGREES_F"
         "FDFMToolDryBulbIndex::FIFTY_TO_SIXTY_NINE_DEGREES_F"
         "FDFMToolDryBulbIndex::SEVENTY_TO_EIGHTY_NINE_DEGREES_F"
         "FDFMToolDryBulbIndex::NINETY_TO_ONE_HUNDRED_NINE_DEGREES_F"
         "FDFMToolDryBulbIndex::GREATER_THAN_ONE_HUNDRED_NINE_DEGREES_F"]))

(def fdfm-tool-elevation-index
  (enum "FDFMToolElevationIndex_ElevationIndexEnum"
        ["FDFMToolElevationIndex::BELOW_1000_TO_2000_FT"
         "FDFMToolElevationIndex::LEVEL_WITHIN_1000_FT"
         "FDFMToolElevationIndex::ABOVE_1000_TO_2000_FT"]))

(def fdfm-tool-month-index
  (enum "FDFMToolMonthIndex_MonthIndexEnum"
        ["FDFMToolMonthIndex::MAY_JUNE_JULY"
         "FDFMToolMonthIndex::FEB_MAR_APR_AUG_SEP_OCT"
         "FDFMToolMonthIndex::NOV_DEC_JAN"]))

(def fdfm-tool-rh-index
  (enum "FDFMToolRHIndex_RHIndexEnum"
        ["FDFMToolRHIndex::ZERO_TO_FOUR_PERCENT"
         "FDFMToolRHIndex::FIVE_TO_NINE_PERCENT"
         "FDFMToolRHIndex::TEN_TO_FOURTEEN_PERCENT"
         "FDFMToolRHIndex::FIFTEEN_TO_NINETEEN_PERCENT"
         "FDFMToolRHIndex::TWENTY_TO_TWENTY_FOUR_PERCENT"
         "FDFMToolRHIndex::TWENTY_FIVE_TO_TWENTY_NINE_PERCENT"
         "FDFMToolRHIndex::THIRTY_TO_THIRTY_FOUR_PERCENT"
         "FDFMToolRHIndex::THIRTY_FIVE_TO_THIRTY_NINE_PERCENT"
         "FDFMToolRHIndex::FORTY_TO_FORTY_FOUR_PERCENT"
         "FDFMToolRHIndex::FORTY_FIVE_TO_FORTY_NINE_PERCENT"
         "FDFMToolRHIndex::FIFTY_TO_FIFTY_FOUR_PERCENT"
         "FDFMToolRHIndex::FIFTY_FIVE_TO_FIFTY_NINE_PERCENT"
         "FDFMToolRHIndex::SIXTY_TO_SIXTY_FOUR_PERCENT"
         "FDFMToolRHIndex::SIXTY_FIVE_TO_SIXTY_NINE_PERCENT"
         "FDFMToolRHIndex::SEVENTY_TO_SEVENTY_FOUR_PERCENT"
         "FDFMToolRHIndex::SEVENTY_FIVE_TO_SEVENTY_NINE_PERCENT"
         "FDFMToolRHIndex::EIGHTY_TO_EIGHTY_FOUR_PERCENT"
         "FDFMToolRHIndex::EIGHTY_FIVE_TO_EIGHTY_NINE_PERCENT"
         "FDFMToolRHIndex::NINETY_TO_NINETY_FOUR_PERCENT"
         "FDFMToolRHIndex::NINETY_FIVE_TO_NINETY_NINE_PERCENT"
         "FDFMToolRHIndex::ONE_HUNDRED_PERCENT"]))

(def fdfm-tool-shading-index
  (enum "FDFMToolShadingIndex_ShadingIndexEnum"
        ["FDFMToolShadingIndex::EXPOSED"
         "FDFMToolShadingIndex::SHADED"]))

(def fdfm-tool-slope-index
  (enum "FDFMToolSlopeIndex_SlopeIndexEnum"
        ["FDFMToolSlopeIndex::ZERO_TO_THIRTY_PERCENT"
         "FDFMToolSlopeIndex::GREATER_THAN_OR_EQUAL_TO_THIRTY_ONE_PERCENT"]))

(def fdfm-tool-time-of-day
  (enum "FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum"
        ["FDFMToolTimeOfDayIndex::EIGHT_HUNDRED_HOURS_TO_NINE_HUNDRED_FIFTY_NINE"
         "FDFMToolTimeOfDayIndex::TEN_HUNDRED_HOURS_TO_ELEVEN__HUNDRED_FIFTY_NINE"
         "FDFMToolTimeOfDayIndex::TWELVE_HUNDRED_HOURS_TO_THIRTEEN_HUNDRED_FIFTY_NINE"
         "FDFMToolTimeOfDayIndex::FOURTEEN_HUNDRED_HOURS_TO_FIFTEEN_HUNDRED_FIFTY_NINE"
         "FDFMToolTimeOfDayIndex::SIXTEEN_HUNDRED_HOURS_TO_SIXTEEN_HUNDRED_FIFTY_NINE"
         "FDFMToolTimeOfDayIndex::EIGHTTEEN_HUNDRED_HOURS_TO_SUNSET"]))
