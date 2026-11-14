// commons-compress's 7z reader: not on iOS yet (a .7z in the library says it can't be opened).
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package org.apache.commons.compress.archivers.sevenz

class SevenZArchiveEntry { val name: String = ""; val size: Long = 0; val isDirectory = false; val hasStream = false; fun hasStream() = false }

class SevenZFile(file: java.io.File) : java.io.Closeable {
    init { throw java.io.IOException("7z archives aren't supported on iOS yet") }
    val entries: Iterable<SevenZArchiveEntry> = emptyList()
    fun getInputStream(e: SevenZArchiveEntry): java.io.InputStream = throw java.io.IOException("7z")
    val nextEntry: SevenZArchiveEntry? = null
    fun getNextEntry(): SevenZArchiveEntry? = null
    fun read(b: ByteArray): Int = -1
    fun read(b: ByteArray, off: Int, len: Int): Int = -1
    override fun close() {}
    companion object { fun builder() = Builder() }
    class Builder { private var f: java.io.File? = null; fun setFile(f: java.io.File) = apply { this.f = f }; fun get() = SevenZFile(f!!) }
}
