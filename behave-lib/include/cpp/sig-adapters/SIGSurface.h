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
#include "behaveUnits.h"
#include "fireSize.h"
#include "surfaceFire.h"
#include "surfaceInputs.h"
#include "surface.h"
#include "SIGFuelModels.h"
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
  WindUpslopeAlignmentMode::WindUpslopeAlignmentModeEnum getWindUpslopeAlignmentMode() const;
  double getDirectionOfInterest();

  // SIGSurface Setter Methods
  void setWindUpslopeAlignmentMode(WindUpslopeAlignmentMode::WindUpslopeAlignmentModeEnum) const;
  void setDirectionOfInterest(double directionOfInterest);
  void setSurfaceRunInDirectionOf(SurfaceRunInDirectionOf::SurfaceRunInDirectionOfEnum surfaceRunInDirectionOf);
  void setSurfaceFireSpreadDirectionMode(SurfaceFireSpreadDirectionMode::SurfaceFireSpreadDirectionModeEnum
                                         directionMode);

  // Fuel Model Getter Methods
  char* getFuelCode(int fuelModelNumber) const;
  char* getFuelName(int fuelModelNumber) const;

  // ChaparralFuel Getter Methods
  ChaparralFuelLoadInputMode::ChaparralFuelInputLoadModeEnum getChaparralFuelLoadInputMode();
  double getChaparralLoadDeadFine(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadDeadSmall(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadDeadMedium(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadDeadLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadDeadVeryLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadLiveFine(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadLiveSmall(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadLiveMedium(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadLiveLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const;
  double getChaparralLoadLiveVeryLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const;

  // Surface Getter Methods
  double getSurfaceFireReactionIntensityDead() const;
  double getSurfaceFireReactionIntensityLive() const;

  // SruFaceFire Getter Methods
  double getCharacteristicMoistureDead(MoistureUnits::MoistureUnitsEnum moistureUnits) const;
  double getCharacteristicMoistureLive(MoistureUnits::MoistureUnitsEnum moistureUnits) const;
  double getPackingRatio() const;
  double getRelativePackingRatio() const;
  double getSlopeFactor() const;
  double getWindAdjustmentFactor() const;

protected:
  SurfaceFireSpreadDirectionMode::SurfaceFireSpreadDirectionModeEnum directionMode_;
  SurfaceRunInDirectionOf::SurfaceRunInDirectionOfEnum surfaceRunInDirectionOf_;
  WindUpslopeAlignmentMode::WindUpslopeAlignmentModeEnum windUpslopeAlignmentMode_;
  double directionOfInterest_;
};
