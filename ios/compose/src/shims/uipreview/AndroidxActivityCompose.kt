package androidx.activity.compose

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable

/** The last content an activity passed to setContent {}. */
object Captured { var content: (@Composable () -> Unit)? = null }

fun ComponentActivity.setContent(parent: androidx.compose.runtime.CompositionContext? = null, content: @Composable () -> Unit) {
    Captured.content = content
    this.content = content
}

/** BACK (iOS: ActivityHost's back gesture / button) goes to the innermost enabled handler, else finishes the activity. */
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val current = androidx.compose.runtime.rememberUpdatedState(onBack)
    val handler = androidx.compose.runtime.remember { com.pokedaisy.ios.BackDispatcher.Handler { current.value() } }
    handler.enabled = enabled
    androidx.compose.runtime.DisposableEffect(handler) {
        com.pokedaisy.ios.BackDispatcher.add(handler)
        onDispose { com.pokedaisy.ios.BackDispatcher.remove(handler) }
    }
}

@Composable
fun <I, O> rememberLauncherForActivityResult(c: ActivityResultContract<I, O>, cb: (O) -> Unit): ActivityResultLauncher<I> =
    ActivityResultLauncher()
