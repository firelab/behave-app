//{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}
// Name: SIGMortality.h
// Desc: Main interface for Morality CodeBlock
// Author: Richard J. Sheperd, Spatial Informatics Group
//*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}
#pragma once 

#include <string>
#include <vector>

#include "canopy_coefficient_table.h"
#include "mortality_inputs.h"
#include "mortality.h"
#include "SIGString.h"

class SIGMortality : public Mortality
{
public:
  SIGMortality() = delete; // There is no default constructor
  explicit SIGMortality(SpeciesMasterTable& speciesMasterTable);
  SIGMortality(const SIGMortality& rhs);
  SIGMortality& operator=(const SIGMortality& rhs);
  ~SIGMortality();

  void setSpeciesCode(char* speciesCode);
  bool updateInputsForSpeciesCodeAndEquationType(char* speciesCode, EquationType equationType);
  char* getSpeciesCode() const;
  char* getSpeciesCodeAtSpeciesTableIndex(int index) const;
  char* getScientificNameAtSpeciesTableIndex(int index) const;
  char* getCommonNameAtSpeciesTableIndex(int index) const;
  char* getScientificNameFromSpeciesCode(char* speciesCode) const;
  char* getCommonNameFromSpeciesCode(char* speciesCode) const;
  int getSpeciesTableIndexFromSpeciesCode(char* speciesNameCode) const;
  int getSpeciesTableIndexFromSpeciesCodeAndEquationType(char* speciesNameCode, EquationType equationType) const;

  bool* getRequiredFieldVector();
  SpeciesMasterTableRecord getSpeciesRecordBySpeciesCodeAndEquationType(char* speciesCode, EquationType equationType) const;
  SpeciesMasterTableRecord* getSpeciesRecordVectorForRegion(RegionCode region) const;
  SpeciesMasterTableRecord* getSpeciesRecordVectorForRegionAndEquationType(RegionCode region, EquationType equationType) const;

  };
