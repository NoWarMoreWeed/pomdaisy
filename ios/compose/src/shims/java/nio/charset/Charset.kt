@file:Suppress("PackageDirectoryMismatch", "unused")

package java.nio.charset

/** The charsets the app names: UTF-8 (and UTF-16), ISO-8859-1, US-ASCII. */
class Charset internal constructor(private val name: String) {
    fun name() = name
    fun displayName() = name
    override fun toString() = name
    override fun equals(other: Any?) = other is Charset && other.name.equals(name, ignoreCase = true)
    override fun hashCode() = name.uppercase().hashCode()

    fun decode(b: ByteArray, off: Int, len: Int): String {
        val end = if (len < 0) b.size else off + len
        return when (name.uppercase()) {
            "ISO-8859-1", "US-ASCII", "ASCII", "LATIN1" -> {
                val c = CharArray(end - off) { (b[off + it].toInt() and 0xFF).toChar() }
                c.concatToString()
            }
            "UTF-16LE" -> CharArray((end - off) / 2) { ((b[off + 2 * it].toInt() and 0xFF) or ((b[off + 2 * it + 1].toInt() and 0xFF) shl 8)).toChar() }.concatToString()
            "UTF-16BE", "UTF-16" -> CharArray((end - off) / 2) { (((b[off + 2 * it].toInt() and 0xFF) shl 8) or (b[off + 2 * it + 1].toInt() and 0xFF)).toChar() }.concatToString()
            else -> b.decodeToString(off, end)
        }
    }

    fun encode(s: String): ByteArray = when (name.uppercase()) {
        "ISO-8859-1", "LATIN1" -> ByteArray(s.length) { s[it].code.coerceAtMost(0xFF).let { c -> if (c > 0xFF) '?'.code else c }.toByte() }
        "US-ASCII", "ASCII" -> ByteArray(s.length) { (if (s[it].code > 0x7F) '?'.code else s[it].code).toByte() }
        "UTF-16LE" -> ByteArray(s.length * 2) { if (it % 2 == 0) s[it / 2].code.toByte() else (s[it / 2].code shr 8).toByte() }
        "UTF-16BE", "UTF-16" -> ByteArray(s.length * 2) { if (it % 2 == 1) s[it / 2].code.toByte() else (s[it / 2].code shr 8).toByte() }
        else -> s.encodeToByteArray()
    }

    fun newDecoder(): CharsetDecoder = CharsetDecoder(this)
    fun newEncoder(): CharsetEncoder = CharsetEncoder(this)
    fun decode(b: java.nio.ByteBuffer): String { val n = b.remaining(); val a = ByteArray(n); b.get(a); return decode(a, 0, n) }
    fun encode(s: CharSequence): java.nio.ByteBuffer = java.nio.ByteBuffer.wrap(encode(s.toString()))
    companion object {
        fun forName(name: String): Charset = Charset(name)
        fun defaultCharset(): Charset = Charset("UTF-8")
        fun isSupported(name: String) = true
    }
}

object StandardCharsets {
    val UTF_8 = kotlin.text.Charsets.UTF_8
    val US_ASCII = kotlin.text.Charsets.US_ASCII
    val ISO_8859_1 = kotlin.text.Charsets.ISO_8859_1
    val UTF_16 = kotlin.text.Charsets.UTF_16
    val UTF_16LE = kotlin.text.Charsets.UTF_16LE
    val UTF_16BE = kotlin.text.Charsets.UTF_16BE
}

open class CharacterCodingException : java.io.IOException()

/** Strict decoding (UTF-8: malformed input throws), as the app uses it to test whether bytes are text. */
class CharsetDecoder internal constructor(private val cs: Charset) {
    fun onMalformedInput(a: CodingErrorAction) = this
    fun onUnmappableCharacter(a: CodingErrorAction) = this
    fun decode(b: java.nio.ByteBuffer): java.nio.CharBuffer {
        val n = b.remaining(); val a = ByteArray(n); b.get(a)
        val s = try { if (cs.name().uppercase() == "UTF-8") a.decodeToString(throwOnInvalidSequence = true) else cs.decode(a, 0, n) }
            catch (e: CharacterCodingException) { throw e } catch (e: Exception) { throw MalformedInputException(0) }
        return java.nio.CharBuffer(s)
    }
}
class CharsetEncoder internal constructor(private val cs: Charset) {
    fun canEncode(s: CharSequence) = true
    fun encode(s: java.nio.CharBuffer): java.nio.ByteBuffer = java.nio.ByteBuffer.wrap(cs.encode(s.toString()))
}
class CodingErrorAction private constructor() { companion object { val REPORT = CodingErrorAction(); val REPLACE = CodingErrorAction(); val IGNORE = CodingErrorAction() } }
open class MalformedInputException(val inputLength: Int) : CharacterCodingException()
