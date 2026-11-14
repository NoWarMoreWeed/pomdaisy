/* Runs a game through the Android app's JNI glue, as its Kotlin does (MgbaCore.pkInit, pkRunFrame ...), over the JNI of
 * our own: pd_native_check <rom> [frames]. Prints the frame rate and a checksum of the picture. */
#include "pd_jni.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

/* The glue's functions this check calls (their JNI signatures: env, class, then the arguments). */
jboolean Java_com_pokedaisy_app_MgbaCore_pkInit(JNIEnv *, jclass, jstring, jstring);
void Java_com_pokedaisy_app_MgbaCore_pkRunFrame(JNIEnv *, jclass);
jobject Java_com_pokedaisy_app_MgbaCore_pkVideoBuffer(JNIEnv *, jclass);
jint Java_com_pokedaisy_app_MgbaCore_pkVideoWidth(JNIEnv *, jclass);
jint Java_com_pokedaisy_app_MgbaCore_pkVideoHeight(JNIEnv *, jclass);
void Java_com_pokedaisy_app_MgbaCore_pkDeinit(JNIEnv *, jclass);

static double now(void) { struct timespec t; clock_gettime(CLOCK_MONOTONIC, &t); return t.tv_sec + t.tv_nsec / 1e9; }

int main(int argc, char **argv) {
    if (argc < 2) return 0;
    int frames = argc > 2 ? atoi(argv[2]) : 600;
    JNIEnv *env = pd_jni_env();
    char save[1024];
    snprintf(save, sizeof save, "%s.sav", argv[1]);
    jstring rom = pd_jni_string(argv[1]), sav = pd_jni_string(save);
    if (!Java_com_pokedaisy_app_MgbaCore_pkInit(env, NULL, rom, sav)) { printf("pkInit failed\n"); return 1; }
    double t0 = now();
    for (int i = 0; i < frames; i++) Java_com_pokedaisy_app_MgbaCore_pkRunFrame(env, NULL);
    double t = now() - t0;
    jobject buf = Java_com_pokedaisy_app_MgbaCore_pkVideoBuffer(env, NULL);
    int w = Java_com_pokedaisy_app_MgbaCore_pkVideoWidth(env, NULL), h = Java_com_pokedaisy_app_MgbaCore_pkVideoHeight(env, NULL);
    const uint32_t *px = pd_jni_data(buf);
    uint32_t sum = 0;
    for (int i = 0; px && i < w * h; i++) sum = sum * 31 + px[i];
    printf("%s: %d frames in %.2f s = %.0f fps, %dx%d, sum %08x\n", argv[1], frames, t, frames / t, w, h, sum);
    Java_com_pokedaisy_app_MgbaCore_pkDeinit(env, NULL);
    return 0;
}
