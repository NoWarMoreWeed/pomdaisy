// PomDaisy (iOS): the entry points Swift calls. The window runs the Android app's own activities (its Kotlin / Compose
// sources, module appui) on ActivityHost's stack, starting with its Library as on Android.
package com.pokedaisy.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.UIKit.UIViewController

private var registered = false

private fun registerActivities() {
    if (registered) return
    registered = true
    // The Application's start, as Android runs it before any activity.
    runCatching { com.pokedaisy.app.PokeDaisyApp().onCreate() }.onFailure { it.printStackTrace() }
    ActivityHost.register("LibraryActivity") { com.pokedaisy.app.LibraryActivity() }
    ActivityHost.register("SettingsActivity") { com.pokedaisy.app.SettingsActivity() }
    ActivityHost.register("SaveEditorActivity") { com.pokedaisy.app.SaveEditorActivity() }
    ActivityHost.register("PokeDaisyActivity") { com.pokedaisy.app.PokeDaisyActivity() }
    ActivityHost.register("LaunchActivity") { com.pokedaisy.app.LaunchActivity() }
    ActivityHost.register("LinkActivity") { com.pokedaisy.app.LinkActivity() }
}

@OptIn(ExperimentalForeignApi::class)
/** The app as on Android: its Library first, the rest started from there. [onMissing]: a screen iOS hasn't got yet. */
fun AppViewController(onMissing: (String) -> Unit): UIViewController = ComposeUIViewController {
    registerActivities()
    ActivityHost.missing = onMissing
    LaunchedEffect(Unit) {
        if (ActivityHost.top == null) {
            ActivityHost.start(android.content.Intent().also { it.component = "LibraryActivity" })
            // CI's simulator test: PD_TEST_ROM starts that game over the Library, as a tap on its card does.
            val rom = platform.posix.getenv("PD_TEST_ROM")?.toKString()
            if (!rom.isNullOrEmpty()) {
                println("PD_TEST: starting $rom")
                ActivityHost.start(android.content.Intent().also { it.component = "PokeDaisyActivity" }.putExtra("rom", rom))
            }
        }
    }
    BackEdge { ActivityHost.Content() }
}

/** BACK on iOS: a swipe in from the left edge (as Android's back gesture). */
@Composable
private fun BackEdge(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        var fromEdge = false
        var dx = 0f
        detectHorizontalDragGestures(
            onDragStart = { o -> fromEdge = o.x < 24f * density; dx = 0f },
            onDragEnd = { if (fromEdge && dx > 80f * density) ActivityHost.back() },
        ) { _, d -> dx += d }
    }) { content() }
}

/** BACK from Swift (the app's own back button, if it shows one). */
fun goBack() = ActivityHost.back()
