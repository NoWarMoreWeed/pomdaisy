// okhttp3's few classes the co-op relay names (behind its feature flag): it isn't available on iOS yet.
@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package okhttp3

class OkHttpClient {
    class Builder {
        fun pingInterval(n: Long, u: java.util.concurrent.TimeUnit) = this
        fun connectTimeout(n: Long, u: java.util.concurrent.TimeUnit) = this
        fun readTimeout(n: Long, u: java.util.concurrent.TimeUnit) = this
        fun writeTimeout(n: Long, u: java.util.concurrent.TimeUnit) = this
        fun build() = OkHttpClient()
    }
    fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
        listener.onFailure(object : WebSocket { override fun send(text: String) = false; override fun send(bytes: okio.ByteString) = false; override fun close(code: Int, reason: String?) = true; override fun cancel() {} },
            java.io.IOException("WebSockets aren't available on iOS yet"), null)
        return object : WebSocket { override fun send(text: String) = false; override fun send(bytes: okio.ByteString) = false; override fun close(code: Int, reason: String?) = true; override fun cancel() {} }
    }
    fun newBuilder() = Builder()
    val dispatcher = Dispatcher()
    class Dispatcher { val executorService = java.util.concurrent.Executors.newSingleThreadExecutor() }
}
class Request private constructor(val url: String) {
    class Builder { private var u = ""; fun url(s: String) = apply { u = s }; fun header(k: String, v: String) = this; fun addHeader(k: String, v: String) = this; fun build() = Request(u) }
}
class Response(val code: Int = 0, val message: String = "")
interface WebSocket {
    fun send(text: String): Boolean
    fun send(bytes: okio.ByteString): Boolean
    fun close(code: Int, reason: String?): Boolean
    fun cancel()
}
abstract class WebSocketListener {
    open fun onOpen(webSocket: WebSocket, response: Response) {}
    open fun onMessage(webSocket: WebSocket, text: String) {}
    open fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {}
    open fun onClosing(webSocket: WebSocket, code: Int, reason: String) {}
    open fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}
    open fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}
}
