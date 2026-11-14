@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package javax.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*

open class AEADBadTagException(message: String? = null) : BadPaddingException(message)
open class BadPaddingException(message: String? = null) : java.security.GeneralSecurityException(message)
interface SecretKey : java.security.Key

/** HmacSHA256 through CommonCrypto. */
class Mac private constructor(val algorithm: String) {
    private var key = ByteArray(0)
    private val buf = java.io.ByteArrayOutputStream()
    fun init(k: java.security.Key) { key = k.encoded }
    fun update(b: ByteArray) = buf.write(b, 0, b.size)
    fun update(b: ByteArray, off: Int, len: Int) = buf.write(b, off, len)
    fun update(b: Byte) = buf.write(b.toInt())
    fun doFinal(b: ByteArray): ByteArray { update(b); return doFinal() }
    fun doFinal(): ByteArray {
        val data = buf.toByteArray(); buf.reset()
        val out = UByteArray(32)
        key.usePinned { kp -> data.usePinned { dp -> out.usePinned { op ->
            CCHmac(kCCHmacAlgSHA256, if (key.isEmpty()) null else kp.addressOf(0), key.size.toULong(),
                if (data.isEmpty()) null else dp.addressOf(0), data.size.toULong(), op.addressOf(0))
        } } }
        return out.toByteArray()
    }
    val macLength get() = 32
    companion object {
        fun getInstance(algorithm: String): Mac {
            if (!algorithm.equals("HmacSHA256", ignoreCase = true)) throw java.security.NoSuchAlgorithmException(algorithm)
            return Mac(algorithm)
        }
    }
}

/** AES-GCM isn't in CommonCrypto's public API: the co-op link's cipher (behind its feature flag) isn't available here yet. */
class Cipher private constructor(val algorithm: String) {
    fun init(mode: Int, key: java.security.Key, params: java.security.spec.AlgorithmParameterSpec? = null): Unit = throw java.security.NoSuchAlgorithmException("$algorithm on iOS")
    fun updateAAD(b: ByteArray) {}
    fun doFinal(b: ByteArray): ByteArray = throw java.security.NoSuchAlgorithmException(algorithm)
    fun doFinal(b: ByteArray, off: Int, len: Int): ByteArray = throw java.security.NoSuchAlgorithmException(algorithm)
    companion object {
        const val ENCRYPT_MODE = 1
        const val DECRYPT_MODE = 2
        fun getInstance(algorithm: String) = Cipher(algorithm)
    }
}
