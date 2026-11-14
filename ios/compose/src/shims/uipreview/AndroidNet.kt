package android.net

class Uri private constructor(private val s: String) {
    val scheme: String? get() = s.substringBefore(':', "").ifEmpty { null }
    val authority: String? get() = s.substringAfter("://", "").substringBefore('/').ifEmpty { null }
    val path: String? get() = "/" + s.substringAfter("://", "").substringAfter('/', "")
    val lastPathSegment: String? get() = path?.substringAfterLast('/')?.ifEmpty { null }

    val host: String? get() = authority?.substringBefore(':')
    val port: Int get() = authority?.substringAfter(':', "")?.toIntOrNull() ?: -1
    val query: String? get() = s.substringAfter('?', "").ifEmpty { null }
    val fragment: String? get() = s.substringAfter('#', "").ifEmpty { null }
    val pathSegments: List<String> get() = path?.split('/')?.filter { it.isNotEmpty() } ?: emptyList()
    val isAbsolute: Boolean get() = scheme != null
    fun getQueryParameter(k: String): String? = query?.split('&')?.firstOrNull { it.substringBefore('=') == k }?.substringAfter('=', "")?.let { java.net.URLDecoder.decode(it) }
    fun buildUpon() = Builder(s)
    class Builder(private var b: String = "") {
        fun scheme(x: String) = apply { b = "$x://" }
        fun authority(x: String) = apply { b += x }
        fun path(x: String) = apply { b += x }
        fun appendPath(x: String) = apply { b = b.trimEnd('/') + "/" + x }
        fun appendQueryParameter(k: String, v: String) = apply { b += (if ('?' in b) "&" else "?") + k + "=" + java.net.URLEncoder.encode(v) }
        fun build() = Uri(b)
    }
    override fun equals(other: Any?) = other is Uri && other.s == s
    override fun hashCode() = s.hashCode()
    companion object {
        fun parse(s: String) = Uri(s)
        fun fromFile(f: java.io.File) = Uri("file://" + f.absolutePath)
        fun encode(s: String?): String = java.net.URLEncoder.encode(s ?: "").replace("+", "%20")
        fun decode(s: String?): String = java.net.URLDecoder.decode(s ?: "")
        val EMPTY = Uri("")
    }

    override fun toString() = s
}
