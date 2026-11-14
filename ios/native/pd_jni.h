/* PomDaisy (iOS): what Kotlin/Native needs to call the Android app's JNI glue (pd_jni.c). */
#ifndef PD_JNI_H
#define PD_JNI_H
#include <jni.h>
#include <stdint.h>

JNIEnv *pd_jni_env(void);
/* Objects made from Kotlin's arrays / strings (copies), read back after the call, then freed. */
jobject pd_jni_bytes(const void *data, int32_t len);
jobject pd_jni_ints(const int32_t *data, int32_t len);
jobject pd_jni_shorts(const int16_t *data, int32_t len);
jobject pd_jni_string(const char *utf8);
jobject pd_jni_objects(int32_t len);
void pd_jni_objects_set(jobject array, int32_t index, jobject value);
int32_t pd_jni_length(jobject o);
void *pd_jni_data(jobject o);
int64_t pd_jni_capacity(jobject o);
int32_t pd_jni_kind(jobject o);
void pd_jni_free(jobject o);
/* CallStaticVoidMethod(V): the class and method names and the arguments. */
typedef void (*pd_jni_callback)(const char *cls, const char *method, va_list args);
void pd_jni_set_callback(pd_jni_callback cb);
#endif
