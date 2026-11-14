/* PomDaisy (iOS): Android's log, to stderr (warnings and errors only). */
#ifndef PD_ANDROID_LOG_H
#define PD_ANDROID_LOG_H
#include <stdarg.h>
#include <stdio.h>
enum { ANDROID_LOG_UNKNOWN = 0, ANDROID_LOG_DEFAULT, ANDROID_LOG_VERBOSE, ANDROID_LOG_DEBUG, ANDROID_LOG_INFO, ANDROID_LOG_WARN,
    ANDROID_LOG_ERROR, ANDROID_LOG_FATAL, ANDROID_LOG_SILENT };
static inline int __android_log_vprint(int prio, const char *tag, const char *fmt, va_list ap) {
    if (prio < ANDROID_LOG_WARN) return 0;
    fprintf(stderr, "%s: ", tag);
    vfprintf(stderr, fmt, ap);
    fputc('\n', stderr);
    return 0;
}
static inline int __android_log_print(int prio, const char *tag, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    int r = __android_log_vprint(prio, tag, fmt, ap);
    va_end(ap);
    return r;
}
static inline int __android_log_write(int prio, const char *tag, const char *text) { return __android_log_print(prio, tag, "%s", text); }
#endif
