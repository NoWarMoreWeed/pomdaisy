@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.nio.channels

import kotlinx.cinterop.*
import platform.posix.*

/** FileChannel over the stream's FILE: size / position / read / write / force / lock (a no-op lock). */
class FileChannel internal constructor(private val f: CPointer<FILE>, private val readOnly: Boolean) : java.io.Closeable {
    fun size(): Long { val cur = ftello(f); fseeko(f, 0, SEEK_END); val n = ftello(f); fseeko(f, cur, SEEK_SET); return n }
    fun position(): Long = ftello(f)
    fun position(p: Long): FileChannel { fseeko(f, p, SEEK_SET); return this }
    fun read(dst: java.nio.ByteBuffer): Int {
        val n = dst.remaining()
        val tmp = ByteArray(n)
        val got = if (n == 0) 0 else tmp.usePinned { p -> fread(p.addressOf(0), 1u, n.toULong(), f).toInt() }
        if (got <= 0) return -1
        dst.put(tmp, 0, got)
        return got
    }
    fun read(dst: java.nio.ByteBuffer, pos: Long): Int { val cur = ftello(f); fseeko(f, pos, SEEK_SET); val r = read(dst); fseeko(f, cur, SEEK_SET); return r }
    fun write(src: java.nio.ByteBuffer): Int {
        val n = src.remaining()
        val tmp = ByteArray(n)
        src.get(tmp)
        if (n > 0) tmp.usePinned { p -> fwrite(p.addressOf(0), 1u, n.toULong(), f) }
        return n
    }
    fun write(src: java.nio.ByteBuffer, pos: Long): Int { val cur = ftello(f); fseeko(f, pos, SEEK_SET); val r = write(src); fseeko(f, cur, SEEK_SET); return r }
    fun truncate(size: Long): FileChannel { fflush(f); ftruncate(fileno(f), size); return this }
    fun force(metaData: Boolean) { fflush(f); fsync(fileno(f)) }
    /** A "mapping" read into memory (READ_ONLY only: the app maps ROMs to scan them). */
    fun map(mode: MapMode, position: Long, size: Long): java.nio.ByteBuffer {
        val b = ByteArray(size.toInt())
        val cur = ftello(f); fseeko(f, position, SEEK_SET)
        if (size > 0) b.usePinned { p -> fread(p.addressOf(0), 1u, size.toULong(), f) }
        fseeko(f, cur, SEEK_SET)
        return java.nio.ByteBuffer.wrap(b)
    }
    enum class MapMode { READ_ONLY, READ_WRITE, PRIVATE }
    fun lock(): FileLock = FileLock()
    fun tryLock(): FileLock? = FileLock()
    fun transferTo(position: Long, count: Long, target: FileChannel): Long {
        val buf = java.nio.ByteBuffer.allocate(count.toInt())
        read(buf, position); buf.flip(); return target.write(buf).toLong()
    }
    val isOpen: Boolean get() = true
    override fun close() {}
}

class FileLock { fun release() {}; fun close() {}; val isValid get() = true }
