package com.pokedaisy.app

/** The app's generated BuildConfig (AGP makes it on Android): the iOS build's values. */
object BuildConfig {
    const val DEBUG = false
    const val VERSION_NAME = "1.1.0"
    const val SCREENSCRAPER_DEV_ID = ""
    const val SCREENSCRAPER_DEV_PASSWORD = ""
    /** Off as in a default build; the ScreenScraper shots turn it on (`withScreenScraper`). */
    @kotlin.jvm.JvmField var FEATURE_SCREENSCRAPER = false
    /** Off: the co-op link (sockets, WebSockets, AES-GCM) isn't on iOS yet. */
    @kotlin.jvm.JvmField var FEATURE_COOP = false
    /** Off as in a release: the blocked-game shots show the notice. */
    @kotlin.jvm.JvmField var FEATURE_ALLOW_BLOCKED = false
    @kotlin.jvm.JvmField var FEATURE_KEEP_BLOCKED = false
    const val COOP_RELAY_URL = ""
}
