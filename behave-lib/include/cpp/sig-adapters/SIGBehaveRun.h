/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Interface for Behave application based on the Facade OOP Design
*           Pattern used to tie together the modules and objects used by Behave
* Author:   William Chatham <wchatham@fs.fed.us>
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
#include "SIGFuelModels.h"
#include "SIGContainAdapter.h"
#include "SIGCrown.h"
#include "SIGIgnite.h"
#include "SIGMortality.h"
#include "safety.h"
#include "SIGSpot.h"
#include "SIGSurface.h"
#include "SIGString.h"

class SIGFuelModels;

class SIGBehaveRun
{
public:
    SIGBehaveRun() = delete; // There is no default constructor
    explicit SIGBehaveRun(SIGFuelModels& fuelModels, SpeciesMasterTable& speciesMasterTable);

    SIGBehaveRun(const SIGBehaveRun& rhs);
    SIGBehaveRun& operator=(const SIGBehaveRun& rhs);
    ~SIGBehaveRun();

    void reinitialize();

    void setFuelModels(SIGFuelModels& fuelModels);
    void setMoistureScenarios(SIGMoistureScenarios& moistureScenarios);

    // Fuel Model Getter Methods
    char* getFuelCode(int fuelModelNumber) const;
    char* getFuelName(int fuelModelNumber) const;
    double getFuelbedDepth(int fuelModelNumber, LengthUnits::LengthUnitsEnum lengthUnits) const;
    double getFuelMoistureOfExtinctionDead(int fuelModelNumber, FractionUnits::FractionUnitsEnum moistureUnits) const;;
    double getFuelHeatOfCombustionDead(int fuelModelNumber, HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionUnits) const;
    double getFuelHeatOfCombustionLive(int fuelModelNumber, HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionUnits) const;
    double getFuelLoadOneHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const;
    double getFuelLoadTenHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const;
    double getFuelLoadHundredHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const;
    double getFuelLoadLiveHerbaceous(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const;
    double getFuelLoadLiveWoody(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const;
    double getFuelSavrOneHour(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const;
    double getFuelSavrLiveHerbaceous(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const;
    double getFuelSavrLiveWoody(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const;
    bool isFuelDynamic(int fuelModelNumber) const;
    bool isFuelModelDefined(int fuelModelNumber) const;
    bool isFuelModelReserved(int fuelModelNumber) const;
    bool isAllFuelLoadZero(int fuelModelNumber) const;

    // SURFACE Module
    SIGSurface surface;

    // CROWN Module
    SIGCrown crown;

    // SPOT Module
    SIGSpot spot;

    //  Ignite Module
    SIGIgnite ignite;

    //  Contain Module
    SIGContainAdapter contain;

    //  Safety Module
    Safety safety;

    // Mortality Module
    SIGMortality mortality;

 private:
    void memberwiseCopyAssignment(const SIGBehaveRun& rhs);

    // Fuel models (orginal 13, 40 and custom)
    SIGFuelModels* fuelModels_;

    // Tree species data for Mortality Module
    SpeciesMasterTable* speciesMasterTable_;
};
