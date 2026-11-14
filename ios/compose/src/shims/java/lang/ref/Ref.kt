@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package java.lang.ref

/** java.lang.ref.WeakReference over Kotlin/Native's. */
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
open class WeakReference<T : Any>(referent: T?) {
    private val r = referent?.let { kotlin.native.ref.WeakReference(it) }
    open fun get(): T? = r?.get()
    fun clear() {}
}
open class SoftReference<T : Any>(private var referent: T?) { fun get(): T? = referent; fun clear() { referent = null } }
