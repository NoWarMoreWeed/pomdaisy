/* PomDaisy (iOS): the JNI the Android app's native glue calls (include/jni.h), over plain C structs, and the helpers
 * Kotlin/Native uses to make and read those structs (pd_jni.h). One process-wide env: the glue keeps no per-thread
 * JNI state that matters here (it attaches for RetroAchievements' callbacks, which go through pd_jni_set_callback). */
#include "pd_jni.h"

#include <stdlib.h>
#include <string.h>

enum { K_BYTES = 1, K_INTS, K_SHORTS, K_STRING, K_OBJECTS, K_BUFFER, K_CLASS, K_OTHER };

struct pd_jobject {
    int kind;
    jsize len;       /* elements (arrays), bytes (string, without the 0) */
    void *data;      /* elements; UTF-8 for a string; the address for a direct buffer */
    jlong cap;       /* direct buffer capacity */
    char name[96];   /* a class's name */
};

struct pd_jmethod {
    char name[64];
};

static size_t elem_size(int kind) { return kind == K_INTS ? 4 : kind == K_SHORTS ? 2 : kind == K_OBJECTS ? sizeof(jobject) : 1; }

static jobject new_obj(int kind, jsize len) {
    jobject o = calloc(1, sizeof(struct pd_jobject));
    o->kind = kind;
    o->len = len;
    if (kind == K_BYTES || kind == K_INTS || kind == K_SHORTS || kind == K_OBJECTS)
        o->data = calloc(len > 0 ? (size_t)len : 1, elem_size(kind));
    return o;
}

/* ---- the JNI functions ---- */
static jsize GetArrayLength(JNIEnv *e, jarray a) { (void)e; return a ? a->len : 0; }
static jbyte *GetByteArrayElements(JNIEnv *e, jbyteArray a, jboolean *c) { (void)e; if (c) *c = 0; return a ? a->data : NULL; }
static void ReleaseByteArrayElements(JNIEnv *e, jbyteArray a, jbyte *p, jint m) { (void)e; (void)a; (void)p; (void)m; }
static jint *GetIntArrayElements(JNIEnv *e, jintArray a, jboolean *c) { (void)e; if (c) *c = 0; return a ? a->data : NULL; }
static void ReleaseIntArrayElements(JNIEnv *e, jintArray a, jint *p, jint m) { (void)e; (void)a; (void)p; (void)m; }
static jshort *GetShortArrayElements(JNIEnv *e, jshortArray a, jboolean *c) { (void)e; if (c) *c = 0; return a ? a->data : NULL; }
static void ReleaseShortArrayElements(JNIEnv *e, jshortArray a, jshort *p, jint m) { (void)e; (void)a; (void)p; (void)m; }
static void *GetPrimitiveArrayCritical(JNIEnv *e, jarray a, jboolean *c) { (void)e; if (c) *c = 0; return a ? a->data : NULL; }
static void ReleasePrimitiveArrayCritical(JNIEnv *e, jarray a, void *p, jint m) { (void)e; (void)a; (void)p; (void)m; }
static void GetByteArrayRegion(JNIEnv *e, jbyteArray a, jsize s, jsize n, jbyte *out) { (void)e; if (a && n > 0) memcpy(out, (jbyte *)a->data + s, (size_t)n); }
static void SetByteArrayRegion(JNIEnv *e, jbyteArray a, jsize s, jsize n, const jbyte *in) { (void)e; if (a && n > 0) memcpy((jbyte *)a->data + s, in, (size_t)n); }
static void GetIntArrayRegion(JNIEnv *e, jintArray a, jsize s, jsize n, jint *out) { (void)e; if (a && n > 0) memcpy(out, (jint *)a->data + s, (size_t)n * 4); }
static void SetIntArrayRegion(JNIEnv *e, jintArray a, jsize s, jsize n, const jint *in) { (void)e; if (a && n > 0) memcpy((jint *)a->data + s, in, (size_t)n * 4); }
static void SetShortArrayRegion(JNIEnv *e, jshortArray a, jsize s, jsize n, const jshort *in) { (void)e; if (a && n > 0) memcpy((jshort *)a->data + s, in, (size_t)n * 2); }
static jbyteArray NewByteArray(JNIEnv *e, jsize n) { (void)e; return new_obj(K_BYTES, n); }
static jintArray NewIntArray(JNIEnv *e, jsize n) { (void)e; return new_obj(K_INTS, n); }
static jshortArray NewShortArray(JNIEnv *e, jsize n) { (void)e; return new_obj(K_SHORTS, n); }
static jstring NewStringUTF(JNIEnv *e, const char *s) { (void)e; return pd_jni_string(s); }
static const char *GetStringUTFChars(JNIEnv *e, jstring s, jboolean *c) { (void)e; if (c) *c = 0; return s ? s->data : NULL; }
static void ReleaseStringUTFChars(JNIEnv *e, jstring s, const char *p) { (void)e; (void)s; (void)p; }
static jsize GetStringUTFLength(JNIEnv *e, jstring s) { (void)e; return s ? s->len : 0; }
static jobject GetObjectArrayElement(JNIEnv *e, jobjectArray a, jsize i) { (void)e; return a && i < a->len ? ((jobject *)a->data)[i] : NULL; }
static jobject NewDirectByteBuffer(JNIEnv *e, void *p, jlong cap) { (void)e; jobject o = new_obj(K_BUFFER, 0); o->data = p; o->cap = cap; return o; }
static void *GetDirectBufferAddress(JNIEnv *e, jobject b) { (void)e; return b ? b->data : NULL; }
static jlong GetDirectBufferCapacity(JNIEnv *e, jobject b) { (void)e; return b ? b->cap : -1; }
static jobject NewGlobalRef(JNIEnv *e, jobject o) { (void)e; return o; }
static void DeleteGlobalRef(JNIEnv *e, jobject o) { (void)e; (void)o; }
static void DeleteLocalRef(JNIEnv *e, jobject o) { (void)e; (void)o; }
static jclass FindClass(JNIEnv *e, const char *name) {
    (void)e;
    jobject c = new_obj(K_CLASS, 0);
    strncpy(c->name, name ? name : "", sizeof c->name - 1);
    return c;
}
static jclass GetObjectClass(JNIEnv *e, jobject o) { (void)o; return FindClass(e, "java/lang/Object"); }
static jmethodID method(const char *name) {
    jmethodID m = calloc(1, sizeof(struct pd_jmethod));
    strncpy(m->name, name ? name : "", sizeof m->name - 1);
    return m;
}
static jmethodID GetMethodID(JNIEnv *e, jclass c, const char *name, const char *sig) { (void)e; (void)c; (void)sig; return method(name); }
static jmethodID GetStaticMethodID(JNIEnv *e, jclass c, const char *name, const char *sig) { (void)e; (void)c; (void)sig; return method(name); }
/* new String(bytes, charset): the bytes as a UTF-8 string */
static jobject NewObject(JNIEnv *e, jclass c, jmethodID m, ...) {
    (void)e; (void)c; (void)m;
    va_list ap;
    va_start(ap, m);
    jbyteArray bytes = va_arg(ap, jbyteArray);
    va_end(ap);
    if (!bytes) return NULL;
    char *s = malloc((size_t)bytes->len + 1);
    memcpy(s, bytes->data, (size_t)bytes->len);
    s[bytes->len] = 0;
    jstring r = pd_jni_string(s);
    free(s);
    return r;
}
static pd_jni_callback callback;
static void CallStaticVoidMethodV(JNIEnv *e, jclass c, jmethodID m, va_list ap) {
    (void)e;
    if (callback) callback(c ? c->name : "", m ? m->name : "", ap);
}
static void CallStaticVoidMethod(JNIEnv *e, jclass c, jmethodID m, ...) {
    va_list ap;
    va_start(ap, m);
    CallStaticVoidMethodV(e, c, m, ap);
    va_end(ap);
}
static JavaVM theVm;
static jint GetJavaVM(JNIEnv *e, JavaVM **vm) { (void)e; *vm = &theVm; return JNI_OK; }
static jboolean ExceptionCheck(JNIEnv *e) { (void)e; return 0; }
static void ExceptionClear(JNIEnv *e) { (void)e; }
static void ExceptionDescribe(JNIEnv *e) { (void)e; }
static jint ThrowNew(JNIEnv *e, jclass c, const char *msg) { (void)e; (void)c; (void)msg; return 0; }

static const struct JNINativeInterface functions = {
    GetArrayLength, GetByteArrayElements, ReleaseByteArrayElements, GetIntArrayElements, ReleaseIntArrayElements,
    GetShortArrayElements, ReleaseShortArrayElements, GetPrimitiveArrayCritical, ReleasePrimitiveArrayCritical,
    GetByteArrayRegion, SetByteArrayRegion, GetIntArrayRegion, SetIntArrayRegion, SetShortArrayRegion, NewByteArray,
    NewIntArray, NewShortArray, NewStringUTF, GetStringUTFChars, ReleaseStringUTFChars, GetStringUTFLength,
    GetObjectArrayElement, NewDirectByteBuffer, GetDirectBufferAddress, GetDirectBufferCapacity, NewGlobalRef,
    DeleteGlobalRef, DeleteLocalRef, FindClass, GetObjectClass, GetMethodID, GetStaticMethodID, NewObject,
    CallStaticVoidMethodV, CallStaticVoidMethod, GetJavaVM, ExceptionCheck, ExceptionClear, ExceptionDescribe, ThrowNew,
};
static JNIEnv theEnv = &functions;

static jint AttachCurrentThread(JavaVM *vm, JNIEnv **env, void *args) { (void)vm; (void)args; *env = &theEnv; return JNI_OK; }
static jint DetachCurrentThread(JavaVM *vm) { (void)vm; return JNI_OK; }
static jint GetEnv(JavaVM *vm, void **env, jint v) { (void)vm; (void)v; *env = &theEnv; return JNI_OK; }
static const struct JNIInvokeInterface invoke = {AttachCurrentThread, DetachCurrentThread, GetEnv};
static JavaVM theVm = &invoke;

/* ---- for Kotlin/Native ---- */
JNIEnv *pd_jni_env(void) { return &theEnv; }
jobject pd_jni_bytes(const void *data, int32_t len) {
    jobject o = new_obj(K_BYTES, len);
    if (data && len > 0) memcpy(o->data, data, (size_t)len);
    return o;
}
jobject pd_jni_ints(const int32_t *data, int32_t len) {
    jobject o = new_obj(K_INTS, len);
    if (data && len > 0) memcpy(o->data, data, (size_t)len * 4);
    return o;
}
jobject pd_jni_shorts(const int16_t *data, int32_t len) {
    jobject o = new_obj(K_SHORTS, len);
    if (data && len > 0) memcpy(o->data, data, (size_t)len * 2);
    return o;
}
jobject pd_jni_string(const char *utf8) {
    if (!utf8) return NULL;
    jobject o = new_obj(K_STRING, (jsize)strlen(utf8));
    o->data = strdup(utf8);
    return o;
}
jobject pd_jni_objects(int32_t len) { return new_obj(K_OBJECTS, len); }
void pd_jni_objects_set(jobject a, int32_t i, jobject v) { if (a && i < a->len) ((jobject *)a->data)[i] = v; }
int32_t pd_jni_length(jobject o) { return o ? o->len : 0; }
void *pd_jni_data(jobject o) { return o ? o->data : NULL; }
int64_t pd_jni_capacity(jobject o) { return o ? o->cap : 0; }
int32_t pd_jni_kind(jobject o) { return o ? o->kind : 0; }
void pd_jni_free(jobject o) {
    if (!o) return;
    if (o->kind == K_OBJECTS)
        for (jsize i = 0; i < o->len; i++) pd_jni_free(((jobject *)o->data)[i]);
    if (o->kind != K_BUFFER && o->kind != K_CLASS) free(o->data);
    free(o);
}
void pd_jni_set_callback(pd_jni_callback cb) { callback = cb; }
