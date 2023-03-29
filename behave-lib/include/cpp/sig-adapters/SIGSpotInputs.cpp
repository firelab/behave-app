/******************************************************************************
 *
 * Project:  CodeBlocks
 * Purpose:  Class for storing inputs used by the Spot class
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

#include "SIGSpotInputs.h"

SIGSpotInputs::SIGSpotInputs()
{
    initializeMembers();
}

void SIGSpotInputs::setBurningPileFlameHeight(double buringPileFlameHeight, LengthUnits::LengthUnitsEnum flameHeightUnits)
{
    buringPileFlameHeight_ = LengthUnits::toBaseUnits(buringPileFlameHeight, flameHeightUnits);
}

void SIGSpotInputs::setDBH(double DBH, LengthUnits::LengthUnitsEnum DBHUnits)
{
    DBH_ = LengthUnits::toBaseUnits(DBH, DBHUnits);
}

void SIGSpotInputs::setDownwindCoverHeight(double downwindCoverHeight, LengthUnits::LengthUnitsEnum coverHeightUnits)
{
    downwindCoverHeight_ = LengthUnits::toBaseUnits(downwindCoverHeight, coverHeightUnits);
}

void SIGSpotInputs::setSurfaceFlameLength(double surfaceFlameLength, LengthUnits::LengthUnitsEnum flameLengthUnits)
{
    surfaceFlameLength_ = LengthUnits::toBaseUnits(surfaceFlameLength, flameLengthUnits);
}

void SIGSpotInputs::setLocation(SpotFireLocation location)
{
    location_ = location;
}

void SIGSpotInputs::setRidgeToValleyDistance(double ridgeToValleyDistance, LengthUnits::LengthUnitsEnum ridgeToValleyDistanceUnits)
{
    ridgeToValleyDistance_ = LengthUnits::toBaseUnits(ridgeToValleyDistance, ridgeToValleyDistanceUnits);
}

void SIGSpotInputs::setRidgeToValleyElevation(double ridgeToValleyElevation, LengthUnits::LengthUnitsEnum elevationUnits)
{
    ridgeToValleyElevation_ = LengthUnits::toBaseUnits(ridgeToValleyElevation, elevationUnits);
}

void SIGSpotInputs::setTorchingTrees(int torchingTrees)
{
    torchingTrees_ = torchingTrees;
}

void SIGSpotInputs::setTreeHeight(double treeHeight, LengthUnits::LengthUnitsEnum  treeHeightUnits)
{
    treeHeight_ = LengthUnits::toBaseUnits(treeHeight, treeHeightUnits);
}

void SIGSpotInputs::setTreeSpecies(SpotTreeSpecies treeSpecies)
{
    treeSpecies_ = treeSpecies;
}

void SIGSpotInputs::setWindSpeedAtTwentyFeet(double windSpeedAtTwentyFeet, SpeedUnits::SpeedUnitsEnum windSpeedUnits)
{
    windSpeedAtTwentyFeet_ = SpeedUnits::toBaseUnits(windSpeedAtTwentyFeet, windSpeedUnits);
}

void SIGSpotInputs::updateSpotInputsForBurningPile(SpotFireLocation location, double ridgeToValleyDistance,
        LengthUnits::LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits::LengthUnitsEnum elevationUnits,
        double downwindCoverHeight, LengthUnits::LengthUnitsEnum coverHeightUnits, double buringPileFlameHeight,
        LengthUnits::LengthUnitsEnum flameHeightUnits, double windSpeedAtTwentyFeet, SpeedUnits::SpeedUnitsEnum windSpeedUnits)
{
    setLocation(location);
    setRidgeToValleyDistance(ridgeToValleyDistance, ridgeToValleyDistanceUnits);
    setRidgeToValleyElevation(ridgeToValleyElevation, elevationUnits);
    setDownwindCoverHeight(downwindCoverHeight, coverHeightUnits);
    setWindSpeedAtTwentyFeet(windSpeedAtTwentyFeet, windSpeedUnits);
    setBurningPileFlameHeight(buringPileFlameHeight, flameHeightUnits);
}

void SIGSpotInputs::updateSpotInputsForSurfaceFire(SpotFireLocation location, double ridgeToValleyDistance,
        LengthUnits::LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits::LengthUnitsEnum elevationUnits,
        double downwindCoverHeight, LengthUnits::LengthUnitsEnum coverHeightUnits, double windSpeedAtTwentyFeet,
        SpeedUnits::SpeedUnitsEnum windSpeedUnits, double surfaceFlameLength, LengthUnits::LengthUnitsEnum flameLengthUnits)
{
    setLocation(location);
    setRidgeToValleyDistance(ridgeToValleyDistance, ridgeToValleyDistanceUnits);
    setRidgeToValleyElevation(ridgeToValleyElevation, elevationUnits);
    setDownwindCoverHeight(downwindCoverHeight, coverHeightUnits);
    setWindSpeedAtTwentyFeet(windSpeedAtTwentyFeet, windSpeedUnits);
    setSurfaceFlameLength(surfaceFlameLength, flameLengthUnits);
}

void SIGSpotInputs::updateSpotInputsForTorchingTrees(SpotFireLocation location, double ridgeToValleyDistance,
        LengthUnits::LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits::LengthUnitsEnum elevationUnits,
        double downwindCoverHeight, LengthUnits::LengthUnitsEnum coverHeightUnits, int torchingTrees, double DBH,
        LengthUnits::LengthUnitsEnum DBHUnits, double treeHeight, LengthUnits::LengthUnitsEnum  treeHeightUnits,
        SpotTreeSpecies treeSpecies, double windSpeedAtTwentyFeet, SpeedUnits::SpeedUnitsEnum windSpeedUnits)
{
    setLocation(location);
    setRidgeToValleyDistance(ridgeToValleyDistance, ridgeToValleyDistanceUnits);
    setRidgeToValleyElevation(ridgeToValleyElevation, elevationUnits);
    setDownwindCoverHeight(downwindCoverHeight, coverHeightUnits);
    setTorchingTrees(torchingTrees);
    setDBH(DBH, DBHUnits);
    setTreeHeight(treeHeight, treeHeightUnits);
    setTreeSpecies(treeSpecies);
    setWindSpeedAtTwentyFeet(windSpeedAtTwentyFeet, windSpeedUnits);
}

double SIGSpotInputs::getBurningPileFlameHeight(LengthUnits::LengthUnitsEnum flameHeightUnits)
{
    return LengthUnits::fromBaseUnits(buringPileFlameHeight_, flameHeightUnits);
}

double SIGSpotInputs::getDBH(LengthUnits::LengthUnitsEnum DBHUnits)
{
    return LengthUnits::fromBaseUnits(DBH_, DBHUnits);
}

double SIGSpotInputs::getDownwindCoverHeight(LengthUnits::LengthUnitsEnum coverHeightUnits)
{
    return LengthUnits::fromBaseUnits(downwindCoverHeight_, coverHeightUnits);
}

double SIGSpotInputs::getSurfaceFlameLength(LengthUnits::LengthUnitsEnum surfaceFlameLengthUnits)
{
    return LengthUnits::fromBaseUnits(surfaceFlameLength_, surfaceFlameLengthUnits);
}

SpotFireLocation SIGSpotInputs::getLocation()
{
    return location_;
}

double SIGSpotInputs::getRidgeToValleyDistance(LengthUnits::LengthUnitsEnum ridgeToValleyDistanceUnits)
{
    return LengthUnits::fromBaseUnits(ridgeToValleyDistance_, ridgeToValleyDistanceUnits);
}

double SIGSpotInputs::getRidgeToValleyElevation(LengthUnits::LengthUnitsEnum elevationUnits)
{
    return LengthUnits::fromBaseUnits(ridgeToValleyElevation_, elevationUnits);
}

int SIGSpotInputs::getTorchingTrees()
{
    return torchingTrees_;
}

double SIGSpotInputs::getTreeHeight(LengthUnits::LengthUnitsEnum  treeHeightUnits)
{
    return LengthUnits::fromBaseUnits(treeHeight_, treeHeightUnits);
}

SpotTreeSpecies SIGSpotInputs::getTreeSpecies()
{
    return treeSpecies_;
}

double SIGSpotInputs::getWindSpeedAtTwentyFeet(SpeedUnits::SpeedUnitsEnum windSpeedUnits)
{
    return SpeedUnits::fromBaseUnits(windSpeedAtTwentyFeet_, windSpeedUnits);
}

void SIGSpotInputs::initializeMembers()
{
    downwindCoverHeight_ = 0.0;
    location_ = MIDSLOPE_WINDWARD;
    ridgeToValleyDistance_ = 0.0;
    ridgeToValleyElevation_ = 0.0;
    windSpeedAtTwentyFeet_ = 0.0;
    buringPileFlameHeight_ = 0.0;
    surfaceFlameLength_ = 0.0;
    torchingTrees_ = 0.0;
    DBH_ = 0.0;
    treeHeight_ = 0.0;
}

