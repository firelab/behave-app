cmake_minimum_required(VERSION 3.5 FATAL_ERROR)

# Optional JS
OPTION(BEHAVE_JS "Enable compiling to .js (WARNING: Emits a large file)" OFF)
OPTION(BEHAVE_WASM "Enable compiling to .wasm" ON)
OPTION(BEHAVE_HTML "Enable compiling to .html" OFF)

# Emscripten Compilation
set(WRAPPER_FILE ${CMAKE_CURRENT_SOURCE_DIR}/include/cpp/emscripten/wrapper.cpp)
set(EMSCRIPTEN_CPP_FILE ${CMAKE_SOURCE_DIR}/include/cpp/emscripten/)
set(GLUE_JS_FILE ${CMAKE_CURRENT_SOURCE_DIR}/include/js/glue.js)
set(BEHAVE_SRC_DIR ${CMAKE_SOURCE_DIR}/behave-mirror/src/behave/)
set(SIG_SRC_DIR ${CMAKE_SOURCE_DIR}/include/cpp/sig-adapters/)
set(BEHAVE_LIB libbehave.a)

set(DWARF_DIR ${CMAKE_SOURCE_DIR})
# NOTE: If you are using ubuntu through windows WSL set the DWARF_DIR to the
# file path to the WSL full path. Here's an example:
# set(DWARF_DIR //wsl%24/Ubuntu/home/kcheung/work/code/behave-polylith/behave-lib)

set(EMCC_ARGS
  -I${BEHAVE_SRC_DIR}
  -I${SIG_SRC_DIR}
  -I${EMSCRIPTEN_SRC_FILE}
  -L${BEHAVE_LIB}
  --post-js ${GLUE_JS_FILE}
  -sNO_DISABLE_EXCEPTION_CATCHING
  -sEXPORTED_FUNCTIONS=["_malloc","_free"]
  -sEXPORTED_RUNTIME_METHODS=["UTF8ToString","allocateUTF8","addFunction"])

set(EMCC_JS_ARGS ${EMCC_ARGS} -s WASM=0)

set(EMCC_WASM_ARGS ${EMCC_ARGS}
  --closure 1
  -g
  -s WASM=1
  -s ALLOW_MEMORY_GROWTH
  -fdebug-prefix-map=${CMAKE_SOURCE_DIR}=${DWARF_DIR})

# Make use of c++11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14")

project(behave LANGUAGES CXX)

include_directories(${CMAKE_SOURCE_DIR}/behave-mirror/src/behave)
include_directories(${CMAKE_SOURCE_DIR}/include/cpp/sig-adapters)
include_directories(${CMAKE_SOURCE_DIR}/include/cpp/emscripten)

SET(SOURCE
    behave-mirror/src/behave/behaveUnits.cpp
    behave-mirror/src/behave/canopy_coefficient_table.cpp
    behave-mirror/src/behave/crown.cpp
    behave-mirror/src/behave/crownInputs.cpp
    behave-mirror/src/behave/fireSize.cpp
    behave-mirror/src/behave/mortality.cpp
    behave-mirror/src/behave/mortality_equation_table.cpp
    behave-mirror/src/behave/mortality_inputs.cpp
    behave-mirror/src/behave/newext.cpp
    behave-mirror/src/behave/palmettoGallberry.cpp
    behave-mirror/src/behave/randfuel.cpp
    behave-mirror/src/behave/randthread.cpp
    behave-mirror/src/behave/safety.cpp
    behave-mirror/src/behave/species_master_table.cpp
    behave-mirror/src/behave/surface.cpp
    behave-mirror/src/behave/surfaceFireReactionIntensity.cpp
    behave-mirror/src/behave/surfaceFuelbedIntermediates.cpp
    behave-mirror/src/behave/surfaceInputs.cpp
    behave-mirror/src/behave/surfaceFire.cpp
    behave-mirror/src/behave/surfaceTwoFuelModels.cpp
    behave-mirror/src/behave/westernAspen.cpp
    behave-mirror/src/behave/windAdjustmentFactor.cpp
    behave-mirror/src/behave/windSpeedUtility.cpp

    include/cpp/sig-adapters/SIGBehaveRun.cpp
    include/cpp/sig-adapters/SIGContain.cpp
    include/cpp/sig-adapters/SIGContainAdapter.cpp
    include/cpp/sig-adapters/SIGContainForce.cpp
    include/cpp/sig-adapters/SIGContainForceAdapter.cpp
    include/cpp/sig-adapters/SIGContainResource.cpp
    include/cpp/sig-adapters/SIGContainSim.cpp
    include/cpp/sig-adapters/SIGCrown.cpp
    include/cpp/sig-adapters/SIGFuelModels.cpp
    include/cpp/sig-adapters/SIGIgnite.cpp
    include/cpp/sig-adapters/SIGIgniteInputs.cpp
    include/cpp/sig-adapters/SIGMortality.cpp
    include/cpp/sig-adapters/SIGSurface.h
    include/cpp/sig-adapters/SIGSpot.cpp
    include/cpp/sig-adapters/SIGSpotInputs.cpp
    include/cpp/sig-adapters/SIGSurface.cpp)

SET(HEADERS
    behave-mirror/src/behave/behaveUnits.h
    behave-mirror/src/behave/canopy_coefficient_table.h
    behave-mirror/src/behave/crown.h
    behave-mirror/src/behave/crownInputs.h
    behave-mirror/src/behave/fireSize.h
    behave-mirror/src/behave/mortality.h
    behave-mirror/src/behave/mortality_equation_table.h
    behave-mirror/src/behave/mortality_inputs.h
    behave-mirror/src/behave/newext.h
    behave-mirror/src/behave/palmettoGallberry.h
    behave-mirror/src/behave/randfuel.h
    behave-mirror/src/behave/randthread.h
    behave-mirror/src/behave/safety.h
    behave-mirror/src/behave/species_master_table.h
    behave-mirror/src/behave/surface.h
    behave-mirror/src/behave/surfaceFireReactionIntensity.h
    behave-mirror/src/behave/surfaceFuelbedIntermediates.h
    behave-mirror/src/behave/surfaceInputs.h
    behave-mirror/src/behave/surfaceFire.h
    behave-mirror/src/behave/surfaceTwoFuelModels.h
    behave-mirror/src/behave/westernAspen.h
    behave-mirror/src/behave/windAdjustmentFactor.h
    behave-mirror/src/behave/windSpeedUtility.h

    include/cpp/sig-adapters/SIGBehaveRun.h
    include/cpp/sig-adapters/SIGDiurnalROS.h
    include/cpp/sig-adapters/SIGContain.h
    include/cpp/sig-adapters/SIGContainAdapter.h
    include/cpp/sig-adapters/SIGContainForce.h
    include/cpp/sig-adapters/SIGContainForceAdapter.h
    include/cpp/sig-adapters/SIGContainResource.h
    include/cpp/sig-adapters/SIGContainSim.h
    include/cpp/sig-adapters/SIGCrown.h
    include/cpp/sig-adapters/SIGFuelModels.cpp
    include/cpp/sig-adapters/SIGIgnite.h
    include/cpp/sig-adapters/SIGIgniteInputs.h
    include/cpp/sig-adapters/SIGMortality.h
    include/cpp/sig-adapters/SIGSpot.h
    include/cpp/sig-adapters/SIGSpotInputs.h
    include/cpp/sig-adapters/SIGSurface.h
    include/cpp/sig-adapters/SIGString.h
    include/cpp/sig-adapters/SIGSurface.h)

source_group("Behave Core Source Files" FILES ${SOURCE})

source_group("Behave Core Header Files" FILES ${HEADERS})

add_library(behave STATIC ${SOURCE} ${HEADERS})

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
        COMMAND emcc ${WRAPPER_FILE} ${EMCC_WASM_ARGS} -o behave-min.js
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
