// PomDaisy's crash catcher: a fatal signal (a crash in native code, Kotlin/Native's abort after an uncaught
// exception, a stack overflow - hence the alternate signal stack) appends the signal, the app's load slide and a
// backtrace to the pending crash file, then lets the crash go on as before. The next start finds the file
// (Crash.swift). Only async-signal-safe calls in the handler, beside backtrace(), which crash reporters use too.
#include "pd_crash.h"

#include <execinfo.h>
#include <fcntl.h>
#include <mach-o/dyld.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int crash_fd = -1;
static char slide_line[96];

static void put(const char *s) {
    if (crash_fd >= 0) (void)!write(crash_fd, s, strlen(s));
}

static void put_hex(unsigned long v) {
    char b[19] = "0x";
    int n = 2;
    for (int sh = 60; sh >= 0; sh -= 4) {
        int d = (int)((v >> sh) & 0xF);
        if (n > 2 || d || sh == 0) b[n++] = (char)(d < 10 ? '0' + d : 'a' + d - 10);
    }
    b[n] = 0;
    put(b);
}

static void handler(int sig, siginfo_t *info, void *ctx) {
    (void)ctx;
    put("\n=== native crash: signal ");
    char num[4] = {(char)('0' + sig / 10), (char)('0' + sig % 10), '\n', 0};
    put(num);
    put("fault address ");
    put_hex((unsigned long)(info ? info->si_addr : 0));
    put("\n");
    put(slide_line);
    void *frames[128];
    int n = backtrace(frames, 128);
    if (crash_fd >= 0) backtrace_symbols_fd(frames, n, crash_fd);
    // The default action now: the system's own crash report is made as before.
    signal(sig, SIG_DFL);
    raise(sig);
}

void pd_crash_install(const char *path) {
    crash_fd = open(path, O_WRONLY | O_CREAT | O_APPEND, 0644);
    // The main executable's slide, to turn the backtrace's addresses into symbols with the build's dSYM.
    unsigned long slide = (unsigned long)_dyld_get_image_vmaddr_slide(0);
    char hex[17];
    for (int i = 15; i >= 0; i--) { int d = (int)(slide & 0xF); hex[i] = (char)(d < 10 ? '0' + d : 'a' + d - 10); slide >>= 4; }
    hex[16] = 0;
    strcpy(slide_line, "image slide 0x");
    strcat(slide_line, hex);
    strcat(slide_line, "\n");

    static char alt[64 * 1024];
    stack_t ss = {.ss_sp = alt, .ss_size = sizeof alt, .ss_flags = 0};
    sigaltstack(&ss, NULL);
    struct sigaction sa;
    memset(&sa, 0, sizeof sa);
    sa.sa_sigaction = handler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
    sigemptyset(&sa.sa_mask);
    int sigs[] = {SIGSEGV, SIGBUS, SIGILL, SIGABRT, SIGFPE, SIGTRAP};
    for (unsigned i = 0; i < sizeof sigs / sizeof *sigs; i++) sigaction(sigs[i], &sa, NULL);
}

void pd_crash_note(const char *text) {
    put(text);
}
