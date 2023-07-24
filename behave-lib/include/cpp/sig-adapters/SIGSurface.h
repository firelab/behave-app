/******************************************************************************
 *
 * Project:  CodeBlocks
 * Purpose:  Class for handling surface fire behavior based on the Facade OOP
 *           Design Pattern and using the Rothermel spread model
 * Author:   William Chatham <wchatham@fs.fed.us>
 * Author:   Richard Sheperd <rsheperd@sig-gis.com>
 * Credits:  Some of the code in the corresponding cpp file is, in part or in
 *           whole, from BehavePlus5 source originally authored by Collin D.
 *           Bevins and is used with or without modification.
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

#pragma once

// The SURFACE module of BehavePlus
#include "surface.h"
#include "SIGFuelModels.h"
#include "SIGMoistureScenarios.h"
#include "SIGString.h"
#include "SIGSurfaceEnums.h"

class SIGSurface : public Surface
{
public:
  SIGSurface() = delete; // No default constructor
  SIGSurface(SIGFuelModels& fuelModels);
  ~SIGSurface();

  void doSurfaceRun();

  // SIGSurface Getter Methods
  WindUpslopeAlignmentMode getWindUpslopeAlignmentMode() const;
  double getDirectionOfInterest() const;
  double getDirectionOfFlanking() const;
  double getDirectionOfBacking() const;

  // SIGSurface Setter Methods
  void setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits);
  void setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode);
  void setWindUpslopeAlignmentMode(WindUpslopeAlignmentMode windUpslopeAlignmentMode);
  void setDirectionOfInterest(double directionOfInterest);
  void setSurfaceRunInDirectionOf(SurfaceRunInDirectionOf surfaceRunInDirectionOf);
  void setSurfaceFireSpreadDirectionMode(SurfaceFireSpreadDirectionMode::SurfaceFireSpreadDirectionModeEnum
                                         directionMode);

  // Fuel Model Getter Methods
  char* getFuelCode(int fuelModelNumber) const;
  char* getFuelName(int fuelModelNumber) const;

  // ChaparralFuel Getter Methods
  ChaparralFuelLoadInputMode::ChaparralFuelInputLoadModeEnum getChaparralFuelLoadInputMode();

  // Surface Getter Methods
  double getSurfaceFireReactionIntensityDead() const;
  double getSurfaceFireReactionIntensityLive() const;

  // SruFaceFire Getter Methods
  double getCharacteristicMoistureDead(MoistureUnits::MoistureUnitsEnum moistureUnits) const;
  double getCharacteristicMoistureLive(MoistureUnits::MoistureUnitsEnum moistureUnits) const;
  double getSlopeFactor() const;
  double getWindAdjustmentFactor() const;
  double getEllipticalA(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getEllipticalB(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getEllipticalC(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getFireArea(AreaUnits::AreaUnitsEnum areaUnits) const;
  double getFirePerimeter(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getBackingSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits);
  double getFlankingSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits);
  double getSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getSpreadDistanceInDirectionOfInterest(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getSpreadRate(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const;

  // MoistureScenario Getter Methods
  char* getMoistureScenarioDescriptionByName(const char* name);
  char* getMoistureScenarioNameByIndex(const int index);
  char* getMoistureScenarioDescriptionByIndex(const int index);

  // MoistureScenario Setter Methods
  void setMoistureScenarios(SIGMoistureScenarios& moistureScenarios);

protected:
  SurfaceFireSpreadDirectionMode::SurfaceFireSpreadDirectionModeEnum directionMode_;
  SurfaceRunInDirectionOf surfaceRunInDirectionOf_;
  WindUpslopeAlignmentMode windUpslopeAlignmentMode_;
  double directionOfInterest_;
  double windSpeed_;
  WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode_;
};
