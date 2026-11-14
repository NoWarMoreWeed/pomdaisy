@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.hardware.display

/** One display: the iPhone's (no second screen for the app here). */
class DisplayManager {
    fun getDisplays(): Array<android.view.Display> = arrayOf(android.view.Display.main)
    val displays: Array<android.view.Display> get() = getDisplays()
    fun getDisplays(category: String?): Array<android.view.Display> = if (category == null) getDisplays() else emptyArray()
    fun getDisplay(id: Int): android.view.Display? = if (id == 0) android.view.Display.main else null
    fun registerDisplayListener(l: DisplayListener, h: android.os.Handler?) {}
    fun unregisterDisplayListener(l: DisplayListener) {}
    interface DisplayListener { fun onDisplayAdded(displayId: Int); fun onDisplayRemoved(displayId: Int); fun onDisplayChanged(displayId: Int) }
    companion object { const val DISPLAY_CATEGORY_PRESENTATION = "android.hardware.display.category.PRESENTATION" }
}
