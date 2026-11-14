# PomDaisy (iOS): melonDS - the Android app's Nintendo DS core (app/src/main/cpp/nds: melonDS 1.1 with our patch,
# EXPAND, the field reads) - as ONE prelinked object (pomdaisy_md.o) the app links beside the Game Boy core (ios/core)
# and the DSperate one (ios/ds). Only its pdmd_* entry points stay visible (exports.txt), so nothing in it meets the
# other cores' symbols.
#
# No JIT (TestFlight / the App Store can't run one) and no GPU renderer (DS RESOLUTION needs OpenGL ES 3.2, which iOS
# doesn't have): melonDS's interpreter and software renderer, with EXPAND's slices on helper threads.
#
# Also builds on Linux (GNU ld + objcopy do the prelink): ios/ds's pomdaisy_ds_check then plays games on it through
# the app's bridge (-DPD_MD_CORE=<this build's pomdaisy_md.o>).
cmake_minimum_required(VERSION 3.21)
project(pomdaisy_md LANGUAGES C CXX)
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()
# An uninitialised global must be a definition (not a common symbol) for the prelink to keep it to itself.
add_compile_options(-fno-common)

set(ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../..)
set(NDS ${ROOT}/app/src/main/cpp/nds)
set(MELONDS_DIR ${ROOT}/third_party/melonds)
if(NOT EXISTS ${MELONDS_DIR}/src/NDS.h)
  message(FATAL_ERROR "Initialize the pinned core: git submodule update --init third_party/melonds")
endif()
if(APPLE AND NOT CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
  set(CMAKE_SYSTEM_PROCESSOR arm64)
endif()

# melonDS's core library alone, as native-capture/nds_dump builds it.
foreach(off ENABLE_JIT ENABLE_OGLRENDERER ENABLE_GDBSTUB BUILD_QT_SDL ENABLE_LTO_RELEASE ENABLE_LTO)
  set(${off} OFF CACHE BOOL "" FORCE)
endforeach()
include(${MELONDS_DIR}/../patches/apply_melonds_patch.cmake)
add_subdirectory(${MELONDS_DIR} melonds EXCLUDE_FROM_ALL)

add_library(pomdaisy_md_entry STATIC pd_md_entry.cpp pk_nds_gl_none.cpp
  ${NDS}/pk_nds.cpp ${NDS}/pk_nds_platform.cpp ${NDS}/pk_nds_field.cpp ${NDS}/pk_nds_field_gpu.cpp
  ${NDS}/pk_nds_call.cpp ${NDS}/pk_nds_call_melonds.cpp ${NDS}/pk_nds_wide.cpp ${NDS}/pk_nds_music.cpp)
# shim/: the Android log header pk_nds.cpp includes.
target_include_directories(pomdaisy_md_entry PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/shim ${NDS}
  ${ROOT}/third_party/mgba/src/platform/libretro)
target_link_libraries(pomdaisy_md_entry PRIVATE core)

# Every static library melonDS's core target pulls in (teakra, fatfs ...), for the prelink.
set(PARTS $<TARGET_FILE:pomdaisy_md_entry> $<TARGET_FILE:core>)
foreach(dep teakra fatfs)
  if(TARGET ${dep})
    list(APPEND PARTS $<TARGET_FILE:${dep}>)
    set(DEPS ${DEPS} ${dep})
  endif()
endforeach()
set(OBJ ${CMAKE_CURRENT_BINARY_DIR}/pomdaisy_md.o)
set(EXPORTS ${CMAKE_CURRENT_SOURCE_DIR}/exports.txt)
if(APPLE)
  set(EXPORTS_LD ${CMAKE_CURRENT_BINARY_DIR}/exports.ld.txt)
  file(STRINGS ${EXPORTS} names)
  list(TRANSFORM names PREPEND "_")
  list(JOIN names "\n" names)
  file(WRITE ${EXPORTS_LD} "${names}\n")
  set(MIN_IOS ${CMAKE_OSX_DEPLOYMENT_TARGET})
  if(NOT MIN_IOS)
    set(MIN_IOS 16.0)
  endif()
  # The device or the simulator (CMAKE_OSX_SYSROOT=iphonesimulator: CI's simulator test).
  if(CMAKE_OSX_SYSROOT MATCHES "[Ss]imulator")
    set(IOS_SDK_NAME iphonesimulator)
    set(IOS_PLATFORM ios-simulator)
  else()
    set(IOS_SDK_NAME iphoneos)
    set(IOS_PLATFORM ios)
  endif()
  execute_process(COMMAND xcrun --sdk ${IOS_SDK_NAME} --show-sdk-version OUTPUT_VARIABLE IOS_SDK OUTPUT_STRIP_TRAILING_WHITESPACE)
  if(NOT IOS_SDK)
    set(IOS_SDK ${MIN_IOS})
  endif()
  add_custom_command(OUTPUT ${OBJ}
    COMMAND xcrun ld -r -arch arm64 -platform_version ${IOS_PLATFORM} ${MIN_IOS} ${IOS_SDK}
      -exported_symbols_list ${EXPORTS_LD} -all_load ${PARTS} -o ${OBJ}
    DEPENDS pomdaisy_md_entry core ${DEPS} ${EXPORTS} VERBATIM)
else()
  add_custom_command(OUTPUT ${OBJ}
    COMMAND ${CMAKE_LINKER} -r --whole-archive ${PARTS} -o ${OBJ}
    # Its symbols local, and its COMDAT groups dissolved: a group the linker had already taken from another core's
    # object would otherwise be dropped from this one, which still calls into it by a local name.
    COMMAND ${CMAKE_OBJCOPY} --keep-global-symbols=${EXPORTS} --remove-section=.group ${OBJ}
    DEPENDS pomdaisy_md_entry core ${DEPS} ${EXPORTS} VERBATIM)
endif()
add_custom_target(pomdaisy_md ALL DEPENDS ${OBJ})
