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

#include "ContainAdapter.h"
#include "SIGCollections.h"

enum class ContainMode {
    Default,
    ComputeWithOptimalResource
};

class SIGContainAdapter : public ContainAdapter
{
public:

  // Inputs
  void addResource(double arrival,
                   TimeUnits::TimeUnitsEnum arrivalTimeUnit,
                   double duration,
                   TimeUnits::TimeUnitsEnum durationTimeUnit,
                   double productionRate,
                   SpeedUnits::SpeedUnitsEnum productionRateUnits,
                   const char * description = "",
                   double baseCost = 0.0,
                   double hourCost = 0.0);
  int removeResourceWithThisDesc(const char * desc);
  int removeAllResourcesWithThisDesc(const char * desc);

  //TODO PR to behave to fix mispelled perimiter
  double getPerimeterAtInitialAttack(LengthUnits::LengthUnitsEnum lengthUnits);

  DoubleVector getFirePerimeterX( void ) const;
  DoubleVector getFirePerimeterY( void ) const;
  int getFirePerimeterPointCount( void ) const;

  double getFireBackAtReport( void ) const;
  double getFireHeadAtReport( void ) const;
  double getFireHeadAtAttack( void ) const;
  double getFireBackAtAttack( void ) const;
  double getLengthToWidthRatio ( void ) const;
  double getAttackDistance(LengthUnits::LengthUnitsEnum lengthUnits) const;
  double getReportSize( AreaUnits::AreaUnitsEnum areaUnits ) const;
  double getReportRate( SpeedUnits::SpeedUnitsEnum speedUnits ) const;
  int    getTactic( void ) const;
  double getAutoComputedResourceProductionRate(SpeedUnits::SpeedUnitsEnum speedUnits);
  void   doContainRun();
  void   doContainRunWithOptimalResource();
  void   setContainMode(ContainMode containMode);
  void   setResourceArrivalTime(double arrivalTime, TimeUnits::TimeUnitsEnum timeUnits);
  void   setResourceDuration(double duration, TimeUnits::TimeUnitsEnum timeUnits);
  void   setAutoComputedResourceProductionRate(double resourceProductionRate, SpeedUnits::SpeedUnitsEnum speedUnits);

protected:
  double autoComputedResourceProductionRate_;
  ContainMode containMode_ = ContainMode::Default;
  double  resourceArrivalTime_; //min
  double  resourceDuration_; //min
};

#endif //CONTAINADAPTER_H
