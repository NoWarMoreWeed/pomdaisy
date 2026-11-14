@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.util

import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.NSDate
import platform.Foundation.localTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.countryCode

class Locale(val language: String, val country: String = "", val variant: String = "") {
    override fun toString() = if (country.isEmpty()) language else "${language}_$country"
    fun toLanguageTag() = if (country.isEmpty()) language else "$language-$country"
    val displayLanguage: String get() = language
    val displayName: String get() = toString()
    fun getDisplayLanguage(l: Locale = this) = language
    val iSO3Language: String get() = language
    override fun equals(other: Any?) = other is Locale && other.language == language && other.country == country
    override fun hashCode() = language.hashCode() * 31 + country.hashCode()
    companion object {
        val ROOT = Locale("")
        val US = Locale("en", "US")
        val ENGLISH = Locale("en")
        val GERMAN = Locale("de")
        val GERMANY = Locale("de", "DE")
        val FRENCH = Locale("fr")
        val ITALIAN = Locale("it")
        val JAPANESE = Locale("ja")
        val KOREAN = Locale("ko")
        val UK = Locale("en", "GB")
        private var default: Locale? = null
        fun getDefault(): Locale = default ?: NSLocale.currentLocale.let { Locale(it.languageCode, it.countryCode ?: "") }
        fun setDefault(l: Locale) { default = l }
        fun forLanguageTag(tag: String): Locale { val p = tag.split('-', '_'); return Locale(p[0], p.getOrElse(1) { "" }) }
        fun getAvailableLocales(): Array<Locale> = arrayOf(US, GERMANY, UK)
    }
    class Builder { private var l = ""; private var r = ""; fun setLanguage(s: String) = apply { l = s }; fun setRegion(s: String) = apply { r = s }; fun build() = Locale(l, r) }
}

open class Random(seed: Long = kotlin.random.Random.nextLong()) {
    private var r = kotlin.random.Random(seed)
    fun setSeed(seed: Long) { r = kotlin.random.Random(seed) }
    fun nextInt() = r.nextInt()
    fun nextInt(bound: Int) = r.nextInt(bound)
    fun nextInt(from: Int, until: Int) = r.nextInt(from, until)
    fun nextLong() = r.nextLong()
    fun nextDouble() = r.nextDouble()
    fun nextFloat() = r.nextFloat()
    fun nextBoolean() = r.nextBoolean()
    fun nextBytes(b: ByteArray) { r.nextBytes(b) }
    fun nextGaussian(): Double {
        val u = r.nextDouble().coerceAtLeast(1e-300); val v = r.nextDouble()
        return kotlin.math.sqrt(-2 * kotlin.math.ln(u)) * kotlin.math.cos(2 * kotlin.math.PI * v)
    }
}

class Date(private val ms: Long = java.lang.System.currentTimeMillis()) : Comparable<Date> {
    val time: Long get() = ms
    fun getTime() = ms
    override fun compareTo(other: Date) = ms.compareTo(other.ms)
    override fun toString() = ms.toString()
    fun before(o: Date) = ms < o.ms
    fun after(o: Date) = ms > o.ms
}

class Properties(private val m: HashMap<Any, Any> = HashMap()) : MutableMap<Any, Any> by m {
    fun getProperty(key: String): String? = get(key) as String?
    fun getProperty(key: String, def: String): String = getProperty(key) ?: def
    fun setProperty(key: String, value: String): Any? = put(key, value)
    fun stringPropertyNames(): Set<String> = keys.map { it.toString() }.toSet()
    fun load(input: java.io.InputStream) = load(java.io.InputStreamReader(input))
    fun load(r: java.io.Reader) {
        for (raw in r.readAllText().lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue
            val i = line.indexOfFirst { it == '=' || it == ':' }
            if (i < 0) put(line, "") else put(line.substring(0, i).trim(), unescape(line.substring(i + 1).trim()))
        }
    }
    fun store(out: java.io.OutputStream, comments: String?) = store(java.io.OutputStreamWriter(out), comments)
    fun store(w: java.io.Writer, comments: String?) {
        if (comments != null) w.write("#$comments\n")
        for ((k, v) in entries) w.write("${escape(k.toString())}=${escape(v.toString())}\n")
        w.flush()
    }
    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=").replace(":", "\\:")
    private fun unescape(s: String): String {
        val sb = StringBuilder(); var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                sb.append(when (n) { 'n' -> '\n'; 't' -> '\t'; 'r' -> '\r'; else -> n })
                i += 2
            } else { sb.append(c); i++ }
        }
        return sb.toString()
    }
}

/** A binary heap. */
class PriorityQueue<E>(initialCapacity: Int = 11, private val cmp: Comparator<in E>? = null) : AbstractMutableCollection<E>() {
    constructor(cmp: Comparator<in E>) : this(11, cmp)
    private val heap = ArrayList<E>(initialCapacity)
    @Suppress("UNCHECKED_CAST")
    private fun less(a: E, b: E): Boolean = if (cmp != null) cmp.compare(a, b) < 0 else (a as Comparable<E>) < b
    override val size get() = heap.size
    override fun add(element: E): Boolean { offer(element); return true }
    fun offer(e: E): Boolean {
        heap.add(e); var i = heap.size - 1
        while (i > 0) { val p = (i - 1) / 2; if (less(heap[i], heap[p])) { val t = heap[i]; heap[i] = heap[p]; heap[p] = t; i = p } else break }
        return true
    }
    /** Non-null as Java's platform type reads (null when empty). */
    @Suppress("UNCHECKED_CAST") fun peek(): E = heap.firstOrNull() as E
    @Suppress("UNCHECKED_CAST") fun poll(): E {
        if (heap.isEmpty()) return null as E
        val top = heap[0]; val last = heap.removeAt(heap.size - 1)
        if (heap.isNotEmpty()) {
            heap[0] = last; var i = 0
            while (true) {
                val l = 2 * i + 1; val r = l + 1; var m = i
                if (l < heap.size && less(heap[l], heap[m])) m = l
                if (r < heap.size && less(heap[r], heap[m])) m = r
                if (m == i) break
                val t = heap[i]; heap[i] = heap[m]; heap[m] = t; i = m
            }
        }
        return top
    }
    fun remove(): E = if (heap.isEmpty()) throw NoSuchElementException() else poll()
    override fun iterator(): MutableIterator<E> = heap.iterator()
    override fun clear() = heap.clear()
}

/** java.util.TreeMap: a map whose keys iterate in order (sorted on read). */
class TreeMap<K, V>(private val cmp: Comparator<in K>? = null) : AbstractMutableMap<K, V>() {
    private val m = HashMap<K, V>()
    @Suppress("UNCHECKED_CAST")
    private fun sortedKeys(): List<K> = if (cmp != null) m.keys.sortedWith(cmp) else m.keys.sortedWith { a, b -> (a as Comparable<Any?>).compareTo(b) }
    override fun put(key: K, value: V): V? = m.put(key, value)
    override fun get(key: K): V? = m[key]
    override fun remove(key: K): V? = m.remove(key)
    override fun containsKey(key: K) = m.containsKey(key)
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size get() = m.size
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean { m[element.key] = element.value; return true }
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val keys = sortedKeys().iterator()
                var last: K? = null
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    override fun hasNext() = keys.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        val k = keys.next(); last = k
                        return object : MutableMap.MutableEntry<K, V> {
                            override val key = k
                            @Suppress("UNCHECKED_CAST") override val value get() = m[k] as V
                            override fun setValue(newValue: V): V { @Suppress("UNCHECKED_CAST") return m.put(k, newValue) as V }
                        }
                    }
                    @Suppress("UNCHECKED_CAST") override fun remove() { m.remove(last as K) }
                }
            }
        }
    fun firstKey(): K = sortedKeys().first()
    fun lastKey(): K = sortedKeys().last()
    fun firstEntry(): Map.Entry<K, V>? = entries.firstOrNull()
    fun lastEntry(): Map.Entry<K, V>? = entries.lastOrNull()
    fun floorKey(k: K): K? = sortedKeys().lastOrNull { compare(it, k) <= 0 }
    fun ceilingKey(k: K): K? = sortedKeys().firstOrNull { compare(it, k) >= 0 }
    fun floorEntry(k: K): Map.Entry<K, V>? = floorKey(k)?.let { key -> entries.first { it.key == key } }
    fun ceilingEntry(k: K): Map.Entry<K, V>? = ceilingKey(k)?.let { key -> entries.first { it.key == key } }
    fun headMap(k: K): Map<K, V> = sortedKeys().filter { compare(it, k) < 0 }.associateWith { m[it] as V }
    fun tailMap(k: K): Map<K, V> = sortedKeys().filter { compare(it, k) >= 0 }.associateWith { m[it] as V }
    @Suppress("UNCHECKED_CAST")
    private fun compare(a: K, b: K): Int = cmp?.compare(a, b) ?: (a as Comparable<Any?>).compareTo(b)
}

typealias SortedMap<K, V> = TreeMap<K, V>
typealias NavigableMap<K, V> = TreeMap<K, V>

class TreeSet<E>(private val cmp: Comparator<in E>? = null) : AbstractMutableSet<E>() {
    private val s = HashSet<E>()
    @Suppress("UNCHECKED_CAST")
    private fun sorted(): List<E> = if (cmp != null) s.sortedWith(cmp) else s.sortedWith { a, b -> (a as Comparable<Any?>).compareTo(b) }
    override val size get() = s.size
    override fun add(element: E) = s.add(element)
    override fun iterator(): MutableIterator<E> { val it = sorted().iterator(); var last: E? = null
        return object : MutableIterator<E> { override fun hasNext() = it.hasNext(); override fun next() = it.next().also { last = it }; @Suppress("UNCHECKED_CAST") override fun remove() { s.remove(last as E) } } }
    fun first(): E = sorted().first()
    fun last(): E = sorted().last()
}

object Objects {
    fun equals(a: Any?, b: Any?) = a == b
    fun hash(vararg values: Any?): Int = values.contentHashCode()
    fun hashCode(o: Any?): Int = o?.hashCode() ?: 0
    fun <T> requireNonNull(o: T?): T = o ?: throw NullPointerException()
    fun <T> requireNonNull(o: T?, msg: String): T = o ?: throw NullPointerException(msg)
    fun isNull(o: Any?) = o == null
    fun nonNull(o: Any?) = o != null
    fun toString(o: Any?) = o.toString()
    fun <T> requireNonNullElse(o: T?, d: T): T = o ?: d
}

object Arrays {
    fun fill(a: ByteArray, v: Byte) = a.fill(v)
    fun fill(a: ByteArray, from: Int, to: Int, v: Byte) = a.fill(v, from, to)
    fun fill(a: IntArray, v: Int) = a.fill(v)
    fun fill(a: IntArray, from: Int, to: Int, v: Int) = a.fill(v, from, to)
    fun fill(a: ShortArray, v: Short) = a.fill(v)
    fun fill(a: LongArray, v: Long) = a.fill(v)
    fun fill(a: BooleanArray, v: Boolean) = a.fill(v)
    fun fill(a: CharArray, v: Char) = a.fill(v)
    fun fill(a: FloatArray, v: Float) = a.fill(v)
    fun <T> fill(a: Array<T>, v: T) = a.fill(v)
    fun equals(a: ByteArray?, b: ByteArray?) = a.contentEquals(b)
    fun equals(a: IntArray?, b: IntArray?) = a.contentEquals(b)
    fun hashCode(a: ByteArray?) = a.contentHashCode()
    fun hashCode(a: IntArray?) = a.contentHashCode()
    fun copyOf(a: ByteArray, n: Int) = a.copyOf(n)
    fun copyOf(a: IntArray, n: Int) = a.copyOf(n)
    fun copyOfRange(a: ByteArray, from: Int, to: Int) = a.copyOfRange(from, to)
    fun copyOfRange(a: IntArray, from: Int, to: Int) = a.copyOfRange(from, to)
    fun sort(a: IntArray) = a.sort()
    fun sort(a: LongArray) = a.sort()
    fun toString(a: IntArray?) = a.contentToString()
    fun binarySearch(a: IntArray, key: Int) = a.asList().binarySearch(key)
    fun binarySearch(a: LongArray, key: Long) = a.asList().binarySearch(key)
    fun binarySearch(a: ShortArray, key: Short) = a.asList().binarySearch(key)
    fun binarySearch(a: CharArray, key: Char) = a.asList().binarySearch(key)
    fun <T : Comparable<T>> binarySearch(a: Array<T>, key: T) = a.asList().binarySearch(key)
    fun sort(a: ShortArray) = a.sort()
    fun sort(a: CharArray) = a.sort()
    fun <T> sort(a: Array<T>, c: Comparator<in T>) = a.sortWith(c)
    fun stream(a: IntArray) = a.asSequence()
    fun toString(a: ByteArray?) = a.contentToString()
    fun <T> asList(vararg a: T): List<T> = a.toList()
    fun <T> toString(a: Array<T>?) = a.contentToString()
}

object Collections {
    fun <T> emptyList(): List<T> = kotlin.collections.emptyList()
    fun <K, V> emptyMap(): Map<K, V> = kotlin.collections.emptyMap()
    fun <T> emptySet(): Set<T> = kotlin.collections.emptySet()
    fun <T> unmodifiableList(l: List<T>): List<T> = l
    fun <K, V> unmodifiableMap(m: Map<K, V>): Map<K, V> = m
    fun <T> unmodifiableSet(s: Set<T>): Set<T> = s
    fun <T> singletonList(t: T): List<T> = listOf(t)
    fun <T> synchronizedList(l: MutableList<T>): MutableList<T> = l
    fun <K, V> synchronizedMap(m: MutableMap<K, V>): MutableMap<K, V> = m
    fun <T> synchronizedSet(s: MutableSet<T>): MutableSet<T> = s
    fun <T> newSetFromMap(m: MutableMap<T, Boolean>): MutableSet<T> = HashSet()
    fun <T : Comparable<T>> sort(l: MutableList<T>) = l.sort()
    fun <T> sort(l: MutableList<T>, c: Comparator<in T>) = l.sortWith(c)
    fun reverse(l: MutableList<*>) = l.reverse()
    fun <T> swap(l: MutableList<T>, i: Int, j: Int) { val t = l[i]; l[i] = l[j]; l[j] = t }
    fun shuffle(l: MutableList<*>) = l.shuffle()
    fun <T> list(e: Enumeration<T>): ArrayList<T> = ArrayList<T>().also { while (e.hasMoreElements()) it.add(e.nextElement()) }
    fun <T> nCopies(n: Int, t: T): List<T> = List(n) { t }
    fun <T> reverseOrder(): Comparator<T> = @Suppress("UNCHECKED_CAST") (Comparator<Comparable<Any?>> { a, b -> b.compareTo(a) } as Comparator<T>)
}

class UUID(private val hi: Long, private val lo: Long) {
    override fun toString(): String {
        val h = hi.toULong().toString(16).padStart(16, '0'); val l = lo.toULong().toString(16).padStart(16, '0')
        return "${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12)}-${l.substring(0, 4)}-${l.substring(4)}"
    }
    companion object {
        fun randomUUID(): UUID = UUID((kotlin.random.Random.nextLong() and -0xf001L) or 0x4000L, (kotlin.random.Random.nextLong() and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE)
        fun fromString(s: String): UUID { val h = s.replace("-", ""); return UUID(h.substring(0, 16).toULong(16).toLong(), h.substring(16).toULong(16).toLong()) }
        fun nameUUIDFromBytes(b: ByteArray): UUID = randomUUID()
    }
}

class StringJoiner(private val sep: CharSequence, private val prefix: CharSequence = "", private val suffix: CharSequence = "") {
    private val parts = ArrayList<CharSequence>()
    fun add(s: CharSequence?): StringJoiner { parts.add(s ?: "null"); return this }
    override fun toString() = parts.joinToString(sep, prefix, suffix)
}

class Base64 private constructor() {
    class Encoder internal constructor(private val url: Boolean, private val pad: Boolean) {
        fun encodeToString(b: ByteArray): String = encode(b).decodeToString()
        fun encode(b: ByteArray): ByteArray {
            val enc = if (url) kotlin.io.encoding.Base64.UrlSafe else kotlin.io.encoding.Base64.Default
            var s = enc.encode(b)
            if (!pad) s = s.trimEnd('=')
            return s.encodeToByteArray()
        }
        fun withoutPadding() = Encoder(url, false)
    }
    class Decoder internal constructor(private val url: Boolean) {
        fun decode(s: String): ByteArray {
            val enc = (if (url) kotlin.io.encoding.Base64.UrlSafe else kotlin.io.encoding.Base64.Default).withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT_OPTIONAL)
            return enc.decode(s.trim())
        }
        fun decode(b: ByteArray) = decode(b.decodeToString())
    }
    companion object {
        fun getEncoder() = Encoder(false, true)
        fun getUrlEncoder() = Encoder(true, true)
        fun getDecoder() = Decoder(false)
        fun getUrlDecoder() = Decoder(true)
        fun getMimeDecoder() = Decoder(false)
    }
}

typealias ArrayDeque<E> = kotlin.collections.ArrayDeque<E>
typealias LinkedList<E> = kotlin.collections.ArrayDeque<E>
typealias EnumMap<K, V> = kotlin.collections.LinkedHashMap<K, V>
typealias Optional<T> = Opt<T>
class Opt<T>(private val v: T?) {
    fun isPresent() = v != null
    fun get(): T = v ?: throw NoSuchElementException()
    fun orElse(d: T?): T? = v ?: d
    companion object { fun <T> of(v: T) = Opt(v); fun <T> ofNullable(v: T?) = Opt(v); fun <T> empty() = Opt<T>(null) }
}

open class NoSuchElementException(message: String? = null) : kotlin.NoSuchElementException(message)
open class ConcurrentModificationException(message: String? = null) : kotlin.ConcurrentModificationException(message)
open class MissingResourceException(message: String?, cls: String?, key: String?) : RuntimeException(message)
open class Timer(name: String? = null, daemon: Boolean = true) {
    fun cancel() {}
}

class TimeZone private constructor(val id: String) {
    val rawOffset: Int get() = platform.Foundation.NSTimeZone.localTimeZone.secondsFromGMTForDate(platform.Foundation.NSDate()).toInt() * 1000
    fun getOffset(ms: Long): Int = rawOffset
    companion object {
        fun getDefault() = TimeZone(NSTimeZone.localTimeZone.name)
        fun getTimeZone(id: String) = TimeZone(id)
    }
}

class Calendar private constructor() {
    private var ms = java.lang.System.currentTimeMillis()
    var timeInMillis: Long get() = ms; set(v) { ms = v }
    var time: Date get() = Date(ms); set(v) { ms = v.time }
    fun get(field: Int): Int {
        val d = NSDate.dateWithTimeIntervalSince1970(ms / 1000.0)
        val c = platform.Foundation.NSCalendar.currentCalendar.components(
            platform.Foundation.NSCalendarUnitYear or platform.Foundation.NSCalendarUnitMonth or platform.Foundation.NSCalendarUnitDay or
                platform.Foundation.NSCalendarUnitHour or platform.Foundation.NSCalendarUnitMinute or platform.Foundation.NSCalendarUnitSecond or
                platform.Foundation.NSCalendarUnitWeekday, d)
        return when (field) {
            YEAR -> c.year.toInt(); MONTH -> c.month.toInt() - 1; DAY_OF_MONTH -> c.day.toInt(); HOUR_OF_DAY -> c.hour.toInt()
            MINUTE -> c.minute.toInt(); SECOND -> c.second.toInt(); DAY_OF_WEEK -> c.weekday.toInt(); else -> 0
        }
    }
    companion object {
        const val YEAR = 1; const val MONTH = 2; const val DAY_OF_MONTH = 5; const val DATE = 5; const val HOUR_OF_DAY = 11
        const val MINUTE = 12; const val SECOND = 13; const val DAY_OF_WEEK = 7; const val HOUR = 10
        fun getInstance() = Calendar()
        fun getInstance(tz: TimeZone) = Calendar()
    }
}

class Enumeration<E>(items: List<E>) : Iterator<E> {
    private val it = items.iterator()
    fun hasMoreElements() = it.hasNext()
    fun nextElement() = it.next()
    override fun hasNext() = it.hasNext()
    override fun next() = it.next()
    fun asIterator(): Iterator<E> = this
}

typealias IdentityHashMap<K, V> = HashMap<K, V>
typealias WeakHashMap<K, V> = HashMap<K, V>
typealias Hashtable<K, V> = HashMap<K, V>
typealias Vector<E> = ArrayList<E>
typealias Stack<E> = ArrayList<E>

/** java.util.LinkedHashMap with its access order and removeEldestEntry hook (open, unlike Kotlin/Native's). */
open class LinkedHashMap<K, V>(initialCapacity: Int = 16, loadFactor: Float = 0.75f, private val accessOrder: Boolean = false) : AbstractMutableMap<K, V>() {
    private val m = kotlin.collections.LinkedHashMap<K, V>()
    protected open fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = false
    override fun put(key: K, value: V): V? {
        val old = if (accessOrder) m.remove(key) else null
        val r = m.put(key, value) ?: old
        val eldest = m.entries.firstOrNull()
        if (eldest != null && removeEldestEntry(eldest)) m.remove(eldest.key)
        return r
    }
    override fun get(key: K): V? { val v = m[key]; if (accessOrder && v != null) { m.remove(key); m[key] = v }; return v }
    override fun remove(key: K): V? = m.remove(key)
    override fun containsKey(key: K) = m.containsKey(key)
    override fun clear() = m.clear()
    override val size get() = m.size
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = m.entries
}

class BitSet(nbits: Int = 64) {
    private var words = LongArray((nbits + 63) / 64)
    private fun ensure(i: Int) { val w = i / 64; if (w >= words.size) words = words.copyOf(maxOf(words.size * 2, w + 1)) }
    operator fun get(i: Int): Boolean { val w = i / 64; return w < words.size && (words[w] and (1L shl (i % 64))) != 0L }
    fun set(i: Int) { ensure(i); words[i / 64] = words[i / 64] or (1L shl (i % 64)) }
    fun set(i: Int, v: Boolean) = if (v) set(i) else clear(i)
    fun set(from: Int, to: Int) { for (i in from until to) set(i) }
    fun clear(i: Int) { val w = i / 64; if (w < words.size) words[w] = words[w] and (1L shl (i % 64)).inv() }
    fun clear() { words.fill(0) }
    fun cardinality(): Int = words.sumOf { it.countOneBits() }
    fun nextSetBit(from: Int): Int { var i = from; while (i < words.size * 64) { if (get(i)) return i; i++ }; return -1 }
    fun nextClearBit(from: Int): Int { var i = from; while (get(i)) i++; return i }
    fun length(): Int { for (w in words.indices.reversed()) if (words[w] != 0L) return w * 64 + 64 - words[w].countLeadingZeroBits(); return 0 }
    fun size() = words.size * 64
    val isEmpty: Boolean get() = words.all { it == 0L }
    fun isEmpty() = isEmpty
    fun or(o: BitSet) { if (o.words.size > words.size) words = words.copyOf(o.words.size); for (i in o.words.indices) words[i] = words[i] or o.words[i] }
    fun and(o: BitSet) { for (i in words.indices) words[i] = words[i] and (o.words.getOrElse(i) { 0L }) }
    fun toLongArray() = words.copyOf()
}
