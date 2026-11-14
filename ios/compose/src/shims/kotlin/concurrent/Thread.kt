@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package kotlin.concurrent

/** kotlin.concurrent.thread (JVM-only): a started java.lang.Thread. */
fun thread(start: Boolean = true, isDaemon: Boolean = false, contextClassLoader: Any? = null, name: String? = null, priority: Int = -1, block: () -> Unit): java.lang.Thread {
    val t = java.lang.Thread(java.lang.Runnable { block() }, name ?: "Thread")
    if (start) t.start()
    return t
}

fun java.util.Timer.schedule(delay: Long, action: () -> Unit) { thread { java.lang.Thread.sleep(delay); action() } }
