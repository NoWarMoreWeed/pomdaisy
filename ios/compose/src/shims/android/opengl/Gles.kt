// android.opengl's GLES20 / GLES30 / GLES11Ext / GLUtils over iOS's OpenGL ES 3 (platform.gles3): the calls the app's
// renderers make, with Java's arrays / buffers turned into pointers. Framebuffer 0 is the GL view's own (GlViews):
// Android's default framebuffer is the window, iOS draws into one we made.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER", "FunctionName")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package android.opengl

import kotlinx.cinterop.*
import platform.gles3.*

object GLES20 {
    const val GL_TEXTURE_2D = 0x0DE1
    const val GL_TEXTURE0 = 0x84C0
    const val GL_TEXTURE1 = 0x84C1
    const val GL_TEXTURE2 = 0x84C2
    const val GL_TEXTURE3 = 0x84C3
    const val GL_TEXTURE_MIN_FILTER = 0x2801
    const val GL_TEXTURE_MAG_FILTER = 0x2800
    const val GL_TEXTURE_WRAP_S = 0x2802
    const val GL_TEXTURE_WRAP_T = 0x2803
    const val GL_NEAREST = 0x2600
    const val GL_LINEAR = 0x2601
    const val GL_CLAMP_TO_EDGE = 0x812F
    const val GL_REPEAT = 0x2901
    const val GL_RGBA = 0x1908
    const val GL_RGB = 0x1907
    const val GL_ALPHA = 0x1906
    const val GL_LUMINANCE = 0x1909
    const val GL_UNSIGNED_BYTE = 0x1401
    const val GL_UNSIGNED_SHORT = 0x1403
    const val GL_UNSIGNED_SHORT_5_6_5 = 0x8363
    const val GL_FLOAT = 0x1406
    const val GL_TRIANGLES = 0x0004
    const val GL_TRIANGLE_STRIP = 0x0005
    const val GL_TRIANGLE_FAN = 0x0006
    const val GL_FRAMEBUFFER = 0x8D40
    const val GL_RENDERBUFFER = 0x8D41
    const val GL_COLOR_ATTACHMENT0 = 0x8CE0
    const val GL_FRAMEBUFFER_COMPLETE = 0x8CD5
    const val GL_FRAMEBUFFER_BINDING = 0x8CA6
    const val GL_VERTEX_SHADER = 0x8B31
    const val GL_FRAGMENT_SHADER = 0x8B30
    const val GL_COMPILE_STATUS = 0x8B81
    const val GL_LINK_STATUS = 0x8B82
    const val GL_INFO_LOG_LENGTH = 0x8B84
    const val GL_COLOR_BUFFER_BIT = 0x4000
    const val GL_DEPTH_BUFFER_BIT = 0x0100
    const val GL_BLEND = 0x0BE2
    const val GL_DEPTH_TEST = 0x0B71
    const val GL_SCISSOR_TEST = 0x0C11
    const val GL_CULL_FACE = 0x0B44
    const val GL_DITHER = 0x0BD0
    const val GL_ONE = 1
    const val GL_ZERO = 0
    const val GL_SRC_ALPHA = 0x0302
    const val GL_ONE_MINUS_SRC_ALPHA = 0x0303
    const val GL_MAX_TEXTURE_SIZE = 0x0D33
    const val GL_UNPACK_ALIGNMENT = 0x0CF5
    const val GL_PACK_ALIGNMENT = 0x0D05
    const val GL_NO_ERROR = 0
    const val GL_EXTENSIONS = 0x1F03
    const val GL_VERSION = 0x1F02
    const val GL_RENDERER = 0x1F01
    const val GL_VENDOR = 0x1F00
    const val GL_SHADING_LANGUAGE_VERSION = 0x8B8C
    const val GL_ARRAY_BUFFER = 0x8892
    const val GL_STATIC_DRAW = 0x88E4
    const val GL_TRUE = 1
    const val GL_FALSE = 0

    fun glActiveTexture(t: Int) = platform.gles3.glActiveTexture(t.toUInt())
    fun glBindTexture(target: Int, tex: Int) = platform.gles3.glBindTexture(target.toUInt(), tex.toUInt())
    fun glTexParameteri(target: Int, p: Int, v: Int) = platform.gles3.glTexParameteri(target.toUInt(), p.toUInt(), v)
    fun glTexParameterf(target: Int, p: Int, v: Float) = platform.gles3.glTexParameterf(target.toUInt(), p.toUInt(), v)
    fun glPixelStorei(p: Int, v: Int) = platform.gles3.glPixelStorei(p.toUInt(), v)
    fun glGenTextures(n: Int, out: IntArray, off: Int) = memScoped {
        val ids = allocArray<UIntVar>(n); platform.gles3.glGenTextures(n, ids); for (i in 0 until n) out[off + i] = ids[i].toInt()
    }
    fun glDeleteTextures(n: Int, ids: IntArray, off: Int) = memScoped {
        val a = allocArray<UIntVar>(n); for (i in 0 until n) a[i] = ids[off + i].toUInt(); platform.gles3.glDeleteTextures(n, a)
    }
    fun glGenFramebuffers(n: Int, out: IntArray, off: Int) = memScoped {
        val ids = allocArray<UIntVar>(n); platform.gles3.glGenFramebuffers(n, ids); for (i in 0 until n) out[off + i] = ids[i].toInt()
    }
    fun glDeleteFramebuffers(n: Int, ids: IntArray, off: Int) = memScoped {
        val a = allocArray<UIntVar>(n); for (i in 0 until n) a[i] = ids[off + i].toUInt(); platform.gles3.glDeleteFramebuffers(n, a)
    }
    fun glGenBuffers(n: Int, out: IntArray, off: Int) = memScoped {
        val ids = allocArray<UIntVar>(n); platform.gles3.glGenBuffers(n, ids); for (i in 0 until n) out[off + i] = ids[i].toInt()
    }
    /** Framebuffer 0 is the GL view's (GlViews.screenFbo). */
    fun glBindFramebuffer(target: Int, fb: Int) = platform.gles3.glBindFramebuffer(target.toUInt(), if (fb == 0) GlViews.screenFbo.toUInt() else fb.toUInt())
    fun glFramebufferTexture2D(target: Int, att: Int, texTarget: Int, tex: Int, level: Int) =
        platform.gles3.glFramebufferTexture2D(target.toUInt(), att.toUInt(), texTarget.toUInt(), tex.toUInt(), level)
    fun glCheckFramebufferStatus(target: Int): Int = platform.gles3.glCheckFramebufferStatus(target.toUInt()).toInt()
    fun glTexImage2D(target: Int, level: Int, internal: Int, w: Int, h: Int, border: Int, format: Int, type: Int, pixels: java.nio.Buffer?) =
        withPointer(pixels) { p -> platform.gles3.glTexImage2D(target.toUInt(), level, internal, w, h, border, format.toUInt(), type.toUInt(), p) }
    fun glTexSubImage2D(target: Int, level: Int, x: Int, y: Int, w: Int, h: Int, format: Int, type: Int, pixels: java.nio.Buffer?) =
        withPointer(pixels) { p -> platform.gles3.glTexSubImage2D(target.toUInt(), level, x, y, w, h, format.toUInt(), type.toUInt(), p) }
    fun glViewport(x: Int, y: Int, w: Int, h: Int) = platform.gles3.glViewport(x, y, w, h)
    fun glScissor(x: Int, y: Int, w: Int, h: Int) = platform.gles3.glScissor(x, y, w, h)
    fun glClearColor(r: Float, g: Float, b: Float, a: Float) = platform.gles3.glClearColor(r, g, b, a)
    fun glClear(mask: Int) = platform.gles3.glClear(mask.toUInt())
    fun glEnable(cap: Int) = platform.gles3.glEnable(cap.toUInt())
    fun glDisable(cap: Int) = platform.gles3.glDisable(cap.toUInt())
    fun glBlendFunc(s: Int, d: Int) = platform.gles3.glBlendFunc(s.toUInt(), d.toUInt())
    fun glBlendFuncSeparate(a: Int, b: Int, c: Int, d: Int) = platform.gles3.glBlendFuncSeparate(a.toUInt(), b.toUInt(), c.toUInt(), d.toUInt())
    fun glCreateShader(type: Int): Int = platform.gles3.glCreateShader(type.toUInt()).toInt()
    fun glShaderSource(shader: Int, src: String) = memScoped {
        val strs = allocArrayOf(src.cstr.ptr)
        platform.gles3.glShaderSource(shader.toUInt(), 1, strs, null)
    }
    fun glCompileShader(shader: Int) = platform.gles3.glCompileShader(shader.toUInt())
    fun glDeleteShader(shader: Int) = platform.gles3.glDeleteShader(shader.toUInt())
    fun glGetShaderiv(shader: Int, p: Int, out: IntArray, off: Int) = memScoped {
        val v = alloc<IntVar>(); platform.gles3.glGetShaderiv(shader.toUInt(), p.toUInt(), v.ptr); out[off] = v.value
    }
    fun glGetShaderInfoLog(shader: Int): String = memScoped {
        val buf = allocArray<ByteVar>(4096); val len = alloc<IntVar>()
        platform.gles3.glGetShaderInfoLog(shader.toUInt(), 4096, len.ptr, buf); buf.toKString()
    }
    fun glCreateProgram(): Int = platform.gles3.glCreateProgram().toInt()
    fun glAttachShader(p: Int, s: Int) = platform.gles3.glAttachShader(p.toUInt(), s.toUInt())
    fun glBindAttribLocation(p: Int, i: Int, name: String) = platform.gles3.glBindAttribLocation(p.toUInt(), i.toUInt(), name)
    fun glLinkProgram(p: Int) = platform.gles3.glLinkProgram(p.toUInt())
    fun glGetProgramiv(p: Int, pname: Int, out: IntArray, off: Int) = memScoped {
        val v = alloc<IntVar>(); platform.gles3.glGetProgramiv(p.toUInt(), pname.toUInt(), v.ptr); out[off] = v.value
    }
    fun glGetProgramInfoLog(p: Int): String = memScoped {
        val buf = allocArray<ByteVar>(4096); val len = alloc<IntVar>()
        platform.gles3.glGetProgramInfoLog(p.toUInt(), 4096, len.ptr, buf); buf.toKString()
    }
    fun glUseProgram(p: Int) = platform.gles3.glUseProgram(p.toUInt())
    fun glDeleteProgram(p: Int) = platform.gles3.glDeleteProgram(p.toUInt())
    fun glGetUniformLocation(p: Int, name: String): Int = platform.gles3.glGetUniformLocation(p.toUInt(), name)
    fun glGetAttribLocation(p: Int, name: String): Int = platform.gles3.glGetAttribLocation(p.toUInt(), name)
    fun glUniform1i(l: Int, v: Int) = platform.gles3.glUniform1i(l, v)
    fun glUniform1f(l: Int, v: Float) = platform.gles3.glUniform1f(l, v)
    fun glUniform2f(l: Int, a: Float, b: Float) = platform.gles3.glUniform2f(l, a, b)
    fun glUniform3f(l: Int, a: Float, b: Float, c: Float) = platform.gles3.glUniform3f(l, a, b, c)
    fun glUniform4f(l: Int, a: Float, b: Float, c: Float, d: Float) = platform.gles3.glUniform4f(l, a, b, c, d)
    fun glUniformMatrix4fv(l: Int, count: Int, transpose: Boolean, v: FloatArray, off: Int) =
        v.usePinned { platform.gles3.glUniformMatrix4fv(l, count, if (transpose) 1u else 0u, it.addressOf(off)) }
    fun glUniformMatrix3fv(l: Int, count: Int, transpose: Boolean, v: FloatArray, off: Int) =
        v.usePinned { platform.gles3.glUniformMatrix3fv(l, count, if (transpose) 1u else 0u, it.addressOf(off)) }
    fun glEnableVertexAttribArray(i: Int) = platform.gles3.glEnableVertexAttribArray(i.toUInt())
    fun glDisableVertexAttribArray(i: Int) = platform.gles3.glDisableVertexAttribArray(i.toUInt())
    fun glVertexAttrib4f(i: Int, a: Float, b: Float, c: Float, d: Float) = platform.gles3.glVertexAttrib4f(i.toUInt(), a, b, c, d)
    /** A client-side array (the app's direct FloatBuffers live in native memory, so the pointer stays good). */
    fun glVertexAttribPointer(i: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: java.nio.Buffer) =
        platform.gles3.glVertexAttribPointer(i.toUInt(), size, type.toUInt(), if (normalized) 1u else 0u, stride, ptr.nativePointer())
    fun glVertexAttribPointer(i: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) =
        platform.gles3.glVertexAttribPointer(i.toUInt(), size, type.toUInt(), if (normalized) 1u else 0u, stride, offset.toLong().toCPointer<ByteVar>())
    fun glDrawArrays(mode: Int, first: Int, count: Int) = platform.gles3.glDrawArrays(mode.toUInt(), first, count)
    fun glFinish() = platform.gles3.glFinish()
    fun glFlush() = platform.gles3.glFlush()
    fun glGetError(): Int = platform.gles3.glGetError().toInt()
    fun glGetIntegerv(p: Int, out: IntArray, off: Int) = memScoped {
        val v = allocArray<IntVar>(4); platform.gles3.glGetIntegerv(p.toUInt(), v); out[off] = v[0]
    }
    fun glGetString(name: Int): String? = platform.gles3.glGetString(name.toUInt())?.reinterpret<ByteVar>()?.toKString()
    fun glReadPixels(x: Int, y: Int, w: Int, h: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
        withPointer(pixels) { p -> platform.gles3.glReadPixels(x, y, w, h, format.toUInt(), type.toUInt(), p) }
    fun glColorMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean) =
        platform.gles3.glColorMask(if (r) 1u else 0u, if (g) 1u else 0u, if (b) 1u else 0u, if (a) 1u else 0u)

    internal inline fun <R> withPointer(b: java.nio.Buffer?, block: (COpaquePointer?) -> R): R = b.nativePointerOrCopy(block)
}

/** GLES30: the same calls (it extends GLES20 in Java) and its own constants. */
object GLES30 {
    const val GL_TEXTURE_2D = GLES20.GL_TEXTURE_2D
    const val GL_RGBA = GLES20.GL_RGBA
    const val GL_RGBA8 = 0x8058
    const val GL_RGBA16F = 0x881A
    const val GL_HALF_FLOAT = 0x140B
    const val GL_FLOAT = GLES20.GL_FLOAT
    const val GL_UNSIGNED_BYTE = GLES20.GL_UNSIGNED_BYTE
    const val GL_READ_FRAMEBUFFER = 0x8CA8
    const val GL_DRAW_FRAMEBUFFER = 0x8CA9
    fun glTexImage2D(target: Int, level: Int, internal: Int, w: Int, h: Int, border: Int, format: Int, type: Int, pixels: java.nio.Buffer?) =
        GLES20.glTexImage2D(target, level, internal, w, h, border, format, type, pixels)
    fun glBlitFramebuffer(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int, mask: Int, filter: Int) =
        platform.gles3.glBlitFramebuffer(a, b, c, d, e, f, g, h, mask.toUInt(), filter.toUInt())
}

object GLES11Ext { const val GL_TEXTURE_EXTERNAL_OES = 0x8D65 }

object GLUtils {
    /** A Bitmap into the bound texture (its pixels as RGBA bytes). */
    fun texImage2D(target: Int, level: Int, b: android.graphics.Bitmap, border: Int) {
        val px = IntArray(b.width * b.height)
        b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        val bytes = ByteArray(px.size * 4)
        for (i in px.indices) {
            val c = px[i]
            bytes[i * 4] = (c shr 16).toByte(); bytes[i * 4 + 1] = (c shr 8).toByte(); bytes[i * 4 + 2] = c.toByte(); bytes[i * 4 + 3] = (c ushr 24).toByte()
        }
        GLES20.glTexImage2D(target, level, GLES20.GL_RGBA, b.width, b.height, border, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, java.nio.ByteBuffer.wrap(bytes))
    }
    fun texImage2D(target: Int, level: Int, internal: Int, b: android.graphics.Bitmap, border: Int) = texImage2D(target, level, b, border)
    fun texSubImage2D(target: Int, level: Int, x: Int, y: Int, b: android.graphics.Bitmap) {
        val px = IntArray(b.width * b.height)
        b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        val bytes = ByteArray(px.size * 4)
        for (i in px.indices) { val c = px[i]; bytes[i * 4] = (c shr 16).toByte(); bytes[i * 4 + 1] = (c shr 8).toByte(); bytes[i * 4 + 2] = c.toByte(); bytes[i * 4 + 3] = (c ushr 24).toByte() }
        GLES20.glTexSubImage2D(target, level, x, y, b.width, b.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, java.nio.ByteBuffer.wrap(bytes))
    }
    fun getEGLErrorString(e: Int) = "EGL error $e"
}

/** A buffer's memory for GL: a direct buffer's own (from its position), a heap one's pinned for the call. */
internal fun java.nio.Buffer.nativePointer(): COpaquePointer? = when (this) {
    is java.nio.ByteBuffer -> nativeAddress?.let { it + position() }
    is java.nio.FloatBuffer -> address?.let { (it + position())?.reinterpret() }
    else -> null
}

internal inline fun <R> java.nio.Buffer?.nativePointerOrCopy(block: (COpaquePointer?) -> R): R {
    if (this == null) return block(null)
    nativePointer()?.let { return block(it) }
    if (this is java.nio.ByteBuffer) {
        val n = remaining()
        val a = ByteArray(n)
        duplicate().get(a)
        return if (n == 0) block(null) else a.usePinned { block(it.addressOf(0)) }
    }
    return block(null)
}
