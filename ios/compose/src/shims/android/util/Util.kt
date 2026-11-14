@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.util

class DisplayMetrics {
    var density = 1f
    var densityDpi = 160
    var scaledDensity = 1f
    var widthPixels = 0
    var heightPixels = 0
    var xdpi = 160f
    var ydpi = 160f
    fun setTo(o: DisplayMetrics) { density = o.density; densityDpi = o.densityDpi; scaledDensity = o.scaledDensity; widthPixels = o.widthPixels; heightPixels = o.heightPixels; xdpi = o.xdpi; ydpi = o.ydpi }
    companion object {
        const val DENSITY_DEFAULT = 160
        fun current() = DisplayMetrics().also {
            val d = com.pokedaisy.ios.IosScreen.density
            it.density = d; it.scaledDensity = d; it.densityDpi = (160 * d).toInt()
            it.widthPixels = com.pokedaisy.ios.IosScreen.widthPx; it.heightPixels = com.pokedaisy.ios.IosScreen.heightPx
            it.xdpi = 460f; it.ydpi = 460f
        }
    }
}

object TypedValue {
    const val COMPLEX_UNIT_PX = 0
    const val COMPLEX_UNIT_DIP = 1
    const val COMPLEX_UNIT_SP = 2
    const val COMPLEX_UNIT_PT = 3
    const val COMPLEX_UNIT_IN = 4
    const val COMPLEX_UNIT_MM = 5
    fun applyDimension(unit: Int, value: Float, m: DisplayMetrics): Float = when (unit) {
        COMPLEX_UNIT_PX -> value
        COMPLEX_UNIT_DIP, COMPLEX_UNIT_SP -> value * m.density
        COMPLEX_UNIT_PT -> value * m.xdpi / 72f
        COMPLEX_UNIT_IN -> value * m.xdpi
        COMPLEX_UNIT_MM -> value * m.xdpi / 25.4f
        else -> value
    }
}

class SparseArray<E> {
    private val m = HashMap<Int, E>()
    operator fun get(k: Int): E? = m[k]
    fun get(k: Int, d: E): E = m[k] ?: d
    fun put(k: Int, v: E) { m[k] = v }
    operator fun set(k: Int, v: E) { m[k] = v }
    fun remove(k: Int) { m.remove(k) }
    fun delete(k: Int) { m.remove(k) }
    fun size() = m.size
    fun clear() = m.clear()
    fun keyAt(i: Int) = m.keys.sorted()[i]
    fun valueAt(i: Int): E = m[keyAt(i)]!!
    fun indexOfKey(k: Int) = m.keys.sorted().indexOf(k)
}
class SparseIntArray {
    private val m = HashMap<Int, Int>()
    fun get(k: Int, d: Int = 0) = m[k] ?: d
    fun put(k: Int, v: Int) { m[k] = v }
    fun delete(k: Int) { m.remove(k) }
    fun size() = m.size
    fun clear() = m.clear()
    fun keyAt(i: Int) = m.keys.sorted()[i]
    fun valueAt(i: Int) = m[keyAt(i)]!!
}
class SparseBooleanArray {
    private val m = HashMap<Int, Boolean>()
    fun get(k: Int, d: Boolean = false) = m[k] ?: d
    fun put(k: Int, v: Boolean) { m[k] = v }
    fun delete(k: Int) { m.remove(k) }
    fun size() = m.size
    fun clear() = m.clear()
}
class Size(val width: Int, val height: Int)
class SizeF(val width: Float, val height: Float)
class Range<T : Comparable<T>>(val lower: T, val upper: T)
class AtomicFile(val baseFile: java.io.File) {
    fun startWrite(): java.io.FileOutputStream = java.io.FileOutputStream(java.io.File(baseFile.path + ".new"))
    fun finishWrite(s: java.io.FileOutputStream) { s.close(); java.io.File(baseFile.path + ".new").renameTo(baseFile) }
    fun failWrite(s: java.io.FileOutputStream) { s.close(); java.io.File(baseFile.path + ".new").delete() }
    fun openRead(): java.io.FileInputStream = java.io.FileInputStream(baseFile)
    fun readFully(): ByteArray = baseFile.readBytes()
    fun delete() { baseFile.delete() }
}
object Base64 {
    const val DEFAULT = 0; const val NO_WRAP = 2; const val NO_PADDING = 1; const val URL_SAFE = 8
    fun encodeToString(b: ByteArray, flags: Int): String = java.util.Base64.getEncoder().encodeToString(b)
    fun encode(b: ByteArray, flags: Int): ByteArray = java.util.Base64.getEncoder().encode(b)
    fun decode(s: String, flags: Int): ByteArray = java.util.Base64.getMimeDecoder().decode(s)
    fun decode(b: ByteArray, flags: Int): ByteArray = java.util.Base64.getMimeDecoder().decode(b)
}
class Pair<F, S>(@kotlin.jvm.JvmField val first: F, @kotlin.jvm.JvmField val second: S) { companion object { fun <A, B> create(a: A, b: B) = Pair(a, b) } }
