# PomDaisy (iOS): the Nintendo DS core - linux/libretro-ds (DSperate through dsperate-lr, plus the DS companion) - as
# ONE prelinked object (pomdaisy_ds.o) the app links beside the Game Boy core (ios/core).
#
# No JIT: a TestFlight / App Store build can't make memory executable, so DSperate's interpreters run both CPUs
# (DSPERATE_JIT off; its NEON renderer kernels stay on). And one object with only pdds_* visible: the two cores are both
# libretro cores (retro_*) and both build 3ds/core, linux/hud.c and linux/quick_menu.c, each with its own definitions,
# so linked as two plain static libraries their symbols would collide. The prelink keeps every other symbol local.
#
# Also builds on Linux (GNU ld + objcopy do the prelink), where pomdaisy_ds_check can run games through the app's
# bridge with both cores linked in: the only place this can be tried without a Mac.
cmake_minimum_required(VERSION 3.21)
project(pomdaisy_ds LANGUAGES C CXX)
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()

set(ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../..)
set(LR ${ROOT}/third_party/dsperate-lr)
set(DS ${LR}/DSperate)
set(APP ${ROOT}/app/src/main/cpp/nds)

# DSperate picks NEON by CMAKE_SYSTEM_PROCESSOR, which an iOS cross build may leave empty.
if(APPLE AND NOT CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
  set(CMAKE_SYSTEM_PROCESSOR arm64)
endif()
foreach(off DSPERATE_JIT DSPERATE_VULKAN DSPERATE_GLES BUILD_TESTING)
  set(${off} OFF CACHE BOOL "" FORCE)
endforeach()
# An uninitialised C global (pd_dark, ui_game ...) must be a definition, not a common symbol: the prelink can't make
# those local, and the linker would merge them with the Game Boy core's variables of the same name.
add_compile_options(-fno-common)
# The Linux DS core's own build: dsperate_core, the generated companion data and pd_ds_presentation. Its shared
# library target (GNU link options) is never built here.
add_subdirectory(${ROOT}/linux/libretro-ds lrds EXCLUDE_FROM_ALL)

# ---- what clang and Apple's toolchain need (never a change to the submodule: fixed copies in the build directory,
# as app/src/main/cpp/nds/dsperate.cmake does for the NDK) ----
# Replaces [from] with [to] in DSperate's src/core/[rel]; each must be there, so a moved line fails the configure.
# (They travel as a CMake list: no semicolon and no empty string in either.)
function(dsperate_fix rel)
  file(READ "${DS}/src/core/${rel}" text)
  set(pairs ${ARGN})
  list(LENGTH pairs n)
  math(EXPR last "${n} - 1")
  foreach(i RANGE 0 ${last} 2)
    math(EXPR j "${i} + 1")
    list(GET pairs ${i} from)
    list(GET pairs ${j} to)
    string(FIND "${text}" "${from}" at)
    if(at EQUAL -1)
      message(FATAL_ERROR "ios/ds: '${from}' isn't in ${rel} any more - check the fix against the new DSperate")
    endif()
    string(REPLACE "${from}" "${to}" text "${text}")
  endforeach()
  set(out "${CMAKE_CURRENT_BINARY_DIR}/dsperate-fixed/${rel}")
  file(WRITE "${out}.new" "${text}")
  file(COPY_FILE "${out}.new" "${out}" ONLY_IF_DIFFERENT)
  get_target_property(srcs dsperate_core SOURCES)
  list(TRANSFORM srcs REPLACE "^${rel}$" "${out}")
  set_target_properties(dsperate_core PROPERTIES SOURCES "${srcs}")
  # The copy sits elsewhere: quoted includes beside the original still have to be found.
  get_filename_component(dir "${DS}/src/core/${rel}" DIRECTORY)
  set_property(SOURCE "${out}" TARGET_DIRECTORY dsperate_core APPEND PROPERTY INCLUDE_DIRECTORIES "${dir}")
  get_source_file_property(defs "${DS}/src/core/${rel}" TARGET_DIRECTORY dsperate_core COMPILE_DEFINITIONS)
  if(defs)
    set_property(SOURCE "${out}" TARGET_DIRECTORY dsperate_core PROPERTY COMPILE_DEFINITIONS "${defs}")
  endif()
  set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${DS}/src/core/${rel}")
endfunction()

if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
  # clang won't reinterpret_cast a const void* to a function pointer ("casts away qualifiers"); GCC does.
  dsperate_fix(gpu/render3d.cpp "reinterpret_cast<GatherNFn>(sh.gather4)"
    "reinterpret_cast<GatherNFn>(const_cast<void*>(sh.gather4))")
  if(CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
    # clang's arm_neon.h makes the intrinsics macros, where a braced vector's comma splits the arguments.
    dsperate_fix(gpu/kernels_neon.cpp
      "uint32x2_t{c, cs}" "(uint32x2_t{c, cs})" "uint32x2_t{cd, c}" "(uint32x2_t{cd, c})")
  endif()
endif()
if(APPLE)
  # Forced into every DSperate source: Darwin's PAGE_SIZE / PAGE_MASK / PAGE_SHIFT macros against its constants.
  target_compile_options(dsperate_core PRIVATE "SHELL:-include ${CMAKE_CURRENT_SOURCE_DIR}/dsperate_apple.h")
  # The linked-in data files: Mach-O spells the section its own way, has no .previous, and C names carry an underscore.
  dsperate_fix(io/dsi_font.cpp
    ".section .rodata.dsperate_dsi_font,\\\"a\\\",%progbits" ".section __DATA,__const"
    "\\t.globl \" sym \"_begin\\n\" sym \"_begin:\\n\"" "\\t.globl _\" sym \"_begin\\n_\" sym \"_begin:\\n\""
    "\\t.globl \" sym \"_end\\n\" sym \"_end:\\n\"" "\\t.globl _\" sym \"_end\\n_\" sym \"_end:\\n\""
    ".previous" ".text")
  dsperate_fix(cheat/analog_db.cpp
    ".section .rodata.dsperate_analog_db,\\\"a\\\",%progbits" ".section __DATA,__const"
    ".globl dsperate_analog_db_begin\\ndsperate_analog_db_begin:" ".globl _dsperate_analog_db_begin\\n_dsperate_analog_db_begin:"
    ".globl dsperate_analog_db_end\\ndsperate_analog_db_end:" ".globl _dsperate_analog_db_end\\n_dsperate_analog_db_end:"
    ".previous" ".text")
  # DSperate's worker threads (2D lines, 3D bands) name themselves as they start: there they also ask for the emulation
  # thread's priority, or iOS runs them as background work on the slow cores.
  dsperate_fix(host_cores.cpp "(void)name" "(void)name, pthread_set_qos_class_self_np(QOS_CLASS_USER_INTERACTIVE, 0)")
  # No getauxval on Darwin, and every arm64 iPhone has the AES instructions.
  dsperate_fix(crypto/aes.c
    "#include <sys/auxv.h>" "/* no <sys/auxv.h> on Darwin */"
    "(getauxval(AT_HWCAP) & HWCAP_AES_BIT) != 0" "1")
endif()

# ---- the core's entry points under names of their own, then the prelink ----
# (pd_ds_entry.cpp includes linux/libretro-ds/pd_ds_libretro.cpp: one translation unit.)
add_library(pomdaisy_ds_entry STATIC pd_ds_entry.cpp ${APP}/pk_nds_call.cpp)
set_property(SOURCE pd_ds_entry.cpp APPEND PROPERTY OBJECT_DEPENDS ${ROOT}/linux/libretro-ds/pd_ds_libretro.cpp)
target_include_directories(pomdaisy_ds_entry PRIVATE ${LR} ${LR}/include ${DS}/src)
target_link_libraries(pomdaisy_ds_entry PRIVATE pd_ds_presentation dsperate_core)

set(PARTS $<TARGET_FILE:pomdaisy_ds_entry> $<TARGET_FILE:pd_ds_presentation> $<TARGET_FILE:dsperate_core>)
set(OBJ ${CMAKE_CURRENT_BINARY_DIR}/pomdaisy_ds.o)
set(EXPORTS ${CMAKE_CURRENT_SOURCE_DIR}/exports.txt) # the C names, one a line
if(APPLE)
  set(EXPORTS_LD ${CMAKE_CURRENT_BINARY_DIR}/exports.ld.txt) # Mach-O's: with the underscore
  file(STRINGS ${EXPORTS} names)
  list(TRANSFORM names PREPEND "_")
  list(JOIN names "\n" names)
  file(WRITE ${EXPORTS_LD} "${names}\n")
  # ld wants the platform named: the deployment target and the SDK the parts were compiled with.
  set(MIN_IOS ${CMAKE_OSX_DEPLOYMENT_TARGET})
  if(NOT MIN_IOS)
    set(MIN_IOS 16.0)
  endif()
  execute_process(COMMAND xcrun --sdk iphoneos --show-sdk-version OUTPUT_VARIABLE IOS_SDK OUTPUT_STRIP_TRAILING_WHITESPACE)
  if(NOT IOS_SDK)
    set(IOS_SDK ${MIN_IOS})
  endif()
  # ld -r turns everything outside the list into private symbols, and those into local ones.
  add_custom_command(OUTPUT ${OBJ}
    COMMAND xcrun ld -r -arch arm64 -platform_version ios ${MIN_IOS} ${IOS_SDK}
      -exported_symbols_list ${EXPORTS_LD} -all_load ${PARTS} -o ${OBJ}
    DEPENDS pomdaisy_ds_entry pd_ds_presentation dsperate_core ${EXPORTS} VERBATIM)
else()
  add_custom_command(OUTPUT ${OBJ}
    COMMAND ${CMAKE_LINKER} -r --whole-archive ${PARTS} -o ${OBJ}
    # Its symbols local, and its COMDAT groups dissolved: a group the linker had already taken from another core's
    # object would otherwise be dropped from this one, which still calls into it by a local name.
    COMMAND ${CMAKE_OBJCOPY} --keep-global-symbols=${EXPORTS} --remove-section=.group ${OBJ}
    DEPENDS pomdaisy_ds_entry pd_ds_presentation dsperate_core ${EXPORTS} VERBATIM)
endif()
add_custom_target(pomdaisy_ds ALL DEPENDS ${OBJ})

# The app's link, tried here: its bridge with all the cores (-DPD_GB_CORE=<ios/core's libpomdaisy_core.a>). A duplicate
# or missing symbol shows before Xcode links the app; on a host that can run it, it plays games through the bridge:
# `pomdaisy_ds_check <dir> <game> [frames] ...`.
set(PD_GB_CORE "" CACHE FILEPATH "ios/core's libpomdaisy_core.a, for the two-core link check")
if(PD_GB_CORE)
  set(BRIDGE ${ROOT}/ios/App/Sources/Bridge)
  # The melonDS core (ios/melonds's pomdaisy_md.o): the bridge links all three.
  set(PD_MD_CORE "" CACHE FILEPATH "ios/melonds's pomdaisy_md.o, for the link check")
  add_executable(pomdaisy_ds_check ds_check.c ${BRIDGE}/pd_bridge.c ${OBJ} ${PD_MD_CORE})
  set_source_files_properties(${OBJ} PROPERTIES EXTERNAL_OBJECT TRUE GENERATED TRUE)
  add_dependencies(pomdaisy_ds_check pomdaisy_ds)
  set_target_properties(pomdaisy_ds_check PROPERTIES LINKER_LANGUAGE CXX)
  target_include_directories(pomdaisy_ds_check PRIVATE ${BRIDGE} ${ROOT}/third_party/mgba/src/platform/libretro)
  target_link_libraries(pomdaisy_ds_check PRIVATE ${PD_GB_CORE} m z)
  if(NOT APPLE)
    target_link_libraries(pomdaisy_ds_check PRIVATE pthread dl)
  endif()
endif()
