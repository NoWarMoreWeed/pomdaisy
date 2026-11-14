@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package java.nio.file

import java.io.File

class Path internal constructor(private val f: File) {
    fun toFile(): File = f
    override fun toString() = f.path
    val fileName: Path get() = Path(File(f.name))
    val parent: Path? get() = f.parentFile?.let { Path(it) }
    fun resolve(other: String): Path = Path(File(f, other))
    fun resolveSibling(other: String): Path = Path(File(f.parentFile, other))
    fun toAbsolutePath(): Path = Path(f.absoluteFile)
    fun normalize(): Path = this
}

object Paths {
    fun get(first: String, vararg more: String): Path = Path(File(listOf(first, *more).joinToString("/")))
}

interface CopyOption
enum class StandardCopyOption : CopyOption { REPLACE_EXISTING, ATOMIC_MOVE, COPY_ATTRIBUTES }
enum class LinkOption : CopyOption { NOFOLLOW_LINKS }
open class AtomicMoveNotSupportedException(source: String?, target: String?, reason: String?) : java.io.IOException(reason)
open class NoSuchFileException(file: String?) : java.io.IOException(file)
open class FileAlreadyExistsException(file: String?) : java.io.IOException(file)

object Files {
    fun move(source: Path, target: Path, vararg options: CopyOption): Path {
        if (!source.toFile().renameTo(target.toFile())) throw java.io.IOException("move ${source} -> ${target} failed")
        return target
    }
    fun copy(source: Path, target: Path, vararg options: CopyOption): Path {
        if (target.toFile().exists() && StandardCopyOption.REPLACE_EXISTING !in options) throw FileAlreadyExistsException(target.toString())
        target.toFile().writeBytes(source.toFile().readBytes())
        return target
    }
    fun copy(input: java.io.InputStream, target: Path, vararg options: CopyOption): Long {
        val b = input.readAllBytes(); target.toFile().writeBytes(b); return b.size.toLong()
    }
    fun readAllBytes(p: Path): ByteArray = p.toFile().readBytes()
    fun write(p: Path, bytes: ByteArray, vararg options: Any?): Path { p.toFile().writeBytes(bytes); return p }
    fun exists(p: Path, vararg options: LinkOption) = p.toFile().exists()
    fun isDirectory(p: Path, vararg options: LinkOption) = p.toFile().isDirectory
    fun isRegularFile(p: Path, vararg options: LinkOption) = p.toFile().isFile
    fun delete(p: Path) { if (!p.toFile().delete()) throw NoSuchFileException(p.toString()) }
    fun deleteIfExists(p: Path): Boolean = p.toFile().delete()
    fun createDirectories(p: Path): Path { p.toFile().mkdirs(); return p }
    fun size(p: Path): Long = p.toFile().length()
    fun createTempFile(dir: Path, prefix: String, suffix: String?): Path = Path(File.createTempFile(prefix, suffix, dir.toFile()))
}
