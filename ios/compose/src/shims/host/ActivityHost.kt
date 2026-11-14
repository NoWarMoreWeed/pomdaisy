// PomDaisy (iOS): Android's activity stack, as the app's screens expect it: startActivity(Intent) starts the screen
// its class names (factories registered by the framework module), finish() goes back to the one under it, BACK goes to
// the innermost BackHandler first. One window: the top activity's setContent {} is what shows.
package com.pokedaisy.ios

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

object ActivityHost {
    private val factories = HashMap<String, () -> ComponentActivity>()
    private val stack = mutableStateListOf<ComponentActivity>()
    /** What a screen without an iOS version yet says (its class name). */
    var missing: ((String) -> Unit)? = null

    fun register(name: String, make: () -> ComponentActivity) { factories[name] = make }

    fun start(intent: android.content.Intent) = onMain {
        val name = intent.component
        if (name == null) { openExternal(intent); return@onMain }
        val make = factories[name] ?: run { missing?.invoke(name); return@onMain }
        val a = make()
        a.intent = intent
        stack.lastOrNull()?.onPause()
        stack.add(a)
        runCatching {
            a.onCreate(null)
            a.onStart()
            a.onResume()
        }.onFailure { it.printStackTrace() }
    }

    fun finish(a: ComponentActivity) = onMain {
        if (a.isFinishing) return@onMain
        a.isFinishing = true
        val i = stack.indexOf(a)
        if (i < 0) return@onMain
        a.onPause(); a.onStop(); a.onDestroy()
        stack.removeAt(i)
        a.onResult?.invoke(a.resultCode, a.resultData)
        if (i == stack.size) stack.lastOrNull()?.onResume()
    }

    /** BACK: a BackHandler takes it, else the top activity finishes (never the last one). */
    fun back() {
        if (BackDispatcher.dispatch()) return
        val top = stack.lastOrNull() ?: return
        if (stack.size > 1) finish(top)
    }

    val top: ComponentActivity? get() = stack.lastOrNull()

    /** On the main thread (now, if this is it). */
    fun onMain(block: () -> Unit) {
        if (platform.Foundation.NSThread.isMainThread) block()
        else platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) { block() }
    }

    /** An intent with no activity of ours: a web link opens in Safari. */
    private fun openExternal(intent: android.content.Intent) {
        val url = intent.data?.toString() ?: return
        platform.Foundation.NSURL.URLWithString(url)?.let { platform.UIKit.UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null) }
    }

    /** The window's content: the top activity's. */
    @Composable
    fun Content() {
        val a = stack.lastOrNull()
        if (a == null) {
            Box(Modifier.fillMaxSize().background(Color.Black))
            return
        }
        key(a) {
            val c = a.content
            if (c != null) c() else Box(Modifier.fillMaxSize().background(Color.Black)) { Text(a::class.simpleName ?: "", color = Color.White) }
        }
    }
}

/** BackHandler's callbacks, the innermost (latest) enabled one first. */
object BackDispatcher {
    class Handler(val onBack: () -> Unit) { var enabled = true }
    private val handlers = ArrayList<Handler>()
    fun add(h: Handler) { handlers.add(h) }
    fun remove(h: Handler) { handlers.remove(h) }
    fun dispatch(): Boolean {
        val h = handlers.lastOrNull { it.enabled } ?: return false
        h.onBack()
        return true
    }
}
