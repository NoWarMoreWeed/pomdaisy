/* Forced into every DSperate source of the iOS / Linux build of the Android app's native library (dsperate.cmake). */
#pragma once
#ifdef __APPLE__
#include <mach/vm_param.h>
#undef PAGE_SIZE
#undef PAGE_MASK
#undef PAGE_SHIFT
#include <pthread.h>
#include <pthread/qos.h>
#endif
