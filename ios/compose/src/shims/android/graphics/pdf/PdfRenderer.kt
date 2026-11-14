@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.graphics.pdf

/** The manual's PDFs aren't rendered on iOS yet: no pages. */
class PdfRenderer(fd: android.os.ParcelFileDescriptor) : java.io.Closeable {
    val pageCount = 0
    fun openPage(i: Int): Page = throw java.io.IOException("PDF pages aren't rendered on iOS yet")
    override fun close() {}
    class Page : java.io.Closeable {
        val width = 0; val height = 0
        fun render(b: android.graphics.Bitmap, r: android.graphics.Rect?, m: android.graphics.Matrix?, mode: Int) {}
        override fun close() {}
        companion object { const val RENDER_MODE_FOR_DISPLAY = 1 }
    }
}
