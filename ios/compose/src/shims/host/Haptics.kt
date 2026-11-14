package com.pokedaisy.ios

/** A light tap (the app's button haptics, Vibrator). */
object Haptics {
    private val gen by lazy { platform.UIKit.UIImpactFeedbackGenerator(platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleLight) }
    fun tap() = ActivityHost.onMain { gen.impactOccurred() }
}

/** This thread's QoS (0x21 user-interactive, 0x19 user-initiated, 0x11 utility). */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun setThreadQos(qos: UInt) { platform.posix.pthread_set_qos_class_self_np(qos, 0) }
