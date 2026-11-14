@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.media.audiofx

open class AudioEffect {
    var enabled = false
    fun release() {}
    companion object {
        const val ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION = "android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION"
        const val ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION = "android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION"
        const val EXTRA_AUDIO_SESSION = "android.media.extra.AUDIO_SESSION"
        const val EXTRA_PACKAGE_NAME = "android.media.extra.PACKAGE_NAME"
        const val EXTRA_CONTENT_TYPE = "android.media.extra.CONTENT_TYPE"
        const val CONTENT_TYPE_GAME = 2
        const val CONTENT_TYPE_MUSIC = 0
    }
}
