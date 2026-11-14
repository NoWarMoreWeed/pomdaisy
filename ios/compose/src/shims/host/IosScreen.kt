package com.pokedaisy.ios

import kotlinx.cinterop.useContents

/** The screen as Android's DisplayMetrics / Configuration report it (main screen, pixels and dp). */
object IosScreen {
    val density: Float get() = platform.UIKit.UIScreen.mainScreen.scale.toFloat()
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    val widthDp: Int get() = platform.UIKit.UIScreen.mainScreen.bounds.useContents { size.width.toInt() }
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    val heightDp: Int get() = platform.UIKit.UIScreen.mainScreen.bounds.useContents { size.height.toInt() }
    val widthPx: Int get() = (widthDp * density).toInt()
    val heightPx: Int get() = (heightDp * density).toInt()
    val portrait: Boolean get() = heightDp >= widthDp
    val refreshRate: Float get() = platform.UIKit.UIScreen.mainScreen.maximumFramesPerSecond.toFloat()
    val uiMode: Int get() {
        val dark = platform.UIKit.UIScreen.mainScreen.traitCollection.userInterfaceStyle == platform.UIKit.UIUserInterfaceStyle.UIUserInterfaceStyleDark
        return (if (dark) 0x20 else 0x10) or 1
    }
}
