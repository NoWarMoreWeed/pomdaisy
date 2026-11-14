@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.view

class ViewConfiguration private constructor() {
    val scaledTouchSlop: Int get() = (8 * com.pokedaisy.ios.IosScreen.density).toInt()
    val scaledDoubleTapSlop: Int get() = (100 * com.pokedaisy.ios.IosScreen.density).toInt()
    val scaledMinimumFlingVelocity: Int get() = 50
    val scaledMaximumFlingVelocity: Int get() = 8000
    val scaledPagingTouchSlop: Int get() = scaledTouchSlop * 2
    companion object {
        fun get(c: android.content.Context) = ViewConfiguration()
        fun getLongPressTimeout() = 400
        fun getTapTimeout() = 100
        fun getDoubleTapTimeout() = 300
        fun getKeyRepeatTimeout() = 400
        fun getKeyRepeatDelay() = 50
    }
}

/** PixelCopy: a View's picture (DsShots' game snapshots) - not available on iOS yet: always fails. */
object PixelCopy {
    const val SUCCESS = 0
    const val ERROR_UNKNOWN = 1
    fun interface OnPixelCopyFinishedListener { fun onPixelCopyFinished(result: Int) }
    fun request(src: SurfaceView, dst: android.graphics.Bitmap, l: OnPixelCopyFinishedListener, h: android.os.Handler) { h.post { l.onPixelCopyFinished(ERROR_UNKNOWN) } }
    fun request(src: SurfaceView, r: android.graphics.Rect?, dst: android.graphics.Bitmap, l: OnPixelCopyFinishedListener, h: android.os.Handler) { h.post { l.onPixelCopyFinished(ERROR_UNKNOWN) } }
    fun request(src: Window, dst: android.graphics.Bitmap, l: OnPixelCopyFinishedListener, h: android.os.Handler) { h.post { l.onPixelCopyFinished(ERROR_UNKNOWN) } }
    fun request(src: Window, r: android.graphics.Rect?, dst: android.graphics.Bitmap, l: OnPixelCopyFinishedListener, h: android.os.Handler) { h.post { l.onPixelCopyFinished(ERROR_UNKNOWN) } }
    fun request(src: Surface, dst: android.graphics.Bitmap, l: OnPixelCopyFinishedListener, h: android.os.Handler) { h.post { l.onPixelCopyFinished(ERROR_UNKNOWN) } }
}
interface WindowInsetsAnimation
class HapticFeedbackConstants { companion object { const val VIRTUAL_KEY = 1; const val KEYBOARD_TAP = 3; const val CLOCK_TICK = 4; const val CONTEXT_CLICK = 6; const val LONG_PRESS = 0; const val CONFIRM = 16; const val REJECT = 17; const val SEGMENT_TICK = 26; const val FLAG_IGNORE_GLOBAL_SETTING = 2 } }
class SoundEffectConstants { companion object { const val CLICK = 0 } }
