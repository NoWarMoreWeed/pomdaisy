/* Forced into every DSperate source the iOS build compiles (ios/ds/CMakeLists.txt). Darwin's <mach/vm_param.h> makes
 * PAGE_SIZE, PAGE_MASK and PAGE_SHIFT macros, which would turn DSperate's own constants of those names
 * (mem/page_table.h: its 2 KB guest page) into the host's. Included first, then undefined: the header's include guard
 * keeps them from coming back. */
#pragma once
#ifdef __APPLE__
#include <mach/vm_param.h>
#undef PAGE_SIZE
#undef PAGE_MASK
#undef PAGE_SHIFT
#include <pthread.h>
#include <pthread/qos.h> /* the worker threads' priority (CMakeLists.txt's host_cores.cpp fix) */
#endif
