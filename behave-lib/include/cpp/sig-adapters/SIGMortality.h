//{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}
// Name: SIGMortality.h
// Desc: Main interface for Morality CodeBlock
// Author: Richard J. Sheperd, Spatial Informatics Group
//*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}
#pragma once

#include <string>
#include <vector>

#include "SIGCollections.h"
#include "SIGString.h"
#include "canopy_coefficient_table.h"
#include "mortality.h"
#include "mortality_inputs.h"
#include "species_master_table.h"
#include "surfaceInputEnums.h"

class SIGMortality : public Mortality {
public:
  SIGMortality() = delete; // There is no default constructor
  explicit SIGMortality(SpeciesMasterTable &speciesMasterTable);
  SIGMortality(const SIGMortality &rhs);

  // Init Methods
  void initializeMembers();

  // SIGMortality Setters. Sets for all Directions
  void setSpeciesCode(char *speciesCode);
  void setTreeHeight(double treeHeight, LengthUnits::LengthUnitsEnum treeHeightUnits);
  void setCrownRatio(double crownRatio, FractionUnits::FractionUnitsEnum crownRatioUnits);
  void setDBH(double dbh, LengthUnits::LengthUnitsEnum diameterUnits);
  void setBoleCharHeight(double boleCharHeight, LengthUnits::LengthUnitsEnum boleCharHeightUnits);
  void setEquationType(EquationType equationType);
  void setAirTemperature(double airTemperature, TemperatureUnits::TemperatureUnitsEnum temperatureUnits);
  void setMidFlameWindSpeed(double midFlameWindSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits);
  void setUserProvidedWindAdjustmentFactor(double userProvidedWindAdjustmentFactor);
  void setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode);
  void setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits);
  void setWindSpeedAndWindHeightInputMode(double windwindSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode, double userProvidedWindAdjustmentFactor);

  // SIGMortality Setters Heading Direction
  void setSurfaceFireFlameLength(double value, LengthUnits::LengthUnitsEnum lengthUnits);
  void setSurfaceFireFirelineIntensity(double value, FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits);
  void setSurfaceFireScorchHeight(double value, LengthUnits::LengthUnitsEnum lengthUnits);
  bool updateInputsForSpeciesCodeAndEquationType(char *speciesCode, EquationType equationType);

  // SIGMortality Setters Backing Direction
  void setSurfaceFireFlameLengthBacking(double value, LengthUnits::LengthUnitsEnum lengthUnits);
  void setSurfaceFireFirelineIntensityBacking(double value, FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits);

  // SIGMortality Setters Flanking Direction
  void setSurfaceFireFlameLengthFlanking(double value, LengthUnits::LengthUnitsEnum lengthUnits);
  void setSurfaceFireFirelineIntensityFlanking(double value, FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits);

  // SIGMortality Getters
  char *getSpeciesCode() const;
  char *getSpeciesCodeAtSpeciesTableIndex(int index) const;
  char *getScientificNameAtSpeciesTableIndex(int index) const;
  char *getCommonNameAtSpeciesTableIndex(int index) const;
  char *getScientificNameFromSpeciesCode(char *speciesCode) const;
  char *getCommonNameFromSpeciesCode(char *speciesCode) const;
  int getMortalityEquationNumberFromSpeciesCode(char *speciesCode) const;
  int getBarkEquationNumberFromSpeciesCode(char *speciesCode) const;
  int getCrownCoefficientCodeFromSpeciesCode(char *speciesCode) const;
  EquationType getEquationTypeFromSpeciesCode(char *speciesCode) const;
  EquationType getEquationType() const;
  CrownDamageEquationCode getCrownDamageEquationCodeFromSpeciesCode(char *speciesCode) const;
  int getSpeciesTableIndexFromSpeciesCode(char *speciesNameCode) const;
  int getSpeciesTableIndexFromSpeciesCodeAndEquationType(char *speciesNameCode, EquationType equationType) const;
  SpeciesMasterTableRecord getSpeciesRecordBySpeciesCodeAndEquationType(char *speciesCode,
                                                                        EquationType equationType) const;
  BoolVector *getRequiredFieldVector();
  SpeciesMasterTableRecordVector *getSpeciesRecordVectorForGACCRegion(GACC region);
  SpeciesMasterTableRecordVector *getSpeciesRecordVectorForGACCRegionAndEquationType(GACC region,
                                                                                 EquationType equationType);
  void calculateMortalityAllDirections(FractionUnits::FractionUnitsEnum probablityUnits);
  char *getCVSorCLS();

  // SIGMortality Getters Heading
  double getProbabilityOfMortality(FractionUnits::FractionUnitsEnum probabilityUnits) const;
  double getTreeCrownLengthScorched(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getTreeCrownVolumeScorched(FractionUnits::FractionUnitsEnum fractionUnits) const;
  double getScorchHeight(LengthUnits::LengthUnitsEnum scorchHeightUnits);
  double getBoleCharHeight(LengthUnits::LengthUnitsEnum boleCharHeightUnits) const;

  // SIGMortality Getters Backing
  double getProbabilityOfMortalityBacking(FractionUnits::FractionUnitsEnum probabilityUnits) const;
  double getTreeCrownLengthScorchedBacking(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getTreeCrownVolumeScorchedBacking(FractionUnits::FractionUnitsEnum fractionUnits) const;
  double getScorchHeightBacking(LengthUnits::LengthUnitsEnum scorchHeightUnits);
  double getBoleCharHeightBacking(LengthUnits::LengthUnitsEnum boleCharHeightUnits) const;

  // SIGMortality Getters Flanking
  double getProbabilityOfMortalityFlanking(FractionUnits::FractionUnitsEnum probabilityUnits) const;
  double getTreeCrownLengthScorchedFlanking(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getTreeCrownVolumeScorchedFlanking(FractionUnits::FractionUnitsEnum fractionUnits) const;
  double getScorchHeightFlanking(LengthUnits::LengthUnitsEnum scorchHeightUnits);
  double getBoleCharHeightFlanking(LengthUnits::LengthUnitsEnum boleCharHeightUnits) const;

protected:
  Mortality heading_;
  Mortality backing_;
  Mortality flanking_;

  double fireLineIntensity_;
  double midFlameWindSpeed_;
  double airTemperature_;
  double windSpeed_;
  double userProvidedWindAdjustmentFactor_;
  WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode_;
};
