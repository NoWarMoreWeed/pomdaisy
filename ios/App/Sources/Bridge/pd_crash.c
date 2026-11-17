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

// ---- Hangs. iOS kills an app whose main thread is stuck for ~10 s (0x8BADF00D) with SIGKILL, which no handler sees,
// so a watchdog thread notices first: if the main thread's heartbeat (pd_hang_beat, a timer on its run loop) stops
// for 5 s, it stops the main thread for a moment, walks its frame pointers and writes the addresses (with the slide,
// for the dSYM) to the crash file. Once per run.
#include <mach/mach.h>
#include <pthread.h>

static volatile unsigned long beats;
static mach_port_t main_mach;
static pthread_t main_pthread;

void pd_hang_beat(void) { beats++; }

static void write_addr_line(unsigned long a) {
    put("  ");
    put_hex(a & 0x0000000FFFFFFFFFul);
    put("\n");
}

static void *hang_watch(void *arg) {
    (void)arg;
    unsigned long last = beats;
    int still = 0;
    for (;;) {
        sleep(1);
        if (beats != last) { last = beats; still = 0; continue; }
        if (++still < 5) continue;
        put("\n=== hang: the main thread has been stuck for 5 s\n");
        put(slide_line);
        char *lo = (char *)pthread_get_stackaddr_np(main_pthread) - pthread_get_stacksize_np(main_pthread);
        char *hi = (char *)pthread_get_stackaddr_np(main_pthread);
        if (thread_suspend(main_mach) == KERN_SUCCESS) {
            arm_thread_state64_t st;
            mach_msg_type_number_t n = ARM_THREAD_STATE64_COUNT;
            if (thread_get_state(main_mach, ARM_THREAD_STATE64, (thread_state_t)&st, &n) == KERN_SUCCESS) {
                write_addr_line((unsigned long)arm_thread_state64_get_pc(st));
                write_addr_line((unsigned long)arm_thread_state64_get_lr(st));
                unsigned long *fp = (unsigned long *)arm_thread_state64_get_fp(st);
                for (int i = 0; i < 96 && (char *)fp > lo && (char *)fp < hi - 16; i++) {
                    write_addr_line(fp[1]);
                    unsigned long *next = (unsigned long *)fp[0];
                    if (next <= fp) break;
                    fp = next;
                }
            }
            thread_resume(main_mach);
        }
        put("=== end of hang\n");
        return NULL;
    }
}

void pd_hang_install(void) {
    main_mach = mach_thread_self();
    main_pthread = pthread_self();
    pthread_t t;
    pthread_create(&t, NULL, hang_watch, NULL);
    pthread_detach(t);
}
