/* PomDaisy (iOS): Android's native window (DS RESOLUTION's GPU path draws into one): none on iOS. */
#ifndef PD_ANDROID_NATIVE_WINDOW_JNI_H
#define PD_ANDROID_NATIVE_WINDOW_JNI_H
#include <jni.h>
typedef struct ANativeWindow ANativeWindow;
static inline ANativeWindow *ANativeWindow_fromSurface(JNIEnv *env, jobject surface) { (void)env; (void)surface; return 0; }
static inline void ANativeWindow_release(ANativeWindow *w) { (void)w; }
static inline void ANativeWindow_acquire(ANativeWindow *w) { (void)w; }
#endif
