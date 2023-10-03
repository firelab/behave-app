#pragma once

#include "fineDeadFuelMoistureTool.h"

class SIGFineDeadFuelMoistureTool : public FineDeadFuelMoistureTool {
public:
  void calculate();
  void setAspectIndex(FDFMToolAspectIndex::AspectIndexEnum aspectIndex);
  void setDryBulbIndex(FDFMToolDryBulbIndex::DryBulbIndexEnum dryBulbIndex);
  void setElevationIndex(FDFMToolElevationIndex::ElevationIndexEnum elevationIndex);
  void setMonthIndex(FDFMToolMonthIndex::MonthIndexEnum monthIndex);
  void setRHIndex(FDFMToolRHIndex::RHIndexEnum relativeHumidityIndex);
  void setShadingIndex(FDFMToolShadingIndex::ShadingIndexEnum shadingIndex);
  void setSlopeIndex(FDFMToolSlopeIndex::SlopeIndexEnum slopeIndex);
  void setTimeOfDayIndex(FDFMToolTimeOfDayIndex::TimeOfDayIndexEnum timeOfDayIndex);

protected:
  FDFMToolAspectIndex::AspectIndexEnum       aspectIndex_;
  FDFMToolDryBulbIndex::DryBulbIndexEnum     dryBulbIndex_;
  FDFMToolElevationIndex::ElevationIndexEnum elevationIndex_;
  FDFMToolMonthIndex::MonthIndexEnum         monthIndex_;
  FDFMToolRHIndex::RHIndexEnum               relativeHumidityIndex_;
  FDFMToolShadingIndex::ShadingIndexEnum     shadingIndex_;
  FDFMToolSlopeIndex::SlopeIndexEnum         slopeIndex_;
  FDFMToolTimeOfDayIndex::TimeOfDayIndexEnum timeOfDayIndex_;
};
