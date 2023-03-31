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

#include "fuelModels.h"
#include "SIGFuelModels.h"
#include "surfaceInputs.h"
#include "SIGString.h"

SIGFuelModels::SIGFuelModels() : FuelModels() {}

// TODO: Implement similar to SI
SIGFuelModels::SIGFuelModels& operator=(const SIGFuelModels& rhs) {
    if(this != &rhs)
    {
      static_cast SIGFuelModels FuelModels::memberwiseCopyAssignment(static_cast FuelModels rhs);
    }
    return *this;
}

SIGFuelModels::SIGFuelModels(const SIGFuelModels& rhs) {
  return static_cast SIGFuelModels FuelModels::FuelModels(static_cast FuelModels rhs);
}

SIGFuelModels::~SIGFuelModels();

void SIGFuelModels::setFuelModelRecord(int fuelModelNumber, char* c_code, char* c_name,
                                       double fuelBedDepth, double moistureOfExtinctionDead, double heatOfCombustionDead, double heatOfCombustionLive,
                                       double fuelLoadOneHour, double fuelLoadTenHour, double fuelLoadHundredHour, double fuelLoadliveHerbaceous,
                                       double fuelLoadliveWoody, double savrOneHour, double savrLiveHerbaceous, double savrLiveWoody,
                                       bool isDynamic, bool isReserved)
{
  FuelModels::setFuelModelRecord(fuelModelNumber, std::string(c_code), std::string(c_name),
                                 fuelBedDepth, moistureOfExtinctionDead, heatOfCombustionDead, heatOfCombustionLive,
                                 fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadliveHerbaceous,
                                 fuelLoadliveWoody, savrOneHour, savrLiveHerbaceous, savrLiveWoody,
                                 isDynamic, isReserved);
}

char* SIGFuelModels::getFuelCode(int fuelModelNumber) const
{
    return SIGString::str2charptr(FuelModels::getFuelCode(fuelModelNumber));
}

char* SIGFuelModels::getFuelName(int fuelModelNumber) const
{
    return SIGString::str2charptr(FuelModels::getFuelCode(fuelModelNumber));
}
