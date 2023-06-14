#include "SIGMoistureScenarios.h"

char* SIGMoistureScenarios::getMoistureScenarioDescriptionByName(const char* name) {
  SIGString::str2charptr(MoistureScenarios::getMoistureScenarioDescriptionByName(std::string(name)));
}

char* SIGMoistureScenarios::getMoistureScenarioNameByIndex(const int index) {
  SIGString::str2charptr(MoistureScenarios::getMoistureScenarioDescriptionByIndex(index));
}

char* SIGMoistureScenarios::getMoistureScenarioDescriptionByIndex(const int index) {
  SIGString::str2charptr(MoistureScenarios::getMoistureScenarioDescriptionByIndex(index));
}
