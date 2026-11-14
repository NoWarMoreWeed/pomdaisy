// Bouncy Castle's X25519 (the co-op link's key agreement, behind its feature flag): not on iOS yet.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package org.bouncycastle.math.ec.rfc7748

object X25519 {
    const val POINT_SIZE = 32
    const val SCALAR_SIZE = 32
    fun generatePrivateKey(random: java.security.SecureRandom, k: ByteArray) { random.nextBytes(k) }
    fun generatePublicKey(k: ByteArray, kOff: Int, r: ByteArray, rOff: Int): Unit = throw UnsupportedOperationException("X25519 on iOS")
    fun scalarMultBase(k: ByteArray, kOff: Int, r: ByteArray, rOff: Int): Unit = throw UnsupportedOperationException("X25519 on iOS")
    fun calculateAgreement(k: ByteArray, kOff: Int, u: ByteArray, uOff: Int, r: ByteArray, rOff: Int): Boolean = throw UnsupportedOperationException("X25519 on iOS")
    fun scalarMult(k: ByteArray, kOff: Int, u: ByteArray, uOff: Int, r: ByteArray, rOff: Int): Unit = throw UnsupportedOperationException("X25519 on iOS")
}
