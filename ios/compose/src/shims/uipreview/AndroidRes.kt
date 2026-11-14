package android.content.res

/** The device's configuration as iOS reports it: orientation, night mode, screen size in dp. */
class Configuration() {
    constructor(o: Configuration?) : this()
    fun setTo(o: Configuration?) {}
    fun setLocale(l: java.util.Locale?) {}
    fun setLocales(l: android.os.LocaleList?) {}
    val uiMode: Int get() = com.pokedaisy.ios.IosScreen.uiMode
    val orientation: Int get() = if (com.pokedaisy.ios.IosScreen.portrait) ORIENTATION_PORTRAIT else ORIENTATION_LANDSCAPE
    val screenWidthDp: Int get() = com.pokedaisy.ios.IosScreen.widthDp
    val screenHeightDp: Int get() = com.pokedaisy.ios.IosScreen.heightDp
    val smallestScreenWidthDp: Int get() = minOf(screenWidthDp, screenHeightDp)
    val densityDpi: Int get() = (160 * com.pokedaisy.ios.IosScreen.density).toInt()
    val fontScale = 1f
    val keyboard = KEYBOARD_NOKEYS
    val navigation = NAVIGATION_NONAV
    val touchscreen = 3
    val screenLayout = 0
    val locales: android.os.LocaleList get() = android.os.LocaleList(java.util.Locale.getDefault())
    val locale: java.util.Locale get() = java.util.Locale.getDefault()
    fun isNightModeActive() = uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES

    companion object {
        const val UI_MODE_NIGHT_MASK = 0x30
        const val UI_MODE_NIGHT_NO = 0x10
        const val UI_MODE_NIGHT_YES = 0x20
        const val UI_MODE_TYPE_MASK = 0x0F
        const val UI_MODE_TYPE_NORMAL = 1
        const val UI_MODE_TYPE_TELEVISION = 4
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
        const val ORIENTATION_UNDEFINED = 0
        const val KEYBOARD_NOKEYS = 1
        const val KEYBOARD_QWERTY = 2
        const val NAVIGATION_NONAV = 1
        const val NAVIGATION_DPAD = 2
    }
}

class Resources() {
    constructor(assets: android.content.AssetManager?, metrics: android.util.DisplayMetrics?, config: Configuration?) : this()
    val assets: android.content.AssetManager get() = android.content.Context().assets
    val configuration = Configuration()
    val displayMetrics: android.util.DisplayMetrics get() = android.util.DisplayMetrics.current()
    fun getDimension(id: Int) = 0f
    fun getDimensionPixelSize(id: Int) = 0
    fun getIdentifier(name: String, type: String, pkg: String) = 0
    fun getString(id: Int) = ""
    fun getColor(id: Int, theme: Any?) = 0
}
