@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package com.google.zxing.qrcode

class QRCodeWriter {
    fun encode(contents: String, format: com.google.zxing.BarcodeFormat, width: Int, height: Int, hints: Map<com.google.zxing.EncodeHintType, *>? = null): com.google.zxing.common.BitMatrix =
        throw com.google.zxing.WriterException("QR codes aren't available on iOS yet")
}
