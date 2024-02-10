#include <iostream>
#include <sstream>
#include <functional>
#include <variant>
#include <fstream>
#include <type_traits>
#include <typeinfo>
#include <typeindex>
#include <nlohmann/json.hpp>
#include "behaveUnits.cpp"






// Helper struct to store the number of arguments as a non-type template parameter
template <typename Func>
struct Arity;

// Specialization of Arity for functions with non-void return type
template <typename R, typename... Args>
struct Arity<R(Args...)> {
    static constexpr std::size_t value = sizeof...(Args);
};

// Specialization of Arity for functions with void return type
template <typename... Args>
struct Arity<void(Args...)> {
    static constexpr std::size_t value = sizeof...(Args);
};

// Helper function to simplify the usage of getArity
template <typename Func>
constexpr std::size_t getArity(const Func&) {
    return Arity<Func>::value;
}

int main() {
  getArity(&SIGContainAdapter::get)


}

#include <iostream>
#include <sstream>
#include <functional>
#include <variant>
#include <fstream>
#include <type_traits>
#include <typeinfo>
#include <typeindex>
#include <nlohmann/json.hpp>
#include "behaveUnits.cpp"






class UnitResolver {
private:
  std::unordered_map<std::string, int> units_;
public:
  UnitResolver() {
    addUnit(AreaUnits::Acres, "ac");
    addUnit(AreaUnits::Hectares, "ha");
    addUnit(AreaUnits::SquareFeet, "ft2");
    addUnit(AreaUnits::SquareMeters, "m2");

    addUnit(DensityUnits::KilogramsPerCubicMeter, "kg/m3");
    addUnit(DensityUnits::PoundsPerCubicFoot, "lb/ft3");
    addUnit(DensityUnits::PoundsPerCubicFoot, "lbs/ft3");

    addUnit(FirelineIntensityUnits::BtusPerFootPerMinute, "Btu/ft/min");
    addUnit(FirelineIntensityUnits::BtusPerFootPerSecond, "Btu/ft/s");
    addUnit(FirelineIntensityUnits::KilowattsPerMeter, "kW/m");

    addUnit(FractionUnits::Fraction, "fraction");
    addUnit(FractionUnits::Percent, "%");

    addUnit(HeatOfCombustionUnits::BtusPerPound, "Btu/lb");
    addUnit(HeatOfCombustionUnits::KilojoulesPerKilogram, "kJ/kg");

    addUnit(HeatPerUnitAreaUnits::BtusPerSquareFoot, "Btu/ft2");
    addUnit(HeatPerUnitAreaUnits::KilojoulesPerSquareMeter, "kJ/m2");

    addUnit(HeatSinkUnits::BtusPerCubicFoot, "Btu/ft3");
    addUnit(HeatSinkUnits::KilojoulesPerCubicMeter, "kJ/m3");

    addUnit(HeatSourceAndReactionIntensityUnits::BtusPerSquareFootPerMinute, "Btu/ft2/min");
    addUnit(HeatSourceAndReactionIntensityUnits::BtusPerSquareFootPerSecond, "Btu/ft2/sec");
    addUnit(HeatSourceAndReactionIntensityUnits::KilowattsPerSquareMeter, "kW/m2");

    addUnit(LengthUnits::Centimeters, "cm");
    addUnit(LengthUnits::Chains, "ch");
    addUnit(LengthUnits::Feet, "ft");
    addUnit(LengthUnits::Inches, "in");
    addUnit(LengthUnits::Kilometers, "km");
    addUnit(LengthUnits::Meters, "m");
    addUnit(LengthUnits::Miles, "mi");
    addUnit(LengthUnits::Millimeters, "mm");

    addUnit(LoadingUnits::TonnesPerHectare, "tonne/ha");
    addUnit(LoadingUnits::TonsPerAcre, "ton/ac");

    addUnit(SlopeUnits::Degrees, "deg");

    addUnit(SpeedUnits::ChainsPerHour, "ch/h");
    addUnit(SpeedUnits::FeetPerMinute, "ft/min");
    addUnit(SpeedUnits::KilometersPerHour, "km/h");
    addUnit(SpeedUnits::MetersPerHour, "m/h"); // FIXME
    addUnit(SpeedUnits::MetersPerMinute, "m/min");
    addUnit(SpeedUnits::MilesPerHour, "mi/h");

    addUnit(SurfaceAreaToVolumeUnits::SquareFeetOverCubicFeet, "ft2/ft3");
    addUnit(SurfaceAreaToVolumeUnits::SquareMetersOverCubicMeters, "m2/m3");

    addUnit(TemperatureUnits::Celsius, "oC");
    addUnit(TemperatureUnits::Fahrenheit, "oF");

    addUnit(TimeUnits::Days, "days");
    addUnit(TimeUnits::Hours, "h");
    addUnit(TimeUnits::Minutes, "min");
    addUnit(TimeUnits::Seconds, "s");
    addUnit(TimeUnits::Years, "years");
  }

  void addUnit(int value, const std::string& unit) {
    units_[unit] = value;
  }

  template<typename Enum>
  Enum resolveUnit(const std::string& unit) {
    return static_cast<Enum>(units_[unit]);
  }
};

class FuncConverter {
private:
  UnitResolver _unitResolver;
public:
  FuncConverter(UnitResolver &unitResolver) : _unitResolver(unitResolver) {}

  // Discrete Setter
  template<typename ObjType, typename EnumType>
  void wrapperDiscreteSetter(ObjType* obj, void (ObjType::*func)(EnumType), std::string str) {
    // Convert the input string to the desired EnumType
    EnumType enumValue = static_cast<EnumType>(std::stoi(str));

    // Call the member function of the object with the converted EnumType
    (obj->*func)(enumValue);
  }

  template<typename ObjType, typename EnumType>
  std::function<void(void*, std::string)> convertDiscreteSetter(void (ObjType::*func)(EnumType)) {
    return [=](void* obj, std::string str) {
      wrapperDiscreteSetter(static_cast<ObjType*>(obj), func, str);
    };
  }

  // Continous Setter
  template<typename ObjType, typename EnumType>
  void wrapperContinuousSetter(ObjType* obj, void (ObjType::*func)(double, EnumType), double num, std::string str) {
    // Convert the input string to the desired EnumType
    EnumType enumValue = _unitResolver.resolveUnit<EnumType>(str);

    // Call the member function of the object with the converted EnumType
    (obj->*func)(num, enumValue);
  }

  template<typename ObjType, typename EnumType>
  std::function<void(void*, double, std::string)> convertContinuousSetter(void (ObjType::*func)(double, EnumType)) {
    return [=](void* obj, double num, std::string str) {
      wrapperContinuousSetter(static_cast<ObjType*>(obj), func, num, str);
    };
  }

  // Calculate Wrapper
  template<typename ObjType>
  std::function<void(void*)> convertCalculate(void (ObjType::*func)()) {
    return [=](void* obj, std::string str) {
      (obj->*func)();
    };
  }

  // Getters
  template<typename ObjType, typename EnumType>
  double wrapperGetter(ObjType* obj, double (ObjType::*func)(EnumType), std::string str) {
    // Convert the input string to the desired EnumType
    EnumType enumValue = _unitResolver.resolveUnit<EnumType>(str);

    // Call the member function of the object with the converted EnumType
    return (obj->*func)(enumValue);
  }

  template<typename ObjType, typename EnumType>
  std::function<double(void*, std::string)> convertGetter(double (ObjType::*func)(EnumType)) {
    return [=](void* obj, std::string str) {
      return wrapperGetter(static_cast<ObjType*>(obj), func, str);
    };
  }
};

class SIGTestClass {
public:
  void setHeight(double height, LengthUnits::LengthUnitsEnum units) {
    height_ = LengthUnits::toBaseUnits(height, units);
  };

  double getHeight(LengthUnits::LengthUnitsEnum units) {
    return LengthUnits::fromBaseUnits(height_, units);
  };

  void setSpeed(double speed, SpeedUnits::SpeedUnitsEnum units) {
    speed_ = SpeedUnits::toBaseUnits(speed, units);
  };

  double getSpeed(SpeedUnits::SpeedUnitsEnum units) {
    return SpeedUnits::fromBaseUnits(speed_, units);
  };

  void setUnits(LengthUnits::LengthUnitsEnum units) {
    units_ = units;
  }

  LengthUnits::LengthUnitsEnum getUnits() {
    return units_;
  }

private:
  double height_;
  double speed_;
  LengthUnits::LengthUnitsEnum units_;
};

int main() {
  std::unordered_map<std::string, std::function<void(void*, std::string)>> discrete_setters;
  std::unordered_map<std::string, std::function<void(void*, double, std::string)>> cont_setters;
  std::unordered_map<std::string, std::function<double(void*, std::string)>> getters;

  UnitResolver unitResolver;

  FuncConverter fnConverter(unitResolver);

  SIGTestClass sut;

  // sut.setSpeed(30.0, SpeedUnits::MilesPerHour);
  cont_setters["setHeight"] = fnConverter.convertContinuousSetter(&SIGTestClass::setHeight);
  cont_setters["setHeight"](&sut, 10.0, "mi");

  cont_setters["setSpeed"] = fnConverter.convertContinuousSetter(&SIGTestClass::setSpeed);
  cont_setters["setSpeed"](&sut, 30.0, "mi/h");

  discrete_setters["setUnits"] = fnConverter.convertDiscreteSetter(&SIGTestClass::setUnits);
  discrete_setters["setUnits"](&sut, "3");

  getters["getSpeed"] = fnConverter.convertGetter(&SIGTestClass::getSpeed);
  std::cout << getters["getSpeed"](&sut, "ft/min") << std::endl;
  std::cout << sut.getSpeed(SpeedUnits::FeetPerMinute) << std::endl;

  getters["getHeight"] = fnConverter.convertGetter(&SIGTestClass::getHeight);
  std::cout << getters["getHeight"](&sut, "ft") << std::endl;
  std::cout << sut.getHeight(LengthUnits::Feet) << std::endl;

  std::cout << sut.getUnits() << std::endl;

  return 0;
}
