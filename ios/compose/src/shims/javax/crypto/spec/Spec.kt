@file:Suppress("PackageDirectoryMismatch", "unused")

package javax.crypto.spec

class SecretKeySpec(private val key: ByteArray, override val algorithm: String) : javax.crypto.SecretKey, java.security.spec.KeySpec {
    constructor(key: ByteArray, off: Int, len: Int, algorithm: String) : this(key.copyOfRange(off, off + len), algorithm)
    override val encoded: ByteArray get() = key.copyOf()
    fun getEncoded() = encoded
    fun getAlgorithm() = algorithm
}
class GCMParameterSpec(val tLen: Int, val iv: ByteArray) : java.security.spec.AlgorithmParameterSpec {
    constructor(tLen: Int, src: ByteArray, off: Int, len: Int) : this(tLen, src.copyOfRange(off, off + len))
}
class IvParameterSpec(val iv: ByteArray) : java.security.spec.AlgorithmParameterSpec
