package android.os

open class Bundle {
    private val m = HashMap<String, Any?>()
    fun put(k: String, v: Any?) { m[k] = v }
    fun getString(k: String): String? = m[k] as? String
    fun getString(k: String, d: String): String = m[k] as? String ?: d
    fun getInt(k: String, d: Int = 0): Int = m[k] as? Int ?: d
    fun getLong(k: String, d: Long = 0L): Long = m[k] as? Long ?: d
    fun getBoolean(k: String, d: Boolean = false): Boolean = m[k] as? Boolean ?: d
    fun putString(k: String, v: String?) { m[k] = v }
    fun putInt(k: String, v: Int) { m[k] = v }
    fun putLong(k: String, v: Long) { m[k] = v }
    fun putBoolean(k: String, v: Boolean) { m[k] = v }
    fun containsKey(k: String) = m.containsKey(k)
    fun keySet(): Set<String> = m.keys
    fun get(k: String): Any? = m[k]
    fun remove(k: String) { m.remove(k) }
    val isEmpty: Boolean get() = m.isEmpty()
    fun size() = m.size
}

object Environment {
    fun isExternalStorageManager() = true
    fun getExternalStorageDirectory() = java.io.File("/sdcard")
}
