// android.app as the game screen uses it: Activity (ComponentActivity's base: its content view, window, lifecycle),
// Application, ActivityManager, Dialog and Presentation (a second screen: none on iOS).
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.app

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize

/**
 * An activity as ActivityHost runs it: onCreate() / onResume() as on Android, its setContent {} or setContentView(view)
 * kept as [content]; startActivity / finish go through ActivityHost's stack.
 */
open class Activity : android.content.ContextWrapper() {
    companion object { const val RESULT_OK = -1; const val RESULT_CANCELED = 0; const val RESULT_FIRST_USER = 1 }
    var intent: android.content.Intent = android.content.Intent()
    var content: (@androidx.compose.runtime.Composable () -> Unit)? = null
    var isFinishing = false
        internal set
    var resultCode = RESULT_CANCELED
        internal set
    var resultData: android.content.Intent? = null
        internal set
    var onResult: ((Int, android.content.Intent?) -> Unit)? = null
    /** setContentView's view: ActivityHost shows it through ViewHost. */
    var contentView: View? = null
        internal set
    internal fun showView(v: View) {
        val c: @androidx.compose.runtime.Composable () -> Unit = { com.pokedaisy.ios.ViewHost.Host(v, androidx.compose.ui.Modifier.fillMaxSize()) }
        content = c
        androidx.activity.compose.Captured.content = c
    }
    open fun onCreate(savedInstanceState: android.os.Bundle?) {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onStart() {}
    open fun onStop() {}
    open fun onDestroy() {}
    open fun onRestart() {}
    open fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean = contentView?.dispatchKeyEvent(event) ?: false
    open fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean = false
    open fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean = false
    open fun onKeyLongPress(keyCode: Int, event: android.view.KeyEvent): Boolean = false
    open fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean = contentView?.dispatchGenericMotionEvent(event) ?: false
    open fun onGenericMotionEvent(event: android.view.MotionEvent): Boolean = false
    open fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean = contentView?.dispatchTouchEvent(event) ?: false
    open fun onTouchEvent(event: android.view.MotionEvent): Boolean = false
    open fun finish() { com.pokedaisy.ios.ActivityHost.finish(this) }
    fun setResult(code: Int, data: android.content.Intent? = null) { resultCode = code; resultData = data }
    fun overridePendingTransition(enter: Int, exit: Int) {}
    fun overrideActivityTransition(type: Int, enter: Int, exit: Int) {}
    fun startActivity(i: android.content.Intent) { com.pokedaisy.ios.ActivityHost.start(i) }
    fun startActivity(i: android.content.Intent, options: android.os.Bundle?) = startActivity(i)
    fun requestPermissions(p: Array<String>, code: Int) {}
    fun runOnUiThread(r: Runnable) = com.pokedaisy.ios.ActivityHost.onMain { r.run() }
    fun recreate() {}
    fun setIntent(i: android.content.Intent) { intent = i }
    fun hasWindowFocus() = !isFinishing
    val referrer: android.net.Uri? get() = null
    fun isTaskRoot() = true
    val window = android.view.Window()
    val windowManager: android.view.WindowManager get() = android.view.WindowManager()
    var requestedOrientation = -1
    open val isFinishingActivity: Boolean get() = false
    val isDestroyed: Boolean get() = destroyed
    internal var destroyed = false
    val isInMultiWindowMode = false
    val isInPictureInPictureMode = false
    val isChangingConfigurations = false
    val componentName: android.content.ComponentName get() = android.content.ComponentName("com.pokedaisy.app", this::class.simpleName ?: "")
    val callingActivity: android.content.ComponentName? get() = null
    val application: Application get() = Application.instance
    val taskId = 1
    val title: CharSequence get() = ""
    open fun setContentView(v: View) { contentView = v; showView(v) }
    open fun setContentView(v: View, p: android.view.ViewGroup.LayoutParams?) = setContentView(v)
    open fun addContentView(v: View, p: android.view.ViewGroup.LayoutParams?) {}
    open fun finishAndRemoveTask() = finish()
    open fun finishAffinity() = finishAndRemoveTask()
    open fun moveTaskToBack(b: Boolean): Boolean = false
    open fun setShowWhenLocked(b: Boolean) {}
    open fun setTurnScreenOn(b: Boolean) {}
    open fun setTaskDescription(d: Any?) {}
    open fun reportFullyDrawn() {}
    open fun setVolumeControlStream(s: Int) {}
    open fun onWindowFocusChanged(hasFocus: Boolean) {}
    open fun onConfigurationChanged(c: android.content.res.Configuration) {}
    open fun onLowMemory() {}
    open fun onTrimMemory(level: Int) {}
    open fun onUserLeaveHint() {}
    open fun onNewIntent(i: android.content.Intent) {}
    open fun onSaveInstanceState(b: android.os.Bundle) {}
    open fun onRestoreInstanceState(b: android.os.Bundle) {}
    open fun onPostCreate(b: android.os.Bundle?) {}
    open fun onMultiWindowModeChanged(b: Boolean) {}
    open fun onBackPressed() = finish()
    open fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {}
    open fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {}
    fun <T : View> findViewById(id: Int): T? = null
    fun setTitle(t: CharSequence?) {}
    fun startActivityForResult(i: android.content.Intent, code: Int) = startActivity(i)
    fun shouldShowRequestPermissionRationale(p: String) = false
    fun enterPictureInPictureMode(): Boolean = false
    val currentFocus: View? get() = null
    val onBackPressedDispatcher = androidx.activity.OnBackPressedDispatcher()
}

open class Application : android.content.ContextWrapper() {
    open fun onCreate() {}
    open fun onTerminate() {}
    open fun registerActivityLifecycleCallbacks(c: Any?) {}
    companion object { val instance = Application() }
}

class ActivityManager {
    class MemoryInfo { var totalMem = (platform.Foundation.NSProcessInfo.processInfo.physicalMemory).toLong(); var availMem = totalMem / 2; var lowMemory = false; var threshold = 0L }
    fun getMemoryInfo(m: MemoryInfo) {}
    val memoryClass = 512
    val largeMemoryClass = 1024
    val isLowRamDevice = false
    fun getRunningAppProcesses(): List<Any> = emptyList()
    val appTasks: List<AppTask> get() = emptyList()
    fun moveTaskToFront(id: Int, flags: Int) {}
    fun moveTaskToFront(id: Int, flags: Int, o: android.os.Bundle?) {}
    val deviceConfigurationInfo: ConfigurationInfo get() = ConfigurationInfo()
    class AppTask { fun finishAndRemoveTask() {}; fun moveToFront() {}; val taskInfo: Any? = null }
    class ConfigurationInfo { val reqGlEsVersion = 0x30002; val glEsVersion = "3.2" }
    companion object { fun isUserAMonkey() = false; fun getMyMemoryState(o: Any?) {} }
}

open class Dialog(val context: Context, theme: Int = 0) {
    var isShowing = false; private set
    val window = android.view.Window()
    open fun show() { isShowing = true; onStart() }
    open fun dismiss() { isShowing = false; onStop() }
    fun cancel() = dismiss()
    fun hide() { isShowing = false }
    open fun setContentView(v: View) {}
    open fun onCreate(b: android.os.Bundle?) {}
    open fun onStart() {}
    open fun onStop() {}
    fun setCancelable(b: Boolean) {}
    fun setCanceledOnTouchOutside(b: Boolean) {}
    fun setOnDismissListener(l: Any?) {}
    fun setOnCancelListener(l: Any?) {}
    fun setOnShowListener(l: Any?) {}
}

/** A window on a second display: iOS shows no second screen to the app here (DisplayManager lists one display). */
open class Presentation(outerContext: Context, val display: android.view.Display, theme: Int = 0) : Dialog(outerContext, theme) {
    override fun show() { /* never shown: there is no second display */ }
    open fun onDisplayRemoved() {}
    open fun dispatchKeyEvent(e: android.view.KeyEvent): Boolean = false
    open fun dispatchGenericMotionEvent(e: android.view.MotionEvent): Boolean = false
    open fun onDisplayChanged() {}
}

object PendingIntentFlags
class PendingIntent { companion object { const val FLAG_IMMUTABLE = 0x04000000; const val FLAG_UPDATE_CURRENT = 0x08000000; fun getActivity(c: Context, r: Int, i: android.content.Intent, f: Int) = PendingIntent(); fun getBroadcast(c: Context, r: Int, i: android.content.Intent, f: Int) = PendingIntent() } }
class ActivityOptions { fun toBundle(): android.os.Bundle = android.os.Bundle(); fun setLaunchDisplayId(id: Int) = this; companion object { fun makeBasic() = ActivityOptions(); fun makeCustomAnimation(c: Context, a: Int, b: Int) = ActivityOptions() } }
class AlertDialog(context: Context) : Dialog(context) {
    class Builder(val context: Context) {
        fun setTitle(t: CharSequence?) = this; fun setMessage(t: CharSequence?) = this; fun setPositiveButton(t: CharSequence?, l: Any?) = this
        fun setNegativeButton(t: CharSequence?, l: Any?) = this; fun setView(v: View?) = this; fun setCancelable(b: Boolean) = this
        fun create() = AlertDialog(context); fun show() = create().also { it.show() }
    }
}
class UiModeManager { val currentModeType = 1; var nightMode = 0 }
