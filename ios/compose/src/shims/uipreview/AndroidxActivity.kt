package androidx.activity

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract

/**
 * An activity as ActivityHost runs it: onCreate() / onResume() as on Android, its setContent {} kept as [content]
 * (and in [androidx.activity.compose.Captured]); startActivity / finish go through ActivityHost's stack.
 */
open class ComponentActivity : Context() {
    companion object { const val RESULT_OK = -1; const val RESULT_CANCELED = 0 }
    var intent: Intent = Intent()
    var content: (@androidx.compose.runtime.Composable () -> Unit)? = null
    var isFinishing = false
        internal set
    internal var resultCode = RESULT_CANCELED
    internal var resultData: Intent? = null
    internal var onResult: ((Int, Intent?) -> Unit)? = null
    val contentResolver = android.content.ContentResolver()
    val packageManager = android.content.pm.PackageManager()
    open fun onCreate(savedInstanceState: android.os.Bundle?) {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onStart() {}
    open fun onStop() {}
    open fun onDestroy() {}
    open fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean = false
    open fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean = false
    open fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean = false
    open fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean = false
    fun finish() { com.pokedaisy.ios.ActivityHost.finish(this) }
    fun setResult(code: Int, data: Intent? = null) { resultCode = code; resultData = data }
    fun overridePendingTransition(enter: Int, exit: Int) {}
    fun startActivity(i: Intent) { com.pokedaisy.ios.ActivityHost.start(i) }
    fun startActivity(i: Intent, options: android.os.Bundle?) = startActivity(i)
    fun requestPermissions(p: Array<String>, code: Int) {}
    fun runOnUiThread(r: () -> Unit) = com.pokedaisy.ios.ActivityHost.onMain(r)
    fun recreate() {}
    override fun getSystemService(n: String): Any? = android.content.ClipboardManager()
    fun <I, O> registerForActivityResult(c: ActivityResultContract<I, O>, cb: (O) -> Unit): ActivityResultLauncher<I> =
        ActivityResultLauncher()
}
