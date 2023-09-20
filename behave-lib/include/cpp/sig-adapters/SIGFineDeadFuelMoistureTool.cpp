#include "SIGFineDeadFuelMoistureTool.h"

void SIGFineDeadFuelMoistureTool::calculate() {
  return FineDeadFuelMoistureTool::calculate(aspectIndex_,
                                             dryBulbIndex_,
                                             elevationIndex_,
                                             monthIndex_,
                                             relativeHumidityIndex_,
                                             shadingIndex_,
                                             slopeIndex_,
                                             timeOfDayIndex_);
}

void SIGFineDeadFuelMoistureTool::setAspectIndex(FDFMToolAspectIndex::AspectIndexEnum aspectIndex){
  aspectIndex_ = aspectIndex;
}

void SIGFineDeadFuelMoistureTool::setDryBulbIndex(FDFMToolDryBulbIndex::DryBulbIndexEnum dryBulbIndex){
  dryBulbIndex_ = dryBulbIndex;
}

void SIGFineDeadFuelMoistureTool::setElevationIndex(FDFMToolElevationIndex::ElevationIndexEnum elevationIndex){
  elevationIndex_ = elevationIndex;
}

void SIGFineDeadFuelMoistureTool::setMonthIndex(FDFMToolMonthIndex::MonthIndexEnum monthIndex){
  monthIndex_ = monthIndex;
}

void SIGFineDeadFuelMoistureTool::setRHIndex(FDFMToolRHIndex::RHIndexEnum relativeHumidityIndex){
  relativeHumidityIndex_ = relativeHumidityIndex;
}

void SIGFineDeadFuelMoistureTool::setShadingIndex(FDFMToolShadingIndex::ShadingIndexEnum shadingIndex){
  shadingIndex_ = shadingIndex;
}

void SIGFineDeadFuelMoistureTool::setSlopeIndex(FDFMToolSlopeIndex::SlopeIndexEnum slopeIndex){
  slopeIndex_ = slopeIndex;
}

void SIGFineDeadFuelMoistureTool::setTimeOfDayIndex(FDFMToolTimeOfDayIndex::TimeOfDayIndexEnum timeOfDayIndex){
  timeOfDayIndex_ = timeOfDayIndex;
}
