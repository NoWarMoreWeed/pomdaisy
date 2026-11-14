// kotlin.text / kotlin.io as Kotlin/JVM has them: charsets, String(bytes, charset), File's extension functions.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package kotlin.text

typealias Charset = java.nio.charset.Charset

object Charsets {
    val UTF_8 = Charset("UTF-8")
    val UTF_16 = Charset("UTF-16")
    val UTF_16LE = Charset("UTF-16LE")
    val UTF_16BE = Charset("UTF-16BE")
    val US_ASCII = Charset("US-ASCII")
    val ISO_8859_1 = Charset("ISO-8859-1")
}

fun String(bytes: ByteArray, charset: Charset): String = charset.decode(bytes, 0, bytes.size)
fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String = charset.decode(bytes, offset, length)
fun String(bytes: ByteArray): String = bytes.decodeToString()
fun String(bytes: ByteArray, offset: Int, length: Int): String = bytes.decodeToString(offset, offset + length)
fun String(chars: CharArray, offset: Int, length: Int): String = chars.concatToString(offset, offset + length)
fun String.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = charset.encode(this)
fun ByteArray.toString(charset: Charset): String = charset.decode(this, 0, size)
fun String.Companion.valueOf(o: Any?): String = o.toString()
fun String.Companion.valueOf(c: CharArray): String = c.concatToString()
fun String.Companion.join(sep: CharSequence, parts: Iterable<CharSequence>): String = parts.joinToString(sep)
fun String.Companion.join(sep: CharSequence, vararg parts: CharSequence): String = parts.joinToString(sep)
fun String.intern(): String = this
fun String.codePointAt(i: Int): Int {
    val c = this[i]
    return if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate())
        ((c.code - 0xD800) shl 10) + (this[i + 1].code - 0xDC00) + 0x10000 else c.code
}
fun String.codePoints(): kotlin.sequences.Sequence<Int> = kotlin.sequences.sequence {
    var i = 0
    while (i < length) { val cp = codePointAt(i); yield(cp); i += if (cp >= 0x10000) 2 else 1 }
}
fun String.regionMatches(thisOffset: Int, other: String, otherOffset: Int, length: Int, ignoreCase: Boolean = false): Boolean =
    regionMatches(thisOffset, other, otherOffset, length, ignoreCase)
fun StringBuilder.setLength(n: Int) { if (n < length) deleteRange(n, length) else repeat(n - length) { append('\u0000') } }
fun String.format(locale: java.util.Locale?, vararg args: Any?): String = pdFormat(this, args)
fun String.Companion.format(locale: java.util.Locale?, format: String, vararg args: Any?): String = pdFormat(format, args)
fun Char.Companion.toChars(cp: Int): CharArray = java.lang.Character.toChars(cp)
fun String.uppercase(locale: java.util.Locale): String = uppercase()
fun String.lowercase(locale: java.util.Locale): String = lowercase()
fun String.toUpperCase(locale: java.util.Locale): String = uppercase()
fun String.toLowerCase(locale: java.util.Locale): String = lowercase()
fun String.capitalize(locale: java.util.Locale): String = replaceFirstChar { it.uppercaseChar() }
fun Char.uppercase(locale: java.util.Locale): String = uppercase()
fun Char.lowercase(locale: java.util.Locale): String = lowercase()
