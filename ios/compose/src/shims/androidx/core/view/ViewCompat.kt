@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package androidx.core.view

object ViewCompat {
    fun setOnApplyWindowInsetsListener(v: android.view.View, l: ((android.view.View, WindowInsetsCompat) -> WindowInsetsCompat)?) {}
    fun getRootWindowInsets(v: android.view.View): WindowInsetsCompat? = WindowInsetsCompat()
    fun requestApplyInsets(v: android.view.View) {}
    fun setLayerType(v: android.view.View, t: Int, p: android.graphics.Paint?) {}
    fun onApplyWindowInsets(v: android.view.View, i: WindowInsetsCompat): WindowInsetsCompat = i
}
object WindowCompat {
    fun setDecorFitsSystemWindows(w: android.view.Window, fits: Boolean) {}
    fun getInsetsController(w: android.view.Window, v: android.view.View) = WindowInsetsControllerCompat(w, v)
}
class WindowInsetsCompat {
    fun getInsets(type: Int) = androidx.core.graphics.Insets.NONE
    fun getInsetsIgnoringVisibility(type: Int) = androidx.core.graphics.Insets.NONE
    fun isVisible(type: Int) = false
    val displayCutout: Any? = null
    object Type {
        fun systemBars() = 7; fun statusBars() = 1; fun navigationBars() = 2; fun displayCutout() = 128; fun ime() = 8
        fun systemGestures() = 16; fun captionBar() = 4; fun tappableElement() = 64; fun mandatorySystemGestures() = 32
    }
    companion object { val CONSUMED = WindowInsetsCompat() }
}
class WindowInsetsControllerCompat(w: android.view.Window, v: android.view.View) {
    fun hide(types: Int) {}
    fun show(types: Int) {}
    var systemBarsBehavior = 0
    var isAppearanceLightStatusBars = false
    var isAppearanceLightNavigationBars = false
    companion object { const val BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2; const val BEHAVIOR_DEFAULT = 1 }
}
