// androidx.compose.ui.platform's View-side classes: ComposeView (a View whose content is Compose: ViewHost composes it
// in place), its composition strategies, and AndroidUiDispatcher.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package androidx.compose.ui.platform

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

open class AbstractComposeView(context: Context) : android.widget.FrameLayout(context)

class ComposeView(context: Context) : android.widget.FrameLayout(context) {
    var content: (@Composable () -> Unit)? by mutableStateOf(null)
        private set
    fun setContent(content: @Composable () -> Unit) { this.content = content; requestLayout() }
    fun setViewCompositionStrategy(s: ViewCompositionStrategy) {}
    fun disposeComposition() { content = null }
    fun setParentCompositionContext(c: androidx.compose.runtime.CompositionContext?) {}
    val hasComposition: Boolean get() = content != null
    var isTransitionGroup = false
    /** ViewHost's measurer for wrap_content sizes (the content's intrinsic size, inside the host's measure pass). */
    internal var measurer: ((ComposeView, Int, Int) -> Pair<Int, Int>)? = null
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val exact = android.view.MeasureSpec.getMode(widthMeasureSpec) == android.view.MeasureSpec.EXACTLY &&
            android.view.MeasureSpec.getMode(heightMeasureSpec) == android.view.MeasureSpec.EXACTLY
        if (!exact) {
            val m = measurer ?: host?.measurer
            if (m != null) {
                val (w, h) = m(this, widthMeasureSpec, heightMeasureSpec)
                setMeasuredDimension(w, h)
                return
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

interface ViewCompositionStrategy {
    object DisposeOnDetachedFromWindow : ViewCompositionStrategy
    object DisposeOnDetachedFromWindowOrReleasedFromPool : ViewCompositionStrategy
    object DisposeOnViewTreeLifecycleDestroyed : ViewCompositionStrategy
    class DisposeOnLifecycleDestroyed(l: Any?) : ViewCompositionStrategy
}

object AndroidUiDispatcher {
    val Main: kotlinx.coroutines.CoroutineDispatcher get() = kotlinx.coroutines.Dispatchers.Main
    val CurrentThread: kotlinx.coroutines.CoroutineDispatcher get() = kotlinx.coroutines.Dispatchers.Main
}

/** The View a composable runs in (Android's LocalView): the host's root here. */
val LocalView = androidx.compose.runtime.staticCompositionLocalOf<View> { View(Context()) }

/** The composition context a View tree's ComposeViews share (Android sets it on the root): one Compose tree here. */
var View.compositionContext: androidx.compose.runtime.CompositionContext?
    get() = null
    set(v) {}

/** Android's LocalConfiguration: the device's (orientation, screen dp). */
val LocalConfiguration = androidx.compose.runtime.staticCompositionLocalOf { android.content.res.Configuration() }
