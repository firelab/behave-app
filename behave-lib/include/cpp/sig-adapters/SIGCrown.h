/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class for handling crown fire behavior
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

// TODO: Add unit conversions for energy and incorporate into calculateCrownCriticalSurfaceFireIntensity() - WMC 11/16
// TODO: Allow for use case in which Crown is run completely without Surface, will involve allowing direct input of HPUA
//       and surface flame length, as well as setting all other pertinent surface inputs in Crown's copy of Surface - WMC 11/16

#pragma once

#include "behaveUnits.h"
#include "crownInputs.h"
#include "surface.h"
#include "crown.h"
#include "SIGFuelModels.h"

enum class CrownFireCalculationMethod
  {
    rothermel,
    scott_and_reinhardt
  };

class SIGCrown : public Crown
{
public:
  SIGCrown(SIGFuelModels& fuelModels);

  void setFuelModels(SIGFuelModels& fuelModels);
  void setCrownFireCalculationMethod(CrownFireCalculationMethod CrownFireCalculationMethod);
  void doCrownRun();
  char* getFuelCode(int fuelModelNumber) const;
  char* getFuelName(int fuelModelNumber) const;
  double getCrownCriticalFireSpreadRate(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const;
  double getCrownCriticalSurfaceFirelineIntensity(FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits) const;
  double getCrownCriticalSurfaceFlameLength(LengthUnits::LengthUnitsEnum flameLengthUnits) const;
  double getCrownFireActiveRatio() const;
  double getCrownTransitionRatio() const;
  char* getMoistureScenarioDescriptionByName(const char* name);
  char* getMoistureScenarioNameByIndex(const int index);
  char* getMoistureScenarioDescriptionByIndex(const int index);
  double getCrownFireSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getSurfaceFireSpreadDistance(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getCrownFireArea(AreaUnits::AreaUnitsEnum areaUnits) const;
  double getCrownFirePerimeter(LengthUnits::LengthUnitsEnum lengthUnits) const;
  void setElapsedTime(double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits);
  double getElapsedTime(TimeUnits::TimeUnitsEnum timeUnits);

private:
  CrownFireCalculationMethod crownFireCalculationMethod_;
};
