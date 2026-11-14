@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.graphics.fonts

class Font private constructor() {
    class Builder(path: Any?) { fun setWeight(w: Int) = this; fun setSlant(s: Int) = this; fun build() = Font() }
}
class FontFamily private constructor() {
    class Builder(f: Font) { fun addFont(f: Font) = this; fun build() = FontFamily() }
}
