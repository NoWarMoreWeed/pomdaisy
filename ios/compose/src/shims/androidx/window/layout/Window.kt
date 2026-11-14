@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package androidx.window.layout

/** Jetpack WindowManager's fold reports: an iPhone doesn't fold - no features ever. */
interface DisplayFeature { val bounds: android.graphics.Rect }
interface FoldingFeature : DisplayFeature {
    val state: State
    val orientation: Orientation
    val isSeparating: Boolean
    val occlusionType: OcclusionType
    class State private constructor(val name: String) { companion object { val FLAT = State("FLAT"); val HALF_OPENED = State("HALF_OPENED") } }
    class Orientation private constructor(val name: String) { companion object { val VERTICAL = Orientation("VERTICAL"); val HORIZONTAL = Orientation("HORIZONTAL") } }
    class OcclusionType private constructor(val name: String) { companion object { val NONE = OcclusionType("NONE"); val FULL = OcclusionType("FULL") } }
}
class WindowLayoutInfo(val displayFeatures: List<DisplayFeature> = emptyList())
class WindowInfoTracker private constructor() {
    companion object { fun getOrCreate(c: android.content.Context) = WindowInfoTracker() }
}
