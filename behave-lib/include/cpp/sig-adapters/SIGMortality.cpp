#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

#include <algorithm>
#include <functional>

#include "mortality_inputs.h" 
#include "mortality.h"
#include "SIGMortality.h"
#include "species_master_table.h"
#include "mortality_equation_table.h"

SIGMortality::SIGMortality(SpeciesMasterTable& speciesMasterTable) : Mortality(SpeciesMasterTable& speciesMasterTable) {}

SIGMortality::SIGMortality(const SIGMortality& rhs) {
  return Mortality::Mortality(static_cast Mortality (rhs));
}

SIGMortality& SIGMortality::operator=(const SIGMortality& rhs)
{
    if(this != &rhs)
    {
        memberwiseCopyAssignment(rhs);
    }
    return *this;
}

SIGMortality::~SIGMortality() {
  Mortality::~Mortality();
}; 

void SIGMortality::setSpeciesCode(char* speciesCode)
{
  return Mortality::setSpeciesCode(std::string(speciesCode));
}

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

bool* SIGMortality::getRequiredFieldVector()
{
  std::vector<bool> result = Mortality::getRequiredFieldVector();
  return static_cast bool[] (&result[0]);
}

SpeciesMasterTableRecord SIGMortality::getSpeciesRecordBySpeciesCodeAndEquationType(char* speciesCode, EquationType equationType) const
{
  return Mortality::getSpeciesRecordBySpeciesCodeAndEquationType(std::string(speciesCode), equationType);
}

SpeciesMasterTableRecord* SIGMortality::getSpeciesRecordVectorForRegion(RegionCode region) const
{
  std::vector<SpeciesMasterTableRecord> result = Mortality::getSpeciesRecordVectorForRegion(region);
  return static_cast SpeciesMasterTableRecord[] (&result[0]);
}

SpeciesMasterTableRecord* SIGMortality::getSpeciesRecordVectorForRegionAndEquationType(RegionCode region, EquationType equationType) const
{
  std::vector<SpeciesMasterTableRecord> result = Mortality::getSpeciesRecordVectorForRegionAndEquationType(region, equationType);
  return static_cast SpeciesMasterTableRecord[] (&result[0]);
}

bool SIGMortality::updateInputsForSpeciesCodeAndEquationType(char* speciesCode, EquationType equationType)
{
  return Mortality::updateInputsForSpeciesCodeAndEquationType(std::string(speciesCode), equationType);
}
