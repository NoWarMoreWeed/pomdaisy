@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.media

/** MediaCodec / MediaExtractor (FLAC clips through the platform codec): none on iOS - FlacPcm's own decoder serves. */
class MediaCodec private constructor() {
    class BufferInfo { var offset = 0; var size = 0; var presentationTimeUs = 0L; var flags = 0 }
    fun configure(f: MediaFormat?, s: android.view.Surface?, c: Any?, flags: Int) {}
    fun start() {}
    fun stop() {}
    fun release() {}
    fun flush() {}
    fun dequeueInputBuffer(t: Long) = -1
    fun dequeueOutputBuffer(i: BufferInfo, t: Long) = INFO_TRY_AGAIN_LATER
    fun getInputBuffer(i: Int): java.nio.ByteBuffer? = null
    fun getOutputBuffer(i: Int): java.nio.ByteBuffer? = null
    fun queueInputBuffer(i: Int, off: Int, size: Int, t: Long, flags: Int) {}
    fun releaseOutputBuffer(i: Int, render: Boolean) {}
    val outputFormat: MediaFormat get() = MediaFormat()
    companion object {
        const val BUFFER_FLAG_END_OF_STREAM = 4
        const val INFO_TRY_AGAIN_LATER = -1
        const val INFO_OUTPUT_FORMAT_CHANGED = -2
        const val CONFIGURE_FLAG_ENCODE = 1
        fun createDecoderByType(t: String): MediaCodec = throw java.io.IOException("no MediaCodec on iOS")
        fun createEncoderByType(t: String): MediaCodec = throw java.io.IOException("no MediaCodec on iOS")
    }
}
class MediaFormat {
    private val m = HashMap<String, Any>()
    fun getInteger(k: String): Int = m[k] as? Int ?: 0
    fun getInteger(k: String, d: Int): Int = m[k] as? Int ?: d
    fun getLong(k: String): Long = m[k] as? Long ?: 0L
    fun getString(k: String): String? = m[k] as? String
    fun containsKey(k: String) = m.containsKey(k)
    fun setInteger(k: String, v: Int) { m[k] = v }
    fun setString(k: String, v: String) { m[k] = v }
    fun setLong(k: String, v: Long) { m[k] = v }
    companion object {
        const val KEY_MIME = "mime"; const val KEY_SAMPLE_RATE = "sample-rate"; const val KEY_CHANNEL_COUNT = "channel-count"
        const val KEY_DURATION = "durationUs"; const val KEY_PCM_ENCODING = "pcm-encoding"; const val KEY_BIT_RATE = "bitrate"
        const val KEY_MAX_INPUT_SIZE = "max-input-size"; const val KEY_FLAC_COMPRESSION_LEVEL = "flac-compression-level"
        const val MIMETYPE_AUDIO_FLAC = "audio/flac"; const val MIMETYPE_AUDIO_RAW = "audio/raw"
        fun createAudioFormat(mime: String, rate: Int, channels: Int) = MediaFormat().also { it.setString(KEY_MIME, mime); it.setInteger(KEY_SAMPLE_RATE, rate); it.setInteger(KEY_CHANNEL_COUNT, channels) }
    }
}
class MediaExtractor {
    fun setDataSource(path: String) { throw java.io.IOException("no MediaExtractor on iOS") }
    fun setDataSource(fd: java.io.FileDescriptor) { throw java.io.IOException("no MediaExtractor on iOS") }
    val trackCount = 0
    fun getTrackFormat(i: Int) = MediaFormat()
    fun selectTrack(i: Int) {}
    fun readSampleData(b: java.nio.ByteBuffer, off: Int) = -1
    val sampleTime: Long get() = -1
    fun advance() = false
    fun release() {}
}
/** MediaPlayer (the launcher's videos): plays nothing on iOS yet. */
class MediaPlayer {
    var isLooping = false
    val isPlaying = false
    fun setSurface(s: android.view.Surface?) {}
    fun setDataSource(path: String) {}
    fun setVolume(l: Float, r: Float) {}
    fun setOnPreparedListener(l: ((MediaPlayer) -> Unit)?) {}
    fun setOnCompletionListener(l: ((MediaPlayer) -> Unit)?) {}
    fun setOnErrorListener(l: ((MediaPlayer, Int, Int) -> Boolean)?) {}
    fun prepare() {}
    fun prepareAsync() {}
    fun start() {}
    fun pause() {}
    fun stop() {}
    fun reset() {}
    fun release() {}
    fun seekTo(ms: Int) {}
    val duration = 0
    val currentPosition = 0
    val videoWidth = 0
    val videoHeight = 0
}
/** SoundPool (the click sound): loads nothing on iOS yet. */
class SoundPool private constructor() {
    class Builder { fun setMaxStreams(n: Int) = this; fun setAudioAttributes(a: AudioAttributes) = this; fun build() = SoundPool() }
    fun load(path: String, priority: Int) = 0
    fun load(c: android.content.Context, res: Int, priority: Int) = 0
    fun play(id: Int, l: Float, r: Float, priority: Int, loop: Int, rate: Float) = 0
    fun stop(id: Int) {}
    fun unload(id: Int) = true
    fun release() {}
    fun setOnLoadCompleteListener(l: ((SoundPool, Int, Int) -> Unit)?) {}
    fun autoPause() {}
    fun autoResume() {}
}
