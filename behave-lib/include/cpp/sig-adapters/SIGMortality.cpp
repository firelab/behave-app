#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include <memory>

#include <algorithm>
#include <functional>

#include "mortality_inputs.h"
#include "mortality.h"
#include "SIGMortality.h"
#include "species_master_table.h"
#include "mortality_equation_table.h"
#include "SIGCollections.h"

SIGMortality::SIGMortality(SpeciesMasterTable& speciesMasterTable) : Mortality(speciesMasterTable) {}

SIGMortality::SIGMortality(const SIGMortality& rhs) : Mortality(static_cast <Mortality> (rhs)) {}

void SIGMortality::setSpeciesCode(char* speciesCode)
{
  return Mortality::setSpeciesCode(std::string(speciesCode));
}

void setSurfaceFireFlameLength(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
  setFlameLengthOrScorchHeightSwitch(FlameLengthOrScorchHeightSwitch::flame_length);
  setFlameLengthOrScorchHeightValue(value, lengthUnits);
};

void setSurfaceFireScorchHeight(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
  setFlameLengthOrScorchHeightSwitch(FlameLengthOrScorchHeightSwitch::scorch_height);
  setFlameLengthOrScorchHeightValue(value, lengthUnits);
};

char* SIGMortality::getSpeciesCode() const
{
  return SIGString::str2charptr(Mortality::getSpeciesCode());
}

char* SIGMortality::getSpeciesCodeAtSpeciesTableIndex(int index) const
{
  return SIGString::str2charptr(Mortality::getSpeciesCodeAtSpeciesTableIndex(index));
}

char* SIGMortality::getScientificNameAtSpeciesTableIndex(int index) const
{
  return SIGString::str2charptr(Mortality::getScientificNameAtSpeciesTableIndex(index));
}

char* SIGMortality::getCommonNameAtSpeciesTableIndex(int index) const
{
  return SIGString::str2charptr(Mortality::getCommonNameAtSpeciesTableIndex(index));
}

char* SIGMortality::getScientificNameFromSpeciesCode(char* speciesCode) const
{
  return SIGString::str2charptr(Mortality::getScientificNameFromSpeciesCode(std::string(speciesCode)));
}

char* SIGMortality::getCommonNameFromSpeciesCode(char* speciesCode) const
{
  return SIGString::str2charptr(Mortality::getScientificNameFromSpeciesCode(std::string(speciesCode)));
}

int SIGMortality::getMortalityEquationNumberFromSpeciesCode(char* speciesCode) const
{
  return Mortality::getMortalityEquationNumberFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getBarkEquationNumberFromSpeciesCode(char* speciesCode) const
{
  return Mortality::getBarkEquationNumberFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getCrownCoefficientCodeFromSpeciesCode(char* speciesCode) const
{
  return Mortality::getCrownCoefficientCodeFromSpeciesCode(std::string(speciesCode));
}

EquationType SIGMortality::getEquationTypeFromSpeciesCode(char* speciesCode) const
{
  return Mortality::getEquationTypeFromSpeciesCode(std::string(speciesCode));
}

CrownDamageEquationCode SIGMortality::getCrownDamageEquationCodeFromSpeciesCode(char* speciesCode) const
{
  return Mortality::getCrownDamageEquationCodeFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getSpeciesTableIndexFromSpeciesCode(char* speciesCode) const
{
  return Mortality::getSpeciesTableIndexFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getSpeciesTableIndexFromSpeciesCodeAndEquationType(char* speciesNameCode, EquationType equationType) const
{
  return Mortality::getSpeciesTableIndexFromSpeciesCodeAndEquationType(std::string(speciesNameCode), equationType);
}

bool SIGMortality::updateInputsForSpeciesCodeAndEquationType(char* speciesCode, EquationType equationType)
{
  return Mortality::updateInputsForSpeciesCodeAndEquationType(std::string(speciesCode), equationType);
}

BoolVector* SIGMortality::getRequiredFieldVector()
{
    vector<bool> results = Mortality::getRequiredFieldVector();
    BoolVector* ptr = new BoolVector(results);
    return ptr;
}

SpeciesMasterTableRecordVector* SIGMortality::getSpeciesRecordVectorForRegion(RegionCode region) {
  vector<SpeciesMasterTableRecord> results = Mortality::getSpeciesRecordVectorForRegion(region);
  SpeciesMasterTableRecordVector *ptr = new SpeciesMasterTableRecordVector(results);
  return ptr;
}

SpeciesMasterTableRecordVector* SIGMortality::getSpeciesRecordVectorForRegionAndEquationType(RegionCode region, EquationType equationType) {
  vector<SpeciesMasterTableRecord> results = Mortality::getSpeciesRecordVectorForRegionAndEquationType(region, equationType);
  SpeciesMasterTableRecordVector *ptr = new SpeciesMasterTableRecordVector(results);
  return ptr;
}
