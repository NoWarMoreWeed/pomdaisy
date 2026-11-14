@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.opengl

/** EGL (DS RESOLUTION's GPU path makes its own contexts): absent on iOS - every call fails, so that path stays off. */
object EGL14 {
    val EGL_NO_DISPLAY = EGLDisplay(); val EGL_NO_CONTEXT = EGLContext(); val EGL_NO_SURFACE = EGLSurface()
    const val EGL_DEFAULT_DISPLAY = 0; const val EGL_SUCCESS = 0x3000; const val EGL_RED_SIZE = 0x3024; const val EGL_GREEN_SIZE = 0x3023
    const val EGL_BLUE_SIZE = 0x3022; const val EGL_ALPHA_SIZE = 0x3021; const val EGL_RENDERABLE_TYPE = 0x3040; const val EGL_NONE = 0x3038
    const val EGL_CONTEXT_CLIENT_VERSION = 0x3098; const val EGL_OPENGL_ES2_BIT = 4; const val EGL_SURFACE_TYPE = 0x3033; const val EGL_PBUFFER_BIT = 1
    const val EGL_WIDTH = 0x3057; const val EGL_HEIGHT = 0x3056; const val EGL_WINDOW_BIT = 4; const val EGL_DEPTH_SIZE = 0x3025; const val EGL_STENCIL_SIZE = 0x3026
    const val EGL_EXTENSIONS = 0x3055; const val EGL_VENDOR = 0x3053; const val EGL_VERSION = 0x3054
    fun eglGetDisplay(id: Int) = EGL_NO_DISPLAY
    fun eglInitialize(d: EGLDisplay, major: IntArray?, mo: Int, minor: IntArray?, mi: Int) = false
    fun eglChooseConfig(d: EGLDisplay, attribs: IntArray, ao: Int, configs: Array<EGLConfig?>, co: Int, size: Int, num: IntArray, no: Int) = false
    fun eglCreateContext(d: EGLDisplay, c: EGLConfig?, share: EGLContext, attribs: IntArray, o: Int) = EGL_NO_CONTEXT
    fun eglCreatePbufferSurface(d: EGLDisplay, c: EGLConfig?, attribs: IntArray, o: Int) = EGL_NO_SURFACE
    fun eglCreateWindowSurface(d: EGLDisplay, c: EGLConfig?, win: Any?, attribs: IntArray, o: Int) = EGL_NO_SURFACE
    fun eglMakeCurrent(d: EGLDisplay, a: EGLSurface, b: EGLSurface, c: EGLContext) = false
    fun eglDestroyContext(d: EGLDisplay, c: EGLContext) = true
    fun eglDestroySurface(d: EGLDisplay, s: EGLSurface) = true
    fun eglTerminate(d: EGLDisplay) = true
    fun eglReleaseThread() = true
    fun eglSwapBuffers(d: EGLDisplay, s: EGLSurface) = false
    fun eglGetError() = 0x3001
    fun eglGetCurrentContext() = EGL_NO_CONTEXT
    fun eglGetCurrentDisplay() = EGL_NO_DISPLAY
    fun eglQueryString(d: EGLDisplay, name: Int): String? = null
}
object EGLExt { const val EGL_OPENGL_ES3_BIT_KHR = 0x40; fun eglPresentationTimeANDROID(d: EGLDisplay, s: EGLSurface, t: Long) = false }
class EGLDisplay
class EGLContext
class EGLSurface
class EGLConfig
