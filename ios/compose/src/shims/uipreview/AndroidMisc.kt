package android.util

object Log {
    fun d(t: String, m: String) = 0
    fun i(t: String, m: String) = 0
    fun w(t: String, m: String) = 0
    fun w(t: String, m: String, e: Throwable?) = 0
    fun e(t: String, m: String) = 0
    fun e(t: String, m: String, e: Throwable?) = 0
}

/** android.util.LruCache's surface the library's cover cache uses: an access-ordered map evicting by [sizeOf]. */
open class LruCache<K : Any, V : Any>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()
    private var size = 0
    protected open fun sizeOf(key: K, value: V): Int = 1
    fun get(key: K): V? = synchronized(this) { map.remove(key)?.also { map[key] = it } } // re-inserted: the newest last
    fun put(key: K, value: V): V? = synchronized(this) {
        val old = map.put(key, value)
        if (old != null) size -= sizeOf(key, old)
        size += sizeOf(key, value)
        val it = map.entries.iterator()
        while (size > maxSize && it.hasNext()) {
            val e = it.next()
            if (e.key == key) continue
            size -= sizeOf(e.key, e.value)
            it.remove()
        }
        old
    }
}
