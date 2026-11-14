@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.lang

import kotlinx.cinterop.*
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.posix.*
import kotlin.math.pow

fun interface Runnable { fun run() }

open class InterruptedException(message: String? = null) : Exception(message)
open class CloneNotSupportedException(message: String? = null) : Exception(message)
open class SecurityException(message: String? = null) : RuntimeException(message)
typealias NumberFormatException = kotlin.NumberFormatException
interface Cloneable
typealias AutoCloseable = kotlin.AutoCloseable

/** java.lang.Thread over pthreads: start / join / sleep / interrupt (a flag), daemon threads (all are). */
open class Thread(private val target: Runnable? = null, var name: String = "Thread") {
    constructor(target: Runnable?) : this(target, "Thread")
    constructor(block: () -> Unit) : this(Runnable { block() }, "Thread")
    constructor(name: String) : this(null, name)
    constructor(group: Any?, target: Runnable?, name: String, stackSize: Long) : this(target, name)

    var isDaemon = true
    var priority = NORM_PRIORITY
    @kotlin.concurrent.Volatile var isInterrupted = false
        private set
    @kotlin.concurrent.Volatile private var running = false
    @kotlin.concurrent.Volatile private var finished = false
    private var handle: pthread_t? = null
    var uncaughtExceptionHandler: UncaughtExceptionHandler? = null

    val isAlive: Boolean get() = running && !finished
    val id: Long get() = handle.rawValue.toLong()

    open fun run() { target?.run() }

    fun start() {
        running = true
        val ref = kotlinx.cinterop.StableRef.create(this)
        memScoped {
            val t = alloc<pthread_tVar>()
            pthread_create(t.ptr, null, staticCFunction { arg: COpaquePointer? ->
                val r = arg!!.asStableRef<Thread>()
                val th = r.get()
                r.dispose()
                currentThreadRef = th
                try { th.run() } catch (e: Throwable) {
                    val h = th.uncaughtExceptionHandler ?: defaultUncaughtExceptionHandler
                    if (h != null) h.uncaughtException(th, e) else e.printStackTrace()
                }
                th.finished = true
                null
            }, ref.asCPointer())
            handle = t.value
            pthread_detach(t.value)
        }
    }

    fun join() { while (running && !finished) usleep(1000u) }
    fun join(millis: Long) {
        val end = System.currentTimeMillis() + millis
        while (running && !finished && System.currentTimeMillis() < end) usleep(1000u)
    }
    fun interrupt() { isInterrupted = true }
    fun setName(n: String) { name = n }
    fun getName() = name

    fun interface UncaughtExceptionHandler { fun uncaughtException(t: Thread, e: Throwable) }

    companion object {
        const val MIN_PRIORITY = 1
        const val NORM_PRIORITY = 5
        const val MAX_PRIORITY = 10
        var defaultUncaughtExceptionHandler: UncaughtExceptionHandler? = null
        fun currentThread(): Thread = currentThreadRef ?: Thread(null, "main").also { currentThreadRef = it }
        fun sleep(ms: Long) { if (ms > 0) usleep((ms * 1000).toUInt()) }
        fun sleep(ms: Long, nanos: Int) = sleep(ms)
        fun interrupted(): Boolean {
            val t = currentThread()
            val was = t.isInterrupted
            t.clearInterrupt()
            return was
        }
        fun yield() { sched_yield() }
        fun onSpinWait() {}
    }
    private fun clearInterrupt() { isInterrupted = false }
}

@kotlin.native.concurrent.ThreadLocal
private var currentThreadRef: Thread? = null

object System {
    fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
    fun nanoTime(): Long = memScoped {
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr)
        ts.tv_sec * 1_000_000_000L + ts.tv_nsec
    }
    fun arraycopy(src: Any, srcPos: Int, dest: Any, destPos: Int, length: Int) {
        when (src) {
            is ByteArray -> src.copyInto(dest as ByteArray, destPos, srcPos, srcPos + length)
            is IntArray -> src.copyInto(dest as IntArray, destPos, srcPos, srcPos + length)
            is ShortArray -> src.copyInto(dest as ShortArray, destPos, srcPos, srcPos + length)
            is LongArray -> src.copyInto(dest as LongArray, destPos, srcPos, srcPos + length)
            is CharArray -> src.copyInto(dest as CharArray, destPos, srcPos, srcPos + length)
            is FloatArray -> src.copyInto(dest as FloatArray, destPos, srcPos, srcPos + length)
            is DoubleArray -> src.copyInto(dest as DoubleArray, destPos, srcPos, srcPos + length)
            is BooleanArray -> src.copyInto(dest as BooleanArray, destPos, srcPos, srcPos + length)
            is Array<*> -> @Suppress("UNCHECKED_CAST") (src as Array<Any?>).copyInto(dest as Array<Any?>, destPos, srcPos, srcPos + length)
            else -> error("arraycopy: ${src::class}")
        }
    }
    private val props = HashMap<String, String>()
    fun getProperty(key: String): String? = props[key] ?: when (key) {
        "line.separator" -> "\n"
        "file.separator" -> "/"
        "os.name" -> "iOS"
        "user.dir" -> platform.Foundation.NSBundle.mainBundle.resourcePath
        "java.io.tmpdir" -> platform.Foundation.NSTemporaryDirectory()
        else -> null
    }
    fun getProperty(key: String, def: String): String = getProperty(key) ?: def
    fun setProperty(key: String, value: String): String? = props.put(key, value)
    fun getenv(key: String): String? = platform.posix.getenv(key)?.toKString()
    fun lineSeparator() = "\n"
    fun identityHashCode(o: Any?): Int = o?.hashCode() ?: 0
    fun gc() {}
    fun exit(status: Int) { platform.posix.exit(status) }
    fun loadLibrary(name: String) {} // the native code is linked into the app
    val out = PrintStream()
    val err = PrintStream()
}

class PrintStream {
    fun println(x: Any?) = kotlin.io.println(x)
    fun println() = kotlin.io.println()
    fun print(x: Any?) = kotlin.io.print(x)
    fun printf(f: String, vararg a: Any?) = kotlin.io.print(kotlin.text.pdFormat(f, a))
    fun flush() {}
}

object Math {
    fun max(a: Int, b: Int) = kotlin.math.max(a, b)
    fun max(a: Long, b: Long) = kotlin.math.max(a, b)
    fun max(a: Float, b: Float) = kotlin.math.max(a, b)
    fun max(a: Double, b: Double) = kotlin.math.max(a, b)
    fun min(a: Int, b: Int) = kotlin.math.min(a, b)
    fun min(a: Long, b: Long) = kotlin.math.min(a, b)
    fun min(a: Float, b: Float) = kotlin.math.min(a, b)
    fun min(a: Double, b: Double) = kotlin.math.min(a, b)
    fun abs(a: Int) = kotlin.math.abs(a)
    fun abs(a: Long) = kotlin.math.abs(a)
    fun abs(a: Float) = kotlin.math.abs(a)
    fun abs(a: Double) = kotlin.math.abs(a)
    fun floorMod(a: Int, b: Int) = a.mod(b)
    fun floorMod(a: Long, b: Long) = a.mod(b)
    fun floorDiv(a: Int, b: Int) = kotlin.math.floor(a.toDouble() / b).toInt()
    fun floorDiv(a: Long, b: Long) = (a - a.mod(b)) / b
    fun round(a: Double): Long = kotlin.math.floor(a + 0.5).toLong()
    fun round(a: Float): Int = kotlin.math.floor(a + 0.5f).toInt()
    fun floor(a: Double) = kotlin.math.floor(a)
    fun ceil(a: Double) = kotlin.math.ceil(a)
    fun sqrt(a: Double) = kotlin.math.sqrt(a)
    fun pow(a: Double, b: Double): Double = a.pow(b)
    fun log(a: Double) = kotlin.math.ln(a)
    fun log10(a: Double) = kotlin.math.log10(a)
    fun exp(a: Double) = kotlin.math.exp(a)
    fun sin(a: Double) = kotlin.math.sin(a)
    fun cos(a: Double) = kotlin.math.cos(a)
    fun tan(a: Double) = kotlin.math.tan(a)
    fun atan2(y: Double, x: Double) = kotlin.math.atan2(y, x)
    fun hypot(x: Double, y: Double) = kotlin.math.hypot(x, y)
    fun signum(a: Double) = kotlin.math.sign(a)
    fun signum(a: Float) = kotlin.math.sign(a)
    fun toRadians(a: Double) = a * kotlin.math.PI / 180.0
    fun toDegrees(a: Double) = a * 180.0 / kotlin.math.PI
    fun random() = kotlin.random.Random.nextDouble()
    fun addExact(a: Int, b: Int): Int { val r = a.toLong() + b; if (r != r.toInt().toLong()) throw ArithmeticException("overflow"); return r.toInt() }
    fun multiplyExact(a: Int, b: Int): Int { val r = a.toLong() * b; if (r != r.toInt().toLong()) throw ArithmeticException("overflow"); return r.toInt() }
    fun toIntExact(a: Long): Int { if (a != a.toInt().toLong()) throw ArithmeticException("overflow"); return a.toInt() }
    fun clamp(v: Long, lo: Int, hi: Int): Int = v.coerceIn(lo.toLong(), hi.toLong()).toInt()
    fun clamp(v: Double, lo: Double, hi: Double): Double = v.coerceIn(lo, hi)
    fun clamp(v: Float, lo: Float, hi: Float): Float = v.coerceIn(lo, hi)
    const val PI = kotlin.math.PI
    const val E = kotlin.math.E
}

object JLong {
    const val MAX_VALUE = kotlin.Long.MAX_VALUE
    const val MIN_VALUE = kotlin.Long.MIN_VALUE
    fun toHexString(v: kotlin.Long): String = v.toULong().toString(16)
    fun toBinaryString(v: kotlin.Long): String = v.toULong().toString(2)
    fun toString(v: kotlin.Long, radix: Int = 10): String = v.toString(radix)
    fun toUnsignedString(v: kotlin.Long): String = v.toULong().toString()
    fun parseLong(s: String, radix: Int = 10): kotlin.Long = s.toLong(radix)
    fun parseUnsignedLong(s: String, radix: Int = 10): kotlin.Long = s.toULong(radix).toLong()
    fun valueOf(v: kotlin.Long) = v
    fun bitCount(v: kotlin.Long) = v.countOneBits()
    fun numberOfTrailingZeros(v: kotlin.Long) = v.countTrailingZeroBits()
    fun numberOfLeadingZeros(v: kotlin.Long) = v.countLeadingZeroBits()
    fun compare(a: kotlin.Long, b: kotlin.Long) = a.compareTo(b)
    fun compareUnsigned(a: kotlin.Long, b: kotlin.Long) = a.toULong().compareTo(b.toULong())
    fun signum(v: kotlin.Long) = if (v > 0) 1 else if (v < 0) -1 else 0
    fun reverseBytes(v: kotlin.Long): kotlin.Long { var r = 0L; var x = v; repeat(8) { r = (r shl 8) or (x and 0xFF); x = x ushr 8 }; return r }
    fun rotateLeft(v: kotlin.Long, d: Int) = v.rotateLeft(d)
    fun hashCode(v: kotlin.Long) = v.hashCode()
    fun sum(a: kotlin.Long, b: kotlin.Long) = a + b
    fun max(a: kotlin.Long, b: kotlin.Long) = kotlin.math.max(a, b)
}

object Integer {
    fun decode(s: String): Int {
        val t = s.trim(); val neg = t.startsWith("-"); val u = t.removePrefix("-").removePrefix("+")
        val v = when { u.startsWith("0x") || u.startsWith("0X") -> u.substring(2).toLong(16); u.startsWith("#") -> u.substring(1).toLong(16)
            u.length > 1 && u.startsWith("0") -> u.substring(1).toLong(8); else -> u.toLong() }
        return (if (neg) -v else v).toInt()
    }
    const val MAX_VALUE = Int.MAX_VALUE
    const val MIN_VALUE = Int.MIN_VALUE
    fun toHexString(i: Int): String = i.toUInt().toString(16)
    fun toBinaryString(i: Int): String = i.toUInt().toString(2)
    fun toOctalString(i: Int): String = i.toUInt().toString(8)
    fun toString(i: Int, radix: Int = 10): String = i.toString(radix)
    fun parseInt(s: String, radix: Int = 10): Int = s.toInt(radix)
    fun parseUnsignedInt(s: String, radix: Int = 10): Int = s.toUInt(radix).toInt()
    fun valueOf(i: Int) = i
    fun valueOf(s: String) = s.toInt()
    fun bitCount(i: Int) = i.countOneBits()
    fun reverse(i: Int): Int { var r = 0; var v = i; repeat(32) { r = (r shl 1) or (v and 1); v = v ushr 1 }; return r }
    fun reverseBytes(i: Int): Int = ((i and 0xFF) shl 24) or ((i and 0xFF00) shl 8) or ((i ushr 8) and 0xFF00) or (i ushr 24)
    fun numberOfLeadingZeros(i: Int) = i.countLeadingZeroBits()
    fun numberOfTrailingZeros(i: Int) = i.countTrailingZeroBits()
    fun highestOneBit(i: Int) = i.takeHighestOneBit()
    fun lowestOneBit(i: Int) = i.takeLowestOneBit()
    fun signum(i: Int) = i.sign
    fun compare(a: Int, b: Int) = a.compareTo(b)
    fun compareUnsigned(a: Int, b: Int) = a.toUInt().compareTo(b.toUInt())
    fun toUnsignedLong(i: Int) = i.toLong() and 0xFFFFFFFFL
    fun rotateLeft(i: Int, d: Int) = i.rotateLeft(d)
    fun rotateRight(i: Int, d: Int) = i.rotateRight(d)
    fun sum(a: Int, b: Int) = a + b
    fun max(a: Int, b: Int) = kotlin.math.max(a, b)
    fun min(a: Int, b: Int) = kotlin.math.min(a, b)
    private val Int.sign get() = if (this > 0) 1 else if (this < 0) -1 else 0
}

object Character {
    fun isDigit(c: Char) = c.isDigit()
    fun isLetter(c: Char) = c.isLetter()
    fun isLetterOrDigit(c: Char) = c.isLetterOrDigit()
    fun isWhitespace(c: Char) = c.isWhitespace()
    fun isUpperCase(c: Char) = c.isUpperCase()
    fun isLowerCase(c: Char) = c.isLowerCase()
    fun isSpaceChar(c: Char) = c == ' ' || c == ' '
    fun toUpperCase(c: Char) = c.uppercaseChar()
    fun toLowerCase(c: Char) = c.lowercaseChar()
    fun toChars(cp: Int): CharArray = if (cp < 0x10000) charArrayOf(cp.toChar()) else {
        val v = cp - 0x10000
        charArrayOf((0xD800 + (v shr 10)).toChar(), (0xDC00 + (v and 0x3FF)).toChar())
    }
    fun charCount(cp: Int) = if (cp >= 0x10000) 2 else 1
    fun isHighSurrogate(c: Char) = c.isHighSurrogate()
    fun isLowSurrogate(c: Char) = c.isLowSurrogate()
    fun toCodePoint(h: Char, l: Char) = ((h.code - 0xD800) shl 10) + (l.code - 0xDC00) + 0x10000
    fun getNumericValue(c: Char) = c.digitToIntOrNull(36) ?: -1
    fun digit(c: Char, radix: Int) = c.digitToIntOrNull(radix) ?: -1
    fun forDigit(d: Int, radix: Int) = d.digitToChar(radix)
    fun isISOControl(c: Char) = c.isISOControl()
    fun isSurrogate(c: Char) = c.isSurrogate()
    fun getType(c: Char): Int = c.category.ordinal
    fun isDefined(c: Char) = c.isDefined()
    const val MIN_VALUE = '\u0000'
    const val MAX_VALUE = '￿'
    const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
    const val NON_SPACING_MARK = 6
}

abstract class Process

open class LinkageError(message: String? = null) : Error(message)
open class UnsatisfiedLinkError(message: String? = null) : LinkageError(message)
open class NoSuchMethodException(message: String? = null) : Exception(message)
open class NoSuchFieldException(message: String? = null) : Exception(message)
open class ClassNotFoundException(message: String? = null) : Exception(message)

/** java.lang.reflect.Field's getInt for the classes whose constants the app looks up by name (KeyEvent's KEYCODE_*). */
class Field internal constructor(val name: String, private val value: Int) {
    fun getInt(obj: Any?): Int = value
    var isAccessible = true
}

class Class<T>(val simpleName: String, val name: String) {
    fun getField(field: String): Field {
        val v = if (simpleName == "KeyEvent") android.view.KeyEvent.fieldValue(field) else null
        return Field(field, v ?: throw NoSuchFieldException(field))
    }
    fun getDeclaredField(field: String) = getField(field)
    fun getMethod(name: String, vararg types: Any?): java.lang.reflect.Method = throw NoSuchMethodException(name)
    fun getDeclaredMethod(name: String, vararg types: Any?): java.lang.reflect.Method = throw NoSuchMethodException(name)
    val canonicalName: String get() = name
    fun getSimpleName() = simpleName
    fun getName() = name
    override fun toString() = "class $name"
    override fun equals(other: Any?) = other is Class<*> && other.name == name
    override fun hashCode() = name.hashCode()
}

/**
 * java.lang.Object's monitor methods for objects used as locks (synchronized(lock) { lock.wait() }): wait gives up
 * synchronized's process-wide lock while it sleeps, notify / notifyAll wake its waiters.
 */
open class Object {
    private val cond = platform.Foundation.NSCondition()
    fun wait() = wait(0L)
    fun wait(ms: Long) {
        cond.lock()
        val depth = kotlin.pdLock.releaseAll()
        if (ms <= 0) cond.wait() else cond.waitUntilDate(platform.Foundation.NSDate.dateWithTimeIntervalSinceNow(ms / 1000.0))
        cond.unlock()
        kotlin.pdLock.reacquire(depth)
    }
    fun wait(ms: Long, nanos: Int) = wait(if (ms == 0L && nanos > 0) 1L else ms)
    fun notify() { cond.lock(); cond.signal(); cond.unlock() }
    fun notifyAll() { cond.lock(); cond.broadcast(); cond.unlock() }
}
