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

class SIGContainAdapter : public ContainAdapter
{
public:

  // Inputs
  void addResource(double arrival,
                   double duration,
                   TimeUnits::TimeUnitsEnum timeUnit,
                   double productionRate,
                   SpeedUnits::SpeedUnitsEnum productionRateUnits,
                   const char * description = "",
                   double baseCost = 0.0,
                   double hourCost = 0.0);
  int removeResourceWithThisDesc(const char * desc);
  int removeAllResourcesWithThisDesc(const char * desc);

  //TODO PR to behave to fix mispelled perimiter
  double getPerimeterAtInitialAttack(LengthUnits::LengthUnitsEnum lengthUnits);

  void doContainRun();

  DoubleVector firePerimeterX( void ) const;
  DoubleVector firePerimeterY( void ) const;
  int firePoints( void ) const;

  // Intermediate Variables
  DoubleVector m_x;           //!< Array of perimeter x coordinates (ch)
  DoubleVector m_y;           //!< Array of perimeter y coordinates (ch)
  int  m_size;                //!< Size of the arrays (m_maxSteps or 2*m_maxSteps)
};

#endif //CONTAINADAPTER_H
