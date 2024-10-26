/******************************************************************************
 *
 * Project:  CodeBlocks
 * Purpose:  Class for handling surface fire behavior based on the Facade OOP
 *           Design Pattern and using the Rothermel spread model
 * Author:   William Chatham <wchatham@fs.fed.us>
 * Author:   Richard J. Sheperd <rsheperd@sig-gis.com>
 * Credits:  Some of the code in this file is, in part or in whole, from
 *           BehavePlus5 source originally authored by Collin D. Bevins and is
 *           used with or without modification.
 *
 ******************************************************************************
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

#include "surface.h"
#include "SIGSurface.h"

#include "surfaceTwoFuelModels.h"
#include "surfaceInputs.h"
#include "SIGString.h"

SIGSurface::SIGSurface(SIGFuelModels& fuelModels) : Surface(fuelModels) {}

SIGSurface::~SIGSurface() {
  Surface::~Surface();
};

char* SIGSurface::getFuelCode(int fuelModelNumber) const
{
  return SIGString::str2charptr(Surface::getFuelCode(fuelModelNumber));
}

char* SIGSurface::getFuelName(int fuelModelNumber) const
{
  return SIGString::str2charptr(Surface::getFuelName(fuelModelNumber));
}

ChaparralFuelLoadInputMode::ChaparralFuelInputLoadModeEnum SIGSurface::getChaparralFuelLoadInputMode() {
  return surfaceInputs_.getChaparralFuelLoadInputMode();
}

double SIGSurface::getSurfaceFireReactionIntensityDead() const {
  return getSurfaceFireReactionIntensityForLifeState(FuelLifeState::Dead);
}

double SIGSurface::getSurfaceFireReactionIntensityLive() const {
  return getSurfaceFireReactionIntensityForLifeState(FuelLifeState::Live);
}

double SIGSurface::getCharacteristicMoistureDead(FractionUnits::FractionUnitsEnum moistureUnits) const {
  return FractionUnits::fromBaseUnits(surfaceFire_.getWeightedMoistureByLifeState(FuelLifeState::Dead),
                                      moistureUnits);
}

double SIGSurface::getCharacteristicMoistureLive(FractionUnits::FractionUnitsEnum moistureUnits) const {
  return FractionUnits::fromBaseUnits(surfaceFire_.getWeightedMoistureByLifeState(FuelLifeState::Live),
                                      moistureUnits);
}

double SIGSurface::getSlopeFactor() const {
  return surfaceFire_.getSlopeFactor();
}

double SIGSurface::getWindAdjustmentFactor() const {
  return surfaceFire_.getWindAdjustmentFactor();
}

WindUpslopeAlignmentMode SIGSurface::getWindUpslopeAlignmentMode() const {
  return windUpslopeAlignmentMode_;
}

void SIGSurface::setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits) {
  windSpeed_ = SpeedUnits::toBaseUnits(windSpeed, windSpeedUnits);
  Surface::setWindSpeed(windSpeed_, SpeedUnits::FeetPerMinute, windHeightInputMode_);
}

void SIGSurface::setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode) {
  windHeightInputMode_ = windHeightInputMode;
  Surface::setWindSpeed(windSpeed_, SpeedUnits::FeetPerMinute, windHeightInputMode_);
}

void SIGSurface::setWindUpslopeAlignmentMode(WindUpslopeAlignmentMode windUpslopeAlignmentMode) {
  windUpslopeAlignmentMode_ = windUpslopeAlignmentMode;
}

double SIGSurface::getDirectionOfInterest() const {
  return directionOfInterest_;
}

double SIGSurface::getDirectionOfBacking () const {
  return int(Surface::getDirectionOfMaxSpread() + 180.0) % 360;
}

double SIGSurface::getDirectionOfFlanking () const {
  return int(Surface::getDirectionOfMaxSpread() + 90.0) % 360;
}

void SIGSurface::setDirectionOfInterest(double directionOfInterest) {
  directionOfInterest_ = directionOfInterest;
}

void SIGSurface::setSurfaceRunInDirectionOf(SurfaceRunInDirectionOf surfaceRunInDirectionOf) {
  surfaceRunInDirectionOf_ = surfaceRunInDirectionOf;
}

void SIGSurface::setSurfaceFireSpreadDirectionMode(SurfaceFireSpreadDirectionMode::SurfaceFireSpreadDirectionModeEnum directionMode) {
  directionMode_ = directionMode;
}

void SIGSurface::doSurfaceRun() {
    if (surfaceRunInDirectionOf_ == SurfaceRunInDirectionOf::MaxSpread || (surfaceRunInDirectionOf_ == SurfaceRunInDirectionOf::HeadingFlankingBacking)) {
    doSurfaceRunInDirectionOfMaxSpread();
  } else {
    doSurfaceRunInDirectionOfInterest(directionOfInterest_, directionMode_);
  }
}

double SIGSurface::getEllipticalA(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getEllipticalA(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getEllipticalB(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getEllipticalB(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getEllipticalC(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getEllipticalC(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getFireLength(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getFireLength(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getMaxFireWidth(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getMaxFireWidth(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getFireArea(AreaUnits::AreaUnitsEnum areaUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getFireArea(areaUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getFirePerimeter(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getFirePerimeter(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getBackingSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getBackingSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getFlankingSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getFlankingSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getSpreadRate(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const {
    return Surface::getSpreadRate(spreadRateUnits);
}

double SIGSurface::getHeadingSpreadRate(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const {
  return Surface::getSpreadRate(spreadRateUnits);
}

SurfaceRunInDirectionOf SIGSurface::getSurfaceRunInDirectionOf() const {
  return surfaceRunInDirectionOf_;
}

double SIGSurface::getSpreadDistanceInDirectionOfInterest(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime(TimeUnits::Minutes);
  return Surface::getSpreadDistanceInDirectionOfInterest(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

char* SIGSurface::getMoistureScenarioDescriptionByName(const char* name) {
  return SIGString::str2charptr(Surface::getMoistureScenarioDescriptionByName(std::string(name)));
}

char* SIGSurface::getMoistureScenarioNameByIndex(const int index) {
  return SIGString::str2charptr(Surface::getMoistureScenarioNameByIndex(index));
}

char* SIGSurface::getMoistureScenarioDescriptionByIndex(const int index) {
  return SIGString::str2charptr(Surface::getMoistureScenarioDescriptionByIndex(index));
}

void SIGSurface::setMoistureScenarios(SIGMoistureScenarios& moistureScenarios){
  Surface::setMoistureScenarios(moistureScenarios);
}
