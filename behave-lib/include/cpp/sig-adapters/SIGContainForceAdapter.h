/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Class to handle resource vector required for Contain simulations.
*           Allows for original Contain source files to be used without
*           modification with a C++ interface to be used in Behave-like
*           applications
* Author:   William Chatham <wchatham@fs.fed.us>
* Author:   Richard Sheperd <rsheperd@sig-gis.com>
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

#ifndef CONTAINFORCEADAPTER_H
#define CONTAINFORCEADAPTER_H

#include <string>
#include <vector>
#include "SIGContainResource.h"

class SIGContainForceAdapter
{
public:
    SIGContainForceAdapter();
    ~SIGContainForceAdapter();

    void addResource(SIGContainResource& resource);
    void addResource(double arrival, double production, double duration,
        ContainFlank flank, const char * desc, double baseCost, double hourCost);
    double firstArrival(ContainFlank flank) const;

    int removeResourceAt(int index);
    int removeResourceWithThisDesc(const char * desc);
    int removeAllResourcesWithThisDesc(const char * desc);

    std::vector<SIGContainResource> resourceVector;
private:
};

#endif // CONTAINFORCEADAPTER_H

