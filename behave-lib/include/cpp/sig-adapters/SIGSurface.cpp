/******************************************************************************
 *
 * Project:  CodeBlocks
 * Purpose:  Class for handling surface fire behavior based on the Facade OOP
 *           Design Pattern and using the Rothermel spread model
 * Author:   William Chatham <wchatham@fs.fed.us>
 * Author:   Richard J. Sheperd <rsheperd@sig-gis.com>
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

#include "surface.h"
#include "SIGSurface.h"

#include "surfaceTwoFuelModels.h"
#include "surfaceInputs.h"
#include "SIGString.h"

SIGSurface::SIGSurface(SIGFuelModels& fuelModels) : Surface(fuelModels) {}

SIGSurface::~SIGSurface() {
  Surface::~Surface();
};

char* SIGSurface::getFuelCode(int fuelModelNumber) const
{
  return SIGString::str2charptr(Surface::getFuelCode(fuelModelNumber));
}

char* SIGSurface::getFuelName(int fuelModelNumber) const
{
  return SIGString::str2charptr(Surface::getFuelName(fuelModelNumber));
}

ChaparralFuelLoadInputMode::ChaparralFuelInputLoadModeEnum SIGSurface::getChaparralFuelLoadInputMode() {
  return surfaceInputs_.getChaparralFuelLoadInputMode();
}

double SIGSurface::getChaparralLoadDeadFine(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Dead, 0) , loadingUnits);
}

double SIGSurface::getChaparralLoadDeadSmall(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Dead, 1) , loadingUnits);
}

double SIGSurface::getChaparralLoadDeadMedium(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Dead, 2) , loadingUnits);
}

double SIGSurface::getChaparralLoadDeadLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Dead, 3) , loadingUnits);
}

double SIGSurface::getChaparralLoadDeadVeryLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Dead, 4) , loadingUnits);
}

double SIGSurface::getChaparralLoadLiveFine(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Live, 0) , loadingUnits);
}

double SIGSurface::getChaparralLoadLiveSmall(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Live, 1) , loadingUnits);
}

double SIGSurface::getChaparralLoadLiveMedium(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Live, 2) , loadingUnits);
}

double SIGSurface::getChaparralLoadLiveLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Live, 3) , loadingUnits);
}

double SIGSurface::getChaparralLoadLiveLarge(LoadingUnits::LoadingUnitsEnum loadingUnits) const {
  return LoadingUnits::fromBaseUnits(surfaceFire_.getChaparralLoad(FuelLifeState::Live, 4) , loadingUnits);
}
