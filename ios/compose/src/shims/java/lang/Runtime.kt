@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package java.lang

class Runtime private constructor() {
    fun availableProcessors(): Int = platform.Foundation.NSProcessInfo.processInfo.activeProcessorCount.toInt()
    fun totalMemory(): Long = 512L shl 20
    fun freeMemory(): Long = 256L shl 20
    fun maxMemory(): Long = 1024L shl 20
    fun gc() {}
    fun addShutdownHook(t: Thread) {}
    fun exec(cmd: String): Process = throw java.io.IOException("no processes on iOS")
    fun exec(cmd: Array<String>): Process = throw java.io.IOException("no processes on iOS")
    companion object { private val r = Runtime(); fun getRuntime() = r }
}
