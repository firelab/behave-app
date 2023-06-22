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

class SIGMortality : public Mortality {
public:
  SIGMortality() = delete; // There is no default constructor
  explicit SIGMortality(SpeciesMasterTable &speciesMasterTable);
  SIGMortality(const SIGMortality &rhs);

  // SIGMortality Setters
  void setSpeciesCode(char *speciesCode);
  void setSurfaceFireFlameLength(double value, LengthUnits::LengthUnitsEnum lengthUnits);
  void setSurfaceFireScorchHeight(double value, LengthUnits::LengthUnitsEnum lengthUnits);
  void setFirelineIntensity(double firelineIntensity,
                            FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits);
  void setMidFlameWindSpeed(double midFlameWindSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits);
  void setAirTemperature(double airTemperature, TemperatureUnits::TemperatureUnitsEnum temperatureUnits,
                         LengthUnits::LengthUnitsEnum scorchHeightUnits);
  bool updateInputsForSpeciesCodeAndEquationType(char *speciesCode, EquationType equationType);

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
  CrownDamageEquationCode getCrownDamageEquationCodeFromSpeciesCode(char *speciesCode) const;
  int getSpeciesTableIndexFromSpeciesCode(char *speciesNameCode) const;
  int getSpeciesTableIndexFromSpeciesCodeAndEquationType(char *speciesNameCode, EquationType equationType) const;
  SpeciesMasterTableRecord getSpeciesRecordBySpeciesCodeAndEquationType(char *speciesCode,
                                                                        EquationType equationType) const;
  BoolVector *getRequiredFieldVector();
  SpeciesMasterTableRecordVector *getSpeciesRecordVectorForRegion(RegionCode region);
  SpeciesMasterTableRecordVector *getSpeciesRecordVectorForRegionAndEquationType(RegionCode region,
                                                                                 EquationType equationType);
  double getCalculatedScorchHeight();

protected:
  double fireLineIntensity_;
  double midFlameWindSpeed_;
  double getAirTemperature_;
}
