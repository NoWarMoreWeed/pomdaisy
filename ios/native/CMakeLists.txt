# PomDaisy (iOS): the Android app's native library (app/src/main/cpp: libmgba with our layer, melonDS and DSperate for the
# DS, rcheevos) built for iPhone / iPad with its JNI glue unchanged, over a JNI of our own (include/jni.h, pd_jni.c), and
# prelinked into ONE object (pomdaisy_android.o) whose only visible symbols are the glue's Java_* functions and pd_jni_*.
# The app's Kotlin (ios/compose) calls them through cinterop, exactly as Android's calls them through JNI.
#
# No JIT anywhere (melonDS, DSperate): TestFlight / the App Store can't run one. No GPU renderer (DS RESOLUTION).
# Also builds on Linux (GNU ld + objcopy do the prelink), where pd_native_check runs a game through the glue.
cmake_minimum_required(VERSION 3.22)
project(pomdaisy_android C CXX)
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()
add_compile_options(-fno-common)
if(APPLE AND NOT CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
  set(CMAKE_SYSTEM_PROCESSOR arm64)
endif()

set(ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../..)
set(CPP ${ROOT}/app/src/main/cpp)
set(FAKE ${CMAKE_CURRENT_SOURCE_DIR}/include)

# ---- mGBA, as the Android build configures it ----
set(MGBA_DIR ${ROOT}/third_party/mgba)
foreach(on BUILD_STATIC DISABLE_FRONTENDS DISABLE_DEPS M_CORE_GBA M_CORE_GB SKIP_INSTALL_ALL)
  set(${on} ON CACHE BOOL "" FORCE)
endforeach()
foreach(off BUILD_SHARED SKIP_LIBRARY BUILD_GL BUILD_GLES2 BUILD_GLES3 BUILD_PERF BUILD_TEST BUILD_SUITE BUILD_LIBRETRO
    BUILD_PYTHON USE_DEBUGGERS USE_GDB_STUB USE_EDITLINE USE_FFMPEG USE_DISCORD_RPC USE_LZMA USE_LUA USE_ELF USE_SQLITE3
    USE_LIBZIP USE_MINIZIP USE_PNG USE_ZLIB BUILD_LTO BUILD_QT BUILD_SDL USE_EPOXY USE_FREETYPE)
  set(${off} OFF CACHE BOOL "" FORCE)
endforeach()
add_subdirectory(${MGBA_DIR} mgba EXCLUDE_FROM_ALL)
# mGBA's log categories are tentative definitions: as definitions (not common symbols) the prelink's export list hides them.
target_compile_options(mgba PRIVATE -fno-common)

# ---- rcheevos ----
set(RC ${ROOT}/third_party/rcheevos)
file(GLOB RC_SRC ${RC}/src/*.c ${RC}/src/rapi/*.c ${RC}/src/rcheevos/*.c ${RC}/src/rhash/hash.c ${RC}/src/rhash/hash_rom.c ${RC}/src/rhash/md5.c)
list(FILTER RC_SRC EXCLUDE REGEX "rc_libretro\\.c$|rc_client_external\\.c$|rc_client_raintegration\\.c$")
add_library(rcheevos STATIC ${RC_SRC})
target_include_directories(rcheevos PUBLIC ${RC}/include)
target_compile_definitions(rcheevos PUBLIC RC_CLIENT_SUPPORTS_HASH RC_HASH_NO_DISC RC_HASH_NO_ENCRYPTED RC_HASH_NO_ZIP)

# ---- melonDS: no JIT, no OpenGL ----
set(MELONDS_DIR ${ROOT}/third_party/melonds)
foreach(off BUILD_QT_SDL ENABLE_OGLRENDERER ENABLE_GDBSTUB ENABLE_LTO_RELEASE ENABLE_LTO ENABLE_JIT)
  set(${off} OFF CACHE BOOL "" FORCE)
endforeach()
include(${MELONDS_DIR}/../patches/apply_melonds_patch.cmake)
add_subdirectory(${MELONDS_DIR} melonds EXCLUDE_FROM_ALL)

# ---- DSperate (third_party/dsperate, as the Android app builds it): its core library, no JIT ----
include(${CMAKE_CURRENT_SOURCE_DIR}/dsperate.cmake)

# ---- the app's native code ----
add_library(pomdaisy_android_glue STATIC
  ${CPP}/pokedaisy_jni.c ${CPP}/pokedaisy_ra.c ${CPP}/pk_call.c ${CPP}/pk_host.c ${CPP}/pk_cheats.c ${CPP}/pk_coop.c
  ${CPP}/pk_coop_field.c ${CPP}/pk_coop_after.c ${CPP}/pk_coop_lock.c ${CPP}/pk_coop_link.c ${CPP}/pk_coop_link_start.c
  ${CPP}/pk_coop_link_wild.c ${CPP}/pk_coop_link_story.c ${CPP}/pk_coop_link_drop.c ${CPP}/pk_coop_link_rewards.c
  ${CPP}/pk_coop_link_end.c ${CPP}/pk_coop_lock_jni.c ${CPP}/pk_link.c ${CPP}/pk_link_jni.c ${CPP}/pk_rewind.c
  ${CPP}/pk_savevf.c ${CPP}/pk_wide.c ${CPP}/pk_wide_gb.c ${CPP}/pk_flight.c ${CPP}/pk_gb_field.c ${CPP}/pk_gb_audio.c
  ${CPP}/nds/pk_nds_core.cpp ${CPP}/nds/pk_nds_dsp.cpp ${CPP}/nds/pk_nds.cpp ${CPP}/nds/pk_nds_platform.cpp
  ${CPP}/nds/pk_nds_field.cpp ${CPP}/nds/pk_nds_field_gpu.cpp ${CPP}/nds/pk_nds_call.cpp ${CPP}/nds/pk_nds_call_melonds.cpp
  ${CPP}/nds/pk_nds_wide.cpp ${CPP}/nds/pk_nds_music.cpp ${ROOT}/ios/melonds/pk_nds_gl_none.cpp
  pd_jni.c)
target_compile_definitions(pomdaisy_android_glue PRIVATE USE_PTHREADS M_CORE_GBA M_CORE_GB PD_IOS=1)
if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64")
  target_compile_definitions(pomdaisy_android_glue PRIVATE ARCHITECTURE_ARM64=1)
endif()
# include/ first: its jni.h and android/*.h stand in for the NDK's.
target_include_directories(pomdaisy_android_glue PRIVATE ${FAKE} ${CMAKE_CURRENT_SOURCE_DIR} ${MGBA_DIR}/include ${MGBA_DIR}/src
  ${CMAKE_CURRENT_BINARY_DIR}/mgba/include ${CPP} ${CPP}/nds)
target_link_libraries(pomdaisy_android_glue PRIVATE mgba rcheevos core dsperate_core)
# Darwin's PAGE_SIZE / PAGE_SHIFT macros undefined before DSperate's headers (pk_nds_dsp.cpp includes them).
set_source_files_properties(${CPP}/nds/pk_nds_dsp.cpp PROPERTIES COMPILE_OPTIONS "-include;${CMAKE_CURRENT_SOURCE_DIR}/dsperate_host.h")

# ---- the prelink: one object, only the glue's entry points visible ----
# The glue whole, the libraries as archives (members pulled in as needed: melonDS and mGBA both carry a blip_buf, and
# the first one found serves both, as in the Android app's link).
set(GLUE $<TARGET_FILE:pomdaisy_android_glue>)
set(PARTS $<TARGET_FILE:mgba> $<TARGET_FILE:rcheevos> $<TARGET_FILE:core> $<TARGET_FILE:dsperate_core>)
set(DEPS pomdaisy_android_glue mgba rcheevos core dsperate_core)
foreach(dep teakra fatfs)
  if(TARGET ${dep})
    list(APPEND PARTS $<TARGET_FILE:${dep}>)
    list(APPEND DEPS ${dep})
  endif()
endforeach()
set(OBJ ${CMAKE_CURRENT_BINARY_DIR}/pomdaisy_android.o)
# The visible names: every Java_* the glue defines and pd_jni_*, listed after the archives are built.
set(SYMS ${CMAKE_CURRENT_BINARY_DIR}/exports.txt)
if(APPLE)
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
    COMMAND sh -c "nm -gU $<TARGET_FILE:pomdaisy_android_glue> | awk '{print $NF}' | grep -E '^_(Java_|pd_jni_)' | sort -u > ${SYMS}"
    COMMAND xcrun ld -r -arch arm64 -platform_version ${IOS_PLATFORM} ${MIN_IOS} ${IOS_SDK} -exported_symbols_list ${SYMS} -force_load ${GLUE} ${PARTS} -o ${OBJ}
    DEPENDS ${DEPS} VERBATIM)
else()
  add_custom_command(OUTPUT ${OBJ}
    COMMAND sh -c "nm -g --defined-only $<TARGET_FILE:pomdaisy_android_glue> | awk '{print $NF}' | grep -E '^(Java_|pd_jni_)' | sort -u > ${SYMS}"
    COMMAND ${CMAKE_LINKER} -r --whole-archive ${GLUE} --no-whole-archive --start-group ${PARTS} --end-group -o ${OBJ}
    COMMAND ${CMAKE_OBJCOPY} --keep-global-symbols=${SYMS} --remove-section=.group ${OBJ}
    DEPENDS ${DEPS} VERBATIM)
endif()
add_custom_target(pomdaisy_android ALL DEPENDS ${OBJ})

# A run through the glue on a host that can (Linux): pd_native_check <rom> [frames]
add_executable(pd_native_check native_check.c ${OBJ})
set_source_files_properties(${OBJ} PROPERTIES EXTERNAL_OBJECT TRUE GENERATED TRUE)
add_dependencies(pd_native_check pomdaisy_android)
set_target_properties(pd_native_check PROPERTIES LINKER_LANGUAGE CXX)
target_include_directories(pd_native_check PRIVATE ${FAKE} ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(pd_native_check PRIVATE z m)
if(NOT APPLE)
  target_link_libraries(pd_native_check PRIVATE pthread dl)
endif()
