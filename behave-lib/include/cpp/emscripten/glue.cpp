
#include <emscripten.h>

EM_JS_DEPS(webidl_binder, "$intArrayFromString");

extern "C" {

// Not using size_t for array indices as the values used by the javascript code are signed.

EM_JS(void, array_bounds_check_error, (size_t idx, size_t size), {
  throw 'Array index ' + idx + ' out of bounds: [0,' + size + ')';
});

void array_bounds_check(const int array_size, const int array_idx) {
  if (array_idx < 0 || array_idx >= array_size) {
    array_bounds_check_error(array_idx, array_size);
  }
}

// VoidPtr

void EMSCRIPTEN_KEEPALIVE emscripten_bind_VoidPtr___destroy___0(void** self) {
  delete self;
}

// DoublePtr

void EMSCRIPTEN_KEEPALIVE emscripten_bind_DoublePtr___destroy___0(DoublePtr* self) {
  delete self;
}

// FireSize

FireSize* EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_FireSize_0() {
  return new FireSize();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_calculateFireBasicDimensions_4(FireSize* self, double effectiveWindSpeed, SpeedUnits_SpeedUnitsEnum windSpeedRateUnits, double forwardSpreadRate, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  self->calculateFireBasicDimensions(effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFireLengthToWidthRatio_0(FireSize* self) {
  return self->getFireLengthToWidthRatio();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getEccentricity_0(FireSize* self) {
  return self->getEccentricity();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getBackingSpreadRate_1(FireSize* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getBackingSpreadRate(spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getEllipticalA_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getEllipticalA(lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getEllipticalB_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getEllipticalB(lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getEllipticalC_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getEllipticalC(lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFireLength_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getFireLength(lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getMaxFireWidth_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getMaxFireWidth(lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFirePerimeter_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getFirePerimeter(lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFireArea_3(FireSize* self, AreaUnits_AreaUnitsEnum areaUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getFireArea(areaUnits, elapsedTime, timeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize___destroy___0(FireSize* self) {
  delete self;
}

// SIGContainResource

SIGContainResource* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_SIGContainResource_0() {
  return new SIGContainResource();
}

SIGContainResource* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_SIGContainResource_7(double arrival, double production, double duration, ContainFlank flank, const char* desc, double baseCost, double hourCost) {
  return new SIGContainResource(arrival, production, duration, flank, desc, baseCost, hourCost);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_print_2(SIGContainResource* self, char* buf, int buflen) {
  self->print(buf, buflen);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_arrival_0(SIGContainResource* self) {
  return self->arrival();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_hourCost_0(SIGContainResource* self) {
  return self->hourCost();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_duration_0(SIGContainResource* self) {
  return self->duration();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_production_0(SIGContainResource* self) {
  return self->production();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_baseCost_0(SIGContainResource* self) {
  return self->baseCost();
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_description_0(SIGContainResource* self) {
  return self->description();
}

ContainFlank EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource_flank_0(SIGContainResource* self) {
  return self->flank();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainResource___destroy___0(SIGContainResource* self) {
  delete self;
}

// SIGContainForce

SIGContainForce* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_SIGContainForce_0() {
  return new SIGContainForce();
}

SIGContainResource* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_addResource_1(SIGContainForce* self, SIGContainResource* arrival) {
  return self->addResource(arrival);
}

SIGContainResource* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_addResource_7(SIGContainForce* self, double arrival, double production, double duration, ContainFlank flank, const char* desc, double baseCost, double hourCost) {
  return self->addResource(arrival, production, duration, flank, desc, baseCost, hourCost);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_exhausted_1(SIGContainForce* self, ContainFlank flank) {
  return self->exhausted(flank);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_firstArrival_1(SIGContainForce* self, ContainFlank flank) {
  return self->firstArrival(flank);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_nextArrival_3(SIGContainForce* self, double after, double until, ContainFlank flank) {
  return self->nextArrival(after, until, flank);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_productionRate_2(SIGContainForce* self, double minutesSinceReport, ContainFlank flank) {
  return self->productionRate(minutesSinceReport, flank);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resources_0(SIGContainForce* self) {
  return self->resources();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceArrival_1(SIGContainForce* self, int index) {
  return self->resourceArrival(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceBaseCost_1(SIGContainForce* self, int index) {
  return self->resourceBaseCost(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceCost_2(SIGContainForce* self, int index, double finalTime) {
  return self->resourceCost(index, finalTime);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceDescription_1(SIGContainForce* self, int index) {
  return self->resourceDescription(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceDuration_1(SIGContainForce* self, int index) {
  return self->resourceDuration(index);
}

ContainFlank EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceFlank_1(SIGContainForce* self, int index) {
  return self->resourceFlank(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceHourCost_1(SIGContainForce* self, int index) {
  return self->resourceHourCost(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce_resourceProduction_1(SIGContainForce* self, int index) {
  return self->resourceProduction(index);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForce___destroy___0(SIGContainForce* self) {
  delete self;
}

// SIGContainForceAdapter

SIGContainForceAdapter* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForceAdapter_SIGContainForceAdapter_0() {
  return new SIGContainForceAdapter();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForceAdapter_addResource_7(SIGContainForceAdapter* self, double arrival, double production, double duration, ContainFlank flank, const char* desc, double baseCost, double hourCost) {
  self->addResource(arrival, production, duration, flank, desc, baseCost, hourCost);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForceAdapter_firstArrival_1(SIGContainForceAdapter* self, ContainFlank flank) {
  return self->firstArrival(flank);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForceAdapter_removeResourceAt_1(SIGContainForceAdapter* self, int index) {
  return self->removeResourceAt(index);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForceAdapter_removeResourceWithThisDesc_1(SIGContainForceAdapter* self, const char* desc) {
  return self->removeResourceWithThisDesc(desc);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForceAdapter_removeAllResourcesWithThisDesc_1(SIGContainForceAdapter* self, const char* desc) {
  return self->removeAllResourcesWithThisDesc(desc);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainForceAdapter___destroy___0(SIGContainForceAdapter* self) {
  delete self;
}

// SIGContainSim

SIGContainSim* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_SIGContainSim_13(double reportSize, double reportRate, SIGDiurnalROS* diurnalROS, int fireStartMinutesStartTime, double lwRatio, SIGContainForce* force, ContainTactic tactic, double attackDist, bool retry, int minSteps, int maxSteps, int maxFireSize, int maxFireTime) {
  return new SIGContainSim(reportSize, reportRate, diurnalROS, fireStartMinutesStartTime, lwRatio, force, tactic, attackDist, retry, minSteps, maxSteps, maxFireSize, maxFireTime);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_attackDistance_0(SIGContainSim* self) {
  return self->attackDistance();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_attackPointX_0(SIGContainSim* self) {
  return self->attackPointX();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_attackPointY_0(SIGContainSim* self) {
  return self->attackPointY();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_attackTime_0(SIGContainSim* self) {
  return self->attackTime();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_distanceStep_0(SIGContainSim* self) {
  return self->distanceStep();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireBackAtAttack_0(SIGContainSim* self) {
  return self->fireBackAtAttack();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireBackAtReport_0(SIGContainSim* self) {
  return self->fireBackAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireHeadAtAttack_0(SIGContainSim* self) {
  return self->fireHeadAtAttack();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireHeadAtReport_0(SIGContainSim* self) {
  return self->fireHeadAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireLwRatioAtReport_0(SIGContainSim* self) {
  return self->fireLwRatioAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireReportTime_0(SIGContainSim* self) {
  return self->fireReportTime();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireSizeAtReport_0(SIGContainSim* self) {
  return self->fireSizeAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireSpreadRateAtBack_0(SIGContainSim* self) {
  return self->fireSpreadRateAtBack();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireSpreadRateAtReport_0(SIGContainSim* self) {
  return self->fireSpreadRateAtReport();
}

SIGContainForce* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_force_0(SIGContainSim* self) {
  return self->force();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_maximumSimulationSteps_0(SIGContainSim* self) {
  return self->maximumSimulationSteps();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_minimumSimulationSteps_0(SIGContainSim* self) {
  return self->minimumSimulationSteps();
}

ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_status_0(SIGContainSim* self) {
  return self->status();
}

ContainTactic EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_tactic_0(SIGContainSim* self) {
  return self->tactic();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_finalFireCost_0(SIGContainSim* self) {
  return self->finalFireCost();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_finalFireLine_0(SIGContainSim* self) {
  return self->finalFireLine();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_finalFirePerimeter_0(SIGContainSim* self) {
  return self->finalFirePerimeter();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_finalFireSize_0(SIGContainSim* self) {
  return self->finalFireSize();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_finalFireSweep_0(SIGContainSim* self) {
  return self->finalFireSweep();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_finalFireTime_0(SIGContainSim* self) {
  return self->finalFireTime();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_finalResourcesUsed_0(SIGContainSim* self) {
  return self->finalResourcesUsed();
}

DoublePtr* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_fireHeadX_0(SIGContainSim* self) {
  static DoublePtr temp;
  return (temp = self->fireHeadX(), &temp);
}

DoublePtr* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_firePerimeterY_0(SIGContainSim* self) {
  static DoublePtr temp;
  return (temp = self->firePerimeterY(), &temp);
}

DoublePtr* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_firePerimeterX_0(SIGContainSim* self) {
  static DoublePtr temp;
  return (temp = self->firePerimeterX(), &temp);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_firePoints_0(SIGContainSim* self) {
  return self->firePoints();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_run_0(SIGContainSim* self) {
  self->run();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim_UncontainedArea_5(SIGContainSim* self, double head, double lwRatio, double x, double y, ContainTactic tactic) {
  return self->UncontainedArea(head, lwRatio, x, y, tactic);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainSim___destroy___0(SIGContainSim* self) {
  delete self;
}

// SIGDiurnalROS

SIGDiurnalROS* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGDiurnalROS_SIGDiurnalROS_0() {
  return new SIGDiurnalROS();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGDiurnalROS_push_1(SIGDiurnalROS* self, double v) {
  self->push(v);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGDiurnalROS_at_1(SIGDiurnalROS* self, int i) {
  return self->at(i);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGDiurnalROS_size_0(SIGDiurnalROS* self) {
  return self->size();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGDiurnalROS___destroy___0(SIGDiurnalROS* self) {
  delete self;
}

// SIGContain

SIGContain* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_SIGContain_11(double reportSize, double reportRate, SIGDiurnalROS* diurnalROS, int fireStartMinutesStartTime, double lwRatio, double distStep, ContainFlank flank, SIGContainForce* force, double attackTime, ContainTactic tactic, double attackDist) {
  return new SIGContain(reportSize, reportRate, diurnalROS, fireStartMinutesStartTime, lwRatio, distStep, flank, force, attackTime, tactic, attackDist);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_simulationTime_0(SIGContain* self) {
  return self->simulationTime();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireSpreadRateAtBack_0(SIGContain* self) {
  return self->fireSpreadRateAtBack();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireLwRatioAtReport_0(SIGContain* self) {
  return self->fireLwRatioAtReport();
}

SIGContainForce* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_force_0(SIGContain* self) {
  return self->force();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_resourceHourCost_1(SIGContain* self, int index) {
  return self->resourceHourCost(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_attackDistance_0(SIGContain* self) {
  return self->attackDistance();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_attackPointX_0(SIGContain* self) {
  return self->attackPointX();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireHeadAtAttack_0(SIGContain* self) {
  return self->fireHeadAtAttack();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_attackPointY_0(SIGContain* self) {
  return self->attackPointY();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_attackTime_0(SIGContain* self) {
  return self->attackTime();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_resourceBaseCost_1(SIGContain* self, int index) {
  return self->resourceBaseCost(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireSpreadRateAtReport_0(SIGContain* self) {
  return self->fireSpreadRateAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireHeadAtReport_0(SIGContain* self) {
  return self->fireHeadAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireReportTime_0(SIGContain* self) {
  return self->fireReportTime();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_resourceProduction_1(SIGContain* self, int index) {
  return self->resourceProduction(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireBackAtAttack_0(SIGContain* self) {
  return self->fireBackAtAttack();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_simulationStep_0(SIGContain* self) {
  return self->simulationStep();
}

ContainTactic EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_tactic_0(SIGContain* self) {
  return self->tactic();
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_resourceDescription_1(SIGContain* self, int index) {
  return self->resourceDescription(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_distanceStep_0(SIGContain* self) {
  return self->distanceStep();
}

ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_status_0(SIGContain* self) {
  return self->status();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_resourceArrival_1(SIGContain* self, int index) {
  return self->resourceArrival(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireSizeAtReport_0(SIGContain* self) {
  return self->fireSizeAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_setFireStartTimeMinutes_1(SIGContain* self, int starttime) {
  return self->setFireStartTimeMinutes(starttime);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_fireBackAtReport_0(SIGContain* self) {
  return self->fireBackAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_resourceDuration_1(SIGContain* self, int index) {
  return self->resourceDuration(index);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_resources_0(SIGContain* self) {
  return self->resources();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain_exhaustedTime_0(SIGContain* self) {
  return self->exhaustedTime();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContain___destroy___0(SIGContain* self) {
  delete self;
}

// SIGContainAdapter

SIGContainAdapter* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_SIGContainAdapter_0() {
  return new SIGContainAdapter();
}

ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getContainmentStatus_0(SIGContainAdapter* self) {
  return self->getContainmentStatus();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFinalContainmentArea_1(SIGContainAdapter* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getFinalContainmentArea(areaUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFinalCost_0(SIGContainAdapter* self) {
  return self->getFinalCost();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFinalFireLineLength_1(SIGContainAdapter* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFinalFireLineLength(lengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFinalFireSize_1(SIGContainAdapter* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getFinalFireSize(areaUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFinalTimeSinceReport_1(SIGContainAdapter* self, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getFinalTimeSinceReport(timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1(SIGContainAdapter* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getFireSizeAtInitialAttack(areaUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getPerimeterAtContainment_1(SIGContainAdapter* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getPerimeterAtContainment(lengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getPerimeterAtInitialAttack_1(SIGContainAdapter* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getPerimeterAtInitialAttack(lengthUnits);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_removeAllResourcesWithThisDesc_1(SIGContainAdapter* self, const char* desc) {
  return self->removeAllResourcesWithThisDesc(desc);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_removeResourceAt_1(SIGContainAdapter* self, int index) {
  return self->removeResourceAt(index);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_removeResourceWithThisDesc_1(SIGContainAdapter* self, const char* desc) {
  return self->removeResourceWithThisDesc(desc);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_addResource_8(SIGContainAdapter* self, double arrival, double duration, TimeUnits_TimeUnitsEnum timeUnit, double productionRate, SpeedUnits_SpeedUnitsEnum productionRateUnits, char* description, double baseCost, double hourCost) {
  self->addResource(arrival, duration, timeUnit, productionRate, productionRateUnits, description, baseCost, hourCost);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_doContainRun_0(SIGContainAdapter* self) {
  self->doContainRun();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_removeAllResources_0(SIGContainAdapter* self) {
  self->removeAllResources();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setAttackDistance_2(SIGContainAdapter* self, double attackDistance, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setAttackDistance(attackDistance, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setFireStartTime_1(SIGContainAdapter* self, int fireStartTime) {
  self->setFireStartTime(fireStartTime);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setLwRatio_1(SIGContainAdapter* self, double lwRatio) {
  self->setLwRatio(lwRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setMaxFireSize_1(SIGContainAdapter* self, int maxFireSize) {
  self->setMaxFireSize(maxFireSize);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setMaxFireTime_1(SIGContainAdapter* self, int maxFireTime) {
  self->setMaxFireTime(maxFireTime);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setMaxSteps_1(SIGContainAdapter* self, int maxSteps) {
  self->setMaxSteps(maxSteps);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setMinSteps_1(SIGContainAdapter* self, int minSteps) {
  self->setMinSteps(minSteps);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setReportRate_2(SIGContainAdapter* self, double reportRate, SpeedUnits_SpeedUnitsEnum speedUnits) {
  self->setReportRate(reportRate, speedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setReportSize_2(SIGContainAdapter* self, double reportSize, AreaUnits_AreaUnitsEnum areaUnits) {
  self->setReportSize(reportSize, areaUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setRetry_1(SIGContainAdapter* self, bool retry) {
  self->setRetry(retry);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setTactic_1(SIGContainAdapter* self, ContainTactic tactic) {
  self->setTactic(tactic);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter___destroy___0(SIGContainAdapter* self) {
  delete self;
}

// SIGIgniteInputs

SIGIgniteInputs* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_SIGIgniteInputs_0() {
  return new SIGIgniteInputs();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_initializeMembers_0(SIGIgniteInputs* self) {
  self->initializeMembers();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_setAirTemperature_2(SIGIgniteInputs* self, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnits) {
  self->setAirTemperature(airTemperature, temperatureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_setDuffDepth_2(SIGIgniteInputs* self, double duffDepth, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setDuffDepth(duffDepth, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_setIgnitionFuelBedType_1(SIGIgniteInputs* self, IgnitionFuelBedType fuelBedType) {
  self->setIgnitionFuelBedType(fuelBedType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_setLightningChargeType_1(SIGIgniteInputs* self, LightningCharge lightningChargeType) {
  self->setLightningChargeType(lightningChargeType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_setMoistureHundredHour_2(SIGIgniteInputs* self, double hundredHourMoisture, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureHundredHour(hundredHourMoisture, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_setMoistureOneHour_2(SIGIgniteInputs* self, double moistureOneHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureOneHour(moistureOneHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_setSunShade_2(SIGIgniteInputs* self, double sunShade, CoverUnits_CoverUnitsEnum sunShadeUnits) {
  self->setSunShade(sunShade, sunShadeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_updateIgniteInputs_11(SIGIgniteInputs* self, double moistureOneHour, double moistureHundredHour, MoistureUnits_MoistureUnitsEnum moistureUnits, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnits, double sunShade, CoverUnits_CoverUnitsEnum sunShadeUnits, IgnitionFuelBedType fuelBedType, double duffDepth, LengthUnits_LengthUnitsEnum duffDepthUnits, LightningCharge lightningChargeType) {
  self->updateIgniteInputs(moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType);
}

IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_getIgnitionFuelBedType_0(SIGIgniteInputs* self) {
  return self->getIgnitionFuelBedType();
}

LightningCharge EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_getLightningChargeType_0(SIGIgniteInputs* self) {
  return self->getLightningChargeType();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_getAirTemperature_1(SIGIgniteInputs* self, TemperatureUnits_TemperatureUnitsEnum desiredUnits) {
  return self->getAirTemperature(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_getDuffDepth_1(SIGIgniteInputs* self, LengthUnits_LengthUnitsEnum desiredUnits) {
  return self->getDuffDepth(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_getMoistureHundredHour_1(SIGIgniteInputs* self, MoistureUnits_MoistureUnitsEnum desiredUnits) {
  return self->getMoistureHundredHour(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_getMoistureOneHour_1(SIGIgniteInputs* self, MoistureUnits_MoistureUnitsEnum desiredUnits) {
  return self->getMoistureOneHour(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs_getSunShade_1(SIGIgniteInputs* self, CoverUnits_CoverUnitsEnum desiredUnits) {
  return self->getSunShade(desiredUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgniteInputs___destroy___0(SIGIgniteInputs* self) {
  delete self;
}

// SIGIgnite

SIGIgnite* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_SIGIgnite_0() {
  return new SIGIgnite();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_initializeMembers_0(SIGIgnite* self) {
  self->initializeMembers();
}

IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getFuelBedType_0(SIGIgnite* self) {
  return self->getFuelBedType();
}

LightningCharge EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getLightningChargeType_0(SIGIgnite* self) {
  return self->getLightningChargeType();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_1(SIGIgnite* self, ProbabilityUnits_ProbabilityUnitsEnum desiredUnits) {
  return self->calculateFirebrandIgnitionProbability(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_calculateLightningIgnitionProbability_1(SIGIgnite* self, ProbabilityUnits_ProbabilityUnitsEnum desiredUnits) {
  return self->calculateLightningIgnitionProbability(desiredUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setAirTemperature_2(SIGIgnite* self, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnites) {
  self->setAirTemperature(airTemperature, temperatureUnites);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setDuffDepth_2(SIGIgnite* self, double duffDepth, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setDuffDepth(duffDepth, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setIgnitionFuelBedType_1(SIGIgnite* self, IgnitionFuelBedType fuelBedType_) {
  self->setIgnitionFuelBedType(fuelBedType_);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setLightningChargeType_1(SIGIgnite* self, LightningCharge lightningChargeType) {
  self->setLightningChargeType(lightningChargeType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setMoistureHundredHour_2(SIGIgnite* self, double moistureHundredHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureHundredHour(moistureHundredHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setMoistureOneHour_2(SIGIgnite* self, double moistureOneHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureOneHour(moistureOneHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setSunShade_2(SIGIgnite* self, double sunShade, CoverUnits_CoverUnitsEnum sunShadeUnits) {
  self->setSunShade(sunShade, sunShadeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_updateIgniteInputs_11(SIGIgnite* self, double moistureOneHour, double moistureHundredHour, MoistureUnits_MoistureUnitsEnum moistureUnits, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnits, double sunShade, CoverUnits_CoverUnitsEnum sunShadeUnits, IgnitionFuelBedType fuelBedType, double duffDepth, LengthUnits_LengthUnitsEnum duffDepthUnits, LightningCharge lightningChargeType) {
  self->updateIgniteInputs(moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getAirTemperature_1(SIGIgnite* self, TemperatureUnits_TemperatureUnitsEnum desiredUnits) {
  return self->getAirTemperature(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getDuffDepth_1(SIGIgnite* self, LengthUnits_LengthUnitsEnum desiredUnits) {
  return self->getDuffDepth(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getFuelTemperature_1(SIGIgnite* self, TemperatureUnits_TemperatureUnitsEnum desiredUnits) {
  return self->getFuelTemperature(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getMoistureHundredHour_1(SIGIgnite* self, MoistureUnits_MoistureUnitsEnum desiredUnits) {
  return self->getMoistureHundredHour(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getMoistureOneHour_1(SIGIgnite* self, MoistureUnits_MoistureUnitsEnum desiredUnits) {
  return self->getMoistureOneHour(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getSunShade_1(SIGIgnite* self, CoverUnits_CoverUnitsEnum desiredUnits) {
  return self->getSunShade(desiredUnits);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_isFuelDepthNeeded_0(SIGIgnite* self) {
  return self->isFuelDepthNeeded();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite___destroy___0(SIGIgnite* self) {
  delete self;
}

// SIGSpotInputs

SIGSpotInputs* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_SIGSpotInputs_0() {
  return new SIGSpotInputs();
}

SpotFireLocation EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getLocation_0(SIGSpotInputs* self) {
  return self->getLocation();
}

SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getTreeSpecies_0(SIGSpotInputs* self) {
  return self->getTreeSpecies();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setBurningPileFlameHeight_2(SIGSpotInputs* self, double buringPileFlameHeight, LengthUnits_LengthUnitsEnum flameHeightUnits) {
  self->setBurningPileFlameHeight(buringPileFlameHeight, flameHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setDBH_2(SIGSpotInputs* self, double DBH, LengthUnits_LengthUnitsEnum DBHUnits) {
  self->setDBH(DBH, DBHUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setDownwindCoverHeight_2(SIGSpotInputs* self, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  self->setDownwindCoverHeight(downwindCoverHeight, coverHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setLocation_1(SIGSpotInputs* self, SpotFireLocation location) {
  self->setLocation(location);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setRidgeToValleyDistance_2(SIGSpotInputs* self, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits) {
  self->setRidgeToValleyDistance(ridgeToValleyDistance, ridgeToValleyDistanceUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setRidgeToValleyElevation_2(SIGSpotInputs* self, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits) {
  self->setRidgeToValleyElevation(ridgeToValleyElevation, elevationUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setSurfaceFlameLength_2(SIGSpotInputs* self, double surfaceFlameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->setSurfaceFlameLength(surfaceFlameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setTorchingTrees_1(SIGSpotInputs* self, int torchingTrees) {
  self->setTorchingTrees(torchingTrees);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setTreeHeight_2(SIGSpotInputs* self, double treeHeight, LengthUnits_LengthUnitsEnum treeHeightUnits) {
  self->setTreeHeight(treeHeight, treeHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setTreeSpecies_1(SIGSpotInputs* self, SpotTreeSpecies treeSpecies) {
  self->setTreeSpecies(treeSpecies);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_setWindSpeedAtTwentyFeet_2(SIGSpotInputs* self, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setWindSpeedAtTwentyFeet(windSpeedAtTwentyFeet, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_updateSpotInputsForBurningPile_11(SIGSpotInputs* self, SpotFireLocation location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, double buringPileFlameHeight, LengthUnits_LengthUnitsEnum flameHeightUnits, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->updateSpotInputsForBurningPile(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_updateSpotInputsForSurfaceFire_11(SIGSpotInputs* self, SpotFireLocation location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits, double surfaceFlameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->updateSpotInputsForSurfaceFire(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits, surfaceFlameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_updateSpotInputsForTorchingTrees_15(SIGSpotInputs* self, SpotFireLocation location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, int torchingTrees, double DBH, LengthUnits_LengthUnitsEnum DBHUnits, double treeHeight, LengthUnits_LengthUnitsEnum treeHeightUnits, SpotTreeSpecies treeSpecies, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->updateSpotInputsForTorchingTrees(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getBurningPileFlameHeight_1(SIGSpotInputs* self, LengthUnits_LengthUnitsEnum flameHeightUnits) {
  return self->getBurningPileFlameHeight(flameHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getDBH_1(SIGSpotInputs* self, LengthUnits_LengthUnitsEnum DBHUnits) {
  return self->getDBH(DBHUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getDownwindCoverHeight_1(SIGSpotInputs* self, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  return self->getDownwindCoverHeight(coverHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getRidgeToValleyDistance_1(SIGSpotInputs* self, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits) {
  return self->getRidgeToValleyDistance(ridgeToValleyDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getRidgeToValleyElevation_1(SIGSpotInputs* self, LengthUnits_LengthUnitsEnum elevationUnits) {
  return self->getRidgeToValleyElevation(elevationUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getSurfaceFlameLength_1(SIGSpotInputs* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getSurfaceFlameLength(flameLengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getTreeHeight_1(SIGSpotInputs* self, LengthUnits_LengthUnitsEnum treeHeightUnits) {
  return self->getTreeHeight(treeHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getWindSpeedAtTwentyFeet_1(SIGSpotInputs* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  return self->getWindSpeedAtTwentyFeet(windSpeedUnits);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs_getTorchingTrees_0(SIGSpotInputs* self) {
  return self->getTorchingTrees();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpotInputs___destroy___0(SIGSpotInputs* self) {
  delete self;
}

// SIGSpot

SIGSpot* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_SIGSpot_0() {
  return new SIGSpot();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_initializeMembers_0(SIGSpot* self) {
  self->initializeMembers();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_calculateSpottingDistanceFromBurningPile_0(SIGSpot* self) {
  self->calculateSpottingDistanceFromBurningPile();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_calculateSpottingDistanceFromSurfaceFire_0(SIGSpot* self) {
  self->calculateSpottingDistanceFromSurfaceFire();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_calculateSpottingDistanceFromTorchingTrees_0(SIGSpot* self) {
  self->calculateSpottingDistanceFromTorchingTrees();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setBurningPileFlameHeight_2(SIGSpot* self, double buringPileflameHeight, LengthUnits_LengthUnitsEnum flameHeightUnits) {
  self->setBurningPileFlameHeight(buringPileflameHeight, flameHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setDBH_2(SIGSpot* self, double DBH, LengthUnits_LengthUnitsEnum DBHUnits) {
  self->setDBH(DBH, DBHUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setDownwindCoverHeight_2(SIGSpot* self, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  self->setDownwindCoverHeight(downwindCoverHeight, coverHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setFlameLength_2(SIGSpot* self, double flameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->setFlameLength(flameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setLocation_1(SIGSpot* self, SpotFireLocation location) {
  self->setLocation(location);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setRidgeToValleyDistance_2(SIGSpot* self, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits) {
  self->setRidgeToValleyDistance(ridgeToValleyDistance, ridgeToValleyDistanceUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setRidgeToValleyElevation_2(SIGSpot* self, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits) {
  self->setRidgeToValleyElevation(ridgeToValleyElevation, elevationUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setTorchingTrees_1(SIGSpot* self, int torchingTrees) {
  self->setTorchingTrees(torchingTrees);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setTreeHeight_2(SIGSpot* self, double treeHeight, LengthUnits_LengthUnitsEnum treeHeightUnits) {
  self->setTreeHeight(treeHeight, treeHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setTreeSpecies_1(SIGSpot* self, SpotTreeSpecies treeSpecies) {
  self->setTreeSpecies(treeSpecies);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setWindSpeedAtTwentyFeet_2(SIGSpot* self, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setWindSpeedAtTwentyFeet(windSpeedAtTwentyFeet, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_updateSpotInputsForBurningPile_11(SIGSpot* self, SpotFireLocation location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, double buringPileFlameHeight, LengthUnits_LengthUnitsEnum flameHeightUnits, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->updateSpotInputsForBurningPile(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_updateSpotInputsForSurfaceFire_11(SIGSpot* self, SpotFireLocation location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits, double flameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->updateSpotInputsForSurfaceFire(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_updateSpotInputsForTorchingTrees_15(SIGSpot* self, SpotFireLocation location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, int torchingTrees, double DBH, LengthUnits_LengthUnitsEnum DBHUnits, double treeHeight, LengthUnits_LengthUnitsEnum treeHeightUnits, SpotTreeSpecies treeSpecies, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->updateSpotInputsForTorchingTrees(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getTorchingTrees_0(SIGSpot* self) {
  return self->getTorchingTrees();
}

SpotFireLocation EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getLocation_0(SIGSpot* self) {
  return self->getLocation();
}

SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getTreeSpecies_0(SIGSpot* self) {
  return self->getTreeSpecies();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getBurningPileFlameHeight_1(SIGSpot* self, LengthUnits_LengthUnitsEnum flameHeightUnits) {
  return self->getBurningPileFlameHeight(flameHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getCoverHeightUsedForBurningPile_1(SIGSpot* self, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  return self->getCoverHeightUsedForBurningPile(coverHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getCoverHeightUsedForSurfaceFire_1(SIGSpot* self, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  return self->getCoverHeightUsedForSurfaceFire(coverHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getCoverHeightUsedForTorchingTrees_1(SIGSpot* self, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  return self->getCoverHeightUsedForTorchingTrees(coverHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getDBH_1(SIGSpot* self, LengthUnits_LengthUnitsEnum DBHUnits) {
  return self->getDBH(DBHUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getDownwindCoverHeight_1(SIGSpot* self, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  return self->getDownwindCoverHeight(coverHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getFlameDurationForTorchingTrees_1(SIGSpot* self, TimeUnits_TimeUnitsEnum durationUnits) {
  return self->getFlameDurationForTorchingTrees(durationUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getFlameHeightForTorchingTrees_1(SIGSpot* self, LengthUnits_LengthUnitsEnum flameHeightUnits) {
  return self->getFlameHeightForTorchingTrees(flameHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getFlameRatioForTorchingTrees_0(SIGSpot* self) {
  return self->getFlameRatioForTorchingTrees();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxFirebrandHeightFromBurningPile_1(SIGSpot* self, LengthUnits_LengthUnitsEnum firebrandHeightUnits) {
  return self->getMaxFirebrandHeightFromBurningPile(firebrandHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxFirebrandHeightFromSurfaceFire_1(SIGSpot* self, LengthUnits_LengthUnitsEnum firebrandHeightUnits) {
  return self->getMaxFirebrandHeightFromSurfaceFire(firebrandHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxFirebrandHeightFromTorchingTrees_1(SIGSpot* self, LengthUnits_LengthUnitsEnum firebrandHeightUnits) {
  return self->getMaxFirebrandHeightFromTorchingTrees(firebrandHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromBurningPile_1(SIGSpot* self, LengthUnits_LengthUnitsEnum spottingDistanceUnits) {
  return self->getMaxFlatTerrainSpottingDistanceFromBurningPile(spottingDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromSurfaceFire_1(SIGSpot* self, LengthUnits_LengthUnitsEnum spottingDistanceUnits) {
  return self->getMaxFlatTerrainSpottingDistanceFromSurfaceFire(spottingDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxFlatTerrainSpottingDistanceFromTorchingTrees_1(SIGSpot* self, LengthUnits_LengthUnitsEnum spottingDistanceUnits) {
  return self->getMaxFlatTerrainSpottingDistanceFromTorchingTrees(spottingDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromBurningPile_1(SIGSpot* self, LengthUnits_LengthUnitsEnum spottingDistanceUnits) {
  return self->getMaxMountainousTerrainSpottingDistanceFromBurningPile(spottingDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromSurfaceFire_1(SIGSpot* self, LengthUnits_LengthUnitsEnum spottingDistanceUnits) {
  return self->getMaxMountainousTerrainSpottingDistanceFromSurfaceFire(spottingDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromTorchingTrees_1(SIGSpot* self, LengthUnits_LengthUnitsEnum spottingDistanceUnits) {
  return self->getMaxMountainousTerrainSpottingDistanceFromTorchingTrees(spottingDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getRidgeToValleyDistance_1(SIGSpot* self, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits) {
  return self->getRidgeToValleyDistance(ridgeToValleyDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getRidgeToValleyElevation_1(SIGSpot* self, LengthUnits_LengthUnitsEnum elevationUnits) {
  return self->getRidgeToValleyElevation(elevationUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getSurfaceFlameLength_1(SIGSpot* self, LengthUnits_LengthUnitsEnum surfaceFlameLengthUnits) {
  return self->getSurfaceFlameLength(surfaceFlameLengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getTreeHeight_1(SIGSpot* self, LengthUnits_LengthUnitsEnum treeHeightUnits) {
  return self->getTreeHeight(treeHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getWindSpeedAtTwentyFeet_1(SIGSpot* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  return self->getWindSpeedAtTwentyFeet(windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot___destroy___0(SIGSpot* self) {
  delete self;
}

// FuelModels

FuelModels* EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_FuelModels_0() {
  return new FuelModels();
}

FuelModels* EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_FuelModels_1(const FuelModels* rhs) {
  return new FuelModels(*rhs);
}

FuelModels* EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_equal_1(FuelModels* self, const FuelModels* rhs) {
  return &(*self = *rhs);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_clearCustomFuelModel_1(FuelModels* self, int fuelModelNumber) {
  return self->clearCustomFuelModel(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getIsDynamic_1(FuelModels* self, int fuelModelNumber) {
  return self->getIsDynamic(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_isFuelModelDefined_1(FuelModels* self, int fuelModelNumber) {
  return self->isFuelModelDefined(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_setCustomFuelModel_21(FuelModels* self, int fuelModelNumberIn, char code, char name, double fuelBedDepth, LengthUnits_LengthUnitsEnum lengthUnits, double moistureOfExtinctionDead, MoistureUnits_MoistureUnitsEnum moistureUnits, double heatOfCombustionDead, double heatOfCombustionLive, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits, double fuelLoadOneHour, double fuelLoadTenHour, double fuelLoadHundredHour, double fuelLoadLiveHerbaceous, double fuelLoadLiveWoody, LoadingUnits_LoadingUnitsEnum loadingUnits, double savrOneHour, double savrLiveHerbaceous, double savrLiveWoody, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits, bool isDynamic) {
  return self->setCustomFuelModel(fuelModelNumberIn, code, name, fuelBedDepth, lengthUnits, moistureOfExtinctionDead, moistureUnits, heatOfCombustionDead, heatOfCombustionLive, heatOfCombustionUnits, fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadLiveHerbaceous, fuelLoadLiveWoody, loadingUnits, savrOneHour, savrLiveHerbaceous, savrLiveWoody, savrUnits, isDynamic);
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelCode_1(FuelModels* self, int fuelModelNumber) {
  return self->getFuelCode(fuelModelNumber);
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelName_1(FuelModels* self, int fuelModelNumber) {
  return self->getFuelName(fuelModelNumber);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelLoadHundredHour_2(FuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadHundredHour(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelLoadLiveHerbaceous_2(FuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveHerbaceous(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelLoadLiveWoody_2(FuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveWoody(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelLoadOneHour_2(FuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadOneHour(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelLoadTenHour_2(FuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadTenHour(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getFuelbedDepth_2(FuelModels* self, int fuelModelNumber, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFuelbedDepth(fuelModelNumber, lengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getHeatOfCombustionDead_2(FuelModels* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getHeatOfCombustionDead(fuelModelNumber, heatOfCombustionUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getHeatOfCombustionLive_2(FuelModels* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getHeatOfCombustionLive(fuelModelNumber, heatOfCombustionUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getMoistureOfExtinctionDead_2(FuelModels* self, int fuelModelNumber, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureOfExtinctionDead(fuelModelNumber, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getSavrLiveHerbaceous_2(FuelModels* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getSavrLiveHerbaceous(fuelModelNumber, savrUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getSavrLiveWoody_2(FuelModels* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getSavrLiveWoody(fuelModelNumber, savrUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels_getSavrOneHour_2(FuelModels* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getSavrOneHour(fuelModelNumber, savrUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_FuelModels___destroy___0(FuelModels* self) {
  delete self;
}

// SIGSurface

SIGSurface* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_SIGSurface_1(SIGSurface* rhs) {
  return new SIGSurface(*rhs);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_initializeMembers_0(SIGSurface* self) {
  self->initializeMembers();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_1(SIGSurface* self, double directionOfinterest) {
  self->doSurfaceRunInDirectionOfInterest(directionOfinterest);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0(SIGSurface* self) {
  self->doSurfaceRunInDirectionOfMaxSpread();
}

WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindAdjustmentFactorCalculationMethod_0(SIGSurface* self) {
  return self->getWindAdjustmentFactorCalculationMethod();
}

WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindAndSpreadOrientationMode_0(SIGSurface* self) {
  return self->getWindAndSpreadOrientationMode();
}

WindHeightInputMode_WindHeightInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindHeightInputMode_0(SIGSurface* self) {
  return self->getWindHeightInputMode();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isAllFuelLoadZero_1(SIGSurface* self, int fuelModelNumber) {
  return self->isAllFuelLoadZero(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isUsingTwoFuelModels_0(SIGSurface* self) {
  return self->isUsingTwoFuelModels();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_calculateFlameLength_1(SIGSurface* self, double firelineIntensity) {
  return self->calculateFlameLength(firelineIntensity);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspect_0(SIGSurface* self) {
  return self->getAspect();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getBulkDensity_1(SIGSurface* self, DensityUnits_DensityUnitsEnum densityUnits) {
  return self->getBulkDensity(densityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCanopyCover_1(SIGSurface* self, CoverUnits_CoverUnitsEnum coverUnits) {
  return self->getCanopyCover(coverUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCanopyHeight_1(SIGSurface* self, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  return self->getCanopyHeight(canopyHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCrownRatio_0(SIGSurface* self) {
  return self->getCrownRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0(SIGSurface* self) {
  return self->getDirectionOfMaxSpread();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getEllipticalA_3(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getEllipticalA(lengthUnits, elapsedTime, timeUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getEllipticalB_3(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getEllipticalB(lengthUnits, elapsedTime, timeUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getEllipticalC_3(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getEllipticalC(lengthUnits, elapsedTime, timeUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFireArea_2(SIGSurface* self, AreaUnits_AreaUnitsEnum areaUnits, double elapsedTime) {
  return self->getFireArea(areaUnits, elapsedTime);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFireEccentricity_0(SIGSurface* self) {
  return self->getFireEccentricity();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0(SIGSurface* self) {
  return self->getFireLengthToWidthRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFirePerimeter_2(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime) {
  return self->getFirePerimeter(lengthUnits, elapsedTime);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFirelineIntensity_1(SIGSurface* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getFirelineIntensity(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFlameLength_1(SIGSurface* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getFlameLength(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeatPerUnitArea_1(SIGSurface* self, HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum heatPerUnitAreaUnits) {
  return self->getHeatPerUnitArea(heatPerUnitAreaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeatSink_1(SIGSurface* self, HeatSinkUnits_HeatSinkUnitsEnum heatSinkUnits) {
  return self->getHeatSink(heatSinkUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMidflameWindspeed_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  return self->getMidflameWindspeed(windSpeedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureHundredHour_1(SIGSurface* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureHundredHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1(SIGSurface* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureLiveHerbaceous(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureLiveWoody_1(SIGSurface* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureLiveWoody(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureOneHour_1(SIGSurface* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureOneHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureTenHour_1(SIGSurface* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureTenHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getReactionIntensity_1(SIGSurface* self, HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum reactiontionIntensityUnits) {
  return self->getReactionIntensity(reactiontionIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getResidenceTime_1(SIGSurface* self, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getResidenceTime(timeUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSlope_1(SIGSurface* self, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  return self->getSlope(slopeUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSlopeFactor_0(SIGSurface* self) {
  return self->getSlopeFactor();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSpreadRate_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getSpreadRateInDirectionOfInterest(spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindDirection_0(SIGSurface* self) {
  return self->getWindDirection();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindSpeed_2(SIGSurface* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  return self->getWindSpeed(windSpeedUnits, windHeightInputMode);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelModelNumber_0(SIGSurface* self) {
  return self->getFuelModelNumber();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setAspect_1(SIGSurface* self, double aspect) {
  self->setAspect(aspect);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCanopyCover_2(SIGSurface* self, double canopyCover, CoverUnits_CoverUnitsEnum coverUnits) {
  self->setCanopyCover(canopyCover, coverUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCanopyHeight_2(SIGSurface* self, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  self->setCanopyHeight(canopyHeight, canopyHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCrownRatio_1(SIGSurface* self, double crownRatio) {
  self->setCrownRatio(crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setFirstFuelModelNumber_1(SIGSurface* self, int firstFuelModelNumber) {
  self->setFirstFuelModelNumber(firstFuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setFuelModelNumber_1(SIGSurface* self, int fuelModelNumber) {
  self->setFuelModelNumber(fuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setFuelModels_1(SIGSurface* self, FuelModels* fuelModels) {
  self->setFuelModels(*fuelModels);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureHundredHour_2(SIGSurface* self, double moistureHundredHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureHundredHour(moistureHundredHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2(SIGSurface* self, double moistureLiveHerbaceous, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureLiveHerbaceous(moistureLiveHerbaceous, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureLiveWoody_2(SIGSurface* self, double moistureLiveWoody, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureLiveWoody(moistureLiveWoody, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureOneHour_2(SIGSurface* self, double moistureOneHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureOneHour(moistureOneHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureTenHour_2(SIGSurface* self, double moistureTenHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureTenHour(moistureTenHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setSecondFuelModelNumber_1(SIGSurface* self, int secondFuelModelNumber) {
  self->setSecondFuelModelNumber(secondFuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setSlope_2(SIGSurface* self, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  self->setSlope(slope, slopeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setTwoFuelModelsFirstFuelModelCoverage_2(SIGSurface* self, double firstFuelModelCoverage, CoverUnits_CoverUnitsEnum coverUnits) {
  self->setTwoFuelModelsFirstFuelModelCoverage(firstFuelModelCoverage, coverUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setTwoFuelModelsMethod_1(SIGSurface* self, TwoFuelModelsMethod_TwoFuelModelsMethodEnum twoFuelModelsMethod) {
  self->setTwoFuelModelsMethod(twoFuelModelsMethod);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setUserProvidedWindAdjustmentFactor_1(SIGSurface* self, double userProvidedWindAdjustmentFactor) {
  self->setUserProvidedWindAdjustmentFactor(userProvidedWindAdjustmentFactor);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setWindAdjustmentFactorCalculationMethod_1(SIGSurface* self, WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum windAdjustmentFactorCalculationMethod) {
  self->setWindAdjustmentFactorCalculationMethod(windAdjustmentFactorCalculationMethod);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setWindAndSpreadOrientationMode_1(SIGSurface* self, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode) {
  self->setWindAndSpreadOrientationMode(windAndSpreadOrientationMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setWindDirection_1(SIGSurface* self, double windDirection) {
  self->setWindDirection(windDirection);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setWindHeightInputMode_1(SIGSurface* self, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindHeightInputMode(windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setWindSpeed_3(SIGSurface* self, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindSpeed(windSpeed, windSpeedUnits, windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputs_20(SIGSurface* self, int fuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits_MoistureUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits_CoverUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio) {
  self->updateSurfaceInputs(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_23(SIGSurface* self, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits_MoistureUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double ageOfRough, double heightOfUnderstory, double palmettoCoverage, double overstoryBasalArea, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits_CoverUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio) {
  self->updateSurfaceInputsForPalmettoGallbery(moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_24(SIGSurface* self, int firstfuelModelNumber, int secondFuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits_MoistureUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double firstFuelModelCoverage, CoverUnits_CoverUnitsEnum firstFuelModelCoverageUnits, TwoFuelModelsMethod_TwoFuelModelsMethodEnum twoFuelModelsMethod, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits_CoverUnitsEnum canopyCoverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio) {
  self->updateSurfaceInputsForTwoFuelModels(firstfuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyCoverUnits, canopyHeight, canopyHeightUnits, crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_23(SIGSurface* self, int aspenFuelModelNumber, double aspenCuringLevel, AspenFireSeverity_AspenFireSeverityEnum aspenFireSeverity, double DBH, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits_MoistureUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits_CoverUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio) {
  self->updateSurfaceInputsForWesternAspen(aspenFuelModelNumber, aspenCuringLevel, aspenFireSeverity, DBH, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface___destroy___0(SIGSurface* self) {
  delete self;
}

// PalmettoGallberry

PalmettoGallberry* EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_PalmettoGallberry_0() {
  return new PalmettoGallberry();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_initializeMembers_0(PalmettoGallberry* self) {
  self->initializeMembers();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0(PalmettoGallberry* self) {
  return self->getHeatOfCombustionLive();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2(PalmettoGallberry* self, double ageOfRough, double overstoryBasalArea) {
  return self->calculatePalmettoGallberyLitterLoad(ageOfRough, overstoryBasalArea);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveOneHourLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLiveOneHourLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyDeadFoliageLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0(PalmettoGallberry* self) {
  return self->getHeatOfCombustionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3(PalmettoGallberry* self, double ageOfRough, double palmettoCoverage, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyLiveFoliageLoad(ageOfRough, palmettoCoverage, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveTenHourLoad_2(PalmettoGallberry* self, double ageOfRough, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyLiveTenHourLoad(ageOfRough, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadTenHourLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyDeadTenHourLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0(PalmettoGallberry* self) {
  return self->getMoistureOfExtinctionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLiveFoliageLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLitterLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadTenHourLoad_2(PalmettoGallberry* self, double ageOfRough, double palmettoCoverage) {
  return self->calculatePalmettoGallberyDeadTenHourLoad(ageOfRough, palmettoCoverage);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveOneHourLoad_2(PalmettoGallberry* self, double ageOfRough, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyLiveOneHourLoad(ageOfRough, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyFuelBedDepth();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2(PalmettoGallberry* self, double ageOfRough, double palmettoCoverage) {
  return self->calculatePalmettoGallberyDeadFoliageLoad(ageOfRough, palmettoCoverage);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadOneHourLoad_2(PalmettoGallberry* self, double ageOfRough, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyDeadOneHourLoad(ageOfRough, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveTenHourLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLiveTenHourLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadOneHourLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyDeadOneHourLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1(PalmettoGallberry* self, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyFuelBedDepth(heightOfUnderstory);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry___destroy___0(PalmettoGallberry* self) {
  delete self;
}

// WesternAspen

WesternAspen* EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_WesternAspen_0() {
  return new WesternAspen();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_initializeMembers_0(WesternAspen* self) {
  self->initializeMembers();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_calculateAspenMortality_3(WesternAspen* self, int severity, double flameLength, double DBH) {
  return self->calculateAspenMortality(severity, flameLength, DBH);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenDBH_0(WesternAspen* self) {
  return self->getAspenDBH();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenFuelBedDepth_1(WesternAspen* self, int typeIndex) {
  return self->getAspenFuelBedDepth(typeIndex);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenHeatOfCombustionDead_0(WesternAspen* self) {
  return self->getAspenHeatOfCombustionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenHeatOfCombustionLive_0(WesternAspen* self) {
  return self->getAspenHeatOfCombustionLive();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_2(WesternAspen* self, int aspenFuelModelNumber, double aspenCuringLevel) {
  return self->getAspenLoadDeadOneHour(aspenFuelModelNumber, aspenCuringLevel);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_1(WesternAspen* self, int aspenFuelModelNumber) {
  return self->getAspenLoadDeadTenHour(aspenFuelModelNumber);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_2(WesternAspen* self, int aspenFuelModelNumber, double aspenCuringLevel) {
  return self->getAspenLoadLiveHerbaceous(aspenFuelModelNumber, aspenCuringLevel);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadLiveWoody_2(WesternAspen* self, int aspenFuelModelNumber, double aspenCuringLevel) {
  return self->getAspenLoadLiveWoody(aspenFuelModelNumber, aspenCuringLevel);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0(WesternAspen* self) {
  return self->getAspenMoistureOfExtinctionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenMortality_0(WesternAspen* self) {
  return self->getAspenMortality();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_2(WesternAspen* self, int aspenFuelModelNumber, double aspenCuringLevel) {
  return self->getAspenSavrDeadOneHour(aspenFuelModelNumber, aspenCuringLevel);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0(WesternAspen* self) {
  return self->getAspenSavrDeadTenHour();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0(WesternAspen* self) {
  return self->getAspenSavrLiveHerbaceous();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrLiveWoody_2(WesternAspen* self, int aspenFuelModelNumber, double aspenCuringLevel) {
  return self->getAspenSavrLiveWoody(aspenFuelModelNumber, aspenCuringLevel);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen___destroy___0(WesternAspen* self) {
  delete self;
}

// Crown

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_initializeMembers_0(Crown* self) {
  self->initializeMembers();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_doCrownRunRothermel_0(Crown* self) {
  self->doCrownRunRothermel();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_doCrownRunScottAndReinhardt_0(Crown* self) {
  self->doCrownRunScottAndReinhardt();
}

FireType_FireTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getFireType_0(Crown* self) {
  return self->getFireType();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getAspect_0(Crown* self) {
  return self->getAspect();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCanopyBaseHeight_1(Crown* self, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  return self->getCanopyBaseHeight(canopyHeightUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCanopyBulkDensity_1(Crown* self, DensityUnits_DensityUnitsEnum canopyBulkDensityUnits) {
  return self->getCanopyBulkDensity(canopyBulkDensityUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCanopyCover_1(Crown* self, CoverUnits_CoverUnitsEnum canopyCoverUnits) {
  return self->getCanopyCover(canopyCoverUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCanopyHeight_1(Crown* self, LengthUnits_LengthUnitsEnum canopyHeighUnits) {
  return self->getCanopyHeight(canopyHeighUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCriticalOpenWindSpeed_1(Crown* self, SpeedUnits_SpeedUnitsEnum speedUnits) {
  return self->getCriticalOpenWindSpeed(speedUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCrownFireLengthToWidthRatio_0(Crown* self) {
  return self->getCrownFireLengthToWidthRatio();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCrownFireSpreadRate_1(Crown* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getCrownFireSpreadRate(spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCrownFirelineIntensity_1(Crown* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getCrownFirelineIntensity(firelineIntensityUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCrownFlameLength_1(Crown* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getCrownFlameLength(flameLengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getCrownRatio_0(Crown* self) {
  return self->getCrownRatio();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getFinalFirelineIntesity_1(Crown* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getFinalFirelineIntesity(firelineIntensityUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getFinalFlameLength_1(Crown* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getFinalFlameLength(flameLengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getFinalHeatPerUnitArea_1(Crown* self, HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum heatPerUnitAreaUnits) {
  return self->getFinalHeatPerUnitArea(heatPerUnitAreaUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getFinalSpreadRate_1(Crown* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getFinalSpreadRate(spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getMoistureFoliar_1(Crown* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureFoliar(moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getMoistureHundredHour_1(Crown* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureHundredHour(moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getMoistureLiveHerbaceous_1(Crown* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureLiveHerbaceous(moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getMoistureLiveWoody_1(Crown* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureLiveWoody(moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getMoistureOneHour_1(Crown* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureOneHour(moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getMoistureTenHour_1(Crown* self, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  return self->getMoistureTenHour(moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getSlope_1(Crown* self, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  return self->getSlope(slopeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getSurfaceFireSpreadRate_1(Crown* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getSurfaceFireSpreadRate(spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getWindDirection_0(Crown* self) {
  return self->getWindDirection();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getWindSpeed_2(Crown* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  return self->getWindSpeed(windSpeedUnits, windHeightInputMode);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_getFuelModelNumber_0(Crown* self) {
  return self->getFuelModelNumber();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setAspect_1(Crown* self, double aspect) {
  self->setAspect(aspect);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setCanopyBaseHeight_2(Crown* self, double canopyBaseHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  self->setCanopyBaseHeight(canopyBaseHeight, canopyHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setCanopyBulkDensity_2(Crown* self, double canopyBulkDensity, DensityUnits_DensityUnitsEnum densityUnits) {
  self->setCanopyBulkDensity(canopyBulkDensity, densityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setCanopyCover_2(Crown* self, double canopyCover, CoverUnits_CoverUnitsEnum coverUnits) {
  self->setCanopyCover(canopyCover, coverUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setCanopyHeight_2(Crown* self, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  self->setCanopyHeight(canopyHeight, canopyHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setCrownRatio_1(Crown* self, double crownRatio) {
  self->setCrownRatio(crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setFuelModelNumber_1(Crown* self, int fuelModelNumber) {
  self->setFuelModelNumber(fuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setFuelModels_1(Crown* self, FuelModels* fuelModels) {
  self->setFuelModels(*fuelModels);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setMoistureFoliar_2(Crown* self, double foliarMoisture, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureFoliar(foliarMoisture, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setMoistureHundredHour_2(Crown* self, double moistureHundredHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureHundredHour(moistureHundredHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setMoistureLiveHerbaceous_2(Crown* self, double moistureLiveHerbaceous, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureLiveHerbaceous(moistureLiveHerbaceous, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setMoistureLiveWoody_2(Crown* self, double moistureLiveWoody, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureLiveWoody(moistureLiveWoody, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setMoistureOneHour_2(Crown* self, double moistureOneHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureOneHour(moistureOneHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setMoistureTenHour_2(Crown* self, double moistureTenHour, MoistureUnits_MoistureUnitsEnum moistureUnits) {
  self->setMoistureTenHour(moistureTenHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setSlope_2(Crown* self, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  self->setSlope(slope, slopeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setUserProvidedWindAdjustmentFactor_1(Crown* self, double userProvidedWindAdjustmentFactor) {
  self->setUserProvidedWindAdjustmentFactor(userProvidedWindAdjustmentFactor);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setWindAdjustmentFactorCalculationMethod_1(Crown* self, WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum windAdjustmentFactorCalculationMethod) {
  self->setWindAdjustmentFactorCalculationMethod(windAdjustmentFactorCalculationMethod);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setWindAndSpreadOrientationMode_1(Crown* self, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadAngleMode) {
  self->setWindAndSpreadOrientationMode(windAndSpreadAngleMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setWindDirection_1(Crown* self, double windDirection) {
  self->setWindDirection(windDirection);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setWindHeightInputMode_1(Crown* self, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindHeightInputMode(windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_setWindSpeed_3(Crown* self, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindSpeed(windSpeed, windSpeedUnits, windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_updateCrownInputs_24(Crown* self, int fuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, double moistureFoliar, MoistureUnits_MoistureUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits_CoverUnitsEnum coverUnits, double canopyHeight, double canopyBaseHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio, double canopyBulkDensity, DensityUnits_DensityUnitsEnum densityUnits) {
  self->updateCrownInputs(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureFoliar, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyBaseHeight, canopyHeightUnits, crownRatio, canopyBulkDensity, densityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown_updateCrownsSurfaceInputs_20(Crown* self, int fuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, MoistureUnits_MoistureUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, CoverUnits_CoverUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio) {
  self->updateCrownsSurfaceInputs(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_Crown___destroy___0(Crown* self) {
  delete self;
}

// SpeciesMasterTableRecord

SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_1(const SpeciesMasterTableRecord* rhs) {
  return new SpeciesMasterTableRecord(rhs);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecord___destroy___0(SpeciesMasterTableRecord* self) {
  delete self;
}

// SpeciesMasterTable

SpeciesMasterTable* EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTable_SpeciesMasterTable_0() {
  return new SpeciesMasterTable();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTable_initializeMasterTable_0(SpeciesMasterTable* self) {
  self->initializeMasterTable();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCode_1(SpeciesMasterTable* self, char* speciesCode) {
  return self->getSpeciesTableIndexFromSpeciesCode(speciesCode);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTable_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2(SpeciesMasterTable* self, char* speciesCode, EquationType equationType) {
  return self->getSpeciesTableIndexFromSpeciesCodeAndEquationType(speciesCode, equationType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTable_insertRecord_12(SpeciesMasterTable* self, char* speciesCode, char* scientificName, char* commonName, int mortalityEquation, int brkEqu, int crownCoefficientCode, int region1, int region2, int region3, int region4, EquationType equationType, CrownDamageEquationCode crownDamageEquationCode) {
  self->insertRecord(speciesCode, scientificName, commonName, mortalityEquation, brkEqu, crownCoefficientCode, region1, region2, region3, region4, equationType, crownDamageEquationCode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTable___destroy___0(SpeciesMasterTable* self) {
  delete self;
}

// SIGMortality

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_Mortality_1(SIGMortality* self, SpeciesMasterTable* speciesMasterTable) {
  self->Mortality(*speciesMasterTable);
}

const BeetleDamage EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBeetleDamage_0(SIGMortality* self) {
  return self->getBeetleDamage();
}

const CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownDamageEquationCode_0(SIGMortality* self) {
  return self->getCrownDamageEquationCode();
}

const CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownDamageEquationCodeAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getCrownDamageEquationCodeAtSpeciesTableIndex(index);
}

const CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownDamageEquationCodeFromSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  return self->getCrownDamageEquationCodeFromSpeciesCode(speciesCode);
}

const CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownDamageType_0(SIGMortality* self) {
  return self->getCrownDamageType();
}

const EquationType EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getEquationType_0(SIGMortality* self) {
  return self->getEquationType();
}

const EquationType EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getEquationTypeAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getEquationTypeAtSpeciesTableIndex(index);
}

const EquationType EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getEquationTypeFromSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  return self->getEquationTypeFromSpeciesCode(speciesCode);
}

const FireSeverity EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getFireSeverity_0(SIGMortality* self) {
  return self->getFireSeverity();
}

const FlameLengthOrScorchHeightSwitch EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightSwitch_0(SIGMortality* self) {
  return self->getFlameLengthOrScorchHeightSwitch();
}

const RegionCode EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getRegion_0(SIGMortality* self) {
  return self->getRegion();
}

const SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesRecordAtIndex_1(SIGMortality* self, int index) {
  return self->getSpeciesRecordAtIndex(index);
}

const SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesRecordBySpeciesCodeAndEquationType_2(SIGMortality* self, char* speciesCode, EquationType equationType) {
  return self->getSpeciesRecordBySpeciesCodeAndEquationType(speciesCode, equationType);
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_checkIsInRegionAtSpeciesTableIndex_2(SIGMortality* self, int index, RegionCode region) {
  return self->checkIsInRegionAtSpeciesTableIndex(index, region);
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_checkIsInRegionFromSpeciesCode_2(SIGMortality* self, char* speciesCode, RegionCode region) {
  return self->checkIsInRegionFromSpeciesCode(speciesCode, region);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBarkThickness_1(SIGMortality* self, LengthUnits_LengthUnitsEnum barkThicknessUnits) {
  return self->getBarkThickness(barkThicknessUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBasalAreaKillled_0(SIGMortality* self) {
  return self->getBasalAreaKillled();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBasalAreaPostfire_0(SIGMortality* self) {
  return self->getBasalAreaPostfire();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBasalAreaPrefire_0(SIGMortality* self) {
  return self->getBasalAreaPrefire();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBoleCharHeight_1(SIGMortality* self, LengthUnits_LengthUnitsEnum boleCharHeightUnits) {
  return self->getBoleCharHeight(boleCharHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCambiumKillRating_0(SIGMortality* self) {
  return self->getCambiumKillRating();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownDamage_0(SIGMortality* self) {
  return self->getCrownDamage();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownRatio_0(SIGMortality* self) {
  return self->getCrownRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getDBH_1(SIGMortality* self, LengthUnits_LengthUnitsEnum diameterUnits) {
  return self->getDBH(diameterUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightValue_1(SIGMortality* self, LengthUnits_LengthUnitsEnum flameLengthOrScorchHeightUnits) {
  return self->getFlameLengthOrScorchHeightValue(flameLengthOrScorchHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getKilledTrees_0(SIGMortality* self) {
  return self->getKilledTrees();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getProbabilityOfMortality_1(SIGMortality* self, ProbabilityUnits_ProbabilityUnitsEnum probabilityUnits) {
  return self->getProbabilityOfMortality(probabilityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTotalPrefireTrees_0(SIGMortality* self) {
  return self->getTotalPrefireTrees();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeDensityPerUnitArea_1(SIGMortality* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getTreeDensityPerUnitArea(areaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeHeight_1(SIGMortality* self, LengthUnits_LengthUnitsEnum treeHeightUnits) {
  return self->getTreeHeight(treeHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_postfireCanopyCover_0(SIGMortality* self) {
  return self->postfireCanopyCover();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_prefireCanopyCover_0(SIGMortality* self) {
  return self->prefireCanopyCover();
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBarkEquationNumberAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getBarkEquationNumberAtSpeciesTableIndex(index);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBarkEquationNumberFromSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  return self->getBarkEquationNumberFromSpeciesCode(speciesCode);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownCoefficientCodeAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getCrownCoefficientCodeAtSpeciesTableIndex(index);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownCoefficientCodeFromSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  return self->getCrownCoefficientCodeFromSpeciesCode(speciesCode);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownScorchOrBoleCharEquationNumber_0(SIGMortality* self) {
  return self->getCrownScorchOrBoleCharEquationNumber();
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getMortalityEquationNumberAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getMortalityEquationNumberAtSpeciesTableIndex(index);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getMortalityEquationNumberFromSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  return self->getMortalityEquationNumberFromSpeciesCode(speciesCode);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getNumberOfRecordsInSpeciesTable_0(SIGMortality* self) {
  return self->getNumberOfRecordsInSpeciesTable();
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCode_1(SIGMortality* self, char* speciesNameCode) {
  return self->getSpeciesTableIndexFromSpeciesCode(speciesNameCode);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesTableIndexFromSpeciesCodeAndEquationType_2(SIGMortality* self, char* speciesNameCode, EquationType equationType) {
  return self->getSpeciesTableIndexFromSpeciesCodeAndEquationType(speciesNameCode, equationType);
}

char EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesCode_0(SIGMortality* self) {
  return self->getSpeciesCode();
}

const SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegion_1(SIGMortality* self, RegionCode region) {
  return self->getSpeciesRecordVectorForRegion(region);
}

const SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegionAndEquationType_2(SIGMortality* self, RegionCode region, EquationType equationType) {
  return self->getSpeciesRecordVectorForRegionAndEquationType(region, equationType);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCommonNameAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getCommonNameAtSpeciesTableIndex(index);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCommonNameFromSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  return self->getCommonNameFromSpeciesCode(speciesCode);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getScientificNameAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getScientificNameAtSpeciesTableIndex(index);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getScientificNameFromSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  return self->getScientificNameFromSpeciesCode(speciesCode);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getSpeciesCodeAtSpeciesTableIndex(index);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getRequiredFieldVector_0(SIGMortality* self) {
  return self->getRequiredFieldVector();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_calculateMortality_1(SIGMortality* self, ProbabilityUnits_ProbabilityUnitsEnum probablityUnits) {
  return self->calculateMortality(probablityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setBeetleDamage_1(SIGMortality* self, BeetleDamage beetleDamage) {
  self->setBeetleDamage(beetleDamage);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setBoleCharHeight_2(SIGMortality* self, double boleCharHeight, LengthUnits_LengthUnitsEnum boleCharHeightUnits) {
  self->setBoleCharHeight(boleCharHeight, boleCharHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setCambiumKillRating_1(SIGMortality* self, double cambiumKillRating) {
  self->setCambiumKillRating(cambiumKillRating);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setCrownDamage_1(SIGMortality* self, double crownDamage) {
  self->setCrownDamage(crownDamage);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setCrownRatio_1(SIGMortality* self, double crownRatio) {
  self->setCrownRatio(crownRatio);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setDBH_2(SIGMortality* self, double dbh, LengthUnits_LengthUnitsEnum diameterUnits) {
  self->setDBH(dbh, diameterUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setEquationType_1(SIGMortality* self, EquationType equationType) {
  self->setEquationType(equationType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setFireSeverity_1(SIGMortality* self, FireSeverity fireSeverity) {
  self->setFireSeverity(fireSeverity);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightSwitch_1(SIGMortality* self, FlameLengthOrScorchHeightSwitch flameLengthOrScorchHeightSwitch) {
  self->setFlameLengthOrScorchHeightSwitch(flameLengthOrScorchHeightSwitch);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightValue_2(SIGMortality* self, double flameLengthOrScorchHeightValue, LengthUnits_LengthUnitsEnum flameLengthOrScorchHeightUnits) {
  self->setFlameLengthOrScorchHeightValue(flameLengthOrScorchHeightValue, flameLengthOrScorchHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setRegion_1(SIGMortality* self, RegionCode region) {
  self->setRegion(region);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSpeciesCode_1(SIGMortality* self, char speciesCode) {
  self->setSpeciesCode(speciesCode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setTreeDensityPerUnitArea_2(SIGMortality* self, double numberOfTrees, AreaUnits_AreaUnitsEnum areaUnits) {
  self->setTreeDensityPerUnitArea(numberOfTrees, areaUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setTreeHeight_2(SIGMortality* self, double treeHeight, LengthUnits_LengthUnitsEnum treeHeightUnits) {
  self->setTreeHeight(treeHeight, treeHeightUnits);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2(SIGMortality* self, char speciesCode, EquationType equationType) {
  return self->updateInputsForSpeciesCodeAndEquationType(speciesCode, equationType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality___destroy___0(SIGMortality* self) {
  delete self;
}

// WindSpeedUtility

WindSpeedUtility* EMSCRIPTEN_KEEPALIVE emscripten_bind_WindSpeedUtility_WindSpeedUtility_0() {
  return new WindSpeedUtility();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WindSpeedUtility_windSpeedAtMidflame_2(WindSpeedUtility* self, double windSpeedAtTwentyFeet, double windAdjustmentFactor) {
  return self->windSpeedAtMidflame(windSpeedAtTwentyFeet, windAdjustmentFactor);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WindSpeedUtility_windSpeedAtTwentyFeetFromTenMeter_1(WindSpeedUtility* self, double windSpeedAtTenMeters) {
  return self->windSpeedAtTwentyFeetFromTenMeter(windSpeedAtTenMeters);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_WindSpeedUtility___destroy___0(WindSpeedUtility* self) {
  delete self;
}

// AreaUnits_AreaUnitsEnum
AreaUnits_AreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AreaUnits_AreaUnitsEnum_SquareFeet() {
  return AreaUnits::SquareFeet;
}
AreaUnits_AreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AreaUnits_AreaUnitsEnum_Acres() {
  return AreaUnits::Acres;
}
AreaUnits_AreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AreaUnits_AreaUnitsEnum_Hectares() {
  return AreaUnits::Hectares;
}
AreaUnits_AreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMeters() {
  return AreaUnits::SquareMeters;
}
AreaUnits_AreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AreaUnits_AreaUnitsEnum_SquareMiles() {
  return AreaUnits::SquareMiles;
}
AreaUnits_AreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AreaUnits_AreaUnitsEnum_SquareKilometers() {
  return AreaUnits::SquareKilometers;
}

// LengthUnits_LengthUnitsEnum
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Feet() {
  return LengthUnits::Feet;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Inches() {
  return LengthUnits::Inches;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Centimeters() {
  return LengthUnits::Centimeters;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Meters() {
  return LengthUnits::Meters;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Chains() {
  return LengthUnits::Chains;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Miles() {
  return LengthUnits::Miles;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Kilometers() {
  return LengthUnits::Kilometers;
}

// LoadingUnits_LoadingUnitsEnum
LoadingUnits_LoadingUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LoadingUnits_LoadingUnitsEnum_PoundsPerSquareFoot() {
  return LoadingUnits::PoundsPerSquareFoot;
}
LoadingUnits_LoadingUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonsPerAcre() {
  return LoadingUnits::TonsPerAcre;
}
LoadingUnits_LoadingUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LoadingUnits_LoadingUnitsEnum_TonnesPerHectare() {
  return LoadingUnits::TonnesPerHectare;
}
LoadingUnits_LoadingUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LoadingUnits_LoadingUnitsEnum_KilogramsPerSquareMeter() {
  return LoadingUnits::KilogramsPerSquareMeter;
}

// SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum
SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareFeetOverCubicFeet() {
  return SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet;
}
SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareMetersOverCubicMeters() {
  return SurfaceAreaToVolumeUnits::SquareMetersOverCubicMeters;
}
SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareInchesOverCubicInches() {
  return SurfaceAreaToVolumeUnits::SquareInchesOverCubicInches;
}
SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimers() {
  return SurfaceAreaToVolumeUnits::SquareCentimetersOverCubicCentimers;
}

// CoverUnits_CoverUnitsEnum
CoverUnits_CoverUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_CoverUnits_CoverUnitsEnum_Fraction() {
  return CoverUnits::Fraction;
}
CoverUnits_CoverUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_CoverUnits_CoverUnitsEnum_Percent() {
  return CoverUnits::Percent;
}

// SpeedUnits_SpeedUnitsEnum
SpeedUnits_SpeedUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpeedUnits_SpeedUnitsEnum_FeetPerMinute() {
  return SpeedUnits::FeetPerMinute;
}
SpeedUnits_SpeedUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpeedUnits_SpeedUnitsEnum_ChainsPerHour() {
  return SpeedUnits::ChainsPerHour;
}
SpeedUnits_SpeedUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerSecond() {
  return SpeedUnits::MetersPerSecond;
}
SpeedUnits_SpeedUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpeedUnits_SpeedUnitsEnum_MetersPerMinute() {
  return SpeedUnits::MetersPerMinute;
}
SpeedUnits_SpeedUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpeedUnits_SpeedUnitsEnum_MilesPerHour() {
  return SpeedUnits::MilesPerHour;
}
SpeedUnits_SpeedUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpeedUnits_SpeedUnitsEnum_KilometersPerHour() {
  return SpeedUnits::KilometersPerHour;
}

// ProbabilityUnits_ProbabilityUnitsEnum
ProbabilityUnits_ProbabilityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Fraction() {
  return ProbabilityUnits::Fraction;
}
ProbabilityUnits_ProbabilityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ProbabilityUnits_ProbabilityUnitsEnum_Percent() {
  return ProbabilityUnits::Percent;
}

// MoistureUnits_MoistureUnitsEnum
MoistureUnits_MoistureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureUnits_MoistureUnitsEnum_Fraction() {
  return MoistureUnits::Fraction;
}
MoistureUnits_MoistureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureUnits_MoistureUnitsEnum_Percent() {
  return MoistureUnits::Percent;
}

// SlopeUnits_SlopeUnitsEnum
SlopeUnits_SlopeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SlopeUnits_SlopeUnitsEnum_Degrees() {
  return SlopeUnits::Degrees;
}
SlopeUnits_SlopeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SlopeUnits_SlopeUnitsEnum_Percent() {
  return SlopeUnits::Percent;
}

// DensityUnits_DensityUnitsEnum
DensityUnits_DensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_DensityUnits_DensityUnitsEnum_PoundsPerCubicFoot() {
  return DensityUnits::PoundsPerCubicFoot;
}
DensityUnits_DensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_DensityUnits_DensityUnitsEnum_KilogramsPerCubicMeter() {
  return DensityUnits::KilogramsPerCubicMeter;
}

// HeatOfCombustionUnits_HeatOfCombustionUnitsEnum
HeatOfCombustionUnits_HeatOfCombustionUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_BtusPerPound() {
  return HeatOfCombustionUnits::BtusPerPound;
}
HeatOfCombustionUnits_HeatOfCombustionUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatOfCombustionUnits_HeatOfCombustionUnitsEnum_KilojoulesPerKilogram() {
  return HeatOfCombustionUnits::KilojoulesPerKilogram;
}

// HeatSinkUnits_HeatSinkUnitsEnum
HeatSinkUnits_HeatSinkUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_BtusPerCubicFoot() {
  return HeatSinkUnits::BtusPerCubicFoot;
}
HeatSinkUnits_HeatSinkUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatSinkUnits_HeatSinkUnitsEnum_KilojoulesPerCubicMeter() {
  return HeatSinkUnits::KilojoulesPerCubicMeter;
}

// HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum
HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_BtusPerSquareFoot() {
  return HeatPerUnitAreaUnits::BtusPerSquareFoot;
}
HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeterPerSecond() {
  return HeatPerUnitAreaUnits::KilojoulesPerSquareMeterPerSecond;
}
HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattsPerSquareMeter() {
  return HeatPerUnitAreaUnits::KilowattsPerSquareMeter;
}

// HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum
HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerMinute() {
  return HeatSourceAndReactionIntensityUnits::BtusPerSquareFootPerMinute;
}
HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_BtusPerSquareFootPerSecond() {
  return HeatSourceAndReactionIntensityUnits::BtusPerSquareFootPerSecond;
}
HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerSecond() {
  return HeatSourceAndReactionIntensityUnits::KilojoulesPerSquareMeterPerSecond;
}
HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilojoulesPerSquareMeterPerMinute() {
  return HeatSourceAndReactionIntensityUnits::KilojoulesPerSquareMeterPerMinute;
}
HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum_KilowattsPerSquareMeter() {
  return HeatSourceAndReactionIntensityUnits::KilowattsPerSquareMeter;
}

// FirelineIntensityUnits_FirelineIntensityUnitsEnum
FirelineIntensityUnits_FirelineIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerSecond() {
  return FirelineIntensityUnits::BtusPerFootPerSecond;
}
FirelineIntensityUnits_FirelineIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_BtusPerFootPerMinute() {
  return FirelineIntensityUnits::BtusPerFootPerMinute;
}
FirelineIntensityUnits_FirelineIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerSecond() {
  return FirelineIntensityUnits::KilojoulesPerMeterPerSecond;
}
FirelineIntensityUnits_FirelineIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilojoulesPerMeterPerMinute() {
  return FirelineIntensityUnits::KilojoulesPerMeterPerMinute;
}
FirelineIntensityUnits_FirelineIntensityUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FirelineIntensityUnits_FirelineIntensityUnitsEnum_KilowattsPerMeter() {
  return FirelineIntensityUnits::KilowattsPerMeter;
}

// TemperatureUnits_TemperatureUnitsEnum
TemperatureUnits_TemperatureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Fahrenheit() {
  return TemperatureUnits::Fahrenheit;
}
TemperatureUnits_TemperatureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Celsius() {
  return TemperatureUnits::Celsius;
}
TemperatureUnits_TemperatureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TemperatureUnits_TemperatureUnitsEnum_Kelvin() {
  return TemperatureUnits::Kelvin;
}

// TimeUnits_TimeUnitsEnum
TimeUnits_TimeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TimeUnits_TimeUnitsEnum_Minutes() {
  return TimeUnits::Minutes;
}
TimeUnits_TimeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TimeUnits_TimeUnitsEnum_Seconds() {
  return TimeUnits::Seconds;
}
TimeUnits_TimeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TimeUnits_TimeUnitsEnum_Hours() {
  return TimeUnits::Hours;
}

// ContainTactic
ContainTactic EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainTactic_HeadAttack() {
  return HeadAttack;
}
ContainTactic EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainTactic_RearAttack() {
  return RearAttack;
}

// ContainStatus
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_Unreported() {
  return Unreported;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_Reported() {
  return Reported;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_Attacked() {
  return Attacked;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_Contained() {
  return Contained;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_Overrun() {
  return Overrun;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_Exhausted() {
  return Exhausted;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_Overflow() {
  return Overflow;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_SizeLimitExceeded() {
  return SizeLimitExceeded;
}
ContainStatus EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_TimeLimitExceeded() {
  return TimeLimitExceeded;
}

// ContainFlank
ContainFlank EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_LeftFlank() {
  return LeftFlank;
}
ContainFlank EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_RightFlank() {
  return RightFlank;
}
ContainFlank EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_BothFlanks() {
  return BothFlanks;
}
ContainFlank EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_NeitherFlank() {
  return NeitherFlank;
}

// IgnitionFuelBedType
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_PonderosaPineLitter() {
  return PonderosaPineLitter;
}
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_PunkyWoodRottenChunky() {
  return PunkyWoodRottenChunky;
}
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_PunkyWoodPowderDeep() {
  return PunkyWoodPowderDeep;
}
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_PunkWoodPowderShallow() {
  return PunkWoodPowderShallow;
}
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_LodgepolePineDuff() {
  return LodgepolePineDuff;
}
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_DouglasFirDuff() {
  return DouglasFirDuff;
}
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_HighAltitudeMixed() {
  return HighAltitudeMixed;
}
IgnitionFuelBedType EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_PeatMoss() {
  return PeatMoss;
}

// LightningCharge
LightningCharge EMSCRIPTEN_KEEPALIVE emscripten_enum_LightningCharge_Negative() {
  return Negative;
}
LightningCharge EMSCRIPTEN_KEEPALIVE emscripten_enum_LightningCharge_Positive() {
  return Positive;
}
LightningCharge EMSCRIPTEN_KEEPALIVE emscripten_enum_LightningCharge_Unknown() {
  return Unknown;
}

// SpotTreeSpecies
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_ENGELMANN_SPRUCE() {
  return ENGELMANN_SPRUCE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_DOUGLAS_FIR() {
  return DOUGLAS_FIR;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SUBALPINE_FIR() {
  return SUBALPINE_FIR;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_WESTERN_HEMLOCK() {
  return WESTERN_HEMLOCK;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_PONDEROSA_PINE() {
  return PONDEROSA_PINE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_LODGEPOLE_PINE() {
  return LODGEPOLE_PINE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_WESTERN_WHITE_PINE() {
  return WESTERN_WHITE_PINE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_GRAND_FIR() {
  return GRAND_FIR;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_BALSAM_FIR() {
  return BALSAM_FIR;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SLASH_PINE() {
  return SLASH_PINE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_LONGLEAF_PINE() {
  return LONGLEAF_PINE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_POND_PINE() {
  return POND_PINE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SHORTLEAF_PINE() {
  return SHORTLEAF_PINE;
}
SpotTreeSpecies EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_LOBLOLLY_PINE() {
  return LOBLOLLY_PINE;
}

// SpotFireLocation
SpotFireLocation EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_MIDSLOPE_WINDWARD() {
  return MIDSLOPE_WINDWARD;
}
SpotFireLocation EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_VALLEY_BOTTOM() {
  return VALLEY_BOTTOM;
}
SpotFireLocation EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_MIDSLOPE_LEEWARD() {
  return MIDSLOPE_LEEWARD;
}
SpotFireLocation EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_RIDGE_TOP() {
  return RIDGE_TOP;
}

// SpotArrayConstants
SpotArrayConstants EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotArrayConstants_NUM_COLS() {
  return NUM_COLS;
}
SpotArrayConstants EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotArrayConstants_NUM_FIREBRAND_ROWS() {
  return NUM_FIREBRAND_ROWS;
}
SpotArrayConstants EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotArrayConstants_NUM_SPECIES() {
  return NUM_SPECIES;
}

// AspenFireSeverity_AspenFireSeverityEnum
AspenFireSeverity_AspenFireSeverityEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low() {
  return AspenFireSeverity::Low;
}
AspenFireSeverity_AspenFireSeverityEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Medium() {
  return AspenFireSeverity::Medium;
}

// TwoFuelModelsMethod_TwoFuelModelsMethodEnum
TwoFuelModelsMethod_TwoFuelModelsMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_NoMethod() {
  return TwoFuelModelsMethod::NoMethod;
}
TwoFuelModelsMethod_TwoFuelModelsMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Arithmetic() {
  return TwoFuelModelsMethod::Arithmetic;
}
TwoFuelModelsMethod_TwoFuelModelsMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_Harmonic() {
  return TwoFuelModelsMethod::Harmonic;
}
TwoFuelModelsMethod_TwoFuelModelsMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_TwoFuelModelsMethod_TwoFuelModelsMethodEnum_TwoDimensional() {
  return TwoFuelModelsMethod::TwoDimensional;
}

// WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum
WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Unsheltered() {
  return WindAdjustmentFactorShelterMethod::Unsheltered;
}
WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindAdjustmentFactorShelterMethod_WindAdjustmentFactorShelterMethodEnum_Sheltered() {
  return WindAdjustmentFactorShelterMethod::Sheltered;
}

// WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum
WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UserInput() {
  return WindAdjustmentFactorCalculationMethod::UserInput;
}
WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_UseCrownRatio() {
  return WindAdjustmentFactorCalculationMethod::UseCrownRatio;
}
WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum_DontUseCrownRatio() {
  return WindAdjustmentFactorCalculationMethod::DontUseCrownRatio;
}

// WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum
WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToUpslope() {
  return WindAndSpreadOrientationMode::RelativeToUpslope;
}
WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum_RelativeToNorth() {
  return WindAndSpreadOrientationMode::RelativeToNorth;
}

// WindHeightInputMode_WindHeightInputModeEnum
WindHeightInputMode_WindHeightInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_DirectMidflame() {
  return WindHeightInputMode::DirectMidflame;
}
WindHeightInputMode_WindHeightInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TwentyFoot() {
  return WindHeightInputMode::TwentyFoot;
}
WindHeightInputMode_WindHeightInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_WindHeightInputMode_WindHeightInputModeEnum_TenMeter() {
  return WindHeightInputMode::TenMeter;
}

// FireType_FireTypeEnum
FireType_FireTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FireType_FireTypeEnum_Surface() {
  return FireType::Surface;
}
FireType_FireTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FireType_FireTypeEnum_Torching() {
  return FireType::Torching;
}
FireType_FireTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FireType_FireTypeEnum_ConditionalCrownFire() {
  return FireType::ConditionalCrownFire;
}
FireType_FireTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FireType_FireTypeEnum_Crowning() {
  return FireType::Crowning;
}

// BeetleDamage
BeetleDamage EMSCRIPTEN_KEEPALIVE emscripten_enum_BeetleDamage_not_set() {
  return not_set;
}
BeetleDamage EMSCRIPTEN_KEEPALIVE emscripten_enum_BeetleDamage_no() {
  return no;
}
BeetleDamage EMSCRIPTEN_KEEPALIVE emscripten_enum_BeetleDamage_yes() {
  return yes;
}

// FireSeverity
FireSeverity EMSCRIPTEN_KEEPALIVE emscripten_enum_FireSeverity_not_set() {
  return not_set;
}
FireSeverity EMSCRIPTEN_KEEPALIVE emscripten_enum_FireSeverity_empty() {
  return empty;
}
FireSeverity EMSCRIPTEN_KEEPALIVE emscripten_enum_FireSeverity_low() {
  return low;
}

// FlameLengthOrScorchHeightSwitch
FlameLengthOrScorchHeightSwitch EMSCRIPTEN_KEEPALIVE emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length() {
  return flame_length;
}
FlameLengthOrScorchHeightSwitch EMSCRIPTEN_KEEPALIVE emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height() {
  return scorch_height;
}

// RegionCode
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_interior_west() {
  return interior_west;
}
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_pacific_west() {
  return pacific_west;
}
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_north_east() {
  return north_east;
}
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_south_east() {
  return south_east;
}

// CrownDamageType
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_not_set() {
  return not_set;
}
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_crown_length() {
  return crown_length;
}
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_crown_volume() {
  return crown_volume;
}
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_crown_kill() {
  return crown_kill;
}

// RequiredFieldNames
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_region() {
  return region;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_switch() {
  return flame_length_or_scorch_height_switch;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_value() {
  return flame_length_or_scorch_height_value;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_equation_type() {
  return equation_type;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_dbh() {
  return dbh;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_tree_height() {
  return tree_height;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_crown_ratio() {
  return crown_ratio;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_crown_damage() {
  return crown_damage;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_cambium_kill_rating() {
  return cambium_kill_rating;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_beetle_damage() {
  return beetle_damage;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_bole_char_height() {
  return bole_char_height;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_bark_thickness() {
  return bark_thickness;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_fire_severity() {
  return fire_severity;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_num_inputs() {
  return num_inputs;
}

// CrownDamageEquationCode
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_not_set() {
  return not_set;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_white_fir() {
  return white_fir;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_subalpine_fir() {
  return subalpine_fir;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_incense_cedar() {
  return incense_cedar;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_western_larch() {
  return western_larch;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_whitebark_pine() {
  return whitebark_pine;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_engelmann_spruce() {
  return engelmann_spruce;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_sugar_pine() {
  return sugar_pine;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_red_fir() {
  return red_fir;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_ponderosa_pine() {
  return ponderosa_pine;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_ponderosa_kill() {
  return ponderosa_kill;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_douglas_fir() {
  return douglas_fir;
}

// EquationType
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_not_set() {
  return not_set;
}
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_crown_scorch() {
  return crown_scorch;
}
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_bole_char() {
  return bole_char;
}
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_crown_damage() {
  return crown_damage;
}

}

