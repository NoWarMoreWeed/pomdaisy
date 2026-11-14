/* PomDaisy (iOS): the one Android header the DS core (app/src/main/cpp/nds/pk_nds.cpp) includes, its log to stderr. */
#pragma once
#include <stdarg.h>
#include <stdio.h>

enum { ANDROID_LOG_INFO = 4, ANDROID_LOG_WARN = 5, ANDROID_LOG_ERROR = 6 };

static inline int __android_log_print(int prio, const char *tag, const char *fmt, ...) {
  if (prio < ANDROID_LOG_WARN)
    return 0;
  va_list ap;
  va_start(ap, fmt);
  fprintf(stderr, "%s: ", tag);
  vfprintf(stderr, fmt, ap);
  fputc('\n', stderr);
  va_end(ap);
  return 0;
}
