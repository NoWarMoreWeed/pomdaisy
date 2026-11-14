// android.os: the main thread's Handler / Looper over GCD, SystemClock, threads, power, vibration, battery.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package android.os

import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.darwin.dispatch_time

class Looper private constructor(internal val queue: dispatch_queue_t, private val main: Boolean) {
    val thread: java.lang.Thread get() = java.lang.Thread.currentThread()
    val isCurrentThread: Boolean get() = main && platform.Foundation.NSThread.isMainThread
    fun quit() {}
    fun quitSafely() {}
    companion object {
        private val mainLooper = Looper(dispatch_get_main_queue(), true)
        fun getMainLooper() = mainLooper
        fun myLooper(): Looper? = if (platform.Foundation.NSThread.isMainThread) mainLooper else null
        fun prepare() {}
        fun loop() {}
        internal fun background(name: String) = Looper(dispatch_queue_create(name, null)!!, false)
    }
}

/** A Handler posts to its looper's GCD queue; removeCallbacks drops posts not yet run. */
open class Handler(private val looper: Looper = Looper.getMainLooper(), private val callback: Callback? = null) {
    constructor(callback: Callback?) : this(Looper.getMainLooper(), callback)
    fun interface Callback { fun handleMessage(m: Message): Boolean }
    private val cancelled = HashSet<Any>()
    private val pending = HashMap<Any, Int>()
    val getLooper: Looper get() = looper
    fun getLooper() = looper
    private fun track(r: Any) = synchronized(this) { pending[r] = (pending[r] ?: 0) + 1; cancelled.remove(r) }
    private fun runIfLive(r: Any, block: () -> Unit) {
        val live = synchronized(this) {
            val n = (pending[r] ?: 1) - 1
            if (n <= 0) pending.remove(r) else pending[r] = n
            !cancelled.contains(r)
        }
        if (live) block()
    }
    fun post(r: Runnable): Boolean { track(r); dispatch_async(looper.queue) { runIfLive(r) { r.run() } }; return true }
    fun post(r: () -> Unit): Boolean = post(Runnable(r))
    fun postDelayed(r: Runnable, ms: Long): Boolean {
        track(r)
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, ms.coerceAtLeast(0) * 1_000_000), looper.queue) { runIfLive(r) { r.run() } }
        return true
    }
    fun postDelayed(r: () -> Unit, ms: Long): Boolean = postDelayed(Runnable(r), ms)
    fun postDelayed(r: Runnable, token: Any?, ms: Long): Boolean = postDelayed(r, ms)
    fun postAtFrontOfQueue(r: Runnable) = post(r)
    fun postAtTime(r: Runnable, uptimeMs: Long): Boolean = postDelayed(r, uptimeMs - SystemClock.uptimeMillis())
    fun removeCallbacks(r: Runnable) { synchronized(this) { if (pending.containsKey(r)) cancelled.add(r) } }
    fun removeCallbacksAndMessages(token: Any?) { synchronized(this) { cancelled.addAll(pending.keys) } }
    fun sendEmptyMessage(what: Int): Boolean = sendMessage(Message.obtain(this, what))
    fun sendEmptyMessageDelayed(what: Int, ms: Long): Boolean { val m = Message.obtain(this, what); postDelayed(Runnable { dispatchMessage(m) }, ms); return true }
    fun sendMessage(m: Message): Boolean { post(Runnable { dispatchMessage(m) }); return true }
    fun sendMessageDelayed(m: Message, ms: Long): Boolean { postDelayed(Runnable { dispatchMessage(m) }, ms); return true }
    fun removeMessages(what: Int) {}
    fun hasMessages(what: Int) = false
    fun obtainMessage(what: Int = 0, obj: Any? = null) = Message.obtain(this, what).also { it.obj = obj }
    open fun handleMessage(m: Message) {}
    fun dispatchMessage(m: Message) { m.callback?.run() ?: run { if (callback?.handleMessage(m) != true) handleMessage(m) } }
    companion object {
        internal val main = Handler(Looper.getMainLooper())
        fun createAsync(l: Looper) = Handler(l)
    }
}

class Message {
    var what = 0
    var arg1 = 0
    var arg2 = 0
    var obj: Any? = null
    var target: Handler? = null
    internal var callback: Runnable? = null
    fun sendToTarget() { target?.sendMessage(this) }
    fun recycle() {}
    companion object {
        fun obtain() = Message()
        fun obtain(h: Handler?, what: Int = 0) = Message().also { it.target = h; it.what = what }
        fun obtain(h: Handler?, r: Runnable) = Message().also { it.target = h; it.callback = r }
    }
}

open class HandlerThread(threadName: String, priority: Int = 0) : java.lang.Thread(null, threadName) {
    private var l: Looper? = null
    override fun run() {}
    private val queueName = threadName
    val looper: Looper get() = l ?: Looper.background(queueName).also { l = it }
    fun getLooper() = looper
    fun quit(): Boolean = true
    fun quitSafely(): Boolean = true
    val threadId: Int get() = 0
}

object SystemClock {
    fun uptimeMillis(): Long = java.lang.System.nanoTime() / 1_000_000
    fun elapsedRealtime(): Long = java.lang.System.nanoTime() / 1_000_000
    fun elapsedRealtimeNanos(): Long = java.lang.System.nanoTime()
    fun uptimeNanos(): Long = java.lang.System.nanoTime()
    fun currentThreadTimeMillis(): Long = uptimeMillis()
    fun sleep(ms: Long) = java.lang.Thread.sleep(ms)
}

object Process {
    const val THREAD_PRIORITY_DEFAULT = 0
    const val THREAD_PRIORITY_BACKGROUND = 10
    const val THREAD_PRIORITY_URGENT_AUDIO = -19
    const val THREAD_PRIORITY_AUDIO = -16
    const val THREAD_PRIORITY_DISPLAY = -4
    const val THREAD_PRIORITY_URGENT_DISPLAY = -8
    const val THREAD_PRIORITY_FOREGROUND = -2
    const val THREAD_PRIORITY_MORE_FAVORABLE = -1
    fun setThreadPriority(p: Int) {
        com.pokedaisy.ios.setThreadQos(when {
            p <= THREAD_PRIORITY_DISPLAY -> 0x21u
            p >= THREAD_PRIORITY_BACKGROUND -> 0x11u
            else -> 0x19u
        })
    }
    fun setThreadPriority(tid: Int, p: Int) = setThreadPriority(p)
    fun myPid() = platform.posix.getpid()
    fun myTid() = 0
    fun myUid() = 0
    fun killProcess(pid: Int) {}
    fun getThreadPriority(tid: Int) = 0
    fun is64Bit() = true
}

class IBinder
class PowerManager {
    val isPowerSaveMode: Boolean get() = false
    val isSustainedPerformanceModeSupported = false
    val isInteractive = true
    fun newWakeLock(flags: Int, tag: String) = WakeLock()
    class WakeLock { fun acquire() {}; fun acquire(ms: Long) {}; fun release() {}; val isHeld = false; fun setReferenceCounted(b: Boolean) {} }
    val currentThermalStatus: Int get() = 0
    fun addThermalStatusListener(l: Any?) {}
    fun removeThermalStatusListener(l: Any?) {}
    companion object { const val PARTIAL_WAKE_LOCK = 1; const val THERMAL_STATUS_NONE = 0; const val THERMAL_STATUS_SEVERE = 3 }
}
open class Vibrator {
    fun hasVibrator() = true
    fun vibrate(ms: Long) = com.pokedaisy.ios.Haptics.tap()
    fun vibrate(e: VibrationEffect) = com.pokedaisy.ios.Haptics.tap()
    fun vibrate(e: VibrationEffect, a: Any?) = com.pokedaisy.ios.Haptics.tap()
    fun cancel() {}
    fun hasAmplitudeControl() = false
}
class VibratorManager { val defaultVibrator = Vibrator() }
class VibrationEffect private constructor() {
    companion object {
        const val DEFAULT_AMPLITUDE = -1
        const val EFFECT_CLICK = 0
        const val EFFECT_TICK = 2
        fun createOneShot(ms: Long, amp: Int) = VibrationEffect()
        fun createPredefined(id: Int) = VibrationEffect()
        fun createWaveform(t: LongArray, r: Int) = VibrationEffect()
    }
}
class BatteryManager {
    fun getIntProperty(id: Int): Int = when (id) {
        BATTERY_PROPERTY_CAPACITY -> { platform.UIKit.UIDevice.currentDevice.batteryMonitoringEnabled = true; (platform.UIKit.UIDevice.currentDevice.batteryLevel * 100).toInt().coerceIn(0, 100) }
        else -> 0
    }
    val isCharging: Boolean get() = platform.UIKit.UIDevice.currentDevice.batteryState == platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateCharging
    companion object {
        const val BATTERY_PROPERTY_CAPACITY = 4
        const val EXTRA_TEMPERATURE = "temperature"
        const val EXTRA_VOLTAGE = "voltage"
        const val EXTRA_HEALTH = "health"
        const val EXTRA_PRESENT = "present"
        const val BATTERY_PLUGGED_AC = 1
        const val BATTERY_PLUGGED_USB = 2
        const val EXTRA_LEVEL = "level"
        const val EXTRA_SCALE = "scale"
        const val EXTRA_STATUS = "status"
        const val EXTRA_PLUGGED = "plugged"
        const val BATTERY_STATUS_CHARGING = 2
        const val BATTERY_STATUS_FULL = 5
        const val ACTION_CHARGING = "android.os.action.CHARGING"
    }
}
class ParcelFileDescriptor(val file: java.io.File? = null) : java.io.Closeable {
    val fd = -1
    override fun close() {}
    fun detachFd() = -1
    val fileDescriptor: java.io.FileDescriptor? get() = null
    val statSize: Long get() = file?.length() ?: -1
    companion object {
        const val MODE_READ_ONLY = 0x10000000
        const val MODE_WRITE_ONLY = 0x20000000
        const val MODE_READ_WRITE = 0x30000000
        const val MODE_CREATE = 0x08000000
        const val MODE_TRUNCATE = 0x04000000
        fun open(f: java.io.File, mode: Int) = ParcelFileDescriptor(f)
        fun dup(fd: java.io.FileDescriptor?) = ParcelFileDescriptor()
    }
}
class CancellationSignal { fun cancel() {}; val isCanceled = false }
object Debug {
    fun isDebuggerConnected() = false
    fun getNativeHeapAllocatedSize(): Long = 0L
    fun getNativeHeapSize(): Long = 0L
    fun getNativeHeapFreeSize(): Long = 0L
}
