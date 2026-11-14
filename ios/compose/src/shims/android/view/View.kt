// android.view's View and ViewGroup as the app's game screen uses them: measure / layout with Android's rules (the app's
// own FrameLayout subclasses run as written), visibility, alpha, translation, posting to the main thread, touch
// dispatch. ViewHost (com.pokedaisy.ios) runs the tree inside one Compose layout: ComposeViews are subcomposed in
// place, GLSurfaceView becomes a UIKit view, an onDraw view draws on a Skia canvas.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")

package android.view

import android.content.Context
import android.graphics.Canvas

typealias MeasureSpec = View.MeasureSpec

object MeasureSpecImpl {
    const val UNSPECIFIED = 0
    const val EXACTLY = 1 shl 30
    const val AT_MOST = 2 shl 30
    private const val MODE_MASK = 3 shl 30
    fun makeMeasureSpec(size: Int, mode: Int) = (size and MODE_MASK.inv()) or (mode and MODE_MASK)
    fun getMode(spec: Int) = spec and MODE_MASK
    fun getSize(spec: Int) = spec and MODE_MASK.inv()
    fun toString(spec: Int) = "MeasureSpec(${getMode(spec)}, ${getSize(spec)})"
}

object Gravity {
    const val NO_GRAVITY = 0
    const val LEFT = 3
    const val RIGHT = 5
    const val START = 0x00800003
    const val END = 0x00800005
    const val TOP = 48
    const val BOTTOM = 80
    const val CENTER_HORIZONTAL = 1
    const val CENTER_VERTICAL = 16
    const val CENTER = 17
    const val FILL = 119
    const val FILL_HORIZONTAL = 7
    const val FILL_VERTICAL = 112
    const val HORIZONTAL_GRAVITY_MASK = 7
    const val VERTICAL_GRAVITY_MASK = 112
    const val RELATIVE_HORIZONTAL_GRAVITY_MASK = 0x00800007
}

open class View(val context: Context) {
    object MeasureSpec {
        const val UNSPECIFIED = 0
        const val EXACTLY = 1 shl 30
        const val AT_MOST = 2 shl 30
        private const val MODE_MASK = 3 shl 30
        fun makeMeasureSpec(size: Int, mode: Int) = (size and MODE_MASK.inv()) or (mode and MODE_MASK)
        fun getMode(spec: Int) = spec and MODE_MASK
        fun getSize(spec: Int) = spec and MODE_MASK.inv()
        fun toString(spec: Int) = "MeasureSpec(${getMode(spec)}, ${getSize(spec)})"
    }
    companion object {
        const val VISIBLE = 0
        const val INVISIBLE = 4
        const val GONE = 8
        const val NO_ID = -1
        const val LAYER_TYPE_NONE = 0
        const val LAYER_TYPE_HARDWARE = 2
        const val IMPORTANT_FOR_ACCESSIBILITY_NO = 2
        const val OVER_SCROLL_NEVER = 2
        const val SYSTEM_UI_FLAG_FULLSCREEN = 4
        const val SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2
        const val SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096
        const val SYSTEM_UI_FLAG_LAYOUT_STABLE = 256
        const val SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512
        const val SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024
        const val FOCUS_DOWN = 130
        const val TEXT_ALIGNMENT_CENTER = 4
        const val HAPTIC_FEEDBACK_ENABLED = 1
        private var nextId = 1
        fun generateViewId() = nextId++
        val STATE_CHANGED = Unit
    }

    var id: Int = NO_ID
    var tag: Any? = null
    var parent: ViewGroup? = null
        internal set
    private var params: ViewGroup.LayoutParams? = null
    /** Never null here (Android's is a platform type the app treats as set): wrap_content until given. */
    open var layoutParams: ViewGroup.LayoutParams
        get() = params ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { params = it }
        set(v) { params = v; requestLayout() }
    internal val hasLayoutParams: Boolean get() = params != null

    var visibility: Int = VISIBLE
        set(v) { if (field != v) { field = v; requestLayout() } }
    var alpha: Float = 1f
        set(v) { if (field != v) { field = v; invalidate() } }
    var translationX: Float = 0f
        set(v) { if (field != v) { field = v; invalidate() } }
    var translationY: Float = 0f
        set(v) { if (field != v) { field = v; invalidate() } }
    var translationZ: Float = 0f
    var elevation: Float = 0f
    var scaleX: Float = 1f
        set(v) { field = v; invalidate() }
    var scaleY: Float = 1f
        set(v) { field = v; invalidate() }
    var rotation: Float = 0f
    var pivotX: Float = 0f
    var pivotY: Float = 0f
    var x: Float get() = left + translationX; set(v) { translationX = v - left }
    var y: Float get() = top + translationY; set(v) { translationY = v - top }

    var left = 0; private set
    var top = 0; private set
    var right = 0; private set
    var bottom = 0; private set
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    var measuredWidth = 0; private set
    var measuredHeight = 0; private set
    val measuredState = 0
    var minimumWidth = 0
    var minimumHeight = 0
    var paddingLeft = 0; private set
    var paddingTop = 0; private set
    var paddingRight = 0; private set
    var paddingBottom = 0; private set
    val paddingStart get() = paddingLeft
    val paddingEnd get() = paddingRight
    fun setPadding(l: Int, t: Int, r: Int, b: Int) { paddingLeft = l; paddingTop = t; paddingRight = r; paddingBottom = b; requestLayout() }
    fun setPaddingRelative(l: Int, t: Int, r: Int, b: Int) = setPadding(l, t, r, b)

    var isFocusable = false
    var isFocusableInTouchMode = false
    var isClickable = false
    var isLongClickable = false
    var isEnabled = true
    var isSelected = false
    var isActivated = false
    var isPressed = false
    var keepScreenOn = false
    var isHapticFeedbackEnabled = true
    var isSoundEffectsEnabled = true
    var importantForAccessibility = 0
    var contentDescription: CharSequence? = null
    var layerType = LAYER_TYPE_NONE; private set
    fun setLayerType(type: Int, paint: android.graphics.Paint?) { layerType = type }
    var clipToOutline = false
    var clipChildren = true
    var overScrollMode = 0
    var systemUiVisibility = 0
    var foreground: android.graphics.drawable.Drawable? = null
    var background: android.graphics.drawable.Drawable? = null
    /** The background colour (setBackgroundColor), drawn under the view's content. */
    var backgroundColor: Int = 0
        private set
    open fun setBackgroundColor(color: Int) { backgroundColor = color; invalidate() }
    fun setBackgroundResource(res: Int) {}
    open fun setBackground(d: android.graphics.drawable.Drawable?) { background = d; invalidate() }
    val isShown: Boolean get() = visibility == VISIBLE && (parent?.isShown ?: isAttachedToWindow)
    val isLaidOut: Boolean get() = width > 0 || height > 0
    fun hasFocus(): Boolean = isFocused
    val isInTouchMode: Boolean get() = true
    var defaultFocusHighlightEnabled = false
    fun findFocus(): View? = if (isFocused) this else (this as? ViewGroup)?.childList?.firstNotNullOfOrNull { it.findFocus() }
    open fun onApplyWindowInsets(insets: WindowInsets): WindowInsets = insets
    fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets = onApplyWindowInsets(insets)
    fun requestTransparentRegion(v: View) {}
    var isFocused = false; private set
    val rootView: View get() = parent?.rootView ?: this
    val display: Display? get() = if (isAttachedToWindow) Display.main else null
    val resources get() = context.resources
    val handler: android.os.Handler get() = android.os.Handler(android.os.Looper.getMainLooper())
    val viewTreeObserver = ViewTreeObserver()
    val windowToken: android.os.IBinder? get() = if (isAttachedToWindow) android.os.IBinder() else null
    open val isInEditMode = false

    // ---- attachment ----
    var isAttachedToWindow = false
        private set
    internal open fun dispatchAttached(attached: Boolean) {
        if (attached == isAttachedToWindow) return
        isAttachedToWindow = attached
        if (attached) { onAttachedToWindow(); attachListeners.toList().forEach { it.onViewAttachedToWindow(this) } }
        else { onDetachedFromWindow(); attachListeners.toList().forEach { it.onViewDetachedFromWindow(this) } }
    }
    protected open fun onAttachedToWindow() {}
    protected open fun onDetachedFromWindow() {}
    interface OnAttachStateChangeListener { fun onViewAttachedToWindow(v: View); fun onViewDetachedFromWindow(v: View) }
    private val attachListeners = ArrayList<OnAttachStateChangeListener>()
    fun addOnAttachStateChangeListener(l: OnAttachStateChangeListener) { attachListeners.add(l) }
    fun removeOnAttachStateChangeListener(l: OnAttachStateChangeListener) { attachListeners.remove(l) }

    // ---- measure / layout ----
    /** Set by requestLayout / invalidate: ViewHost lays out and redraws the tree on its next frame. */
    internal var host: com.pokedaisy.ios.ViewHost.Root? = null
        get() = field ?: parent?.host
    open fun requestLayout() { host?.changed() }
    open fun invalidate() { host?.changed() }
    fun invalidate(l: Int, t: Int, r: Int, b: Int) = invalidate()
    fun postInvalidate() = invalidate()
    fun postInvalidateOnAnimation() = invalidate()
    fun forceLayout() = requestLayout()
    val isLayoutRequested: Boolean get() = false

    fun measure(widthMeasureSpec: Int, heightMeasureSpec: Int) { onMeasure(widthMeasureSpec, heightMeasureSpec) }
    protected open fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
    }
    protected val suggestedMinimumWidth: Int get() = minimumWidth
    protected val suggestedMinimumHeight: Int get() = minimumHeight
    protected fun setMeasuredDimension(w: Int, h: Int) { measuredWidth = w; measuredHeight = h }
    fun layout(l: Int, t: Int, r: Int, b: Int) {
        val changed = l != left || t != top || r != right || b != bottom
        val oldW = width; val oldH = height
        val oldL = left; val oldT = top; val oldR = right; val oldB = bottom
        left = l; top = t; right = r; bottom = b
        if (changed && (oldW != width || oldH != height)) onSizeChanged(width, height, oldW, oldH)
        onLayout(changed, l, t, r, b)
        if (changed) layoutListeners.toList().forEach { it.onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB) }
    }
    protected open fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {}
    protected open fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {}
    fun offsetTopAndBottom(d: Int) { top += d; bottom += d; invalidate() }
    fun offsetLeftAndRight(d: Int) { left += d; right += d; invalidate() }
    fun setLeftTopRightBottom(l: Int, t: Int, r: Int, b: Int) = layout(l, t, r, b)

    fun interface OnLayoutChangeListener { fun onLayoutChange(v: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or: Int, ob: Int) }
    private val layoutListeners = ArrayList<OnLayoutChangeListener>()
    fun addOnLayoutChangeListener(l: OnLayoutChangeListener) { layoutListeners.add(l) }
    fun removeOnLayoutChangeListener(l: OnLayoutChangeListener) { layoutListeners.remove(l) }

    /** The view's place in its window (the host's root), in px. */
    fun getLocationInWindow(out: IntArray) {
        var x = left + translationX.toInt(); var y = top + translationY.toInt()
        var p = parent
        while (p != null) { x += p.left + p.translationX.toInt(); y += p.top + p.translationY.toInt(); p = p.parent }
        out[0] = x; out[1] = y
    }
    fun getLocationOnScreen(out: IntArray) = getLocationInWindow(out)
    fun getGlobalVisibleRect(r: android.graphics.Rect): Boolean { val o = IntArray(2); getLocationInWindow(o); r.set(o[0], o[1], o[0] + width, o[1] + height); return isShown }
    fun getHitRect(r: android.graphics.Rect) { r.set(left, top, right, bottom) }
    fun getDrawingRect(r: android.graphics.Rect) { r.set(0, 0, width, height) }

    // ---- drawing ----
    /** Whether ViewHost draws this view's onDraw (any subclass that overrides it says so by drawing). */
    open val drawsItself: Boolean get() = false
    open fun onDraw(canvas: Canvas) {}
    open fun draw(canvas: Canvas) { if (backgroundColor != 0) canvas.drawColor(backgroundColor); onDraw(canvas) }
    open fun setWillNotDraw(willNotDraw: Boolean) {}

    // ---- the main thread ----
    fun post(r: Runnable): Boolean { android.os.Handler.main.post(r); return true }
    fun post(r: () -> Unit): Boolean = post(Runnable(r))
    fun postDelayed(r: Runnable, ms: Long): Boolean { android.os.Handler.main.postDelayed(r, ms); return true }
    fun postDelayed(r: () -> Unit, ms: Long): Boolean = postDelayed(Runnable(r), ms)
    fun postOnAnimation(r: Runnable) = post(r)
    fun removeCallbacks(r: Runnable?): Boolean { if (r != null) android.os.Handler.main.removeCallbacks(r); return true }

    // ---- input ----
    fun interface OnTouchListener { fun onTouch(v: View, e: MotionEvent): Boolean }
    fun interface OnClickListener { fun onClick(v: View) }
    fun interface OnLongClickListener { fun onLongClick(v: View): Boolean }
    fun interface OnKeyListener { fun onKey(v: View, keyCode: Int, e: KeyEvent): Boolean }
    fun interface OnGenericMotionListener { fun onGenericMotion(v: View, e: MotionEvent): Boolean }
    fun interface OnFocusChangeListener { fun onFocusChange(v: View, hasFocus: Boolean) }
    fun interface OnSystemUiVisibilityChangeListener { fun onSystemUiVisibilityChange(visibility: Int) }
    private var touchListener: OnTouchListener? = null
    private var clickListener: OnClickListener? = null
    var onFocusChangeListener: OnFocusChangeListener? = null
    fun setOnTouchListener(l: OnTouchListener?) { touchListener = l }
    fun setOnTouchListener(l: ((View, MotionEvent) -> Boolean)?) { touchListener = l?.let { f -> OnTouchListener { v, e -> f(v, e) } } }
    fun setOnClickListener(l: OnClickListener?) { clickListener = l; isClickable = l != null }
    fun setOnClickListener(l: ((View) -> Unit)?) = setOnClickListener(l?.let { f -> OnClickListener { f(it) } })
    fun setOnLongClickListener(l: OnLongClickListener?) {}
    fun setOnKeyListener(l: OnKeyListener?) {}
    fun setOnGenericMotionListener(l: OnGenericMotionListener?) {}
    fun setOnSystemUiVisibilityChangeListener(l: OnSystemUiVisibilityChangeListener?) {}
    fun setOnApplyWindowInsetsListener(l: Any?) {}
    open fun performClick(): Boolean { clickListener?.onClick(this); return clickListener != null }
    fun performHapticFeedback(type: Int): Boolean { com.pokedaisy.ios.Haptics.tap(); return true }
    fun performHapticFeedback(type: Int, flags: Int) = performHapticFeedback(type)
    fun playSoundEffect(id: Int) {}
    open fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (touchListener?.onTouch(this, e) == true) return true
        return onTouchEvent(e)
    }
    open fun onTouchEvent(e: MotionEvent): Boolean {
        if (clickListener != null && e.actionMasked == MotionEvent.ACTION_UP) performClick()
        return isClickable
    }
    open fun dispatchGenericMotionEvent(e: MotionEvent): Boolean = onGenericMotionEvent(e)
    open fun onGenericMotionEvent(e: MotionEvent): Boolean = false
    open fun dispatchKeyEvent(e: KeyEvent): Boolean = false
    open fun onKeyDown(keyCode: Int, e: KeyEvent): Boolean = false
    open fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    fun requestFocus(): Boolean { isFocused = true; onFocusChangeListener?.onFocusChange(this, true); return true }
    fun requestFocus(direction: Int) = requestFocus()
    fun requestFocusFromTouch() = requestFocus()
    fun clearFocus() { isFocused = false; onFocusChangeListener?.onFocusChange(this, false) }
    fun hasWindowFocus() = isAttachedToWindow
    fun cancelPendingInputEvents() {}
    fun setOnCapturedPointerListener(l: Any?) {}
    fun releasePointerCapture() {}
    fun requestPointerCapture() {}
    fun announceForAccessibility(text: CharSequence?) {}

    fun animate(): ViewPropertyAnimator = ViewPropertyAnimator(this)
    fun bringToFront() { parent?.bringChildToFront(this) }
    fun findViewById(id: Int): View? = if (this.id == id) this else null
    fun <T : View> findViewById(id: Int, cls: Any? = null): T? = @Suppress("UNCHECKED_CAST") (findViewById(id) as T?)
    fun setRenderEffect(e: android.graphics.RenderEffect?) {}
    fun getWindowVisibleDisplayFrame(r: android.graphics.Rect) { r.set(0, 0, rootView.width, rootView.height) }
    val rootWindowInsets: WindowInsets? get() = null
    fun requestApplyInsets() {}
    fun getDefaultSize(size: Int, spec: Int): Int = when (MeasureSpec.getMode(spec)) {
        MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> MeasureSpec.getSize(spec)
        else -> size
    }
    open fun onWindowFocusChanged(hasFocus: Boolean) {}
    open fun onConfigurationChanged(c: android.content.res.Configuration) {}
}

open class ViewGroup(context: Context) : View(context) {
    open class LayoutParams(var width: Int, var height: Int) {
        constructor(src: LayoutParams) : this(src.width, src.height)
        companion object { const val MATCH_PARENT = -1; const val FILL_PARENT = -1; const val WRAP_CONTENT = -2 }
    }
    open class MarginLayoutParams(width: Int, height: Int) : LayoutParams(width, height) {
        companion object { const val MATCH_PARENT = -1; const val FILL_PARENT = -1; const val WRAP_CONTENT = -2 }
        constructor(src: LayoutParams) : this(src.width, src.height) {
            if (src is MarginLayoutParams) { leftMargin = src.leftMargin; topMargin = src.topMargin; rightMargin = src.rightMargin; bottomMargin = src.bottomMargin }
        }
        var leftMargin = 0
        var topMargin = 0
        var rightMargin = 0
        var bottomMargin = 0
        var marginStart: Int get() = leftMargin; set(v) { leftMargin = v }
        var marginEnd: Int get() = rightMargin; set(v) { rightMargin = v }
        fun setMargins(l: Int, t: Int, r: Int, b: Int) { leftMargin = l; topMargin = t; rightMargin = r; bottomMargin = b }
    }

    private val children = ArrayList<View>()
    val childCount: Int get() = children.size
    /** Android's is a platform type the app reads within childCount. */
    fun getChildAt(i: Int): View = children[i]
    fun indexOfChild(v: View): Int = children.indexOf(v)
    internal val childList: List<View> get() = children
    var clipToPadding = true
    var descendantFocusability = 0
    var layoutTransition: Any? = null
    var isMotionEventSplittingEnabled = true

    open fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    open fun generateLayoutParams(p: LayoutParams): LayoutParams = p
    fun addView(child: View) = addView(child, -1)
    fun addView(child: View, index: Int) = addView(child, index, if (child.hasLayoutParams) child.layoutParams else generateDefaultLayoutParams())
    fun addView(child: View, params: LayoutParams) = addView(child, -1, params)
    fun addView(child: View, w: Int, h: Int) = addView(child, -1, generateLayoutParams(LayoutParams(w, h)))
    open fun addView(child: View, index: Int, params: LayoutParams) {
        child.parent?.removeView(child)
        child.layoutParams = generateLayoutParams(params)
        child.parent = this
        if (index < 0 || index > children.size) children.add(child) else children.add(index, child)
        if (isAttachedToWindow) child.dispatchAttached(true)
        requestLayout()
    }
    open fun removeView(child: View) {
        if (children.remove(child)) {
            child.dispatchAttached(false)
            child.parent = null
            requestLayout()
        }
    }
    fun removeViewAt(i: Int) { children.getOrNull(i)?.let { removeView(it) } }
    fun removeViewInLayout(child: View) = removeView(child)
    fun removeAllViews() { children.toList().forEach { removeView(it) } }
    fun removeAllViewsInLayout() = removeAllViews()
    fun bringChildToFront(child: View) { if (children.remove(child)) { children.add(child); requestLayout() } }
    override fun dispatchAttached(attached: Boolean) {
        super.dispatchAttached(attached)
        children.toList().forEach { it.dispatchAttached(attached) }
    }

    protected fun measureChild(child: View, wSpec: Int, hSpec: Int) {
        val lp = child.layoutParams
        child.measure(getChildMeasureSpec(wSpec, paddingLeft + paddingRight, lp.width), getChildMeasureSpec(hSpec, paddingTop + paddingBottom, lp.height))
    }
    protected fun measureChildWithMargins(child: View, wSpec: Int, wUsed: Int, hSpec: Int, hUsed: Int) {
        val lp = child.layoutParams as? MarginLayoutParams ?: MarginLayoutParams(child.layoutParams)
        child.measure(
            getChildMeasureSpec(wSpec, paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin + wUsed, lp.width),
            getChildMeasureSpec(hSpec, paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin + hUsed, lp.height),
        )
    }
    fun measureChildren(wSpec: Int, hSpec: Int) { children.forEach { if (it.visibility != GONE) measureChild(it, wSpec, hSpec) } }

    /** The dispatch Android does: the topmost child under the touch that takes it (front to back). */
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (onInterceptTouchEvent(e)) return super.dispatchTouchEvent(e)
        for (c in children.reversed()) {
            if (c.visibility != VISIBLE) continue
            val x = e.x - c.left - c.translationX
            val y = e.y - c.top - c.translationY
            if (x < 0 || y < 0 || x >= c.width || y >= c.height) continue
            val ce = e.offset(-(c.left + c.translationX), -(c.top + c.translationY))
            if (c.dispatchTouchEvent(ce)) return true
        }
        return super.dispatchTouchEvent(e)
    }
    open fun onInterceptTouchEvent(e: MotionEvent): Boolean = false
    open fun requestDisallowInterceptTouchEvent(disallow: Boolean) {}
    override fun dispatchKeyEvent(e: KeyEvent): Boolean = children.any { it.dispatchKeyEvent(e) }

    companion object {
        fun getChildMeasureSpec(spec: Int, padding: Int, childDimension: Int): Int {
            val specMode = MeasureSpec.getMode(spec)
            val size = maxOf(0, MeasureSpec.getSize(spec) - padding)
            return when {
                childDimension >= 0 -> MeasureSpec.makeMeasureSpec(childDimension, MeasureSpec.EXACTLY)
                childDimension == LayoutParams.MATCH_PARENT -> when (specMode) {
                    MeasureSpec.EXACTLY -> MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
                    MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST)
                    else -> MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                }
                else -> when (specMode) {
                    MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST)
                    else -> MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                }
            }
        }
    }
}

class ViewPropertyAnimator internal constructor(private val v: View) {
    private var a: Float? = null
    private var tx: Float? = null
    private var ty: Float? = null
    private var end: Runnable? = null
    private var duration = 300L
    fun alpha(x: Float) = apply { a = x }
    fun translationX(x: Float) = apply { tx = x }
    fun translationY(x: Float) = apply { ty = x }
    fun scaleX(x: Float) = this
    fun scaleY(x: Float) = this
    fun setDuration(ms: Long) = apply { duration = ms }
    fun setStartDelay(ms: Long) = this
    fun setInterpolator(i: Any?) = this
    fun withEndAction(r: Runnable?) = apply { end = r }
    fun setUpdateListener(l: ((android.animation.ValueAnimator) -> Unit)?) = this

    fun withStartAction(r: Runnable) = apply { r.run() }
    fun setListener(l: Any?) = this
    /** Animated by ViewHost over [duration] (the values step there each frame); here it ends right away. */
    fun start() {
        val r = Runnable {
            a?.let { v.alpha = it }; tx?.let { v.translationX = it }; ty?.let { v.translationY = it }
            end?.run()
        }
        com.pokedaisy.ios.ViewHost.animate(v, a, tx, ty, duration, r)
    }
    fun cancel() {}
}

class ViewTreeObserver {
    fun interface OnGlobalLayoutListener { fun onGlobalLayout() }
    fun interface OnPreDrawListener { fun onPreDraw(): Boolean }
    fun addOnGlobalLayoutListener(l: OnGlobalLayoutListener) {}
    fun removeOnGlobalLayoutListener(l: OnGlobalLayoutListener) {}
    fun addOnPreDrawListener(l: OnPreDrawListener) {}
    fun removeOnPreDrawListener(l: OnPreDrawListener) {}
    val isAlive = true
}

class WindowInsets
