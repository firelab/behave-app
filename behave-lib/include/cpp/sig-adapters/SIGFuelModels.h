/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class for handling values associated with fuel models used in the
*           Rothermel model
* Author:   William Chatham <wchatham@fs.fed.us>
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

#include "behaveUnits.h"
#include <string>
#include <vector>
#include "fuelModels.h"
#include "SIGString.h"

class SIGFuelModels : public FuelModels
{
public:
    SIGFuelModels();
    SIGFuelModels& operator=(const SIGFuelModels& rhs);
    SIGFuelModels(const SIGFuelModels& rhs);

    bool setCustomFuelModel(int fuelModelNumber,
                            char* code,
                            char* name,
                            double fuelBedDepth,
                            LengthUnits::LengthUnitsEnum lengthUnits,
                            double moistureOfExtinctionDead,
                            FractionUnits::FractionUnitsEnum moistureUnits,
                            double heatOfCombustionDead,
                            double heatOfCombustionLive,
                            HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionUnits,
                            double fuelLoadOneHour,
                            double fuelLoadTenHour,
                            double fuelLoadHundredHour,
                            double fuelLoadLiveHerbaceous,
                            double fuelLoadLiveWoody,
                            LoadingUnits::LoadingUnitsEnum loadingUnits,
                            double savrOneHour,
                            double savrLiveHerbaceous,
                            double savrLiveWoody,
                            SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits,
                            bool isDynamic);

    char* getFuelCode(int fuelModelNumber) const;
    char* getFuelName(int fuelModelNumber) const;
};
