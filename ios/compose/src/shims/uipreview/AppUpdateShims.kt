package android.content.pm

/** In-app update: the preview may always "install". */
class PackageManager {
    fun canRequestPackageInstalls() = true
    fun queryIntentActivities(i: android.content.Intent, flags: Int): List<ResolveInfo> = emptyList()
    fun getPackageArchiveInfo(path: String, flags: Int): PackageInfo? = null
    fun getPackageInfo(name: String, flags: Int) = PackageInfo()

    companion object {
        const val GET_SIGNATURES = 0x40
        const val GET_SIGNING_CERTIFICATES = 0x8000000
    }
}

class PackageInfo {
    val packageName = ""
    val versionCode = 0
    val longVersionCode = 0L
    val firstInstallTime = 0L
    val lastUpdateTime = 0L
    val signingInfo: SigningInfo? = null
    val signatures: Array<Signature>? = null
}

class SigningInfo { val apkContentsSigners: Array<Signature> = emptyArray() }

class Signature { fun toCharsString() = "" }

class ResolveInfo { val activityInfo = ActivityInfo() }

class ActivityInfo {
    val packageName = ""; val name = ""; val applicationInfo = ApplicationInfo()
    companion object {
        const val SCREEN_ORIENTATION_UNSPECIFIED = -1
        const val SCREEN_ORIENTATION_LANDSCAPE = 0
        const val SCREEN_ORIENTATION_PORTRAIT = 1
        const val SCREEN_ORIENTATION_USER = 2
        const val SCREEN_ORIENTATION_SENSOR = 4
        const val SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6
        const val SCREEN_ORIENTATION_SENSOR_PORTRAIT = 7
        const val SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8
        const val SCREEN_ORIENTATION_FULL_SENSOR = 10
        const val SCREEN_ORIENTATION_USER_LANDSCAPE = 11
        const val SCREEN_ORIENTATION_LOCKED = 14
    }
}

class ApplicationInfo {
    val flags = 0
    companion object { const val FLAG_SYSTEM = 1 }
}

class ShortcutManager {
    val isRequestPinShortcutSupported = false
    fun requestPinShortcut(i: ShortcutInfo, s: Any?) = false
    val dynamicShortcuts: List<ShortcutInfo> get() = emptyList()
    fun setDynamicShortcuts(l: List<ShortcutInfo>) = false
    fun removeAllDynamicShortcuts() {}
    fun reportShortcutUsed(id: String) {}
}
class ShortcutInfo private constructor(val id: String) {
    class Builder(c: android.content.Context, val id: String) {
        fun setShortLabel(s: CharSequence) = this; fun setLongLabel(s: CharSequence) = this; fun setIcon(i: android.graphics.drawable.Icon) = this
        fun setIntent(i: android.content.Intent) = this; fun setIntents(i: Array<android.content.Intent>) = this; fun build() = ShortcutInfo(id)
    }
}
