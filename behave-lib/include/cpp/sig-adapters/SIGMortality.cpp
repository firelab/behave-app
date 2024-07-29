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
      flanking_(speciesMasterTable)
{
  initializeMembers();
}

SIGMortality::SIGMortality(const SIGMortality& rhs)
    : Mortality(static_cast <Mortality> (rhs)),
      heading_(static_cast <Mortality> (rhs)),
      backing_(static_cast <Mortality> (rhs)),
      flanking_(static_cast <Mortality> (rhs))
{
    fireLineIntensity_ = rhs.fireLineIntensity_;
    midFlameWindSpeed_ = rhs.midFlameWindSpeed_;
    airTemperature_ = rhs.airTemperature_;
    windSpeed_ = rhs.windSpeed_;
    userProvidedWindAdjustmentFactor_ = rhs.userProvidedWindAdjustmentFactor_;
    windHeightInputMode_ = WindHeightInputMode::DirectMidflame;
}

void SIGMortality::initializeMembers() {
    fireLineIntensity_ = 0.0;
    midFlameWindSpeed_ = 0.0;
    airTemperature_ = 0.0;
    windSpeed_ = 0.0;
    userProvidedWindAdjustmentFactor_ = 0.0;
}

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

void SIGMortality::setAirTemperature(double airTemperature, TemperatureUnits::TemperatureUnitsEnum temperatureUnits)
{
    heading_.setAirTemperature(airTemperature, temperatureUnits);
    backing_.setAirTemperature(airTemperature, temperatureUnits);
    flanking_.setAirTemperature(airTemperature, temperatureUnits);
}

void SIGMortality::setMidFlameWindSpeed(double midFlameWindSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits)
{
    heading_.setMidFlameWindSpeed(midFlameWindSpeed, windSpeedUnits);
    backing_.setMidFlameWindSpeed(midFlameWindSpeed, windSpeedUnits);
    flanking_.setMidFlameWindSpeed(midFlameWindSpeed, windSpeedUnits);
}

/* Wind Speed */

void SIGMortality::setUserProvidedWindAdjustmentFactor(double userProvidedWindAdjustmentFactor)
{
  setWindSpeedAndWindHeightInputMode(windSpeed_, SpeedUnits::FeetPerMinute, windHeightInputMode_, userProvidedWindAdjustmentFactor);
}

void SIGMortality::setWindSpeedAndWindHeightInputMode(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode, double userProvidedWindAdjustmentFactor) {
  // Set member variables
  windSpeed_ = SpeedUnits::toBaseUnits(windSpeed, windSpeedUnits);
  windHeightInputMode_ = windHeightInputMode;
  userProvidedWindAdjustmentFactor_ = userProvidedWindAdjustmentFactor;

  if (windSpeed > 0.0 && userProvidedWindAdjustmentFactor > 0.0) {

    // Calculate wind speed at 20 feet
    double midFlameWindSpeed = 0.0;

    if (windHeightInputMode_ == WindHeightInputMode::DirectMidflame) {
      midFlameWindSpeed = windSpeed_;
    } else if (windHeightInputMode_ == WindHeightInputMode::TenMeter) {
      // Adjust by dividing by 1.15 to obtain 20-ft Wind Speed, then adjust using user-provided WAF
      midFlameWindSpeed = (windSpeed_ / 1.15) * userProvidedWindAdjustmentFactor_;
    } else {
      // Adjust using user-provided WAF
      midFlameWindSpeed = windSpeed_ * userProvidedWindAdjustmentFactor_;
    }

    setMidFlameWindSpeed(midFlameWindSpeed, SpeedUnits::FeetPerMinute);
  }
}

void SIGMortality::setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits) {
  setWindSpeedAndWindHeightInputMode(windSpeed, windSpeedUnits, windHeightInputMode_, userProvidedWindAdjustmentFactor_);
}

void SIGMortality::setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode) {
  setWindSpeedAndWindHeightInputMode(windSpeed_, SpeedUnits::FeetPerMinute, windHeightInputMode, userProvidedWindAdjustmentFactor_);
}

void SIGMortality::setSurfaceFireFlameLength(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
    heading_.setFlameLength(value, lengthUnits);

    double boleCharHeight = LengthUnits::toBaseUnits(value, lengthUnits) / 1.8;
    heading_.setBoleCharHeight(boleCharHeight, LengthUnits::Feet);
}

void SIGMortality::setSurfaceFireFlameLengthBacking(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
    backing_.setFlameLength(value, lengthUnits);

    double boleCharHeight = LengthUnits::toBaseUnits(value, lengthUnits) / 1.8;
    backing_.setBoleCharHeight(boleCharHeight, LengthUnits::Feet);
}

void SIGMortality::setSurfaceFireFlameLengthFlanking(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
    flanking_.setFlameLength(value, lengthUnits);

    double boleCharHeight = LengthUnits::toBaseUnits(value, lengthUnits) / 1.8;
    flanking_.setBoleCharHeight(boleCharHeight, LengthUnits::Feet);
}

void SIGMortality::setSurfaceFireScorchHeight(double value, LengthUnits::LengthUnitsEnum lengthUnits)
{
    heading_.setScorchHeight(value, lengthUnits);
}

void SIGMortality::setSurfaceFireFirelineIntensity(double value, FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits)
{
    heading_.setFirelineIntensity(value, firelineIntensityUnits);
}

void SIGMortality::setSurfaceFireFirelineIntensityBacking(double value, FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits)
{
    backing_.setFirelineIntensity(value, firelineIntensityUnits);
}

void SIGMortality::setSurfaceFireFirelineIntensityFlanking(double value, FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits)
{
    flanking_.setFirelineIntensity(value, firelineIntensityUnits);
}

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

double SIGMortality::getScorchHeight(LengthUnits::LengthUnitsEnum scorchHeightUnits)
{
    return heading_.getScorchHeight(scorchHeightUnits);
}

double SIGMortality::getScorchHeightBacking(LengthUnits::LengthUnitsEnum scorchHeightUnits)
{
    return backing_.getScorchHeight(scorchHeightUnits);
}

double SIGMortality::getScorchHeightFlanking(LengthUnits::LengthUnitsEnum scorchHeightUnits)
{
    return flanking_.getScorchHeight(scorchHeightUnits);
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

void SIGMortality::calculateMortalityAllDirections(FractionUnits::FractionUnitsEnum probablityUnits) {
    heading_.calculateMortality(probablityUnits);
    backing_.calculateMortality(probablityUnits);
    flanking_.calculateMortality(probablityUnits);
}

double SIGMortality::getTreeCrownLengthScorched(LengthUnits::LengthUnitsEnum lengthUnits) const
{
    return heading_.getTreeCrownLengthScorched(lengthUnits);
}

double SIGMortality::getTreeCrownLengthScorchedBacking(LengthUnits::LengthUnitsEnum lengthUnits) const
{
    return backing_.getTreeCrownLengthScorched(lengthUnits);
}

double SIGMortality::getTreeCrownLengthScorchedFlanking(LengthUnits::LengthUnitsEnum lengthUnits) const
{
    return flanking_.getTreeCrownLengthScorched(lengthUnits);
}

double SIGMortality::getTreeCrownVolumeScorched(FractionUnits::FractionUnitsEnum fractionUnits) const
{
    return heading_.getTreeCrownVolumeScorched(fractionUnits);
}

double SIGMortality::getTreeCrownVolumeScorchedBacking(FractionUnits::FractionUnitsEnum fractionUnits) const
{
    return backing_.getTreeCrownVolumeScorched(fractionUnits);
}

double SIGMortality::getTreeCrownVolumeScorchedFlanking(FractionUnits::FractionUnitsEnum fractionUnits) const
{
    return flanking_.getTreeCrownVolumeScorched(fractionUnits);
}

char* SIGMortality::getCVSorCLS()
{
    double cls = heading_.getTreeCrownLengthScorched(LengthUnits::Feet);
    double cvs = heading_.getTreeCrownVolumeScorched(FractionUnits::Fraction);
    if (cls == -1 && cvs == -1) {
        return SIGString::str2charptr("None");
    } else if (cls != -1 && cvs != -1) {
        return SIGString::str2charptr("Both");
    } else if (cls != -1) {
        return SIGString::str2charptr("CLS");
    } else {
        return SIGString::str2charptr("CVS");
    }
};

EquationType SIGMortality::getEquationType() const
{
    return heading_.getEquationType();
}

