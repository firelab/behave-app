/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class for handling surface fire behavior based on the Facade OOP
*           Design Pattern and using the Rothermel spread model
* Author:   William Chatham <wchatham@fs.fed.us>
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

#include "SIGSurface.h"

#include "surfaceTwoFuelModels.h"
#include "surfaceInputs.h"

SIGSurface::SIGSurface(const FuelModels& fuelModels)
    : surfaceInputs_(),
    surfaceFire_(fuelModels, surfaceInputs_, size_)
{
    fuelModels_ = &fuelModels;
}

// Copy Ctor
SIGSurface::SIGSurface(const SIGSurface& rhs)
    : surfaceFire_()
{
    memberwiseCopyAssignment(rhs);
}

SIGSurface& SIGSurface::operator=(const SIGSurface& rhs)
{
    if (this != &rhs)
    {
        memberwiseCopyAssignment(rhs);
    }
    return *this;
}

void SIGSurface::memberwiseCopyAssignment(const SIGSurface& rhs)
{
    surfaceInputs_ = rhs.surfaceInputs_;
    surfaceFire_ = rhs.surfaceFire_;
    size_ = rhs.size_;
}

bool SIGSurface::isAllFuelLoadZero(int fuelModelNumber)
{
   return fuelModels_->isAllFuelLoadZero(fuelModelNumber);
}

void SIGSurface::doSurfaceRunInDirectionOfMaxSpread()
{
    double directionOfInterest = -1; // dummy value
    bool hasDirectionOfInterest = false;
    if (isUsingTwoFuelModels())
    {
        // Calculate spread rate for Two Fuel Models
        SurfaceTwoFuelModels surfaceTwoFuelModels(surfaceFire_);
        TwoFuelModelsMethod::TwoFuelModelsMethodEnum twoFuelModelsMethod = surfaceInputs_.getTwoFuelModelsMethod();
        int firstFuelModelNumber = surfaceInputs_.getFirstFuelModelNumber();
        double firstFuelModelCoverage = surfaceInputs_.getFirstFuelModelCoverage();
        int secondFuelModelNumber = surfaceInputs_.getSecondFuelModelNumber();
        surfaceTwoFuelModels.calculateWeightedSpreadRate(twoFuelModelsMethod, firstFuelModelNumber, firstFuelModelCoverage, 
            secondFuelModelNumber, hasDirectionOfInterest, directionOfInterest);
    }
    else // Use only one fuel model
    {
        // Calculate spread rate
        int fuelModelNumber = surfaceInputs_.getFuelModelNumber();
        if (isAllFuelLoadZero(fuelModelNumber) || !fuelModels_->isFuelModelDefined(fuelModelNumber))
        {
            // No fuel to burn, spread rate is zero
            surfaceFire_.skipCalculationForZeroLoad();
        }
        else
        {
            // Calculate spread rate
            surfaceFire_.calculateForwardSpreadRate(fuelModelNumber, hasDirectionOfInterest, directionOfInterest);
        }
    }
}

void SIGSurface::doSurfaceRunInDirectionOfInterest(double directionOfInterest)
{
    bool hasDirectionOfInterest = true;
    if (isUsingTwoFuelModels())
    {
        // Calculate spread rate for Two Fuel Models
        SurfaceTwoFuelModels surfaceTwoFuelModels(surfaceFire_);
        TwoFuelModelsMethod::TwoFuelModelsMethodEnum  twoFuelModelsMethod = surfaceInputs_.getTwoFuelModelsMethod();
        int firstFuelModelNumber = surfaceInputs_.getFirstFuelModelNumber();
        double firstFuelModelCoverage = surfaceInputs_.getFirstFuelModelCoverage();
        int secondFuelModelNumber = surfaceInputs_.getSecondFuelModelNumber();
        surfaceTwoFuelModels.calculateWeightedSpreadRate(twoFuelModelsMethod, firstFuelModelNumber, firstFuelModelCoverage,
            secondFuelModelNumber, hasDirectionOfInterest, directionOfInterest);
    }
    else // Use only one fuel model
    {   
        int fuelModelNumber = surfaceInputs_.getFuelModelNumber();
        if (isAllFuelLoadZero(fuelModelNumber) || !fuelModels_->isFuelModelDefined(fuelModelNumber))
        {
            // No fuel to burn, spread rate is zero
            surfaceFire_.skipCalculationForZeroLoad();
        }
        else
        {
            // Calculate spread rate
            surfaceFire_.calculateForwardSpreadRate(fuelModelNumber, hasDirectionOfInterest, directionOfInterest);
        }
    }
}

double SIGSurface::calculateFlameLength(double firelineIntensity)
{
    return surfaceFire_.calculateFlameLength(firelineIntensity);
}

void SIGSurface::setFuelModels(FuelModels& fuelModels)
{
    fuelModels_ = &fuelModels;
}

void SIGSurface::initializeMembers()
{
    surfaceFire_.initializeMembers();
    surfaceInputs_.initializeMembers();
}

double SIGSurface::calculateSpreadRateAtVector(double directionOfinterest)
{
    return surfaceFire_.calculateSpreadRateAtVector(directionOfinterest);
}

double SIGSurface::getSpreadRate(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const
{
    return SpeedUnits::fromBaseUnits(surfaceFire_.getSpreadRate(), spreadRateUnits);
}

double SIGSurface::getSpreadRateInDirectionOfInterest(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const
{
    return SpeedUnits::fromBaseUnits(surfaceFire_.getSpreadRateInDirectionOfInterest(), spreadRateUnits);
}

double SIGSurface::getDirectionOfMaxSpread() const
{
    double directionOfMaxSpread = surfaceFire_.getDirectionOfMaxSpread();
    return directionOfMaxSpread;
}

double SIGSurface::getFlameLength(LengthUnits::LengthUnitsEnum flameLengthUnits) const
{
    return LengthUnits::fromBaseUnits(surfaceFire_.getFlameLength(), flameLengthUnits);
}

double SIGSurface::getFireLengthToWidthRatio() const
{
    return size_.getFireLengthToWidthRatio();
}

double SIGSurface::getFireEccentricity() const
{
    return size_.getEccentricity();
}

double SIGSurface::getFirelineIntensity(FirelineIntensityUnits::FirelineIntensityUnitsEnum firelineIntensityUnits) const
{
    return FirelineIntensityUnits::fromBaseUnits(surfaceFire_.getFirelineIntensity(), firelineIntensityUnits);
}

double SIGSurface::getHeatPerUnitArea(HeatPerUnitAreaUnits::HeatPerUnitAreaUnitsEnum heatPerUnitAreaUnits) const
{
    return HeatPerUnitAreaUnits::fromBaseUnits(surfaceFire_.getHeatPerUnitArea(), heatPerUnitAreaUnits);
}

double SIGSurface::getResidenceTime(TimeUnits::TimeUnitsEnum timeUnits) const
{
    return TimeUnits::fromBaseUnits(surfaceFire_.getResidenceTime(), timeUnits);
}

double SIGSurface::getReactionIntensity(HeatSourceAndReactionIntensityUnits::HeatSourceAndReactionIntensityUnitsEnum reactiontionIntensityUnits) const
{
    return HeatSourceAndReactionIntensityUnits::fromBaseUnits(surfaceFire_.getReactionIntensity(), reactiontionIntensityUnits);
}

double SIGSurface::getMidflameWindspeed(SpeedUnits::SpeedUnitsEnum spreadRateUnits) const
{
    return SpeedUnits::fromBaseUnits(surfaceFire_.getMidflameWindSpeed(), spreadRateUnits);
}

double SIGSurface::getEllipticalA(LengthUnits::LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits) const
{
    return size_.getEllipticalA(lengthUnits, elapsedTime, timeUnits);
}

double SIGSurface::getEllipticalB(LengthUnits::LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits) const
{
    return size_.getEllipticalB(lengthUnits, elapsedTime, timeUnits);
}

double SIGSurface::getEllipticalC(LengthUnits::LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits) const
{
    return size_.getEllipticalC(lengthUnits, elapsedTime, timeUnits);
}

double SIGSurface::getSlopeFactor() const
{
    return surfaceFire_.getSlopeFactor();
}

double SIGSurface::getBulkDensity(DensityUnits::DensityUnitsEnum densityUnits) const
{
    return DensityUnits::fromBaseUnits(surfaceFire_.getBulkDensity(), densityUnits);
}

double SIGSurface::getHeatSink(HeatSinkUnits::HeatSinkUnitsEnum heatSinkUnits) const
{
    return HeatSinkUnits::fromBaseUnits(surfaceFire_.getHeatSink(), heatSinkUnits);
}

double SIGSurface::getFirePerimeter(LengthUnits::LengthUnitsEnum lengthUnits , double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits) const
{
    return size_.getFirePerimeter(lengthUnits, elapsedTime, timeUnits);
}

double SIGSurface::getFireArea(AreaUnits::AreaUnitsEnum areaUnits, double elapsedTime, TimeUnits::TimeUnitsEnum timeUnits) const
{
    return size_.getFireArea(areaUnits, elapsedTime, timeUnits);
}

void SIGSurface::setCanopyCover(double canopyCover, CoverUnits::CoverUnitsEnum coverUnits)
{
    surfaceInputs_.setCanopyCover(canopyCover, coverUnits);
}

void SIGSurface::setCanopyHeight(double canopyHeight, LengthUnits::LengthUnitsEnum canopyHeightUnits)
{
    surfaceInputs_.setCanopyHeight(canopyHeight, canopyHeightUnits);
}

void SIGSurface::setCrownRatio(double crownRatio)
{
    surfaceInputs_.setCrownRatio(crownRatio);
}

char* SIGSurface::getFuelCode(int fuelModelNumber) const
{
    std::string val = fuelModels_->getFuelCode(fuelModelNumber);
    char* c_val = new char[val.length() + 1];
    std::strcpy(c_val, val.c_str());
    return c_val;
}

char* SIGSurface::getFuelName(int fuelModelNumber) const
{
    std::string val = fuelModels_->getFuelName(fuelModelNumber);
    char* c_val = new char[val.length() + 1];
    std::strcpy(c_val, val.c_str());
    return c_val;
}

double SIGSurface::getFuelbedDepth(int fuelModelNumber, LengthUnits::LengthUnitsEnum lengthUnits) const
{
    return fuelModels_->getFuelbedDepth(fuelModelNumber, lengthUnits);
}

double SIGSurface::getFuelMoistureOfExtinctionDead(int fuelModelNumber, MoistureUnits::MoistureUnitsEnum moistureUnits) const
{
    return fuelModels_->getMoistureOfExtinctionDead(fuelModelNumber, moistureUnits);
}

double SIGSurface::getFuelHeatOfCombustionDead(int fuelModelNumber, HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionUnits) const
{
    return fuelModels_->getHeatOfCombustionDead(fuelModelNumber, heatOfCombustionUnits);
}

double SIGSurface::getFuelHeatOfCombustionLive(int fuelModelNumber, HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionUnits) const
{
    return fuelModels_->getHeatOfCombustionLive(fuelModelNumber, heatOfCombustionUnits);
}

double SIGSurface::getFuelLoadOneHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadOneHour(fuelModelNumber, loadingUnits);
}

double SIGSurface::getFuelLoadTenHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadTenHour(fuelModelNumber, loadingUnits);
}

double SIGSurface::getFuelLoadHundredHour(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadHundredHour(fuelModelNumber, loadingUnits);
}

double SIGSurface::getFuelLoadLiveHerbaceous(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadLiveHerbaceous(fuelModelNumber, loadingUnits);
}

double SIGSurface::getFuelLoadLiveWoody(int fuelModelNumber, LoadingUnits::LoadingUnitsEnum loadingUnits) const
{
    return fuelModels_->getFuelLoadLiveWoody(fuelModelNumber, loadingUnits);
}

double SIGSurface::getFuelSavrOneHour(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const
{
    return fuelModels_->getSavrOneHour(fuelModelNumber, savrUnits);
}

double SIGSurface::getFuelSavrLiveHerbaceous(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const
{
    return fuelModels_->getSavrLiveHerbaceous(fuelModelNumber, savrUnits);
}

double SIGSurface::getFuelSavrLiveWoody(int fuelModelNumber, SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrUnits) const
{
    return fuelModels_->getSavrLiveWoody(fuelModelNumber, savrUnits);
}

bool SIGSurface::isFuelDynamic(int fuelModelNumber) const
{
    return fuelModels_->getIsDynamic(fuelModelNumber);
}

bool SIGSurface::isFuelModelDefined(int fuelModelNumber) const
{
    return fuelModels_->isFuelModelDefined(fuelModelNumber);
}

bool SIGSurface::isFuelModelReserved(int fuelModelNumber) const
{
    return fuelModels_->isFuelModelReserved(fuelModelNumber);
}

bool SIGSurface::isAllFuelLoadZero(int fuelModelNumber) const
{
    return fuelModels_->isAllFuelLoadZero(fuelModelNumber);
}

bool SIGSurface::isUsingTwoFuelModels() const
{
    return surfaceInputs_.isUsingTwoFuelModels();
}

int SIGSurface::getFuelModelNumber() const
{
	return surfaceInputs_.getFuelModelNumber();
}

double SIGSurface::getMoistureOneHour(MoistureUnits::MoistureUnitsEnum moistureUnits) const
{
    return surfaceInputs_.getMoistureOneHour(moistureUnits);
}

double SIGSurface::getMoistureTenHour(MoistureUnits::MoistureUnitsEnum moistureUnits) const
{
    return surfaceInputs_.getMoistureTenHour(moistureUnits);
}

double SIGSurface::getMoistureHundredHour(MoistureUnits::MoistureUnitsEnum moistureUnits) const
{
    return surfaceInputs_.getMoistureHundredHour(moistureUnits);
}

double SIGSurface::getMoistureLiveHerbaceous(MoistureUnits::MoistureUnitsEnum moistureUnits) const
{
    return surfaceInputs_.getMoistureLiveHerbaceous(moistureUnits);
}

double SIGSurface::getMoistureLiveWoody(MoistureUnits::MoistureUnitsEnum moistureUnits) const
{
    return surfaceInputs_.getMoistureLiveWoody(moistureUnits);
}

double SIGSurface::getCanopyCover(CoverUnits::CoverUnitsEnum coverUnits) const
{
    return CoverUnits::fromBaseUnits(surfaceInputs_.getCanopyCover(), coverUnits);
}

double SIGSurface::getCanopyHeight(LengthUnits::LengthUnitsEnum canopyHeightUnits) const
{
    return LengthUnits::fromBaseUnits(surfaceInputs_.getCanopyHeight(), canopyHeightUnits);
}

double SIGSurface::getCrownRatio() const
{
    return surfaceInputs_.getCrownRatio();
}

WindAndSpreadOrientationMode::WindAndSpreadOrientationModeEnum SIGSurface::getWindAndSpreadOrientationMode() const
{
    return surfaceInputs_.getWindAndSpreadOrientationMode();
}

WindHeightInputMode::WindHeightInputModeEnum SIGSurface::getWindHeightInputMode() const
{
    return surfaceInputs_.getWindHeightInputMode();
}

WindAdjustmentFactorCalculationMethod::WindAdjustmentFactorCalculationMethodEnum SIGSurface::getWindAdjustmentFactorCalculationMethod() const
{
    return surfaceInputs_.getWindAdjustmentFactorCalculationMethod();
}

double SIGSurface::getWindSpeed(SpeedUnits::SpeedUnitsEnum windSpeedUnits, 
    WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode) const
{
    double midFlameWindSpeed = surfaceFire_.getMidflameWindSpeed();
    double windSpeed = midFlameWindSpeed;
    if (windHeightInputMode == WindHeightInputMode::DirectMidflame)
    {
        windSpeed = midFlameWindSpeed;
    }
    else 
    {
        double windAdjustmentFactor = surfaceFire_.getWindAdjustmentFactor();
    
        if ((windHeightInputMode == WindHeightInputMode::TwentyFoot) && (windAdjustmentFactor > 0.0))
        {
            windSpeed = midFlameWindSpeed / windAdjustmentFactor;
        }
        else // Ten Meter
        {
            if (windAdjustmentFactor > 0.0)
            {
                windSpeed = (midFlameWindSpeed / windAdjustmentFactor) * 1.15;
            }
        }
    }
    return SpeedUnits::fromBaseUnits(windSpeed, windSpeedUnits);
}

double SIGSurface::getWindDirection() const
{
    return surfaceInputs_.getWindDirection();
}

double SIGSurface::getSlope(SlopeUnits::SlopeUnitsEnum slopeUnits) const
{
    return SlopeUnits::fromBaseUnits(surfaceInputs_.getSlope(), slopeUnits);
}

double SIGSurface::getAspect() const
{
    return surfaceInputs_.getAspect();
}

void SIGSurface::setFuelModelNumber(int fuelModelNumber)
{
    surfaceInputs_.setFuelModelNumber(fuelModelNumber);
}

void SIGSurface::setMoistureOneHour(double moistureOneHour, MoistureUnits::MoistureUnitsEnum moistureUnits)
{
    surfaceInputs_.setMoistureOneHour(moistureOneHour, moistureUnits);
}

void SIGSurface::setMoistureTenHour(double moistureTenHour, MoistureUnits::MoistureUnitsEnum moistureUnits)
{
    surfaceInputs_.setMoistureTenHour(moistureTenHour, moistureUnits);
}

void SIGSurface::setMoistureHundredHour(double moistureHundredHour, MoistureUnits::MoistureUnitsEnum moistureUnits)
{
    surfaceInputs_.setMoistureHundredHour(moistureHundredHour, moistureUnits);
}

void SIGSurface::setMoistureLiveHerbaceous(double moistureLiveHerbaceous, MoistureUnits::MoistureUnitsEnum moistureUnits)
{
    surfaceInputs_.setMoistureLiveHerbaceous(moistureLiveHerbaceous, moistureUnits);
}

void SIGSurface::setMoistureLiveWoody(double moistureLiveWoody, MoistureUnits::MoistureUnitsEnum moistureUnits)
{
    surfaceInputs_.setMoistureLiveWoody(moistureLiveWoody, moistureUnits);
}

void SIGSurface::setSlope(double slope, SlopeUnits::SlopeUnitsEnum slopeUnits)
{
    surfaceInputs_.setSlope(slope, slopeUnits);
}

void SIGSurface::setAspect(double aspect)
{
    surfaceInputs_.setAspect(aspect);
}

void SIGSurface::setWindSpeed(double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode)
{
    surfaceInputs_.setWindSpeed(windSpeed, windSpeedUnits, windHeightInputMode);
    surfaceFire_.calculateMidflameWindSpeed();
}

void SIGSurface::setUserProvidedWindAdjustmentFactor(double userProvidedWindAdjustmentFactor)
{
    surfaceInputs_.setUserProvidedWindAdjustmentFactor(userProvidedWindAdjustmentFactor);
}

void SIGSurface::setWindDirection(double windDirection)
{
    surfaceInputs_.setWindDirection(windDirection);
}

void SIGSurface::setWindAndSpreadOrientationMode(WindAndSpreadOrientationMode::WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode)
{
    surfaceInputs_.setWindAndSpreadOrientationMode(windAndSpreadOrientationMode);
}

void SIGSurface::setWindHeightInputMode(WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode)
{
    surfaceInputs_.setWindHeightInputMode(windHeightInputMode);
}

void SIGSurface::setFirstFuelModelNumber(int firstFuelModelNumber)
{
    surfaceInputs_.setFirstFuelModelNumber(firstFuelModelNumber);
}

void SIGSurface::setSecondFuelModelNumber(int secondFuelModelNumber)
{
    surfaceInputs_.setSecondFuelModelNumber(secondFuelModelNumber);
}

void SIGSurface::setTwoFuelModelsMethod(TwoFuelModelsMethod::TwoFuelModelsMethodEnum  twoFuelModelsMethod)
{
    surfaceInputs_.setTwoFuelModelsMethod(twoFuelModelsMethod);
}

void SIGSurface::setTwoFuelModelsFirstFuelModelCoverage(double firstFuelModelCoverage, CoverUnits::CoverUnitsEnum coverUnits)
{
    surfaceInputs_.setTwoFuelModelsFirstFuelModelCoverage(firstFuelModelCoverage, coverUnits);
}

void SIGSurface::setWindAdjustmentFactorCalculationMethod(WindAdjustmentFactorCalculationMethod::WindAdjustmentFactorCalculationMethodEnum windAdjustmentFactorCalculationMethod)
{
    surfaceInputs_.setWindAdjustmentFactorCalculationMethod(windAdjustmentFactorCalculationMethod);
}

void SIGSurface::updateSurfaceInputs(int fuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour,
    double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits::MoistureUnitsEnum moistureUnits, double windSpeed, 
    SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode, 
    double windDirection, WindAndSpreadOrientationMode::WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode,
    double slope, SlopeUnits::SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits::CoverUnitsEnum coverUnits, double canopyHeight,
    LengthUnits::LengthUnitsEnum canopyHeightUnits, double crownRatio)
{
    surfaceInputs_.updateSurfaceInputs(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous,
        moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode,
        slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
    surfaceFire_.calculateMidflameWindSpeed();
}

void SIGSurface::updateSurfaceInputsForTwoFuelModels(int firstfuelModelNumber, int secondFuelModelNumber, double moistureOneHour,
    double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody,
    MoistureUnits::MoistureUnitsEnum moistureUnits, double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits,
    WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode, double windDirection,
    WindAndSpreadOrientationMode::WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double firstFuelModelCoverage,
    CoverUnits::CoverUnitsEnum firstFuelModelCoverageUnits, TwoFuelModelsMethod::TwoFuelModelsMethodEnum twoFuelModelsMethod,
    double slope, SlopeUnits::SlopeUnitsEnum slopeUnits, double aspect, double canopyCover,
    CoverUnits::CoverUnitsEnum canopyCoverUnits, double canopyHeight, LengthUnits::LengthUnitsEnum canopyHeightUnits, double crownRatio)
{
    surfaceInputs_.updateSurfaceInputsForTwoFuelModels(firstfuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour,
        moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, 
        windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope,
        slopeUnits, aspect, canopyCover,canopyCoverUnits, canopyHeight, canopyHeightUnits, crownRatio);
    surfaceFire_.calculateMidflameWindSpeed();
}

void SIGSurface::updateSurfaceInputsForPalmettoGallbery(double moistureOneHour, double moistureTenHour, double moistureHundredHour,
    double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits::MoistureUnitsEnum moistureUnits, double windSpeed, 
    SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode, double windDirection,
    WindAndSpreadOrientationMode::WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double ageOfRough,
    double heightOfUnderstory, double palmettoCoverage, double overstoryBasalArea, double slope, SlopeUnits::SlopeUnitsEnum slopeUnits,
    double aspect, double canopyCover, CoverUnits::CoverUnitsEnum coverUnits, double canopyHeight, LengthUnits::LengthUnitsEnum canopyHeightUnits, double crownRatio)
{
    surfaceInputs_.updateSurfaceInputsForPalmettoGallbery(moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous,
        moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode,
        ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, slope, slopeUnits, aspect, canopyCover, coverUnits,
        canopyHeight, canopyHeightUnits, crownRatio);
    surfaceFire_.calculateMidflameWindSpeed();
}

void SIGSurface::updateSurfaceInputsForWesternAspen(int aspenFuelModelNumber, double aspenCuringLevel, 
    AspenFireSeverity::AspenFireSeverityEnum aspenFireSeverity, double DBH, double moistureOneHour, double moistureTenHour, 
    double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits::MoistureUnitsEnum moistureUnits,
    double windSpeed, SpeedUnits::SpeedUnitsEnum windSpeedUnits, WindHeightInputMode::WindHeightInputModeEnum windHeightInputMode,
    double windDirection, WindAndSpreadOrientationMode::WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope,
    SlopeUnits::SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits::CoverUnitsEnum coverUnits, double canopyHeight,
    LengthUnits::LengthUnitsEnum canopyHeightUnits, double crownRatio)
{
    surfaceInputs_.updateSurfaceInputsForWesternAspen(aspenFuelModelNumber, aspenCuringLevel, aspenFireSeverity, DBH, moistureOneHour,
        moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits,
        windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits,
        canopyHeight, canopyHeightUnits, crownRatio);
    surfaceFire_.calculateMidflameWindSpeed();
}
