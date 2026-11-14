package android.content

import java.io.File
import java.io.InputStream

/** The app's assets (app/src/main/assets), copied into the iOS app's bundle as "assets". */
class AssetManager(private val root: File) {
    fun open(name: String): InputStream = File(root, name).inputStream()
    fun list(path: String): Array<String>? = File(root, path).list()
}

/** Context.applicationInfo's one field the app reads (RomIntake: the private data directory). */
class ContextAppInfo(val dataDir: String? = null)

open class Context {
    val applicationContext: Context get() = this
    val packageName = "com.pokedaisy.app"
    val applicationInfo = ContextAppInfo(IosDirs.documents)
    val resources = android.content.res.Resources()
    val assets = AssetManager(File(IosDirs.bundle, "assets"))
    val filesDir = File(IosDirs.documents).apply { mkdirs() }
    val cacheDir = File(IosDirs.caches).apply { mkdirs() }
    fun getExternalFilesDir(t: String?): File? = (if (t == null) File(IosDirs.documents) else File(IosDirs.documents, t)).apply { mkdirs() }
    fun getSharedPreferences(n: String, mode: Int): SharedPreferences = SharedPreferences.named(n)
    open fun getSystemService(n: String): Any? = null
    fun checkSelfPermission(p: String) = 0

    companion object {
        const val MODE_PRIVATE = 0
        const val CLIPBOARD_SERVICE = "clipboard"
        const val CONNECTIVITY_SERVICE = "connectivity"
        const val WIFI_SERVICE = "wifi"
        const val NSD_SERVICE = "servicediscovery"
    }
}

/** Where things are on iOS: the app bundle's resources, Documents (the Files app shows it), Caches. */
object IosDirs {
    val bundle: String get() = platform.Foundation.NSBundle.mainBundle.resourcePath ?: "."
    val documents: String get() = (platform.Foundation.NSSearchPathForDirectoriesInDomains(platform.Foundation.NSDocumentDirectory,
        platform.Foundation.NSUserDomainMask, true).firstOrNull() as? String) ?: "."
    val caches: String get() = (platform.Foundation.NSSearchPathForDirectoriesInDomains(platform.Foundation.NSCachesDirectory,
        platform.Foundation.NSUserDomainMask, true).firstOrNull() as? String) ?: "."
}

/** SharedPreferences over NSUserDefaults (one key prefix per preferences file), values kept in memory as well. */
class SharedPreferences private constructor(private val name: String) {
    private val d = platform.Foundation.NSUserDefaults.standardUserDefaults
    private fun k(key: String) = "$name/$key"
    val all: Map<String, *> get() = d.dictionaryRepresentation().entries.mapNotNull { (key, v) -> (key as? String)?.takeIf { it.startsWith("$name/") }?.let { it.removePrefix("$name/") to v } }.toMap()
    fun getAll(): Map<String, *> = all
    fun getString(key: String, def: String?): String? = d.objectForKey(k(key)) as? String ?: def
    fun getStringSet(key: String, def: Set<String>?): Set<String>? = (d.objectForKey(k(key)) as? List<*>)?.map { it.toString() }?.toSet() ?: def
    fun getInt(key: String, def: Int): Int = (d.objectForKey(k(key)) as? Number)?.toInt() ?: def
    fun getLong(key: String, def: Long): Long = (d.objectForKey(k(key)) as? Number)?.toLong() ?: def
    fun getFloat(key: String, def: Float): Float = (d.objectForKey(k(key)) as? Number)?.toFloat() ?: def
    fun getBoolean(key: String, def: Boolean): Boolean = (d.objectForKey(k(key)) as? Boolean) ?: def
    fun contains(key: String) = d.objectForKey(k(key)) != null
    fun edit() = Editor(this)
    fun registerOnSharedPreferenceChangeListener(l: Any?) {}
    fun unregisterOnSharedPreferenceChangeListener(l: Any?) {}

    class Editor(private val p: SharedPreferences) {
        private val d = p.d
        fun putString(key: String, v: String?) = apply { if (v == null) d.removeObjectForKey(p.k(key)) else d.setObject(v, p.k(key)) }
        fun putStringSet(key: String, v: Set<String>?) = apply { if (v == null) d.removeObjectForKey(p.k(key)) else d.setObject(v.toList(), p.k(key)) }
        fun putInt(key: String, v: Int) = apply { d.setInteger(v.toLong(), p.k(key)) }
        fun putLong(key: String, v: Long) = apply { d.setObject(platform.Foundation.NSNumber(longLong = v), p.k(key)) }
        fun putFloat(key: String, v: Float) = apply { d.setFloat(v, p.k(key)) }
        fun putBoolean(key: String, v: Boolean) = apply { d.setBool(v, p.k(key)) }
        fun remove(key: String) = apply { d.removeObjectForKey(p.k(key)) }
        fun clear() = apply { p.all.keys.forEach { d.removeObjectForKey(p.k(it)) } }
        fun apply() {}
        fun commit() = true
    }

    companion object {
        private val files = HashMap<String, SharedPreferences>()
        fun named(n: String): SharedPreferences = synchronized(files) { files.getOrPut(n) { SharedPreferences(n) } }
        val shared: SharedPreferences get() = named("pokedaisy")
    }
}
