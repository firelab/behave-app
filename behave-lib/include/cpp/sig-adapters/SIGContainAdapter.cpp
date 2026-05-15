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
#include <vector>

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
    std::vector<double> v(m_x, m_x + m_size);
    v.insert(v.end(), m_x, m_x + m_size);
    return( DoubleVector(v) );
}

DoubleVector SIGContainAdapter::getFirePerimeterY( void ) const
{
    std::vector<double> v(m_y, m_y + m_size);
    for (int i = 0; i < m_size; i++) { v.push_back(-m_y[i]); }
    return( DoubleVector(v) );
}

int SIGContainAdapter::getFirePerimeterPointCount( void ) const
{
    return( 2 * m_size );
}

DoubleVector SIGContainAdapter::getOptimizedContainProductionRates( void ) const
{
    return( DoubleVector(optimizedContainProductionRates_) );
}

DoubleVector SIGContainAdapter::getOptimizedContainAreas( void ) const
{
    return( DoubleVector(optimizedContainAreas_) );
}

int SIGContainAdapter::getOptimizedContainPointCount( void ) const
{
    return( (int)optimizedContainProductionRates_.size() );
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

double SIGContainAdapter::getAutoComputedResourceProductionRate(SpeedUnits::SpeedUnitsEnum speedUnits)
{
    return ( SpeedUnits::fromBaseUnits(autoComputedResourceProductionRate_, speedUnits ));
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

std::unique_ptr<Sem::ContainSim> SIGContainAdapter::runSimAtProductionRate(double productionRate,
                                                                            double resourceArrival,
                                                                            double resourceDuration,
                                                                            const char* desc)
{
    Sem::ContainForce force;
    force.addResource(resourceArrival,
                      productionRate,
                      resourceDuration,
                      Sem::ContainFlank::LeftFlank,
                      const_cast<char*>(desc));
    auto sim = std::unique_ptr<Sem::ContainSim>(
        new Sem::ContainSim(ContainAdapter::reportSize_,
                            ContainAdapter::reportRate_,
                            ContainAdapter::diurnalROS_,
                            ContainAdapter::fireStartTime_,
                            ContainAdapter::lwRatio_,
                            &force,
                            ContainAdapter::tactic_,
                            ContainAdapter::attackDistance_,
                            ContainAdapter::retry_,
                            ContainAdapter::minSteps_,
                            ContainAdapter::maxSteps_,
                            ContainAdapter::maxFireSize_,
                            ContainAdapter::maxFireTime_));
    sim->run();
    return sim;
}

void SIGContainAdapter::storeSimResults(Sem::ContainSim* sim)
{
    ContainAdapter::m_size       = sim->firePoints();
    ContainAdapter::m_x          = sim->firePerimeterX();
    ContainAdapter::m_y          = sim->firePerimeterY();
    ContainAdapter::m_reportHead = sim->fireHeadAtReport();
    ContainAdapter::m_reportBack = sim->fireBackAtReport();
    ContainAdapter::m_attackHead = sim->fireHeadAtAttack();
    ContainAdapter::m_attackBack = sim->fireBackAtAttack();

    ContainAdapter::finalCost_              = sim->finalFireCost();
    ContainAdapter::finalFireLineLength_    = LengthUnits::toBaseUnits(sim->finalFireLine(), LengthUnits::Chains);
    ContainAdapter::perimeterAtContainment_ = LengthUnits::toBaseUnits(sim->finalFirePerimeter(), LengthUnits::Chains);
    ContainAdapter::finalFireSize_          = AreaUnits::toBaseUnits(sim->finalFireSize(), AreaUnits::Acres);
    ContainAdapter::finalContainmentArea_   = AreaUnits::toBaseUnits(sim->finalFireSweep(), AreaUnits::Acres);
    ContainAdapter::finalTime_              = TimeUnits::toBaseUnits(sim->finalFireTime(), TimeUnits::Minutes);
    ContainAdapter::containmentStatus_      = static_cast<ContainStatus::ContainStatusEnum>(sim->status());
    ContainAdapter::finalProductionRate_    = finalFireLineLength_ / finalTime_;
}

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

        const char* desc = "AutoComputed";
        auto initialSim = runSimAtProductionRate(10000, resourceArrivalTime_, resourceDuration_, desc);
        ContainStatus::ContainStatusEnum initialStatus = convertSemStatusToAdapterStatus(initialSim->status());

        if (initialStatus == ContainStatus::ContainStatusEnum::Contained) {
            int left = 0;
            int right = 9999; // assumes 10000 chains per hour is the maximum
            std::unique_ptr<Sem::ContainSim> bestSim;
            while (left <= right) {
                int mid = (left + right) / 2;
                auto sim = runSimAtProductionRate(mid, resourceArrivalTime_, resourceDuration_, desc);
                if (convertSemStatusToAdapterStatus(sim->status()) == ContainStatus::ContainStatusEnum::Contained) {
                    right = mid - 1;
                    setAutoComputedResourceProductionRate(mid, SpeedUnits::ChainsPerHour);
                    bestSim = std::move(sim);
                } else {
                    left = mid + 1;
                }
            }
            storeSimResults(bestSim.get());

            // Sweep 0..floor(1.5 * optimal) to build production-rate vs containment-area curve
            optimizedContainProductionRates_.clear();
            optimizedContainAreas_.clear();
            int optimalChH = static_cast<int>(
                SpeedUnits::fromBaseUnits(autoComputedResourceProductionRate_, SpeedUnits::ChainsPerHour));
            int sweepMax = optimalChH + optimalChH / 2;
            for (int i = 0; i <= sweepMax; i++) {
                auto sweepSim = runSimAtProductionRate(static_cast<double>(i), resourceArrivalTime_, resourceDuration_, desc);
                optimizedContainProductionRates_.push_back(static_cast<double>(i));
                optimizedContainAreas_.push_back(sweepSim->finalFireSweep());
            }
        } else { // the Simulation is not contained even at maximum resource production rate (10000 ch/h).
            optimizedContainProductionRates_.clear();
            optimizedContainAreas_.clear();
            storeSimResults(initialSim.get());
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
        double firstArrivalTime = resourceArrivalTime_;
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

void SIGContainAdapter::setAutoComputedResourceProductionRate(double resourceProductionRate, SpeedUnits::SpeedUnitsEnum speedUnits)
{
    autoComputedResourceProductionRate_ = SpeedUnits::toBaseUnits(resourceProductionRate, speedUnits);
}
