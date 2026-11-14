package android.graphics

import java.io.File
import java.io.InputStream

/** Just enough of android.graphics.Bitmap for the app's decode/UI code: an ARGB int array. */
class Bitmap(val width: Int, val height: Int, internal val px: IntArray = IntArray(width * height)) {
    enum class Config { ARGB_8888, RGB_565 }
    enum class CompressFormat { PNG, JPEG, WEBP }

    var isPremultiplied = false
    val config get() = Config.ARGB_8888

    fun getPixels(out: IntArray, offset: Int, stride: Int, x: Int, y: Int, w: Int, h: Int) {
        for (r in 0 until h) for (c in 0 until w) out[offset + r * stride + c] = px[(y + r) * width + x + c]
    }

    fun setPixels(src: IntArray, offset: Int, stride: Int, x: Int, y: Int, w: Int, h: Int) {
        for (r in 0 until h) for (c in 0 until w) px[(y + r) * width + x + c] = src[offset + r * stride + c]
    }

    fun getPixel(x: Int, y: Int) = px[y * width + x]
    /** RGBA bytes (as the GL / core frames are) into the pixels. */
    fun copyPixelsFromBuffer(b: java.nio.Buffer) {
        val bb = b as? java.nio.ByteBuffer ?: return
        val n = minOf(px.size, bb.remaining() / 4)
        val bytes = ByteArray(n * 4); bb.duplicate().get(bytes)
        for (i in 0 until n) {
            val o = i * 4
            px[i] = ((bytes[o + 3].toInt() and 0xFF) shl 24) or ((bytes[o].toInt() and 0xFF) shl 16) or ((bytes[o + 1].toInt() and 0xFF) shl 8) or (bytes[o + 2].toInt() and 0xFF)
        }
    }
    fun copyPixelsToBuffer(b: java.nio.Buffer) {
        val bb = b as? java.nio.ByteBuffer ?: return
        for (c in px) { bb.put((c shr 16).toByte()); bb.put((c shr 8).toByte()); bb.put(c.toByte()); bb.put((c ushr 24).toByte()) }
    }
    fun eraseColor(c: Int) { px.fill(c) }
    val byteCount: Int get() = px.size * 4
    val allocationByteCount: Int get() = byteCount
    fun copy(c: Config, mutable: Boolean) = Bitmap(width, height, px.copyOf())
    val isMutable = true
    val isRecycled = false
    fun hasAlpha() = true
    fun sameAs(o: Bitmap?) = o != null && o.width == width && o.height == height && o.px.contentEquals(px)
    var density = 160
    fun setPixel(x: Int, y: Int, c: Int) { px[y * width + x] = c }
    fun recycle() {}
    fun compress(f: CompressFormat, q: Int, s: java.io.OutputStream): Boolean = true

    companion object {
        fun createBitmap(w: Int, h: Int, c: Config) = Bitmap(w, h)
        fun createBitmap(src: IntArray, w: Int, h: Int, c: Config) = Bitmap(w, h, src.copyOf())
        fun createBitmap(src: Bitmap, x: Int, y: Int, w: Int, h: Int): Bitmap {
            val out = Bitmap(w, h)
            src.getPixels(out.px, 0, w, x, y, w, h)
            return out
        }
        fun createScaledBitmap(src: Bitmap, w: Int, h: Int, filter: Boolean): Bitmap {
            val out = Bitmap(w, h)
            for (y in 0 until h) for (x in 0 until w) {
                out.px[y * w + x] = src.px[(y * src.height / h) * src.width + x * src.width / w]
            }
            return out
        }
    }
}

/** Non-null returns: Android's are platform types the app's code treats as non-null. */
object BitmapFactory {
    class Options {
        var inJustDecodeBounds = false
        var inPreferredConfig: Bitmap.Config? = null
        var inScaled = true
        var inMutable = false
        var inDensity = 0
        var inTargetDensity = 0
        var inPremultiplied = true
        var outMimeType: String? = null
        var inSampleSize = 1
        var outWidth = 0
        var outHeight = 0
    }
    /** [o]'s bounds-only decode fills its size; the sample size is ignored (the preview keeps full size). */
    fun decodeFile(p: String, o: Options): Bitmap? {
        val b = decodeFile(p)
        o.outWidth = b.width
        o.outHeight = b.height
        return if (o.inJustDecodeBounds) null else b
    }
    /** PNG / JPEG / WebP through Skia: the pixels as ARGB ints (unpremultiplied). */
    fun decodeStream(s: InputStream?): Bitmap {
        val img = org.jetbrains.skia.Image.makeFromEncoded(s!!.readAllBytes())
        val bmp = org.jetbrains.skia.Bitmap()
        bmp.allocPixels(org.jetbrains.skia.ImageInfo.makeN32(img.width, img.height, org.jetbrains.skia.ColorAlphaType.UNPREMUL))
        img.readPixels(bmp)
        val bytes = bmp.readPixels() ?: ByteArray(img.width * img.height * 4)
        val b = Bitmap(img.width, img.height)
        // N32 on iOS is BGRA in memory
        for (i in 0 until img.width * img.height) {
            val o = i * 4
            b.px[i] = ((bytes[o + 3].toInt() and 0xFF) shl 24) or ((bytes[o + 2].toInt() and 0xFF) shl 16) or
                ((bytes[o + 1].toInt() and 0xFF) shl 8) or (bytes[o].toInt() and 0xFF)
        }
        return b
    }
    fun decodeFile(p: String): Bitmap = File(p).inputStream().use { decodeStream(it) }
    fun decodeByteArray(d: ByteArray, o: Int, l: Int): Bitmap = decodeStream(d.inputStream(o, l))
}
