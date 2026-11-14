// java.io over posix: File, the streams, RandomAccessFile, readers / writers - the parts the app's code uses.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.io

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.text.Charset
import kotlin.text.Charsets

open class IOException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable?) : this(cause?.toString(), cause)
}
open class FileNotFoundException(message: String? = null) : IOException(message)
open class EOFException(message: String? = null) : IOException(message)
open class UncheckedIOException(message: String?, cause: IOException?) : RuntimeException(message, cause) {
    constructor(cause: IOException) : this(cause.message, cause)
}
open class UnsupportedEncodingException(message: String? = null) : IOException(message)
open class InterruptedIOException(message: String? = null) : IOException(message)
interface Serializable
interface Closeable : AutoCloseable
interface Flushable { fun flush() }

internal fun pdReadFile(path: String): ByteArray? {
    val f = fopen(path, "rb") ?: return null
    try {
        fseeko(f, 0, SEEK_END)
        val n = ftello(f)
        fseeko(f, 0, SEEK_SET)
        if (n < 0) return null
        val out = ByteArray(n.toInt())
        if (n > 0) out.usePinned { p -> if (fread(p.addressOf(0), 1u, n.toULong(), f).toLong() != n) return null }
        return out
    } finally {
        fclose(f)
    }
}

internal fun pdWriteFile(path: String, data: ByteArray, append: Boolean, offset: Int = 0, length: Int = data.size): Boolean {
    val f = fopen(path, if (append) "ab" else "wb") ?: return false
    var ok = true
    if (length > 0) data.usePinned { p -> ok = fwrite(p.addressOf(offset), 1u, length.toULong(), f).toInt() == length }
    ok = fclose(f) == 0 && ok
    return ok
}

private fun stat(path: String): stat? = memScoped {
    val st = alloc<stat>()
    if (platform.posix.stat(path, st.ptr) == 0) st else null
}

private inline fun <R> withStat(path: String, block: (stat?) -> R): R = memScoped {
    val st = alloc<stat>()
    block(if (platform.posix.stat(path, st.ptr) == 0) st else null)
}

open class File(pathname: String) : Comparable<File>, Serializable {
    constructor(parent: String?, child: String) : this(if (parent.isNullOrEmpty()) child else parent.trimEnd('/') + "/" + child)
    constructor(parent: File?, child: String) : this(parent?.path, child)

    val path: String = if (pathname.length > 1) pathname.trimEnd('/') else pathname
    val name: String get() = path.substringAfterLast('/')
    val parent: String? get() = path.substringBeforeLast('/', "").ifEmpty { if (path.startsWith("/") && path != "/") "/" else null }
    val parentFile: File? get() = parent?.let { File(it) }
    val absolutePath: String get() = if (path.startsWith("/")) path else (cwd() + "/" + path)
    val absoluteFile: File get() = File(absolutePath)
    val canonicalPath: String get() = absolutePath
    val canonicalFile: File get() = absoluteFile
    val isAbsolute: Boolean get() = path.startsWith("/")
    val isFile: Boolean get() = withStat(path) { it != null && (it.st_mode.toInt() and S_IFMT) == S_IFREG }
    val isDirectory: Boolean get() = withStat(path) { it != null && (it.st_mode.toInt() and S_IFMT) == S_IFDIR }
    val isHidden: Boolean get() = name.startsWith(".")

    fun exists(): Boolean = access(path, F_OK) == 0
    fun canRead(): Boolean = access(path, R_OK) == 0
    fun canWrite(): Boolean = access(path, W_OK) == 0 || (!exists() && parentFile?.canWrite() == true)
    fun length(): Long = withStat(path) { it?.st_size ?: 0L }
    fun lastModified(): Long = withStat(path) { if (it == null) 0L else it.st_mtimespec.tv_sec * 1000L + it.st_mtimespec.tv_nsec / 1_000_000L }
    fun setLastModified(time: Long): Boolean = memScoped {
        val times = allocArray<timeval>(2)
        for (i in 0..1) { times[i].tv_sec = time / 1000; times[i].tv_usec = ((time % 1000) * 1000).toInt() }
        utimes(path, times) == 0
    }
    fun delete(): Boolean = if (isDirectory) rmdir(path) == 0 else unlink(path) == 0
    fun deleteOnExit() {}
    fun mkdir(): Boolean = mkdir(path, 0x1EDu) == 0
    fun mkdirs(): Boolean {
        if (isDirectory) return false
        parentFile?.let { if (!it.isDirectory) it.mkdirs() }
        return mkdir()
    }
    fun createNewFile(): Boolean {
        if (exists()) return false
        val fd = open(path, O_CREAT or O_EXCL or O_WRONLY, 0x1A4)
        if (fd < 0) throw IOException("can't create $path")
        close(fd)
        return true
    }
    fun renameTo(dest: File): Boolean = rename(path, dest.path) == 0
    fun list(): Array<String>? {
        val d = opendir(path) ?: return null
        val names = ArrayList<String>()
        try {
            while (true) {
                val e = readdir(d) ?: break
                val n = e.pointed.d_name.toKString()
                if (n != "." && n != "..") names.add(n)
            }
        } finally { closedir(d) }
        return names.toTypedArray()
    }
    fun listFiles(): Array<File>? = list()?.map { File(this, it) }?.toTypedArray()
    fun listFiles(filter: (File) -> Boolean): Array<File>? = listFiles()?.filter(filter)?.toTypedArray()
    fun listFiles(filter: FileFilter): Array<File>? = listFiles()?.filter { filter.accept(it) }?.toTypedArray()
    fun listFiles(filter: FilenameFilter): Array<File>? = listFiles()?.filter { filter.accept(this, it.name) }?.toTypedArray()
    fun list(filter: FilenameFilter): Array<String>? = list()?.filter { filter.accept(this, it) }?.toTypedArray()
    fun toPath(): java.nio.file.Path = java.nio.file.Path(this)
    fun toURI(): java.net.URI = java.net.URI("file://$absolutePath")
    private fun fsAttr(key: Any?): Long =
        (platform.Foundation.NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)?.get(key) as? platform.Foundation.NSNumber)?.longLongValue ?: 0L
    val freeSpace: Long get() = fsAttr(platform.Foundation.NSFileSystemFreeSize)
    val usableSpace: Long get() = freeSpace
    val totalSpace: Long get() = fsAttr(platform.Foundation.NSFileSystemSize)
    fun setReadable(r: Boolean) = true
    fun setWritable(w: Boolean) = true
    fun setExecutable(e: Boolean) = true

    override fun compareTo(other: File) = path.compareTo(other.path)
    override fun equals(other: Any?) = other is File && other.path == path
    override fun hashCode() = path.hashCode()
    override fun toString() = path

    companion object {
        const val separator = "/"
        const val separatorChar = '/'
        const val pathSeparator = ":"
        fun createTempFile(prefix: String, suffix: String?, directory: File? = null): File {
            val dir = directory?.path ?: platform.Foundation.NSTemporaryDirectory().trimEnd('/')
            val f = File(dir, prefix + kotlin.random.Random.nextLong().toULong().toString(36) + (suffix ?: ".tmp"))
            f.createNewFile()
            return f
        }
        private fun cwd(): String = memScoped {
            val buf = allocArray<ByteVar>(4096)
            getcwd(buf, 4096u)?.toKString() ?: ""
        }
    }
}

fun interface FileFilter { fun accept(f: File): Boolean }
fun interface FilenameFilter { fun accept(dir: File, name: String): Boolean }

// ---- streams ----

abstract class InputStream : Closeable {
    abstract fun read(): Int
    open fun read(b: ByteArray): Int = read(b, 0, b.size)
    open fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        var i = 0
        while (i < len) {
            val c = read()
            if (c < 0) return if (i == 0) -1 else i
            b[off + i] = c.toByte()
            i++
        }
        return i
    }
    open fun readAllBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(65536)
        while (true) {
            val n = read(buf, 0, buf.size)
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
    open fun readNBytes(n: Int): ByteArray {
        val out = ByteArray(n)
        var got = 0
        while (got < n) { val r = read(out, got, n - got); if (r < 0) break; got += r }
        return if (got == n) out else out.copyOf(got)
    }
    open fun readNBytes(b: ByteArray, off: Int, len: Int): Int {
        var got = 0
        while (got < len) { val r = read(b, off + got, len - got); if (r < 0) break; got += r }
        return got
    }
    open fun skip(n: Long): Long { var s = 0L; while (s < n && read() >= 0) s++; return s }
    open fun available(): Int = 0
    override fun close() {}
    open fun mark(limit: Int) {}
    open fun reset() { throw IOException("mark/reset not supported") }
    open fun markSupported() = false
    open fun transferTo(out: OutputStream): Long {
        val buf = ByteArray(65536)
        var total = 0L
        while (true) { val n = read(buf); if (n < 0) break; out.write(buf, 0, n); total += n }
        return total
    }
    companion object {
        fun nullInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    }
}

abstract class OutputStream : Closeable, Flushable {
    abstract fun write(b: Int)
    open fun write(b: ByteArray) = write(b, 0, b.size)
    open fun write(b: ByteArray, off: Int, len: Int) { for (i in off until off + len) write(b[i].toInt()) }
    override fun flush() {}
    override fun close() {}
    companion object {
        fun nullOutputStream(): OutputStream = object : OutputStream() { override fun write(b: Int) {} }
    }
}

open class ByteArrayInputStream(private val buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset) : InputStream() {
    protected var pos = offset
    private val end = minOf(buf.size, offset + length)
    private var markPos = offset
    override fun read(): Int = if (pos < end) buf[pos++].toInt() and 0xFF else -1
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        if (pos >= end) return -1
        val n = minOf(len, end - pos)
        buf.copyInto(b, off, pos, pos + n)
        pos += n
        return n
    }
    override fun available() = end - pos
    override fun skip(n: Long): Long { val k = minOf(n, (end - pos).toLong()).toInt(); pos += k; return k.toLong() }
    override fun mark(limit: Int) { markPos = pos }
    override fun reset() { pos = markPos }
    override fun markSupported() = true
}

open class ByteArrayOutputStream(size: Int = 32) : OutputStream() {
    protected var buf = ByteArray(maxOf(size, 1))
    protected var count = 0
    private fun ensure(n: Int) { if (count + n > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, count + n)) }
    override fun write(b: Int) { ensure(1); buf[count++] = b.toByte() }
    override fun write(b: ByteArray, off: Int, len: Int) { ensure(len); b.copyInto(buf, count, off, off + len); count += len }
    fun writeBytes(b: ByteArray) = write(b, 0, b.size)
    fun toByteArray(): ByteArray = buf.copyOf(count)
    fun size() = count
    fun reset() { count = 0 }
    fun writeTo(out: OutputStream) = out.write(buf, 0, count)
    override fun toString(): String = buf.decodeToString(0, count)
    fun toString(charset: Charset): String = charset.decode(buf, 0, count)
    fun toString(charsetName: String): String = Charset.forName(charsetName).decode(buf, 0, count)
}

open class FilterInputStream(protected val `in`: InputStream) : InputStream() {
    override fun read(): Int = `in`.read()
    override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len)
    override fun available(): Int = `in`.available()
    override fun skip(n: Long): Long = `in`.skip(n)
    override fun close() = `in`.close()
    override fun mark(limit: Int) = `in`.mark(limit)
    override fun reset() = `in`.reset()
    override fun markSupported() = `in`.markSupported()
}

open class FilterOutputStream(protected val out: OutputStream) : OutputStream() {
    override fun write(b: Int) = out.write(b)
    override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
    override fun flush() = out.flush()
    override fun close() { flush(); out.close() }
}

open class BufferedInputStream(input: InputStream, size: Int = 8192) : FilterInputStream(input) {
    private var markBuf: ByteArrayOutputStream? = null
    private var replay: ByteArrayInputStream? = null
    override fun read(): Int {
        replay?.let { val c = it.read(); if (c >= 0) { markBuf?.write(c); return c } else replay = null }
        val c = `in`.read()
        if (c >= 0) markBuf?.write(c)
        return c
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        replay?.let { r ->
            val n = r.read(b, off, len)
            if (n > 0) { markBuf?.write(b, off, n); return n }
            replay = null
        }
        val n = `in`.read(b, off, len)
        if (n > 0) markBuf?.write(b, off, n)
        return n
    }
    override fun mark(limit: Int) { markBuf = ByteArrayOutputStream() ; replay?.let { r -> val rest = r.readAllBytes(); replay = ByteArrayInputStream(rest) } }
    override fun reset() { val m = markBuf ?: throw IOException("no mark"); val rest = replay?.readAllBytes() ?: ByteArray(0); replay = ByteArrayInputStream(m.toByteArray() + rest); markBuf = ByteArrayOutputStream() }
    override fun markSupported() = true
}

open class BufferedOutputStream(out: OutputStream, size: Int = 8192) : FilterOutputStream(out)

open class FileInputStream(file: File) : InputStream() {
    constructor(path: String) : this(File(path))
    private val f: CPointer<FILE> = fopen(file.path, "rb") ?: throw FileNotFoundException(file.path)
    private var closed = false
    override fun read(): Int = fgetc(f).let { if (it == EOF) -1 else it }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val n = b.usePinned { p -> fread(p.addressOf(off), 1u, len.toULong(), f).toInt() }
        return if (n <= 0) -1 else n
    }
    override fun skip(n: Long): Long { fseeko(f, n, SEEK_CUR); return n }
    override fun available(): Int {
        val cur = ftello(f); fseeko(f, 0, SEEK_END); val end = ftello(f); fseeko(f, cur, SEEK_SET)
        return (end - cur).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }
    override fun close() { if (!closed) { closed = true; fclose(f) } }
    val channel: java.nio.channels.FileChannel get() = java.nio.channels.FileChannel(f, readOnly = true)
}

open class FileOutputStream(file: File, append: Boolean = false) : OutputStream() {
    constructor(path: String, append: Boolean = false) : this(File(path), append)
    private val f: CPointer<FILE> = fopen(file.path, if (append) "ab" else "wb") ?: throw FileNotFoundException(file.path)
    private var closed = false
    override fun write(b: Int) { fputc(b, f) }
    override fun write(b: ByteArray, off: Int, len: Int) {
        if (len == 0) return
        val n = b.usePinned { p -> fwrite(p.addressOf(off), 1u, len.toULong(), f).toInt() }
        if (n != len) throw IOException("write failed")
    }
    override fun flush() { fflush(f) }
    override fun close() { if (!closed) { closed = true; if (fclose(f) != 0) throw IOException("close failed") } }
    /** fsync: the bytes on the disk. */
    val fd: FileDescriptor get() = FileDescriptor(f)
    val channel: java.nio.channels.FileChannel get() = java.nio.channels.FileChannel(f, readOnly = false)
}

class FileDescriptor internal constructor(private val f: CPointer<FILE>?) {
    fun sync() { if (f != null) { fflush(f); fsync(fileno(f)) } }
    fun valid() = f != null
}

open class RandomAccessFile(file: File, mode: String) : Closeable, DataInput, DataOutput {
    constructor(path: String, mode: String) : this(File(path), mode)
    private val f: CPointer<FILE> = (if (mode == "r") fopen(file.path, "rb") else (fopen(file.path, "r+b") ?: fopen(file.path, "w+b")))
        ?: throw FileNotFoundException(file.path)
    private var closed = false
    fun seek(pos: Long) { fseeko(f, pos, SEEK_SET) }
    fun length(): Long { val cur = ftello(f); fseeko(f, 0, SEEK_END); val n = ftello(f); fseeko(f, cur, SEEK_SET); return n }
    fun setLength(n: Long) { fflush(f); ftruncate(fileno(f), n) }
    val filePointer: Long get() = ftello(f)
    fun read(): Int = fgetc(f).let { if (it == EOF) -1 else it }
    fun read(b: ByteArray): Int = read(b, 0, b.size)
    fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val n = b.usePinned { p -> fread(p.addressOf(off), 1u, len.toULong(), f).toInt() }
        return if (n <= 0) -1 else n
    }
    override fun readFully(b: ByteArray) = readFully(b, 0, b.size)
    override fun readFully(b: ByteArray, off: Int, len: Int) {
        var got = 0
        while (got < len) { val n = read(b, off + got, len - got); if (n < 0) throw EOFException(); got += n }
    }
    fun write(b: Int) { fputc(b, f) }
    fun write(b: ByteArray) = write(b, 0, b.size)
    fun write(b: ByteArray, off: Int, len: Int) {
        if (len == 0) return
        b.usePinned { p -> if (fwrite(p.addressOf(off), 1u, len.toULong(), f).toInt() != len) throw IOException("write failed") }
    }
    override fun close() { if (!closed) { closed = true; fclose(f) } }
    val fd: FileDescriptor get() = FileDescriptor(f)
    val channel: java.nio.channels.FileChannel get() = java.nio.channels.FileChannel(f, readOnly = false)
    override fun skipBytes(n: Int): Int { fseeko(f, n.toLong(), SEEK_CUR); return n }
    override fun readByte(): Byte { val c = read(); if (c < 0) throw EOFException(); return c.toByte() }
    override fun readUnsignedByte(): Int { val c = read(); if (c < 0) throw EOFException(); return c }
    override fun readBoolean() = readByte().toInt() != 0
    override fun readShort(): Short = ((readUnsignedByte() shl 8) or readUnsignedByte()).toShort()
    override fun readUnsignedShort(): Int = (readUnsignedByte() shl 8) or readUnsignedByte()
    override fun readChar(): Char = readUnsignedShort().toChar()
    override fun readInt(): Int = (readUnsignedByte() shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()
    override fun readLong(): Long = (readInt().toLong() shl 32) or (readInt().toLong() and 0xFFFFFFFFL)
    override fun readFloat(): Float = Float.fromBits(readInt())
    override fun readDouble(): Double = Double.fromBits(readLong())
    override fun readUTF(): String { val n = readUnsignedShort(); val b = ByteArray(n); readFully(b); return b.decodeToString() }
    override fun readLine(): String? {
        val sb = StringBuilder()
        while (true) { val c = read(); if (c < 0) return if (sb.isEmpty()) null else sb.toString(); if (c == '\n'.code) return sb.toString(); if (c != '\r'.code) sb.append(c.toChar()) }
    }
    override fun writeBoolean(v: Boolean) = write(if (v) 1 else 0)
    override fun writeByte(v: Int) = write(v)
    override fun writeShort(v: Int) { write(v shr 8); write(v) }
    override fun writeChar(v: Int) = writeShort(v)
    override fun writeInt(v: Int) { write(v ushr 24); write(v ushr 16); write(v ushr 8); write(v) }
    override fun writeLong(v: Long) { writeInt((v ushr 32).toInt()); writeInt(v.toInt()) }
    override fun writeFloat(v: Float) = writeInt(v.toRawBits())
    override fun writeDouble(v: Double) = writeLong(v.toRawBits())
    override fun writeBytes(s: String) { for (c in s) write(c.code) }
    override fun writeChars(s: String) { for (c in s) writeChar(c.code) }
    override fun writeUTF(s: String) { val b = s.encodeToByteArray(); writeShort(b.size); write(b) }
}

interface DataInput {
    fun readFully(b: ByteArray)
    fun readFully(b: ByteArray, off: Int, len: Int)
    fun skipBytes(n: Int): Int
    fun readBoolean(): Boolean
    fun readByte(): Byte
    fun readUnsignedByte(): Int
    fun readShort(): Short
    fun readUnsignedShort(): Int
    fun readChar(): Char
    fun readInt(): Int
    fun readLong(): Long
    fun readFloat(): Float
    fun readDouble(): Double
    fun readLine(): String?
    fun readUTF(): String
}

interface DataOutput {
    fun writeBoolean(v: Boolean)
    fun writeByte(v: Int)
    fun writeShort(v: Int)
    fun writeChar(v: Int)
    fun writeInt(v: Int)
    fun writeLong(v: Long)
    fun writeFloat(v: Float)
    fun writeDouble(v: Double)
    fun writeBytes(s: String)
    fun writeChars(s: String)
    fun writeUTF(s: String)
}

open class DataInputStream(input: InputStream) : FilterInputStream(input), DataInput {
    override fun readFully(b: ByteArray) = readFully(b, 0, b.size)
    override fun readFully(b: ByteArray, off: Int, len: Int) {
        var got = 0
        while (got < len) { val n = read(b, off + got, len - got); if (n < 0) throw EOFException(); got += n }
    }
    override fun skipBytes(n: Int): Int = skip(n.toLong()).toInt()
    override fun readByte(): Byte { val c = read(); if (c < 0) throw EOFException(); return c.toByte() }
    override fun readUnsignedByte(): Int { val c = read(); if (c < 0) throw EOFException(); return c }
    override fun readBoolean() = readByte().toInt() != 0
    override fun readShort(): Short = ((readUnsignedByte() shl 8) or readUnsignedByte()).toShort()
    override fun readUnsignedShort(): Int = (readUnsignedByte() shl 8) or readUnsignedByte()
    override fun readChar(): Char = readUnsignedShort().toChar()
    override fun readInt(): Int = (readUnsignedByte() shl 24) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 8) or readUnsignedByte()
    override fun readLong(): Long = (readInt().toLong() shl 32) or (readInt().toLong() and 0xFFFFFFFFL)
    override fun readFloat(): Float = Float.fromBits(readInt())
    override fun readDouble(): Double = Double.fromBits(readLong())
    override fun readUTF(): String { val n = readUnsignedShort(); val b = ByteArray(n); readFully(b); return b.decodeToString() }
    override fun readLine(): String? {
        val sb = StringBuilder()
        while (true) { val c = read(); if (c < 0) return if (sb.isEmpty()) null else sb.toString(); if (c == '\n'.code) return sb.toString(); if (c != '\r'.code) sb.append(c.toChar()) }
    }
}

open class DataOutputStream(out: OutputStream) : FilterOutputStream(out), DataOutput {
    protected var written = 0
    override fun write(b: Int) { out.write(b); written++ }
    override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); written += len }
    fun size() = written
    override fun writeBoolean(v: Boolean) = write(if (v) 1 else 0)
    override fun writeByte(v: Int) = write(v)
    override fun writeShort(v: Int) { write(v shr 8); write(v) }
    override fun writeChar(v: Int) = writeShort(v)
    override fun writeInt(v: Int) { write(v ushr 24); write(v ushr 16); write(v ushr 8); write(v) }
    override fun writeLong(v: Long) { writeInt((v ushr 32).toInt()); writeInt(v.toInt()) }
    override fun writeFloat(v: Float) = writeInt(v.toRawBits())
    override fun writeDouble(v: Double) = writeLong(v.toRawBits())
    override fun writeBytes(s: String) { for (c in s) write(c.code) }
    override fun writeChars(s: String) { for (c in s) writeChar(c.code) }
    override fun writeUTF(s: String) { val b = s.encodeToByteArray(); writeShort(b.size); write(b, 0, b.size) }
}

open class SequenceInputStream(private val a: InputStream, private val b: InputStream) : InputStream() {
    private var first = true
    override fun read(): Int { if (first) { val c = a.read(); if (c >= 0) return c; first = false }; return b.read() }
}

open class PushbackInputStream(input: InputStream, size: Int = 1) : FilterInputStream(input) {
    private val back = ArrayDeque<Int>()
    override fun read(): Int = back.removeFirstOrNull() ?: `in`.read()
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        var i = 0
        while (i < len && back.isNotEmpty()) { b[off + i] = back.removeFirst().toByte(); i++ }
        if (i == len) return i
        val n = `in`.read(b, off + i, len - i)
        return if (n < 0) (if (i == 0) -1 else i) else i + n
    }
    fun unread(c: Int) = back.addFirst(c and 0xFF)
    fun unread(b: ByteArray, off: Int = 0, len: Int = b.size) { for (i in off + len - 1 downTo off) unread(b[i].toInt()) }
}

// ---- readers / writers (text) ----

abstract class Reader : Closeable {
    abstract fun read(cbuf: CharArray, off: Int, len: Int): Int
    open fun read(): Int { val c = CharArray(1); return if (read(c, 0, 1) < 1) -1 else c[0].code }
    open fun read(cbuf: CharArray): Int = read(cbuf, 0, cbuf.size)
    override fun close() {}
    open fun ready() = false
    fun readAllText(): String {
        val sb = StringBuilder()
        val buf = CharArray(8192)
        while (true) { val n = read(buf, 0, buf.size); if (n < 0) break; sb.appendRange(buf, 0, n) }
        return sb.toString()
    }
}

open class StringReader(private val s: String) : Reader() {
    private var pos = 0
    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (pos >= s.length) return -1
        val n = minOf(len, s.length - pos)
        for (i in 0 until n) cbuf[off + i] = s[pos + i]
        pos += n
        return n
    }
    override fun ready() = true
}

open class InputStreamReader(input: InputStream, charset: Charset = Charsets.UTF_8) : StringReader(charset.decode(input.readAllBytes(), 0, -1)) {
    constructor(input: InputStream, charsetName: String) : this(input, Charset.forName(charsetName))
}

open class BufferedReader(private val r: Reader, size: Int = 8192) : Reader() {
    private val text: String by lazy { r.readAllText() }
    private var pos = 0
    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (pos >= text.length) return -1
        val n = minOf(len, text.length - pos)
        for (i in 0 until n) cbuf[off + i] = text[pos + i]
        pos += n
        return n
    }
    fun readLine(): String? {
        if (pos >= text.length) return null
        val nl = text.indexOf('\n', pos)
        val end = if (nl < 0) text.length else nl
        var line = text.substring(pos, end)
        if (line.endsWith("\r")) line = line.dropLast(1)
        pos = if (nl < 0) text.length else nl + 1
        return line
    }
    fun lines(): Sequence<String> = generateSequence { readLine() }
    override fun close() = r.close()
    override fun ready() = pos < text.length
}

abstract class Writer : Closeable, Flushable, Appendable {
    abstract fun write(cbuf: CharArray, off: Int, len: Int)
    open fun write(c: Int) = write(charArrayOf(c.toChar()), 0, 1)
    open fun write(s: String) = write(s.toCharArray(), 0, s.length)
    open fun write(s: String, off: Int, len: Int) = write(s.substring(off, off + len))
    override fun append(value: Char): Writer { write(value.code); return this }
    override fun append(value: CharSequence?): Writer { write(value.toString()); return this }
    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Writer { write(value.toString().substring(startIndex, endIndex)); return this }
    override fun flush() {}
    override fun close() { flush() }
}

open class StringWriter : Writer() {
    private val sb = StringBuilder()
    override fun write(cbuf: CharArray, off: Int, len: Int) { sb.appendRange(cbuf, off, off + len) }
    val buffer: StringBuilder get() = sb
    override fun toString() = sb.toString()
}

open class OutputStreamWriter(private val out: OutputStream, private val charset: Charset = Charsets.UTF_8) : Writer() {
    constructor(out: OutputStream, charsetName: String) : this(out, Charset.forName(charsetName))
    override fun write(cbuf: CharArray, off: Int, len: Int) { out.write(charset.encode(cbuf.concatToString(off, off + len))) }
    override fun flush() = out.flush()
    override fun close() { flush(); out.close() }
}

open class FileWriter(file: File, append: Boolean = false) : OutputStreamWriter(FileOutputStream(file, append)) {
    constructor(path: String, append: Boolean = false) : this(File(path), append)
}

open class BufferedWriter(private val w: Writer, size: Int = 8192) : Writer() {
    override fun write(cbuf: CharArray, off: Int, len: Int) = w.write(cbuf, off, len)
    fun newLine() = w.write("\n")
    override fun flush() = w.flush()
    override fun close() = w.close()
}

open class PrintWriter(private val w: Writer) : Writer() {
    constructor(out: OutputStream) : this(OutputStreamWriter(out))
    constructor(file: File) : this(FileWriter(file))
    override fun write(cbuf: CharArray, off: Int, len: Int) = w.write(cbuf, off, len)
    fun print(x: Any?) = w.write(x.toString())
    fun println(x: Any?) { w.write(x.toString()); w.write("\n") }
    fun println() = w.write("\n")
    fun printf(f: String, vararg a: Any?): PrintWriter { w.write(kotlin.text.pdFormat(f, a)); return this }
    fun format(f: String, vararg a: Any?): PrintWriter = printf(f, *a)
    override fun flush() = w.flush()
    override fun close() = w.close()
    fun checkError() = false
}

open class PrintStream(target: OutputStream) : FilterOutputStream(target) {
    fun print(x: Any?) = out.write(x.toString().encodeToByteArray())
    fun println(x: Any?) = out.write((x.toString() + "\n").encodeToByteArray())
    fun println() = out.write("\n".encodeToByteArray())
}
