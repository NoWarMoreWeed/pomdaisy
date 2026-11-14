package android.content

import android.net.Uri

open class Intent() {
    /** The activity class it starts (its simple name; ActivityHost finds the screen by it). */
    var component: String? = null
    constructor(ctx: Context, cls: Class<*>) : this() { component = cls.simpleName }
    constructor(action: String, uri: Uri? = null) : this() {
        this.action = action
        this.data = uri
    }
    constructor(o: Intent) : this() {
        action = o.action
        data = o.data
        component = o.component
        extrasMap.putAll(o.extrasMap)
    }
    fun setClass(ctx: Context, cls: Class<*>) = apply { component = cls.simpleName }
    fun setClassName(pkg: String, cls: String) = this
    fun setFlags(f: Int) = apply { flags = f }
    fun addFlags(f: Int) = apply { flags = flags or f }
    fun setDataAndType(u: android.net.Uri, type: String) = apply { data = u }

    var action: String? = null
    var data: Uri? = null
    private val extrasMap = HashMap<String, Any?>()
    fun putExtra(k: String, v: String?) = apply { extrasMap[k] = v }
    fun putExtra(k: String, v: Boolean) = apply { extrasMap[k] = v }
    fun putExtra(k: String, v: Int) = apply { extrasMap[k] = v }
    fun putExtra(k: String, v: Long) = apply { extrasMap[k] = v }
    fun putExtra(k: String, v: Float) = apply { extrasMap[k] = v }
    fun putExtra(k: String, v: Array<String>?) = apply { extrasMap[k] = v }
    fun putExtra(k: String, v: IntArray?) = apply { extrasMap[k] = v }
    fun putExtras(o: Intent) = apply { extrasMap.putAll(o.extrasMap) }
    fun getStringExtra(k: String): String? = extrasMap[k] as? String
    fun getIntExtra(k: String, d: Int): Int = extrasMap[k] as? Int ?: d
    fun getLongExtra(k: String, d: Long): Long = extrasMap[k] as? Long ?: d
    fun getFloatExtra(k: String, d: Float): Float = extrasMap[k] as? Float ?: d
    @Suppress("UNCHECKED_CAST") fun getStringArrayExtra(k: String): Array<String>? = extrasMap[k] as? Array<String>
    fun getIntArrayExtra(k: String): IntArray? = extrasMap[k] as? IntArray
    val extras: android.os.Bundle? get() = if (extrasMap.isEmpty()) null else android.os.Bundle().also { b -> extrasMap.forEach { (k, v) -> b.put(k, v) } }
    val dataString: String? get() = data?.toString()
    fun setAction(a: String?) = apply { action = a }
    fun setData(u: Uri?) = apply { data = u }
    fun setPackage(p: String?) = this
    fun addCategory(c: String) = this
    fun setComponent(c: ComponentName?) = apply { component = c?.className }
    var flags = 0
    var type: String? = null
    fun setType(t: String?) = apply { type = t }
    var clipData: ClipData? = null
    fun hasExtra(k: String) = extrasMap.containsKey(k)
    fun removeExtra(k: String) { extrasMap.remove(k) }
    fun getBooleanExtra(k: String, default: Boolean): Boolean = extrasMap[k] as? Boolean ?: default

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED"
        const val ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED"
        const val ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED"
        const val ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF"
        const val ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON"
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
        const val CATEGORY_HOME = "android.intent.category.HOME"
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        const val FLAG_ACTIVITY_SINGLE_TOP = 0x20000000
        const val FLAG_ACTIVITY_NO_ANIMATION = 0x00010000
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000
        const val FLAG_ACTIVITY_MULTIPLE_TASK = 0x08000000
        const val FLAG_ACTIVITY_LAUNCH_ADJACENT = 0x00001000
        const val ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT"
        const val ACTION_OPEN_DOCUMENT_TREE = "android.intent.action.OPEN_DOCUMENT_TREE"
        const val ACTION_CREATE_DOCUMENT = "android.intent.action.CREATE_DOCUMENT"
        const val ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val EXTRA_TITLE = "android.intent.extra.TITLE"
        const val EXTRA_MIME_TYPES = "android.intent.extra.MIME_TYPES"
        const val EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE"
        const val CATEGORY_OPENABLE = "android.intent.category.OPENABLE"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        fun createChooser(i: Intent, title: CharSequence?) = i
        const val FLAG_GRANT_READ_URI_PERMISSION = 1
        const val FLAG_GRANT_WRITE_URI_PERMISSION = 2
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
    }
}

class ClipData { companion object { fun newPlainText(l: String, t: String) = ClipData() } }

class ClipboardManager { fun setPrimaryClip(c: ClipData) {} }

class ContentResolver {
    fun openInputStream(u: Uri): java.io.InputStream? = null
    fun query(u: Uri, a: Array<String>?, b: String?, c: Array<String>?, d: String?): android.database.Cursor? = null
    fun takePersistableUriPermission(u: Uri, f: Int) {}
}
