#pragma once

#include "slopeTool.h"

class SIGSlopeTool : public SlopeTool {
public:
  // calculateHorizontalDistance
  void calculateHorizontalDistance();
  void setCalculatedMapDistance(double calculatedMapDistance, LengthUnits::LengthUnitsEnum distanceUnits);
  void setMaxSlopeSteepness(double maxSlopeSteepness);
  double getHorizontalDistanceZero(LengthUnits::LengthUnitsEnum mapDistanceUnits);
  double getHorizontalDistanceFifteen(LengthUnits::LengthUnitsEnum mapDistanceUnits);
  double getHorizontalDistanceThirty(LengthUnits::LengthUnitsEnum mapDistanceUnits);
  double getHorizontalDistanceFourtyFive(LengthUnits::LengthUnitsEnum mapDistanceUnits);
  double getHorizontalDistanceSixty(LengthUnits::LengthUnitsEnum mapDistanceUnits);
  double getHorizontalDistanceSeventy(LengthUnits::LengthUnitsEnum mapDistanceUnits);
  double getHorizontalDistanceNinety(LengthUnits::LengthUnitsEnum mapDistanceUnits);

  // calculateSlopeFromMapMeasurements
  void calculateSlopeFromMapMeasurements();
  void setMapRepresentativeFraction(int mapRepresentativeFraction);
  void setMapDistance(double mapDistance, LengthUnits::LengthUnitsEnum distanceUnits);
  void setContourInterval(double contourInterval, LengthUnits::LengthUnitsEnum contourUnits);
  void setNumberOfContours(double numberOfContours);
  double getSlopeFromMapMeasurementsInPercent();
  double getSlopeFromMapMeasurementsInDegrees();

protected:
  // calculateHorizontalDistance

  double calculatedMapDistance_;
  double maxSlopeSteepness_;

  // calculateSlopeFromMapMeasurements
  int mapRepresentativeFraction_;
  double mapDistance_;
  double contourInterval_;
  double numberOfContours_;
};
