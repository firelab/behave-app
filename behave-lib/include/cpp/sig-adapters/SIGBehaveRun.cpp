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

#include "SIGBehaveRun.h"

#include "SIGFuelModels.h"

SIGBehaveRun::SIGBehaveRun(SIGFuelModels& fuelModels, SpeciesMasterTable& speciesMasterTable)
    : surface(fuelModels),
    crown(fuelModels),
    mortality(speciesMasterTable)
{
    fuelModels_ = &fuelModels;
    speciesMasterTable_ = &speciesMasterTable;
}

SIGBehaveRun::SIGBehaveRun(const SIGBehaveRun& rhs)
    : surface(*rhs.fuelModels_),
    crown(*rhs.fuelModels_),
    mortality(*rhs.speciesMasterTable_)
{
    memberwiseCopyAssignment(rhs);
}

SIGBehaveRun& SIGBehaveRun::operator=(const SIGBehaveRun& rhs)
{
    if (this != &rhs)
    {
        memberwiseCopyAssignment(rhs);
    }
    return *this;
}

void SIGBehaveRun::memberwiseCopyAssignment(const SIGBehaveRun& rhs)
{
    setFuelModels(*rhs.fuelModels_);
    surface = rhs.surface;
    crown = rhs.crown;
    spot = rhs.spot;
}

SIGBehaveRun::~SIGBehaveRun()
{

}

void SIGBehaveRun::reinitialize()
{
    surface.initializeMembers();
    crown.initializeMembers();
    spot.initializeMembers();
    ignite.initializeMembers();
    safety.initializeMembers();
}

void SIGBehaveRun::setFuelModels(SIGFuelModels& fuelModels)
{
    // makes this behaveRun's fuelModels_ point to the FuelModels given to this method as a parameter
    fuelModels_ = &fuelModels;
    surface.setFuelModels(fuelModels);
    crown.setFuelModels(fuelModels);
}

char* SIGBehaveRun::getFuelCode(int fuelModelNumber) const
{
    return fuelModels_->getFuelCode(fuelModelNumber);
}

char* SIGBehaveRun::getFuelName(int fuelModelNumber) const
{
    return fuelModels_->getFuelName(fuelModelNumber);
}

double SIGBehaveRun::getFuelbedDepth(int fuelModelNumber, LengthUnits::LengthUnitsEnum lengthUnits) const
{
    return fuelModels_->getFuelbedDepth(fuelModelNumber, lengthUnits);
}

double SIGBehaveRun::getFuelMoistureOfExtinctionDead(int fuelModelNumber, MoistureUnits::MoistureUnitsEnum moistureUnits) const
{
    return fuelModels_->getMoistureOfExtinctionDead(fuelModelNumber, moistureUnits);
}

double SIGBehaveRun::getFuelHeatOfCombustionDead(int fuelModelNumber, HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionUnits) const
{
    return fuelModels_->getHeatOfCombustionDead(fuelModelNumber, heatOfCombustionUnits);
}

double SIGBehaveRun::getFuelHeatOfCombustionLive(int fuelModelNumber, HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionUnits) const
{
    return fuelModels_->getHeatOfCombustionLive(fuelModelNumber, heatOfCombustionUnits);
}

double SIGBehaveRun::getFuelLoadOneHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadOneHour(fuelModelNumber, loadingUnits);
}

double SIGBehaveRun::getFuelLoadTenHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadTenHour(fuelModelNumber, loadingUnits);
}

double SIGBehaveRun::getFuelLoadHundredHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadHundredHour(fuelModelNumber, loadingUnits);
}

double SIGBehaveRun::getFuelLoadLiveHerbaceous(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadLiveHerbaceous(fuelModelNumber, loadingUnits);
}

double SIGBehaveRun::getFuelLoadLiveWoody(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadLiveWoody(fuelModelNumber, loadingUnits);
}

double SIGBehaveRun::getFuelSavrOneHour(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const
{
    return fuelModels_->getSavrOneHour(fuelModelNumber, savrUnits);
}

double SIGBehaveRun::getFuelSavrLiveHerbaceous(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const
{
    return fuelModels_->getSavrLiveHerbaceous(fuelModelNumber, savrUnits);
}

double SIGBehaveRun::getFuelSavrLiveWoody(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const
{
    return fuelModels_->getSavrLiveWoody(fuelModelNumber, savrUnits);
}

bool SIGBehaveRun::isFuelDynamic(int fuelModelNumber) const
{
    return fuelModels_->getIsDynamic(fuelModelNumber);
}

bool SIGBehaveRun::isFuelModelDefined(int fuelModelNumber) const
{
    return fuelModels_->isFuelModelDefined(fuelModelNumber);
}

bool SIGBehaveRun::isFuelModelReserved(int fuelModelNumber) const
{
    return fuelModels_->isFuelModelReserved(fuelModelNumber);
}

bool SIGBehaveRun::isAllFuelLoadZero(int fuelModelNumber) const
{
    return fuelModels_->isAllFuelLoadZero(fuelModelNumber);
}
