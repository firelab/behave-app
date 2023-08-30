#include "SIGSlopeTool.h"

// calculateHorizontalDistance

void SIGSlopeTool::calculateHorizontalDistance() {
  SlopeTool::calculateHorizontalDistance(calculatedMapDistance_,
                                         LengthUnits::Feet,
                                         maxSlopeSteepness_,
                                         SlopeUnits::Percent);
}

void SIGSlopeTool::setCalculatedMapDistance(double calculatedMapDistance,
                                            LengthUnits::LengthUnitsEnum distanceUnits) {
  calculatedMapDistance_ = LengthUnits::toBaseUnits(calculatedMapDistance, distanceUnits);
}

void SIGSlopeTool::setMaxSlopeSteepness(double maxSlopeSteepness) {
  maxSlopeSteepness_ = maxSlopeSteepness;
}

double SIGSlopeTool::getHorizontalDistanceZero(LengthUnits::LengthUnitsEnum mapDistanceUnits) {
  return SlopeTool::getHorizontalDistance(HorizontalDistanceIndex::UPSLOPE_ZERO_DEGREES,
                                         mapDistanceUnits);
}

double SIGSlopeTool::getHorizontalDistanceFifteen(LengthUnits::LengthUnitsEnum mapDistanceUnits) {
  return SlopeTool::getHorizontalDistance(HorizontalDistanceIndex::FIFTEEN_DEGREES_FROM_UPSLOPE,
                                         mapDistanceUnits);
}

double SIGSlopeTool::getHorizontalDistanceThirty(LengthUnits::LengthUnitsEnum mapDistanceUnits) {
  return SlopeTool::getHorizontalDistance(HorizontalDistanceIndex::THIRTY_DEGREES_FROM_UPSLOPE,
                                         mapDistanceUnits);
}

double SIGSlopeTool::getHorizontalDistanceFourtyFive(LengthUnits::LengthUnitsEnum mapDistanceUnits) {
  return SlopeTool::getHorizontalDistance(HorizontalDistanceIndex::FORTY_FIVE_DEGREES_FROM_UPSLOPE,
                                         mapDistanceUnits);
}

double SIGSlopeTool::getHorizontalDistanceSixty(LengthUnits::LengthUnitsEnum mapDistanceUnits) {
  return SlopeTool::getHorizontalDistance(HorizontalDistanceIndex::SIXTY_DEGREES_FROM_UPSLOPE,
                                         mapDistanceUnits);
}

double SIGSlopeTool::getHorizontalDistanceSeventy(LengthUnits::LengthUnitsEnum mapDistanceUnits) {
  return SlopeTool::getHorizontalDistance(HorizontalDistanceIndex::SEVENTY_FIVE_DEGREES_FROM_UPSLOPE,
                                         mapDistanceUnits);
}

double SIGSlopeTool::getHorizontalDistanceNinety(LengthUnits::LengthUnitsEnum mapDistanceUnits) {
  return SlopeTool::getHorizontalDistance(HorizontalDistanceIndex::CROSS_SLOPE_NINETY_DEGREES,
                                         mapDistanceUnits);
}

//calculateSlopeFromMapMeasurements


void SIGSlopeTool::calculateSlopeFromMapMeasurements() {
  SlopeTool::calculateSlopeFromMapMeasurements(mapRepresentativeFraction_,
                                               mapDistance_,
                                               LengthUnits::Feet,
                                               contourInterval_,
                                               numberOfContours_,
                                               LengthUnits::Feet);
}

void SIGSlopeTool::setMapRepresentativeFraction(int mapRepresentativeFraction) {
  mapRepresentativeFraction_ = mapRepresentativeFraction;
}

void SIGSlopeTool::setMapDistance(double mapDistance, LengthUnits::LengthUnitsEnum distanceUnits) {
  mapDistance_ = LengthUnits::toBaseUnits(mapDistance, distanceUnits);
}

void SIGSlopeTool::setContourInterval(double contourInterval, LengthUnits::LengthUnitsEnum contourUnits) {
  contourInterval_ = LengthUnits::toBaseUnits(contourInterval, contourUnits);
}

void SIGSlopeTool::setNumberOfContours(double numberOfContours) {
  numberOfContours_ = numberOfContours;
}

double SIGSlopeTool::getSlopeFromMapMeasurementsInPercent() {
  return SlopeTool::getSlopeFromMapMeasurements(SlopeUnits::Percent);
}

double SIGSlopeTool::getSlopeFromMapMeasurementsInDegrees() {
  return SlopeTool::getSlopeFromMapMeasurements(SlopeUnits::Degrees);
}
