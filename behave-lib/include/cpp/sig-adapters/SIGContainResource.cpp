//------------------------------------------------------------------------------
/*! \file SIGContainResource.cpp
    \author Copyright (C) 2006 by Collin D. Bevins.
    \author Copyright (C) 2022 by Richard Sheperd, Spatial Informatics Group
    \license This is released under the GNU Public License 2.
    \brief A single fire containment resource unit that can be dispatched
    to a fire.  Examples include an engine crew, line crew, bulldozer,
    helicopter, airtanker, etc.
 */

#include <stdio.h>

// Local include files
#include "SIGContain.h"

/* Silence warnings that aren't our fault */
#if defined(OMFFR) && defined(__GNUC__)
#pragma GCC diagnostic ignored "-Wwrite-strings"
#pragma GCC push
#endif /* defined(OMFFR) && defined(__GNUC__) */

#ifdef OMFFR
#pragma GCC diagnostic ignored "-Wwrite-strings"
#pragma GCC push
#endif /* OMFFR */


//------------------------------------------------------------------------------
/*! \brief SIGContainResource default constructor.
 */

SIGContainResource::SIGContainResource( void ) :
    m_arrival(0.0),
    m_duration(0.0),
    m_production(0.0),
    m_baseCost(0.0),
    m_hourCost(0.0),
    m_flank(LeftFlank),
    m_desc("")
{
    return;
}

//------------------------------------------------------------------------------
/*! \brief SIGContainResource constructor.

    Describes a single fire containment resource unit that can be dispatched
    to a fire, such as an engine crew, line crew, bulldozer, helicopter,
    airtanker, etc.

    \param[in] arrival    Fireline production begins at this elapsed time since
                          the fire was \b reported (min).
    \param[in] production Sustained rate of holdable fireline production (ch/h).
                          THIS IS THE TOTAL RATE FOR BOTH FLANKS.
                          THE PRODUCTION RATE WILL BE SPLIT IN HALF AND APPLIED
                          TO ONE FLANK FOR SIMULATION.
    \param[in] duration   Amount of time during which the fireline production
                          rate is maintained (min).
    \param[in] flank      One of LeftFlank, RightFlank, BothFlanks, or NeitherFlank.
    \param[in] desc       Resource description or identification (informational
                          only; not used by the program).
    \param[in] baseCost   Base cost of deploying the resource to the fire.
    \param[in] hourCost   Hourly cost of the resource while at the fire.
 */

SIGContainResource::SIGContainResource(
        double arrival,
        double production,
        double duration,
        ContainFlank flank,
        const char * desc,
        double baseCost,
        double hourCost ) :
    m_arrival(arrival),
    m_duration(duration),
    m_production(production),
    m_baseCost(baseCost),
    m_hourCost(hourCost),
    m_flank(flank),
    m_desc(desc)
{
    return;
}

//------------------------------------------------------------------------------
/*! \brief SIGContainResource copy constructor.
    \param[in] r          SIGContainResource to copy
 */

SIGContainResource::SIGContainResource(const SIGContainResource &r) :
    m_arrival(r.m_arrival),
    m_duration(r.m_duration),
    m_production(r.m_production),
    m_baseCost(r.m_baseCost),
    m_hourCost(r.m_hourCost),
    m_flank(r.m_flank),
    m_desc(r.m_desc)
{
    return;
}

//------------------------------------------------------------------------------
/*! \brief SIGContainResource class destructor.
 */

SIGContainResource::~SIGContainResource( void )
{
    return;
}

//------------------------------------------------------------------------------
/*! \brief Access to the SIGContainResource arrival time at the fire.

    \return Resource arrival time at the fire (minutes since fire ignition).
 */

double SIGContainResource::arrival( void ) const
{
    return( m_arrival );
}

//------------------------------------------------------------------------------
/*! \brief Access to the SIGContainResource unit base operating cost.

    \return Resource base operating cost.
 */

double SIGContainResource::baseCost( void ) const
{
    return( m_baseCost );
}

//------------------------------------------------------------------------------
/*! \brief Access to the SIGContainResource description.

    \return Resource description.
 */

const char * SIGContainResource::description( void ) const
{
    return( m_desc );
}

//------------------------------------------------------------------------------
/*! \brief Access to the SIGContainResource production duration period.

    \return Resource production duration period (minutes).
 */

double SIGContainResource::duration( void ) const
{
    return( m_duration );
}

//------------------------------------------------------------------------------
/*! \brief Access to the SIGContainResource flank assignment.

    \return Resource ContainFlank flank assignment
        - LeftFlank
        - RightFlank
        - BothFlanks
        - NeitherFlank
 */

ContainFlank SIGContainResource::flank( void ) const
{
    return( m_flank );
}

//------------------------------------------------------------------------------
/*! \brief Access to the SIGContainResource unit hourly operating cost.

    \return Resource hourly operating cost.
 */

double SIGContainResource::hourCost( void ) const
{
    return( m_hourCost );
}

//------------------------------------------------------------------------------
/*! \brief Access to the SIGContainResource containment line production rate.

    \return Resource containment line production rate (ft/min).
 */

double SIGContainResource::production( void ) const
{
    return( m_production );
}

void SIGContainResource::print(char buf[], int buflen) const {
	char localbuf[100];
	int i=0;
	if (!buf || buflen<1) return;
	memset(buf,0,buflen);
	i=sprintf(localbuf,"arrival=%5.2f production=%5.2f duration=%6.1f ", m_arrival, m_production, m_duration);
	i=(i<(buflen-1)? i : (buflen-1));
	memcpy(buf,localbuf,i);
	buf[i]='\0';
}

/* Silence warnings that aren't our fault */
#if defined(OMFFR) && defined(__GNUC__)
#pragma GCC pop
#endif /* OMFFR */


//------------------------------------------------------------------------------
//  End of SIGContainResource.cpp
//------------------------------------------------------------------------------


