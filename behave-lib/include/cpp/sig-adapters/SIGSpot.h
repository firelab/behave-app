/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class for calculating spotting distance from a wind-driven surface
*           fire, torching trees, or a burning pile
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

#ifndef _SIGSPOT_H_
#define _SIGSPOT_H_

#include "spot.h"
#include "crown.h"
#include "surfaceInputEnums.h"

class SIGSpot : public Spot {
public:
  void calculateAll();
  void setFireType(FireType::FireTypeEnum fireType);
  void setActiveCrownFlameLength(double flameLength, LengthUnits::LengthUnitsEnum flameLengthUnits);
  void setWindSpeedAndWindHeightInputMode(double windwindSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode);
  void setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits);
  void setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode);
  void setDownwindCoverHeight(double downwindCoverHeight, LengthUnits::LengthUnitsEnum downwindCoverHeightUnits);
  double getMaxMountainousTerrainSpottingDistanceFromTorchingTrees(LengthUnits::LengthUnitsEnum spottingDistanceUnits) const;
  double getMaxMountainousTerrainSpottingDistanceFromActiveCrown(LengthUnits::LengthUnitsEnum spottingDistanceUnits) const;

private:
  double windSpeed_;
  WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode_;
  FireType::FireTypeEnum fireType_;
};

#endif // SPOT_H
