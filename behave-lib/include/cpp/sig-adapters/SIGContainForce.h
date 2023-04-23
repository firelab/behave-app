//------------------------------------------------------------------------------
/*! \file SIGContainForce.h
    \author Copyright (C) 2006 by Collin D. Bevins.
    \author Copyright (C) 2022 by Richard Sheperd, Spatial Informatics Group
    \license This is released under the GNU Public License 2.
    \brief Collection of all SIGContainResources dispatched to the fire.
 */

#ifndef _SIGCONTAINFORCE_H_INCLUDED_
#define _SIGCONTAINFORCE_H_INCLUDED_

// Custom include files
#include "SIGContain.h"
#include "SIGContainResource.h"
#include <cstring>
#include <vector>

// Forward class references
class SIGContain;

//------------------------------------------------------------------------------
/*! \class SIGContainForce SIGContainForce.h
    \brief Collection of all SIGContainResources dispatched to the fire.
 */

class SIGContainForce
{
// Class version
    static const int containForceVersion = 1;   //!< Class version

// Public methods
public:
    // Custom constructors
    SIGContainForce() ;
    // Virtual destructor
    virtual ~SIGContainForce( void ) ;
    // Add SIGContainResource into SIGContainForce
    SIGContainResource* addResource( SIGContainResource* resource ) ;
    // Construct SIGContainResource into SIGContainForce
    SIGContainResource *addResource(
        double arrival,
        double production,
        double duration=480.,
        ContainFlank flank=LeftFlank,
        const char * desc="",
        double baseCost=0.0,
        double hourCost=0.0 );

    // Force-level access methods
    double exhausted( ContainFlank flank ) const ;
    double firstArrival( ContainFlank flank ) const ;
    double nextArrival( double after, double until, ContainFlank flank ) const ;
    double productionRate( double minutesSinceReport, ContainFlank flank ) const ;

    //for debug
    void   logResources(bool debug, const SIGContain*) const ;

    // Public access to individual SIGContainResources
    int     resources( void ) const ;
    double  resourceArrival( int index ) const ;
    double  resourceBaseCost( int index ) const ;
    double  resourceCost( int index, double finalTime ) const ;
    const char * resourceDescription( int index ) const ;
    double  resourceDuration( int index ) const ;
    ContainFlank resourceFlank( int index ) const ;
    double  resourceHourCost( int index ) const ;
    double  resourceProduction( int index ) const ;

// Protected data
protected:
    std::vector<SIGContainResource> m_cr;  //!< Vector of SIGContainResources

friend class SIGContain;
};

#endif

//------------------------------------------------------------------------------
//  End of SIGContainForce.h
//------------------------------------------------------------------------------


