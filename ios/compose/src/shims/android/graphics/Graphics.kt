// android.graphics beyond Bitmap (uipreview/AndroidGraphics.kt): Canvas over a Skia canvas, Paint, Rect / RectF, Color,
// Typeface, the few others the game screen names.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.graphics

object Color {
    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val TRANSPARENT = 0
    const val RED = 0xFFFF0000.toInt()
    const val GREEN = 0xFF00FF00.toInt()
    const val BLUE = 0xFF0000FF.toInt()
    const val GRAY = 0xFF888888.toInt()
    const val DKGRAY = 0xFF444444.toInt()
    const val LTGRAY = 0xFFCCCCCC.toInt()
    const val YELLOW = 0xFFFFFF00.toInt()
    const val CYAN = 0xFF00FFFF.toInt()
    const val MAGENTA = 0xFFFF00FF.toInt()
    fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    fun rgb(r: Int, g: Int, b: Int) = argb(255, r, g, b)
    fun alpha(c: Int) = c ushr 24
    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF
    fun parseColor(s: String): Int {
        val h = s.removePrefix("#")
        return when (h.length) { 6 -> (0xFF000000 or h.toLong(16)).toInt(); 8 -> h.toLong(16).toInt(); else -> throw IllegalArgumentException(s) }
    }
}

class Rect(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0) {
    constructor(r: Rect) : this(r.left, r.top, r.right, r.bottom)
    fun width() = right - left
    fun height() = bottom - top
    fun centerX() = (left + right) / 2
    fun centerY() = (top + bottom) / 2
    fun exactCenterX() = (left + right) / 2f
    fun exactCenterY() = (top + bottom) / 2f
    val isEmpty: Boolean get() = left >= right || top >= bottom
    fun isEmpty() = isEmpty
    fun set(l: Int, t: Int, r: Int, b: Int) { left = l; top = t; right = r; bottom = b }
    fun set(o: Rect) = set(o.left, o.top, o.right, o.bottom)
    fun setEmpty() = set(0, 0, 0, 0)
    fun contains(x: Int, y: Int) = x >= left && x < right && y >= top && y < bottom
    fun contains(r: Rect) = r.left >= left && r.right <= right && r.top >= top && r.bottom <= bottom
    fun offset(dx: Int, dy: Int) { left += dx; right += dx; top += dy; bottom += dy }
    fun offsetTo(x: Int, y: Int) = offset(x - left, y - top)
    fun inset(dx: Int, dy: Int) { left += dx; right -= dx; top += dy; bottom -= dy }
    fun intersect(l: Int, t: Int, r: Int, b: Int): Boolean = intersect(Rect(l, t, r, b))
    fun intersects(l: Int, t: Int, r: Int, b: Int): Boolean = intersects(this, Rect(l, t, r, b))
    fun intersect(r: Rect): Boolean {
        if (!intersects(this, r)) return false
        set(maxOf(left, r.left), maxOf(top, r.top), minOf(right, r.right), minOf(bottom, r.bottom)); return true
    }
    fun union(r: Rect) { if (r.isEmpty) return; if (isEmpty) set(r) else set(minOf(left, r.left), minOf(top, r.top), maxOf(right, r.right), maxOf(bottom, r.bottom)) }
    override fun equals(other: Any?) = other is Rect && other.left == left && other.top == top && other.right == right && other.bottom == bottom
    override fun hashCode() = ((left * 31 + top) * 31 + right) * 31 + bottom
    override fun toString() = "Rect($left, $top - $right, $bottom)"
    companion object { fun intersects(a: Rect, b: Rect) = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom }
}

class RectF(var left: Float = 0f, var top: Float = 0f, var right: Float = 0f, var bottom: Float = 0f) {
    constructor(r: Rect) : this(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
    constructor(r: RectF) : this(r.left, r.top, r.right, r.bottom)
    fun width() = right - left
    fun height() = bottom - top
    fun centerX() = (left + right) / 2f
    fun centerY() = (top + bottom) / 2f
    fun set(l: Float, t: Float, r: Float, b: Float) { left = l; top = t; right = r; bottom = b }
    fun set(o: RectF) = set(o.left, o.top, o.right, o.bottom)
    fun contains(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom
    fun offset(dx: Float, dy: Float) { left += dx; right += dx; top += dy; bottom += dy }
    fun inset(dx: Float, dy: Float) { left += dx; right -= dx; top += dy; bottom -= dy }
    val isEmpty: Boolean get() = left >= right || top >= bottom
    fun round(out: Rect) = out.set(kotlin.math.round(left).toInt(), kotlin.math.round(top).toInt(), kotlin.math.round(right).toInt(), kotlin.math.round(bottom).toInt())
    override fun equals(other: Any?) = other is RectF && other.left == left && other.top == top && other.right == right && other.bottom == bottom
    override fun hashCode() = left.hashCode() * 31 + top.hashCode()
}

class Point(var x: Int = 0, var y: Int = 0) { fun set(x: Int, y: Int) { this.x = x; this.y = y } }
class PointF(var x: Float = 0f, var y: Float = 0f)
object PixelFormat { const val TRANSLUCENT = -3; const val OPAQUE = -1; const val TRANSPARENT = -2; const val RGBA_8888 = 1 }
class Typeface private constructor() {
    companion object {
        val DEFAULT = Typeface(); val DEFAULT_BOLD = Typeface(); val MONOSPACE = Typeface(); val SANS_SERIF = Typeface()
        const val NORMAL = 0; const val BOLD = 1
        fun create(f: Typeface?, style: Int) = DEFAULT
        fun createFromAsset(a: android.content.AssetManager, path: String) = DEFAULT
        fun createFromFile(path: String) = DEFAULT
    }
}
class RenderEffect private constructor() {
    companion object {
        fun createBlurEffect(rx: Float, ry: Float, mode: Shader.TileMode) = RenderEffect()
        fun createRuntimeShaderEffect(s: RuntimeShader, name: String) = RenderEffect()
    }
}
class RuntimeShader(src: String) {
    fun setFloatUniform(name: String, vararg v: Float) {}
    fun setInputShader(name: String, s: Any?) {}
}
open class Shader { enum class TileMode { CLAMP, REPEAT, MIRROR, DECAL } }
class Matrix {
    fun setScale(sx: Float, sy: Float) {}
    fun postTranslate(dx: Float, dy: Float): Boolean = true
    fun reset() {}
}

class Paint(flags: Int = 0) {
    enum class Style { FILL, STROKE, FILL_AND_STROKE }
    enum class Align { LEFT, CENTER, RIGHT }
    var color = Color.BLACK
    var style = Style.FILL
    var strokeWidth = 0f
    var isAntiAlias = flags and ANTI_ALIAS_FLAG != 0
    var isFilterBitmap = flags and FILTER_BITMAP_FLAG != 0
    var isDither = false
    var textSize = 12f
    var textAlign = Align.LEFT
    var typeface: Typeface? = null
    var alpha: Int get() = Color.alpha(color); set(v) { color = (color and 0xFFFFFF) or (v shl 24) }
    var xfermode: Any? = null
    var colorFilter: Any? = null
    var shader: Shader? = null
    fun setARGB(a: Int, r: Int, g: Int, b: Int) { color = Color.argb(a, r, g, b) }
    fun measureText(s: String): Float = s.length * textSize * 0.6f
    fun set(p: Paint) { color = p.color; style = p.style; strokeWidth = p.strokeWidth; isAntiAlias = p.isAntiAlias; isFilterBitmap = p.isFilterBitmap; textSize = p.textSize }
    internal fun skia(): org.jetbrains.skia.Paint = org.jetbrains.skia.Paint().also {
        it.color = color
        it.mode = when (style) { Style.FILL -> org.jetbrains.skia.PaintMode.FILL; Style.STROKE -> org.jetbrains.skia.PaintMode.STROKE; else -> org.jetbrains.skia.PaintMode.STROKE_AND_FILL }
        it.strokeWidth = strokeWidth
        it.isAntiAlias = isAntiAlias
    }
    companion object { const val ANTI_ALIAS_FLAG = 1; const val FILTER_BITMAP_FLAG = 2; const val DITHER_FLAG = 4 }
}

class Path {
    internal val p = org.jetbrains.skia.PathBuilder()
    fun moveTo(x: Float, y: Float) { p.moveTo(x, y) }
    fun lineTo(x: Float, y: Float) { p.lineTo(x, y) }
    fun close() { p.closePath() }
    fun reset() { p.reset() }
    fun addRect(l: Float, t: Float, r: Float, b: Float, d: Direction) { p.addRect(org.jetbrains.skia.Rect.makeLTRB(l, t, r, b)) }
    enum class Direction { CW, CCW }
}

/** android.graphics.Canvas over a Skia canvas (ViewHost's draw scope, or a Bitmap's). */
class Canvas(internal val c: org.jetbrains.skia.Canvas) {
    constructor(b: Bitmap) : this(BitmapCanvas.of(b))
    private var bitmapTarget: Bitmap? = null
    val width: Int get() = 0
    val height: Int get() = 0
    fun save(): Int = c.save()
    fun saveLayer(l: Float, t: Float, r: Float, b: Float, p: Paint?): Int = c.saveLayer(l, t, r, b, p?.skia())
    fun saveLayer(r: RectF?, p: Paint?): Int = if (r == null) c.saveLayer(null, p?.skia()) else saveLayer(r.left, r.top, r.right, r.bottom, p)
    fun restore() { c.restore() }
    fun restoreToCount(n: Int) { c.restoreToCount(n) }
    fun translate(dx: Float, dy: Float) { c.translate(dx, dy) }
    fun scale(sx: Float, sy: Float) { c.scale(sx, sy) }
    fun rotate(deg: Float) { c.rotate(deg) }
    fun clipRect(l: Float, t: Float, r: Float, b: Float): Boolean { c.clipRect(org.jetbrains.skia.Rect.makeLTRB(l, t, r, b)); return true }
    fun clipRect(r: Rect): Boolean = clipRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
    fun clipRect(r: RectF): Boolean = clipRect(r.left, r.top, r.right, r.bottom)
    fun drawColor(color: Int) { c.clear(color) }
    fun drawColor(color: Int, mode: Any?) { c.drawPaint(org.jetbrains.skia.Paint().also { it.color = color }) }
    fun drawARGB(a: Int, r: Int, g: Int, b: Int) = drawColor(Color.argb(a, r, g, b))
    fun drawRect(l: Float, t: Float, r: Float, b: Float, p: Paint) { c.drawRect(org.jetbrains.skia.Rect.makeLTRB(l, t, r, b), p.skia()) }
    fun drawRect(r: Rect, p: Paint) = drawRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat(), p)
    fun drawRect(r: RectF, p: Paint) = drawRect(r.left, r.top, r.right, r.bottom, p)
    fun drawRoundRect(r: RectF, rx: Float, ry: Float, p: Paint) { c.drawRRect(org.jetbrains.skia.RRect.makeLTRB(r.left, r.top, r.right, r.bottom, rx, ry), p.skia()) }
    fun drawCircle(cx: Float, cy: Float, radius: Float, p: Paint) { c.drawCircle(cx, cy, radius, p.skia()) }
    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, p: Paint) { c.drawLine(x0, y0, x1, y1, p.skia()) }
    fun drawPath(path: Path, p: Paint) { c.drawPath(path.p.snapshot(), p.skia()) }
    fun drawText(s: String, x: Float, y: Float, p: Paint) {
        val font = org.jetbrains.skia.Font(null, p.textSize)
        val w = font.measureTextWidth(s)
        val dx = when (p.textAlign) { Paint.Align.CENTER -> -w / 2; Paint.Align.RIGHT -> -w; else -> 0f }
        c.drawString(s, x + dx, y, font, p.skia())
    }
    fun drawBitmap(b: Bitmap, src: Rect?, dst: Rect, p: Paint?) = drawBitmap(b, src, RectF(dst), p)
    fun drawBitmap(b: Bitmap, src: Rect?, dst: RectF, p: Paint?) {
        val img = BitmapCanvas.image(b)
        val s = src?.let { org.jetbrains.skia.Rect.makeLTRB(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
            ?: org.jetbrains.skia.Rect.makeWH(b.width.toFloat(), b.height.toFloat())
        val filter = if (p?.isFilterBitmap == true) org.jetbrains.skia.SamplingMode.LINEAR else org.jetbrains.skia.SamplingMode.DEFAULT
        c.drawImageRect(img, s, org.jetbrains.skia.Rect.makeLTRB(dst.left, dst.top, dst.right, dst.bottom), filter, p?.skia(), true)
    }
    fun drawBitmap(b: Bitmap, x: Float, y: Float, p: Paint?) = drawBitmap(b, null, RectF(x, y, x + b.width, y + b.height), p)
    fun drawBitmap(b: Bitmap, m: Matrix, p: Paint?) = drawBitmap(b, 0f, 0f, p)
}

/** Bitmaps as Skia images for drawing, and canvases that draw into a Bitmap's pixels. */
internal object BitmapCanvas {
    fun image(b: Bitmap): org.jetbrains.skia.Image {
        val bytes = ByteArray(b.width * b.height * 4)
        val px = IntArray(b.width * b.height)
        b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        for (i in px.indices) {
            val c = px[i]
            bytes[i * 4] = (c and 0xFF).toByte(); bytes[i * 4 + 1] = (c shr 8 and 0xFF).toByte()
            bytes[i * 4 + 2] = (c shr 16 and 0xFF).toByte(); bytes[i * 4 + 3] = (c ushr 24).toByte()
        }
        return org.jetbrains.skia.Image.makeRaster(org.jetbrains.skia.ImageInfo.makeN32(b.width, b.height, org.jetbrains.skia.ColorAlphaType.UNPREMUL), bytes, b.width * 4)
    }
    /** A canvas over a fresh Skia bitmap; drawing there doesn't reach the android Bitmap (the app draws into bitmaps nowhere on screen). */
    fun of(b: Bitmap): org.jetbrains.skia.Canvas {
        val sb = org.jetbrains.skia.Bitmap()
        sb.allocN32Pixels(b.width, b.height)
        return org.jetbrains.skia.Canvas(sb)
    }
}

/** SurfaceTexture: DS RESOLUTION's GPU frames (not on iOS). */
class SurfaceTexture(tex: Int, singleBuffered: Boolean = false) {
    fun updateTexImage() {}
    fun getTransformMatrix(m: FloatArray) {}
    val timestamp: Long get() = 0
    fun release() {}
    fun setDefaultBufferSize(w: Int, h: Int) {}
    fun setOnFrameAvailableListener(l: ((SurfaceTexture) -> Unit)?) {}
    fun setOnFrameAvailableListener(l: OnFrameAvailableListener?, h: android.os.Handler?) {}
    fun interface OnFrameAvailableListener { fun onFrameAvailable(t: SurfaceTexture) }
    fun detachFromGLContext() {}
    fun attachToGLContext(tex: Int) {}
    val isReleased: Boolean get() = false
}
