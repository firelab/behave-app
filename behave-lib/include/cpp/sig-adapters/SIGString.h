//------------------------------------------------------------------------------
/*! \file SIGString.cpp
    \author Copyright (C) 2022 by Richard Sheperd, Spatial Informatics Group
*/

#pragma once

#include <string>

class SIGString
{
public:
  static char* str2charptr(std::string str) {
    char* c_str = new char[str.length() + 1];
    std::strcpy(c_str, str.c_str());
    return c_str;
  };
};
