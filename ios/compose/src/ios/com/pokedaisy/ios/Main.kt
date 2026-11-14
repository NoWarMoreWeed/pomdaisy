// PomDaisy (iOS): the entry points Swift calls. Each shows one of the Android app's own screens, built from its own
// Kotlin / Compose sources (see build.gradle.kts).
package com.pokedaisy.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** The Android app's Library (LibraryActivity), the first screen of the app: the games in Documents/files/roms. */
fun LibraryViewController(): UIViewController = ComposeUIViewController { ActivityContent { com.pokedaisy.app.LibraryActivity() } }

/** The Android app's top-screen Settings (SettingsActivity). */
fun SettingsViewController(): UIViewController = ComposeUIViewController { ActivityContent { com.pokedaisy.app.SettingsActivity() } }

/** Runs an activity's onCreate / onResume as Android would and shows what it passed to setContent {}. */
@Composable
private fun ActivityContent(make: () -> androidx.activity.ComponentActivity) {
    val content = remember {
        runCatching {
            androidx.activity.compose.Captured.content = null
            val a = make()
            a.onCreate(null)
            a.onStart()
            a.onResume()
            androidx.activity.compose.Captured.content
        }.onFailure { it.printStackTrace() }.getOrNull()
    }
    if (content != null) content() else Box(Modifier.fillMaxSize().background(Color.Black)) { Text("Couldn't start this screen", color = Color.White) }
}
