@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.animation

open class Animator { open fun start() {}; open fun cancel() {}; open fun end() {}; open var duration = 300L; val isRunning = false }
class ValueAnimator : Animator() {
    val animatedValue: Any? = 1f
    val animatedFraction = 1f
    fun addUpdateListener(l: (ValueAnimator) -> Unit) {}
    fun addListener(l: Any?) {}
    var interpolator: Any? = null
    companion object { fun ofFloat(vararg v: Float) = ValueAnimator(); fun ofInt(vararg v: Int) = ValueAnimator() }
}
open class AnimatorListenerAdapter
