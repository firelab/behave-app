// Verifies SIGFuelModels::defineCustomFuelModel — the units-interleaved entry point — against the
// engine's own setCustomFuelModel. The only real logic in defineCustomFuelModel is unit conversion, so
// the test defines the same fuel model twice (once in non-base units, once in base units) and requires
// every getter to agree.

#include <cmath>
#include <iostream>
#include <string>
#include "SIGFuelModels.h"

constexpr double error_tolerance = 1e-06;

static int numFailed = 0;
static int numPassed = 0;

static void check(const std::string& testName, double observed, double expected)
{
    if (std::fabs(observed - expected) <= error_tolerance) {
        numPassed++;
    } else {
        numFailed++;
        std::cout << "FAILED " << testName << ": observed " << observed
                  << ", expected " << expected << std::endl;
    }
}

static void checkBool(const std::string& testName, bool observed, bool expected)
{
    if (observed == expected) {
        numPassed++;
    } else {
        numFailed++;
        std::cout << "FAILED " << testName << ": observed " << observed
                  << ", expected " << expected << std::endl;
    }
}

int main()
{
    const int fuelModelNumber = 221;

    SIGFuelModels viaDefine;
    SIGFuelModels viaSet;

    // Same model, expressed in non-base units through the new entry point ...
    const bool defineOk =
        viaDefine.defineCustomFuelModel(fuelModelNumber,
                                        (char*)"CH1",
                                        (char*)"Coastal Sage Chaparral",
                                        12.0, LengthUnits::Inches,                             // 1 ft
                                        25.0, FractionUnits::Percent,                          // 0.25
                                        8000.0, HeatOfCombustionUnits::BtusPerPound,
                                        9500.0, HeatOfCombustionUnits::BtusPerPound,
                                        3.0, LoadingUnits::TonsPerAcre,
                                        4.0, LoadingUnits::TonsPerAcre,
                                        1.0, LoadingUnits::TonsPerAcre,
                                        0.0, LoadingUnits::TonsPerAcre,
                                        5.0, LoadingUnits::TonsPerAcre,
                                        2000.0, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                        1600.0, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                        1500.0, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                        false);
    checkBool("defineCustomFuelModel returns true", defineOk, true);

    // ... and through the original, in base units.
    const double tonsPerAcreToPoundsPerSquareFoot = 2000.0 / 43560.0;
    const bool setOk =
        viaSet.setCustomFuelModel(fuelModelNumber,
                                  (char*)"CH1",
                                  (char*)"Coastal Sage Chaparral",
                                  1.0, LengthUnits::Feet,
                                  0.25, FractionUnits::Fraction,
                                  8000.0, 9500.0, HeatOfCombustionUnits::BtusPerPound,
                                  3.0 * tonsPerAcreToPoundsPerSquareFoot,
                                  4.0 * tonsPerAcreToPoundsPerSquareFoot,
                                  1.0 * tonsPerAcreToPoundsPerSquareFoot,
                                  0.0,
                                  5.0 * tonsPerAcreToPoundsPerSquareFoot,
                                  LoadingUnits::PoundsPerSquareFoot,
                                  2000.0, 1600.0, 1500.0,
                                  SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                  false);
    checkBool("setCustomFuelModel returns true", setOk, true);

    // Every property must agree.
    check("fuelbedDepth",
          viaDefine.getFuelbedDepth(fuelModelNumber, LengthUnits::Feet),
          viaSet.getFuelbedDepth(fuelModelNumber, LengthUnits::Feet));
    check("moistureOfExtinctionDead",
          viaDefine.getMoistureOfExtinctionDead(fuelModelNumber, FractionUnits::Fraction),
          viaSet.getMoistureOfExtinctionDead(fuelModelNumber, FractionUnits::Fraction));
    check("heatOfCombustionDead",
          viaDefine.getHeatOfCombustionDead(fuelModelNumber, HeatOfCombustionUnits::BtusPerPound),
          viaSet.getHeatOfCombustionDead(fuelModelNumber, HeatOfCombustionUnits::BtusPerPound));
    check("heatOfCombustionLive",
          viaDefine.getHeatOfCombustionLive(fuelModelNumber, HeatOfCombustionUnits::BtusPerPound),
          viaSet.getHeatOfCombustionLive(fuelModelNumber, HeatOfCombustionUnits::BtusPerPound));
    check("fuelLoadOneHour",
          viaDefine.getFuelLoadOneHour(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot),
          viaSet.getFuelLoadOneHour(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot));
    check("fuelLoadTenHour",
          viaDefine.getFuelLoadTenHour(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot),
          viaSet.getFuelLoadTenHour(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot));
    check("fuelLoadHundredHour",
          viaDefine.getFuelLoadHundredHour(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot),
          viaSet.getFuelLoadHundredHour(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot));
    check("fuelLoadLiveHerbaceous",
          viaDefine.getFuelLoadLiveHerbaceous(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot),
          viaSet.getFuelLoadLiveHerbaceous(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot));
    check("fuelLoadLiveWoody",
          viaDefine.getFuelLoadLiveWoody(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot),
          viaSet.getFuelLoadLiveWoody(fuelModelNumber, LoadingUnits::PoundsPerSquareFoot));
    check("savrOneHour",
          viaDefine.getSavrOneHour(fuelModelNumber, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet),
          viaSet.getSavrOneHour(fuelModelNumber, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet));
    check("savrLiveHerbaceous",
          viaDefine.getSavrLiveHerbaceous(fuelModelNumber, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet),
          viaSet.getSavrLiveHerbaceous(fuelModelNumber, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet));
    check("savrLiveWoody",
          viaDefine.getSavrLiveWoody(fuelModelNumber, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet),
          viaSet.getSavrLiveWoody(fuelModelNumber, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet));
    checkBool("isDynamic",
              viaDefine.getIsDynamic(fuelModelNumber),
              viaSet.getIsDynamic(fuelModelNumber));

    // Conversion actually happened: 1 ft, not 12.
    check("fuelbedDepth converted from inches",
          viaDefine.getFuelbedDepth(fuelModelNumber, LengthUnits::Feet), 1.0);
    check("moistureOfExtinctionDead converted from percent",
          viaDefine.getMoistureOfExtinctionDead(fuelModelNumber, FractionUnits::Fraction), 0.25);

    // A reserved slot must be refused — this is the error the SPA surfaces to the user.
    SIGFuelModels reserved;
    const bool reservedOk =
        reserved.defineCustomFuelModel(1,
                                       (char*)"XX1", (char*)"Reserved",
                                       1.0, LengthUnits::Feet,
                                       0.25, FractionUnits::Fraction,
                                       8000.0, HeatOfCombustionUnits::BtusPerPound,
                                       8000.0, HeatOfCombustionUnits::BtusPerPound,
                                       0.1, LoadingUnits::PoundsPerSquareFoot,
                                       0.0, LoadingUnits::PoundsPerSquareFoot,
                                       0.0, LoadingUnits::PoundsPerSquareFoot,
                                       0.0, LoadingUnits::PoundsPerSquareFoot,
                                       0.0, LoadingUnits::PoundsPerSquareFoot,
                                       2000.0, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                       1600.0, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                       1500.0, SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet,
                                       false);
    checkBool("reserved slot is refused", reservedOk, false);

    std::cout << "testFuelModels: " << numPassed << " passed, " << numFailed << " failed" << std::endl;
    return numFailed == 0 ? 0 : 1;
}
