// The window and display an Activity has: one, the iPhone's screen.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.view

class Display private constructor(val displayId: Int) {
    val name: String get() = "Built-in screen"
    val refreshRate: Float get() = com.pokedaisy.ios.IosScreen.refreshRate
    val rotation: Int get() = 0
    val state: Int get() = STATE_ON
    val flags = 0
    val isValid = true
    val supportedModes: Array<Mode> get() = arrayOf(Mode())
    val mode: Mode get() = Mode()
    fun getRealSize(p: android.graphics.Point) = p.set(com.pokedaisy.ios.IosScreen.widthPx, com.pokedaisy.ios.IosScreen.heightPx)
    fun getSize(p: android.graphics.Point) = getRealSize(p)
    fun getRealMetrics(m: android.util.DisplayMetrics) = m.setTo(android.util.DisplayMetrics.current())
    fun getMetrics(m: android.util.DisplayMetrics) = getRealMetrics(m)
    val cutout: DisplayCutout? get() = null
    val hdrCapabilities: Any? get() = null
    class Mode { val refreshRate: Float get() = com.pokedaisy.ios.IosScreen.refreshRate; val physicalWidth get() = com.pokedaisy.ios.IosScreen.widthPx; val physicalHeight get() = com.pokedaisy.ios.IosScreen.heightPx; val modeId = 1 }
    companion object {
        const val DEFAULT_DISPLAY = 0
        const val FLAG_PRIVATE = 4
        const val FLAG_PRESENTATION = 8
        const val FLAG_SECURE = 2
        const val TYPE_INTERNAL = 1
        const val STATE_ON = 2
        const val STATE_OFF = 1
        internal val main = Display(DEFAULT_DISPLAY)
    }
}
class DisplayCutout { val safeInsetTop = 0; val safeInsetBottom = 0; val safeInsetLeft = 0; val safeInsetRight = 0; val boundingRects: List<android.graphics.Rect> = emptyList() }

class Window {
    var attributes = WindowManager.LayoutParams()
    val decorView: View get() = com.pokedaisy.ios.ActivityHost.top?.let { it.contentView } ?: View(android.content.Context())
    val insetsController: WindowInsetsController? get() = WindowInsetsController()
    var statusBarColor = 0
    var navigationBarColor = 0
    var isNavigationBarContrastEnforced = false
    var isStatusBarContrastEnforced = false
    fun addFlags(f: Int) {}
    fun clearFlags(f: Int) {}
    fun setFlags(f: Int, mask: Int) {}
    fun setAttributes(a: WindowManager.LayoutParams) {}
    fun setDecorFitsSystemWindows(b: Boolean) {}
    fun setSustainedPerformanceMode(b: Boolean) {}
    fun setBackgroundDrawable(d: android.graphics.drawable.Drawable?) {}
    fun setBackgroundDrawableResource(id: Int) {}
    fun setLayout(w: Int, h: Int) {}
    fun setGravity(g: Int) {}
    fun setType(t: Int) {}
    fun setSoftInputMode(m: Int) {}
    fun setWindowAnimations(a: Int) {}
    fun setPreferMinimalPostProcessing(b: Boolean) {}
    fun setHideOverlayWindows(b: Boolean) {}
    fun takeSurface(c: Any?) {}
    fun setDimAmount(a: Float) {}
    val context: android.content.Context get() = com.pokedaisy.ios.ActivityHost.top ?: android.content.Context()
    val windowManager: WindowManager get() = WindowManager()
}

class WindowInsetsController {
    fun hide(types: Int) {}
    fun show(types: Int) {}
    var systemBarsBehavior = 0
    companion object { const val BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2 }
}

open class WindowManager {
    val defaultDisplay: Display get() = Display.main
    fun addView(v: View, p: ViewGroup.LayoutParams) {}
    fun removeView(v: View) {}
    fun removeViewImmediate(v: View) {}
    fun updateViewLayout(v: View, p: ViewGroup.LayoutParams) {}
    val currentWindowMetrics: WindowMetrics get() = WindowMetrics()
    val maximumWindowMetrics: WindowMetrics get() = WindowMetrics()
    class WindowMetrics { val bounds: android.graphics.Rect get() = android.graphics.Rect(0, 0, com.pokedaisy.ios.IosScreen.widthPx, com.pokedaisy.ios.IosScreen.heightPx) }
    open class LayoutParams(w: Int = MATCH_PARENT, h: Int = MATCH_PARENT) : ViewGroup.LayoutParams(w, h) {
        constructor(w: Int, h: Int, type: Int, flags: Int, format: Int) : this(w, h)
        var flags = 0
        var type = 0
        var format = 0
        var gravity = 0
        var x = 0
        var y = 0
        var token: android.os.IBinder? = null
        var title: CharSequence = ""
        var screenBrightness = -1f
        var preferredRefreshRate = 0f
        var preferredDisplayModeId = 0
        var layoutInDisplayCutoutMode = 0
        var rotationAnimation = 0
        var dimAmount = 0f
        var inputFeatures = 0
        var softInputMode = 0
        var windowAnimations = 0
        fun setTitle(t: CharSequence) { title = t }
        companion object {
            const val MATCH_PARENT = -1
            const val WRAP_CONTENT = -2
            const val FLAG_KEEP_SCREEN_ON = 0x80
            const val FLAG_FULLSCREEN = 0x400
            const val FLAG_SECURE = 0x2000
            const val FLAG_NOT_FOCUSABLE = 0x8
            const val FLAG_NOT_TOUCHABLE = 0x10
            const val FLAG_NOT_TOUCH_MODAL = 0x20
            const val FLAG_LAYOUT_NO_LIMITS = 0x200
            const val FLAG_LAYOUT_IN_SCREEN = 0x100
            const val FLAG_HARDWARE_ACCELERATED = 0x01000000
            const val FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = Int.MIN_VALUE
            const val FLAG_WATCH_OUTSIDE_TOUCH = 0x40000
            const val FLAG_SPLIT_TOUCH = 0x800000
            const val FLAG_ALT_FOCUSABLE_IM = 0x20000
            const val TYPE_APPLICATION = 2
            const val TYPE_APPLICATION_PANEL = 1000
            const val TYPE_APPLICATION_SUB_PANEL = 1002
            const val TYPE_APPLICATION_OVERLAY = 2038
            const val TYPE_PRIVATE_PRESENTATION = 2030
            const val LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1
            const val LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS = 3
            const val ROTATION_ANIMATION_SEAMLESS = 3
            const val ROTATION_ANIMATION_JUMPCUT = 2
            const val SOFT_INPUT_ADJUST_RESIZE = 16
            const val SOFT_INPUT_STATE_HIDDEN = 2
        }
    }
}

object WindowInsetsCompatTypes
