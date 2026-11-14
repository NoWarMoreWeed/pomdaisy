@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.system

/** sysconf for the CPU-time readout (CpuUsage). */
object Os {
    fun sysconf(name: Int): Long = when (name) { OsConstants._SC_CLK_TCK -> 100L; OsConstants._SC_NPROCESSORS_CONF -> platform.Foundation.NSProcessInfo.processInfo.processorCount.toLong(); else -> -1L }
    fun gettid(): Int = 0
    fun getpid(): Int = platform.posix.getpid()
}
object OsConstants { const val _SC_CLK_TCK = 2; const val _SC_NPROCESSORS_CONF = 83; const val _SC_PAGESIZE = 29 }
class ErrnoException(val functionName: String, val errno: Int) : Exception(functionName)
