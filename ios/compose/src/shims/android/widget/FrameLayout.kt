@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.widget

import android.content.Context
import android.view.Gravity
import android.view.MeasureSpec
import android.view.View
import android.view.ViewGroup

/** FrameLayout's measure and layout as Android's (children stacked, each by its gravity and margins). */
open class FrameLayout(context: Context) : ViewGroup(context) {
    open class LayoutParams(width: Int, height: Int, var gravity: Int = UNSPECIFIED_GRAVITY) : MarginLayoutParams(width, height) {
        constructor(src: ViewGroup.LayoutParams) : this(src.width, src.height) {
            if (src is MarginLayoutParams) { leftMargin = src.leftMargin; topMargin = src.topMargin; rightMargin = src.rightMargin; bottomMargin = src.bottomMargin }
            if (src is LayoutParams) gravity = src.gravity
        }
        companion object { const val UNSPECIFIED_GRAVITY = -1; const val MATCH_PARENT = -1; const val FILL_PARENT = -1; const val WRAP_CONTENT = -2 }
    }

    var foregroundGravity = 0
    var measureAllChildren = false

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams = if (p is LayoutParams) p else LayoutParams(p)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxW = 0
        var maxH = 0
        val matchParent = ArrayList<View>()
        val measureMatch = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY || MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE && !measureAllChildren) continue
            measureChildWithMargins(c, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = c.layoutParams as MarginLayoutParams
            maxW = maxOf(maxW, c.measuredWidth + lp.leftMargin + lp.rightMargin)
            maxH = maxOf(maxH, c.measuredHeight + lp.topMargin + lp.bottomMargin)
            if (measureMatch && (lp.width == ViewGroup.LayoutParams.MATCH_PARENT || lp.height == ViewGroup.LayoutParams.MATCH_PARENT)) matchParent.add(c)
        }
        maxW += paddingLeft + paddingRight
        maxH += paddingTop + paddingBottom
        maxW = maxOf(maxW, suggestedMinimumWidth)
        maxH = maxOf(maxH, suggestedMinimumHeight)
        setMeasuredDimension(resolveSize(maxW, widthMeasureSpec), resolveSize(maxH, heightMeasureSpec))
        if (matchParent.size > 1) for (c in matchParent) {
            val lp = c.layoutParams as MarginLayoutParams
            val ws = if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT)
                MeasureSpec.makeMeasureSpec(maxOf(0, measuredWidth - paddingLeft - paddingRight - lp.leftMargin - lp.rightMargin), MeasureSpec.EXACTLY)
            else getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width)
            val hs = if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT)
                MeasureSpec.makeMeasureSpec(maxOf(0, measuredHeight - paddingTop - paddingBottom - lp.topMargin - lp.bottomMargin), MeasureSpec.EXACTLY)
            else getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height)
            c.measure(ws, hs)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val pl = paddingLeft; val pr = right - left - paddingRight
        val pt = paddingTop; val pb = bottom - top - paddingBottom
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            val lp = c.layoutParams as? LayoutParams ?: LayoutParams(c.layoutParams)
            val w = c.measuredWidth; val h = c.measuredHeight
            var g = lp.gravity
            if (g == -1) g = Gravity.TOP or Gravity.START
            val hg = g and Gravity.HORIZONTAL_GRAVITY_MASK
            val vg = g and Gravity.VERTICAL_GRAVITY_MASK
            val x = when (hg) {
                Gravity.CENTER_HORIZONTAL -> pl + (pr - pl - w) / 2 + lp.leftMargin - lp.rightMargin
                Gravity.RIGHT -> pr - w - lp.rightMargin
                else -> pl + lp.leftMargin
            }
            val y = when (vg) {
                Gravity.CENTER_VERTICAL -> pt + (pb - pt - h) / 2 + lp.topMargin - lp.bottomMargin
                Gravity.BOTTOM -> pb - h - lp.bottomMargin
                else -> pt + lp.topMargin
            }
            c.layout(x, y, x + w, y + h)
        }
    }

    companion object {
        fun resolveSize(size: Int, spec: Int): Int = when (MeasureSpec.getMode(spec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(spec)
            MeasureSpec.AT_MOST -> minOf(size, MeasureSpec.getSize(spec))
            else -> size
        }
    }
}

/** A FrameLayout by another name (the app uses none of LinearLayout's own rules). */
open class LinearLayout(context: Context) : FrameLayout(context) {
    var orientation = 0
    companion object { const val HORIZONTAL = 0; const val VERTICAL = 1 }
}
open class RelativeLayout(context: Context) : FrameLayout(context)

/** A TextView: its text drawn on the view's canvas. */
open class TextView(context: Context) : View(context) {
    var text: CharSequence = ""
        set(v) { field = v; requestLayout() }
    fun setText(t: CharSequence?) { text = t ?: "" }
    var textSizePx = 14f * 3
    fun setTextSize(unit: Int, size: Float) { textSizePx = if (unit == 0) size else size * 3; requestLayout() }
    fun setTextSize(size: Float) = setTextSize(2, size)
    var textColor = 0xFFFFFFFF.toInt()
    fun setTextColor(c: Int) { textColor = c; invalidate() }
    var gravity = 0
    var typeface: android.graphics.Typeface? = null
    fun setShadowLayer(r: Float, dx: Float, dy: Float, c: Int) {}
    fun setSingleLine(b: Boolean = true) {}
    var maxLines = Int.MAX_VALUE
    var includeFontPadding = true
    fun setLineSpacing(add: Float, mult: Float) {}
    override val drawsItself get() = true
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val lines = text.toString().lines()
        val w = (lines.maxOfOrNull { it.length } ?: 0) * textSizePx * 0.6f + paddingLeft + paddingRight
        val h = lines.size * textSizePx * 1.25f + paddingTop + paddingBottom
        setMeasuredDimension(FrameLayout.resolveSize(w.toInt(), widthMeasureSpec), FrameLayout.resolveSize(h.toInt(), heightMeasureSpec))
    }
    override fun onDraw(canvas: android.graphics.Canvas) {
        val p = android.graphics.Paint().also { it.color = textColor; it.textSize = textSizePx }
        text.toString().lines().forEachIndexed { i, l -> canvas.drawText(l, paddingLeft.toFloat(), paddingTop + (i + 1) * textSizePx * 1.1f, p) }
    }
}
