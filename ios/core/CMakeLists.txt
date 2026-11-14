# PomDaisy (iOS): the PokeDaisy libretro core (mGBA + the companion layer) as a STATIC library for iPhone / iPad.
# It compiles the same sources as libretro/CMakeLists.txt (pokedaisy_gbc_libretro), which handles .gb, .gbc and .gba:
# iOS doesn't let an app load a core from a file at run time, and armGBA (the Linux GBA core) is a recompiler, i.e.
# JIT, which a TestFlight / App Store build can't run. So one mGBA core, linked into the app.
cmake_minimum_required(VERSION 3.18)
project(pomdaisy_core C)
set(CMAKE_POLICY_VERSION_MINIMUM 3.5) # mGBA 0.10 declares an old minimum
set(CMAKE_C_STANDARD 11)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../..)
foreach(feature BUILD_QT BUILD_SDL BUILD_GL BUILD_GLES2 BUILD_GLES3 BUILD_SHARED USE_DISCORD_RPC USE_LUA USE_FFMPEG USE_LIBZIP USE_PNG USE_SQLITE3 USE_ELF USE_LZMA USE_DEBUGGERS USE_EDITLINE USE_EPOXY USE_FREETYPE USE_MINIZIP USE_ZLIB)
  set(${feature} OFF CACHE BOOL "" FORCE)
endforeach()
set(BUILD_STATIC OFF CACHE BOOL "" FORCE)
set(SKIP_LIBRARY ON CACHE BOOL "" FORCE)
set(BUILD_LIBRETRO ON CACHE BOOL "" FORCE)
set(MGBA ${ROOT}/third_party/mgba)
if(NOT EXISTS ${MGBA}/CMakeLists.txt)
  message(FATAL_ERROR "Initialize the pinned core: git submodule update --init third_party/mgba")
endif()
add_subdirectory(${MGBA} mgba EXCLUDE_FROM_ALL)

# mGBA's own libretro target names the sources, definitions and includes of the minimal core (as libretro/ does).
get_target_property(MGBA_SRC mgba_libretro SOURCES)
get_target_property(MGBA_DEFS mgba_libretro COMPILE_DEFINITIONS)
get_directory_property(MGBA_DIR_DEFS DIRECTORY ${MGBA} COMPILE_DEFINITIONS)
list(REMOVE_ITEM MGBA_DIR_DEFS USE_PTHREADS) # threading is off; pk_rewind.c is not built
get_target_property(MGBA_INC mgba_libretro INCLUDE_DIRECTORIES)
list(FILTER MGBA_SRC EXCLUDE REGEX "platform/libretro/libretro\\.c$") # pd_libretro.c includes it, renamed

set(APP ${ROOT}/app/src/main/cpp)
set(LR ${ROOT}/libretro)
set(LX ${ROOT}/linux)
file(GLOB PD_CORE CONFIGURE_DEPENDS ${ROOT}/3ds/core/*.c)

add_library(pomdaisy_core STATIC ${MGBA_SRC} ${LR}/pd_libretro.c ${LR}/pd_lr_mem.c ${PD_CORE}
  ${LX}/hud.c ${LX}/quick_menu.c ${LX}/fly.c ${LX}/flight.c ${LX}/flash.c ${LX}/field_items.c ${LX}/wild.c ${LX}/gb_field.c
  ${LX}/polished_field.c
  ${LR}/pd_flight_area.c ${APP}/pk_call.c ${APP}/pk_flight.c ${APP}/pk_wide.c ${APP}/pk_wide_gb.c ${APP}/pk_gb_field.c)
set_source_files_properties(${APP}/pk_wide.c ${APP}/pk_wide_gb.c PROPERTIES COMPILE_OPTIONS -Wno-unused-parameter)
set_source_files_properties(${APP}/pk_wide_gb.c PROPERTIES COMPILE_DEFINITIONS PK_WIDE_GB_GLOW=1)
set_target_properties(pomdaisy_core PROPERTIES COMPILE_DEFINITIONS "${MGBA_DEFS};${MGBA_DIR_DEFS};_GNU_SOURCE")
target_include_directories(pomdaisy_core PRIVATE ${MGBA_INC} ${MGBA}/include ${CMAKE_CURRENT_BINARY_DIR}/mgba/include
  ${MGBA}/src/platform/libretro ${MGBA}/src ${ROOT}/3ds/core ${LX} ${APP} ${LR})
target_include_directories(pomdaisy_core INTERFACE ${MGBA}/src/platform/libretro)

# A link check: a tiny program that calls every retro_* entry point the app will use. A static library hides missing
# symbols until the app links; this finds them in CI already.
add_executable(pomdaisy_link_check link_check.c)
target_link_libraries(pomdaisy_link_check PRIVATE pomdaisy_core m)
