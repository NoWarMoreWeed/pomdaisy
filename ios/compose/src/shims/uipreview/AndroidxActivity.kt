package androidx.activity

import androidx.compose.foundation.layout.fillMaxSize

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract

/** The androidx activity: android.app.Activity (where the lifecycle and stack live) plus activity results. */
open class ComponentActivity : android.app.Activity() {
    fun <I, O> registerForActivityResult(c: ActivityResultContract<I, O>, cb: (O) -> Unit): ActivityResultLauncher<I> =
        ActivityResultLauncher()
}

class OnBackPressedDispatcher {
    fun addCallback(owner: Any?, cb: OnBackPressedCallback) { cb.handler = com.pokedaisy.ios.BackDispatcher.Handler { if (cb.isEnabled) cb.handleOnBackPressed() }.also { com.pokedaisy.ios.BackDispatcher.add(it) } }
    fun addCallback(cb: OnBackPressedCallback) = addCallback(null, cb)
    fun onBackPressed() = com.pokedaisy.ios.ActivityHost.back()
}
abstract class OnBackPressedCallback(enabled: Boolean) {
    var isEnabled = enabled
        set(v) { field = v; handler?.enabled = v }
    internal var handler: com.pokedaisy.ios.BackDispatcher.Handler? = null
    abstract fun handleOnBackPressed()
    fun remove() { handler?.let { com.pokedaisy.ios.BackDispatcher.remove(it) } }
}
