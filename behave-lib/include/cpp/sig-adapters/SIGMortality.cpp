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

SIGMortality::SIGMortality(SpeciesMasterTable& speciesMasterTable)
  : Mortality(speciesMasterTable),
    heading_(speciesMasterTable),
    backing_(speciesMasterTable),
    flanking_(speciesMasterTable) {}

SIGMortality::SIGMortality(const SIGMortality& rhs)
  : Mortality(static_cast <Mortality> (rhs)),
    heading_(static_cast <Mortality> (rhs)),
    backing_(static_cast <Mortality> (rhs)),
    flanking_(static_cast <Mortality> (rhs)) {}

void SIGMortality::setSpeciesCode(char* speciesCode)
{
  heading_.setSpeciesCode(std::string(speciesCode));
  backing_.setSpeciesCode(std::string(speciesCode));
  flanking_.setSpeciesCode(std::string(speciesCode));
}

void SIGMortality::setTreeHeight(double treeHeight, LengthUnits::LengthUnitsEnum treeHeightUnits) {
  heading_.setTreeHeight(treeHeight, treeHeightUnits);
  backing_.setTreeHeight(treeHeight, treeHeightUnits);
  flanking_.setTreeHeight(treeHeight, treeHeightUnits);
}

void SIGMortality::setCrownRatio(double crownRatio)
{
  heading_.setCrownRatio(crownRatio);
  backing_.setCrownRatio(crownRatio);
  flanking_.setCrownRatio(crownRatio);
}

void SIGMortality::setDBH(double dbh, LengthUnits::LengthUnitsEnum diameterUnits)
{
  heading_.setDBH(dbh, diameterUnits);
  backing_.setDBH(dbh, diameterUnits);
  flanking_.setDBH(dbh, diameterUnits);
}

void SIGMortality::setBoleCharHeight(double boleCharHeight, LengthUnits::LengthUnitsEnum boleCharHeightUnits)
{
  heading_.setBoleCharHeight(boleCharHeight, boleCharHeightUnits);
  backing_.setBoleCharHeight(boleCharHeight, boleCharHeightUnits);
  flanking_.setBoleCharHeight(boleCharHeight, boleCharHeightUnits);
}

void SIGMortality::setEquationType(EquationType equationType)
{
  heading_.setEquationType(equationType);
  backing_.setEquationType(equationType);
  flanking_.setEquationType(equationType);
}

void SIGMortality::setSurfaceFireFlameLength(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
  heading_.setFlameLengthOrScorchHeightSwitch(FlameLengthOrScorchHeightSwitch::flame_length);
  heading_.setFlameLengthOrScorchHeightValue(value, lengthUnits);
};

void SIGMortality::setSurfaceFireFlameLengthBacking(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
  backing_.setFlameLengthOrScorchHeightSwitch(FlameLengthOrScorchHeightSwitch::flame_length);
  backing_.setFlameLengthOrScorchHeightValue(value, lengthUnits);
};

void SIGMortality::setSurfaceFireFlameLengthFlanking(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
  flanking_.setFlameLengthOrScorchHeightSwitch(FlameLengthOrScorchHeightSwitch::flame_length);
  flanking_.setFlameLengthOrScorchHeightValue(value, lengthUnits);
};

void SIGMortality::setSurfaceFireScorchHeight(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
  heading_.setFlameLengthOrScorchHeightSwitch(FlameLengthOrScorchHeightSwitch::scorch_height);
  heading_.setFlameLengthOrScorchHeightValue(value, lengthUnits);
};

char* SIGMortality::getSpeciesCode() const
{
  return SIGString::str2charptr(heading_.getSpeciesCode());
}

char* SIGMortality::getSpeciesCodeAtSpeciesTableIndex(int index) const
{
  return SIGString::str2charptr(heading_.getSpeciesCodeAtSpeciesTableIndex(index));
}

char* SIGMortality::getScientificNameAtSpeciesTableIndex(int index) const
{
  return SIGString::str2charptr(heading_.getScientificNameAtSpeciesTableIndex(index));
}

double SIGMortality::getProbabilityOfMortality(FractionUnits::FractionUnitsEnum probabilityUnits) const
{
  return heading_.getProbabilityOfMortality(probabilityUnits);
}

double SIGMortality::getProbabilityOfMortalityBacking(FractionUnits::FractionUnitsEnum probabilityUnits) const
{
  return backing_.getProbabilityOfMortality(probabilityUnits);
}

double SIGMortality::getProbabilityOfMortalityFlanking(FractionUnits::FractionUnitsEnum probabilityUnits) const
{
  return flanking_.getProbabilityOfMortality(probabilityUnits);
}

char* SIGMortality::getCommonNameAtSpeciesTableIndex(int index) const
{
  return SIGString::str2charptr(heading_.getCommonNameAtSpeciesTableIndex(index));
}

char* SIGMortality::getScientificNameFromSpeciesCode(char* speciesCode) const
{
  return SIGString::str2charptr(heading_.getScientificNameFromSpeciesCode(std::string(speciesCode)));
}

char* SIGMortality::getCommonNameFromSpeciesCode(char* speciesCode) const
{
  return SIGString::str2charptr(heading_.getScientificNameFromSpeciesCode(std::string(speciesCode)));
}

int SIGMortality::getMortalityEquationNumberFromSpeciesCode(char* speciesCode) const
{
  return heading_.getMortalityEquationNumberFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getBarkEquationNumberFromSpeciesCode(char* speciesCode) const
{
  return heading_.getBarkEquationNumberFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getCrownCoefficientCodeFromSpeciesCode(char* speciesCode) const
{
  return heading_.getCrownCoefficientCodeFromSpeciesCode(std::string(speciesCode));
}

EquationType SIGMortality::getEquationTypeFromSpeciesCode(char* speciesCode) const
{
  return heading_.getEquationTypeFromSpeciesCode(std::string(speciesCode));
}

CrownDamageEquationCode SIGMortality::getCrownDamageEquationCodeFromSpeciesCode(char* speciesCode) const
{
  return heading_.getCrownDamageEquationCodeFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getSpeciesTableIndexFromSpeciesCode(char* speciesCode) const
{
  return heading_.getSpeciesTableIndexFromSpeciesCode(std::string(speciesCode));
}

int SIGMortality::getSpeciesTableIndexFromSpeciesCodeAndEquationType(char* speciesNameCode, EquationType equationType) const
{
  return heading_.getSpeciesTableIndexFromSpeciesCodeAndEquationType(std::string(speciesNameCode), equationType);
}

bool SIGMortality::updateInputsForSpeciesCodeAndEquationType(char* speciesCode, EquationType equationType)
{
  return heading_.updateInputsForSpeciesCodeAndEquationType(std::string(speciesCode), equationType);
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

void SIGMortality::setFirelineIntensity(double firelineIntensity,
                                        FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits) {
  fireLineIntensity_ = FirelineIntensityUnits::toBaseUnits(firelineIntensity, firelineIntensityUnits);
}

void SIGMortality::setMidFlameWindSpeed(double midFlameWindSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits) {
  midFlameWindSpeed_ = SpeedUnits::toBaseUnits(midFlameWindSpeed, windSpeedUnits);
}

void SIGMortality::setAirTemperature(double airTemperature,
                                     TemperatureUnits::TemperatureUnitsEnum temperatureUnits) {
  airTemperature_ = TemperatureUnits::toBaseUnits(airTemperature, temperatureUnits);
}

double SIGMortality::getCalculatedScorchHeight(LengthUnits::LengthUnitsEnum scorchHeightUnits) {
  return calculateScorchHeight(fireLineIntensity_,
                               FirelineIntensityUnits::BtusPerFootPerSecond,
                               midFlameWindSpeed_,
                               SpeedUnits::FeetPerMinute,
                               airTemperature_,
                               TemperatureUnits::Fahrenheit,
                               scorchHeightUnits);
}

void SIGMortality::calculateMortalityAllDirections(FractionUnits::FractionUnitsEnum probablityUnits) {
  heading_.calculateMortality(probablityUnits);
  backing_.calculateMortality(probablityUnits);
  flanking_.calculateMortality(probablityUnits);
}

double SIGMortality::getTreeCrownLengthScorched(FractionUnits::FractionUnitsEnum fractionUnits) const
{
  heading_.getTreeCrownLengthScorched(fractionUnits);
}

double SIGMortality::getTreeCrownVolumeScorched(FractionUnits::FractionUnitsEnum fractionUnits) const
{
  heading_.getTreeCrownVolumeScorched(fractionUnits);
}

double SIGMortality::getTreeCrownLengthScorchedBacking(FractionUnits::FractionUnitsEnum fractionUnits) const
{
  backing_.getTreeCrownLengthScorched(fractionUnits);
}

double SIGMortality::getTreeCrownVolumeScorchedBacking(FractionUnits::FractionUnitsEnum fractionUnits) const
{
  backing_.getTreeCrownVolumeScorched(fractionUnits);
}

double SIGMortality::getTreeCrownLengthScorchedFlanking(FractionUnits::FractionUnitsEnum fractionUnits) const
{
  flanking_.getTreeCrownLengthScorched(fractionUnits);
}

double SIGMortality::getTreeCrownVolumeScorchedFlanking(FractionUnits::FractionUnitsEnum fractionUnits) const
{
  flanking_.getTreeCrownVolumeScorched(fractionUnits);
}

void SIGMortality::setBoleCharHeightFromFlameLengthHeading(double flameLength,
                                                           LengthUnits::LengthUnitsEnum flameLengthunits)
{
  double boleCharHeight = LengthUnits::toBaseUnits(flameLength, flameLengthunits) / 1.8;
  heading_.setBoleCharHeight(boleCharHeight, LengthUnits::Feet);
}

void SIGMortality::setBoleCharHeightFromFlameLengthBacking(double flameLength,
                                                           LengthUnits::LengthUnitsEnum flameLengthunits)
{
  double boleCharHeight = LengthUnits::toBaseUnits(flameLength, flameLengthunits) / 1.8;
  backing_.setBoleCharHeight(boleCharHeight, LengthUnits::Feet);
}

void SIGMortality::setBoleCharHeightFromFlameLengthFlanking(double flameLength,
                                                            LengthUnits::LengthUnitsEnum flameLengthunits)
{
  double boleCharHeight = LengthUnits::toBaseUnits(flameLength, flameLengthunits) / 1.8;
  flanking_.setBoleCharHeight(boleCharHeight, LengthUnits::Feet);
}



