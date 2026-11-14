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
        extras.putAll(o.extras)
    }
    fun setClass(ctx: Context, cls: Class<*>) = apply { component = cls.simpleName }
    fun setClassName(pkg: String, cls: String) = this
    fun setFlags(f: Int) = this
    fun addFlags(f: Int) = this
    fun setDataAndType(u: android.net.Uri, type: String) = apply { data = u }

    var action: String? = null
    var data: Uri? = null
    private val extras = HashMap<String, Any?>()
    fun putExtra(k: String, v: String?) = apply { extras[k] = v }
    fun putExtra(k: String, v: Boolean) = apply { extras[k] = v }
    fun putExtra(k: String, v: Int) = apply { extras[k] = v }
    fun putExtra(k: String, v: Long) = apply { extras[k] = v }
    fun putExtra(k: String, v: Float) = apply { extras[k] = v }
    fun putExtra(k: String, v: Array<String>?) = apply { extras[k] = v }
    fun putExtra(k: String, v: IntArray?) = apply { extras[k] = v }
    fun putExtras(o: Intent) = apply { extras.putAll(o.extras) }
    fun getStringExtra(k: String): String? = extras[k] as? String
    fun getIntExtra(k: String, d: Int): Int = extras[k] as? Int ?: d
    fun getLongExtra(k: String, d: Long): Long = extras[k] as? Long ?: d
    fun getFloatExtra(k: String, d: Float): Float = extras[k] as? Float ?: d
    @Suppress("UNCHECKED_CAST") fun getStringArrayExtra(k: String): Array<String>? = extras[k] as? Array<String>
    fun getIntArrayExtra(k: String): IntArray? = extras[k] as? IntArray
    fun hasExtra(k: String) = extras.containsKey(k)
    fun removeExtra(k: String) { extras.remove(k) }
    fun setType(t: String) = this
    fun getBooleanExtra(k: String, default: Boolean): Boolean = extras[k] as? Boolean ?: default

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
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
