// android.media's AudioTrack over AVAudioEngine: a ring of 16-bit stereo samples the engine's source node pulls, write()
// filling it (blocking or not, as asked), the play head counted in frames as Android's.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package android.media

import kotlinx.cinterop.*
import platform.AVFAudio.*

class AudioFormat private constructor(val sampleRate: Int, val channelMask: Int, val encoding: Int) {
    class Builder {
        private var rate = 48000; private var mask = CHANNEL_OUT_STEREO; private var enc = ENCODING_PCM_16BIT
        fun setSampleRate(r: Int) = apply { rate = r }
        fun setChannelMask(m: Int) = apply { mask = m }
        fun setEncoding(e: Int) = apply { enc = e }
        fun build() = AudioFormat(rate, mask, enc)
    }
    val channelCount: Int get() = if (channelMask == CHANNEL_OUT_MONO || channelMask == CHANNEL_IN_MONO) 1 else 2
    companion object {
        const val ENCODING_PCM_16BIT = 2
        const val ENCODING_PCM_FLOAT = 4
        const val ENCODING_PCM_8BIT = 3
        const val CHANNEL_OUT_STEREO = 12
        const val CHANNEL_OUT_MONO = 4
        const val CHANNEL_IN_MONO = 16
        const val CHANNEL_IN_STEREO = 12
    }
}

class AudioAttributes private constructor() {
    class Builder {
        fun setUsage(u: Int) = this
        fun setContentType(c: Int) = this
        fun setFlags(f: Int) = this
        fun setLegacyStreamType(s: Int) = this
        fun setAllowedCapturePolicy(p: Int) = this
        fun setHapticChannelsMuted(b: Boolean) = this
        fun build() = AudioAttributes()
    }
    companion object {
        const val USAGE_GAME = 14
        const val USAGE_MEDIA = 1
        const val CONTENT_TYPE_MUSIC = 2
        const val CONTENT_TYPE_SONIFICATION = 4
        const val CONTENT_TYPE_MOVIE = 3
        const val FLAG_LOW_LATENCY = 256
        const val ALLOW_CAPTURE_BY_ALL = 1
    }
}

class AudioTrack private constructor(private val rate: Int, private val channels: Int, private val floatPcm: Boolean, val bufferSizeInBytes: Int) {
    class Builder {
        private var format = AudioFormat.Builder().build()
        private var bytes = 8192
        fun setAudioAttributes(a: AudioAttributes) = this
        fun setAudioFormat(f: AudioFormat) = apply { format = f }
        fun setBufferSizeInBytes(n: Int) = apply { bytes = n }
        fun setTransferMode(m: Int) = this
        fun setPerformanceMode(m: Int) = this
        fun setSessionId(id: Int) = this
        fun setOffloadedPlayback(b: Boolean) = this
        fun build() = AudioTrack(format.sampleRate, format.channelCount, format.encoding == AudioFormat.ENCODING_PCM_FLOAT, bytes)
    }

    private val frameBytes = channels * (if (floatPcm) 4 else 2)
    /** The ring: frames as float L / R (mono doubled). */
    private val capacity = maxOf(bufferSizeInBytes / frameBytes, 1024) * 2
    private val ring = FloatArray(capacity * 2)
    private val lock = kotlin.PdMutex()
    private var readPos = 0L
    private var writePos = 0L
    @kotlin.concurrent.Volatile private var playing = false
    @kotlin.concurrent.Volatile private var volume = 1f
    @kotlin.concurrent.Volatile private var released = false
    @kotlin.concurrent.Volatile private var underruns = 0
    private var engine: AVAudioEngine? = null

    val sampleRate: Int get() = rate
    val playbackRate: Int get() = rate
    val channelCount: Int get() = channels
    var bufferSizeInFrames: Int
        get() = capacity
        set(v) {}
    var startThresholdInFrames: Int = 0
    val bufferCapacityInFrames: Int get() = capacity
    val underrunCount: Int get() = underruns
    val playState: Int get() = if (playing) PLAYSTATE_PLAYING else PLAYSTATE_PAUSED
    val state: Int get() = if (released) STATE_UNINITIALIZED else STATE_INITIALIZED
    val audioSessionId = 1
    val playbackHeadPosition: Int get() { lock.lock(); try { return readPos.toInt() } finally { lock.unlock() } }
    fun setBufferSizeInFrames(n: Int): Int = capacity
    fun setVolume(v: Float): Int { volume = v; return SUCCESS }
    fun setStereoVolume(l: Float, r: Float): Int { volume = maxOf(l, r); return SUCCESS }
    fun setPlaybackRate(r: Int): Int = SUCCESS
    fun getTimestamp(t: AudioTimestamp): Boolean { t.framePosition = playbackHeadPosition.toLong(); t.nanoTime = java.lang.System.nanoTime(); return true }
    fun attachAuxEffect(id: Int): Int = SUCCESS
    fun setAuxEffectSendLevel(l: Float): Int = SUCCESS

    fun play() {
        if (released) return
        playing = true
        if (engine == null) start()
    }
    fun pause() { playing = false }
    fun stop() { playing = false; flush() }
    fun flush() { lock.lock(); try { readPos = writePos } finally { lock.unlock() } }
    fun release() {
        released = true
        playing = false
        engine?.stop()
        engine = null
    }

    fun write(data: ShortArray, off: Int, len: Int): Int = write(data, off, len, WRITE_BLOCKING)
    fun write(data: ShortArray, off: Int, len: Int, mode: Int): Int = put(len / channels, mode == WRITE_BLOCKING) { f, c -> data[off + f * channels + c] / 32768f } * channels
    fun write(data: FloatArray, off: Int, len: Int, mode: Int): Int = put(len / channels, mode == WRITE_BLOCKING) { f, c -> data[off + f * channels + c] } * channels
    fun write(data: ByteArray, off: Int, len: Int): Int = write(data, off, len, WRITE_BLOCKING)
    fun write(data: ByteArray, off: Int, len: Int, mode: Int): Int = put(len / frameBytes, mode == WRITE_BLOCKING) { f, c ->
        val i = off + f * frameBytes + c * 2
        ((data[i].toInt() and 0xFF) or (data[i + 1].toInt() shl 8)).toShort() / 32768f
    } * frameBytes
    fun write(data: java.nio.ByteBuffer, size: Int, mode: Int): Int {
        val b = ByteArray(size); data.get(b); return write(b, 0, size, mode)
    }

    private inline fun put(frames: Int, blocking: Boolean, sample: (Int, Int) -> Float): Int {
        var done = 0
        while (done < frames) {
            lock.lock()
            val room = (capacity - (writePos - readPos)).toInt()
            val n = minOf(room, frames - done)
            for (k in 0 until n) {
                val at = ((writePos + k) % capacity).toInt() * 2
                val l = sample(done + k, 0)
                ring[at] = l
                ring[at + 1] = if (channels > 1) sample(done + k, 1) else l
            }
            writePos += n
            lock.unlock()
            done += n
            if (!blocking || released) break
            if (done < frames) java.lang.Thread.sleep(2)
        }
        return done
    }

    private fun start() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, null)
        session.setPreferredIOBufferDuration(0.005, null)
        session.setActive(true, null)
        val e = AVAudioEngine()
        val format = AVAudioFormat(standardFormatWithSampleRate = rate.toDouble(), channels = 2u) ?: return
        val node = AVAudioSourceNode(format) { _, _, frameCount, abl ->
            val frames = frameCount.toInt()
            val list = abl!!.pointed
            val bufs = list.mBuffers
            val left = bufs[0].mData?.reinterpret<FloatVar>()
            val right = if (list.mNumberBuffers > 1u) bufs[1].mData?.reinterpret<FloatVar>() else left
            lock.lock()
            val avail = if (playing) (writePos - readPos).toInt() else 0
            val n = minOf(avail, frames)
            val v = volume
            for (i in 0 until frames) {
                if (i < n) {
                    val at = ((readPos + i) % capacity).toInt() * 2
                    left?.set(i, ring[at] * v); right?.set(i, ring[at + 1] * v)
                } else { left?.set(i, 0f); right?.set(i, 0f) }
            }
            readPos += n
            if (playing && n < frames) underruns++
            lock.unlock()
            0
        }
        e.attachNode(node)
        e.connect(node, e.mainMixerNode, format)
        if (e.startAndReturnError(null)) engine = e
    }

    companion object {
        const val MODE_STREAM = 1
        const val MODE_STATIC = 0
        const val WRITE_BLOCKING = 0
        const val WRITE_NON_BLOCKING = 1
        const val PLAYSTATE_STOPPED = 1
        const val PLAYSTATE_PAUSED = 2
        const val PLAYSTATE_PLAYING = 3
        const val STATE_UNINITIALIZED = 0
        const val STATE_INITIALIZED = 1
        const val SUCCESS = 0
        const val ERROR = -1
        const val ERROR_BAD_VALUE = -2
        const val ERROR_INVALID_OPERATION = -3
        const val PERFORMANCE_MODE_LOW_LATENCY = 1
        const val PERFORMANCE_MODE_NONE = 0
        const val PERFORMANCE_MODE_POWER_SAVING = 2
        fun getMinBufferSize(rate: Int, mask: Int, enc: Int): Int = rate / 50 * 4
        fun getNativeOutputSampleRate(stream: Int) = 48000
    }
}

class AudioTimestamp { var framePosition = 0L; var nanoTime = 0L }

class AudioManager {
    fun getProperty(p: String): String? = when (p) { PROPERTY_OUTPUT_SAMPLE_RATE -> "48000"; PROPERTY_OUTPUT_FRAMES_PER_BUFFER -> "256"; else -> null }
    fun getStreamVolume(s: Int) = 10
    fun getStreamMaxVolume(s: Int) = 15
    fun setStreamVolume(s: Int, v: Int, f: Int) {}
    fun adjustStreamVolume(s: Int, d: Int, f: Int) {}
    fun isStreamMute(s: Int) = false
    fun generateAudioSessionId() = 1
    fun requestAudioFocus(r: Any?): Int = 1
    fun abandonAudioFocusRequest(r: Any?): Int = 1
    fun getDevices(f: Int): Array<Any> = emptyArray()
    fun registerAudioDeviceCallback(c: Any?, h: Any?) {}
    fun unregisterAudioDeviceCallback(c: Any?) {}
    fun playSoundEffect(e: Int) {}
    fun playSoundEffect(e: Int, v: Float) {}
    val isMusicActive = false
    companion object {
        const val STREAM_MUSIC = 3
        const val PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE"
        const val PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER"
        const val ADJUST_RAISE = 1; const val ADJUST_LOWER = -1; const val FLAG_SHOW_UI = 1
        const val AUDIOFOCUS_GAIN = 1; const val FX_KEY_CLICK = 0; const val GET_DEVICES_OUTPUTS = 2
    }
}
