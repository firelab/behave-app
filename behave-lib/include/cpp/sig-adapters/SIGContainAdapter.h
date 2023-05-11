/******************************************************************************
*
* Project:  CodeBlocks
* Purpose:  Interface for Contain to be used in Behave-like applications
*           used to tie together the classes used in a Contain simulation
* Authors:  William Chatham <wchatham@fs.fed.us>
*           Richard Sheperd <rsheperd@sig-gis.com>
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

#ifndef _SIGCONTAINADAPTER_H_
#define _SIGCONTAINADAPTER_H_

#include "SIGContain.h"
#include "SIGContainSim.h"
#include "SIGContainForceAdapter.h"
#include "SIGDiurnalROS.h"
#include "SIGCollections.h"
#include "behaveUnits.h"
#include "fireSize.h"

class SIGContainAdapter
{
public:
    SIGContainAdapter();
    ~SIGContainAdapter();

    void addResource(SIGContainResource& resource);
    // Construct ContainResource into ContainForce
    void addResource(
        double arrival,
        double duration,
        TimeUnits::TimeUnitsEnum timeUnit,
        double productionRate,
        SpeedUnits::SpeedUnitsEnum productionRateUnits,
        const char * description = "",
        double baseCost = 0.0,
        double hourCost = 0.0);
    int removeResourceAt(int index);
    int removeResourceWithThisDesc(const char * desc);
    int removeAllResourcesWithThisDesc(const char * desc);
    void removeAllResources();

    void setReportSize(double reportSize, AreaUnits::AreaUnitsEnum areaUnits);
    void setReportRate(double reportRate, SpeedUnits::SpeedUnitsEnum speedUnits);
    void setFireStartTime(int fireStartTime);
    void setLwRatio(double lwRatio);
    void setTactic(ContainTactic tactic);
    void setAttackDistance(double attackDistance, LengthUnits::LengthUnitsEnum lengthUnits);
    void setRetry(bool retry);
    void setMinSteps(int minSteps);
    void setMaxSteps(int maxSteps);
    void setMaxFireSize(int maxFireSize);
    void setMaxFireTime(int maxFireTime);

    void doContainRun();

    double getFinalCost() const;
    double getFinalFireLineLength(LengthUnits::LengthUnitsEnum lengthUnits) const;
    double getPerimeterAtInitialAttack(LengthUnits::LengthUnitsEnum lengthUnits) const;
    double getPerimeterAtContainment(LengthUnits::LengthUnitsEnum lengthUnits) const;
    double getFireSizeAtInitialAttack(AreaUnits::AreaUnitsEnum areaUnits) const;
    double getFinalFireSize(AreaUnits::AreaUnitsEnum areaUnits) const;
    double getFinalContainmentArea(AreaUnits::AreaUnitsEnum areaUnits) const;
    double getFinalTimeSinceReport(TimeUnits::TimeUnitsEnum timeUnits) const;
    ContainStatus getContainmentStatus() const;

    DoubleVector firePerimeterX( void ) const;
    DoubleVector firePerimeterY( void ) const;
    int firePoints( void ) const;

private:
    FireSize size_;

    // Contain Inputs
    double reportSize_;
    double reportRate_;
    int fireStartTime_;
    double lwRatio_;
    SIGContainForceAdapter force_;
    ContainTactic tactic_;
    double attackDistance_;
    bool retry_;
    int minSteps_;
    int maxSteps_;
    int maxFireSize_;
    int maxFireTime_;

    // Intermediate Variables
    SIGDiurnalROS * diurnalROS_;
    DoubleVector m_x;           //!< Array of perimeter x coordinates (ch)
    DoubleVector m_y;           //!< Array of perimeter y coordinates (ch)
    int  m_size;            //!< Size of the arrays (m_maxSteps or 2*m_maxSteps)

    // Contain Outputs
    double finalCost_; // Final total cost of all resources used
    double finalFireLineLength_;  // Final fire line at containment or escape
    double perimeterAtInitialAttack_; // firee perimeter at time of initial attack
    double perimeterAtContainment_; // Final line plus fire perimeter at containment or escape
    double fireSizeAtIntitialAttack_; // Fire size (area) at time of initial attack
    double finalFireSize_; // Final fire size (area) at containment or escape
    double finalContainmentArea_; // Final containment area at containment or escape
    double finalTime_; // Containment or escape time since report
    ContainStatus containmentStatus_;

};

#endif //CONTAINADAPTER_H
