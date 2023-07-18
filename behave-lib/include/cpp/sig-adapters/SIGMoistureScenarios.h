//{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}
// Name: SIGMoistureScenarios.h
// Desc: Main interface for MoistureScenarios CodeBlock
// Author: Kenneth Cheung, Spatial Informatics Group
//{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}{*}

#ifndef _SIGMOISTURESCENARIOS_H_
#define _SIGMOISTURESCENARIOS_H_

#include <string.h>
#include "moistureScenarios.h"
#include "SIGString.h"

class SIGMoistureScenarios : public MoistureScenarios
{
public:

  char* getMoistureScenarioDescriptionByName(const char* name);
  char* getMoistureScenarioNameByIndex(const int index);
  char* getMoistureScenarioDescriptionByIndex(const int index);
};

#endif
