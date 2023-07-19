//------------------------------------------------------------------------------
/*! \file SIGCollections.h
    \author Copyright (C) 2022 by Richard Sheperd, Spatial Informatics Group
*/

#pragma once

#include <iterator>
#include <vector>
#include "species_master_table.h"

// SimpleVectors
template <typename T>
struct SimpleVector {
  std::vector<T> vec;

  SimpleVector() {}
  SimpleVector(size_t size) : vec(size) {}
  SimpleVector(vector<T> other_vec) : vec(other_vec) {}
  SimpleVector(T* other_array, int size) {
    for (int i = 0; i < size; i++)
      vec.push_back(other_array[i]);
  }

  void resize(size_t size) {
    vec.resize(size);
  }

  T get(size_t i) const {
    return vec.at(i);
  }

  void set(size_t i, T val) {
    vec.at(i) = val;
  }

  size_t size() const {
    return vec.size();
  }
};

typedef SimpleVector<bool> BoolVector;
typedef SimpleVector<char> CharVector;
typedef SimpleVector<int> IntVector;
typedef SimpleVector<double> DoubleVector;

// ClassVectors
template <typename T>
struct ClassVector {
  std::vector<T> vec;

  ClassVector() {}
  ClassVector(size_t size) : vec(size) {}
  ClassVector(const vector<T> other_vec) : vec(other_vec) {}

  void resize(size_t size) {
    vec.resize(size);
  }

  T* get(size_t i) {
    return &vec.at(i);
  }

  void set(size_t i, T* val) {
    vec.at(i) = *val;
  }

  size_t size() const {
    return vec.size();
  }
};

typedef ClassVector<SpeciesMasterTableRecord> SpeciesMasterTableRecordVector;
