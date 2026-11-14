@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package java.net

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.*

open class SocketException(message: String? = null) : java.io.IOException(message)
open class SocketTimeoutException(message: String? = null) : java.io.InterruptedIOException(message)
open class UnknownHostException(message: String? = null) : java.io.IOException(message)
open class ConnectException(message: String? = null) : SocketException(message)
open class MalformedURLException(message: String? = null) : java.io.IOException(message)
open class URISyntaxException(input: String?, reason: String?) : Exception(reason)
open class ProtocolException(message: String? = null) : java.io.IOException(message)

class URI(private val s: String) {
    override fun toString() = s
    val scheme: String? get() = s.substringBefore("://", "").ifEmpty { null }
    val host: String? get() = s.substringAfter("://", "").substringBefore('/').substringBefore(':').ifEmpty { null }
    val path: String? get() = "/" + s.substringAfter("://", s).substringAfter('/', "").substringBefore('?')
    val port: Int get() = s.substringAfter("://", "").substringBefore('/').substringAfter(':', "").toIntOrNull() ?: -1
    val query: String? get() = s.substringAfter('?', "").ifEmpty { null }
    val rawQuery: String? get() = query
    fun toURL() = URL(s)
    fun resolve(other: String) = URI(if (other.contains("://")) other else s.substringBeforeLast('/') + "/" + other)
    companion object { fun create(s: String) = URI(s) }
}

class URL(private val spec: String) {
    constructor(base: URL?, spec: String) : this(if (base == null || spec.contains("://")) spec else base.toString().substringBeforeLast('/') + "/" + spec)
    override fun toString() = spec
    val host: String get() = URI(spec).host ?: ""
    val path: String get() = URI(spec).path ?: ""
    val protocol: String get() = URI(spec).scheme ?: ""
    val query: String? get() = URI(spec).query
    val port: Int get() = URI(spec).port
    fun toURI() = URI(spec)
    fun toExternalForm() = spec
    fun openConnection(): URLConnection = HttpURLConnection(this)
    fun openConnection(proxy: Any?): URLConnection = HttpURLConnection(this)
    fun openStream(): java.io.InputStream = (openConnection() as HttpURLConnection).inputStream
    fun readBytes(): ByteArray = openStream().readAllBytes()
    fun readText(): String = readBytes().decodeToString()
}

abstract class URLConnection(val url: URL) {
    var connectTimeout = 15000
    var readTimeout = 15000
    var doOutput = false
    var doInput = true
    var useCaches = false
    var instanceFollowRedirects = true
    abstract fun connect()
    abstract val inputStream: java.io.InputStream
    abstract val outputStream: java.io.OutputStream
    abstract fun setRequestProperty(key: String, value: String)
    abstract fun addRequestProperty(key: String, value: String)
    abstract fun getHeaderField(name: String): String?
    open val contentLength: Int get() = getHeaderField("Content-Length")?.toIntOrNull() ?: -1
    open val contentLengthLong: Long get() = getHeaderField("Content-Length")?.toLongOrNull() ?: -1
    open val contentType: String? get() = getHeaderField("Content-Type")
    fun getContentLengthLong() = contentLengthLong
}

/** One synchronous request through NSURLSession, made on first use (connect / response / stream). */
open class HttpURLConnection(url: URL) : URLConnection(url) {
    var requestMethod = "GET"
    private val headers = LinkedHashMap<String, String>()
    private val body = java.io.ByteArrayOutputStream()
    private var done = false
    private var code = -1
    private var message: String? = null
    private var data = ByteArray(0)
    private var respHeaders: Map<String, String> = emptyMap()
    private var failure: java.io.IOException? = null

    override fun setRequestProperty(key: String, value: String) { headers[key] = value }
    override fun addRequestProperty(key: String, value: String) { headers[key] = headers[key]?.let { "$it, $value" } ?: value }
    fun getRequestProperty(key: String) = headers[key]
    override val outputStream: java.io.OutputStream get() = body
    fun setFixedLengthStreamingMode(n: Int) {}
    fun setFixedLengthStreamingMode(n: Long) {}
    fun setChunkedStreamingMode(n: Int) {}

    override fun connect() {
        if (done) return
        done = true
        val nsUrl = NSURL.URLWithString(url.toString()) ?: run { failure = MalformedURLException(url.toString()); return }
        val req = NSMutableURLRequest.requestWithURL(nsUrl, NSURLRequestReloadIgnoringLocalCacheData, (connectTimeout + readTimeout) / 1000.0)
        req.setHTTPMethod(requestMethod)
        for ((k, v) in headers) req.setValue(v, forHTTPHeaderField = k)
        val bytes = body.toByteArray()
        if (bytes.isNotEmpty()) req.setHTTPBody(bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) })
        val sem = dispatch_semaphore_create(0)
        val task = NSURLSession.sharedSession.dataTaskWithRequest(req) { d, r, e ->
            if (e != null) failure = if (e.code == NSURLErrorTimedOut) SocketTimeoutException(e.localizedDescription) else java.io.IOException(e.localizedDescription)
            (r as? NSHTTPURLResponse)?.let { resp ->
                code = resp.statusCode.toInt()
                message = NSHTTPURLResponse.localizedStringForStatusCode(resp.statusCode)
                respHeaders = resp.allHeaderFields.entries.associate { (k, v) -> k.toString().lowercase() to v.toString() }
            }
            if (d != null) data = d.toByteArray()
            dispatch_semaphore_signal(sem)
        }
        task.resume()
        dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER)
    }

    val responseCode: Int get() { connect(); failure?.let { if (code < 0) throw it }; return code }
    fun getResponseCode() = responseCode
    val responseMessage: String? get() { connect(); return message }
    override val inputStream: java.io.InputStream get() {
        connect()
        failure?.let { if (code < 0) throw it }
        if (code >= 400) throw java.io.FileNotFoundException(url.toString())
        return java.io.ByteArrayInputStream(data)
    }
    val errorStream: java.io.InputStream? get() { connect(); return if (code >= 400) java.io.ByteArrayInputStream(data) else null }
    override fun getHeaderField(name: String): String? { connect(); return respHeaders[name.lowercase()] }
    val headerFields: Map<String, List<String>> get() { connect(); return respHeaders.mapValues { listOf(it.value) } }
    fun disconnect() {}

    companion object {
        const val HTTP_OK = 200
        const val HTTP_CREATED = 201
        const val HTTP_NO_CONTENT = 204
        const val HTTP_PARTIAL = 206
        const val HTTP_MOVED_PERM = 301
        const val HTTP_MOVED_TEMP = 302
        const val HTTP_NOT_MODIFIED = 304
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_UNAVAILABLE = 503
        const val HTTP_INTERNAL_ERROR = 500
    }
}

typealias HttpsURLConnection = HttpURLConnection

internal fun NSData.toByteArray(): ByteArray {
    val n = length.toInt()
    val out = ByteArray(n)
    if (n > 0) out.usePinned { platform.posix.memcpy(it.addressOf(0), bytes, length) }
    return out
}

object URLEncoder {
    fun encode(s: String, enc: String = "UTF-8"): String = buildString {
        for (b in s.encodeToByteArray()) {
            val c = b.toInt() and 0xFF
            val ch = c.toChar()
            when {
                ch.isLetterOrDigit() && c < 0x80 || ch == '.' || ch == '-' || ch == '*' || ch == '_' -> append(ch)
                ch == ' ' -> append('+')
                else -> append('%').append(c.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
    fun encode(s: String, cs: kotlin.text.Charset) = encode(s)
}

object URLDecoder {
    fun decode(s: String, enc: String = "UTF-8"): String {
        val out = java.io.ByteArrayOutputStream(); var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '+' -> { out.write(' '.code); i++ }
                c == '%' && i + 2 < s.length + 0 && i + 2 <= s.length - 1 + 1 -> { out.write(s.substring(i + 1, i + 3).toInt(16)); i += 3 }
                else -> { out.write(c.toString().encodeToByteArray()); i++ }
            }
        }
        return out.toByteArray().decodeToString()
    }
    fun decode(s: String, cs: kotlin.text.Charset) = decode(s)
}

// ---- sockets: the co-op link's local paths (behind its feature flag) aren't available on iOS yet ----

open class InetAddress internal constructor(val hostAddress: String, private val bytes: ByteArray = ByteArray(4)) {
    val address: ByteArray get() = bytes
    val hostName: String get() = hostAddress
    open val isLoopbackAddress: Boolean get() = hostAddress.startsWith("127.")
    open val isMulticastAddress: Boolean get() = (bytes.firstOrNull()?.toInt()?.and(0xF0)) == 0xE0
    open val isSiteLocalAddress: Boolean get() = hostAddress.startsWith("192.168.") || hostAddress.startsWith("10.") || hostAddress.startsWith("172.")
    open val isLinkLocalAddress: Boolean get() = hostAddress.startsWith("169.254.")
    val isAnyLocalAddress: Boolean get() = hostAddress == "0.0.0.0"
    override fun toString() = "/$hostAddress"
    companion object {
        fun getByName(host: String): InetAddress = Inet4Address(host)
        fun getAllByName(host: String): Array<InetAddress> = arrayOf(getByName(host))
        fun getLocalHost(): InetAddress = Inet4Address("127.0.0.1")
        fun getLoopbackAddress(): InetAddress = Inet4Address("127.0.0.1")
        fun getByAddress(b: ByteArray): InetAddress = Inet4Address(b.joinToString(".") { (it.toInt() and 0xFF).toString() })
    }
}
class Inet4Address(host: String) : InetAddress(host, host.split('.').mapNotNull { it.toIntOrNull()?.toByte() }.toByteArray())
class Inet6Address(host: String) : InetAddress(host, ByteArray(16))

open class SocketAddress
class InetSocketAddress(val hostString: String?, val port: Int) : SocketAddress() {
    constructor(port: Int) : this("0.0.0.0", port)
    constructor(addr: InetAddress?, port: Int) : this(addr?.hostAddress, port)
    val address: InetAddress get() = InetAddress.getByName(hostString ?: "0.0.0.0")
    val hostName: String? get() = hostString
    val isUnresolved: Boolean get() = false
    companion object { fun createUnresolved(host: String, port: Int) = InetSocketAddress(host, port) }
}

open class Socket() : java.io.Closeable {
    constructor(host: String, port: Int) : this() { throw ConnectException("sockets aren't available on iOS yet") }
    fun connect(addr: SocketAddress, timeout: Int = 0): Unit = throw ConnectException("sockets aren't available on iOS yet")
    val inputStream: java.io.InputStream get() = throw SocketException("not connected")
    val outputStream: java.io.OutputStream get() = throw SocketException("not connected")
    fun getInputStream(): java.io.InputStream = inputStream
    fun getOutputStream(): java.io.OutputStream = outputStream
    var soTimeout = 0
    var tcpNoDelay = false
    var keepAlive = false
    val isConnected = false
    val isClosed = true
    val inetAddress: InetAddress? = null
    val localAddress: InetAddress? = null
    val port = 0
    val remoteSocketAddress: SocketAddress? = null
    fun setSoTimeout(t: Int) { soTimeout = t }
    fun shutdownOutput() {}
    fun shutdownInput() {}
    override fun close() {}
}

open class ServerSocket(port: Int = 0, backlog: Int = 50, bindAddr: InetAddress? = null) : java.io.Closeable {
    fun accept(): Socket = throw SocketException("server sockets aren't available on iOS yet")
    fun bind(a: SocketAddress?) {}
    val localPort = -1
    var soTimeout = 0
    var reuseAddress = false
    val isClosed = true
    override fun close() {}
}

class NetworkInterface private constructor(val name: String) {
    val inetAddresses: java.util.Enumeration<InetAddress> get() = java.util.Enumeration(emptyList())
    fun getInetAddresses(): java.util.Enumeration<InetAddress> = java.util.Enumeration(emptyList())
    val isUp = false
    val isLoopback = false
    val isVirtual = false
    val displayName get() = name
    companion object {
        fun getNetworkInterfaces(): java.util.Enumeration<NetworkInterface> = java.util.Enumeration(emptyList())
    }
}

class DatagramSocket(port: Int = 0) : java.io.Closeable { override fun close() {} }
