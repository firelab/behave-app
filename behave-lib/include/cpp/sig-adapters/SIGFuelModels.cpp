/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class for handling values associated with fuel models used in the
*           Rothermel model
* Author:   William Chatham <wchatham@fs.fed.us>
* Author:   Richard Sheperd <rsheperd@sig-gis.com>
* Credits:  Some of the code in this file is, in part or in whole, from
*           BehavePlus5 source originally authored by Collin D. Bevins and is
*           used with or without modification.
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

#include "SIGFuelModels.h"

SIGFuelModels::SIGFuelModels() : FuelModels() {}

// TODO: Implement similar to SI

SIGFuelModels& SIGFuelModels::operator=(const SIGFuelModels& rhs) {
  FuelModels::operator=(rhs);
}

SIGFuelModels::SIGFuelModels(const SIGFuelModels& rhs) : FuelModels::FuelModels(static_cast <FuelModels> (rhs)) {}

bool SIGFuelModels::setCustomFuelModel(int fuelModelNumber,
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
                                       bool isDynamic)
{
  return FuelModels::setCustomFuelModel(fuelModelNumber,
                                        std::string(code),
                                        std::string(name),
                                        fuelBedDepth,
                                        lengthUnits,
                                        moistureOfExtinctionDead,
                                        moistureUnits,
                                        heatOfCombustionDead,
                                        heatOfCombustionLive,
                                        heatOfCombustionUnits,
                                        fuelLoadOneHour,
                                        fuelLoadTenHour,
                                        fuelLoadHundredHour,
                                        fuelLoadLiveHerbaceous,
                                        fuelLoadLiveWoody,
                                        loadingUnits,
                                        savrOneHour,
                                        savrLiveHerbaceous,
                                        savrLiveWoody,
                                        savrUnits,
                                        isDynamic);
}

bool SIGFuelModels::defineCustomFuelModel(int fuelModelNumber,
                                          char* code,
                                          char* name,
                                          double fuelBedDepth,
                                          LengthUnits::LengthUnitsEnum fuelBedDepthUnits,
                                          double moistureOfExtinctionDead,
                                          FractionUnits::FractionUnitsEnum moistureOfExtinctionDeadUnits,
                                          double heatOfCombustionDead,
                                          HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionDeadUnits,
                                          double heatOfCombustionLive,
                                          HeatOfCombustionUnits::HeatOfCombustionUnitsEnum heatOfCombustionLiveUnits,
                                          double fuelLoadOneHour,
                                          LoadingUnits::LoadingUnitsEnum fuelLoadOneHourUnits,
                                          double fuelLoadTenHour,
                                          LoadingUnits::LoadingUnitsEnum fuelLoadTenHourUnits,
                                          double fuelLoadHundredHour,
                                          LoadingUnits::LoadingUnitsEnum fuelLoadHundredHourUnits,
                                          double fuelLoadLiveHerbaceous,
                                          LoadingUnits::LoadingUnitsEnum fuelLoadLiveHerbaceousUnits,
                                          double fuelLoadLiveWoody,
                                          LoadingUnits::LoadingUnitsEnum fuelLoadLiveWoodyUnits,
                                          double savrOneHour,
                                          SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrOneHourUnits,
                                          double savrLiveHerbaceous,
                                          SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrLiveHerbaceousUnits,
                                          double savrLiveWoody,
                                          SurfaceAreaToVolumeUnits::SurfaceAreaToVolumeUnitsEnum savrLiveWoodyUnits,
                                          bool isDynamic)
{
  // Each value is converted to its base unit here, because the engine's setCustomFuelModel accepts only
  // one unit per property class (one LoadingUnits for all five loads, one SurfaceAreaToVolumeUnits for
  // all three SAVRs, one HeatOfCombustionUnits for both heats of combustion). Converting up front lets
  // every property carry its own unit, and the base-unit enums below make the engine's own conversion
  // a no-op.
  const double fuelBedDepthBase =
      LengthUnits::toBaseUnits(fuelBedDepth, fuelBedDepthUnits);
  const double moistureOfExtinctionDeadBase =
      FractionUnits::toBaseUnits(moistureOfExtinctionDead, moistureOfExtinctionDeadUnits);
  const double heatOfCombustionDeadBase =
      HeatOfCombustionUnits::toBaseUnits(heatOfCombustionDead, heatOfCombustionDeadUnits);
  const double heatOfCombustionLiveBase =
      HeatOfCombustionUnits::toBaseUnits(heatOfCombustionLive, heatOfCombustionLiveUnits);
  const double fuelLoadOneHourBase =
      LoadingUnits::toBaseUnits(fuelLoadOneHour, fuelLoadOneHourUnits);
  const double fuelLoadTenHourBase =
      LoadingUnits::toBaseUnits(fuelLoadTenHour, fuelLoadTenHourUnits);
  const double fuelLoadHundredHourBase =
      LoadingUnits::toBaseUnits(fuelLoadHundredHour, fuelLoadHundredHourUnits);
  const double fuelLoadLiveHerbaceousBase =
      LoadingUnits::toBaseUnits(fuelLoadLiveHerbaceous, fuelLoadLiveHerbaceousUnits);
  const double fuelLoadLiveWoodyBase =
      LoadingUnits::toBaseUnits(fuelLoadLiveWoody, fuelLoadLiveWoodyUnits);
  const double savrOneHourBase =
      SurfaceAreaToVolumeUnits::toBaseUnits(savrOneHour, savrOneHourUnits);
  const double savrLiveHerbaceousBase =
      SurfaceAreaToVolumeUnits::toBaseUnits(savrLiveHerbaceous, savrLiveHerbaceousUnits);
  const double savrLiveWoodyBase =
      SurfaceAreaToVolumeUnits::toBaseUnits(savrLiveWoody, savrLiveWoodyUnits);

  return FuelModels::setCustomFuelModel(fuelModelNumber,
                                        std::string(code),
                                        std::string(name),
                                        fuelBedDepthBase,
                                        LengthUnits::Feet,
                                        moistureOfExtinctionDeadBase,
                                        FractionUnits::Fraction,
                                        heatOfCombustionDeadBase,
                                        heatOfCombustionLiveBase,
                                        HeatOfCombustionUnits::BtusPerPound,
                                        fuelLoadOneHourBase,
                                        fuelLoadTenHourBase,
                                        fuelLoadHundredHourBase,
                                        fuelLoadLiveHerbaceousBase,
                                        fuelLoadLiveWoodyBase,
                                        LoadingUnits::PoundsPerSquareFoot,
                                        savrOneHourBase,
                                        savrLiveHerbaceousBase,
                                        savrLiveWoodyBase,
                                        SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                        isDynamic);
}

char* SIGFuelModels::getFuelCode(int fuelModelNumber) const
{
    return SIGString::str2charptr(FuelModels::getFuelCode(fuelModelNumber));
}

char* SIGFuelModels::getFuelName(int fuelModelNumber) const
{
    return SIGString::str2charptr(FuelModels::getFuelName(fuelModelNumber));
}
