package android.os

/** The device as Android code asks about it: an Apple phone or tablet on a current API level. */
object Build {
    val MANUFACTURER: String get() = "Apple"
    val BRAND: String get() = "Apple"
    val MODEL: String get() = platform.UIKit.UIDevice.currentDevice.model
    val DEVICE: String get() = platform.UIKit.UIDevice.currentDevice.model
    val PRODUCT: String get() = platform.UIKit.UIDevice.currentDevice.model
    val HARDWARE: String get() = "apple"
    val BOARD: String get() = "apple"
    val FINGERPRINT: String get() = "apple/ios"
    val SOC_MODEL: String get() = "apple"
    val SOC_MANUFACTURER: String get() = "Apple"
    val SUPPORTED_ABIS: Array<String> get() = arrayOf("arm64-v8a")
    val SUPPORTED_64_BIT_ABIS: Array<String> get() = arrayOf("arm64-v8a")
    val SUPPORTED_32_BIT_ABIS: Array<String> get() = emptyArray()
    const val TYPE = "user"
    object VERSION { const val SDK_INT = 34; const val RELEASE = "14"; const val INCREMENTAL = "ios"; const val CODENAME = "REL" }
    object VERSION_CODES {
        const val LOLLIPOP = 21; const val M = 23; const val N = 24; const val N_MR1 = 25; const val O = 26; const val O_MR1 = 27; const val P = 28
        const val Q = 29; const val R = 30; const val S = 31; const val S_V2 = 32; const val TIRAMISU = 33; const val UPSIDE_DOWN_CAKE = 34; const val VANILLA_ICE_CREAM = 35
    }
}

class LocaleList(vararg val locales: java.util.Locale) {
    fun get(i: Int) = locales[i]
    fun size() = locales.size
    val isEmpty: Boolean get() = locales.isEmpty()
    companion object { fun getDefault() = LocaleList(java.util.Locale.getDefault()); fun forLanguageTags(t: String) = LocaleList(java.util.Locale.forLanguageTag(t)) }
}
