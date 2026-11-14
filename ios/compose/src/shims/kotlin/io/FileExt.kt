// kotlin.io's java.io.File extensions (JVM-only in the standard library), over the File shim.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package kotlin.io

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import kotlin.text.Charset
import kotlin.text.Charsets

fun File.readBytes(): ByteArray = java.io.pdReadFile(path) ?: throw java.io.FileNotFoundException(path)
fun File.writeBytes(array: ByteArray) { if (!java.io.pdWriteFile(path, array, append = false)) throw java.io.IOException("can't write $path") }
fun File.appendBytes(array: ByteArray) { if (!java.io.pdWriteFile(path, array, append = true)) throw java.io.IOException("can't write $path") }
fun File.readText(charset: Charset = Charsets.UTF_8): String = charset.decode(readBytes(), 0, readBytes().size)
fun File.writeText(text: String, charset: Charset = Charsets.UTF_8) = writeBytes(charset.encode(text))
fun File.appendText(text: String, charset: Charset = Charsets.UTF_8) = appendBytes(charset.encode(text))
fun File.readLines(charset: Charset = Charsets.UTF_8): List<String> = readText(charset).lines().let { if (it.lastOrNull() == "") it.dropLast(1) else it }
fun File.forEachLine(charset: Charset = Charsets.UTF_8, action: (String) -> Unit) = readLines(charset).forEach(action)
fun <T> File.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T = block(readLines(charset).asSequence())
fun File.inputStream(): FileInputStream = FileInputStream(this)
fun File.outputStream(): FileOutputStream = FileOutputStream(this)
fun File.bufferedReader(charset: Charset = Charsets.UTF_8): BufferedReader = BufferedReader(java.io.StringReader(readText(charset)))
fun File.reader(charset: Charset = Charsets.UTF_8): Reader = java.io.StringReader(readText(charset))
fun File.bufferedWriter(charset: Charset = Charsets.UTF_8): java.io.BufferedWriter = java.io.BufferedWriter(java.io.FileWriter(this))
fun File.writer(charset: Charset = Charsets.UTF_8): Writer = java.io.FileWriter(this)
fun File.printWriter(): java.io.PrintWriter = java.io.PrintWriter(java.io.FileWriter(this))

val File.extension: String get() = name.substringAfterLast('.', "")
val File.nameWithoutExtension: String get() = name.substringBeforeLast(".")
val File.invariantSeparatorsPath: String get() = path

fun File.copyTo(target: File, overwrite: Boolean = false, bufferSize: Int = 8192): File {
    if (target.exists() && !overwrite) throw FileAlreadyExistsException(target)
    if (isDirectory) { target.mkdirs(); return target }
    target.parentFile?.mkdirs()
    target.writeBytes(readBytes())
    return target
}

fun File.copyRecursively(target: File, overwrite: Boolean = false): Boolean {
    walkTopDown().forEach { src ->
        val rel = src.path.removePrefix(path).trimStart('/')
        val dst = if (rel.isEmpty()) target else File(target, rel)
        if (src.isDirectory) dst.mkdirs() else src.copyTo(dst, overwrite)
    }
    return true
}

fun File.deleteRecursively(): Boolean = walkBottomUp().fold(true) { ok, f -> f.delete() && ok }

fun File.walkTopDown(): FileTreeWalk = FileTreeWalk(this, topDown = true)
fun File.walkBottomUp(): FileTreeWalk = FileTreeWalk(this, topDown = false)
fun File.walk(): FileTreeWalk = walkTopDown()

class FileTreeWalk internal constructor(private val root: File, private val topDown: Boolean, private val maxDepth: Int = Int.MAX_VALUE) : Sequence<File> {
    override fun iterator(): Iterator<File> = sequence { visit(root, 0) }.iterator()
    private suspend fun SequenceScope<File>.visit(f: File, depth: Int) {
        if (topDown) yield(f)
        if (f.isDirectory && depth < maxDepth) f.listFiles()?.sortedBy { it.name }?.forEach { visit(it, depth + 1) }
        if (!topDown) yield(f)
    }
    fun maxDepth(depth: Int) = FileTreeWalk(root, topDown, depth)
    fun onEnter(f: (File) -> Boolean) = this
}

fun File.relativeTo(base: File): File = File(path.removePrefix(base.path).trimStart('/'))
fun File.relativeToOrSelf(base: File): File = relativeToOrNull(base) ?: this
fun File.relativeToOrNull(base: File): File? = if (path.startsWith(base.path)) relativeTo(base) else null
fun File.toRelativeString(base: File): String = relativeTo(base).path
fun File.resolve(relative: String): File = File(this, relative)
fun File.resolve(relative: File): File = File(this, relative.path)
fun File.resolveSibling(relative: String): File = File(parentFile, relative)
fun File.startsWith(other: File): Boolean = path.startsWith(other.path)
fun File.normalize(): File = this

open class FileSystemException(val file: File, val other: File? = null, val reason: String? = null) : java.io.IOException(reason ?: file.path)
class FileAlreadyExistsException(file: File, other: File? = null, reason: String? = null) : FileSystemException(file, other, reason)
class NoSuchFileException(file: File, other: File? = null, reason: String? = null) : FileSystemException(file, other, reason)
class AccessDeniedException(file: File, other: File? = null, reason: String? = null) : FileSystemException(file, other, reason)

fun createTempFile(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File = File.createTempFile(prefix, suffix, directory)

// Streams
fun InputStream.readBytes(): ByteArray = readAllBytes()
fun InputStream.readBytes(estimatedSize: Int): ByteArray = readAllBytes()
fun InputStream.copyTo(out: OutputStream, bufferSize: Int = 8192): Long {
    val buf = ByteArray(bufferSize)
    var total = 0L
    while (true) {
        val n = read(buf)
        if (n < 0) break
        out.write(buf, 0, n)
        total += n
    }
    return total
}
fun InputStream.buffered(bufferSize: Int = 8192): java.io.BufferedInputStream = java.io.BufferedInputStream(this, bufferSize)
fun OutputStream.buffered(bufferSize: Int = 8192): java.io.BufferedOutputStream = java.io.BufferedOutputStream(this, bufferSize)
fun InputStream.bufferedReader(charset: Charset = Charsets.UTF_8): BufferedReader = BufferedReader(java.io.StringReader(charset.decode(readAllBytes().let { it }, 0, -1)))
fun InputStream.reader(charset: Charset = Charsets.UTF_8): Reader = java.io.StringReader(charset.decode(readAllBytes(), 0, -1))
fun OutputStream.bufferedWriter(charset: Charset = Charsets.UTF_8): java.io.BufferedWriter = java.io.BufferedWriter(java.io.OutputStreamWriter(this, charset))
fun OutputStream.writer(charset: Charset = Charsets.UTF_8): Writer = java.io.OutputStreamWriter(this, charset)
fun ByteArray.inputStream(): java.io.ByteArrayInputStream = java.io.ByteArrayInputStream(this)
fun ByteArray.inputStream(offset: Int, length: Int): java.io.ByteArrayInputStream = java.io.ByteArrayInputStream(this, offset, length)
fun String.byteInputStream(charset: Charset = Charsets.UTF_8): java.io.ByteArrayInputStream = java.io.ByteArrayInputStream(charset.encode(this))
fun Reader.readText(): String = readAllText()
fun Reader.readLines(): List<String> = readAllText().lines()
fun Reader.buffered(): BufferedReader = BufferedReader(this)
fun BufferedReader.lineSequence(): Sequence<String> = readAllText().lineSequence()
fun Reader.forEachLine(action: (String) -> Unit) = readAllText().lines().forEach(action)
fun <T> Reader.useLines(block: (Sequence<String>) -> T): T = block(readAllText().lineSequence())

