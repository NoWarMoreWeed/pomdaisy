// The conversions MgbaCore's generated calls into the Android app's JNI glue use (ios/compose's genJni): Kotlin arrays
// and strings into the glue's objects (ios/native/pd_jni.c) and back.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.pokedaisy.ios

import kotlinx.cinterop.*
import pdglue.*

val pdEnv: CPointer<JNIEnvVar>? get() = pd_jni_env()

fun pdBool(b: Boolean): UByte = if (b) 1u else 0u

fun pdString(s: String?): jobject? = s?.let { pd_jni_string(it) }

fun pdStrings(a: Array<String>?): jobject? {
    if (a == null) return null
    val o = pd_jni_objects(a.size)
    a.forEachIndexed { i, s -> pd_jni_objects_set(o, i, pd_jni_string(s)) }
    return o
}

fun pdBytes(a: ByteArray?): jobject? = a?.let { arr -> if (arr.isEmpty()) pd_jni_bytes(null, 0) else arr.usePinned { pd_jni_bytes(it.addressOf(0), arr.size) } }
fun pdInts(a: IntArray?): jobject? = a?.let { arr -> if (arr.isEmpty()) pd_jni_ints(null, 0) else arr.usePinned { pd_jni_ints(it.addressOf(0), arr.size) } }
fun pdShorts(a: ShortArray?): jobject? = a?.let { arr -> if (arr.isEmpty()) pd_jni_shorts(null, 0) else arr.usePinned { pd_jni_shorts(it.addressOf(0), arr.size) } }

/** What the glue wrote into an array argument, back into the Kotlin array. */
fun pdBytesBack(j: jobject?, a: ByteArray?) {
    if (j == null || a == null || a.isEmpty()) return
    val p = pd_jni_data(j)?.reinterpret<ByteVar>() ?: return
    a.usePinned { platform.posix.memcpy(it.addressOf(0), p, a.size.toULong()) }
}
fun pdIntsBack(j: jobject?, a: IntArray?) {
    if (j == null || a == null || a.isEmpty()) return
    val p = pd_jni_data(j) ?: return
    a.usePinned { platform.posix.memcpy(it.addressOf(0), p, (a.size * 4).toULong()) }
}
fun pdShortsBack(j: jobject?, a: ShortArray?) {
    if (j == null || a == null || a.isEmpty()) return
    val p = pd_jni_data(j) ?: return
    a.usePinned { platform.posix.memcpy(it.addressOf(0), p, (a.size * 2).toULong()) }
}

/** A result object: copied out, then freed. */
fun pdTakeString(j: jobject?): String? {
    if (j == null) return null
    val s = pd_jni_data(j)?.reinterpret<ByteVar>()?.toKString()
    pd_jni_free(j)
    return s
}
fun pdTakeBytes(j: jobject?): ByteArray? {
    if (j == null) return null
    val n = pd_jni_length(j)
    val out = ByteArray(n)
    if (n > 0) out.usePinned { platform.posix.memcpy(it.addressOf(0), pd_jni_data(j), n.toULong()) }
    pd_jni_free(j)
    return out
}
fun pdTakeInts(j: jobject?): IntArray? {
    if (j == null) return null
    val n = pd_jni_length(j)
    val out = IntArray(n)
    if (n > 0) out.usePinned { platform.posix.memcpy(it.addressOf(0), pd_jni_data(j), (n * 4).toULong()) }
    pd_jni_free(j)
    return out
}
fun pdTakeShorts(j: jobject?): ShortArray? {
    if (j == null) return null
    val n = pd_jni_length(j)
    val out = ShortArray(n)
    if (n > 0) out.usePinned { platform.posix.memcpy(it.addressOf(0), pd_jni_data(j), (n * 2).toULong()) }
    pd_jni_free(j)
    return out
}
/** A direct buffer over the glue's memory (the frame): read in place, never copied. */
fun pdTakeBuffer(j: jobject?): java.nio.ByteBuffer? {
    if (j == null) return null
    val p = pd_jni_data(j)?.reinterpret<ByteVar>()
    val cap = pd_jni_capacity(j)
    pd_jni_free(j)
    return if (p == null) null else java.nio.ByteBuffer.native(p, cap.toInt())
}
