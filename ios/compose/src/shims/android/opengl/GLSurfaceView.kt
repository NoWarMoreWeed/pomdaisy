// android.opengl.GLSurfaceView on iOS: a CAEAGLLayer in a UIView (ViewHost places it), drawn by a GL thread of its own
// as Android's is - the renderer's onSurfaceCreated / onSurfaceChanged / onDrawFrame, continuously or on requestRender,
// queueEvent's jobs between frames. Framebuffer 0 means the layer's (GLES20.glBindFramebuffer).
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package android.opengl

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlinx.cinterop.*
import platform.EAGL.*
import platform.QuartzCore.CAEAGLLayer
import platform.UIKit.UIView
import platform.gles3.*
import platform.darwin.NSObject
import androidx.compose.foundation.layout.fillMaxSize

open class GLSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback2 {
    interface Renderer {
        fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
        fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
        fun onDrawFrame(gl: GL10?)
    }
    companion object { const val RENDERMODE_WHEN_DIRTY = 0; const val RENDERMODE_CONTINUOUSLY = 1; const val DEBUG_CHECK_GL_ERROR = 1 }

    private var renderer: Renderer? = null
    private var glVersion = 3
    var renderMode = RENDERMODE_CONTINUOUSLY
        set(v) { field = v; thread?.wake() }
    var preserveEGLContextOnPause = true
    var debugFlags = 0
    fun setEGLContextClientVersion(v: Int) { glVersion = v }
    fun setEGLConfigChooser(r: Int, g: Int, b: Int, a: Int, depth: Int, stencil: Int) {}
    fun setEGLConfigChooser(b: Boolean) {}
    fun setPreserveEGLContextOnPause(b: Boolean) { preserveEGLContextOnPause = b }
    fun setRenderer(r: Renderer) { renderer = r; holder.addCallback(this); maybeStart() }
    fun requestRender() { thread?.requestRender() }
    fun queueEvent(r: Runnable) { thread?.queue(r) ?: pendingEvents.add(r) }
    private val pendingEvents = ArrayList<Runnable>()
    open fun onPause() { thread?.paused = true }
    open fun onResume() { thread?.paused = false; thread?.wake() }

    // SurfaceHolder.Callback2: Android's GLSurfaceView implements these (subclasses call super)
    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}
    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {}
    override fun surfaceRedrawNeededAsync(holder: SurfaceHolder, drawingFinished: Runnable) { drawingFinished.run() }

    /** The UIKit view ViewHost shows (its CAEAGLLayer is the GL thread's drawable). */
    val uiView: UIView by lazy { GlLayerView() }
    private val layer: CAEAGLLayer get() = (uiView as GlLayerView).glLayer
    private var thread: GlThread? = null

    override fun onAttachedToWindow() { super.onAttachedToWindow(); maybeStart() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); thread?.stop(); thread = null; holder.fireDestroyed() }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        (uiView as GlLayerView).resize(w, h)
        thread?.resized(w, h)
        holder.fireChanged(w, h)
    }

    private fun maybeStart() {
        val r = renderer ?: return
        if (thread != null || !isAttachedToWindow) return
        holder.fireCreated()
        thread = GlThread(r, glVersion, layer).also { t ->
            pendingEvents.forEach { t.queue(it) }
            pendingEvents.clear()
            t.resized(width, height)
            t.start()
        }
    }

    private inner class GlThread(private val r: Renderer, private val version: Int, private val layer: CAEAGLLayer) {
        private val lock = kotlin.PdMutex()
        private val jobs = ArrayList<Runnable>()
        @kotlin.concurrent.Volatile private var running = true
        @kotlin.concurrent.Volatile var paused = false
        @kotlin.concurrent.Volatile private var dirty = true
        @kotlin.concurrent.Volatile private var w = 0
        @kotlin.concurrent.Volatile private var h = 0
        @kotlin.concurrent.Volatile private var sizeChanged = true
        private val t = java.lang.Thread({ loop() }, "GLThread")

        fun start() { t.start() }
        fun stop() { running = false }
        fun wake() { dirty = true }
        fun requestRender() { dirty = true }
        fun queue(j: Runnable) { lock.lock(); try { jobs.add(j) } finally { lock.unlock() }; dirty = true }
        fun resized(nw: Int, nh: Int) { if (nw != w || nh != h) { w = nw; h = nh; sizeChanged = true; dirty = true } }

        private fun loop() {
            com.pokedaisy.ios.setThreadQos(0x21u)
            val ctx = (if (version >= 3) EAGLContext(kEAGLRenderingAPIOpenGLES3) else null) ?: EAGLContext(kEAGLRenderingAPIOpenGLES2) ?: return
            EAGLContext.setCurrentContext(ctx)
            val ids = IntArray(2)
            memScoped {
                val a = allocArray<UIntVar>(1)
                glGenFramebuffers(1, a); ids[0] = a[0].toInt()
                glGenRenderbuffers(1, a); ids[1] = a[0].toInt()
            }
            val fbo = ids[0].toUInt(); val rbo = ids[1].toUInt()
            glBindFramebuffer(GL_FRAMEBUFFER.toUInt(), fbo)
            glBindRenderbuffer(GL_RENDERBUFFER.toUInt(), rbo)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER.toUInt(), GL_COLOR_ATTACHMENT0.toUInt(), GL_RENDERBUFFER.toUInt(), rbo)
            GlViews.screenFbo = ids[0]
            var created = false
            var next = platform.Foundation.NSProcessInfo.processInfo.systemUptime
            while (running) {
                val todo = run { lock.lock(); try { jobs.toList().also { jobs.clear() } } finally { lock.unlock() } }
                todo.forEach { runCatching { it.run() }.onFailure { e -> e.printStackTrace() } }
                if (sizeChanged && w > 0 && h > 0) {
                    sizeChanged = false
                    glBindRenderbuffer(GL_RENDERBUFFER.toUInt(), rbo)
                    ctx.renderbufferStorage(GL_RENDERBUFFER.toULong(), layer)
                    if (!created) { created = true; r.onSurfaceCreated(null, null) }
                    r.onSurfaceChanged(null, w, h)
                    dirty = true
                }
                val draw = created && !paused && (renderMode == RENDERMODE_CONTINUOUSLY || dirty)
                if (draw) {
                    dirty = false
                    glBindFramebuffer(GL_FRAMEBUFFER.toUInt(), fbo)
                    runCatching { r.onDrawFrame(null) }.onFailure { e -> e.printStackTrace() }
                    glBindRenderbuffer(GL_RENDERBUFFER.toUInt(), rbo)
                    ctx.presentRenderbuffer(GL_RENDERBUFFER.toULong())
                }
                // ~60 Hz (the renderer itself waits for fresh frames as on Android)
                next += 1.0 / 60.0
                val now = platform.Foundation.NSProcessInfo.processInfo.systemUptime
                if (next > now) platform.posix.usleep(((next - now) * 1_000_000).toUInt()) else next = now
            }
            EAGLContext.setCurrentContext(null)
        }
    }
}

/** The layer GL draws into, sized with the view (in pixels: the screen's scale). */
private class GlLayerView : UIView(platform.CoreGraphics.CGRectMake(0.0, 0.0, 1.0, 1.0)) {
    val glLayer = CAEAGLLayer().also {
        it.opaque = true
        it.contentsScale = platform.UIKit.UIScreen.mainScreen.scale
    }
    init {
        layer.addSublayer(glLayer)
        userInteractionEnabled = false
    }
    fun resize(pxW: Int, pxH: Int) = com.pokedaisy.ios.ActivityHost.onMain {
        val s = platform.UIKit.UIScreen.mainScreen.scale
        platform.QuartzCore.CATransaction.begin()
        platform.QuartzCore.CATransaction.setDisableActions(true)
        glLayer.frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, pxW / s, pxH / s)
        platform.QuartzCore.CATransaction.commit()
    }
}

/** The GL thread's screen framebuffer (Android's 0). */
@kotlin.native.concurrent.ThreadLocal
private var screenFboOfThread: Int = 0

object GlViews {
    var screenFbo: Int
        get() = screenFboOfThread
        set(v) { screenFboOfThread = v }

    @androidx.compose.runtime.Composable
    fun Show(v: GLSurfaceView) {
        androidx.compose.ui.viewinterop.UIKitView(
            factory = { v.uiView },
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            properties = androidx.compose.ui.viewinterop.UIKitInteropProperties(interactionMode = null, isNativeAccessibilityEnabled = false),
        )
    }
}
