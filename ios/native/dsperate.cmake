# DSperate (third_party/dsperate) for the iOS build of the Android app's native library: its core library only, no JIT,
# NEON on arm64. Fixes go into copies in the build directory, never into the submodule (as app/src/main/cpp/nds/
# dsperate.cmake does for the NDK): its clang fixes, plus what Apple's toolchain needs (ios/ds/CMakeLists.txt's).
set(DSPERATE_DIR ${ROOT}/third_party/dsperate)
set(DSPERATE_JIT OFF)
set(DSPERATE_HOST_ARM32 OFF)
if(CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
  set(DSPERATE_HOST_AARCH64 ON)
  set(DSPERATE_NEON ON)
else()
  set(DSPERATE_HOST_AARCH64 OFF)
  set(DSPERATE_NEON OFF)
endif()
set(DSPERATE_VULKAN OFF CACHE BOOL "" FORCE)
set(DSPERATE_GLES OFF CACHE BOOL "" FORCE)
add_subdirectory(${DSPERATE_DIR}/src/core ${CMAKE_CURRENT_BINARY_DIR}/dsperate EXCLUDE_FROM_ALL)
set_target_properties(dsperate_core PROPERTIES CXX_STANDARD 17 CXX_STANDARD_REQUIRED ON CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON INTERPROCEDURAL_OPTIMIZATION OFF)
target_compile_options(dsperate_core PRIVATE -O3 -fno-stack-protector -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0 -Wno-unused-parameter
    "SHELL:-include ${CMAKE_CURRENT_LIST_DIR}/dsperate_host.h")

function(dsperate_fix rel)
  file(READ "${DSPERATE_DIR}/src/core/${rel}" text)
  set(pairs ${ARGN})
  list(LENGTH pairs n)
  math(EXPR last "${n} - 1")
  foreach(i RANGE 0 ${last} 2)
    math(EXPR j "${i} + 1")
    list(GET pairs ${i} from)
    list(GET pairs ${j} to)
    string(FIND "${text}" "${from}" at)
    if(at EQUAL -1)
      message(FATAL_ERROR "ios/native: '${from}' isn't in ${rel} any more - check the fix against the new DSperate")
    endif()
    string(REPLACE "${from}" "${to}" text "${text}")
  endforeach()
  set(out "${CMAKE_CURRENT_BINARY_DIR}/dsperate-fixed/${rel}")
  file(WRITE "${out}.new" "${text}")
  file(COPY_FILE "${out}.new" "${out}" ONLY_IF_DIFFERENT)
  get_target_property(srcs dsperate_core SOURCES)
  list(TRANSFORM srcs REPLACE "^${rel}$" "${out}")
  set_target_properties(dsperate_core PROPERTIES SOURCES "${srcs}")
  get_filename_component(dir "${DSPERATE_DIR}/src/core/${rel}" DIRECTORY)
  set_property(SOURCE "${out}" TARGET_DIRECTORY dsperate_core APPEND PROPERTY INCLUDE_DIRECTORIES "${dir}")
  get_source_file_property(defs "${DSPERATE_DIR}/src/core/${rel}" TARGET_DIRECTORY dsperate_core COMPILE_DEFINITIONS)
  if(defs)
    set_property(SOURCE "${out}" TARGET_DIRECTORY dsperate_core PROPERTY COMPILE_DEFINITIONS "${defs}")
  endif()
endfunction()

if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
  dsperate_fix(gpu/render3d.cpp "reinterpret_cast<GatherNFn>(sh.gather4)" "reinterpret_cast<GatherNFn>(const_cast<void*>(sh.gather4))")
  if(DSPERATE_NEON)
    dsperate_fix(gpu/kernels_neon.cpp
      "uint32x2_t{c, cs}" "(uint32x2_t{c, cs})" "uint32x2_t{cd, c}" "(uint32x2_t{cd, c})")
  endif()
endif()
# DSperate and melonDS both bundle tiny-AES-c: DSperate's copy is renamed (as the Android build does).
foreach(sym AES_CBC_decrypt_buffer AES_CTR_next_keystream AES_CTR_xcrypt_buffer AES_ECB_encrypt AES_ctx_set_iv AES_force_software AES_init_ctx AES_init_ctx_iv)
  target_compile_definitions(dsperate_core PRIVATE "${sym}=pk_dsp_${sym}")
endforeach()
if(APPLE)
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
  dsperate_fix(crypto/aes.c "#include <sys/auxv.h>" "/* no <sys/auxv.h> on Darwin */" "(getauxval(AT_HWCAP) & HWCAP_AES_BIT) != 0" "1")
  dsperate_fix(host_cores.cpp "(void)name" "(void)name, pthread_set_qos_class_self_np(QOS_CLASS_USER_INTERACTIVE, 0)")
endif()
