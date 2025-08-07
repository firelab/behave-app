/******************************************************************************
 *
 * Project:  CodeBlocks
 * Purpose:  Interface for SIGContain to be used in Behave-like applications
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

#include "SIGContainAdapter.h"
#include "SIGContainAdapter.h"
#define _USE_MATH_DEFINES
#include <math.h>
#include <iostream>

void SIGContainAdapter::addResource(double arrival,
                                    TimeUnits::TimeUnitsEnum arrivalTimeUnits,
                                    double duration,
                                    TimeUnits::TimeUnitsEnum durationTimeUnits,
                                    double productionRate,
                                    SpeedUnits::SpeedUnitsEnum productionRateUnits,
                                    const char * description,
                                    double baseCost,
                                    double hourCost)
{
    double arrivalInMinutes = TimeUnits::toBaseUnits(arrival, arrivalTimeUnits);
    double durationInMinutes = TimeUnits::toBaseUnits(duration, durationTimeUnits);
    ContainAdapter::addResource(arrivalInMinutes,
                                durationInMinutes,
                                TimeUnits::Minutes,
                                productionRate,
                                productionRateUnits,
                                std::string(description),
                                baseCost,
                                hourCost);
}

int SIGContainAdapter::removeResourceWithThisDesc(const char * desc)
{
    return ContainAdapter::removeResourceWithThisDesc(std::string(desc));
}

int SIGContainAdapter::removeAllResourcesWithThisDesc(const char * desc)
{
    return force_.removeAllResourcesWithThisDesc(desc);
}

double SIGContainAdapter::getPerimeterAtInitialAttack(LengthUnits::LengthUnitsEnum lengthUnits)
{
    return ContainAdapter::getPerimiterAtInitialAttack(lengthUnits);
}

DoubleVector SIGContainAdapter::getFirePerimeterX( void ) const
{
    return( DoubleVector(m_x, m_size) );
}

DoubleVector SIGContainAdapter::getFirePerimeterY( void ) const
{
    return( DoubleVector(m_y, m_size) );
}

int SIGContainAdapter::getFirePerimeterPointCount( void ) const
{
    return( m_size );
}


double SIGContainAdapter::getFireHeadAtReport( void ) const
{
    return ( m_reportHead );
}

double SIGContainAdapter::getFireBackAtReport( void ) const
{
    return ( m_reportBack );
}

double SIGContainAdapter::getFireHeadAtAttack( void ) const
{
    return ( m_attackHead );
}

double SIGContainAdapter::getFireBackAtAttack( void ) const
{
    double v = m_attackBack;
    return (v);
}

double SIGContainAdapter::getLengthToWidthRatio ( void ) const
{
    return ( lwRatio_ );
}

double SIGContainAdapter::getAttackDistance ( LengthUnits::LengthUnitsEnum lengthUnits ) const
{
    return ( LengthUnits::toBaseUnits(attackDistance_, lengthUnits));
}

double SIGContainAdapter::getReportSize( AreaUnits::AreaUnitsEnum areaUnits ) const
{
    double reportSizeInSquareFeet = AreaUnits::toBaseUnits(reportSize_, AreaUnits::Acres); // convert report size to base units
    return ( AreaUnits::fromBaseUnits(reportSizeInSquareFeet, areaUnits));
}

double SIGContainAdapter::getReportRate( SpeedUnits::SpeedUnitsEnum speedUnits ) const
{
    double reportRateInFeetPerMinute = SpeedUnits::toBaseUnits(reportRate_, SpeedUnits::ChainsPerHour); // convert report rate to base units
    return ( SpeedUnits::fromBaseUnits(reportRateInFeetPerMinute, speedUnits));
}

int SIGContainAdapter::getTactic( void ) const
{
    return ( tactic_ );
}


void SIGContainAdapter::doContainRun()
{
    if (containMode_ == ContainMode::Default)
    {
        ContainAdapter::doContainRun();
    } else {
        doContainRunWithOptimalResource();
    }

}

// This is a modified version of the `doContainRun` function in
// `https://github.com/firelab/behave/blob/a2bc39a2c5adc7a510e00ce697c7fabc3b82bd69/src/behave/ContainAdapter.cpp#L137-L244`
// This function expects only a single Resource Arrival Time and Resource Duration to have been set
// and will perform multiple contain simulations to search for the minimial resource production rate
// that will cause the simulation to change from a Non Contained status to Contained. If such a scenario
// does not exists, then the model stores the simulation at the initial maximum Production Rate of 10000 ch/h.

void SIGContainAdapter::doContainRunWithOptimalResource()
{
    if (ContainAdapter::reportRate_ < 0.00001)
    {
        ContainAdapter::reportRate_ = 0.00001; // Contain algorithm can not deal with zero ROS
    }

    if (ContainAdapter::reportSize_ != 0)
    {
        for (int i = 0; i < 24; i++)
        {
            ContainAdapter::diurnalROS_[i] = ContainAdapter::reportRate_;
        }

        Sem::ContainForce oldForce;
        Sem::ContainForce* oldForcePointer = &oldForce;

        double resourceArrival = resourceArrivalTime_; // min
        double resourceDuration = resourceDuration_; // min
        double resourceProduction = 10000; // ch/h

        char* const desc = (char* const)"AutoComputed";
        oldForcePointer->addResource(resourceArrival,
                                     resourceProduction,
                                     resourceDuration,
                                     Sem::ContainFlank::LeftFlank,
                                     desc);

        Sem::ContainSim* initialContainSimPtr;
        initialContainSimPtr = new Sem::ContainSim(ContainAdapter::reportSize_,
                                                   ContainAdapter::reportRate_,
                                                   ContainAdapter::diurnalROS_,
                                                   ContainAdapter::fireStartTime_,
                                                   ContainAdapter::lwRatio_,
                                                   oldForcePointer,
                                                   ContainAdapter::tactic_,
                                                   ContainAdapter::attackDistance_,
                                                   ContainAdapter::retry_,
                                                   ContainAdapter::minSteps_,
                                                   ContainAdapter::maxSteps_,
                                                   ContainAdapter::maxFireSize_,
                                                   ContainAdapter::maxFireTime_);

        // Do Contain simulation
        initialContainSimPtr->run();

        int left = 0;
        int size = 10000; // assumes 10000 chains per hour is the maximum
        int right = size - 1;
        ContainStatus::ContainStatusEnum currentContainmentStatus;
        ContainStatus::ContainStatusEnum previousContainmentStatus;
        ContainStatus::ContainStatusEnum initialContainmentStatus;
        Sem::ContainSim* currentContainSimPtr;
        Sem::ContainSim* containSimPtrAtContainmentPtr;

        initialContainmentStatus = convertSemStatusToAdapterStatus(initialContainSimPtr->status());
        previousContainmentStatus = initialContainmentStatus;

        if (initialContainmentStatus == ContainStatus::ContainStatusEnum::Contained) {
            // run Binary Search for a contain simulation that uses the minimum production rate needed for containment.
            while (left <= right) {
                int mid = (left + right) / 2;
                // std::cout << "mid: " << mid << std::endl;
                int currentProductionRate = mid;
                Sem::ContainForce oldForce;
                Sem::ContainForce* oldForcePointer = &oldForce;
                oldForcePointer->addResource(resourceArrival,
                                             currentProductionRate,
                                             resourceDuration,
                                             Sem::ContainFlank::LeftFlank,
                                             desc);
                currentContainSimPtr = new Sem::ContainSim(ContainAdapter::reportSize_,
                                                           ContainAdapter::reportRate_,
                                                           ContainAdapter::diurnalROS_,
                                                           ContainAdapter::fireStartTime_,
                                                           ContainAdapter::lwRatio_,
                                                           oldForcePointer,
                                                           ContainAdapter::tactic_,
                                                           ContainAdapter::attackDistance_,
                                                           ContainAdapter::retry_,
                                                           ContainAdapter::minSteps_,
                                                           ContainAdapter::maxSteps_,
                                                           ContainAdapter::maxFireSize_,
                                                           ContainAdapter::maxFireTime_);

                // Do Contain simulation
                currentContainSimPtr->run();
                currentContainmentStatus = convertSemStatusToAdapterStatus(currentContainSimPtr->status());
                // std::cout << "currentContainmentStatus: " << currentContainmentStatus << std::endl;

                if ((previousContainmentStatus == ContainStatus::ContainStatusEnum::Contained &&
                     currentContainmentStatus == ContainStatus::ContainStatusEnum::Contained )
                    ||
                    (previousContainmentStatus != ContainStatus::ContainStatusEnum::Contained &&
                     currentContainmentStatus == ContainStatus::ContainStatusEnum::Contained))
                {
                    // std::cout << "search left" <<  std::endl;
                    right = mid - 1; //search left half
                    containSimPtrAtContainmentPtr = currentContainSimPtr;
                    previousContainmentStatus = currentContainmentStatus;
                }
                else if ((previousContainmentStatus == ContainStatus::ContainStatusEnum::Contained &&
                          currentContainmentStatus != ContainStatus::ContainStatusEnum::Contained)
                         ||
                         (previousContainmentStatus != ContainStatus::ContainStatusEnum::Contained &&
                          currentContainmentStatus != ContainStatus::ContainStatusEnum::Contained))
                {
                    // std::cout << "search right" <<  std::endl;
                    left = mid + 1; // Search right half
                    previousContainmentStatus = currentContainmentStatus;
                }
            }

            // Store Values from ContainSim For Access in SIGContainAdapter
            ContainAdapter::m_size       = containSimPtrAtContainmentPtr->firePoints();
            ContainAdapter::m_x          = containSimPtrAtContainmentPtr->firePerimeterX();
            ContainAdapter::m_y          = containSimPtrAtContainmentPtr->firePerimeterY();
            ContainAdapter::m_reportHead = containSimPtrAtContainmentPtr->fireHeadAtReport();
            ContainAdapter::m_reportBack = containSimPtrAtContainmentPtr->fireBackAtReport();
            ContainAdapter::m_attackHead = containSimPtrAtContainmentPtr->fireHeadAtAttack();
            ContainAdapter::m_attackBack = containSimPtrAtContainmentPtr->fireBackAtAttack();

            // Get results from Contain simulation
            ContainAdapter::finalCost_ = containSimPtrAtContainmentPtr->finalFireCost();
            ContainAdapter::finalFireLineLength_ = LengthUnits::toBaseUnits(containSimPtrAtContainmentPtr->finalFireLine(), LengthUnits::Chains);
            ContainAdapter::perimeterAtContainment_ = LengthUnits::toBaseUnits(containSimPtrAtContainmentPtr->finalFirePerimeter(), LengthUnits::Chains);
            ContainAdapter::finalFireSize_ = AreaUnits::toBaseUnits(containSimPtrAtContainmentPtr->finalFireSize(), AreaUnits::Acres);
            ContainAdapter::finalContainmentArea_ = AreaUnits::toBaseUnits(containSimPtrAtContainmentPtr->finalFireSweep(), AreaUnits::Acres);
            ContainAdapter::finalTime_ = TimeUnits::toBaseUnits(containSimPtrAtContainmentPtr->finalFireTime(), TimeUnits::Minutes);
            ContainAdapter::containmentStatus_ = convertSemStatusToAdapterStatus(containSimPtrAtContainmentPtr->status());
            ContainAdapter::containmentStatus_ = static_cast<ContainStatus::ContainStatusEnum>(containSimPtrAtContainmentPtr->status());
            ContainAdapter::finalProductionRate_ = finalFireLineLength_ / finalTime_;



        } else { // the Simulation is not cointained even at maximum  resource production rate (10000 ch/h).
            // Store Values from initialCoontainSim
            ContainAdapter::m_size       = initialContainSimPtr->firePoints();
            ContainAdapter::m_x          = initialContainSimPtr->firePerimeterX();
            ContainAdapter::m_y          = initialContainSimPtr->firePerimeterY();
            ContainAdapter::m_reportHead = initialContainSimPtr->fireHeadAtReport();
            ContainAdapter::m_reportBack = initialContainSimPtr->fireBackAtReport();
            ContainAdapter::m_attackHead = initialContainSimPtr->fireHeadAtAttack();
            ContainAdapter::m_attackBack = initialContainSimPtr->fireBackAtAttack();

            // Get results from Contain simulation
            ContainAdapter::finalCost_ = initialContainSimPtr->finalFireCost();
            ContainAdapter::finalFireLineLength_ = LengthUnits::toBaseUnits(initialContainSimPtr->finalFireLine(), LengthUnits::Chains);
            ContainAdapter::perimeterAtContainment_ = LengthUnits::toBaseUnits(initialContainSimPtr->finalFirePerimeter(), LengthUnits::Chains);
            ContainAdapter::finalFireSize_ = AreaUnits::toBaseUnits(initialContainSimPtr->finalFireSize(), AreaUnits::Acres);
            ContainAdapter::finalContainmentArea_ = AreaUnits::toBaseUnits(initialContainSimPtr->finalFireSweep(), AreaUnits::Acres);
            ContainAdapter::finalTime_ = TimeUnits::toBaseUnits(initialContainSimPtr->finalFireTime(), TimeUnits::Minutes);
            ContainAdapter::containmentStatus_ = convertSemStatusToAdapterStatus(initialContainSimPtr->status());
            ContainAdapter::containmentStatus_ = static_cast<ContainStatus::ContainStatusEnum>(initialContainSimPtr->status());
            ContainAdapter::finalProductionRate_ = finalFireLineLength_ / finalTime_;


        }

        // Calculate effective windspeed needed for Size module
        // Find the effective windspeed
        double effectiveWindspeed = 4.0 * (lwRatio_ - 1.0);
        ContainAdapter::size_.calculateFireBasicDimensions(false, effectiveWindspeed, SpeedUnits::MilesPerHour, reportRate_, SpeedUnits::ChainsPerHour);
        // Find the time elapsed to created the fire at time of report
        LengthUnits::LengthUnitsEnum lengthUnits = LengthUnits::Feet;
        double elapsedTime = 1.0;
        double ellipticalA = size_.getEllipticalA(lengthUnits, elapsedTime, TimeUnits::Minutes); // get base elliptical dimensions
        double ellipticalB = size_.getEllipticalB(lengthUnits, elapsedTime, TimeUnits::Minutes); // get base elliptical dimensions

        // Equation for area of ellipse used in Size Module (calculateFireArea() in fireSize.cpp)
        // A = pi*a*b*s^2
        double reportSizeInSquareFeet = AreaUnits::toBaseUnits(reportSize_, AreaUnits::Acres);
        double intialElapsedTime = 0.0; // time for the fire to get to the reported size
        double totalElapsedTime = 0.0;
        ContainAdapter::perimeterAtInitialAttack_ = 0.0;
        ContainAdapter::fireSizeAtIntitialAttack_ = 0.0;
        double denominator = M_PI * ellipticalA * ellipticalB; // pi*a*b

        // Get the time that the first resource begins to attack the fire
        double firstArrivalTime = resourceArrival;
        if (firstArrivalTime < 0)
        {
            firstArrivalTime = 0.0; // make sure the time isn't negative for some weird reason
        }

        // Solve for seconds elapsed for reported fire size to reach its size at time of report assuming constant rate of growth

        if (denominator > 1.0e-07)
        {
            intialElapsedTime = sqrt(reportSizeInSquareFeet / denominator); // s = sqrt(A/(pi*a*b))
            totalElapsedTime = intialElapsedTime + firstArrivalTime;
            // Use total time elapsed to solve for perimeter and area of fire at time of initial attack
            LengthUnits::LengthUnitsEnum lengthUnits = LengthUnits::Feet;
            ContainAdapter::perimeterAtInitialAttack_ = size_.getFirePerimeter(false, lengthUnits, totalElapsedTime, TimeUnits::Minutes);
            AreaUnits::AreaUnitsEnum areaUnits = AreaUnits::SquareFeet;
            ContainAdapter::fireSizeAtIntitialAttack_ = size_.getFireArea(false, areaUnits, totalElapsedTime, TimeUnits::Minutes);
        }
    }
}

void SIGContainAdapter::setContainMode(ContainMode containMode)
{
    containMode_ = containMode;
}

void SIGContainAdapter::setResourceArrivalTime(double arrivalTime, TimeUnits::TimeUnitsEnum timeUnits)
{
    resourceArrivalTime_ = TimeUnits::toBaseUnits(arrivalTime, timeUnits);
}

void  SIGContainAdapter::setResourceDuration(double duration, TimeUnits::TimeUnitsEnum timeUnits)
{
    resourceDuration_ = TimeUnits::toBaseUnits(duration, timeUnits);
}
