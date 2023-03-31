//------------------------------------------------------------------------------
/*! \file SIGString.cpp
    \author Copyright (C) 2022 by Richard Sheperd, Spatial Informatics Group
*/

#ifndef _SIGSTRING_H_INCLUDED_
#define _SIGSTRING_H_INCLUDED_

#include <string>

class SIGString
{
public:
  static char* str2charptr(std::string val) {
    char* c_val = new char[val.length() + 1];
    std::strcpy(c_val, val.c_str());
    return c_val;
  };
};

#endif
