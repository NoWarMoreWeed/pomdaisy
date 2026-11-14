@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.nio

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.plus
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned

class ByteOrder private constructor(private val name: String) {
    override fun toString() = name
    companion object {
        val BIG_ENDIAN = ByteOrder("BIG_ENDIAN")
        val LITTLE_ENDIAN = ByteOrder("LITTLE_ENDIAN")
        fun nativeOrder() = LITTLE_ENDIAN
    }
}

open class BufferUnderflowException : RuntimeException()
open class BufferOverflowException : RuntimeException()

abstract class Buffer {
    abstract fun capacity(): Int
    abstract fun position(): Int
    abstract fun limit(): Int
    fun remaining() = limit() - position()
    fun hasRemaining() = position() < limit()
}

/** java.nio.ByteBuffer over a ByteArray, or (native) over memory the native code owns (the emulator's frame). */
class ByteBuffer private constructor(private val arr: ByteArray, private val off: Int, private val cap: Int,
    private val nat: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>? = null) : Buffer(), Comparable<ByteBuffer> {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun rb(i: Int): Byte = if (nat != null) nat[i] else arr[i]
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun wb(i: Int, v: Byte) { if (nat != null) nat[i] = v else arr[i] = v }
    /** The native memory under a direct buffer (null for a heap one). */
    val nativeAddress: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>? get() = nat
    private var pos = 0
    private var lim = cap
    private var mark = -1
    private var big = true

    override fun capacity() = cap
    override fun position() = pos
    override fun limit() = lim
    fun position(p: Int): ByteBuffer { pos = p; return this }
    fun limit(l: Int): ByteBuffer { lim = l; if (pos > l) pos = l; return this }
    fun clear(): ByteBuffer { pos = 0; lim = cap; mark = -1; return this }
    fun flip(): ByteBuffer { lim = pos; pos = 0; mark = -1; return this }
    fun rewind(): ByteBuffer { pos = 0; mark = -1; return this }
    fun mark(): ByteBuffer { mark = pos; return this }
    fun reset(): ByteBuffer { pos = mark; return this }
    fun compact(): ByteBuffer { val b = ByteArray(lim - pos); copyOut(off + pos, b, 0, b.size); copyIn(off, b, 0, b.size); pos = lim - pos; lim = cap; return this }
    fun order(): ByteOrder = if (big) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
    fun order(o: ByteOrder): ByteBuffer { big = o == ByteOrder.BIG_ENDIAN; return this }
    fun array(): ByteArray = if (nat == null) arr else throw UnsupportedOperationException("a direct buffer has no array")
    fun arrayOffset() = off
    fun hasArray() = nat == null
    val isDirect: Boolean get() = nat != null
    fun isReadOnly() = false
    fun duplicate(): ByteBuffer = ByteBuffer(arr, off, cap, nat).also { it.pos = pos; it.lim = lim; it.big = big }
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    fun slice(): ByteBuffer = if (nat != null) ByteBuffer(arr, 0, lim - pos, nat + (off + pos)).also { it.big = big } else ByteBuffer(arr, off + pos, lim - pos).also { it.big = big }
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    fun slice(index: Int, length: Int): ByteBuffer = if (nat != null) ByteBuffer(arr, 0, length, nat + (off + index)).also { it.big = big } else ByteBuffer(arr, off + index, length).also { it.big = big }
    fun asReadOnlyBuffer() = duplicate()

    private fun ix(i: Int, n: Int): Int { if (i < 0 || i + n > lim) throw IndexOutOfBoundsException("$i+$n > $lim"); return off + i }
    private fun next(n: Int): Int { if (pos + n > lim) throw BufferUnderflowException(); val i = off + pos; pos += n; return i }
    private fun nextW(n: Int): Int { if (pos + n > lim) throw BufferOverflowException(); val i = off + pos; pos += n; return i }

    private fun rd(i: Int, n: Int): Long {
        var v = 0L
        if (big) for (k in 0 until n) v = (v shl 8) or (rb(i + k).toLong() and 0xFF)
        else for (k in n - 1 downTo 0) v = (v shl 8) or (rb(i + k).toLong() and 0xFF)
        return v
    }
    private fun wr(i: Int, n: Int, v: Long) {
        if (big) for (k in 0 until n) wb(i + k, (v shr ((n - 1 - k) * 8)).toByte())
        else for (k in 0 until n) wb(i + k, (v shr (k * 8)).toByte())
    }

    fun get(): Byte = rb(next(1))
    fun get(i: Int): Byte = rb(ix(i, 1))
    fun get(dst: ByteArray): ByteBuffer = get(dst, 0, dst.size)
    fun get(dst: ByteArray, o: Int, n: Int): ByteBuffer { val i = next(n); copyOut(i, dst, o, n); return this }
    fun get(index: Int, dst: ByteArray, o: Int = 0, n: Int = dst.size): ByteBuffer { val i = ix(index, n); copyOut(i, dst, o, n); return this }
    fun put(b: Byte): ByteBuffer { wb(nextW(1), b); return this }
    fun put(i: Int, b: Byte): ByteBuffer { wb(ix(i, 1), b); return this }
    fun put(src: ByteArray): ByteBuffer = put(src, 0, src.size)
    fun put(src: ByteArray, o: Int, n: Int): ByteBuffer { val i = nextW(n); copyIn(i, src, o, n); return this }
    fun put(src: ByteBuffer): ByteBuffer { val n = src.remaining(); val b = ByteArray(n); src.get(b); return put(b) }
    fun put(index: Int, src: ByteArray, o: Int = 0, n: Int = src.size): ByteBuffer { val i = ix(index, n); copyIn(i, src, o, n); return this }
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun copyOut(i: Int, dst: ByteArray, o: Int, n: Int) {
        if (n <= 0) return
        if (nat == null) arr.copyInto(dst, o, i, i + n)
        else dst.usePinned { platform.posix.memcpy(it.addressOf(o), nat + i, n.toULong()) }
    }
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun copyIn(i: Int, src: ByteArray, o: Int, n: Int) {
        if (n <= 0) return
        if (nat == null) src.copyInto(arr, i, o, o + n)
        else src.usePinned { platform.posix.memcpy(nat + i, it.addressOf(o), n.toULong()) }
    }

    fun getShort(): Short = rd(next(2), 2).toShort()
    fun getShort(i: Int): Short = rd(ix(i, 2), 2).toShort()
    fun getChar(): Char = rd(next(2), 2).toInt().toChar()
    fun getChar(i: Int): Char = rd(ix(i, 2), 2).toInt().toChar()
    fun getInt(): Int = rd(next(4), 4).toInt()
    fun getInt(i: Int): Int = rd(ix(i, 4), 4).toInt()
    fun getLong(): Long = rd(next(8), 8)
    fun getLong(i: Int): Long = rd(ix(i, 8), 8)
    fun getFloat(): Float = Float.fromBits(getInt())
    fun getFloat(i: Int): Float = Float.fromBits(getInt(i))
    fun getDouble(): Double = Double.fromBits(getLong())
    fun getDouble(i: Int): Double = Double.fromBits(getLong(i))
    fun putShort(v: Short): ByteBuffer { wr(nextW(2), 2, v.toLong()); return this }
    fun putShort(i: Int, v: Short): ByteBuffer { wr(ix(i, 2), 2, v.toLong()); return this }
    fun putChar(v: Char): ByteBuffer { wr(nextW(2), 2, v.code.toLong()); return this }
    fun putInt(v: Int): ByteBuffer { wr(nextW(4), 4, v.toLong()); return this }
    fun putInt(i: Int, v: Int): ByteBuffer { wr(ix(i, 4), 4, v.toLong()); return this }
    fun putLong(v: Long): ByteBuffer { wr(nextW(8), 8, v); return this }
    fun putLong(i: Int, v: Long): ByteBuffer { wr(ix(i, 8), 8, v); return this }
    fun putFloat(v: Float): ByteBuffer = putInt(v.toRawBits())
    fun putFloat(i: Int, v: Float): ByteBuffer = putInt(i, v.toRawBits())
    fun putDouble(v: Double): ByteBuffer = putLong(v.toRawBits())

    // Java's getters as Kotlin sees them (buf.int, buf.short ...)
    val int: Int get() = getInt()
    val short: Short get() = getShort()
    val long: Long get() = getLong()
    val char: Char get() = getChar()
    val float: Float get() = getFloat()
    val double: Double get() = getDouble()
    fun asShortBuffer(): ShortBuffer = ShortBuffer(this)
    fun asIntBuffer(): IntBuffer = IntBuffer(this)

    override fun compareTo(other: ByteBuffer): Int {
        val n = minOf(remaining(), other.remaining())
        for (k in 0 until n) { val c = get(pos + k).compareTo(other.get(other.pos + k)); if (c != 0) return c }
        return remaining() - other.remaining()
    }

    companion object {
        fun allocate(n: Int) = ByteBuffer(ByteArray(n), 0, n)
        /** A direct buffer over native memory (the JNI glue's NewDirectByteBuffer). */
        fun native(p: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>, capacity: Int) = ByteBuffer(ByteArray(0), 0, capacity, p).also { it.big = false }
        fun allocateDirect(n: Int) = allocate(n)
        fun wrap(a: ByteArray) = ByteBuffer(a, 0, a.size)
        fun wrap(a: ByteArray, o: Int, n: Int) = ByteBuffer(a, 0, a.size).also { it.pos = o; it.lim = o + n }
    }
}

class ShortBuffer internal constructor(private val b: ByteBuffer) {
    private val base = b.position()
    private var p = 0
    fun get(): Short = b.getShort(base + 2 * p++)
    fun get(i: Int): Short = b.getShort(base + 2 * i)
    fun put(v: Short): ShortBuffer { b.putShort(base + 2 * p++, v); return this }
    fun put(i: Int, v: Short): ShortBuffer { b.putShort(base + 2 * i, v); return this }
    fun get(dst: ShortArray): ShortBuffer { for (k in dst.indices) dst[k] = get(); return this }
    fun put(src: ShortArray): ShortBuffer { for (v in src) put(v); return this }
    fun remaining() = (b.limit() - base) / 2 - p
    fun position(n: Int): ShortBuffer { p = n; return this }
}

class IntBuffer internal constructor(private val b: ByteBuffer) {
    private val base = b.position()
    private var p = 0
    fun get(): Int = b.getInt(base + 4 * p++)
    fun get(i: Int): Int = b.getInt(base + 4 * i)
    fun put(v: Int): IntBuffer { b.putInt(base + 4 * p++, v); return this }
    fun put(i: Int, v: Int): IntBuffer { b.putInt(base + 4 * i, v); return this }
    fun get(dst: IntArray): IntBuffer { for (k in dst.indices) dst[k] = get(); return this }
    fun put(src: IntArray): IntBuffer { for (v in src) put(v); return this }
    fun remaining() = (b.limit() - base) / 4 - p
    fun position(n: Int): IntBuffer { p = n; return this }
}

class CharBuffer(private val s: String) : CharSequence by s {
    override fun toString() = s
    companion object { fun wrap(c: CharSequence) = CharBuffer(c.toString()) }
}
