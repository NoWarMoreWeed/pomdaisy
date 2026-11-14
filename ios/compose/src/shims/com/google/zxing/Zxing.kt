// The QR code writer the co-op invite uses (behind its feature flag): not available on iOS yet.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package com.google.zxing

enum class BarcodeFormat { QR_CODE }
enum class EncodeHintType { ERROR_CORRECTION, MARGIN, CHARACTER_SET }
class WriterException(message: String?) : Exception(message)
