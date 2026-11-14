// PomDaisy (iOS): an Android View tree inside Compose. One SubcomposeLayout runs Android's measure and layout over the
// tree (the app's own onMeasure / onLayout), then places what each view shows at its place: a ComposeView's content,
// a GLSurfaceView's UIKit view, an onDraw view's drawing, a background colour. Touches on views that aren't Compose go
// to their dispatchTouchEvent as MotionEvents.
package com.pokedaisy.ios

import android.view.MeasureSpec
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints

object ViewHost {
    class Root internal constructor(val view: View) {
        var version by mutableIntStateOf(0)
        internal var scope: SubcomposeMeasureScope? = null
        internal val measured = HashMap<View, Measurable>()
        fun changed() { version++ }
        /** A ComposeView's wrap_content size: its content's intrinsic size within the specs. */
        val measurer: (androidx.compose.ui.platform.ComposeView, Int, Int) -> Pair<Int, Int> = { v, ws, hs ->
            val m = measurable(v)
            val maxW = if (MeasureSpec.getMode(ws) == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE / 2 else MeasureSpec.getSize(ws)
            val maxH = if (MeasureSpec.getMode(hs) == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE / 2 else MeasureSpec.getSize(hs)
            val w = when (MeasureSpec.getMode(ws)) {
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(ws)
                else -> runCatching { m?.maxIntrinsicWidth(maxH) ?: 0 }.getOrDefault(maxW).coerceAtMost(maxW)
            }
            val h = when (MeasureSpec.getMode(hs)) {
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(hs)
                else -> runCatching { m?.maxIntrinsicHeight(w) ?: 0 }.getOrDefault(maxH).coerceAtMost(maxH)
            }
            w to h
        }
        internal fun measurable(v: View): Measurable? {
            measured[v]?.let { return it }
            val s = scope ?: return null
            val m = s.subcompose(v) { Shown(v) }.firstOrNull() ?: return null
            measured[v] = m
            return m
        }
    }

    /** The tree [root] fills this composable's space. */
    @Composable
    fun Host(root: View, modifier: Modifier = Modifier) {
        val r = remember(root) { Root(root) }
        DisposableEffect(root) {
            root.host = r
            root.dispatchAttached(true)
            onDispose { root.dispatchAttached(false); root.host = null }
        }
        // A frame clock for the views that redraw on their own (invalidate from other threads lands here too).
        LaunchedEffect(r) { while (true) withFrameNanos { animations.step(it) } }
        SubcomposeLayout(modifier) { constraints ->
            r.version
            val w = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
            val h = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
            r.scope = this
            r.measured.clear()
            root.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            root.layout(0, 0, w, h)
            val items = ArrayList<Placed>()
            collect(root, 0f, 0f, 1f, items)
            val placeables = items.map { p ->
                val m = r.measurable(p.view)
                p to m?.measure(Constraints.fixed(p.view.width.coerceAtLeast(0), p.view.height.coerceAtLeast(0)))
            }
            r.scope = null
            layout(w, h) {
                for ((p, pl) in placeables) pl?.placeWithLayer(p.x.toInt(), p.y.toInt()) { alpha = p.alpha }
            }
        }
    }

    private class Placed(val view: View, val x: Float, val y: Float, val alpha: Float)

    /** The views that show something, in drawing order, at their place in the root. */
    private fun collect(v: View, ox: Float, oy: Float, alpha: Float, out: MutableList<Placed>) {
        if (v.visibility != View.VISIBLE) return
        val x = ox + v.left + v.translationX
        val y = oy + v.top + v.translationY
        val a = alpha * v.alpha
        if (a <= 0f) return
        if (v is androidx.compose.ui.platform.ComposeView || v is android.opengl.GLSurfaceView || v.drawsItself || v.backgroundColor != 0)
            out.add(Placed(v, x, y, a))
        if (v is ViewGroup && v !is androidx.compose.ui.platform.ComposeView) for (c in v.childList) collect(c, x, y, a, out)
    }

    /** What one view shows. */
    @Composable
    private fun Shown(v: View) {
        when {
            v is androidx.compose.ui.platform.ComposeView -> {
                val bg = v.backgroundColor
                Box(if (bg != 0) Modifier.fillMaxSize().background(Color(bg)) else Modifier.fillMaxSize()) { v.content?.invoke() }
            }
            v is android.opengl.GLSurfaceView -> android.opengl.GlViews.Show(v)
            v.drawsItself -> {
                val r = v.host
                Canvas(Modifier.fillMaxSize().then(touches(v))) {
                    r?.version
                    drawIntoCanvas { c -> v.draw(android.graphics.Canvas(c.nativeCanvas)) }
                }
            }
            else -> Box(Modifier.fillMaxSize().background(Color(v.backgroundColor)).then(touches(v)))
        }
    }

    /** Compose pointers as MotionEvents to [v] (a view that isn't Compose). */
    private fun touches(v: View): Modifier = Modifier.pointerInput(v) {
        awaitPointerEventScope {
            var down = 0L
            while (true) {
                val e = awaitPointerEvent()
                val now = android.os.SystemClock.uptimeMillis()
                val pressed = e.changes.filter { it.pressed }
                val action = when (e.type) {
                    PointerEventType.Press -> if (pressed.size <= 1) { down = now; MotionEvent.ACTION_DOWN } else MotionEvent.ACTION_POINTER_DOWN
                    PointerEventType.Release -> if (pressed.isEmpty()) MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_UP
                    PointerEventType.Move -> MotionEvent.ACTION_MOVE
                    else -> continue
                }
                val cs = e.changes
                val ev = MotionEvent.obtain(down, now, action, IntArray(cs.size) { cs[it].id.value.toInt() },
                    FloatArray(cs.size) { cs[it].position.x }, FloatArray(cs.size) { cs[it].position.y })
                if (v.dispatchTouchEvent(ev)) cs.forEach { it.consume() }
            }
        }
    }

    // ---- animate(): the values eased each frame ----
    private class Anim(val v: View, val a0: Float, val a1: Float?, val x0: Float, val x1: Float?, val y0: Float, val y1: Float?, val ms: Long, val end: Runnable) {
        var start = -1L
    }
    private object animations {
        val list = ArrayList<Anim>()
        fun step(nanos: Long) {
            if (list.isEmpty()) return
            val done = ArrayList<Anim>()
            for (a in list.toList()) {
                if (a.start < 0) a.start = nanos
                val t = if (a.ms <= 0) 1f else ((nanos - a.start) / 1e6f / a.ms).coerceIn(0f, 1f)
                a.a1?.let { a.v.alpha = a.a0 + (it - a.a0) * t }
                a.x1?.let { a.v.translationX = a.x0 + (it - a.x0) * t }
                a.y1?.let { a.v.translationY = a.y0 + (it - a.y0) * t }
                if (t >= 1f) done.add(a)
            }
            list.removeAll(done)
            done.forEach { it.end.run() }
        }
    }
    fun animate(v: View, alpha: Float?, tx: Float?, ty: Float?, ms: Long, end: Runnable) {
        animations.list.removeAll { it.v === v }
        if (v.host == null) { end.run(); return }
        animations.list.add(Anim(v, v.alpha, alpha, v.translationX, tx, v.translationY, ty, ms, end))
    }
}
