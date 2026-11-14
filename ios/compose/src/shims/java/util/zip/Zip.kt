@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.util.zip

import kotlinx.cinterop.*
import platform.zlib.*

interface Checksum { fun update(b: Int); fun update(b: ByteArray, off: Int, len: Int); val value: Long; fun reset() }

class CRC32 : Checksum {
    private var crc = 0.inv()
    override fun update(b: Int) { crc = TABLE[(crc xor b) and 0xFF] xor (crc ushr 8) }
    fun update(b: ByteArray) = update(b, 0, b.size)
    override fun update(b: ByteArray, off: Int, len: Int) {
        var c = crc
        for (i in off until off + len) c = TABLE[(c xor b[i].toInt()) and 0xFF] xor (c ushr 8)
        crc = c
    }
    fun update(buf: java.nio.ByteBuffer) { val n = buf.remaining(); val b = ByteArray(n); buf.get(b); update(b) }
    override val value: Long get() = crc.inv().toLong() and 0xFFFFFFFFL
    override fun reset() { crc = 0.inv() }
    companion object {
        private val TABLE = IntArray(256) { n -> var c = n; repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }; c }
    }
}

class Adler32 : Checksum {
    private var a = 1L; private var b = 0L
    override fun update(x: Int) { a = (a + (x and 0xFF)) % 65521; b = (b + a) % 65521 }
    override fun update(buf: ByteArray, off: Int, len: Int) { for (i in off until off + len) update(buf[i].toInt()) }
    fun update(buf: ByteArray) = update(buf, 0, buf.size)
    override val value get() = (b shl 16) or a
    override fun reset() { a = 1; b = 0 }
}

open class DataFormatException(message: String? = null) : Exception(message)
open class ZipException(message: String? = null) : java.io.IOException(message)

/** zlib's deflate (nowrap = raw). */
class Deflater(level: Int = DEFAULT_COMPRESSION, private val nowrap: Boolean = false) {
    private var level = level
    private var input = ByteArray(0)
    private var inPos = 0
    private var finishing = false
    private var done = false
    private val out = java.io.ByteArrayOutputStream()
    private var outPos = 0
    fun setLevel(l: Int) { level = l }
    fun setInput(b: ByteArray) = setInput(b, 0, b.size)
    fun setInput(b: ByteArray, off: Int, len: Int) { input = input.copyOfRange(inPos, input.size) + b.copyOfRange(off, off + len); inPos = 0 }
    fun finish() { finishing = true }
    fun needsInput() = !finishing && inPos >= input.size
    fun finished() = done && outPos >= out.size()
    fun deflate(b: ByteArray): Int = deflate(b, 0, b.size)
    fun deflate(b: ByteArray, off: Int, len: Int, flush: Int = NO_FLUSH): Int {
        if (finishing && !done) { compressAll(); done = true }
        val all = out.toByteArray()
        val n = minOf(len, all.size - outPos)
        if (n <= 0) return 0
        all.copyInto(b, off, outPos, outPos + n)
        outPos += n
        return n
    }
    private fun compressAll() = memScoped {
        val src = input.copyOfRange(inPos, input.size); inPos = input.size
        val s = alloc<z_stream>()
        deflateInit2_(s.ptr, level, Z_DEFLATED, if (nowrap) -15 else 15, 8, Z_DEFAULT_STRATEGY, zlibVersion()?.toKString(), sizeOf<z_stream>().toInt())
        val buf = ByteArray(65536)
        src.usePinned { sp ->
            s.next_in = if (src.isEmpty()) null else sp.addressOf(0).reinterpret()
            s.avail_in = src.size.toUInt()
            buf.usePinned { bp ->
                do {
                    s.next_out = bp.addressOf(0).reinterpret(); s.avail_out = buf.size.toUInt()
                    val r = platform.zlib.deflate(s.ptr, Z_FINISH)
                    out.write(buf, 0, buf.size - s.avail_out.toInt())
                } while (r == Z_OK)
            }
        }
        deflateEnd(s.ptr)
    }
    fun reset() { input = ByteArray(0); inPos = 0; finishing = false; done = false; out.reset(); outPos = 0 }
    fun end() {}
    val totalIn: Int get() = input.size
    val bytesRead: Long get() = input.size.toLong()
    companion object {
        const val DEFAULT_COMPRESSION = -1
        const val BEST_COMPRESSION = 9
        const val BEST_SPEED = 1
        const val NO_COMPRESSION = 0
        const val DEFLATED = 8
        const val NO_FLUSH = 0
        const val SYNC_FLUSH = 2
        const val FULL_FLUSH = 3
    }
}

/** zlib's inflate (nowrap = raw), fed in pieces. */
class Inflater(private val nowrap: Boolean = false) {
    private val s = nativeHeap.alloc<z_stream>().also {
        inflateInit2_(it.ptr, if (nowrap) -15 else 15, zlibVersion()?.toKString(), sizeOf<z_stream>().toInt())
    }
    private var input = ByteArray(0)
    private var inOff = 0
    private var inLen = 0
    private var finished = false
    private var totalOut = 0L
    fun setInput(b: ByteArray) = setInput(b, 0, b.size)
    fun setInput(b: ByteArray, off: Int, len: Int) { input = b; inOff = off; inLen = len }
    fun needsInput() = inLen <= 0
    fun finished() = finished
    fun needsDictionary() = false
    val remaining: Int get() = inLen
    fun getRemaining() = inLen
    fun inflate(b: ByteArray): Int = inflate(b, 0, b.size)
    fun inflate(b: ByteArray, off: Int, len: Int): Int {
        if (finished || len == 0) return 0
        var produced = 0
        input.usePinned { ip ->
            b.usePinned { bp ->
                s.next_in = if (inLen > 0) ip.addressOf(inOff).reinterpret() else null
                s.avail_in = inLen.toUInt()
                s.next_out = bp.addressOf(off).reinterpret()
                s.avail_out = len.toUInt()
                val r = platform.zlib.inflate(s.ptr, Z_NO_FLUSH)
                val used = inLen - s.avail_in.toInt()
                inOff += used; inLen -= used
                produced = len - s.avail_out.toInt()
                if (r == Z_STREAM_END) finished = true
                else if (r != Z_OK && r != Z_BUF_ERROR) throw DataFormatException("inflate: $r")
            }
        }
        totalOut += produced
        return produced
    }
    val bytesWritten: Long get() = totalOut
    fun reset() { inflateReset(s.ptr); finished = false; inLen = 0; totalOut = 0 }
    fun end() { inflateEnd(s.ptr) }
}

open class InflaterInputStream(input: java.io.InputStream, protected val inf: Inflater = Inflater(), size: Int = 8192) : java.io.FilterInputStream(input) {
    private val buf = ByteArray(size)
    override fun read(): Int { val b = ByteArray(1); return if (read(b, 0, 1) <= 0) -1 else b[0].toInt() and 0xFF }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        while (true) {
            if (inf.finished()) return -1
            if (inf.needsInput()) {
                val n = `in`.read(buf, 0, buf.size)
                if (n < 0) return -1
                inf.setInput(buf, 0, n)
            }
            val r = inf.inflate(b, off, len)
            if (r > 0) return r
        }
    }
}

open class DeflaterOutputStream(out: java.io.OutputStream, protected val def: Deflater = Deflater()) : java.io.FilterOutputStream(out) {
    private val pending = java.io.ByteArrayOutputStream()
    override fun write(b: Int) = pending.write(b)
    override fun write(b: ByteArray, off: Int, len: Int) = pending.write(b, off, len)
    fun finish() {
        def.setInput(pending.toByteArray()); def.finish()
        val buf = ByteArray(65536)
        while (!def.finished()) { val n = def.deflate(buf); if (n == 0) break; out.write(buf, 0, n) }
        pending.reset()
    }
    override fun close() { finish(); out.close() }
}

class GZIPInputStream(input: java.io.InputStream, size: Int = 8192) : InflaterInputStream(skipHeader(input), Inflater(true), size) {
    companion object {
        private fun skipHeader(i: java.io.InputStream): java.io.InputStream {
            val h = ByteArray(10); var got = 0
            while (got < 10) { val n = i.read(h, got, 10 - got); if (n < 0) throw ZipException("not gzip"); got += n }
            val flg = h[3].toInt()
            if (flg and 4 != 0) { val lo = i.read(); val hi = i.read(); i.skip(((hi shl 8) or lo).toLong()) }
            if (flg and 8 != 0) while (i.read() > 0) {}
            if (flg and 16 != 0) while (i.read() > 0) {}
            if (flg and 2 != 0) i.skip(2)
            return i
        }
    }
}

class GZIPOutputStream(out: java.io.OutputStream) : DeflaterOutputStream(out, Deflater(Deflater.DEFAULT_COMPRESSION, true)) {
    private val crc = CRC32()
    private var size = 0L
    init { out.write(byteArrayOf(0x1f, 0x8b.toByte(), 8, 0, 0, 0, 0, 0, 0, 3)) }
    override fun write(b: ByteArray, off: Int, len: Int) { crc.update(b, off, len); size += len; super.write(b, off, len) }
    override fun write(b: Int) { crc.update(b); size++; super.write(b) }
    override fun close() {
        finish()
        val v = crc.value
        for (k in 0..3) out.write((v shr (8 * k)).toInt())
        for (k in 0..3) out.write((size shr (8 * k)).toInt())
        out.close()
    }
}

open class ZipEntry(val name: String) {
    var size: Long = -1
    var compressedSize: Long = -1
    var crc: Long = -1
    var method: Int = -1
    var time: Long = 0
    var comment: String? = null
    var extra: ByteArray? = null
    val isDirectory: Boolean get() = name.endsWith("/")
    internal var dataOffset = 0L
    override fun toString() = name
    companion object { const val STORED = 0; const val DEFLATED = 8 }
}

/** Reads a .zip from its local headers in order (stored and deflated entries; sizes from the central directory when a header has the data-descriptor bit). */
open class ZipInputStream(private val input: java.io.InputStream) : java.io.InputStream() {
    private val data: ByteArray by lazy { input.readAllBytes() }
    private val entries: List<ZipEntry> by lazy { ZipFile.parse(data) }
    private var index = -1
    private var current: java.io.InputStream? = null
    open val nextEntry: ZipEntry? get() = getNextEntry()
    fun getNextEntry(): ZipEntry? {
        index++
        val e = entries.getOrNull(index) ?: run { current = null; return null }
        current = ZipFile.open(data, e)
        return e
    }
    fun closeEntry() { current = null }
    override fun read(): Int = current?.read() ?: -1
    override fun read(b: ByteArray, off: Int, len: Int): Int = current?.read(b, off, len) ?: -1
    override fun close() = input.close()
}

open class ZipFile(file: java.io.File) : java.io.Closeable {
    constructor(path: String) : this(java.io.File(path))
    private val data = file.readBytes()
    private val list = parse(data)
    fun entries(): Iterator<ZipEntry> = list.iterator()
    fun stream(): Sequence<ZipEntry> = list.asSequence()
    fun getEntry(name: String): ZipEntry? = list.firstOrNull { it.name == name }
    fun getInputStream(e: ZipEntry): java.io.InputStream = open(data, e)
    fun size() = list.size
    override fun close() {}

    companion object {
        private fun u16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
        private fun u32(b: ByteArray, o: Int) = (u16(b, o).toLong()) or (u16(b, o + 2).toLong() shl 16)
        internal fun parse(b: ByteArray): List<ZipEntry> {
            // the central directory: its end record within the last 64 KB
            var eocd = -1
            for (i in b.size - 22 downTo maxOf(0, b.size - 65557)) if (u32(b, i) == 0x06054b50L) { eocd = i; break }
            if (eocd < 0) throw ZipException("no central directory")
            val count = u16(b, eocd + 10)
            var p = u32(b, eocd + 16).toInt()
            val out = ArrayList<ZipEntry>(count)
            repeat(count) {
                if (u32(b, p) != 0x02014b50L) throw ZipException("bad central entry")
                val method = u16(b, p + 10)
                val crc = u32(b, p + 16)
                val csize = u32(b, p + 20)
                val usize = u32(b, p + 24)
                val nlen = u16(b, p + 28); val xlen = u16(b, p + 30); val clen = u16(b, p + 32)
                val local = u32(b, p + 42).toInt()
                val name = b.decodeToString(p + 46, p + 46 + nlen)
                val e = ZipEntry(name)
                e.method = method; e.crc = crc; e.compressedSize = csize; e.size = usize
                e.dataOffset = (local + 30 + u16(b, local + 26) + u16(b, local + 28)).toLong()
                out.add(e)
                p += 46 + nlen + xlen + clen
            }
            return out
        }
        internal fun open(b: ByteArray, e: ZipEntry): java.io.InputStream {
            val start = e.dataOffset.toInt()
            val raw = java.io.ByteArrayInputStream(b, start, e.compressedSize.toInt())
            return if (e.method == ZipEntry.STORED) raw else InflaterInputStream(raw, Inflater(true))
        }
    }
}

class ZipOutputStream(out: java.io.OutputStream) : java.io.FilterOutputStream(out) {
    private class Item(val name: String, val crc: Long, val csize: Int, val usize: Int, val offset: Int)
    private val items = ArrayList<Item>()
    private var name: String? = null
    private val buf = java.io.ByteArrayOutputStream()
    private var pos = 0
    private fun w16(v: Int) { out.write(v); out.write(v shr 8); pos += 2 }
    private fun w32(v: Long) { w16(v.toInt() and 0xFFFF); w16((v shr 16).toInt() and 0xFFFF) }
    fun putNextEntry(e: ZipEntry) { closeEntry(); name = e.name; buf.reset() }
    override fun write(b: Int) = buf.write(b)
    override fun write(b: ByteArray, off: Int, len: Int) = buf.write(b, off, len)
    fun closeEntry() {
        val n = name ?: return
        val data = buf.toByteArray()
        val crc = CRC32().also { it.update(data) }.value
        val d = Deflater(Deflater.DEFAULT_COMPRESSION, true); d.setInput(data); d.finish()
        val comp = java.io.ByteArrayOutputStream(); val tmp = ByteArray(65536)
        while (!d.finished()) { val k = d.deflate(tmp); if (k == 0) break; comp.write(tmp, 0, k) }
        val cdata = comp.toByteArray()
        val nb = n.encodeToByteArray()
        val off = pos
        w32(0x04034b50L); w16(20); w16(0x0800); w16(8); w16(0); w16(0); w32(crc); w32(cdata.size.toLong()); w32(data.size.toLong()); w16(nb.size); w16(0)
        out.write(nb); pos += nb.size
        out.write(cdata); pos += cdata.size
        items.add(Item(n, crc, cdata.size, data.size, off))
        name = null
    }
    fun finish() {
        closeEntry()
        val cdStart = pos
        for (it in items) {
            val nb = it.name.encodeToByteArray()
            w32(0x02014b50L); w16(20); w16(20); w16(0x0800); w16(8); w16(0); w16(0); w32(it.crc); w32(it.csize.toLong()); w32(it.usize.toLong())
            w16(nb.size); w16(0); w16(0); w16(0); w16(0); w32(0); w32(it.offset.toLong())
            out.write(nb); pos += nb.size
        }
        val cdSize = pos - cdStart
        w32(0x06054b50L); w16(0); w16(0); w16(items.size); w16(items.size); w32(cdSize.toLong()); w32(cdStart.toLong()); w16(0)
    }
    override fun close() { finish(); out.close() }
}
