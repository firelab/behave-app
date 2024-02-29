/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class for handling crown fire behavior
* Author:   William Chatham <wchatham@fs.fed.us>
* Credits:  Some of the code in this file is, in part or in whole, from
*           BehavePlus5 source originally authored by Collin D. Bevins and is
*           used with or without modification.
*
*******************************************************************************
*
* THIS SOFTWARE WAS DEVELOPED AT THE ROCKY MOUNTAIN RESEARCH STATION (RMRS)
* MISSOULA FIRE SCIENCES LABORATORY BY EMPLOYEES OF THE FEDERAL GOVERNMENT
* IN THE COURSE OF THEIR OFFICIAL DUTIES. PURSUANT TO TITLE 17 SECTION 105
* OF THE UNITED STATES CODE, THIS SOFTWARE IS NOT SUBJECT TO COPYRIGHT
* PROTECTION AND IS IN THE PUBLIC DOMAIN. RMRS MISSOULA FIRE SCIENCES
* LABORATORY ASSUMES NO RESPONSIBILITY WHATSOEVER FOR ITS USE BY OTHER
* PARTIES,  AND MAKES NO GUARANTEES, EXPRESSED OR IMPLIED, ABOUT ITS QUALITY,
* RELIABILITY, OR ANY OTHER CHARACTERISTIC.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
******************************************************************************/

#include "crown.h"
#include "SIGCrown.h"

#include <cmath>
#include "fuelModels.h"
#include "fireSize.h"
#include "SIGFuelModels.h"
#include "windSpeedUtility.h"
#include "SIGString.h"

SIGCrown::SIGCrown(SIGFuelModels& fuelModels) : Crown(fuelModels) {}

void SIGCrown::doCrownRun()
{
  if (crownFireCalculationMethod_ == CrownFireCalculationMethod::rothermel) {
    doCrownRunRothermel();
  } else {
    doCrownRunScottAndReinhardt();
  }
}

void SIGCrown::setFuelModels(SIGFuelModels& fuelModels) {
  FuelModels baseFuelModels = static_cast <FuelModels> (fuelModels);
  Crown::setFuelModels(baseFuelModels);
}

void SIGCrown::setCrownFireCalculationMethod(CrownFireCalculationMethod CrownFireCalculationMethod)
{
  crownFireCalculationMethod_ = CrownFireCalculationMethod;
};

void SIGCrown::setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits) {
  windSpeed_ = SpeedUnits::toBaseUnits(windSpeed, windSpeedUnits);
  Crown::setWindSpeed(windSpeed_, SpeedUnits::FeetPerMinute, windHeightInputMode_);
}

void SIGCrown::setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode) {
  windHeightInputMode_ = windHeightInputMode;
  Crown::setWindSpeed(windSpeed_, SpeedUnits::FeetPerMinute, windHeightInputMode_);
}

char* SIGCrown::getFuelCode(int fuelModelNumber) const
{
  return SIGString::str2charptr(Crown::getFuelCode(fuelModelNumber));
}

char* SIGCrown::getFuelName(int fuelModelNumber) const
{
  return SIGString::str2charptr(Crown::getFuelName(fuelModelNumber));
}

double SIGCrown::getCrownCriticalFireSpreadRate(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const
{
  return SpeedUnits::fromBaseUnits(crownCriticalFireSpreadRate_, spreadRateUnits);
}

double SIGCrown::getCrownCriticalSurfaceFirelineIntensity(FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits) const
{
  return FirelineIntensityUnits::fromBaseUnits(crownCriticalSurfaceFirelineIntensity_, firelineIntensityUnits);
}

double SIGCrown::getCrownCriticalSurfaceFlameLength(LengthUnits::LengthUnitsEnum flameLengthUnits) const
{
  return LengthUnits::fromBaseUnits(crownFlameLength_, flameLengthUnits);
}

double SIGCrown::getCrownFireActiveRatio() const {
  return crownFireActiveRatio_;
}

double SIGCrown::getCrownTransitionRatio() const {
  return crownFireTransitionRatio_;
}

char* SIGCrown::getMoistureScenarioDescriptionByName(const char* name) {
  return SIGString::str2charptr(Crown::getMoistureScenarioDescriptionByName(std::string(name)));
}

char* SIGCrown::getMoistureScenarioNameByIndex(const int index) {
  return SIGString::str2charptr(Crown::getMoistureScenarioNameByIndex(index));
}

char* SIGCrown::getMoistureScenarioDescriptionByIndex(const int index) {
  return SIGString::str2charptr(Crown::getMoistureScenarioDescriptionByIndex(index));
}

double SIGCrown::getSurfaceFireSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceFuel_.getElapsedTime(TimeUnits::Minutes);
  return Crown::getSurfaceFireSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGCrown::getCrownFireSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceFuel_.getElapsedTime(TimeUnits::Minutes);
  return Crown::getCrownFireSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGCrown::getCrownFireArea(AreaUnits::AreaUnitsEnum areaUnits) const {
  double elapsedTime = surfaceFuel_.getElapsedTime(TimeUnits::Minutes);
  return Crown::getCrownFireArea(areaUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGCrown::getCrownFirePerimeter(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceFuel_.getElapsedTime(TimeUnits::Minutes);
  return Crown::getCrownFirePerimeter(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGCrown::getFinalSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) const {
  // Obtain elapsed time
  double elapsedTime = surfaceFuel_.getElapsedTime(TimeUnits::Minutes);
  double spreadDistanceInBaseUnits = finalSpreadRate_ * elapsedTime;
  return LengthUnits::fromBaseUnits(spreadDistanceInBaseUnits, lengthUnits);
}

double SIGCrown::getFinalFireArea(AreaUnits::AreaUnitsEnum areaUnits) const {

  // Obtain elapsed time
  TimeUnits::TimeUnitsEnum timeUnits = TimeUnits::Minutes;
  double elapsedTime = surfaceFuel_.getElapsedTime(timeUnits);

  // Rothermel
  if (crownFireCalculationMethod_ == CrownFireCalculationMethod::rothermel) {
    if (isSurfaceFire_) {
      return surfaceFuel_.getFireArea(areaUnits, elapsedTime, timeUnits);
    } else {
      return Crown::getCrownFireArea(areaUnits, elapsedTime, timeUnits);
    }
  } else {
    // Scott & Reinhardt
    if (isSurfaceFire_) {
      return surfaceFuel_.getFireArea(areaUnits, elapsedTime, timeUnits);
    } else if (fireType_ == FireType::Torching) {
      FireSize fireSize;
      fireSize.calculateFireBasicDimensions(true, windSpeedAtTwentyFeet_, SpeedUnits::MilesPerHour, passiveCrownFireSpreadRate_, SpeedUnits::FeetPerMinute);
      return fireSize.getFireArea(true, areaUnits, elapsedTime, timeUnits);
    } else {
      return Crown::getCrownFireArea(areaUnits, elapsedTime, timeUnits);
    }
  }
}

double SIGCrown::getFinalFirePerimeter(LengthUnits::LengthUnitsEnum lengthUnits) const {

  // Obtain elapsed time
  TimeUnits::TimeUnitsEnum timeUnits = TimeUnits::Minutes;
  double elapsedTime = surfaceFuel_.getElapsedTime(timeUnits);

  // Rothermel
  if (crownFireCalculationMethod_ == CrownFireCalculationMethod::rothermel) {
    if (isSurfaceFire_) {
      return surfaceFuel_.getFirePerimeter(lengthUnits, elapsedTime, timeUnits);
    } else {
      return Crown::getCrownFirePerimeter(lengthUnits, elapsedTime, timeUnits);
    }
  } else {
    // Scott & Reinhardt
    if (isSurfaceFire_) {
      return surfaceFuel_.getFirePerimeter(lengthUnits, elapsedTime, timeUnits);
    } else if (fireType_ == FireType::Torching) {
      FireSize fireSize;
      fireSize.calculateFireBasicDimensions(true, windSpeedAtTwentyFeet_, SpeedUnits::MilesPerHour, passiveCrownFireSpreadRate_, SpeedUnits::FeetPerMinute);
      return fireSize.getFirePerimeter(true, lengthUnits, elapsedTime, timeUnits);
    } else {
      return Crown::getCrownFirePerimeter(lengthUnits, elapsedTime, timeUnits);
    }
  }
}

void SIGCrown::setElapsedTime(double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits)
{
  surfaceFuel_.setElapsedTime(elapsedTime, timeUnits);
}
