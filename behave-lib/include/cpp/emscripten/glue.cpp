
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

// BoolVector

BoolVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_BoolVector_BoolVector_0() {
  return new BoolVector();
}

BoolVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_BoolVector_BoolVector_1(int size) {
  return new BoolVector(size);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_BoolVector_resize_1(BoolVector* self, int size) {
  self->resize(size);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_BoolVector_get_1(BoolVector* self, int i) {
  return self->get(i);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_BoolVector_set_2(BoolVector* self, int i, bool val) {
  self->set(i, val);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_BoolVector_size_0(BoolVector* self) {
  return self->size();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_BoolVector___destroy___0(BoolVector* self) {
  delete self;
}

// CharVector

CharVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_CharVector_CharVector_0() {
  return new CharVector();
}

CharVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_CharVector_CharVector_1(int size) {
  return new CharVector(size);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_CharVector_resize_1(CharVector* self, int size) {
  self->resize(size);
}

char EMSCRIPTEN_KEEPALIVE emscripten_bind_CharVector_get_1(CharVector* self, int i) {
  return self->get(i);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_CharVector_set_2(CharVector* self, int i, char val) {
  self->set(i, val);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_CharVector_size_0(CharVector* self) {
  return self->size();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_CharVector___destroy___0(CharVector* self) {
  delete self;
}

// IntVector

IntVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_IntVector_IntVector_0() {
  return new IntVector();
}

IntVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_IntVector_IntVector_1(int size) {
  return new IntVector(size);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_IntVector_resize_1(IntVector* self, int size) {
  self->resize(size);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_IntVector_get_1(IntVector* self, int i) {
  return self->get(i);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_IntVector_set_2(IntVector* self, int i, int val) {
  self->set(i, val);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_IntVector_size_0(IntVector* self) {
  return self->size();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_IntVector___destroy___0(IntVector* self) {
  delete self;
}

// DoubleVector

DoubleVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_DoubleVector_DoubleVector_0() {
  return new DoubleVector();
}

DoubleVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_DoubleVector_DoubleVector_1(int size) {
  return new DoubleVector(size);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_DoubleVector_resize_1(DoubleVector* self, int size) {
  self->resize(size);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_DoubleVector_get_1(DoubleVector* self, int i) {
  return self->get(i);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_DoubleVector_set_2(DoubleVector* self, int i, double val) {
  self->set(i, val);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_DoubleVector_size_0(DoubleVector* self) {
  return self->size();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_DoubleVector___destroy___0(DoubleVector* self) {
  delete self;
}

// SpeciesMasterTableRecordVector

SpeciesMasterTableRecordVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_0() {
  return new SpeciesMasterTableRecordVector();
}

SpeciesMasterTableRecordVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecordVector_SpeciesMasterTableRecordVector_1(int size) {
  return new SpeciesMasterTableRecordVector(size);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecordVector_resize_1(SpeciesMasterTableRecordVector* self, int size) {
  self->resize(size);
}

SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecordVector_get_1(SpeciesMasterTableRecordVector* self, int i) {
  return self->get(i);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecordVector_set_2(SpeciesMasterTableRecordVector* self, int i, SpeciesMasterTableRecord* val) {
  self->set(i, val);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecordVector_size_0(SpeciesMasterTableRecordVector* self) {
  return self->size();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecordVector___destroy___0(SpeciesMasterTableRecordVector* self) {
  delete self;
}

// AreaUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_AreaUnits_toBaseUnits_2(AreaUnits* self, double value, AreaUnits_AreaUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_AreaUnits_fromBaseUnits_2(AreaUnits* self, double value, AreaUnits_AreaUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_AreaUnits___destroy___0(AreaUnits* self) {
  delete self;
}

// BasalAreaUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_BasalAreaUnits_toBaseUnits_2(BasalAreaUnits* self, double value, BasalAreaUnits_BasalAreaUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_BasalAreaUnits_fromBaseUnits_2(BasalAreaUnits* self, double value, BasalAreaUnits_BasalAreaUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_BasalAreaUnits___destroy___0(BasalAreaUnits* self) {
  delete self;
}

// FractionUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FractionUnits_toBaseUnits_2(FractionUnits* self, double value, FractionUnits_FractionUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FractionUnits_fromBaseUnits_2(FractionUnits* self, double value, FractionUnits_FractionUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_FractionUnits___destroy___0(FractionUnits* self) {
  delete self;
}

// LengthUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_LengthUnits_toBaseUnits_2(LengthUnits* self, double value, LengthUnits_LengthUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_LengthUnits_fromBaseUnits_2(LengthUnits* self, double value, LengthUnits_LengthUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_LengthUnits___destroy___0(LengthUnits* self) {
  delete self;
}

// LoadingUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_LoadingUnits_toBaseUnits_2(LoadingUnits* self, double value, LoadingUnits_LoadingUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_LoadingUnits_fromBaseUnits_2(LoadingUnits* self, double value, LoadingUnits_LoadingUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_LoadingUnits___destroy___0(LoadingUnits* self) {
  delete self;
}

// SurfaceAreaToVolumeUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SurfaceAreaToVolumeUnits_toBaseUnits_2(SurfaceAreaToVolumeUnits* self, double value, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SurfaceAreaToVolumeUnits_fromBaseUnits_2(SurfaceAreaToVolumeUnits* self, double value, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SurfaceAreaToVolumeUnits___destroy___0(SurfaceAreaToVolumeUnits* self) {
  delete self;
}

// SpeedUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeedUnits_toBaseUnits_2(SpeedUnits* self, double value, SpeedUnits_SpeedUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeedUnits_fromBaseUnits_2(SpeedUnits* self, double value, SpeedUnits_SpeedUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeedUnits___destroy___0(SpeedUnits* self) {
  delete self;
}

// PressureUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PressureUnits_toBaseUnits_2(PressureUnits* self, double value, PressureUnits_PressureUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PressureUnits_fromBaseUnits_2(PressureUnits* self, double value, PressureUnits_PressureUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_PressureUnits___destroy___0(PressureUnits* self) {
  delete self;
}

// SlopeUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SlopeUnits_toBaseUnits_2(SlopeUnits* self, double value, SlopeUnits_SlopeUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SlopeUnits_fromBaseUnits_2(SlopeUnits* self, double value, SlopeUnits_SlopeUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SlopeUnits___destroy___0(SlopeUnits* self) {
  delete self;
}

// DensityUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_DensityUnits_toBaseUnits_2(DensityUnits* self, double value, DensityUnits_DensityUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_DensityUnits_fromBaseUnits_2(DensityUnits* self, double value, DensityUnits_DensityUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_DensityUnits___destroy___0(DensityUnits* self) {
  delete self;
}

// HeatOfCombustionUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatOfCombustionUnits_toBaseUnits_2(HeatOfCombustionUnits* self, double value, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatOfCombustionUnits_fromBaseUnits_2(HeatOfCombustionUnits* self, double value, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatOfCombustionUnits___destroy___0(HeatOfCombustionUnits* self) {
  delete self;
}

// HeatSinkUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatSinkUnits_toBaseUnits_2(HeatSinkUnits* self, double value, HeatSinkUnits_HeatSinkUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatSinkUnits_fromBaseUnits_2(HeatSinkUnits* self, double value, HeatSinkUnits_HeatSinkUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatSinkUnits___destroy___0(HeatSinkUnits* self) {
  delete self;
}

// HeatPerUnitAreaUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatPerUnitAreaUnits_toBaseUnits_2(HeatPerUnitAreaUnits* self, double value, HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatPerUnitAreaUnits_fromBaseUnits_2(HeatPerUnitAreaUnits* self, double value, HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatPerUnitAreaUnits___destroy___0(HeatPerUnitAreaUnits* self) {
  delete self;
}

// HeatSourceAndReactionIntensityUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatSourceAndReactionIntensityUnits_toBaseUnits_2(HeatSourceAndReactionIntensityUnits* self, double value, HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatSourceAndReactionIntensityUnits_fromBaseUnits_2(HeatSourceAndReactionIntensityUnits* self, double value, HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_HeatSourceAndReactionIntensityUnits___destroy___0(HeatSourceAndReactionIntensityUnits* self) {
  delete self;
}

// FirelineIntensityUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FirelineIntensityUnits_toBaseUnits_2(FirelineIntensityUnits* self, double value, FirelineIntensityUnits_FirelineIntensityUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FirelineIntensityUnits_fromBaseUnits_2(FirelineIntensityUnits* self, double value, FirelineIntensityUnits_FirelineIntensityUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_FirelineIntensityUnits___destroy___0(FirelineIntensityUnits* self) {
  delete self;
}

// TemperatureUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_TemperatureUnits_toBaseUnits_2(TemperatureUnits* self, double value, TemperatureUnits_TemperatureUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_TemperatureUnits_fromBaseUnits_2(TemperatureUnits* self, double value, TemperatureUnits_TemperatureUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_TemperatureUnits___destroy___0(TemperatureUnits* self) {
  delete self;
}

// TimeUnits

double EMSCRIPTEN_KEEPALIVE emscripten_bind_TimeUnits_toBaseUnits_2(TimeUnits* self, double value, TimeUnits_TimeUnitsEnum units) {
  return self->toBaseUnits(value, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_TimeUnits_fromBaseUnits_2(TimeUnits* self, double value, TimeUnits_TimeUnitsEnum units) {
  return self->fromBaseUnits(value, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_TimeUnits___destroy___0(TimeUnits* self) {
  delete self;
}

// FireSize

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getBackingSpreadRate_1(FireSize* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getBackingSpreadRate(spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getEccentricity_0(FireSize* self) {
  return self->getEccentricity();
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

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFireArea_4(FireSize* self, bool isCrown, AreaUnits_AreaUnitsEnum areaUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getFireArea(isCrown, areaUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFireLength_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getFireLength(lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFireLengthToWidthRatio_0(FireSize* self) {
  return self->getFireLengthToWidthRatio();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFirePerimeter_4(FireSize* self, bool isCrown, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getFirePerimeter(isCrown, lengthUnits, elapsedTime, timeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getFlankingSpreadRate_1(FireSize* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getFlankingSpreadRate(spreadRateUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getHeadingToBackingRatio_0(FireSize* self) {
  return self->getHeadingToBackingRatio();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_getMaxFireWidth_3(FireSize* self, LengthUnits_LengthUnitsEnum lengthUnits, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getMaxFireWidth(lengthUnits, elapsedTime, timeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize_calculateFireBasicDimensions_5(FireSize* self, bool isCrown, double effectiveWindSpeed, SpeedUnits_SpeedUnitsEnum windSpeedRateUnits, double forwardSpreadRate, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  self->calculateFireBasicDimensions(isCrown, effectiveWindSpeed, windSpeedRateUnits, forwardSpreadRate, spreadRateUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_FireSize___destroy___0(FireSize* self) {
  delete self;
}

// SIGContainAdapter

SIGContainAdapter* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_SIGContainAdapter_0() {
  return new SIGContainAdapter();
}

ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getContainmentStatus_0(SIGContainAdapter* self) {
  return self->getContainmentStatus();
}

DoubleVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFirePerimeterX_0(SIGContainAdapter* self) {
  static DoubleVector temp;
  return (temp = self->getFirePerimeterX(), &temp);
}

DoubleVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFirePerimeterY_0(SIGContainAdapter* self) {
  static DoubleVector temp;
  return (temp = self->getFirePerimeterY(), &temp);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getAttackDistance_1(SIGContainAdapter* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getAttackDistance(lengthUnits);
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

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFireBackAtAttack_0(SIGContainAdapter* self) {
  return self->getFireBackAtAttack();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFireBackAtReport_0(SIGContainAdapter* self) {
  return self->getFireBackAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFireHeadAtAttack_0(SIGContainAdapter* self) {
  return self->getFireHeadAtAttack();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFireHeadAtReport_0(SIGContainAdapter* self) {
  return self->getFireHeadAtReport();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFireSizeAtInitialAttack_1(SIGContainAdapter* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getFireSizeAtInitialAttack(areaUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getLengthToWidthRatio_0(SIGContainAdapter* self) {
  return self->getLengthToWidthRatio();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getPerimeterAtContainment_1(SIGContainAdapter* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getPerimeterAtContainment(lengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getPerimeterAtInitialAttack_1(SIGContainAdapter* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getPerimeterAtInitialAttack(lengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getReportSize_1(SIGContainAdapter* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getReportSize(areaUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getReportRate_1(SIGContainAdapter* self, SpeedUnits_SpeedUnitsEnum speedUnits) {
  return self->getReportRate(speedUnits);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getTactic_0(SIGContainAdapter* self) {
  return self->getTactic();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_getFirePerimeterPointCount_0(SIGContainAdapter* self) {
  return self->getFirePerimeterPointCount();
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

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter_setTactic_1(SIGContainAdapter* self, ContainTactic_ContainTacticEnum tactic) {
  self->setTactic(tactic);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGContainAdapter___destroy___0(SIGContainAdapter* self) {
  delete self;
}

// SIGIgnite

SIGIgnite* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_SIGIgnite_0() {
  return new SIGIgnite();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_initializeMembers_0(SIGIgnite* self) {
  self->initializeMembers();
}

IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getFuelBedType_0(SIGIgnite* self) {
  return self->getFuelBedType();
}

LightningCharge_LightningChargeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getLightningChargeType_0(SIGIgnite* self) {
  return self->getLightningChargeType();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_calculateFirebrandIgnitionProbability_0(SIGIgnite* self) {
  self->calculateFirebrandIgnitionProbability();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_calculateLightningIgnitionProbability_1(SIGIgnite* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->calculateLightningIgnitionProbability(desiredUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setAirTemperature_2(SIGIgnite* self, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnites) {
  self->setAirTemperature(airTemperature, temperatureUnites);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setDuffDepth_2(SIGIgnite* self, double duffDepth, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setDuffDepth(duffDepth, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setIgnitionFuelBedType_1(SIGIgnite* self, IgnitionFuelBedType_IgnitionFuelBedTypeEnum fuelBedType_) {
  self->setIgnitionFuelBedType(fuelBedType_);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setLightningChargeType_1(SIGIgnite* self, LightningCharge_LightningChargeEnum lightningChargeType) {
  self->setLightningChargeType(lightningChargeType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setMoistureHundredHour_2(SIGIgnite* self, double moistureHundredHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureHundredHour(moistureHundredHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setMoistureOneHour_2(SIGIgnite* self, double moistureOneHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureOneHour(moistureOneHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_setSunShade_2(SIGIgnite* self, double sunShade, FractionUnits_FractionUnitsEnum sunShadeUnits) {
  self->setSunShade(sunShade, sunShadeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_updateIgniteInputs_11(SIGIgnite* self, double moistureOneHour, double moistureHundredHour, FractionUnits_FractionUnitsEnum moistureUnits, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnits, double sunShade, FractionUnits_FractionUnitsEnum sunShadeUnits, IgnitionFuelBedType_IgnitionFuelBedTypeEnum fuelBedType, double duffDepth, LengthUnits_LengthUnitsEnum duffDepthUnits, LightningCharge_LightningChargeEnum lightningChargeType) {
  self->updateIgniteInputs(moistureOneHour, moistureHundredHour, moistureUnits, airTemperature, temperatureUnits, sunShade, sunShadeUnits, fuelBedType, duffDepth, duffDepthUnits, lightningChargeType);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getAirTemperature_1(SIGIgnite* self, TemperatureUnits_TemperatureUnitsEnum desiredUnits) {
  return self->getAirTemperature(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getDuffDepth_1(SIGIgnite* self, LengthUnits_LengthUnitsEnum desiredUnits) {
  return self->getDuffDepth(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getFirebrandIgnitionProbability_1(SIGIgnite* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->getFirebrandIgnitionProbability(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getFuelTemperature_1(SIGIgnite* self, TemperatureUnits_TemperatureUnitsEnum desiredUnits) {
  return self->getFuelTemperature(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getMoistureHundredHour_1(SIGIgnite* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->getMoistureHundredHour(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getMoistureOneHour_1(SIGIgnite* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->getMoistureOneHour(desiredUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_getSunShade_1(SIGIgnite* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->getSunShade(desiredUnits);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite_isFuelDepthNeeded_0(SIGIgnite* self) {
  return self->isFuelDepthNeeded();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGIgnite___destroy___0(SIGIgnite* self) {
  delete self;
}

// SIGMoistureScenarios

SIGMoistureScenarios* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_SIGMoistureScenarios_0() {
  return new SIGMoistureScenarios();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByIndex_1(SIGMoistureScenarios* self, int index) {
  return self->getIsMoistureScenarioDefinedByIndex(index);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getIsMoistureScenarioDefinedByName_1(SIGMoistureScenarios* self, const char* name) {
  return self->getIsMoistureScenarioDefinedByName(name);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByIndex_2(SIGMoistureScenarios* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioHundredHourByIndex(index, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioHundredHourByName_2(SIGMoistureScenarios* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioHundredHourByName(name, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByIndex_2(SIGMoistureScenarios* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveHerbaceousByIndex(index, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveHerbaceousByName_2(SIGMoistureScenarios* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveHerbaceousByName(name, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByIndex_2(SIGMoistureScenarios* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveWoodyByIndex(index, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioLiveWoodyByName_2(SIGMoistureScenarios* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveWoodyByName(name, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByIndex_2(SIGMoistureScenarios* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioOneHourByIndex(index, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioOneHourByName_2(SIGMoistureScenarios* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioOneHourByName(name, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByIndex_2(SIGMoistureScenarios* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioTenHourByIndex(index, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioTenHourByName_2(SIGMoistureScenarios* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioTenHourByName(name, moistureUnits);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioIndexByName_1(SIGMoistureScenarios* self, const char* name) {
  return self->getMoistureScenarioIndexByName(name);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getNumberOfMoistureScenarios_0(SIGMoistureScenarios* self) {
  return self->getNumberOfMoistureScenarios();
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByIndex_1(SIGMoistureScenarios* self, int index) {
  return self->getMoistureScenarioDescriptionByIndex(index);
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioDescriptionByName_1(SIGMoistureScenarios* self, const char* name) {
  return self->getMoistureScenarioDescriptionByName(name);
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios_getMoistureScenarioNameByIndex_1(SIGMoistureScenarios* self, int index) {
  return self->getMoistureScenarioNameByIndex(index);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMoistureScenarios___destroy___0(SIGMoistureScenarios* self) {
  delete self;
}

// SIGSpot

SIGSpot* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_SIGSpot_0() {
  return new SIGSpot();
}

SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getDownwindCanopyMode_0(SIGSpot* self) {
  return self->getDownwindCanopyMode();
}

SpotFireLocation_SpotFireLocationEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getLocation_0(SIGSpot* self) {
  return self->getLocation();
}

SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getTreeSpecies_0(SIGSpot* self) {
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

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getMaxMountainousTerrainSpottingDistanceFromActiveCrown_1(SIGSpot* self, LengthUnits_LengthUnitsEnum spottingDistanceUnits) {
  return self->getMaxMountainousTerrainSpottingDistanceFromActiveCrown(spottingDistanceUnits);
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

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_getTorchingTrees_0(SIGSpot* self) {
  return self->getTorchingTrees();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_calculateAll_0(SIGSpot* self) {
  self->calculateAll();
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

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_initializeMembers_0(SIGSpot* self) {
  self->initializeMembers();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setActiveCrownFlameLength_2(SIGSpot* self, double flameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->setActiveCrownFlameLength(flameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setBurningPileFlameHeight_2(SIGSpot* self, double buringPileflameHeight, LengthUnits_LengthUnitsEnum flameHeightUnits) {
  self->setBurningPileFlameHeight(buringPileflameHeight, flameHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setDBH_2(SIGSpot* self, double DBH, LengthUnits_LengthUnitsEnum DBHUnits) {
  self->setDBH(DBH, DBHUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setDownwindCanopyMode_1(SIGSpot* self, SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum downwindCanopyMode) {
  self->setDownwindCanopyMode(downwindCanopyMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setDownwindCoverHeight_2(SIGSpot* self, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits) {
  self->setDownwindCoverHeight(downwindCoverHeight, coverHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setFireType_1(SIGSpot* self, FireType_FireTypeEnum fireType) {
  self->setFireType(fireType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setFlameLength_2(SIGSpot* self, double flameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->setFlameLength(flameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setLocation_1(SIGSpot* self, SpotFireLocation_SpotFireLocationEnum location) {
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

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setTreeSpecies_1(SIGSpot* self, SpotTreeSpecies_SpotTreeSpeciesEnum treeSpecies) {
  self->setTreeSpecies(treeSpecies);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setWindSpeedAtTwentyFeet_2(SIGSpot* self, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setWindSpeedAtTwentyFeet(windSpeedAtTwentyFeet, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setWindSpeed_2(SIGSpot* self, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setWindSpeed(windSpeed, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setWindSpeedAndWindHeightInputMode_3(SIGSpot* self, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindSpeedAndWindHeightInputMode(windSpeed, windSpeedUnits, windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_setWindHeightInputMode_1(SIGSpot* self, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindHeightInputMode(windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_updateSpotInputsForBurningPile_12(SIGSpot* self, SpotFireLocation_SpotFireLocationEnum location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum downwindCanopyMode, double buringPileFlameHeight, LengthUnits_LengthUnitsEnum flameHeightUnits, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->updateSpotInputsForBurningPile(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, buringPileFlameHeight, flameHeightUnits, windSpeedAtTwentyFeet, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_updateSpotInputsForSurfaceFire_12(SIGSpot* self, SpotFireLocation_SpotFireLocationEnum location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum downwindCanopyMode, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits, double flameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->updateSpotInputsForSurfaceFire(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, windSpeedAtTwentyFeet, windSpeedUnits, flameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot_updateSpotInputsForTorchingTrees_16(SIGSpot* self, SpotFireLocation_SpotFireLocationEnum location, double ridgeToValleyDistance, LengthUnits_LengthUnitsEnum ridgeToValleyDistanceUnits, double ridgeToValleyElevation, LengthUnits_LengthUnitsEnum elevationUnits, double downwindCoverHeight, LengthUnits_LengthUnitsEnum coverHeightUnits, SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum downwindCanopyMode, int torchingTrees, double DBH, LengthUnits_LengthUnitsEnum DBHUnits, double treeHeight, LengthUnits_LengthUnitsEnum treeHeightUnits, SpotTreeSpecies_SpotTreeSpeciesEnum treeSpecies, double windSpeedAtTwentyFeet, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->updateSpotInputsForTorchingTrees(location, ridgeToValleyDistance, ridgeToValleyDistanceUnits, ridgeToValleyElevation, elevationUnits, downwindCoverHeight, coverHeightUnits, downwindCanopyMode, torchingTrees, DBH, DBHUnits, treeHeight, treeHeightUnits, treeSpecies, windSpeedAtTwentyFeet, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSpot___destroy___0(SIGSpot* self) {
  delete self;
}

// SIGFuelModels

SIGFuelModels* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_SIGFuelModels_0() {
  return new SIGFuelModels();
}

SIGFuelModels* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_SIGFuelModels_1(const SIGFuelModels* rhs) {
  return new SIGFuelModels(*rhs);
}

SIGFuelModels* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_equal_1(SIGFuelModels* self, const SIGFuelModels* rhs) {
  return &(*self = *rhs);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_clearCustomFuelModel_1(SIGFuelModels* self, int fuelModelNumber) {
  return self->clearCustomFuelModel(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getIsDynamic_1(SIGFuelModels* self, int fuelModelNumber) {
  return self->getIsDynamic(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_isAllFuelLoadZero_1(SIGFuelModels* self, int fuelModelNumber) {
  return self->isAllFuelLoadZero(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_isFuelModelDefined_1(SIGFuelModels* self, int fuelModelNumber) {
  return self->isFuelModelDefined(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_isFuelModelReserved_1(SIGFuelModels* self, int fuelModelNumber) {
  return self->isFuelModelReserved(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_setCustomFuelModel_21(SIGFuelModels* self, int fuelModelNumber, char* code, char* name, double fuelBedDepth, LengthUnits_LengthUnitsEnum lengthUnits, double moistureOfExtinctionDead, FractionUnits_FractionUnitsEnum moistureUnits, double heatOfCombustionDead, double heatOfCombustionLive, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits, double fuelLoadOneHour, double fuelLoadTenHour, double fuelLoadHundredHour, double fuelLoadLiveHerbaceous, double fuelLoadLiveWoody, LoadingUnits_LoadingUnitsEnum loadingUnits, double savrOneHour, double savrLiveHerbaceous, double savrLiveWoody, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits, bool isDynamic) {
  return self->setCustomFuelModel(fuelModelNumber, code, name, fuelBedDepth, lengthUnits, moistureOfExtinctionDead, moistureUnits, heatOfCombustionDead, heatOfCombustionLive, heatOfCombustionUnits, fuelLoadOneHour, fuelLoadTenHour, fuelLoadHundredHour, fuelLoadLiveHerbaceous, fuelLoadLiveWoody, loadingUnits, savrOneHour, savrLiveHerbaceous, savrLiveWoody, savrUnits, isDynamic);
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelCode_1(SIGFuelModels* self, int fuelModelNumber) {
  return self->getFuelCode(fuelModelNumber);
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelName_1(SIGFuelModels* self, int fuelModelNumber) {
  return self->getFuelName(fuelModelNumber);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelLoadHundredHour_2(SIGFuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadHundredHour(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelLoadLiveHerbaceous_2(SIGFuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveHerbaceous(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelLoadLiveWoody_2(SIGFuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveWoody(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelLoadOneHour_2(SIGFuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadOneHour(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelLoadTenHour_2(SIGFuelModels* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadTenHour(fuelModelNumber, loadingUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getFuelbedDepth_2(SIGFuelModels* self, int fuelModelNumber, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFuelbedDepth(fuelModelNumber, lengthUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getHeatOfCombustionDead_2(SIGFuelModels* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getHeatOfCombustionDead(fuelModelNumber, heatOfCombustionUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getMoistureOfExtinctionDead_2(SIGFuelModels* self, int fuelModelNumber, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureOfExtinctionDead(fuelModelNumber, moistureUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getSavrLiveHerbaceous_2(SIGFuelModels* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getSavrLiveHerbaceous(fuelModelNumber, savrUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getSavrLiveWoody_2(SIGFuelModels* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getSavrLiveWoody(fuelModelNumber, savrUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getSavrOneHour_2(SIGFuelModels* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getSavrOneHour(fuelModelNumber, savrUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels_getHeatOfCombustionLive_2(SIGFuelModels* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getHeatOfCombustionLive(fuelModelNumber, heatOfCombustionUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFuelModels___destroy___0(SIGFuelModels* self) {
  delete self;
}

// SIGSurface

SIGSurface* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_SIGSurface_1(SIGFuelModels* fuelModels) {
  return new SIGSurface(*fuelModels);
}

AspenFireSeverity_AspenFireSeverityEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenFireSeverity_0(SIGSurface* self) {
  return self->getAspenFireSeverity();
}

ChaparralFuelType_ChaparralFuelTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralFuelType_0(SIGSurface* self) {
  return self->getChaparralFuelType();
}

MoistureInputMode_MoistureInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureInputMode_0(SIGSurface* self) {
  return self->getMoistureInputMode();
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

WindUpslopeAlignmentMode EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindUpslopeAlignmentMode_0(SIGSurface* self) {
  return self->getWindUpslopeAlignmentMode();
}

SurfaceRunInDirectionOf EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSurfaceRunInDirectionOf_0(SIGSurface* self) {
  return self->getSurfaceRunInDirectionOf();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByIndex_1(SIGSurface* self, int index) {
  return self->getIsMoistureScenarioDefinedByIndex(index);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getIsMoistureScenarioDefinedByName_1(SIGSurface* self, const char* name) {
  return self->getIsMoistureScenarioDefinedByName(name);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getIsUsingChaparral_0(SIGSurface* self) {
  return self->getIsUsingChaparral();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getIsUsingPalmettoGallberry_0(SIGSurface* self) {
  return self->getIsUsingPalmettoGallberry();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getIsUsingWesternAspen_0(SIGSurface* self) {
  return self->getIsUsingWesternAspen();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isAllFuelLoadZero_1(SIGSurface* self, int fuelModelNumber) {
  return self->isAllFuelLoadZero(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isFuelDynamic_1(SIGSurface* self, int fuelModelNumber) {
  return self->isFuelDynamic(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isFuelModelDefined_1(SIGSurface* self, int fuelModelNumber) {
  return self->isFuelModelDefined(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isFuelModelReserved_1(SIGSurface* self, int fuelModelNumber) {
  return self->isFuelModelReserved(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isMoistureClassInputNeededForCurrentFuelModel_1(SIGSurface* self, MoistureClassInput_MoistureClassInputEnum moistureClass) {
  return self->isMoistureClassInputNeededForCurrentFuelModel(moistureClass);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_isUsingTwoFuelModels_0(SIGSurface* self) {
  return self->isUsingTwoFuelModels();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCurrentMoistureScenarioByIndex_1(SIGSurface* self, int moistureScenarioIndex) {
  return self->setCurrentMoistureScenarioByIndex(moistureScenarioIndex);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCurrentMoistureScenarioByName_1(SIGSurface* self, const char* moistureScenarioName) {
  return self->setCurrentMoistureScenarioByName(moistureScenarioName);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_calculateFlameLength_3(SIGSurface* self, double firelineIntensity, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->calculateFlameLength(firelineIntensity, firelineIntensityUnits, flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAgeOfRough_0(SIGSurface* self) {
  return self->getAgeOfRough();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspect_0(SIGSurface* self) {
  return self->getAspect();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenCuringLevel_1(SIGSurface* self, FractionUnits_FractionUnitsEnum curingLevelUnits) {
  return self->getAspenCuringLevel(curingLevelUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenDBH_1(SIGSurface* self, LengthUnits_LengthUnitsEnum dbhUnits) {
  return self->getAspenDBH(dbhUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenLoadDeadOneHour_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getAspenLoadDeadOneHour(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenLoadDeadTenHour_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getAspenLoadDeadTenHour(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenLoadLiveHerbaceous_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getAspenLoadLiveHerbaceous(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenLoadLiveWoody_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getAspenLoadLiveWoody(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenSavrDeadOneHour_1(SIGSurface* self, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getAspenSavrDeadOneHour(savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenSavrDeadTenHour_1(SIGSurface* self, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getAspenSavrDeadTenHour(savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenSavrLiveHerbaceous_1(SIGSurface* self, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getAspenSavrLiveHerbaceous(savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenSavrLiveWoody_1(SIGSurface* self, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getAspenSavrLiveWoody(savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getBackingFirelineIntensity_1(SIGSurface* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getBackingFirelineIntensity(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getBackingFlameLength_1(SIGSurface* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getBackingFlameLength(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getBackingSpreadDistance_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getBackingSpreadDistance(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getBackingSpreadRate_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getBackingSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getBulkDensity_1(SIGSurface* self, DensityUnits_DensityUnitsEnum densityUnits) {
  return self->getBulkDensity(densityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCanopyCover_1(SIGSurface* self, FractionUnits_FractionUnitsEnum coverUnits) {
  return self->getCanopyCover(coverUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCanopyHeight_1(SIGSurface* self, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  return self->getCanopyHeight(canopyHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralAge_1(SIGSurface* self, TimeUnits_TimeUnitsEnum ageUnits) {
  return self->getChaparralAge(ageUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralDaysSinceMayFirst_0(SIGSurface* self) {
  return self->getChaparralDaysSinceMayFirst();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralDeadFuelFraction_0(SIGSurface* self) {
  return self->getChaparralDeadFuelFraction();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralDeadMoistureOfExtinction_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getChaparralDeadMoistureOfExtinction(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralDensity_3(SIGSurface* self, FuelLifeState_FuelLifeStateEnum lifeState, int sizeClass, DensityUnits_DensityUnitsEnum densityUnits) {
  return self->getChaparralDensity(lifeState, sizeClass, densityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralFuelBedDepth_1(SIGSurface* self, LengthUnits_LengthUnitsEnum depthUnits) {
  return self->getChaparralFuelBedDepth(depthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralFuelDeadLoadFraction_0(SIGSurface* self) {
  return self->getChaparralFuelDeadLoadFraction();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralHeatOfCombustion_3(SIGSurface* self, FuelLifeState_FuelLifeStateEnum lifeState, int sizeClass, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getChaparralHeatOfCombustion(lifeState, sizeClass, heatOfCombustionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLiveMoistureOfExtinction_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getChaparralLiveMoistureOfExtinction(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadDeadHalfInchToLessThanOneInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadDeadHalfInchToLessThanOneInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadDeadLessThanQuarterInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadDeadLessThanQuarterInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadDeadOneInchToThreeInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadDeadOneInchToThreeInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadDeadQuarterInchToLessThanHalfInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadDeadQuarterInchToLessThanHalfInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadLiveHalfInchToLessThanOneInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadLiveHalfInchToLessThanOneInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadLiveLeaves_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadLiveLeaves(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadLiveOneInchToThreeInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadLiveOneInchToThreeInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadLiveQuarterInchToLessThanHalfInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadLiveQuarterInchToLessThanHalfInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralLoadLiveStemsLessThanQuaterInch_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralLoadLiveStemsLessThanQuaterInch(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralMoisture_3(SIGSurface* self, FuelLifeState_FuelLifeStateEnum lifeState, int sizeClass, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getChaparralMoisture(lifeState, sizeClass, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralTotalDeadFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralTotalDeadFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralTotalFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralTotalFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getChaparralTotalLiveFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getChaparralTotalLiveFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCharacteristicMoistureByLifeState_2(SIGSurface* self, FuelLifeState_FuelLifeStateEnum lifeState, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getCharacteristicMoistureByLifeState(lifeState, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCharacteristicMoistureDead_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getCharacteristicMoistureDead(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCharacteristicMoistureLive_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getCharacteristicMoistureLive(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCharacteristicSAVR_1(SIGSurface* self, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getCharacteristicSAVR(savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getCrownRatio_1(SIGSurface* self, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  return self->getCrownRatio(crownRatioUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getDirectionOfMaxSpread_0(SIGSurface* self) {
  return self->getDirectionOfMaxSpread();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getDirectionOfInterest_0(SIGSurface* self) {
  return self->getDirectionOfInterest();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getDirectionOfBacking_0(SIGSurface* self) {
  return self->getDirectionOfBacking();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getDirectionOfFlanking_0(SIGSurface* self) {
  return self->getDirectionOfFlanking();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getElapsedTime_1(SIGSurface* self, TimeUnits_TimeUnitsEnum timeUnits) {
  return self->getElapsedTime(timeUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getEllipticalA_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getEllipticalA(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getEllipticalB_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getEllipticalB(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getEllipticalC_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getEllipticalC(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFireLength_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFireLength(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMaxFireWidth_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getMaxFireWidth(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFireArea_1(SIGSurface* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getFireArea(areaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFireEccentricity_0(SIGSurface* self) {
  return self->getFireEccentricity();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFireLengthToWidthRatio_0(SIGSurface* self) {
  return self->getFireLengthToWidthRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFirePerimeter_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFirePerimeter(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFirelineIntensity_1(SIGSurface* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getFirelineIntensity(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFirelineIntensityInDirectionOfInterest_1(SIGSurface* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getFirelineIntensityInDirectionOfInterest(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFlameLength_1(SIGSurface* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getFlameLength(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFlameLengthInDirectionOfInterest_1(SIGSurface* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getFlameLengthInDirectionOfInterest(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFlankingFirelineIntensity_1(SIGSurface* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getFlankingFirelineIntensity(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFlankingFlameLength_1(SIGSurface* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getFlankingFlameLength(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFlankingSpreadRate_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getFlankingSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFlankingSpreadDistance_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFlankingSpreadDistance(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelHeatOfCombustionDead_2(SIGSurface* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getFuelHeatOfCombustionDead(fuelModelNumber, heatOfCombustionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelHeatOfCombustionLive_2(SIGSurface* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getFuelHeatOfCombustionLive(fuelModelNumber, heatOfCombustionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelLoadHundredHour_2(SIGSurface* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadHundredHour(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelLoadLiveHerbaceous_2(SIGSurface* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveHerbaceous(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelLoadLiveWoody_2(SIGSurface* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveWoody(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelLoadOneHour_2(SIGSurface* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadOneHour(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelLoadTenHour_2(SIGSurface* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadTenHour(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelMoistureOfExtinctionDead_2(SIGSurface* self, int fuelModelNumber, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getFuelMoistureOfExtinctionDead(fuelModelNumber, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelSavrLiveHerbaceous_2(SIGSurface* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getFuelSavrLiveHerbaceous(fuelModelNumber, savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelSavrLiveWoody_2(SIGSurface* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getFuelSavrLiveWoody(fuelModelNumber, savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelSavrOneHour_2(SIGSurface* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getFuelSavrOneHour(fuelModelNumber, savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelbedDepth_2(SIGSurface* self, int fuelModelNumber, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFuelbedDepth(fuelModelNumber, lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeadingSpreadRate_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getHeadingSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeadingToBackingRatio_0(SIGSurface* self) {
  return self->getHeadingToBackingRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeatPerUnitArea_1(SIGSurface* self, HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum heatPerUnitAreaUnits) {
  return self->getHeatPerUnitArea(heatPerUnitAreaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeatSink_1(SIGSurface* self, HeatSinkUnits_HeatSinkUnitsEnum heatSinkUnits) {
  return self->getHeatSink(heatSinkUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeatSource_1(SIGSurface* self, HeatSourceAndReactionIntensityUnits_HeatSourceAndReactionIntensityUnitsEnum heatSourceUnits) {
  return self->getHeatSource(heatSourceUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getHeightOfUnderstory_1(SIGSurface* self, LengthUnits_LengthUnitsEnum heightUnits) {
  return self->getHeightOfUnderstory(heightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getLiveFuelMoistureOfExtinction_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getLiveFuelMoistureOfExtinction(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMidflameWindspeed_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  return self->getMidflameWindspeed(windSpeedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureDeadAggregateValue_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureDeadAggregateValue(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureHundredHour_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureHundredHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureLiveAggregateValue_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureLiveAggregateValue(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureLiveHerbaceous_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureLiveHerbaceous(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureLiveWoody_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureLiveWoody(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureOneHour_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureOneHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByIndex_2(SIGSurface* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioHundredHourByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioHundredHourByName_2(SIGSurface* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioHundredHourByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByIndex_2(SIGSurface* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveHerbaceousByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioLiveHerbaceousByName_2(SIGSurface* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveHerbaceousByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByIndex_2(SIGSurface* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveWoodyByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioLiveWoodyByName_2(SIGSurface* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveWoodyByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioOneHourByIndex_2(SIGSurface* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioOneHourByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioOneHourByName_2(SIGSurface* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioOneHourByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioTenHourByIndex_2(SIGSurface* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioTenHourByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioTenHourByName_2(SIGSurface* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioTenHourByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureTenHour_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureTenHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getOverstoryBasalArea_1(SIGSurface* self, BasalAreaUnits_BasalAreaUnitsEnum basalAreaUnits) {
  return self->getOverstoryBasalArea(basalAreaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberryCoverage_1(SIGSurface* self, FractionUnits_FractionUnitsEnum coverUnits) {
  return self->getPalmettoGallberryCoverage(coverUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionDead_1(SIGSurface* self, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getPalmettoGallberryHeatOfCombustionDead(heatOfCombustionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberryHeatOfCombustionLive_1(SIGSurface* self, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getPalmettoGallberryHeatOfCombustionLive(heatOfCombustionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberryMoistureOfExtinctionDead_1(SIGSurface* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getPalmettoGallberryMoistureOfExtinctionDead(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyDeadFineFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getPalmettoGallberyDeadFineFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyDeadFoliageLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getPalmettoGallberyDeadFoliageLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyDeadMediumFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getPalmettoGallberyDeadMediumFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyFuelBedDepth_1(SIGSurface* self, LengthUnits_LengthUnitsEnum depthUnits) {
  return self->getPalmettoGallberyFuelBedDepth(depthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyLitterLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getPalmettoGallberyLitterLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyLiveFineFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getPalmettoGallberyLiveFineFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyLiveFoliageLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getPalmettoGallberyLiveFoliageLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getPalmettoGallberyLiveMediumFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getPalmettoGallberyLiveMediumFuelLoad(loadingUnits);
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

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSpreadDistance_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getSpreadDistance(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSpreadDistanceInDirectionOfInterest_1(SIGSurface* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getSpreadDistanceInDirectionOfInterest(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSpreadRate_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSpreadRateInDirectionOfInterest_1(SIGSurface* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getSpreadRateInDirectionOfInterest(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getSurfaceFireReactionIntensityForLifeState_1(SIGSurface* self, FuelLifeState_FuelLifeStateEnum lifeState) {
  return self->getSurfaceFireReactionIntensityForLifeState(lifeState);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getTotalLiveFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getTotalLiveFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getTotalDeadFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getTotalDeadFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getTotalDeadHerbaceousFuelLoad_1(SIGSurface* self, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getTotalDeadHerbaceousFuelLoad(loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindDirection_0(SIGSurface* self) {
  return self->getWindDirection();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getWindSpeed_2(SIGSurface* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  return self->getWindSpeed(windSpeedUnits, windHeightInputMode);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getAspenFuelModelNumber_0(SIGSurface* self) {
  return self->getAspenFuelModelNumber();
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelModelNumber_0(SIGSurface* self) {
  return self->getFuelModelNumber();
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioIndexByName_1(SIGSurface* self, const char* name) {
  return self->getMoistureScenarioIndexByName(name);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getNumberOfMoistureScenarios_0(SIGSurface* self) {
  return self->getNumberOfMoistureScenarios();
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelCode_1(SIGSurface* self, int fuelModelNumber) {
  return self->getFuelCode(fuelModelNumber);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getFuelName_1(SIGSurface* self, int fuelModelNumber) {
  return self->getFuelName(fuelModelNumber);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByIndex_1(SIGSurface* self, int index) {
  return self->getMoistureScenarioDescriptionByIndex(index);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioDescriptionByName_1(SIGSurface* self, const char* name) {
  return self->getMoistureScenarioDescriptionByName(name);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_getMoistureScenarioNameByIndex_1(SIGSurface* self, int index) {
  return self->getMoistureScenarioNameByIndex(index);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_doSurfaceRun_0(SIGSurface* self) {
  self->doSurfaceRun();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfInterest_2(SIGSurface* self, double directionOfInterest, SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum directionMode) {
  self->doSurfaceRunInDirectionOfInterest(directionOfInterest, directionMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_doSurfaceRunInDirectionOfMaxSpread_0(SIGSurface* self) {
  self->doSurfaceRunInDirectionOfMaxSpread();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_initializeMembers_0(SIGSurface* self) {
  self->initializeMembers();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setAgeOfRough_1(SIGSurface* self, double ageOfRough) {
  self->setAgeOfRough(ageOfRough);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setAspect_1(SIGSurface* self, double aspect) {
  self->setAspect(aspect);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setAspenCuringLevel_2(SIGSurface* self, double aspenCuringLevel, FractionUnits_FractionUnitsEnum curingLevelUnits) {
  self->setAspenCuringLevel(aspenCuringLevel, curingLevelUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setAspenDBH_2(SIGSurface* self, double dbh, LengthUnits_LengthUnitsEnum dbhUnits) {
  self->setAspenDBH(dbh, dbhUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setAspenFireSeverity_1(SIGSurface* self, AspenFireSeverity_AspenFireSeverityEnum aspenFireSeverity) {
  self->setAspenFireSeverity(aspenFireSeverity);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setAspenFuelModelNumber_1(SIGSurface* self, int aspenFuelModelNumber) {
  self->setAspenFuelModelNumber(aspenFuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCanopyCover_2(SIGSurface* self, double canopyCover, FractionUnits_FractionUnitsEnum coverUnits) {
  self->setCanopyCover(canopyCover, coverUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCanopyHeight_2(SIGSurface* self, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  self->setCanopyHeight(canopyHeight, canopyHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setChaparralFuelBedDepth_2(SIGSurface* self, double chaparralFuelBedDepth, LengthUnits_LengthUnitsEnum depthUnts) {
  self->setChaparralFuelBedDepth(chaparralFuelBedDepth, depthUnts);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setChaparralFuelDeadLoadFraction_1(SIGSurface* self, double chaparralFuelDeadLoadFraction) {
  self->setChaparralFuelDeadLoadFraction(chaparralFuelDeadLoadFraction);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setChaparralFuelLoadInputMode_1(SIGSurface* self, ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum fuelLoadInputMode) {
  self->setChaparralFuelLoadInputMode(fuelLoadInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setChaparralFuelType_1(SIGSurface* self, ChaparralFuelType_ChaparralFuelTypeEnum chaparralFuelType) {
  self->setChaparralFuelType(chaparralFuelType);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setChaparralTotalFuelLoad_2(SIGSurface* self, double chaparralTotalFuelLoad, LoadingUnits_LoadingUnitsEnum fuelLoadUnits) {
  self->setChaparralTotalFuelLoad(chaparralTotalFuelLoad, fuelLoadUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setCrownRatio_2(SIGSurface* self, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  self->setCrownRatio(crownRatio, crownRatioUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setDirectionOfInterest_1(SIGSurface* self, double directionOfInterest) {
  self->setDirectionOfInterest(directionOfInterest);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setElapsedTime_2(SIGSurface* self, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  self->setElapsedTime(elapsedTime, timeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setFirstFuelModelNumber_1(SIGSurface* self, int firstFuelModelNumber) {
  self->setFirstFuelModelNumber(firstFuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setFuelModels_1(SIGSurface* self, SIGFuelModels* fuelModels) {
  self->setFuelModels(*fuelModels);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setHeightOfUnderstory_2(SIGSurface* self, double heightOfUnderstory, LengthUnits_LengthUnitsEnum heightUnits) {
  self->setHeightOfUnderstory(heightOfUnderstory, heightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setIsUsingChaparral_1(SIGSurface* self, bool isUsingChaparral) {
  self->setIsUsingChaparral(isUsingChaparral);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setIsUsingPalmettoGallberry_1(SIGSurface* self, bool isUsingPalmettoGallberry) {
  self->setIsUsingPalmettoGallberry(isUsingPalmettoGallberry);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setIsUsingWesternAspen_1(SIGSurface* self, bool isUsingWesternAspen) {
  self->setIsUsingWesternAspen(isUsingWesternAspen);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureDeadAggregate_2(SIGSurface* self, double moistureDead, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureDeadAggregate(moistureDead, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureHundredHour_2(SIGSurface* self, double moistureHundredHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureHundredHour(moistureHundredHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureInputMode_1(SIGSurface* self, MoistureInputMode_MoistureInputModeEnum moistureInputMode) {
  self->setMoistureInputMode(moistureInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureLiveAggregate_2(SIGSurface* self, double moistureLive, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureLiveAggregate(moistureLive, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureLiveHerbaceous_2(SIGSurface* self, double moistureLiveHerbaceous, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureLiveHerbaceous(moistureLiveHerbaceous, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureLiveWoody_2(SIGSurface* self, double moistureLiveWoody, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureLiveWoody(moistureLiveWoody, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureOneHour_2(SIGSurface* self, double moistureOneHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureOneHour(moistureOneHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureScenarios_1(SIGSurface* self, SIGMoistureScenarios* moistureScenarios) {
  self->setMoistureScenarios(*moistureScenarios);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setMoistureTenHour_2(SIGSurface* self, double moistureTenHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureTenHour(moistureTenHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setOverstoryBasalArea_2(SIGSurface* self, double overstoryBasalArea, BasalAreaUnits_BasalAreaUnitsEnum basalAreaUnits) {
  self->setOverstoryBasalArea(overstoryBasalArea, basalAreaUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setPalmettoCoverage_2(SIGSurface* self, double palmettoCoverage, FractionUnits_FractionUnitsEnum coverUnits) {
  self->setPalmettoCoverage(palmettoCoverage, coverUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setSecondFuelModelNumber_1(SIGSurface* self, int secondFuelModelNumber) {
  self->setSecondFuelModelNumber(secondFuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setSlope_2(SIGSurface* self, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  self->setSlope(slope, slopeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setSurfaceFireSpreadDirectionMode_1(SIGSurface* self, SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum directionMode) {
  self->setSurfaceFireSpreadDirectionMode(directionMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setSurfaceRunInDirectionOf_1(SIGSurface* self, SurfaceRunInDirectionOf surfaceRunInDirectionOf) {
  self->setSurfaceRunInDirectionOf(surfaceRunInDirectionOf);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setTwoFuelModelsFirstFuelModelCoverage_2(SIGSurface* self, double firstFuelModelCoverage, FractionUnits_FractionUnitsEnum coverUnits) {
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

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setWindSpeed_2(SIGSurface* self, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setWindSpeed(windSpeed, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputs_21(SIGSurface* self, int fuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, FractionUnits_FractionUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, FractionUnits_FractionUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  self->updateSurfaceInputs(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputsForPalmettoGallbery_25(SIGSurface* self, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, FractionUnits_FractionUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double ageOfRough, double heightOfUnderstory, double palmettoCoverage, double overstoryBasalArea, BasalAreaUnits_BasalAreaUnitsEnum basalAreaUnits, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, FractionUnits_FractionUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  self->updateSurfaceInputsForPalmettoGallbery(moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, ageOfRough, heightOfUnderstory, palmettoCoverage, overstoryBasalArea, basalAreaUnits, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputsForTwoFuelModels_25(SIGSurface* self, int firstFuelModelNumber, int secondFuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, FractionUnits_FractionUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double firstFuelModelCoverage, FractionUnits_FractionUnitsEnum firstFuelModelCoverageUnits, TwoFuelModelsMethod_TwoFuelModelsMethodEnum twoFuelModelsMethod, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, FractionUnits_FractionUnitsEnum canopyFractionUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnitso) {
  self->updateSurfaceInputsForTwoFuelModels(firstFuelModelNumber, secondFuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, firstFuelModelCoverage, firstFuelModelCoverageUnits, twoFuelModelsMethod, slope, slopeUnits, aspect, canopyCover, canopyFractionUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnitso);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_updateSurfaceInputsForWesternAspen_26(SIGSurface* self, int aspenFuelModelNumber, double aspenCuringLevel, FractionUnits_FractionUnitsEnum curingLevelUnits, AspenFireSeverity_AspenFireSeverityEnum aspenFireSeverity, double dbh, LengthUnits_LengthUnitsEnum dbhUnits, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, FractionUnits_FractionUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, FractionUnits_FractionUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  self->updateSurfaceInputsForWesternAspen(aspenFuelModelNumber, aspenCuringLevel, curingLevelUnits, aspenFireSeverity, dbh, dbhUnits, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSurface_setFuelModelNumber_1(SIGSurface* self, int fuelModelNumber) {
  self->setFuelModelNumber(fuelModelNumber);
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

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFineFuelLoad_2(PalmettoGallberry* self, double ageOfRough, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyDeadFineFuelLoad(ageOfRough, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadFoliageLoad_2(PalmettoGallberry* self, double ageOfRough, double palmettoCoverage) {
  return self->calculatePalmettoGallberyDeadFoliageLoad(ageOfRough, palmettoCoverage);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyDeadMediumFuelLoad_2(PalmettoGallberry* self, double ageOfRough, double palmettoCoverage) {
  return self->calculatePalmettoGallberyDeadMediumFuelLoad(ageOfRough, palmettoCoverage);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyFuelBedDepth_1(PalmettoGallberry* self, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyFuelBedDepth(heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLitterLoad_2(PalmettoGallberry* self, double ageOfRough, double overstoryBasalArea) {
  return self->calculatePalmettoGallberyLitterLoad(ageOfRough, overstoryBasalArea);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFineFuelLoad_2(PalmettoGallberry* self, double ageOfRough, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyLiveFineFuelLoad(ageOfRough, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveFoliageLoad_3(PalmettoGallberry* self, double ageOfRough, double palmettoCoverage, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyLiveFoliageLoad(ageOfRough, palmettoCoverage, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_calculatePalmettoGallberyLiveMediumFuelLoad_2(PalmettoGallberry* self, double ageOfRough, double heightOfUnderstory) {
  return self->calculatePalmettoGallberyLiveMediumFuelLoad(ageOfRough, heightOfUnderstory);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getHeatOfCombustionDead_0(PalmettoGallberry* self) {
  return self->getHeatOfCombustionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getHeatOfCombustionLive_0(PalmettoGallberry* self) {
  return self->getHeatOfCombustionLive();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getMoistureOfExtinctionDead_0(PalmettoGallberry* self) {
  return self->getMoistureOfExtinctionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFineFuelLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyDeadFineFuelLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadFoliageLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyDeadFoliageLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyDeadMediumFuelLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyDeadMediumFuelLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyFuelBedDepth_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyFuelBedDepth();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLitterLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLitterLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFineFuelLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLiveFineFuelLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveFoliageLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLiveFoliageLoad();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_PalmettoGallberry_getPalmettoGallberyLiveMediumFuelLoad_0(PalmettoGallberry* self) {
  return self->getPalmettoGallberyLiveMediumFuelLoad();
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

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenFuelBedDepth_1(WesternAspen* self, int typeIndex) {
  return self->getAspenFuelBedDepth(typeIndex);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenHeatOfCombustionDead_0(WesternAspen* self) {
  return self->getAspenHeatOfCombustionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenHeatOfCombustionLive_0(WesternAspen* self) {
  return self->getAspenHeatOfCombustionLive();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadDeadOneHour_0(WesternAspen* self) {
  return self->getAspenLoadDeadOneHour();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadDeadTenHour_0(WesternAspen* self) {
  return self->getAspenLoadDeadTenHour();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadLiveHerbaceous_0(WesternAspen* self) {
  return self->getAspenLoadLiveHerbaceous();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenLoadLiveWoody_0(WesternAspen* self) {
  return self->getAspenLoadLiveWoody();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenMoistureOfExtinctionDead_0(WesternAspen* self) {
  return self->getAspenMoistureOfExtinctionDead();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenMortality_0(WesternAspen* self) {
  return self->getAspenMortality();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrDeadOneHour_0(WesternAspen* self) {
  return self->getAspenSavrDeadOneHour();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrDeadTenHour_0(WesternAspen* self) {
  return self->getAspenSavrDeadTenHour();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrLiveHerbaceous_0(WesternAspen* self) {
  return self->getAspenSavrLiveHerbaceous();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen_getAspenSavrLiveWoody_0(WesternAspen* self) {
  return self->getAspenSavrLiveWoody();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_WesternAspen___destroy___0(WesternAspen* self) {
  delete self;
}

// SIGCrown

SIGCrown* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_SIGCrown_1(SIGFuelModels* fuelModels) {
  return new SIGCrown(*fuelModels);
}

FireType_FireTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFireType_0(SIGCrown* self) {
  return self->getFireType();
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByIndex_1(SIGCrown* self, int index) {
  return self->getIsMoistureScenarioDefinedByIndex(index);
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getIsMoistureScenarioDefinedByName_1(SIGCrown* self, const char* name) {
  return self->getIsMoistureScenarioDefinedByName(name);
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_isAllFuelLoadZero_1(SIGCrown* self, int fuelModelNumber) {
  return self->isAllFuelLoadZero(fuelModelNumber);
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_isFuelDynamic_1(SIGCrown* self, int fuelModelNumber) {
  return self->isFuelDynamic(fuelModelNumber);
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_isFuelModelDefined_1(SIGCrown* self, int fuelModelNumber) {
  return self->isFuelModelDefined(fuelModelNumber);
}

const bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_isFuelModelReserved_1(SIGCrown* self, int fuelModelNumber) {
  return self->isFuelModelReserved(fuelModelNumber);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCurrentMoistureScenarioByIndex_1(SIGCrown* self, int moistureScenarioIndex) {
  return self->setCurrentMoistureScenarioByIndex(moistureScenarioIndex);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCurrentMoistureScenarioByName_1(SIGCrown* self, const char* moistureScenarioName) {
  return self->setCurrentMoistureScenarioByName(moistureScenarioName);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getAspect_0(SIGCrown* self) {
  return self->getAspect();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCanopyBaseHeight_1(SIGCrown* self, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  return self->getCanopyBaseHeight(canopyHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCanopyBulkDensity_1(SIGCrown* self, DensityUnits_DensityUnitsEnum canopyBulkDensityUnits) {
  return self->getCanopyBulkDensity(canopyBulkDensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCanopyCover_1(SIGCrown* self, FractionUnits_FractionUnitsEnum canopyFractionUnits) {
  return self->getCanopyCover(canopyFractionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCanopyHeight_1(SIGCrown* self, LengthUnits_LengthUnitsEnum canopyHeighUnits) {
  return self->getCanopyHeight(canopyHeighUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCriticalOpenWindSpeed_1(SIGCrown* self, SpeedUnits_SpeedUnitsEnum speedUnits) {
  return self->getCriticalOpenWindSpeed(speedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownCriticalFireSpreadRate_1(SIGCrown* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getCrownCriticalFireSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownCriticalSurfaceFirelineIntensity_1(SIGCrown* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getCrownCriticalSurfaceFirelineIntensity(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownCriticalSurfaceFlameLength_1(SIGCrown* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getCrownCriticalSurfaceFlameLength(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFireActiveRatio_0(SIGCrown* self) {
  return self->getCrownFireActiveRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFireArea_1(SIGCrown* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getCrownFireArea(areaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFirePerimeter_1(SIGCrown* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getCrownFirePerimeter(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownTransitionRatio_0(SIGCrown* self) {
  return self->getCrownTransitionRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFireLengthToWidthRatio_0(SIGCrown* self) {
  return self->getCrownFireLengthToWidthRatio();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFireSpreadDistance_1(SIGCrown* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getCrownFireSpreadDistance(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFireSpreadRate_1(SIGCrown* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getCrownFireSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFirelineIntensity_1(SIGCrown* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getCrownFirelineIntensity(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFlameLength_1(SIGCrown* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getCrownFlameLength(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownFractionBurned_0(SIGCrown* self) {
  return self->getCrownFractionBurned();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getCrownRatio_1(SIGCrown* self, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  return self->getCrownRatio(crownRatioUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFinalFirelineIntesity_1(SIGCrown* self, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  return self->getFinalFirelineIntesity(firelineIntensityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFinalHeatPerUnitArea_1(SIGCrown* self, HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum heatPerUnitAreaUnits) {
  return self->getFinalHeatPerUnitArea(heatPerUnitAreaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFinalSpreadRate_1(SIGCrown* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getFinalSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFinalSpreadDistance_1(SIGCrown* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFinalSpreadDistance(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFinalFireArea_1(SIGCrown* self, AreaUnits_AreaUnitsEnum areaUnits) {
  return self->getFinalFireArea(areaUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFinalFirePerimeter_1(SIGCrown* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFinalFirePerimeter(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelHeatOfCombustionDead_2(SIGCrown* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getFuelHeatOfCombustionDead(fuelModelNumber, heatOfCombustionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelHeatOfCombustionLive_2(SIGCrown* self, int fuelModelNumber, HeatOfCombustionUnits_HeatOfCombustionUnitsEnum heatOfCombustionUnits) {
  return self->getFuelHeatOfCombustionLive(fuelModelNumber, heatOfCombustionUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelLoadHundredHour_2(SIGCrown* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadHundredHour(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelLoadLiveHerbaceous_2(SIGCrown* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveHerbaceous(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelLoadLiveWoody_2(SIGCrown* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadLiveWoody(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelLoadOneHour_2(SIGCrown* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadOneHour(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelLoadTenHour_2(SIGCrown* self, int fuelModelNumber, LoadingUnits_LoadingUnitsEnum loadingUnits) {
  return self->getFuelLoadTenHour(fuelModelNumber, loadingUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelMoistureOfExtinctionDead_2(SIGCrown* self, int fuelModelNumber, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getFuelMoistureOfExtinctionDead(fuelModelNumber, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelSavrLiveHerbaceous_2(SIGCrown* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getFuelSavrLiveHerbaceous(fuelModelNumber, savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelSavrLiveWoody_2(SIGCrown* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getFuelSavrLiveWoody(fuelModelNumber, savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelSavrOneHour_2(SIGCrown* self, int fuelModelNumber, SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum savrUnits) {
  return self->getFuelSavrOneHour(fuelModelNumber, savrUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelbedDepth_2(SIGCrown* self, int fuelModelNumber, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getFuelbedDepth(fuelModelNumber, lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureFoliar_1(SIGCrown* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureFoliar(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureHundredHour_1(SIGCrown* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureHundredHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureLiveHerbaceous_1(SIGCrown* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureLiveHerbaceous(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureLiveWoody_1(SIGCrown* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureLiveWoody(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureOneHour_1(SIGCrown* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureOneHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByIndex_2(SIGCrown* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioHundredHourByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioHundredHourByName_2(SIGCrown* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioHundredHourByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByIndex_2(SIGCrown* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveHerbaceousByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioLiveHerbaceousByName_2(SIGCrown* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveHerbaceousByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByIndex_2(SIGCrown* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveWoodyByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioLiveWoodyByName_2(SIGCrown* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioLiveWoodyByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioOneHourByIndex_2(SIGCrown* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioOneHourByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioOneHourByName_2(SIGCrown* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioOneHourByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioTenHourByIndex_2(SIGCrown* self, int index, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioTenHourByIndex(index, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioTenHourByName_2(SIGCrown* self, const char* name, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureScenarioTenHourByName(name, moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureTenHour_1(SIGCrown* self, FractionUnits_FractionUnitsEnum moistureUnits) {
  return self->getMoistureTenHour(moistureUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getSlope_1(SIGCrown* self, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  return self->getSlope(slopeUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getSurfaceFireSpreadDistance_1(SIGCrown* self, LengthUnits_LengthUnitsEnum lengthUnits) {
  return self->getSurfaceFireSpreadDistance(lengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getSurfaceFireSpreadRate_1(SIGCrown* self, SpeedUnits_SpeedUnitsEnum spreadRateUnits) {
  return self->getSurfaceFireSpreadRate(spreadRateUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getWindDirection_0(SIGCrown* self) {
  return self->getWindDirection();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getWindSpeed_2(SIGCrown* self, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  return self->getWindSpeed(windSpeedUnits, windHeightInputMode);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelModelNumber_0(SIGCrown* self) {
  return self->getFuelModelNumber();
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioIndexByName_1(SIGCrown* self, const char* name) {
  return self->getMoistureScenarioIndexByName(name);
}

const int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getNumberOfMoistureScenarios_0(SIGCrown* self) {
  return self->getNumberOfMoistureScenarios();
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelCode_1(SIGCrown* self, int fuelModelNumber) {
  return self->getFuelCode(fuelModelNumber);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFuelName_1(SIGCrown* self, int fuelModelNumber) {
  return self->getFuelName(fuelModelNumber);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByIndex_1(SIGCrown* self, int index) {
  return self->getMoistureScenarioDescriptionByIndex(index);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioDescriptionByName_1(SIGCrown* self, const char* name) {
  return self->getMoistureScenarioDescriptionByName(name);
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getMoistureScenarioNameByIndex_1(SIGCrown* self, int index) {
  return self->getMoistureScenarioNameByIndex(index);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_doCrownRun_0(SIGCrown* self) {
  self->doCrownRun();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_doCrownRunRothermel_0(SIGCrown* self) {
  self->doCrownRunRothermel();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_doCrownRunScottAndReinhardt_0(SIGCrown* self) {
  self->doCrownRunScottAndReinhardt();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_initializeMembers_0(SIGCrown* self) {
  self->initializeMembers();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setAspect_1(SIGCrown* self, double aspect) {
  self->setAspect(aspect);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCanopyBaseHeight_2(SIGCrown* self, double canopyBaseHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  self->setCanopyBaseHeight(canopyBaseHeight, canopyHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCanopyBulkDensity_2(SIGCrown* self, double canopyBulkDensity, DensityUnits_DensityUnitsEnum densityUnits) {
  self->setCanopyBulkDensity(canopyBulkDensity, densityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCanopyCover_2(SIGCrown* self, double canopyCover, FractionUnits_FractionUnitsEnum coverUnits) {
  self->setCanopyCover(canopyCover, coverUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCanopyHeight_2(SIGCrown* self, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits) {
  self->setCanopyHeight(canopyHeight, canopyHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCrownRatio_2(SIGCrown* self, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  self->setCrownRatio(crownRatio, crownRatioUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setFuelModelNumber_1(SIGCrown* self, int fuelModelNumber) {
  self->setFuelModelNumber(fuelModelNumber);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setCrownFireCalculationMethod_1(SIGCrown* self, CrownFireCalculationMethod CrownFireCalculationMethod) {
  self->setCrownFireCalculationMethod(CrownFireCalculationMethod);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setElapsedTime_2(SIGCrown* self, double elapsedTime, TimeUnits_TimeUnitsEnum timeUnits) {
  self->setElapsedTime(elapsedTime, timeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setFuelModels_1(SIGCrown* self, SIGFuelModels* fuelModels) {
  self->setFuelModels(*fuelModels);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureDeadAggregate_2(SIGCrown* self, double moistureDead, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureDeadAggregate(moistureDead, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureFoliar_2(SIGCrown* self, double foliarMoisture, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureFoliar(foliarMoisture, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureHundredHour_2(SIGCrown* self, double moistureHundredHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureHundredHour(moistureHundredHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureInputMode_1(SIGCrown* self, MoistureInputMode_MoistureInputModeEnum moistureInputMode) {
  self->setMoistureInputMode(moistureInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureLiveAggregate_2(SIGCrown* self, double moistureLive, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureLiveAggregate(moistureLive, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureLiveHerbaceous_2(SIGCrown* self, double moistureLiveHerbaceous, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureLiveHerbaceous(moistureLiveHerbaceous, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureLiveWoody_2(SIGCrown* self, double moistureLiveWoody, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureLiveWoody(moistureLiveWoody, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureOneHour_2(SIGCrown* self, double moistureOneHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureOneHour(moistureOneHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureScenarios_1(SIGCrown* self, SIGMoistureScenarios* moistureScenarios) {
  self->setMoistureScenarios(*moistureScenarios);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setMoistureTenHour_2(SIGCrown* self, double moistureTenHour, FractionUnits_FractionUnitsEnum moistureUnits) {
  self->setMoistureTenHour(moistureTenHour, moistureUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setSlope_2(SIGCrown* self, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  self->setSlope(slope, slopeUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setUserProvidedWindAdjustmentFactor_1(SIGCrown* self, double userProvidedWindAdjustmentFactor) {
  self->setUserProvidedWindAdjustmentFactor(userProvidedWindAdjustmentFactor);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setWindAdjustmentFactorCalculationMethod_1(SIGCrown* self, WindAdjustmentFactorCalculationMethod_WindAdjustmentFactorCalculationMethodEnum windAdjustmentFactorCalculationMethod) {
  self->setWindAdjustmentFactorCalculationMethod(windAdjustmentFactorCalculationMethod);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setWindAndSpreadOrientationMode_1(SIGCrown* self, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadAngleMode) {
  self->setWindAndSpreadOrientationMode(windAndSpreadAngleMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setWindDirection_1(SIGCrown* self, double windDirection) {
  self->setWindDirection(windDirection);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setWindHeightInputMode_1(SIGCrown* self, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindHeightInputMode(windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_setWindSpeed_2(SIGCrown* self, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setWindSpeed(windSpeed, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_updateCrownInputs_25(SIGCrown* self, int fuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, double moistureFoliar, FractionUnits_FractionUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, FractionUnits_FractionUnitsEnum coverUnits, double canopyHeight, double canopyBaseHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits, double canopyBulkDensity, DensityUnits_DensityUnitsEnum densityUnits) {
  self->updateCrownInputs(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureFoliar, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyBaseHeight, canopyHeightUnits, crownRatio, crownRatioUnits, canopyBulkDensity, densityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_updateCrownsSurfaceInputs_21(SIGCrown* self, int fuelModelNumber, double moistureOneHour, double moistureTenHour, double moistureHundredHour, double moistureLiveHerbaceous, double moistureLiveWoody, FractionUnits_FractionUnitsEnum moistureUnits, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double windDirection, WindAndSpreadOrientationMode_WindAndSpreadOrientationModeEnum windAndSpreadOrientationMode, double slope, SlopeUnits_SlopeUnitsEnum slopeUnits, double aspect, double canopyCover, FractionUnits_FractionUnitsEnum coverUnits, double canopyHeight, LengthUnits_LengthUnitsEnum canopyHeightUnits, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  self->updateCrownsSurfaceInputs(fuelModelNumber, moistureOneHour, moistureTenHour, moistureHundredHour, moistureLiveHerbaceous, moistureLiveWoody, moistureUnits, windSpeed, windSpeedUnits, windHeightInputMode, windDirection, windAndSpreadOrientationMode, slope, slopeUnits, aspect, canopyCover, coverUnits, canopyHeight, canopyHeightUnits, crownRatio, crownRatioUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown_getFinalFlameLength_1(SIGCrown* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getFinalFlameLength(flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGCrown___destroy___0(SIGCrown* self) {
  delete self;
}

// SpeciesMasterTableRecord

SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_0() {
  return new SpeciesMasterTableRecord();
}

SpeciesMasterTableRecord* EMSCRIPTEN_KEEPALIVE emscripten_bind_SpeciesMasterTableRecord_SpeciesMasterTableRecord_1(const SpeciesMasterTableRecord* rhs) {
  return new SpeciesMasterTableRecord(*rhs);
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

SIGMortality* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_SIGMortality_1(SpeciesMasterTable* speciesMasterTable) {
  return new SIGMortality(*speciesMasterTable);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_initializeMembers_0(SIGMortality* self) {
  self->initializeMembers();
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_checkIsInRegionAtSpeciesTableIndex_2(SIGMortality* self, int index, RegionCode region) {
  return self->checkIsInRegionAtSpeciesTableIndex(index, region);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_checkIsInRegionFromSpeciesCode_2(SIGMortality* self, char* speciesCode, RegionCode region) {
  return self->checkIsInRegionFromSpeciesCode(speciesCode, region);
}

bool EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_updateInputsForSpeciesCodeAndEquationType_2(SIGMortality* self, char* speciesCode, EquationType equationType) {
  return self->updateInputsForSpeciesCodeAndEquationType(speciesCode, equationType);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_calculateMortality_1(SIGMortality* self, FractionUnits_FractionUnitsEnum probablityUnits) {
  return self->calculateMortality(probablityUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_calculateScorchHeight_7(SIGMortality* self, double firelineIntensity, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits, double midFlameWindSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnits, LengthUnits_LengthUnitsEnum scorchHeightUnits) {
  return self->calculateScorchHeight(firelineIntensity, firelineIntensityUnits, midFlameWindSpeed, windSpeedUnits, airTemperature, temperatureUnits, scorchHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_calculateMortalityAllDirections_1(SIGMortality* self, FractionUnits_FractionUnitsEnum probablityUnits) {
  self->calculateMortalityAllDirections(probablityUnits);
}

BoolVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getRequiredFieldVector_0(SIGMortality* self) {
  return self->getRequiredFieldVector();
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

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesCode_0(SIGMortality* self) {
  return self->getSpeciesCode();
}

const char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesCodeAtSpeciesTableIndex_1(SIGMortality* self, int index) {
  return self->getSpeciesCodeAtSpeciesTableIndex(index);
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

const SpeciesMasterTableRecordVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegion_1(SIGMortality* self, RegionCode region) {
  return self->getSpeciesRecordVectorForRegion(region);
}

const SpeciesMasterTableRecordVector* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getSpeciesRecordVectorForRegionAndEquationType_2(SIGMortality* self, RegionCode region, EquationType equationType) {
  return self->getSpeciesRecordVectorForRegionAndEquationType(region, equationType);
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

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBoleCharHeightBacking_1(SIGMortality* self, LengthUnits_LengthUnitsEnum boleCharHeightUnits) {
  return self->getBoleCharHeightBacking(boleCharHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getBoleCharHeightFlanking_1(SIGMortality* self, LengthUnits_LengthUnitsEnum boleCharHeightUnits) {
  return self->getBoleCharHeightFlanking(boleCharHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCambiumKillRating_0(SIGMortality* self) {
  return self->getCambiumKillRating();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownDamage_0(SIGMortality* self) {
  return self->getCrownDamage();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCrownRatio_1(SIGMortality* self, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  return self->getCrownRatio(crownRatioUnits);
}

char* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getCVSorCLS_0(SIGMortality* self) {
  return self->getCVSorCLS();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getDBH_1(SIGMortality* self, LengthUnits_LengthUnitsEnum diameterUnits) {
  return self->getDBH(diameterUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getFlameLength_1(SIGMortality* self, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  return self->getFlameLength(flameLengthUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getFlameLengthOrScorchHeightValue_1(SIGMortality* self, LengthUnits_LengthUnitsEnum flameLengthOrScorchHeightUnits) {
  return self->getFlameLengthOrScorchHeightValue(flameLengthOrScorchHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getKilledTrees_0(SIGMortality* self) {
  return self->getKilledTrees();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getProbabilityOfMortality_1(SIGMortality* self, FractionUnits_FractionUnitsEnum probabilityUnits) {
  return self->getProbabilityOfMortality(probabilityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getProbabilityOfMortalityBacking_1(SIGMortality* self, FractionUnits_FractionUnitsEnum probabilityUnits) {
  return self->getProbabilityOfMortalityBacking(probabilityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getProbabilityOfMortalityFlanking_1(SIGMortality* self, FractionUnits_FractionUnitsEnum probabilityUnits) {
  return self->getProbabilityOfMortalityFlanking(probabilityUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getScorchHeight_1(SIGMortality* self, LengthUnits_LengthUnitsEnum scorchHeightUnits) {
  return self->getScorchHeight(scorchHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getScorchHeightBacking_1(SIGMortality* self, LengthUnits_LengthUnitsEnum scorchHeightUnits) {
  return self->getScorchHeightBacking(scorchHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getScorchHeightFlanking_1(SIGMortality* self, LengthUnits_LengthUnitsEnum scorchHeightUnits) {
  return self->getScorchHeightFlanking(scorchHeightUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTotalPrefireTrees_0(SIGMortality* self) {
  return self->getTotalPrefireTrees();
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeCrownLengthScorched_1(SIGMortality* self, LengthUnits_LengthUnitsEnum treeCrownLengthScorchedUnits) {
  return self->getTreeCrownLengthScorched(treeCrownLengthScorchedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeCrownLengthScorchedBacking_1(SIGMortality* self, LengthUnits_LengthUnitsEnum treeCrownLengthScorchedUnits) {
  return self->getTreeCrownLengthScorchedBacking(treeCrownLengthScorchedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeCrownLengthScorchedFlanking_1(SIGMortality* self, LengthUnits_LengthUnitsEnum treeCrownLengthScorchedUnits) {
  return self->getTreeCrownLengthScorchedFlanking(treeCrownLengthScorchedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeCrownVolumeScorched_1(SIGMortality* self, FractionUnits_FractionUnitsEnum getTreeCrownVolumeScorchedUnits) {
  return self->getTreeCrownVolumeScorched(getTreeCrownVolumeScorchedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeCrownVolumeScorchedBacking_1(SIGMortality* self, FractionUnits_FractionUnitsEnum getTreeCrownVolumeScorchedUnits) {
  return self->getTreeCrownVolumeScorchedBacking(getTreeCrownVolumeScorchedUnits);
}

const double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_getTreeCrownVolumeScorchedFlanking_1(SIGMortality* self, FractionUnits_FractionUnitsEnum getTreeCrownVolumeScorchedUnits) {
  return self->getTreeCrownVolumeScorchedFlanking(getTreeCrownVolumeScorchedUnits);
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

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setAirTemperature_2(SIGMortality* self, double airTemperature, TemperatureUnits_TemperatureUnitsEnum temperatureUnits) {
  self->setAirTemperature(airTemperature, temperatureUnits);
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

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setCrownRatio_2(SIGMortality* self, double crownRatio, FractionUnits_FractionUnitsEnum crownRatioUnits) {
  self->setCrownRatio(crownRatio, crownRatioUnits);
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

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setFirelineIntensity_2(SIGMortality* self, double firelineIntensity, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  self->setFirelineIntensity(firelineIntensity, firelineIntensityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setFlameLength_2(SIGMortality* self, double flameLength, LengthUnits_LengthUnitsEnum flameLengthUnits) {
  self->setFlameLength(flameLength, flameLengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightSwitch_1(SIGMortality* self, FlameLengthOrScorchHeightSwitch flameLengthOrScorchHeightSwitch) {
  self->setFlameLengthOrScorchHeightSwitch(flameLengthOrScorchHeightSwitch);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setFlameLengthOrScorchHeightValue_2(SIGMortality* self, double flameLengthOrScorchHeightValue, LengthUnits_LengthUnitsEnum flameLengthOrScorchHeightUnits) {
  self->setFlameLengthOrScorchHeightValue(flameLengthOrScorchHeightValue, flameLengthOrScorchHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setMidFlameWindSpeed_2(SIGMortality* self, double midFlameWindSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setMidFlameWindSpeed(midFlameWindSpeed, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setRegion_1(SIGMortality* self, RegionCode region) {
  self->setRegion(region);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setScorchHeight_2(SIGMortality* self, double scorchHeight, LengthUnits_LengthUnitsEnum scorchHeightUnits) {
  self->setScorchHeight(scorchHeight, scorchHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSpeciesCode_1(SIGMortality* self, char* speciesCode) {
  self->setSpeciesCode(speciesCode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensity_2(SIGMortality* self, double value, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  self->setSurfaceFireFirelineIntensity(value, firelineIntensityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensityBacking_2(SIGMortality* self, double value, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  self->setSurfaceFireFirelineIntensityBacking(value, firelineIntensityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSurfaceFireFirelineIntensityFlanking_2(SIGMortality* self, double value, FirelineIntensityUnits_FirelineIntensityUnitsEnum firelineIntensityUnits) {
  self->setSurfaceFireFirelineIntensityFlanking(value, firelineIntensityUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSurfaceFireFlameLength_2(SIGMortality* self, double value, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setSurfaceFireFlameLength(value, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSurfaceFireFlameLengthBacking_2(SIGMortality* self, double value, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setSurfaceFireFlameLengthBacking(value, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSurfaceFireFlameLengthFlanking_2(SIGMortality* self, double value, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setSurfaceFireFlameLengthFlanking(value, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setSurfaceFireScorchHeight_2(SIGMortality* self, double value, LengthUnits_LengthUnitsEnum lengthUnits) {
  self->setSurfaceFireScorchHeight(value, lengthUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setTreeDensityPerUnitArea_2(SIGMortality* self, double numberOfTrees, AreaUnits_AreaUnitsEnum areaUnits) {
  self->setTreeDensityPerUnitArea(numberOfTrees, areaUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setTreeHeight_2(SIGMortality* self, double treeHeight, LengthUnits_LengthUnitsEnum treeHeightUnits) {
  self->setTreeHeight(treeHeight, treeHeightUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setUserProvidedWindAdjustmentFactor_1(SIGMortality* self, double userProvidedWindAdjustmentFactor) {
  self->setUserProvidedWindAdjustmentFactor(userProvidedWindAdjustmentFactor);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setWindHeightInputMode_1(SIGMortality* self, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode) {
  self->setWindHeightInputMode(windHeightInputMode);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setWindSpeed_2(SIGMortality* self, double windSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits) {
  self->setWindSpeed(windSpeed, windSpeedUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGMortality_setWindSpeedAndWindHeightInputMode_4(SIGMortality* self, double windwindSpeed, SpeedUnits_SpeedUnitsEnum windSpeedUnits, WindHeightInputMode_WindHeightInputModeEnum windHeightInputMode, double userProvidedWindAdjustmentFactor) {
  self->setWindSpeedAndWindHeightInputMode(windwindSpeed, windSpeedUnits, windHeightInputMode, userProvidedWindAdjustmentFactor);
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

// SIGFineDeadFuelMoistureTool

SIGFineDeadFuelMoistureTool* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_SIGFineDeadFuelMoistureTool_0() {
  return new SIGFineDeadFuelMoistureTool();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_calculate_0(SIGFineDeadFuelMoistureTool* self) {
  self->calculate();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setTimeOfDayIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum timeOfDayIndex) {
  self->setTimeOfDayIndex(timeOfDayIndex);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setSlopeIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolSlopeIndex_SlopeIndexEnum slopeIndex) {
  self->setSlopeIndex(slopeIndex);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setShadingIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolShadingIndex_ShadingIndexEnum shadingIndex) {
  self->setShadingIndex(shadingIndex);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setAspectIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolAspectIndex_AspectIndexEnum aspectIndex) {
  self->setAspectIndex(aspectIndex);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setRHIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolRHIndex_RHIndexEnum relativeHumidityIndex) {
  self->setRHIndex(relativeHumidityIndex);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setElevationIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolElevationIndex_ElevationIndexEnum elevationIndex) {
  self->setElevationIndex(elevationIndex);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setDryBulbIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolDryBulbIndex_DryBulbIndexEnum dryBulbIndex) {
  self->setDryBulbIndex(dryBulbIndex);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_setMonthIndex_1(SIGFineDeadFuelMoistureTool* self, FDFMToolMonthIndex_MonthIndexEnum monthIndex) {
  self->setMonthIndex(monthIndex);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getFineDeadFuelMoisture_1(SIGFineDeadFuelMoistureTool* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->getFineDeadFuelMoisture(desiredUnits);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getSlopeIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getSlopeIndexSize();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getElevationIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getElevationIndexSize();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getMonthIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getMonthIndexSize();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getDryBulbTemperatureIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getDryBulbTemperatureIndexSize();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getReferenceMoisture_1(SIGFineDeadFuelMoistureTool* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->getReferenceMoisture(desiredUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_calculateByIndex_8(SIGFineDeadFuelMoistureTool* self, int aspectIndex, int dryBulbIndex, int elevationIndex, int monthIndex, int relativeHumidityIndex, int shadingIndex, int slopeIndex, int timeOfDayIndex) {
  self->calculateByIndex(aspectIndex, dryBulbIndex, elevationIndex, monthIndex, relativeHumidityIndex, shadingIndex, slopeIndex, timeOfDayIndex);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getTimeOfDayIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getTimeOfDayIndexSize();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getCorrectionMoisture_1(SIGFineDeadFuelMoistureTool* self, FractionUnits_FractionUnitsEnum desiredUnits) {
  return self->getCorrectionMoisture(desiredUnits);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getAspectIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getAspectIndexSize();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getShadingIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getShadingIndexSize();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool_getRelativeHumidityIndexSize_0(SIGFineDeadFuelMoistureTool* self) {
  return self->getRelativeHumidityIndexSize();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGFineDeadFuelMoistureTool___destroy___0(SIGFineDeadFuelMoistureTool* self) {
  delete self;
}

// SIGSlopeTool

SIGSlopeTool* EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_SIGSlopeTool_0() {
  return new SIGSlopeTool();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getCentimetersPerKilometerAtIndex_1(SIGSlopeTool* self, int index) {
  return self->getCentimetersPerKilometerAtIndex(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getCentimetersPerKilometerAtRepresentativeFraction_1(SIGSlopeTool* self, RepresentativeFraction_RepresentativeFractionEnum representativeFraction) {
  return self->getCentimetersPerKilometerAtRepresentativeFraction(representativeFraction);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistance_2(SIGSlopeTool* self, HorizontalDistanceIndex_HorizontalDistanceIndexEnum horizontalDistanceIndex, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistance(horizontalDistanceIndex, mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceAtIndex_2(SIGSlopeTool* self, int index, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceAtIndex(index, mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceFifteen_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceFifteen(mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceFourtyFive_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceFourtyFive(mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceMaxSlope_1(SIGSlopeTool* self, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  return self->getHorizontalDistanceMaxSlope(slopeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceNinety_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceNinety(mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceSeventy_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceSeventy(mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceSixty_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceSixty(mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceThirty_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceThirty(mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getHorizontalDistanceZero_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum mapDistanceUnits) {
  return self->getHorizontalDistanceZero(mapDistanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getInchesPerMileAtIndex_1(SIGSlopeTool* self, int index) {
  return self->getInchesPerMileAtIndex(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getInchesPerMileAtRepresentativeFraction_1(SIGSlopeTool* self, RepresentativeFraction_RepresentativeFractionEnum representativeFraction) {
  return self->getInchesPerMileAtRepresentativeFraction(representativeFraction);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getKilometersPerCentimeterAtIndex_1(SIGSlopeTool* self, int index) {
  return self->getKilometersPerCentimeterAtIndex(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getKilometersPerCentimeterAtRepresentativeFraction_1(SIGSlopeTool* self, RepresentativeFraction_RepresentativeFractionEnum representativeFraction) {
  return self->getKilometersPerCentimeterAtRepresentativeFraction(representativeFraction);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getMilesPerInchAtIndex_1(SIGSlopeTool* self, int index) {
  return self->getMilesPerInchAtIndex(index);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getMilesPerInchAtRepresentativeFraction_1(SIGSlopeTool* self, RepresentativeFraction_RepresentativeFractionEnum representativeFraction) {
  return self->getMilesPerInchAtRepresentativeFraction(representativeFraction);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getSlopeElevationChangeFromMapMeasurements_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum elevationUnits) {
  return self->getSlopeElevationChangeFromMapMeasurements(elevationUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurements_1(SIGSlopeTool* self, SlopeUnits_SlopeUnitsEnum slopeUnits) {
  return self->getSlopeFromMapMeasurements(slopeUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getSlopeHorizontalDistanceFromMapMeasurements_1(SIGSlopeTool* self, LengthUnits_LengthUnitsEnum distanceUnits) {
  return self->getSlopeHorizontalDistanceFromMapMeasurements(distanceUnits);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurementsInDegrees_0(SIGSlopeTool* self) {
  return self->getSlopeFromMapMeasurementsInDegrees();
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getSlopeFromMapMeasurementsInPercent_0(SIGSlopeTool* self) {
  return self->getSlopeFromMapMeasurementsInPercent();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getNumberOfHorizontalDistances_0(SIGSlopeTool* self) {
  return self->getNumberOfHorizontalDistances();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getNumberOfRepresentativeFractions_0(SIGSlopeTool* self) {
  return self->getNumberOfRepresentativeFractions();
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getRepresentativeFractionAtIndex_1(SIGSlopeTool* self, int index) {
  return self->getRepresentativeFractionAtIndex(index);
}

int EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_getRepresentativeFractionAtRepresentativeFraction_1(SIGSlopeTool* self, RepresentativeFraction_RepresentativeFractionEnum representativeFraction) {
  return self->getRepresentativeFractionAtRepresentativeFraction(representativeFraction);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_calculateHorizontalDistance_0(SIGSlopeTool* self) {
  self->calculateHorizontalDistance();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_calculateSlopeFromMapMeasurements_0(SIGSlopeTool* self) {
  self->calculateSlopeFromMapMeasurements();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_setCalculatedMapDistance_2(SIGSlopeTool* self, double calculatedMapDistance, LengthUnits_LengthUnitsEnum distanceUnits) {
  self->setCalculatedMapDistance(calculatedMapDistance, distanceUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_setContourInterval_2(SIGSlopeTool* self, double contourInterval, LengthUnits_LengthUnitsEnum contourUnits) {
  self->setContourInterval(contourInterval, contourUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_setMapDistance_2(SIGSlopeTool* self, double mapDistance, LengthUnits_LengthUnitsEnum distanceUnits) {
  self->setMapDistance(mapDistance, distanceUnits);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_setMapRepresentativeFraction_1(SIGSlopeTool* self, int mapRepresentativeFraction) {
  self->setMapRepresentativeFraction(mapRepresentativeFraction);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_setMaxSlopeSteepness_1(SIGSlopeTool* self, double maxSlopeSteepness) {
  self->setMaxSlopeSteepness(maxSlopeSteepness);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool_setNumberOfContours_1(SIGSlopeTool* self, double numberOfContours) {
  self->setNumberOfContours(numberOfContours);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_SIGSlopeTool___destroy___0(SIGSlopeTool* self) {
  delete self;
}

// VaporPressureDeficitCalculator

VaporPressureDeficitCalculator* EMSCRIPTEN_KEEPALIVE emscripten_bind_VaporPressureDeficitCalculator_VaporPressureDeficitCalculator_0() {
  return new VaporPressureDeficitCalculator();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_VaporPressureDeficitCalculator_runCalculation_0(VaporPressureDeficitCalculator* self) {
  self->runCalculation();
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_VaporPressureDeficitCalculator_setTemperature_2(VaporPressureDeficitCalculator* self, double temperature, TemperatureUnits_TemperatureUnitsEnum units) {
  self->setTemperature(temperature, units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_VaporPressureDeficitCalculator_setRelativeHumidity_2(VaporPressureDeficitCalculator* self, double relativeHumidity, FractionUnits_FractionUnitsEnum units) {
  self->setRelativeHumidity(relativeHumidity, units);
}

double EMSCRIPTEN_KEEPALIVE emscripten_bind_VaporPressureDeficitCalculator_getVaporPressureDeficit_1(VaporPressureDeficitCalculator* self, PressureUnits_PressureUnitsEnum units) {
  return self->getVaporPressureDeficit(units);
}

void EMSCRIPTEN_KEEPALIVE emscripten_bind_VaporPressureDeficitCalculator___destroy___0(VaporPressureDeficitCalculator* self) {
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

// BasalAreaUnits_BasalAreaUnitsEnum
BasalAreaUnits_BasalAreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareFeetPerAcre() {
  return BasalAreaUnits::SquareFeetPerAcre;
}
BasalAreaUnits_BasalAreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_BasalAreaUnits_BasalAreaUnitsEnum_SquareMetersPerHectare() {
  return BasalAreaUnits::SquareMetersPerHectare;
}

// FractionUnits_FractionUnitsEnum
FractionUnits_FractionUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FractionUnits_FractionUnitsEnum_Fraction() {
  return FractionUnits::Fraction;
}
FractionUnits_FractionUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FractionUnits_FractionUnitsEnum_Percent() {
  return FractionUnits::Percent;
}

// LengthUnits_LengthUnitsEnum
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Feet() {
  return LengthUnits::Feet;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Inches() {
  return LengthUnits::Inches;
}
LengthUnits_LengthUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LengthUnits_LengthUnitsEnum_Millimeters() {
  return LengthUnits::Millimeters;
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
SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceAreaToVolumeUnits_SurfaceAreaToVolumeUnitsEnum_SquareCentimetersOverCubicCentimeters() {
  return SurfaceAreaToVolumeUnits::SquareCentimetersOverCubicCentimeters;
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

// PressureUnits_PressureUnitsEnum
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_Pascal() {
  return PressureUnits::Pascal;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_HectoPascal() {
  return PressureUnits::HectoPascal;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_KiloPascal() {
  return PressureUnits::KiloPascal;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_MegaPascal() {
  return PressureUnits::MegaPascal;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_GigaPascal() {
  return PressureUnits::GigaPascal;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_Bar() {
  return PressureUnits::Bar;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_Atmosphere() {
  return PressureUnits::Atmosphere;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_TechnicalAtmosphere() {
  return PressureUnits::TechnicalAtmosphere;
}
PressureUnits_PressureUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_PressureUnits_PressureUnitsEnum_PoundPerSquareInch() {
  return PressureUnits::PoundPerSquareInch;
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
HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilojoulesPerSquareMeter() {
  return HeatPerUnitAreaUnits::KilojoulesPerSquareMeter;
}
HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HeatPerUnitAreaUnits_HeatPerUnitAreaUnitsEnum_KilowattSecondsPerSquareMeter() {
  return HeatPerUnitAreaUnits::KilowattSecondsPerSquareMeter;
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

// ContainTactic_ContainTacticEnum
ContainTactic_ContainTacticEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainTactic_ContainTacticEnum_HeadAttack() {
  return ContainTactic::HeadAttack;
}
ContainTactic_ContainTacticEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainTactic_ContainTacticEnum_RearAttack() {
  return ContainTactic::RearAttack;
}

// ContainStatus_ContainStatusEnum
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_Unreported() {
  return ContainStatus::Unreported;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_Reported() {
  return ContainStatus::Reported;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_Attacked() {
  return ContainStatus::Attacked;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_Contained() {
  return ContainStatus::Contained;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_Overrun() {
  return ContainStatus::Overrun;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_Exhausted() {
  return ContainStatus::Exhausted;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_Overflow() {
  return ContainStatus::Overflow;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_SizeLimitExceeded() {
  return ContainStatus::SizeLimitExceeded;
}
ContainStatus_ContainStatusEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainStatus_ContainStatusEnum_TimeLimitExceeded() {
  return ContainStatus::TimeLimitExceeded;
}

// ContainFlank_ContainFlankEnum
ContainFlank_ContainFlankEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_ContainFlankEnum_LeftFlank() {
  return ContainFlank::LeftFlank;
}
ContainFlank_ContainFlankEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_ContainFlankEnum_RightFlank() {
  return ContainFlank::RightFlank;
}
ContainFlank_ContainFlankEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_ContainFlankEnum_BothFlanks() {
  return ContainFlank::BothFlanks;
}
ContainFlank_ContainFlankEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ContainFlank_ContainFlankEnum_NeitherFlank() {
  return ContainFlank::NeitherFlank;
}

// IgnitionFuelBedType_IgnitionFuelBedTypeEnum
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PonderosaPineLitter() {
  return IgnitionFuelBedType::PonderosaPineLitter;
}
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodRottenChunky() {
  return IgnitionFuelBedType::PunkyWoodRottenChunky;
}
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkyWoodPowderDeep() {
  return IgnitionFuelBedType::PunkyWoodPowderDeep;
}
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PunkWoodPowderShallow() {
  return IgnitionFuelBedType::PunkWoodPowderShallow;
}
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_LodgepolePineDuff() {
  return IgnitionFuelBedType::LodgepolePineDuff;
}
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_DouglasFirDuff() {
  return IgnitionFuelBedType::DouglasFirDuff;
}
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_HighAltitudeMixed() {
  return IgnitionFuelBedType::HighAltitudeMixed;
}
IgnitionFuelBedType_IgnitionFuelBedTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_IgnitionFuelBedType_IgnitionFuelBedTypeEnum_PeatMoss() {
  return IgnitionFuelBedType::PeatMoss;
}

// LightningCharge_LightningChargeEnum
LightningCharge_LightningChargeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LightningCharge_LightningChargeEnum_Negative() {
  return LightningCharge::Negative;
}
LightningCharge_LightningChargeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LightningCharge_LightningChargeEnum_Positive() {
  return LightningCharge::Positive;
}
LightningCharge_LightningChargeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_LightningCharge_LightningChargeEnum_Unknown() {
  return LightningCharge::Unknown;
}

// SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum
SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_CLOSED() {
  return SpotDownWindCanopyMode::CLOSED;
}
SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotDownWindCanopyMode_SpotDownWindCanopyModeEnum_OPEN() {
  return SpotDownWindCanopyMode::OPEN;
}

// SpotTreeSpecies_SpotTreeSpeciesEnum
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_ENGELMANN_SPRUCE() {
  return SpotTreeSpecies::ENGELMANN_SPRUCE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_DOUGLAS_FIR() {
  return SpotTreeSpecies::DOUGLAS_FIR;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SUBALPINE_FIR() {
  return SpotTreeSpecies::SUBALPINE_FIR;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_HEMLOCK() {
  return SpotTreeSpecies::WESTERN_HEMLOCK;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_PONDEROSA_PINE() {
  return SpotTreeSpecies::PONDEROSA_PINE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LODGEPOLE_PINE() {
  return SpotTreeSpecies::LODGEPOLE_PINE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_WESTERN_WHITE_PINE() {
  return SpotTreeSpecies::WESTERN_WHITE_PINE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_GRAND_FIR() {
  return SpotTreeSpecies::GRAND_FIR;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_BALSAM_FIR() {
  return SpotTreeSpecies::BALSAM_FIR;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SLASH_PINE() {
  return SpotTreeSpecies::SLASH_PINE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LONGLEAF_PINE() {
  return SpotTreeSpecies::LONGLEAF_PINE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_POND_PINE() {
  return SpotTreeSpecies::POND_PINE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_SHORTLEAF_PINE() {
  return SpotTreeSpecies::SHORTLEAF_PINE;
}
SpotTreeSpecies_SpotTreeSpeciesEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotTreeSpecies_SpotTreeSpeciesEnum_LOBLOLLY_PINE() {
  return SpotTreeSpecies::LOBLOLLY_PINE;
}

// SpotFireLocation_SpotFireLocationEnum
SpotFireLocation_SpotFireLocationEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_WINDWARD() {
  return SpotFireLocation::MIDSLOPE_WINDWARD;
}
SpotFireLocation_SpotFireLocationEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_SpotFireLocationEnum_VALLEY_BOTTOM() {
  return SpotFireLocation::VALLEY_BOTTOM;
}
SpotFireLocation_SpotFireLocationEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_SpotFireLocationEnum_MIDSLOPE_LEEWARD() {
  return SpotFireLocation::MIDSLOPE_LEEWARD;
}
SpotFireLocation_SpotFireLocationEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SpotFireLocation_SpotFireLocationEnum_RIDGE_TOP() {
  return SpotFireLocation::RIDGE_TOP;
}

// FuelLifeState_FuelLifeStateEnum
FuelLifeState_FuelLifeStateEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelLifeState_FuelLifeStateEnum_Dead() {
  return FuelLifeState::Dead;
}
FuelLifeState_FuelLifeStateEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelLifeState_FuelLifeStateEnum_Live() {
  return FuelLifeState::Live;
}

// FuelConstantsEnum_FuelConstantsEnum
FuelConstantsEnum_FuelConstantsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLifeStates() {
  return FuelConstants::MaxLifeStates;
}
FuelConstantsEnum_FuelConstantsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxLiveSizeClasses() {
  return FuelConstants::MaxLiveSizeClasses;
}
FuelConstantsEnum_FuelConstantsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxDeadSizeClasses() {
  return FuelConstants::MaxDeadSizeClasses;
}
FuelConstantsEnum_FuelConstantsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxParticles() {
  return FuelConstants::MaxParticles;
}
FuelConstantsEnum_FuelConstantsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxSavrSizeClasses() {
  return FuelConstants::MaxSavrSizeClasses;
}
FuelConstantsEnum_FuelConstantsEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FuelConstantsEnum_FuelConstantsEnum_MaxFuelModels() {
  return FuelConstants::MaxFuelModels;
}

// AspenFireSeverity_AspenFireSeverityEnum
AspenFireSeverity_AspenFireSeverityEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Low() {
  return AspenFireSeverity::Low;
}
AspenFireSeverity_AspenFireSeverityEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_AspenFireSeverity_AspenFireSeverityEnum_Moderate() {
  return AspenFireSeverity::Moderate;
}

// ChaparralFuelType_ChaparralFuelTypeEnum
ChaparralFuelType_ChaparralFuelTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_NotSet() {
  return ChaparralFuelType::NotSet;
}
ChaparralFuelType_ChaparralFuelTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_Chamise() {
  return ChaparralFuelType::Chamise;
}
ChaparralFuelType_ChaparralFuelTypeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ChaparralFuelType_ChaparralFuelTypeEnum_MixedBrush() {
  return ChaparralFuelType::MixedBrush;
}

// ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum
ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_DirectFuelLoad() {
  return ChaparralFuelLoadInputMode::DirectFuelLoad;
}
ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_ChaparralFuelLoadInputMode_ChaparralFuelInputLoadModeEnum_FuelLoadFromDepthAndChaparralType() {
  return ChaparralFuelLoadInputMode::FuelLoadFromDepthAndChaparralType;
}

// MoistureInputMode_MoistureInputModeEnum
MoistureInputMode_MoistureInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureInputMode_MoistureInputModeEnum_BySizeClass() {
  return MoistureInputMode::BySizeClass;
}
MoistureInputMode_MoistureInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureInputMode_MoistureInputModeEnum_AllAggregate() {
  return MoistureInputMode::AllAggregate;
}
MoistureInputMode_MoistureInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureInputMode_MoistureInputModeEnum_DeadAggregateAndLiveSizeClass() {
  return MoistureInputMode::DeadAggregateAndLiveSizeClass;
}
MoistureInputMode_MoistureInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureInputMode_MoistureInputModeEnum_LiveAggregateAndDeadSizeClass() {
  return MoistureInputMode::LiveAggregateAndDeadSizeClass;
}
MoistureInputMode_MoistureInputModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureInputMode_MoistureInputModeEnum_MoistureScenario() {
  return MoistureInputMode::MoistureScenario;
}

// MoistureClassInput_MoistureClassInputEnum
MoistureClassInput_MoistureClassInputEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureClassInput_MoistureClassInputEnum_OneHour() {
  return MoistureClassInput::OneHour;
}
MoistureClassInput_MoistureClassInputEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureClassInput_MoistureClassInputEnum_TenHour() {
  return MoistureClassInput::TenHour;
}
MoistureClassInput_MoistureClassInputEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureClassInput_MoistureClassInputEnum_HundredHour() {
  return MoistureClassInput::HundredHour;
}
MoistureClassInput_MoistureClassInputEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveHerbaceous() {
  return MoistureClassInput::LiveHerbaceous;
}
MoistureClassInput_MoistureClassInputEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveWoody() {
  return MoistureClassInput::LiveWoody;
}
MoistureClassInput_MoistureClassInputEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureClassInput_MoistureClassInputEnum_DeadAggregate() {
  return MoistureClassInput::DeadAggregate;
}
MoistureClassInput_MoistureClassInputEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_MoistureClassInput_MoistureClassInputEnum_LiveAggregate() {
  return MoistureClassInput::LiveAggregate;
}

// SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum
SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromIgnitionPoint() {
  return SurfaceFireSpreadDirectionMode::FromIgnitionPoint;
}
SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceFireSpreadDirectionMode_SurfaceFireSpreadDirectionModeEnum_FromPerimeter() {
  return SurfaceFireSpreadDirectionMode::FromPerimeter;
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

// WindUpslopeAlignmentMode
WindUpslopeAlignmentMode EMSCRIPTEN_KEEPALIVE emscripten_enum_WindUpslopeAlignmentMode_NotAligned() {
  return WindUpslopeAlignmentMode::NotAligned;
}
WindUpslopeAlignmentMode EMSCRIPTEN_KEEPALIVE emscripten_enum_WindUpslopeAlignmentMode_Aligned() {
  return WindUpslopeAlignmentMode::Aligned;
}

// SurfaceRunInDirectionOf
SurfaceRunInDirectionOf EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceRunInDirectionOf_MaxSpread() {
  return SurfaceRunInDirectionOf::MaxSpread;
}
SurfaceRunInDirectionOf EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceRunInDirectionOf_DirectionOfInterest() {
  return SurfaceRunInDirectionOf::DirectionOfInterest;
}
SurfaceRunInDirectionOf EMSCRIPTEN_KEEPALIVE emscripten_enum_SurfaceRunInDirectionOf_HeadingBackingFlanking() {
  return SurfaceRunInDirectionOf::HeadingBackingFlanking;
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
  return BeetleDamage::not_set;
}
BeetleDamage EMSCRIPTEN_KEEPALIVE emscripten_enum_BeetleDamage_no() {
  return BeetleDamage::no;
}
BeetleDamage EMSCRIPTEN_KEEPALIVE emscripten_enum_BeetleDamage_yes() {
  return BeetleDamage::yes;
}

// CrownFireCalculationMethod
CrownFireCalculationMethod EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownFireCalculationMethod_rothermel() {
  return CrownFireCalculationMethod::rothermel;
}
CrownFireCalculationMethod EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownFireCalculationMethod_scott_and_reinhardt() {
  return CrownFireCalculationMethod::scott_and_reinhardt;
}

// CrownDamageEquationCode
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_not_set() {
  return CrownDamageEquationCode::not_set;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_white_fir() {
  return CrownDamageEquationCode::white_fir;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_subalpine_fir() {
  return CrownDamageEquationCode::subalpine_fir;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_incense_cedar() {
  return CrownDamageEquationCode::incense_cedar;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_western_larch() {
  return CrownDamageEquationCode::western_larch;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_whitebark_pine() {
  return CrownDamageEquationCode::whitebark_pine;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_engelmann_spruce() {
  return CrownDamageEquationCode::engelmann_spruce;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_sugar_pine() {
  return CrownDamageEquationCode::sugar_pine;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_red_fir() {
  return CrownDamageEquationCode::red_fir;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_ponderosa_pine() {
  return CrownDamageEquationCode::ponderosa_pine;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_ponderosa_kill() {
  return CrownDamageEquationCode::ponderosa_kill;
}
CrownDamageEquationCode EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageEquationCode_douglas_fir() {
  return CrownDamageEquationCode::douglas_fir;
}

// CrownDamageType
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_not_set() {
  return CrownDamageType::not_set;
}
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_crown_length() {
  return CrownDamageType::crown_length;
}
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_crown_volume() {
  return CrownDamageType::crown_volume;
}
CrownDamageType EMSCRIPTEN_KEEPALIVE emscripten_enum_CrownDamageType_crown_kill() {
  return CrownDamageType::crown_kill;
}

// EquationType
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_not_set() {
  return EquationType::not_set;
}
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_crown_scorch() {
  return EquationType::crown_scorch;
}
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_bole_char() {
  return EquationType::bole_char;
}
EquationType EMSCRIPTEN_KEEPALIVE emscripten_enum_EquationType_crown_damage() {
  return EquationType::crown_damage;
}

// FireSeverity
FireSeverity EMSCRIPTEN_KEEPALIVE emscripten_enum_FireSeverity_not_set() {
  return FireSeverity::not_set;
}
FireSeverity EMSCRIPTEN_KEEPALIVE emscripten_enum_FireSeverity_empty() {
  return FireSeverity::empty;
}
FireSeverity EMSCRIPTEN_KEEPALIVE emscripten_enum_FireSeverity_low() {
  return FireSeverity::low;
}

// FlameLengthOrScorchHeightSwitch
FlameLengthOrScorchHeightSwitch EMSCRIPTEN_KEEPALIVE emscripten_enum_FlameLengthOrScorchHeightSwitch_flame_length() {
  return FlameLengthOrScorchHeightSwitch::flame_length;
}
FlameLengthOrScorchHeightSwitch EMSCRIPTEN_KEEPALIVE emscripten_enum_FlameLengthOrScorchHeightSwitch_scorch_height() {
  return FlameLengthOrScorchHeightSwitch::scorch_height;
}

// RegionCode
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_interior_west() {
  return RegionCode::interior_west;
}
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_pacific_west() {
  return RegionCode::pacific_west;
}
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_north_east() {
  return RegionCode::north_east;
}
RegionCode EMSCRIPTEN_KEEPALIVE emscripten_enum_RegionCode_south_east() {
  return RegionCode::south_east;
}

// RequiredFieldNames
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_region() {
  return RequiredFieldNames::region;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_switch() {
  return RequiredFieldNames::flame_length_or_scorch_height_switch;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_flame_length_or_scorch_height_value() {
  return RequiredFieldNames::flame_length_or_scorch_height_value;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_equation_type() {
  return RequiredFieldNames::equation_type;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_dbh() {
  return RequiredFieldNames::dbh;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_tree_height() {
  return RequiredFieldNames::tree_height;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_crown_ratio() {
  return RequiredFieldNames::crown_ratio;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_crown_damage() {
  return RequiredFieldNames::crown_damage;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_cambium_kill_rating() {
  return RequiredFieldNames::cambium_kill_rating;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_beetle_damage() {
  return RequiredFieldNames::beetle_damage;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_bole_char_height() {
  return RequiredFieldNames::bole_char_height;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_bark_thickness() {
  return RequiredFieldNames::bark_thickness;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_fire_severity() {
  return RequiredFieldNames::fire_severity;
}
RequiredFieldNames EMSCRIPTEN_KEEPALIVE emscripten_enum_RequiredFieldNames_num_inputs() {
  return RequiredFieldNames::num_inputs;
}

// FDFMToolAspectIndex_AspectIndexEnum
FDFMToolAspectIndex_AspectIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_NORTH() {
  return FDFMToolAspectIndex::NORTH;
}
FDFMToolAspectIndex_AspectIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_EAST() {
  return FDFMToolAspectIndex::EAST;
}
FDFMToolAspectIndex_AspectIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_SOUTH() {
  return FDFMToolAspectIndex::SOUTH;
}
FDFMToolAspectIndex_AspectIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolAspectIndex_AspectIndexEnum_WEST() {
  return FDFMToolAspectIndex::WEST;
}

// FDFMToolDryBulbIndex_DryBulbIndexEnum
FDFMToolDryBulbIndex_DryBulbIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_TEN_TO_TWENTY_NINE_DEGREES_F() {
  return FDFMToolDryBulbIndex::TEN_TO_TWENTY_NINE_DEGREES_F;
}
FDFMToolDryBulbIndex_DryBulbIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_THRITY_TO_FOURTY_NINE_DEGREES_F() {
  return FDFMToolDryBulbIndex::THRITY_TO_FOURTY_NINE_DEGREES_F;
}
FDFMToolDryBulbIndex_DryBulbIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_FIFTY_TO_SIXTY_NINE_DEGREES_F() {
  return FDFMToolDryBulbIndex::FIFTY_TO_SIXTY_NINE_DEGREES_F;
}
FDFMToolDryBulbIndex_DryBulbIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_SEVENTY_TO_EIGHTY_NINE_DEGREES_F() {
  return FDFMToolDryBulbIndex::SEVENTY_TO_EIGHTY_NINE_DEGREES_F;
}
FDFMToolDryBulbIndex_DryBulbIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_NINETY_TO_ONE_HUNDRED_NINE_DEGREES_F() {
  return FDFMToolDryBulbIndex::NINETY_TO_ONE_HUNDRED_NINE_DEGREES_F;
}
FDFMToolDryBulbIndex_DryBulbIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolDryBulbIndex_DryBulbIndexEnum_GREATER_THAN_ONE_HUNDRED_NINE_DEGREES_F() {
  return FDFMToolDryBulbIndex::GREATER_THAN_ONE_HUNDRED_NINE_DEGREES_F;
}

// FDFMToolElevationIndex_ElevationIndexEnum
FDFMToolElevationIndex_ElevationIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_BELOW_1000_TO_2000_FT() {
  return FDFMToolElevationIndex::BELOW_1000_TO_2000_FT;
}
FDFMToolElevationIndex_ElevationIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_LEVEL_WITHIN_1000_FT() {
  return FDFMToolElevationIndex::LEVEL_WITHIN_1000_FT;
}
FDFMToolElevationIndex_ElevationIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolElevationIndex_ElevationIndexEnum_ABOVE_1000_TO_2000_FT() {
  return FDFMToolElevationIndex::ABOVE_1000_TO_2000_FT;
}

// FDFMToolMonthIndex_MonthIndexEnum
FDFMToolMonthIndex_MonthIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_MAY_JUNE_JULY() {
  return FDFMToolMonthIndex::MAY_JUNE_JULY;
}
FDFMToolMonthIndex_MonthIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_FEB_MAR_APR_AUG_SEP_OCT() {
  return FDFMToolMonthIndex::FEB_MAR_APR_AUG_SEP_OCT;
}
FDFMToolMonthIndex_MonthIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolMonthIndex_MonthIndexEnum_NOV_DEC_JAN() {
  return FDFMToolMonthIndex::NOV_DEC_JAN;
}

// FDFMToolRHIndex_RHIndexEnum
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_ZERO_TO_FOUR_PERCENT() {
  return FDFMToolRHIndex::ZERO_TO_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIVE_TO_NINE_PERCENT() {
  return FDFMToolRHIndex::FIVE_TO_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_TEN_TO_FOURTEEN_PERCENT() {
  return FDFMToolRHIndex::TEN_TO_FOURTEEN_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIFTEEN_TO_NINETEEN_PERCENT() {
  return FDFMToolRHIndex::FIFTEEN_TO_NINETEEN_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_TWENTY_TO_TWENTY_FOUR_PERCENT() {
  return FDFMToolRHIndex::TWENTY_TO_TWENTY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_TWENTY_FIVE_TO_TWENTY_NINE_PERCENT() {
  return FDFMToolRHIndex::TWENTY_FIVE_TO_TWENTY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_THIRTY_TO_THIRTY_FOUR_PERCENT() {
  return FDFMToolRHIndex::THIRTY_TO_THIRTY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_THIRTY_FIVE_TO_THIRTY_NINE_PERCENT() {
  return FDFMToolRHIndex::THIRTY_FIVE_TO_THIRTY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FORTY_TO_FORTY_FOUR_PERCENT() {
  return FDFMToolRHIndex::FORTY_TO_FORTY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FORTY_FIVE_TO_FORTY_NINE_PERCENT() {
  return FDFMToolRHIndex::FORTY_FIVE_TO_FORTY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIFTY_TO_FIFTY_FOUR_PERCENT() {
  return FDFMToolRHIndex::FIFTY_TO_FIFTY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_FIFTY_FIVE_TO_FIFTY_NINE_PERCENT() {
  return FDFMToolRHIndex::FIFTY_FIVE_TO_FIFTY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SIXTY_TO_SIXTY_FOUR_PERCENT() {
  return FDFMToolRHIndex::SIXTY_TO_SIXTY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SIXTY_FIVE_TO_SIXTY_NINE_PERCENT() {
  return FDFMToolRHIndex::SIXTY_FIVE_TO_SIXTY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SEVENTY_TO_SEVENTY_FOUR_PERCENT() {
  return FDFMToolRHIndex::SEVENTY_TO_SEVENTY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_SEVENTY_FIVE_TO_SEVENTY_NINE_PERCENT() {
  return FDFMToolRHIndex::SEVENTY_FIVE_TO_SEVENTY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_EIGHTY_TO_EIGHTY_FOUR_PERCENT() {
  return FDFMToolRHIndex::EIGHTY_TO_EIGHTY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_EIGHTY_FIVE_TO_EIGHTY_NINE_PERCENT() {
  return FDFMToolRHIndex::EIGHTY_FIVE_TO_EIGHTY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_NINETY_TO_NINETY_FOUR_PERCENT() {
  return FDFMToolRHIndex::NINETY_TO_NINETY_FOUR_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_NINETY_FIVE_TO_NINETY_NINE_PERCENT() {
  return FDFMToolRHIndex::NINETY_FIVE_TO_NINETY_NINE_PERCENT;
}
FDFMToolRHIndex_RHIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolRHIndex_RHIndexEnum_ONE_HUNDRED_PERCENT() {
  return FDFMToolRHIndex::ONE_HUNDRED_PERCENT;
}

// FDFMToolShadingIndex_ShadingIndexEnum
FDFMToolShadingIndex_ShadingIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolShadingIndex_ShadingIndexEnum_EXPOSED() {
  return FDFMToolShadingIndex::EXPOSED;
}
FDFMToolShadingIndex_ShadingIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolShadingIndex_ShadingIndexEnum_SHADED() {
  return FDFMToolShadingIndex::SHADED;
}

// FDFMToolSlopeIndex_SlopeIndexEnum
FDFMToolSlopeIndex_SlopeIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolSlopeIndex_SlopeIndexEnum_ZERO_TO_THIRTY_PERCENT() {
  return FDFMToolSlopeIndex::ZERO_TO_THIRTY_PERCENT;
}
FDFMToolSlopeIndex_SlopeIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolSlopeIndex_SlopeIndexEnum_GREATER_THAN_OR_EQUAL_TO_THIRTY_ONE_PERCENT() {
  return FDFMToolSlopeIndex::GREATER_THAN_OR_EQUAL_TO_THIRTY_ONE_PERCENT;
}

// FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum
FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_EIGHT_HUNDRED_HOURS_TO_NINE_HUNDRED_FIFTY_NINE() {
  return FDFMToolTimeOfDayIndex::EIGHT_HUNDRED_HOURS_TO_NINE_HUNDRED_FIFTY_NINE;
}
FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_TEN_HUNDRED_HOURS_TO_ELEVEN__HUNDRED_FIFTY_NINE() {
  return FDFMToolTimeOfDayIndex::TEN_HUNDRED_HOURS_TO_ELEVEN__HUNDRED_FIFTY_NINE;
}
FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_TWELVE_HUNDRED_HOURS_TO_THIRTEEN_HUNDRED_FIFTY_NINE() {
  return FDFMToolTimeOfDayIndex::TWELVE_HUNDRED_HOURS_TO_THIRTEEN_HUNDRED_FIFTY_NINE;
}
FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_FOURTEEN_HUNDRED_HOURS_TO_FIFTEEN_HUNDRED_FIFTY_NINE() {
  return FDFMToolTimeOfDayIndex::FOURTEEN_HUNDRED_HOURS_TO_FIFTEEN_HUNDRED_FIFTY_NINE;
}
FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_SIXTEEN_HUNDRED_HOURS_TO_SIXTEEN_HUNDRED_FIFTY_NINE() {
  return FDFMToolTimeOfDayIndex::SIXTEEN_HUNDRED_HOURS_TO_SIXTEEN_HUNDRED_FIFTY_NINE;
}
FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_FDFMToolTimeOfDayIndex_TimeOfDayIndexEnum_EIGHTTEEN_HUNDRED_HOURS_TO_SUNSET() {
  return FDFMToolTimeOfDayIndex::EIGHTTEEN_HUNDRED_HOURS_TO_SUNSET;
}

// RepresentativeFraction_RepresentativeFractionEnum
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_NINTEEN_HUNDRED_EIGHTY() {
  return RepresentativeFraction::NINTEEN_HUNDRED_EIGHTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_THREE_THOUSAND_NINEHUNDRED_SIXTY() {
  return RepresentativeFraction::THREE_THOUSAND_NINEHUNDRED_SIXTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_SEVEN_THOUSAND_NINEHUNDRED_TWENTY() {
  return RepresentativeFraction::SEVEN_THOUSAND_NINEHUNDRED_TWENTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TEN_THOUSAND() {
  return RepresentativeFraction::TEN_THOUSAND;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_FIFTEEN_THOUSAND_EIGHT_HUNDRED_FORTY() {
  return RepresentativeFraction::FIFTEEN_THOUSAND_EIGHT_HUNDRED_FORTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWENTY_ONE_THOUSAND_ONE_HUNDRED_TWENTY() {
  return RepresentativeFraction::TWENTY_ONE_THOUSAND_ONE_HUNDRED_TWENTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWENTY_FOUR_THOUSAND() {
  return RepresentativeFraction::TWENTY_FOUR_THOUSAND;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_THRITY_ONE_THOUSAND_SIX_HUNDRED_EIGHTY() {
  return RepresentativeFraction::THRITY_ONE_THOUSAND_SIX_HUNDRED_EIGHTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_FIFTY_THOUSAND() {
  return RepresentativeFraction::FIFTY_THOUSAND;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_SIXTY_TWO_THOUSAND_FIVE_HUNDRED() {
  return RepresentativeFraction::SIXTY_TWO_THOUSAND_FIVE_HUNDRED;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_SIXTY_THREE_THOUSAND_THREE_HUNDRED_SIXTY() {
  return RepresentativeFraction::SIXTY_THREE_THOUSAND_THREE_HUNDRED_SIXTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_HUNDRED_THOUSAND() {
  return RepresentativeFraction::ONE_HUNDRED_THOUSAND;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_HUNDRED_TWENTY_SIX_THOUSAND_SEVEN_HUNDRED_TWENTY() {
  return RepresentativeFraction::ONE_HUNDRED_TWENTY_SIX_THOUSAND_SEVEN_HUNDRED_TWENTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWO_HUNDRED_FIFTY_THOUSAND() {
  return RepresentativeFraction::TWO_HUNDRED_FIFTY_THOUSAND;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_TWO_HUNDRED_FIFTY_THREE_THOUSAND_FOUR_HUNDRED_FORTY() {
  return RepresentativeFraction::TWO_HUNDRED_FIFTY_THREE_THOUSAND_FOUR_HUNDRED_FORTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_FIVE_HUNDRED_SIX_THOUSAND_EIGHT_HUNDRED_EIGHTY() {
  return RepresentativeFraction::FIVE_HUNDRED_SIX_THOUSAND_EIGHT_HUNDRED_EIGHTY;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_MILLION() {
  return RepresentativeFraction::ONE_MILLION;
}
RepresentativeFraction_RepresentativeFractionEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_RepresentativeFraction_RepresentativeFractionEnum_ONE_MILLION_THIRTEEN_THOUSAND_SEVEN_HUNDRED_SIXTY() {
  return RepresentativeFraction::ONE_MILLION_THIRTEEN_THOUSAND_SEVEN_HUNDRED_SIXTY;
}

// HorizontalDistanceIndex_HorizontalDistanceIndexEnum
HorizontalDistanceIndex_HorizontalDistanceIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_UPSLOPE_ZERO_DEGREES() {
  return HorizontalDistanceIndex::UPSLOPE_ZERO_DEGREES;
}
HorizontalDistanceIndex_HorizontalDistanceIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_FIFTEEN_DEGREES_FROM_UPSLOPE() {
  return HorizontalDistanceIndex::FIFTEEN_DEGREES_FROM_UPSLOPE;
}
HorizontalDistanceIndex_HorizontalDistanceIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_THIRTY_DEGREES_FROM_UPSLOPE() {
  return HorizontalDistanceIndex::THIRTY_DEGREES_FROM_UPSLOPE;
}
HorizontalDistanceIndex_HorizontalDistanceIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_FORTY_FIVE_DEGREES_FROM_UPSLOPE() {
  return HorizontalDistanceIndex::FORTY_FIVE_DEGREES_FROM_UPSLOPE;
}
HorizontalDistanceIndex_HorizontalDistanceIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_SIXTY_DEGREES_FROM_UPSLOPE() {
  return HorizontalDistanceIndex::SIXTY_DEGREES_FROM_UPSLOPE;
}
HorizontalDistanceIndex_HorizontalDistanceIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_SEVENTY_FIVE_DEGREES_FROM_UPSLOPE() {
  return HorizontalDistanceIndex::SEVENTY_FIVE_DEGREES_FROM_UPSLOPE;
}
HorizontalDistanceIndex_HorizontalDistanceIndexEnum EMSCRIPTEN_KEEPALIVE emscripten_enum_HorizontalDistanceIndex_HorizontalDistanceIndexEnum_CROSS_SLOPE_NINETY_DEGREES() {
  return HorizontalDistanceIndex::CROSS_SLOPE_NINETY_DEGREES;
}

}

