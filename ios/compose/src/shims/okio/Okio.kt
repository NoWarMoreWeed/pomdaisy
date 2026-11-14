@file:Suppress("PackageDirectoryMismatch", "unused")

package okio

class ByteString(private val data: ByteArray) {
    fun toByteArray() = data.copyOf()
    val size: Int get() = data.size
    companion object {
        fun of(vararg b: Byte) = ByteString(b)
        fun ByteArray.toByteString(offset: Int = 0, byteCount: Int = size) = ByteString(copyOfRange(offset, offset + byteCount))
    }
}
