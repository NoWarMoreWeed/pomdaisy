// java.lang and kotlin.jvm as the app's code uses them without imports on the JVM (both are default imports there).
// Here they live in package kotlin, which Kotlin/Native imports by default too.
@file:Suppress("PackageDirectoryMismatch", "NOTHING_TO_INLINE", "unused")

package kotlin

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock
import kotlin.concurrent.AtomicInt

typealias Volatile = kotlin.concurrent.Volatile

/** kotlin.jvm.Synchronized: the method under one lock per process (pd_lock below). */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
annotation class Synchronized

/**
 * kotlin.synchronized: one re-entrant process-wide lock. Coarser than the JVM's per-object monitors, never wrong for
 * mutual exclusion (only for wait / notify, which the app doesn't use on these locks).
 */
@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <R> synchronized(lock: Any, block: () -> R): R {
    kotlin.contracts.contract { callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }
    pdLock.lock()
    try {
        return block()
    } finally {
        pdLock.unlock()
    }
}

object pdLock {
    private val owner = AtomicInt(0)
    private var depth = 0
    private val mutex = PdMutex()
    fun lock() {
        val me = PdThreadId.current()
        if (owner.value == me) { depth++; return }
        mutex.lock()
        owner.value = me
        depth = 1
    }
    fun unlock() {
        if (--depth == 0) {
            owner.value = 0
            mutex.unlock()
        }
    }
    /** Lets go of the lock completely if this thread holds it (Object.wait): its depth, for reacquire. */
    fun releaseAll(): Int {
        if (owner.value != PdThreadId.current()) return 0
        val d = depth
        depth = 0
        owner.value = 0
        mutex.unlock()
        return d
    }
    fun reacquire(d: Int) {
        if (d <= 0) return
        lock()
        depth = d
    }
}

typealias Runnable = java.lang.Runnable
typealias InterruptedException = java.lang.InterruptedException
typealias Thread = java.lang.Thread
typealias System = java.lang.System
typealias Math = java.lang.Math
typealias Integer = java.lang.Integer
typealias Character = java.lang.Character
typealias Process = java.lang.Process
typealias LinkageError = java.lang.LinkageError
typealias Class<T> = java.lang.Class<T>
typealias Object = java.lang.Object
typealias Runtime = java.lang.Runtime
typealias UnsatisfiedLinkError = java.lang.UnsatisfiedLinkError
typealias CloneNotSupportedException = java.lang.CloneNotSupportedException
typealias SecurityException = java.lang.SecurityException
typealias StringBuffer = StringBuilder
typealias Cloneable = java.lang.Cloneable

/** obj.javaClass / X::class.java: what the app reads of a class (its names). */
val <T : Any> T.javaClass: Class<T> get() = Class(this::class.simpleName ?: "Object", this::class.qualifiedName ?: "Object")

val <T : Any> kotlin.reflect.KClass<T>.java: Class<T> get() = Class(simpleName ?: "Object", qualifiedName ?: "Object")

@OptIn(ExperimentalForeignApi::class)
class PdMutex {
    private val m = nativeHeap.alloc<pthread_mutex_t>().also { pthread_mutex_init(it.ptr, null) }
    fun lock() { pthread_mutex_lock(m.ptr) }
    fun unlock() { pthread_mutex_unlock(m.ptr) }
}

/** A small number per thread (1, 2, 3 ...), for the re-entrant lock's owner. */
@kotlin.native.concurrent.ThreadLocal
object PdThreadId {
    private val next = AtomicInt(0)
    private var id = 0
    fun current(): Int {
        if (id == 0) id = next.incrementAndGet()
        return id
    }
}

/** `(x as Object).wait()` / `.notify()` on a plain Any (prepSrc rewrites the cast): on the JVM every object is a monitor,
 *  here a java.lang.Object per lock object is kept for it. */
private val pdMonitors = HashMap<Any, java.lang.Object>()
fun pdMonitor(o: Any): java.lang.Object = o as? java.lang.Object ?: synchronized(pdMonitors) { pdMonitors.getOrPut(o) { java.lang.Object() } }
