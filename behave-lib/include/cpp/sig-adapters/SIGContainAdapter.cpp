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

void SIGContainAdapter::addResource(double arrival, double duration, TimeUnits::TimeUnitsEnum timeUnits, double productionRate, SpeedUnits::SpeedUnitsEnum productionRateUnits, const char * description, double baseCost, double hourCost)
{
  ContainAdapter::addResource(arrival,
                              duration,
                              timeUnits,
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


void SIGContainAdapter::doContainRun()
{
  if (reportRate_ < 0.00001)
    {
      reportRate_ = 0.00001; // Contain algorithm can not deal with zero ROS
    }

  if (force_.resourceVector.size() > 0 && reportSize_ != 0)
    {
      for (int i = 0; i < 24; i++)
        {
          diurnalROS_[i] = reportRate_;
        }

      double  resourceArrival;
      double  resourceBaseCost;
      double  resourceCost;
      std::string resourceDescription;
      double  resourceDuration;
      Sem::ContainFlank resourceFlank;
      double  resourceHourCost;
      double  resourceProduction;

      Sem::ContainForce oldForce;
      Sem::ContainForce* oldForcePointer = &oldForce;
      for (int i = 0; i < force_.resourceVector.size(); i++)
        {
          resourceArrival = force_.resourceVector[i].arrival();
          resourceBaseCost = force_.resourceVector[i].baseCost();
          resourceDescription = force_.resourceVector[i].description();
          resourceDuration = force_.resourceVector[i].duration();
          resourceFlank = force_.resourceVector[i].flank();
          resourceHourCost = force_.resourceVector[i].hourCost();
          resourceProduction = force_.resourceVector[i].production();

          char* const desc = (char* const)resourceDescription.c_str();
          oldForcePointer->addResource(resourceArrival, resourceProduction, resourceDuration, resourceFlank,
                                       desc, resourceBaseCost, resourceHourCost);
        }

      Sem::ContainSim containSim(reportSize_, reportRate_, diurnalROS_, fireStartTime_, lwRatio_,
                                 oldForcePointer, tactic_, attackDistance_, retry_, minSteps_, maxSteps_, maxFireSize_,
                                 maxFireTime_);

      // Do Contain simulation
      containSim.run();

      // Store Values from ContainSim For Access in SIGContainAdapter
      m_size = containSim.firePoints();
      m_x = DoubleVector(containSim.firePerimeterX(), m_size);
      m_y = DoubleVector(containSim.firePerimeterY(), m_size);
      m_reportHead = containSim.fireHeadAtReport();
      m_reportBack = containSim.fireBackAtReport();
      m_attackHead = containSim.fireHeadAtAttack();
      m_attackBack = containSim.fireBackAtAttack();

      // Get results from Contain simulation
      finalCost_ = containSim.finalFireCost();
      finalFireLineLength_ = LengthUnits::toBaseUnits(containSim.finalFireLine(), LengthUnits::Chains);
      perimeterAtContainment_ = LengthUnits::toBaseUnits(containSim.finalFirePerimeter(), LengthUnits::Chains);
      finalFireSize_ = AreaUnits::toBaseUnits(containSim.finalFireSize(), AreaUnits::Acres);
      finalContainmentArea_ = AreaUnits::toBaseUnits(containSim.finalFireSweep(), AreaUnits::Acres);
      finalTime_ = TimeUnits::toBaseUnits(containSim.finalFireTime(), TimeUnits::Minutes);
      containmentStatus_ = convertSemStatusToAdapterStatus(containSim.status());
      containmentStatus_ = static_cast<ContainStatus::ContainStatusEnum>(containSim.status());

      // Calculate effective windspeed needed for Size module
      // Find the effective windspeed
      double effectiveWindspeed = 4.0 * (lwRatio_ - 1.0);
      size_.calculateFireBasicDimensions(false, effectiveWindspeed, SpeedUnits::MilesPerHour, reportRate_, SpeedUnits::ChainsPerHour);
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
      perimeterAtInitialAttack_ = 0.0;
      fireSizeAtIntitialAttack_ = 0.0;
      double denominator = M_PI * ellipticalA * ellipticalB; // pi*a*b

      // Get the time that the first resource begins to attack the fire
      double firstArrivalTime = force_.firstArrival(Sem::ContainFlank::LeftFlank);
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
          perimeterAtInitialAttack_ = size_.getFirePerimeter(false, lengthUnits, totalElapsedTime, TimeUnits::Minutes);
          AreaUnits::AreaUnitsEnum areaUnits = AreaUnits::SquareFeet;
          fireSizeAtIntitialAttack_ = size_.getFireArea(false, areaUnits, totalElapsedTime, TimeUnits::Minutes);
        }
    }
}

double SIGContainAdapter::getPerimeterAtInitialAttack(LengthUnits::LengthUnitsEnum lengthUnits)
{
  return ContainAdapter::getPerimiterAtInitialAttack(lengthUnits);
}

DoubleVector SIGContainAdapter::getFirePerimeterX( void ) const
{
  return( m_x );
}

DoubleVector SIGContainAdapter::getFirePerimeterY( void ) const
{
  return( m_y );
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
  return ( m_attackBack );
}

double SIGContainAdapter::getLengthToWidthRatio ( void ) const
{
  return ( lwRatio_ );
}
