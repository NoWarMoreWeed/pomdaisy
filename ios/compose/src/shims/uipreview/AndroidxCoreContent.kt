package androidx.core.content

object FileProvider {
    fun getUriForFile(c: android.content.Context, authority: String, f: java.io.File): android.net.Uri =
        android.net.Uri.parse("content://$authority/${f.name}")
}

object ContextCompat {
    fun getMainExecutor(c: android.content.Context): java.util.concurrent.Executor = c.mainExecutor
    fun getColor(c: android.content.Context, id: Int) = 0
    fun checkSelfPermission(c: android.content.Context, p: String) = 0
    fun registerReceiver(c: android.content.Context, r: android.content.BroadcastReceiver?, f: android.content.IntentFilter, flags: Int): android.content.Intent? = c.registerReceiver(r, f)
    fun startForegroundService(c: android.content.Context, i: android.content.Intent) {}
    fun getDrawable(c: android.content.Context, id: Int): android.graphics.drawable.Drawable? = null
    const val RECEIVER_EXPORTED = 2
    const val RECEIVER_NOT_EXPORTED = 4
}
