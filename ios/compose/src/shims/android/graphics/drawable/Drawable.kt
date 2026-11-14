@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.graphics.drawable

open class Drawable { open fun draw(c: android.graphics.Canvas) {}; var alpha = 255 }
class ColorDrawable(var color: Int = 0) : Drawable()
class GradientDrawable : Drawable() {
    var cornerRadius = 0f
    fun setColor(c: Int) {}
    fun setStroke(w: Int, c: Int) {}
    var shape = 0
    companion object { const val RECTANGLE = 0; const val OVAL = 1 }
}
class BitmapDrawable(val bitmap: android.graphics.Bitmap?) : Drawable()
class Icon private constructor() { companion object { fun createWithBitmap(b: android.graphics.Bitmap) = Icon(); fun createWithResource(c: Any?, id: Int) = Icon() } }
