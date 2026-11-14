@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package androidx.compose.ui.viewinterop

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** A View inside Compose (its tree run by ViewHost, as the activity's own). */
@Composable
fun <T : View> AndroidView(factory: (Context) -> T, modifier: Modifier = Modifier, update: (T) -> Unit = {}) {
    val ctx = LocalContext.current
    val view = remember { factory(ctx) }
    SideEffect { update(view) }
    com.pokedaisy.ios.ViewHost.Host(view, modifier)
}

@Composable
fun <T : View> AndroidView(factory: (Context) -> T, modifier: Modifier = Modifier, onReset: ((T) -> Unit)?, onRelease: (T) -> Unit = {}, update: (T) -> Unit = {}) =
    AndroidView(factory, modifier, update)
