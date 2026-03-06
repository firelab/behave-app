cmake_minimum_required(VERSION 3.10 FATAL_ERROR)

# Set Compiler
SET (CMAKE_C_COMPILER "clang")
SET (CMAKE_CXX_COMPILER "clang++")
SET (CMAKE_CXX_STANDARD "14")

# optional test executable
OPTION(TEST_BEHAVE "Enable Testing" OFF)

IF(TEST_BEHAVE)
    ADD_DEFINITIONS(-DTEST_BEHAVE)
ENDIF()

# Optional JS
OPTION(BEHAVE_JS "Enable compiling to .js (WARNING: Emits a large file)" OFF)
OPTION(BEHAVE_WASM "Enable compiling to .wasm" ON)
OPTION(BEHAVE_WASM_OPTIMIZED "Enable optimized .wasm compilation" ON)
OPTION(BEHAVE_HTML "Enable compiling to .html" OFF)

IF(BEHAVE_JS)
    ADD_DEFINITIONS(-DBEHAVE_JS)
ENDIF()

IF(BEHAVE_WASM)
    ADD_DEFINITIONS(-DBEHAVE_WASM)
ENDIF()

IF(BEHAVE_HTML)
    ADD_DEFINITIONS(-DBEHAVE_HTML)
ENDIF()

# Emscripten Compilation
set(WRAPPER_FILE ${CMAKE_CURRENT_SOURCE_DIR}/include/cpp/emscripten/wrapper.cpp)
set(EMSCRIPTEN_CPP_FILE ${CMAKE_SOURCE_DIR}/include/cpp/emscripten/)
set(GLUE_JS_FILE ${CMAKE_CURRENT_SOURCE_DIR}/include/js/glue.js)
set(PRE_JS_FILE ${CMAKE_CURRENT_SOURCE_DIR}/include/js/pre.js)
set(BEHAVE_SRC_DIR ${CMAKE_SOURCE_DIR}/behave-mirror/src/behave/)
set(SIG_SRC_DIR ${CMAKE_SOURCE_DIR}/include/cpp/sig-adapters/)
set(SIG_TEST_DIR ${CMAKE_SOURCE_DIR}/test/cpp/)
set(BEHAVE_LIB libbehave.a)


set(EMCC_ARGS
    -std=c++14
    -I${BEHAVE_SRC_DIR}
    -I${SIG_SRC_DIR}
    -I${EMSCRIPTEN_SRC_FILE}
    -L${BEHAVE_LIB}
    --post-js ${GLUE_JS_FILE}
    -s NO_DISABLE_EXCEPTION_CATCHING
    -s MODULARIZE
    -s ENVIRONMENT=web
    -s EXPORT_NAME="createModule"
    -s EXPORTED_FUNCTIONS=["_malloc","_free"]
    -s EXPORTED_RUNTIME_METHODS=["UTF8ToString","allocateUTF8","addFunction","cwrap","ccall"])

set(EMCC_JS_ARGS ${EMCC_ARGS} -s WASM=0)

set(EMCC_WASM_ARGS ${EMCC_ARGS}
    --closure 1
    -s WASM=1
    -s ALLOW_MEMORY_GROWTH)

IF(BEHAVE_WASM_OPTIMIZED)
  set(EMCC_WASM_ARGS ${EMCC_WASM_ARGS} -O3)
ELSE()
  set(EMCC_WASM_ARGS ${EMCC_WASM_ARGS} -g
      -fdebug-prefix-map=${CMAKE_SOURCE_DIR}=${DWARF_DIR})
  set(DWARF_DIR ${CMAKE_SOURCE_DIR})
  # NOTE: If you are using ubuntu through windows WSL set the DWARF_DIR to the
  # file path to the WSL full path. Here's an example:
  # set(DWARF_DIR //wsl%24/Ubuntu/home/kcheung/work/code/behave-polylith/behave-lib)
ENDIF()

project(behave LANGUAGES CXX)

include_directories(${CMAKE_SOURCE_DIR}/behave-mirror/src/behave)
include_directories(${CMAKE_SOURCE_DIR}/include/cpp/sig-adapters)
include_directories(${CMAKE_SOURCE_DIR}/include/cpp/emscripten)

SET(SOURCE
    behave-mirror/src/behave/behaveRun.cpp
    behave-mirror/src/behave/behaveUnits.cpp
    behave-mirror/src/behave/canopy_coefficient_table.cpp
    behave-mirror/src/behave/Contain.cpp
    behave-mirror/src/behave/ContainAdapter.cpp
    behave-mirror/src/behave/ContainForce.cpp
    behave-mirror/src/behave/ContainForceAdapter.cpp
    behave-mirror/src/behave/ContainResource.cpp
    behave-mirror/src/behave/ContainSim.cpp
    behave-mirror/src/behave/chaparralFuel.cpp
    behave-mirror/src/behave/crown.cpp
    behave-mirror/src/behave/crownInputs.cpp
    behave-mirror/src/behave/fineDeadFuelMoistureTool.cpp
    behave-mirror/src/behave/fireSize.cpp
    behave-mirror/src/behave/fuelModels.cpp
    behave-mirror/src/behave/ignite.cpp
    behave-mirror/src/behave/igniteInputs.cpp
    behave-mirror/src/behave/mortality.cpp
    behave-mirror/src/behave/mortality_equation_table.cpp
    behave-mirror/src/behave/mortality_inputs.cpp
    behave-mirror/src/behave/moistureScenarios.cpp
    behave-mirror/src/behave/newext.cpp
    behave-mirror/src/behave/palmettoGallberry.cpp
    behave-mirror/src/behave/randfuel.cpp
    behave-mirror/src/behave/randthread.cpp
    behave-mirror/src/behave/relativeHumidity.cpp
    behave-mirror/src/behave/safety.cpp
    behave-mirror/src/behave/safeSeparationDistanceCalculator.cpp
    behave-mirror/src/behave/slopeTool.cpp
    behave-mirror/src/behave/species_master_table.cpp
    behave-mirror/src/behave/spot.cpp
    behave-mirror/src/behave/spotInputs.cpp
    behave-mirror/src/behave/surface.cpp
    behave-mirror/src/behave/surfaceFireReactionIntensity.cpp
    behave-mirror/src/behave/surfaceFuelbedIntermediates.cpp
    behave-mirror/src/behave/surfaceInputs.cpp
    behave-mirror/src/behave/surfaceFire.cpp
    behave-mirror/src/behave/surfaceTwoFuelModels.cpp
    behave-mirror/src/behave/vaporPressureDeficitCalculator.cpp
    behave-mirror/src/behave/westernAspen.cpp
    behave-mirror/src/behave/windAdjustmentFactor.cpp
    behave-mirror/src/behave/windSpeedUtility.cpp
    behave-mirror/src/behave/CrownFirebrandProcessor.cpp

    include/cpp/sig-adapters/SIGBehaveRun.cpp
    include/cpp/sig-adapters/SIGContainAdapter.cpp
    include/cpp/sig-adapters/SIGCrown.cpp
    include/cpp/sig-adapters/SIGFineDeadFuelMoistureTool.cpp
    include/cpp/sig-adapters/SIGFuelModels.cpp
    include/cpp/sig-adapters/SIGIgnite.cpp
    include/cpp/sig-adapters/SIGMoistureScenarios.cpp
    include/cpp/sig-adapters/SIGMortality.cpp
    include/cpp/sig-adapters/SIGSlopeTool.cpp
    include/cpp/sig-adapters/SIGSpot.cpp
    include/cpp/sig-adapters/SIGSurface.cpp)

SET(HEADERS
    behave-mirror/src/behave/behaveRun.h
    behave-mirror/src/behave/behaveUnits.h
    behave-mirror/src/behave/canopy_coefficient_table.h
    behave-mirror/src/behave/Contain.h
    behave-mirror/src/behave/ContainAdapter.h
    behave-mirror/src/behave/ContainForce.h
    behave-mirror/src/behave/ContainForceAdapter.h
    behave-mirror/src/behave/ContainResource.h
    behave-mirror/src/behave/ContainSim.h
    behave-mirror/src/behave/chaparralFuel.h
    behave-mirror/src/behave/crown.h
    behave-mirror/src/behave/crownInputs.h
    behave-mirror/src/behave/fineDeadFuelMoistureTool.h
    behave-mirror/src/behave/fireSize.h
    behave-mirror/src/behave/fuelModels.h
    behave-mirror/src/behave/ignite.h
    behave-mirror/src/behave/igniteInputs.h
    behave-mirror/src/behave/mortality.h
    behave-mirror/src/behave/mortality_equation_table.h
    behave-mirror/src/behave/mortality_inputs.h
    behave-mirror/src/behave/moistureScenarios.h
    behave-mirror/src/behave/newext.h
    behave-mirror/src/behave/palmettoGallberry.h
    behave-mirror/src/behave/randfuel.h
    behave-mirror/src/behave/randthread.h
    behave-mirror/src/behave/relativeHumidity.h
    behave-mirror/src/behave/safety.h
    behave-mirror/src/behave/safeSeparationDistanceCalculator.h
    behave-mirror/src/behave/slopeTool.h
    behave-mirror/src/behave/species_master_table.h
    behave-mirror/src/behave/spot.h
    behave-mirror/src/behave/spotInputs.h
    behave-mirror/src/behave/surface.h
    behave-mirror/src/behave/surfaceFireReactionIntensity.h
    behave-mirror/src/behave/surfaceFuelbedIntermediates.h
    behave-mirror/src/behave/surfaceInputs.h
    behave-mirror/src/behave/surfaceFire.h
    behave-mirror/src/behave/surfaceTwoFuelModels.h
    behave-mirror/src/behave/westernAspen.h
    behave-mirror/src/behave/windAdjustmentFactor.h
    behave-mirror/src/behave/windSpeedUtility.h
    behave-mirror/src/behave/vaporPressureDeficitCalculator.h
    behave-mirror/src/behave/CrownFirebrandProcessor.h

    include/cpp/sig-adapters/SIGBehaveRun.h
    include/cpp/sig-adapters/SIGContainAdapter.h
    include/cpp/sig-adapters/SIGCrown.h
    include/cpp/sig-adapters/SIGDiurnalROS.h
    include/cpp/sig-adapters/SIGFineDeadFuelMoistureTool.h
    include/cpp/sig-adapters/SIGFuelModels.cpp
    include/cpp/sig-adapters/SIGIgnite.h
    include/cpp/sig-adapters/SIGMoistureScenarios.h
    include/cpp/sig-adapters/SIGMortality.h
    include/cpp/sig-adapters/SIGSlopeTool.h
    include/cpp/sig-adapters/SIGSpot.h
    include/cpp/sig-adapters/SIGString.h
    include/cpp/sig-adapters/SIGSurface.h)

source_group("Behave Core Source Files" FILES ${SOURCE})

source_group("Behave Core Header Files" FILES ${HEADERS})

add_library(behave STATIC ${SOURCE} ${HEADERS})

IF(TEST_BEHAVE)

    add_library(testUtils STATIC test/cpp/testUtils.cpp test/cpp/testUtils.h)

    SET(BEHAVE_TEST_SOURCE
        behave-mirror/src/testBehave/testBehave.cpp)
    ADD_COMPILE_DEFINITIONS(NDEBUG=FALSE)
    ADD_EXECUTABLE(testBehave
        ${SOURCE}
        ${BEHAVE_TEST_SOURCE}
        ${HEADERS})
    TARGET_LINK_LIBRARIES(testBehave)

    SET(SURFACE_TEST_SOURCE
        test/cpp/testSurface.cpp)
    ADD_EXECUTABLE(testSurface
        ${SOURCE}
        ${SURFACE_TEST_SOURCE}
        ${HEADERS})
    TARGET_LINK_LIBRARIES(testSurface)

    SET(CONTAIN_TEST_SOURCE
        test/cpp/testContain.cpp)
    ADD_EXECUTABLE(testContain
        ${SOURCE}
        ${CONTAIN_TEST_SOURCE}
        ${HEADERS})
    TARGET_LINK_LIBRARIES(testContain)

    SET(CROWN_TEST_SOURCE
        test/cpp/testCrown.cpp)
    ADD_EXECUTABLE(testCrown
        ${SOURCE}
        ${CROWN_TEST_SOURCE}
        ${HEADERS})
    TARGET_LINK_LIBRARIES(testCrown testUtils)
ENDIF()

IF(BEHAVE_JS)
    add_custom_command(
        OUTPUT behave.js
        COMMAND emcc ${WRAPPER_FILE} ${EMCC_JS_ARGS} -o behave.js
        DEPENDS behave
        COMMENT "Building javascript"
        VERBATIM)
    add_custom_target(behave-javascript ALL DEPENDS behave.js)
ENDIF()

IF(BEHAVE_WASM)
    add_custom_command(
        OUTPUT behave.wasm
        COMMAND emcc ${WRAPPER_FILE} ${EMCC_WASM_ARGS} -o behave-min.js --pre-js ${PRE_JS_FILE}
        DEPENDS behave
        COMMENT "Building webassembly"
        VERBATIM)
    add_custom_target(behave-webassembly ALL DEPENDS behave.wasm)
ENDIF()

IF(BEHAVE_HTML)
    add_custom_command(
        OUTPUT behave.html
        COMMAND emcc ${WRAPPER_FILE} ${EMCC_WASM_ARGS} -o behave.html
        DEPENDS behave
        COMMENT "Building html"
        VERBATIM)
    add_custom_target(behave-html ALL DEPENDS behave.html)
ENDIF()
