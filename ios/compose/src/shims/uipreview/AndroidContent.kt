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
    open fun getResources(): android.content.res.Resources = defaultResources
    private val defaultResources = android.content.res.Resources()
    val applicationContext: Context get() = this
    val packageName = "com.pokedaisy.app"
    val applicationInfo = ContextAppInfo(IosDirs.documents)
    val resources: android.content.res.Resources get() = getResources()
    val assets = AssetManager(File(IosDirs.bundle, "assets"))
    val filesDir = File(IosDirs.documents).apply { mkdirs() }
    val cacheDir = File(IosDirs.caches).apply { mkdirs() }
    fun getExternalFilesDir(t: String?): File? = (if (t == null) File(IosDirs.documents) else File(IosDirs.documents, t)).apply { mkdirs() }
    fun getSharedPreferences(n: String, mode: Int): SharedPreferences = SharedPreferences.named(n)
    open fun getSystemService(n: String): Any? = when (n) {
        DISPLAY_SERVICE -> android.hardware.display.DisplayManager()
        WINDOW_SERVICE -> android.view.WindowManager()
        POWER_SERVICE -> android.os.PowerManager()
        VIBRATOR_SERVICE -> android.os.Vibrator()
        VIBRATOR_MANAGER_SERVICE -> android.os.VibratorManager()
        BATTERY_SERVICE -> android.os.BatteryManager()
        ACTIVITY_SERVICE -> android.app.ActivityManager()
        AUDIO_SERVICE -> android.media.AudioManager()
        CLIPBOARD_SERVICE -> ClipboardManager()
        INPUT_SERVICE -> android.hardware.input.InputManager()
        UI_MODE_SERVICE -> android.app.UiModeManager()
        else -> null
    }
    /** Android's is a platform type the app treats as present. */
    @Suppress("UNCHECKED_CAST")
    fun <T> getSystemService(c: Class<T>): T = when (c.simpleName) {
        "WindowManager" -> getSystemService(WINDOW_SERVICE)
        "DisplayManager" -> getSystemService(DISPLAY_SERVICE)
        "PowerManager" -> getSystemService(POWER_SERVICE)
        "ActivityManager" -> getSystemService(ACTIVITY_SERVICE)
        "AudioManager" -> getSystemService(AUDIO_SERVICE)
        "BatteryManager" -> getSystemService(BATTERY_SERVICE)
        "Vibrator" -> getSystemService(VIBRATOR_SERVICE)
        "VibratorManager" -> getSystemService(VIBRATOR_MANAGER_SERVICE)
        "InputManager" -> getSystemService(INPUT_SERVICE)
        "ClipboardManager" -> getSystemService(CLIPBOARD_SERVICE)
        "UiModeManager" -> getSystemService(UI_MODE_SERVICE)
        "ShortcutManager" -> android.content.pm.ShortcutManager()
        else -> null
    } as T
    val contentResolver: ContentResolver get() = ContentResolver()
    val packageManager: android.content.pm.PackageManager get() = android.content.pm.PackageManager()
    val mainLooper: android.os.Looper get() = android.os.Looper.getMainLooper()
    val mainExecutor: java.util.concurrent.Executor get() = java.util.concurrent.Executor { r -> com.pokedaisy.ios.ActivityHost.onMain { r.run() } }
    val theme: Any? get() = null
    val display: android.view.Display? get() = android.view.Display.main
    fun getString(id: Int): String = ""
    fun getColor(id: Int): Int = 0
    fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter): Intent? = if (f.hasAction(Intent.ACTION_BATTERY_CHANGED)) batteryIntent() else null
    fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter, flags: Int): Intent? = registerReceiver(r, f)
    fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter, perm: String?, h: android.os.Handler?): Intent? = registerReceiver(r, f)
    fun unregisterReceiver(r: BroadcastReceiver) {}
    fun sendBroadcast(i: Intent) {}
    fun createDisplayContext(d: android.view.Display): Context = this
    fun createConfigurationContext(c: android.content.res.Configuration): Context = this
    fun startService(i: Intent): ComponentName? = null
    fun stopService(i: Intent): Boolean = false
    fun grantUriPermission(p: String, u: android.net.Uri, f: Int) {}
    fun revokeUriPermission(u: android.net.Uri, f: Int) {}
    fun openFileOutput(name: String, mode: Int): java.io.FileOutputStream = java.io.FileOutputStream(java.io.File(filesDir, name))
    fun openFileInput(name: String): java.io.FileInputStream = java.io.FileInputStream(java.io.File(filesDir, name))
    fun getFileStreamPath(name: String) = java.io.File(filesDir, name)
    fun getDir(name: String, mode: Int) = java.io.File(filesDir, name).apply { mkdirs() }
    val noBackupFilesDir: java.io.File get() = filesDir
    val dataDir: java.io.File get() = filesDir
    fun getExternalFilesDirs(t: String?): Array<java.io.File?> = arrayOf(getExternalFilesDir(t))
    val externalCacheDir: java.io.File? get() = cacheDir
    fun deleteFile(name: String) = java.io.File(filesDir, name).delete()
    private fun batteryIntent(): Intent = Intent(Intent.ACTION_BATTERY_CHANGED).also {
        val d = platform.UIKit.UIDevice.currentDevice
        d.batteryMonitoringEnabled = true
        it.putExtra(android.os.BatteryManager.EXTRA_LEVEL, (d.batteryLevel * 100).toInt().coerceIn(0, 100))
        it.putExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
        val charging = d.batteryState == platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateCharging
        it.putExtra(android.os.BatteryManager.EXTRA_STATUS, if (charging) android.os.BatteryManager.BATTERY_STATUS_CHARGING else 3)
        it.putExtra(android.os.BatteryManager.EXTRA_PLUGGED, if (charging) 1 else 0)
    }
    fun checkSelfPermission(p: String) = 0

    companion object {
        const val MODE_PRIVATE = 0
        const val CLIPBOARD_SERVICE = "clipboard"
        const val CONNECTIVITY_SERVICE = "connectivity"
        const val WIFI_SERVICE = "wifi"
        const val NSD_SERVICE = "servicediscovery"
        const val DISPLAY_SERVICE = "display"
        const val WINDOW_SERVICE = "window"
        const val POWER_SERVICE = "power"
        const val VIBRATOR_SERVICE = "vibrator"
        const val VIBRATOR_MANAGER_SERVICE = "vibrator_manager"
        const val BATTERY_SERVICE = "batterymanager"
        const val ACTIVITY_SERVICE = "activity"
        const val AUDIO_SERVICE = "audio"
        const val INPUT_SERVICE = "input"
        const val UI_MODE_SERVICE = "uimode"
        const val SENSOR_SERVICE = "sensor"
        const val STORAGE_SERVICE = "storage"
        const val USB_SERVICE = "usb"
        const val MODE_APPEND = 0x8000
        const val RECEIVER_EXPORTED = 2
        const val RECEIVER_NOT_EXPORTED = 4
        const val BIND_AUTO_CREATE = 1
    }
}

open class ContextWrapper(base: Context? = null) : Context() {
    val baseContext: Context get() = this
}

class ComponentName(val packageName: String, val className: String) {
    constructor(ctx: Context, cls: Class<*>) : this("com.pokedaisy.app", cls.simpleName)
    val shortClassName: String get() = className
    fun flattenToString() = "$packageName/$className"
}

abstract class BroadcastReceiver { abstract fun onReceive(context: Context, intent: Intent) }

class IntentFilter(vararg actions: String) {
    private val list = actions.toMutableList()
    fun addAction(a: String) { list.add(a) }
    fun hasAction(a: String) = a in list
    fun addCategory(c: String) {}
    fun addDataScheme(s: String) {}
    var priority = 0
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
