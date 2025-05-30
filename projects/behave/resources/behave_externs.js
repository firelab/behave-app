/**********************************************************************
 * Extern for behave.js
 * Everything was added manually by RJ.
 **********************************************************************/
const Module = {};

Module.SIGContainAdapter = class {
  constructor() {}
  doContainRun() {}
  addResource() {}
  setAttackDistance() {}
  setFireStartTime() {}
  setLwRatio() {}
  setMaxFireSize() {}
  setMaxFireTime() {}
  setMaxSteps() {}
  setMinSteps() {}
  setReportRate() {}
  setReportSize() {}
  setRetry() {}
  setTactic() {}
  removeAllResources() {}
  removeResourceWithThisDesc() {}
  removeResourceAt() {}
  removeAllResourcesWithThisDesc() {}
  getContainmentStatus() {}
  getFinalContainmentArea() {}
  getFinalCost() {}
  getFinalFireLineLength() {}
  getFinalFireSize() {}
  getFinalTimeSinceReport() {}
  getFireSizeAtInitialAttack() {}
  getPerimeterAtContainment() {}
  getPerimeterAtInitialAttack() {}
  getResourcesUsed() {}
  getFirePerimeterX(){}
  getFirePerimeterY(){}
  getFirePerimeterPointCount(){}
  getFireBackAtReport(){}
  getFireHeadAtReport(){}
  getFireHeadAtAttack(){}
  getFireBackAtAttack(){}
  getLengthToWidthRatio(){}
  getAttackDistance(){}
  getReportSize(){}
  getReportRate(){}
  getTactic(){}
};

Module.SIGSurface = class {
  calculateFlameLength() {}
  constructor() {}
  doSurfaceRun() {}
  doSurfaceRunInDirectionOfInterest() {}
  doSurfaceRunInDirectionOfMaxSpread() {}
  getAgeOfRough() {}
  getAspect() {}
  getAspenCuringLevel() {}
  getAspenDBH() {}
  getAspenFireSeverity() {}
  getAspenFuelModelNumber() {}
  getAspenLoadDeadOneHour() {}
  getAspenLoadDeadTenHour() {}
  getAspenLoadLiveHerbaceous() {}
  getAspenLoadLiveWoody() {}
  getAspenSavrDeadOneHour() {}
  getAspenSavrDeadTenHour() {}
  getAspenSavrLiveHerbaceous() {}
  getAspenSavrLiveWoody() {}
  getBackingFirelineIntensity() {}
  getBackingFlameLength() {}
  getBackingSpreadDistance() {}
  getBackingSpreadRate() {}
  getBulkDensity() {}
  getCanopyCover() {}
  getCanopyHeight() {}
  getChaparralAge() {}
  getChaparralDaysSinceMayFirst() {}
  getChaparralDeadFuelFraction() {}
  getChaparralDeadMoistureOfExtinction() {}
  getChaparralDensity() {}
  getChaparralFuelBedDepth() {}
  getChaparralFuelDeadLoadFraction() {}
  getChaparralFuelType() {}
  getChaparralHeatOfCombustion() {}
  getChaparralLiveMoistureOfExtinction() {}
  getChaparralLoadDeadHalfInchToLessThanOneInch() {}
  getChaparralLoadDeadLessThanQuarterInch() {}
  getChaparralLoadDeadOneInchToThreeInch() {}
  getChaparralLoadDeadQuarterInchToLessThanHalfInch() {}
  getChaparralLoadLiveHalfInchToLessThanOneInch() {}
  getChaparralLoadLiveLeaves() {}
  getChaparralLoadLiveOneInchToThreeInch() {}
  getChaparralLoadLiveQuarterInchToLessThanHalfInch() {}
  getChaparralLoadLiveStemsLessThanQuaterInch() {}
  getChaparralMoisture() {}
  getChaparralTotalDeadFuelLoad() {}
  getChaparralTotalFuelLoad() {}
  getChaparralTotalLiveFuelLoad() {}
  getCharacteristicMoistureByLifeState() {}
  getCharacteristicMoistureDead() {}
  getCharacteristicMoistureLive() {}
  getCharacteristicSAVR() {}
  getCrownRatio() {}
  getDirectionOfBacking() {}
  getDirectionOfFlanking() {}
  getDirectionOfInterest() {}
  getFlameLengthInDirectionOfInterest() {}
  getFirelineIntensityInDirectionOfInterest() {}
  getDirectionOfMaxSpread() {}
  getElapsedTime() {}
  getEllipticalA() {}
  getEllipticalB() {}
  getEllipticalC() {}
  getFireArea() {}
  getFireEccentricity() {}
  getFireLength() {}
  getFireLengthToWidthRatio() {}
  getFirePerimeter() {}
  getFirelineIntensity() {}
  getFlameLength() {}
  getFlankingFirelineIntensity() {}
  getFlankingFlameLength() {}
  getFlankingSpreadDistance() {}
  getFlankingSpreadRate() {}
  getFuelCode() {}
  getFuelHeatOfCombustionDead() {}
  getFuelHeatOfCombustionLive() {}
  getFuelLoadHundredHour() {}
  getFuelLoadLiveHerbaceous() {}
  getFuelLoadLiveWoody() {}
  getFuelLoadOneHour() {}
  getFuelLoadTenHour() {}
  getFuelModelNumber() {}
  getFuelMoistureOfExtinctionDead() {}
  getFuelName() {}
  getFuelSavrLiveHerbaceous() {}
  getFuelSavrLiveWoody() {}
  getFuelSavrOneHour() {}
  getFuelbedDepth() {}
  getHeadingSpreadRate() {}
  getHeadingToBackingRatio() {}
  getHeatPerUnitArea() {}
  getHeatSink() {}
  getHeatSource() {}
  getHeightOfUnderstory() {}
  getIsMoistureScenarioDefinedByIndex() {}
  getIsMoistureScenarioDefinedByName() {}
  getIsUsingChaparral() {}
  getIsUsingPalmettoGallberry() {}
  getIsUsingWesternAspen() {}
  getLiveFuelMoistureOfExtinction() {}
  getMaxFireWidth() {}
  getMidflameWindspeed() {}
  getMoistureDeadAggregateValue() {}
  getMoistureHundredHour() {}
  getMoistureInputMode() {}
  getMoistureLiveAggregateValue() {}
  getMoistureLiveHerbaceous() {}
  getMoistureLiveWoody() {}
  getMoistureOneHour() {}
  getMoistureScenarioDescriptionByIndex() {}
  getMoistureScenarioDescriptionByName() {}
  getMoistureScenarioHundredHourByIndex() {}
  getMoistureScenarioHundredHourByName() {}
  getMoistureScenarioIndexByName() {}
  getMoistureScenarioLiveHerbaceousByIndex() {}
  getMoistureScenarioLiveHerbaceousByName() {}
  getMoistureScenarioLiveWoodyByIndex() {}
  getMoistureScenarioLiveWoodyByName() {}
  getMoistureScenarioNameByIndex() {}
  getMoistureScenarioOneHourByIndex() {}
  getMoistureScenarioOneHourByName() {}
  getMoistureScenarioTenHourByIndex() {}
  getMoistureScenarioTenHourByName() {}
  getMoistureTenHour() {}
  getNumberOfMoistureScenarios() {}
  getOverstoryBasalArea() {}
  getPalmettoGallberryCoverage() {}
  getPalmettoGallberryHeatOfCombustionDead() {}
  getPalmettoGallberryHeatOfCombustionLive() {}
  getPalmettoGallberryMoistureOfExtinctionDead() {}
  getPalmettoGallberyDeadFineFuelLoad() {}
  getPalmettoGallberyDeadFoliageLoad() {}
  getPalmettoGallberyDeadMediumFuelLoad() {}
  getPalmettoGallberyFuelBedDepth() {}
  getPalmettoGallberyLitterLoad() {}
  getPalmettoGallberyLiveFineFuelLoad() {}
  getPalmettoGallberyLiveFoliageLoad() {}
  getPalmettoGallberyLiveMediumFuelLoad() {}
  getReactionIntensity() {}
  getResidenceTime() {}
  getSlope() {}
  getSlopeFactor() {}
  getSpreadDistance() {}
  getSpreadDistanceInDirectionOfInterest() {}
  getSpreadRate() {}
  getSpreadRateInDirectionOfInterest() {}
  getSurfaceFireReactionIntensityForLifeState() {}
  getSurfaceRunInDirectionOf() {}
  getTotalLiveFuelLoad() {}
  getTotalDeadFuelLoad() {}
  getTotalDeadHerbaceousFuelLoad() {}
  getWindAdjustmentFactorCalculationMethod() {}
  getWindAndSpreadOrientationMode() {}
  getWindDirection() {}
  getWindHeightInputMode() {}
  getWindSpeed() {}
  getWindUpslopeAlignmentMode() {}
  initializeMembers() {}
  isAllFuelLoadZero() {}
  isFuelDynamic() {}
  isFuelModelDefined() {}
  isFuelModelReserved() {}
  isMoistureClassInputNeededForCurrentFuelModel() {}
  isUsingTwoFuelModels() {}
  setAgeOfRough() {}
  setAspect() {}
  setAspenCuringLevel() {}
  setAspenDBH() {}
  setAspenFireSeverity() {}
  setAspenFuelModelNumber() {}
  setCanopyCover() {}
  setCanopyHeight() {}
  setChaparralFuelBedDepth() {}
  setChaparralFuelDeadLoadFraction() {}
  setChaparralFuelLoadInputMode() {}
  setChaparralFuelType() {}
  setChaparralTotalFuelLoad() {}
  setCrownRatio() {}
  setCurrentMoistureScenarioByIndex() {}
  setCurrentMoistureScenarioByName() {}
  setDirectionOfInterest() {}
  setElapsedTime() {}
  setFirstFuelModelNumber() {}
  setFuelModelNumber() {}
  setFuelModels() {}
  setHeightOfUnderstory() {}
  setIsUsingChaparral() {}
  setIsUsingPalmettoGallberry() {}
  setIsUsingWesternAspen() {}
  setMoistureDeadAggregate() {}
  setMoistureHundredHour() {}
  setMoistureInputMode() {}
  setMoistureLiveAggregate() {}
  setMoistureLiveHerbaceous() {}
  setMoistureLiveWoody() {}
  setMoistureOneHour() {}
  setMoistureScenarios() {}
  setMoistureTenHour() {}
  setOverstoryBasalArea() {}
  setPalmettoCoverage() {}
  setSecondFuelModelNumber() {}
  setSlope() {}
  setSurfaceFireSpreadDirectionMode() {}
  setSurfaceRunInDirectionOf() {}
  setTwoFuelModelsFirstFuelModelCoverage() {}
  setTwoFuelModelsMethod() {}
  setUserProvidedWindAdjustmentFactor() {}
  setWindAdjustmentFactorCalculationMethod() {}
  setWindAndSpreadOrientationMode() {}
  setWindDirection() {}
  setWindHeightInputMode() {}
  setWindSpeed() {}
  setWindUpslopeAlignmentMode() {}
  updateSurfaceInputs() {}
  updateSurfaceInputsForPalmettoGallbery() {}
  updateSurfaceInputsForTwoFuelModels() {}
  updateSurfaceInputsForWesternAspen() {}
};

Module.SIGCrown = class {
  constructor() {}
  getFireType() {}
  getIsMoistureScenarioDefinedByIndex() {}
  getIsMoistureScenarioDefinedByName() {}
  isAllFuelLoadZero() {}
  isFuelDynamic() {}
  isFuelModelDefined() {}
  isFuelModelReserved() {}
  setCurrentMoistureScenarioByIndex() {}
  setCurrentMoistureScenarioByName() {}
  getAspect() {}
  getCanopyBaseHeight() {}
  getCanopyBulkDensity() {}
  getCanopyCover() {}
  getCanopyHeight() {}
  getCriticalOpenWindSpeed() {}
  getCrownCriticalFireSpreadRate() {}
  getCrownCriticalSurfaceFirelineIntensity() {}
  getCrownCriticalSurfaceFlameLength() {}
  getCrownFireActiveRatio() {}
  getCrownFireArea() {}
  getCrownFirePerimeter() {}
  getCrownTransitionRatio() {}
  getCrownFireLengthToWidthRatio() {}
  getCrownFireSpreadDistance() {}
  getCrownFireSpreadRate() {}
  getCrownFirelineIntensity() {}
  getCrownFlameLength() {}
  getCrownFractionBurned() {}
  getCrownRatio() {}
  getFinalFireArea() {}
  getFinalFirePerimeter() {}
  getFinalFirelineIntesity() {}
  getFinalHeatPerUnitArea() {}
  getFinalSpreadDistance() {}
  getFinalSpreadRate() {}
  getFuelHeatOfCombustionDead() {}
  getFuelHeatOfCombustionLive() {}
  getFuelLoadHundredHour() {}
  getFuelLoadLiveHerbaceous() {}
  getFuelLoadLiveWoody() {}
  getFuelLoadOneHour() {}
  getFuelLoadTenHour() {}
  getFuelMoistureOfExtinctionDead() {}
  getFuelSavrLiveHerbaceous() {}
  getFuelSavrLiveWoody() {}
  getFuelSavrOneHour() {}
  getFuelbedDepth() {}
  getMoistureFoliar() {}
  getMoistureHundredHour() {}
  getMoistureLiveHerbaceous() {}
  getMoistureLiveWoody() {}
  getMoistureOneHour() {}
  getMoistureScenarioHundredHourByIndex() {}
  getMoistureScenarioHundredHourByName() {}
  getMoistureScenarioLiveHerbaceousByIndex() {}
  getMoistureScenarioLiveHerbaceousByName() {}
  getMoistureScenarioLiveWoodyByIndex() {}
  getMoistureScenarioLiveWoodyByName() {}
  getMoistureScenarioOneHourByIndex() {}
  getMoistureScenarioOneHourByName() {}
  getMoistureScenarioTenHourByIndex() {}
  getMoistureScenarioTenHourByName() {}
  getMoistureTenHour() {}
  getSlope() {}
  getSurfaceFireSpreadDistance() {}
  getSurfaceFireSpreadRate() {}
  getWindDirection() {}
  getWindSpeed() {}
  getFuelModelNumber() {}
  getMoistureScenarioIndexByName() {}
  getNumberOfMoistureScenarios() {}
  getFuelCode() {}
  getFuelName() {}
  getMoistureScenarioDescriptionByIndex() {}
  getMoistureScenarioDescriptionByName() {}
  getMoistureScenarioNameByIndex() {}
  doCrownRunRothermel() {}
  doCrownRunScottAndReinhardt() {}
  doCrownRun() {}
  initializeMembers() {}
  setAspect() {}
  setCanopyBaseHeight() {}
  setCanopyBulkDensity() {}
  setCanopyCover() {}
  setCanopyHeight() {}
  setCrownRatio() {}
  setFuelModelNumber() {}
  setCrownFireCalculationMethod() {}
  setElapsedTime() {}
  setFuelModels() {}
  setMoistureDeadAggregate() {}
  setMoistureFoliar() {}
  setMoistureHundredHour() {}
  setMoistureInputMode() {}
  setMoistureLiveAggregate() {}
  setMoistureLiveHerbaceous() {}
  setMoistureLiveWoody() {}
  setMoistureOneHour() {}
  setMoistureScenarios() {}
  setMoistureTenHour() {}
  setSlope() {}
  setUserProvidedWindAdjustmentFactor() {}
  setWindAdjustmentFactorCalculationMethod() {}
  setWindAndSpreadOrientationMode() {}
  setWindDirection() {}
  setWindHeightInputMode() {}
  setWindSpeed() {}
  updateCrownInputs() {}
  updateCrownsSurfaceInputs() {}
  getFinalFlameLength() {}
};

Module.SIGIgnite = class {
    init() {}
    calculateFirebrandIgnitionProbability() {}
    getFirebrandIgnitionProbability() {}
    setAirTemperature() {}
    getMoistureHundredHour() {}
    getLightningChargeType() {}
    setMoistureOneHour() {}
    calculateLightningIgnitionProbability() {}
    getDuffDepth() {}
    setIgnitionFuelBedType() {}
    setSunShade() {}
    getAirTemperature() {}
    getFuelTemperature() {}
    isFuelDepthNeeded() {}
    setMoistureHundredHour() {}
    getSunShade() {}
    initializeMembers() {}
    getFuelBedType() {}
    getMoistureOneHour() {}
    setLightningChargeType() {}
    setDuffDepth() {}
    updateIgniteInputs() {}
};

Module.SIGMortality = class {
  constructor() {}
  calculateMortality() {}
  calculateMortalityAllDirections() {}
  calculateScorchHeight() {}
  checkIsInGACCRegionAtSpeciesTableIndex() {}
  checkIsInGACCRegionFromSpeciesCode() {}
  getBarkEquationNumberAtSpeciesTableIndex() {}
  getBarkEquationNumberFromSpeciesCode() {}
  getBarkThickness() {}
  getBasalAreaKillled() {}
  getBasalAreaPostfire() {}
  getBasalAreaPrefire() {}
  getBeetleDamage() {}
  getBoleCharHeight() {}
  getBoleCharHeightBacking() {}
  getBoleCharHeightFlanking() {}
  getCalculatedScorchHeight() {}
  getCambiumKillRating() {}
  getCommonNameAtSpeciesTableIndex() {}
  getCommonNameFromSpeciesCode() {}
  getCrownCoefficientCodeAtSpeciesTableIndex() {}
  getCrownCoefficientCodeFromSpeciesCode() {}
  getCrownDamage() {}
  getCrownDamageEquationCode() {}
  getCrownDamageEquationCodeAtSpeciesTableIndex() {}
  getCrownDamageEquationCodeFromSpeciesCode() {}
  getCrownDamageType() {}
  getCrownRatio() {}
  getCrownScorchOrBoleCharEquationNumber() {}
  getCVSorCLS() {}
  getDBH() {}
  getEquationType() {}
  getEquationTypeAtSpeciesTableIndex() {}
  getEquationTypeFromSpeciesCode() {}
  getFireSeverity() {}
  getFlameLength() {}
  getFlameLengthOrScorchHeightSwitch() {}
  getFlameLengthOrScorchHeightValue() {}
  getKilledTrees() {}
  getMortalityEquationNumberAtSpeciesTableIndex() {}
  getMortalityEquationNumberFromSpeciesCode() {}
  getNumberOfRecordsInSpeciesTable() {}
  getProbabilityOfMortality() {}
  getProbabilityOfMortalityBacking() {}
  getProbabilityOfMortalityFlanking() {}
  getGACCRegion() {}
  getRequiredFieldVector() {}
  getScientificNameAtSpeciesTableIndex() {}
  getScientificNameFromSpeciesCode() {}
  getScorchHeight(){}
  getScorchHeightBacking(){}
  getScorchHeightFlanking(){}
  getSpeciesCode() {}
  getSpeciesCodeAtSpeciesTableIndex() {}
  getSpeciesRecordAtIndex() {}
  getSpeciesRecordBySpeciesCodeAndEquationType() {}
  getSpeciesRecordVectorForGACCRegion() {}
  getSpeciesRecordVectorForGACCRegionAndEquationType() {}
  getSpeciesTableIndexFromSpeciesCode() {}
  getSpeciesTableIndexFromSpeciesCodeAndEquationType() {}
  getTotalPrefireTrees() {}
  getTreeCrownLengthScorched() {}
  getTreeCrownLengthScorchedBacking() {}
  getTreeCrownLengthScorchedFlanking() {}
  getTreeCrownVolumeScorched() {}
  getTreeCrownVolumeScorchedBacking() {}
  getTreeCrownVolumeScorchedFlanking() {}
  getTreeDensityPerUnitArea() {}
  getTreeHeight() {}
  initializeMembers() {}
  postfireCanopyCover() {}
  prefireCanopyCover() {}
  setAirTemperature() {}
  setBeetleDamage() {}
  setBoleCharHeight() {}
  setCambiumKillRating() {}
  setCrownDamage() {}
  setCrownRatio() {}
  setDBH() {}
  setEquationType() {}
  setFireSeverity() {}
  setFirelineIntensity() {}
  setFlameLength() {}
  setFlameLengthOrScorchHeightSwitch() {}
  setFlameLengthOrScorchHeightValue() {}
  setMidFlameWindSpeed() {}
  setGACCRegion() {}
  setScorchHeight() {}
  setSpeciesCode() {}
  setSurfaceFireFirelineIntensity() {}
  setSurfaceFireFirelineIntensityBacking() {}
  setSurfaceFireFirelineIntensityFlanking() {}
  setSurfaceFireFlameLength() {}
  setSurfaceFireFlameLengthBacking() {}
  setSurfaceFireFlameLengthFlanking() {}
  setSurfaceFireScorchHeight() {}
  setTreeDensityPerUnitArea() {}
  setTreeHeight() {}
  setUserProvidedWindAdjustmentFactor() {}
  setWindHeightInputMode() {}
  setWindSpeed() {}
  setWindSpeedAndWindHeightInputMode() {}
  updateInputsForSpeciesCodeAndEquationType() {}
};

Module.SIGSpot = class {
  SIGSpot() {}
  getDownwindCanopyMode() {}
  getLocation() {}
  getTreeSpecies() {}
  getBurningPileFlameHeight() {}
  getCoverHeightUsedForBurningPile() {}
  getCoverHeightUsedForSurfaceFire() {}
  getCoverHeightUsedForTorchingTrees() {}
  getDBH() {}
  getDownwindCoverHeight() {}
  getFlameDurationForTorchingTrees() {}
  getFlameHeightForTorchingTrees() {}
  getFlameRatioForTorchingTrees() {}
  getMaxFirebrandHeightFromBurningPile() {}
  getMaxFirebrandHeightFromSurfaceFire() {}
  getMaxFirebrandHeightFromTorchingTrees() {}
  getMaxFlatTerrainSpottingDistanceFromBurningPile() {}
  getMaxFlatTerrainSpottingDistanceFromSurfaceFire() {}
  getMaxFlatTerrainSpottingDistanceFromTorchingTrees() {}
  getMaxMountainousTerrainSpottingDistanceFromBurningPile() {}
  getMaxMountainousTerrainSpottingDistanceFromSurfaceFire() {}
  getMaxMountainousTerrainSpottingDistanceFromTorchingTrees() {}
  getMaxMountainousTerrainSpottingDistanceFromActiveCrown() {}
  getRidgeToValleyDistance() {}
  getRidgeToValleyElevation() {}
  getSurfaceFlameLength() {}
  getTreeHeight() {}
  getWindSpeedAtTwentyFeet() {}
  getTorchingTrees() {}
  calculateAll() {}
  calculateSpottingDistanceFromBurningPile() {}
  calculateSpottingDistanceFromSurfaceFire() {}
  calculateSpottingDistanceFromTorchingTrees() {}
  initializeMembers() {}
  setActiveCrownFlameLength() {}
  setBurningPileFlameHeight() {}
  setDBH() {}
  setDownwindCanopyMode() {}
  setDownwindCoverHeight() {}
  setFireType () {}
  setFlameLength() {}
  setFirelineIntensity() {}
  setLocation() {}
  setRidgeToValleyDistance() {}
  setRidgeToValleyElevation() {}
  setTorchingTrees() {}
  setTreeHeight() {}
  setTreeSpecies() {}
  setWindHeightInputMode() {}
  setWindSpeed() {}
  setWindSpeedAndWindHeightInputMode() {}
  setWindSpeedAtTwentyFeet() {}
  updateSpotInputsForBurningPile() {}
  updateSpotInputsForSurfaceFire() {}
  updateSpotInputsForTorchingTrees() {}
};

Module.SIGFineDeadFuelMoistureTool = class {
    init() {}
    calculate() {}
    calculateByIndex() {}
    getAspectIndexSize() {}
    getAspectLabelAtIndex() {}
    getCorrectionMoisture() {}
    getDryBulbTemperatureIndexSize() {}
    getDryBulbTemperatureLabelAtIndex() {}
    getElevationIndexSize() {}
    getElevationLabelAtIndex() {}
    getFineDeadFuelMoisture() {}
    getMonthIndexSize() {}
    getMonthLabelAtIndex() {}
    getReferenceMoisture() {}
    getRelativeHumidityIndexSize() {}
    getRelativeHumidityLabelAtIndex() {}
    getShadingIndexSize() {}
    getShadingLabelAtIndex() {}
    getSlopeIndexSize() {}
    getSlopeLabelAtIndex() {}
    getTimeOfDayIndexSize() {}
    getTimeOfDayLabelAtIndex() {}
    operator() {}
    setAspectIndex() {}
    setDryBulbIndex() {}
    setElevationIndex() {}
    setMonthIndex() {}
    setRHIndex() {}
    setShadingIndex() {}
    setSlopeIndex() {}
    setTimeOfDayIndex() {}
};

Module.SIGFuelModels = class {
  constructor() {}
  SIGFuelModels() {}
  clearCustomFuelModel() {}
  getIsDynamic() {}
  isAllFuelLoadZero() {}
  isFuelModelDefined() {}
  isFuelModelReserved() {}
  setCustomFuelModel() {}
  getFuelCode() {}
  getFuelName() {}
  getFuelLoadHundredHour() {}
  getFuelLoadLiveHerbaceous() {}
  getFuelLoadLiveWoody() {}
  getFuelLoadOneHour() {}
  getFuelLoadTenHour() {}
  getFuelbedDepth() {}
  getHeatOfCombustionDead() {}
  getMoistureOfExtinctionDead() {}
  getSavrLiveHerbaceous() {}
  getSavrLiveWoody() {}
  getSavrOneHour() {}
  getHeatOfCombustionLive() {}
};

Module.SpeciesMasterTable = class {
  constructor() {}
  SpeciesMasterTable() {}
  initializeMasterTable() {}
  getSpeciesTableIndexFromSpeciesCode() {}
  getSpeciesTableIndexFromSpeciesCodeAndEquationType() {}
  insertRecord() {}
};

Module.SIGSlopeTool = class {
    init() {}
    calculateHorizontalDistance() {}
    calculateSlopeFromMapMeasurements() {}
    getCentimetersPerKilometerAtIndex() {}
    getCentimetersPerKilometerAtRepresentativeFraction() {}
    getHorizontalDistance() {}
    getHorizontalDistanceAtIndex() {}
    getHorizontalDistanceFifteen() {}
    getHorizontalDistanceFourtyFive() {}
    getHorizontalDistanceMaxSlope() {}
    getHorizontalDistanceNinety() {}
    getHorizontalDistanceSeventy() {}
    getHorizontalDistanceSixty() {}
    getHorizontalDistanceThirty() {}
    getHorizontalDistanceZero() {}
    getInchesPerMileAtIndex() {}
    getInchesPerMileAtRepresentativeFraction() {}
    getKilometersPerCentimeterAtIndex() {}
    getKilometersPerCentimeterAtRepresentativeFraction() {}
    getMilesPerInchAtIndex() {}
    getMilesPerInchAtRepresentativeFraction() {}
    getNumberOfHorizontalDistances() {}
    getNumberOfRepresentativeFractions() {}
    getRepresentativeFractionAtIndex() {}
    getRepresentativeFractionAtRepresentativeFraction() {}
    getSlopeElevationChangeFromMapMeasurements() {}
    getSlopeFromMapMeasurements() {}
    getSlopeFromMapMeasurementsInDegrees() {}
    getSlopeFromMapMeasurementsInPercent() {}
    getSlopeHorizontalDistanceFromMapMeasurements() {}
    setCalculatedMapDistance() {}
    setContourInterval() {}
    setMapDistance() {}
    setMapRepresentativeFraction() {}
    setMaxSlopeSteepness() {}
    setNumberOfContours() {}
};

Module.SIGMoistureScenarios = class {
  constructor() {}
  getIsMoistureScenarioDefinedByIndex() {}
  getIsMoistureScenarioDefinedByName() {}
  getMoistureScenarioDescriptionByIndex() {}
  getMoistureScenarioDescriptionByName() {}
  getMoistureScenarioHundredHourByIndex() {}
  getMoistureScenarioHundredHourByName() {}
  getMoistureScenarioIndexByName() {}
  getMoistureScenarioLiveHerbaceousByIndex() {}
  getMoistureScenarioLiveHerbaceousByName() {}
  getMoistureScenarioLiveWoodyByIndex() {}
  getMoistureScenarioLiveWoodyByName() {}
  getMoistureScenarioNameByIndex() {}
  getMoistureScenarioOneHourByIndex() {}
  getMoistureScenarioOneHourByName() {}
  getMoistureScenarioTenHourByIndex() {}
  getMoistureScenarioTenHourByName() {}
  getNumberOfMoistureScenarios() {}
};

Module.VaporPressureDeficitCalculator = class {
    init() {}
    runCalculation() {}
    setTemperature() {}
    setRelativeHumidity() {}
    getVaporPressureDeficit() {}
};

// Units

Module.AreaUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.BasalAreaUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.FractionUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.LengthUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.LoadingUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.SurfaceAreaToVolumeUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.SpeedUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.PressureUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.SlopeUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.DensityUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};


Module.HeatOfCombustionUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.HeatSinkUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.HeatPerUnitAreaUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.HeatSourceAndReactionIntensityUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.FirelineIntensityUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.TemperatureUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};

Module.TimeUnits = class {
  toBaseUnits() {}
  fromBaseUnits() {}
};
