@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.security

import kotlinx.cinterop.*
import platform.CoreCrypto.*

open class GeneralSecurityException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
open class NoSuchAlgorithmException(message: String? = null) : GeneralSecurityException(message)
open class InvalidKeyException(message: String? = null) : GeneralSecurityException(message)
interface Key { val algorithm: String; val encoded: ByteArray }

/** SHA-1 / SHA-256 / MD5 through CommonCrypto (input gathered, hashed at digest()). */
class MessageDigest private constructor(val algorithm: String) {
    private val buf = java.io.ByteArrayOutputStream()
    fun update(b: Byte) = buf.write(b.toInt())
    fun update(b: ByteArray) = buf.write(b, 0, b.size)
    fun update(b: ByteArray, off: Int, len: Int) = buf.write(b, off, len)
    fun update(b: java.nio.ByteBuffer) { val n = b.remaining(); val a = ByteArray(n); b.get(a); update(a) }
    fun digest(b: ByteArray): ByteArray { update(b); return digest() }
    fun digest(): ByteArray {
        val data = buf.toByteArray()
        buf.reset()
        val n = digestLength
        val out = UByteArray(n)
        data.usePinned { dp ->
            out.usePinned { op ->
                val src = if (data.isEmpty()) null else dp.addressOf(0)
                when (algorithm.uppercase()) {
                    "SHA-1", "SHA1" -> CC_SHA1(src, data.size.toUInt(), op.addressOf(0))
                    "SHA-256", "SHA256" -> CC_SHA256(src, data.size.toUInt(), op.addressOf(0))
                    "MD5" -> CC_MD5(src, data.size.toUInt(), op.addressOf(0))
                    else -> throw NoSuchAlgorithmException(algorithm)
                }
            }
        }
        return out.toByteArray()
    }
    fun reset() = buf.reset()
    val digestLength: Int get() = when (algorithm.uppercase()) { "SHA-1", "SHA1" -> 20; "SHA-256", "SHA256" -> 32; "MD5" -> 16; else -> throw NoSuchAlgorithmException(algorithm) }
    fun getDigestLength() = digestLength
    companion object {
        fun getInstance(algorithm: String): MessageDigest { MessageDigest(algorithm).digestLength; return MessageDigest(algorithm) }
        fun isEqual(a: ByteArray?, b: ByteArray?) = a.contentEquals(b)
    }
}

class SecureRandom : java.util.Random() {
    fun generateSeed(n: Int) = ByteArray(n).also { nextBytes(it) }
    companion object { fun getInstanceStrong() = SecureRandom(); fun getInstance(a: String) = SecureRandom() }
}

class DigestInputStream(input: java.io.InputStream, val messageDigest: MessageDigest) : java.io.FilterInputStream(input) {
    override fun read(): Int { val c = `in`.read(); if (c >= 0) messageDigest.update(c.toByte()); return c }
    override fun read(b: ByteArray, off: Int, len: Int): Int { val n = `in`.read(b, off, len); if (n > 0) messageDigest.update(b, off, n); return n }
}
