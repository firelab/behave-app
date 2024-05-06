/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class for calculating spotting distance from a wind-driven surface
*			fire, torching trees, or a burning pile
* Author:   William Chatham <wchatham@fs.fed.us>
* Author:   Richard Sheperd <rsheperd@sig-gis.com>
* Credits:  Some of the code in this corresponding cpp file is, in part or in
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

#include "SIGSpot.h"

void SIGSpot::calculateAll() {
  Spot::calculateSpottingDistanceFromBurningPile();
  Spot::calculateSpottingDistanceFromSurfaceFire();
  Spot::calculateSpottingDistanceFromTorchingTrees();
}

void SIGSpot::setWindSpeedAndWindHeightInputMode(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode) {
  // Set member variables
  windSpeed_ = SpeedUnits::toBaseUnits(windSpeed, windSpeedUnits);
  windHeightInputMode_ = windHeightInputMode;

  // Calculate wind speed at 20 feet
  double windSpeedAtTwentyFeet = 0;

  if (windHeightInputMode_ == WindHeightInputMode::TenMeter) {
    windSpeedAtTwentyFeet = windSpeed_ * 1.15;
  } else {
    windSpeedAtTwentyFeet = windSpeed_;
  }

  Spot::setWindSpeedAtTwentyFeet(windSpeedAtTwentyFeet, SpeedUnits::FeetPerMinute);
}

void SIGSpot::setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits) {
  setWindSpeedAndWindHeightInputMode(windSpeed, windSpeedUnits, windHeightInputMode_);
}

void SIGSpot::setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode) {
  setWindSpeedAndWindHeightInputMode(windSpeed_, SpeedUnits::FeetPerMinute, windHeightInputMode);
}
