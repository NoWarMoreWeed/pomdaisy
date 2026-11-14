@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.view

import android.content.Context

class Surface(st: android.graphics.SurfaceTexture? = null) {
    fun release() {}
    val isValid = true
    fun setFrameRate(rate: Float, compat: Int) {}
    fun setFrameRate(rate: Float, compat: Int, strategy: Int) {}
    companion object {
        const val FRAME_RATE_COMPATIBILITY_DEFAULT = 0
        const val FRAME_RATE_COMPATIBILITY_FIXED_SOURCE = 1
        const val ROTATION_0 = 0; const val ROTATION_90 = 1; const val ROTATION_180 = 2; const val ROTATION_270 = 3
    }
}

interface SurfaceHolder {
    interface Callback {
        fun surfaceCreated(holder: SurfaceHolder)
        fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int)
        fun surfaceDestroyed(holder: SurfaceHolder)
    }
    interface Callback2 : Callback {
        fun surfaceRedrawNeeded(holder: SurfaceHolder)
        fun surfaceRedrawNeededAsync(holder: SurfaceHolder, drawingFinished: Runnable) { surfaceRedrawNeeded(holder); drawingFinished.run() }
    }
    val surface: Surface
    fun addCallback(c: Callback)
    fun removeCallback(c: Callback)
    fun setFormat(f: Int)
    fun setFixedSize(w: Int, h: Int)
}

/** SurfaceView: its holder's callbacks fired as it's attached, resized and detached. */
open class SurfaceView(context: Context) : View(context) {
    inner class Holder : SurfaceHolder {
        private val callbacks = ArrayList<SurfaceHolder.Callback>()
        override val surface = Surface()
        override fun addCallback(c: SurfaceHolder.Callback) { if (c !in callbacks) callbacks.add(c) }
        override fun removeCallback(c: SurfaceHolder.Callback) { callbacks.remove(c) }
        override fun setFormat(f: Int) {}
        override fun setFixedSize(w: Int, h: Int) {}
        internal fun fireCreated() = callbacks.toList().forEach { it.surfaceCreated(this) }
        internal fun fireChanged(w: Int, h: Int) = callbacks.toList().forEach { it.surfaceChanged(this, 1, w, h) }
        internal fun fireDestroyed() = callbacks.toList().forEach { it.surfaceDestroyed(this) }
    }
    val holder = Holder()
    fun setZOrderOnTop(b: Boolean) {}
    fun setZOrderMediaOverlay(b: Boolean) {}
    fun setSecure(b: Boolean) {}
}

open class TextureView(context: Context) : View(context) {
    interface SurfaceTextureListener {
        fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int)
        fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int)
        fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean
        fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
    }
    var surfaceTextureListener: SurfaceTextureListener? = null
    val isAvailable = false
    var isOpaque = true
}
