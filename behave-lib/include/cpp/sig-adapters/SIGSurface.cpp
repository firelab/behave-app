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

double SIGSurface::getCharacteristicMoistureDead(MoistureUnits::MoistureUnitsEnum moistureUnits) const {
  return MoistureUnits::fromBaseUnits(surfaceFire_.getWeightedMoistureByLifeState(FuelLifeState::Dead),
                                      moistureUnits);
}

double SIGSurface::getCharacteristicMoistureLive(MoistureUnits::MoistureUnitsEnum moistureUnits) const {
  return MoistureUnits::fromBaseUnits(surfaceFire_.getWeightedMoistureByLifeState(FuelLifeState::Live),
                                      moistureUnits);
}

double SIGSurface::getRelativePackingRatio() const {
  return surfaceFire_.surfaceFuelbedIntermediates_.getRelativePackingRatio();
}

double SIGSurface::getPackingRatio() const {
  return surfaceFire_.surfaceFuelbedIntermediates_.getPackingRatio();
}

double SIGSurface::getSlopeFactor() const {
  return surfaceInputs_.getSlopeFactor();
}

double SIGSurface::getWindAdjustmentFactor() const {
  return surfaceFire_.getWindAdjustmentFactor();
}

WindUpslopeAlignmentMode::WindUpslopeAlignmentModeEnum getWindUpslopeAlignmentMode() const {
  return windUpslopeAlignmentMode_;
}

void SIGSurface::setWindUpslopeAlignmentMode(WindUpslopeAlignmentMode::WindUpslopeAlignmentModeEnum WindUpslopeAlignmentMode) const {
  return windUpslopeAlignmentMode_ = WindUpslopeAlignmentMode;
}

double SIGSurface::getDirectionOfInterest() {
  return directionOfInterest_;
}

void SIGSurface::setDirectionOfInterest(double directionOfInterest) {
  directionOfInterest_ = directionOfInterest;
}

void SIGSurface::setSurfaceRunInDirectionOf(SurfaceRunInDirectionOf::SurfaceRunInDirectionOfEnum surfaceRunInDirectionOf) {
  surfaceRunInDirectionOf_ = surfaceRunInDirectionOf;
}

void SIGSurface::setSurfaceFireSpreadDirectionMode(SurfaceFireSpreadDirectionMode::SurfaceFireSpreadDirectionModeEnum directionMode) {
  directionMode_ = directionMode;
}

void SIGSurface::doSurfaceRun() {
  if surfaceRunInDirectionOf_ == SurfaceRunInDirectionOf::MaxSpread {
      doSurfaceRunInDirectionOfMaxSpread();
    } else {
    doSurfaceRunInDirectionOfInterest(directionOfInterest_, directionMode_);
  }
}

double SIGSurface::getEllipticalA(LengthUnits::LengthUnitsEnum) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getEllipticalA(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getEllipticalB(LengthUnits::LengthUnitsEnum) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getEllipticalB(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getEllipticalC(LengthUnits::LengthUnitsEnum) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getEllipticalC(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getFireArea(AreaUnits::AreaUnitsEnum areaUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getFireArea(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getFirePerimeter(LengthUnits::LengthUnitsEnum lengthUnits) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getFirePerimeter(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getBackingSpreadDistance(LengthUnits::LengthUnitsEnum) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getBackingSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getFlankingSpreadDistance(LengthUnits::LengthUnitsEnum) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getFlankingSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getSpreadDistance(LengthUnits::LengthUnitsEnum) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getSpreadDistance(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

double SIGSurface::getSpreadDistanceInDirectionOfInterest(LengthUnits::LengthUnitsEnum) const {
  double elapsedTime = surfaceInputs_.getElapsedTime();
  return getSpreadDistanceInDirectionOfInterest(lengthUnits, elapsedTime, TimeUnits::Minutes);
}

void SIGSurface::setElapsedTime(double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits) {
  surfaceInputs_.setElapsedTime(elapsedTime, timeUnits);
}
