@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package androidx.core.graphics

class Insets private constructor(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    companion object { val NONE = Insets(0, 0, 0, 0); fun of(l: Int, t: Int, r: Int, b: Int) = Insets(l, t, r, b) }
}
