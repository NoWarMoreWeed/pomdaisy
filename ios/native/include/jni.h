/* PomDaisy (iOS): a small JNI of our own, so the Android app's native glue (app/src/main/cpp/pokedaisy_jni.c & co.)
 * builds and runs unchanged under Kotlin/Native. Its "objects" are C structs (pd_jni.c): arrays copy the Kotlin
 * arrays' bytes in and out, strings are UTF-8, direct buffers a pointer and a length. Only the JNI calls the glue makes
 * are filled in; Kotlin/Native calls the Java_* functions through cinterop (ios/compose's generated MgbaCore). */
#ifndef PD_FAKE_JNI_H
#define PD_FAKE_JNI_H
#include <stdarg.h>
#include <stdint.h>

#define JNIEXPORT __attribute__((visibility("default")))
#define JNICALL
#define JNI_FALSE 0
#define JNI_TRUE 1
#define JNI_OK 0
#define JNI_ERR (-1)
#define JNI_EDETACHED (-2)
#define JNI_VERSION_1_6 0x00010006
#define JNI_COMMIT 1
#define JNI_ABORT 2

typedef uint8_t jboolean;
typedef int8_t jbyte;
typedef uint16_t jchar;
typedef int16_t jshort;
typedef int32_t jint;
typedef int64_t jlong;
typedef float jfloat;
typedef double jdouble;
typedef jint jsize;

struct pd_jobject;
typedef struct pd_jobject *jobject;
typedef jobject jclass, jstring, jarray, jobjectArray, jbooleanArray, jbyteArray, jcharArray, jshortArray, jintArray,
    jlongArray, jfloatArray, jdoubleArray, jthrowable, jweak;
typedef struct pd_jmethod *jmethodID;
typedef struct pd_jfield *jfieldID;
typedef union jvalue { jboolean z; jbyte b; jchar c; jshort s; jint i; jlong j; jfloat f; jdouble d; jobject l; } jvalue;

struct JNINativeInterface;
struct JNIInvokeInterface;
typedef const struct JNINativeInterface *JNIEnv;
typedef const struct JNIInvokeInterface *JavaVM;

struct JNINativeInterface {
    jsize (*GetArrayLength)(JNIEnv *, jarray);
    jbyte *(*GetByteArrayElements)(JNIEnv *, jbyteArray, jboolean *);
    void (*ReleaseByteArrayElements)(JNIEnv *, jbyteArray, jbyte *, jint);
    jint *(*GetIntArrayElements)(JNIEnv *, jintArray, jboolean *);
    void (*ReleaseIntArrayElements)(JNIEnv *, jintArray, jint *, jint);
    jshort *(*GetShortArrayElements)(JNIEnv *, jshortArray, jboolean *);
    void (*ReleaseShortArrayElements)(JNIEnv *, jshortArray, jshort *, jint);
    void *(*GetPrimitiveArrayCritical)(JNIEnv *, jarray, jboolean *);
    void (*ReleasePrimitiveArrayCritical)(JNIEnv *, jarray, void *, jint);
    void (*GetByteArrayRegion)(JNIEnv *, jbyteArray, jsize, jsize, jbyte *);
    void (*SetByteArrayRegion)(JNIEnv *, jbyteArray, jsize, jsize, const jbyte *);
    void (*GetIntArrayRegion)(JNIEnv *, jintArray, jsize, jsize, jint *);
    void (*SetIntArrayRegion)(JNIEnv *, jintArray, jsize, jsize, const jint *);
    void (*SetShortArrayRegion)(JNIEnv *, jshortArray, jsize, jsize, const jshort *);
    jbyteArray (*NewByteArray)(JNIEnv *, jsize);
    jintArray (*NewIntArray)(JNIEnv *, jsize);
    jshortArray (*NewShortArray)(JNIEnv *, jsize);
    jstring (*NewStringUTF)(JNIEnv *, const char *);
    const char *(*GetStringUTFChars)(JNIEnv *, jstring, jboolean *);
    void (*ReleaseStringUTFChars)(JNIEnv *, jstring, const char *);
    jsize (*GetStringUTFLength)(JNIEnv *, jstring);
    jobject (*GetObjectArrayElement)(JNIEnv *, jobjectArray, jsize);
    jobject (*NewDirectByteBuffer)(JNIEnv *, void *, jlong);
    void *(*GetDirectBufferAddress)(JNIEnv *, jobject);
    jlong (*GetDirectBufferCapacity)(JNIEnv *, jobject);
    jobject (*NewGlobalRef)(JNIEnv *, jobject);
    void (*DeleteGlobalRef)(JNIEnv *, jobject);
    void (*DeleteLocalRef)(JNIEnv *, jobject);
    jclass (*FindClass)(JNIEnv *, const char *);
    jclass (*GetObjectClass)(JNIEnv *, jobject);
    jmethodID (*GetMethodID)(JNIEnv *, jclass, const char *, const char *);
    jmethodID (*GetStaticMethodID)(JNIEnv *, jclass, const char *, const char *);
    jobject (*NewObject)(JNIEnv *, jclass, jmethodID, ...);
    void (*CallStaticVoidMethodV)(JNIEnv *, jclass, jmethodID, va_list);
    void (*CallStaticVoidMethod)(JNIEnv *, jclass, jmethodID, ...);
    jint (*GetJavaVM)(JNIEnv *, JavaVM **);
    jboolean (*ExceptionCheck)(JNIEnv *);
    void (*ExceptionClear)(JNIEnv *);
    void (*ExceptionDescribe)(JNIEnv *);
    jint (*ThrowNew)(JNIEnv *, jclass, const char *);
};

struct JNIInvokeInterface {
    jint (*AttachCurrentThread)(JavaVM *, JNIEnv **, void *);
    jint (*DetachCurrentThread)(JavaVM *);
    jint (*GetEnv)(JavaVM *, void **, jint);
};

#endif
