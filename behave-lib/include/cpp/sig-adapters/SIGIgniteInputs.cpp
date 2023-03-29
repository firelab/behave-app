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

#include "SIGIgniteInputs.h"

SIGIgniteInputs::SIGIgniteInputs()
{
    moistureOneHour_ = 0.0;
    moistureHundredHour_ = 0.0;
    airTemperature_ = 0.0;
    sunShade_ = 0.0;
    fuelBedType_ = PonderosaPineLitter;
    duffDepth_ = 0.0;

    lightningChargeType_ = Unknown;
}

SIGIgniteInputs::~SIGIgniteInputs()
{

}

void SIGIgniteInputs::initializeMembers()
{
    moistureOneHour_ = 0.0;
    moistureHundredHour_ = 0.0;
    airTemperature_ = 0.0;
    sunShade_ = 0.0;
    fuelBedType_ = PonderosaPineLitter;
    duffDepth_ = 0.0;

    lightningChargeType_ = Unknown;
}

void SIGIgniteInputs::updateIgniteInputs(double moistureOneHour, double moistureHundredHour, MoistureUnits::MoistureUnitsEnum moistureUnits,
    double airTemperature, TemperatureUnits::TemperatureUnitsEnum temperatureUnits, double sunShade, CoverUnits::CoverUnitsEnum sunShadeUnits,
    IgnitionFuelBedType fuelBedType, double duffDepth, LengthUnits::LengthUnitsEnum duffDepthUnits,
    LightningCharge lightningChargeType)
{
    setMoistureOneHour(moistureOneHour, moistureUnits);
    setMoistureHundredHour(moistureHundredHour, moistureUnits);
    setAirTemperature(airTemperature, temperatureUnits);
    setSunShade(sunShade, sunShadeUnits);
    fuelBedType_ = fuelBedType;
    setDuffDepth(duffDepth, duffDepthUnits);
    lightningChargeType_ = lightningChargeType;
}

void SIGIgniteInputs::setMoistureOneHour(double moistureOneHour, MoistureUnits::MoistureUnitsEnum moistureUnits)
{
    moistureOneHour_ = MoistureUnits::toBaseUnits(moistureOneHour, moistureUnits);
}

void SIGIgniteInputs::setMoistureHundredHour(double moistureHundredHour, MoistureUnits::MoistureUnitsEnum moistureUnits)
{
    moistureHundredHour_ = MoistureUnits::toBaseUnits(moistureHundredHour, moistureUnits);
}

void SIGIgniteInputs::setAirTemperature(double airTemperature, TemperatureUnits::TemperatureUnitsEnum temperatureUnits)
{
    airTemperature_ = TemperatureUnits::toBaseUnits(airTemperature, temperatureUnits);
}

void SIGIgniteInputs::setSunShade(double sunShade, CoverUnits::CoverUnitsEnum sunShadeUnits)
{
    sunShade_ = CoverUnits::toBaseUnits(sunShade, sunShadeUnits);
}

void SIGIgniteInputs::setIgnitionFuelBedType(IgnitionFuelBedType fuelBedType)
{
    fuelBedType_ = fuelBedType;
}

void SIGIgniteInputs::setDuffDepth(double duffDepth, LengthUnits::LengthUnitsEnum lengthUnits)
{
    duffDepth_ = LengthUnits::toBaseUnits(duffDepth, lengthUnits);
}

void SIGIgniteInputs::setLightningChargeType(LightningCharge lightningChargeType)
{
    lightningChargeType_ = lightningChargeType;
}

double SIGIgniteInputs::getAirTemperature(TemperatureUnits::TemperatureUnitsEnum desiredUnits)
{
    return TemperatureUnits::fromBaseUnits(airTemperature_, desiredUnits);
}

double SIGIgniteInputs::getMoistureOneHour(MoistureUnits::MoistureUnitsEnum desiredUnits)
{
    return MoistureUnits::fromBaseUnits(moistureOneHour_, desiredUnits);
}

double SIGIgniteInputs::getMoistureHundredHour(MoistureUnits::MoistureUnitsEnum desiredUnits)
{
    return MoistureUnits::fromBaseUnits(moistureHundredHour_, desiredUnits);
}

double SIGIgniteInputs::getSunShade(CoverUnits::CoverUnitsEnum desiredUnits)
{
    return CoverUnits::fromBaseUnits(sunShade_, desiredUnits);
}

IgnitionFuelBedType SIGIgniteInputs::getIgnitionFuelBedType()
{
    return fuelBedType_;
}

double SIGIgniteInputs::getDuffDepth(LengthUnits::LengthUnitsEnum desiredUnits)
{
    return LengthUnits::fromBaseUnits(duffDepth_,desiredUnits);
}

LightningCharge SIGIgniteInputs::getLightningChargeType()
{
    return lightningChargeType_;
}

