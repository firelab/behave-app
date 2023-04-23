//------------------------------------------------------------------------------
/*! \file SIGDiurnalROS.cpp
    \author Copyright (C) 2022 by Richard Sheperd, Spatial Informatics Group
*/

#ifndef _SIGDIURNALROS_H_INCLUDED_
#define _SIGDIURNALROS_H_INCLUDED_

#include <vector>

class SIGDiurnalROS
{
public:
    SIGDiurnalROS() {};
    ~SIGDiurnalROS() {};

    void push(double v) { diurnalROS.push_back(v); };
    double at(int i) const { if (i < diurnalROS.size()) return diurnalROS[i]; };
    double& get(int i) { if (i < diurnalROS.size()) return diurnalROS[i]; };
    double operator [] (int i) const { return at(i); }
    double& operator [] (int i){ return get(i); }

    void swap(int i, double v) { diurnalROS[i] = v; };

    int size( void ) const { return diurnalROS.size(); };

private:
    std::vector<double> diurnalROS;
};

#endif
