#include "SIGContainForceAdapter.h"

SIGContainForceAdapter::SIGContainForceAdapter()
    :resourceVector(0)
{
}

SIGContainForceAdapter::~SIGContainForceAdapter()
{
}

void SIGContainForceAdapter::addResource(SIGContainResource& resource)
{
    resourceVector.push_back(resource);
}

void SIGContainForceAdapter::addResource(double arrival, double production, double duration,
    ContainFlank flank, const char * desc, double baseCost, double hourCost)
{
    SIGContainResource resource(arrival, production, duration, flank, desc, baseCost, hourCost);
    addResource(resource);
}

double SIGContainForceAdapter::firstArrival(ContainFlank flank) const
{
    double at = 99999999.0;
    for (int i = 0; i < resourceVector.size(); i++)
    {
        if ((resourceVector[i].flank() == flank || resourceVector[i].flank() == BothFlanks)
            && resourceVector[i].arrival() < at)
        {
            at = resourceVector[i].arrival();
        }
    }
    return(at);
}

int SIGContainForceAdapter::removeResourceAt(int index)
{
    int success = 1; // 1 means didn't find it
    if (index < resourceVector.size())
    {
        resourceVector.erase(resourceVector.begin() + index);
        return 0; // success
    }
    return success;
}

int SIGContainForceAdapter::removeResourceWithThisDesc(const char * desc)
{
    std::string descriptionString;
    for (int i = 0; i < resourceVector.size(); i++)
    {
        descriptionString = resourceVector[i].description();
        if (strcmp(descriptionString.c_str(), desc) == 0)
        {
            removeResourceAt(i);
            return 0; // success
        }
    }
    // didn't find it
    return 1; // error
}

int SIGContainForceAdapter::removeAllResourcesWithThisDesc(const char * desc)
{
    int rc;
    int success = 1; // 1 means didn't find it
    while ((rc = removeResourceWithThisDesc(desc)) == 0)
    {
        success = 0; // found at least one
    }
    return success;
}

