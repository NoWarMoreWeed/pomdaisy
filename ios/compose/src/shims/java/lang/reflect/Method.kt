@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package java.lang.reflect

/** Reflection isn't on iOS: Class.getMethod throws, so this is never made. */
class Method private constructor() {
    fun invoke(o: Any?, vararg args: Any?): Any? = null
    var isAccessible = false
    val name = ""
}
