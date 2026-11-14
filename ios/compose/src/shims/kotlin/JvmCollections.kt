// The java.util.Map / List default methods (and a few JVM-only kotlin.collections functions) the app calls.
@file:Suppress("PackageDirectoryMismatch", "unused", "NOTHING_TO_INLINE")

package kotlin.collections

fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V): V? = get(key) ?: run { put(key, value); null }
inline fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, f: (K) -> V): V = get(key) ?: f(key).also { if (it != null) put(key, it) }
inline fun <K, V> MutableMap<K, V>.computeIfPresent(key: K, f: (K, V) -> V?): V? {
    val old = get(key) ?: return null
    val v = f(key, old)
    if (v == null) remove(key) else put(key, v)
    return v
}
inline fun <K, V> MutableMap<K, V>.compute(key: K, f: (K, V?) -> V?): V? { val v = f(key, get(key)); if (v == null) remove(key) else put(key, v); return v }
inline fun <K, V> MutableMap<K, V>.merge(key: K, value: V, f: (V, V) -> V?): V? {
    val old = get(key); val v = if (old == null) value else f(old, value)
    if (v == null) remove(key) else put(key, v)
    return v
}
fun <K, V> Map<out K, V>.getOrDefault(key: K, defaultValue: V): V = get(key) ?: defaultValue
inline fun <K, V> Map<out K, V>.forEach(action: (K, V) -> Unit) { for ((k, v) in this) action(k, v) }
fun <K, V> MutableMap<K, V>.remove(key: K, value: V): Boolean = if (get(key) == value && containsKey(key)) { remove(key); true } else false
fun <K, V> MutableMap<K, V>.replace(key: K, value: V): V? = if (containsKey(key)) put(key, value) else null
inline fun <K, V> MutableMap<K, V>.replaceAll(f: (K, V) -> V) { for (e in entries) e.setValue(f(e.key, e.value)) }
inline fun <T> MutableCollection<T>.removeIf(crossinline predicate: (T) -> Boolean): Boolean = removeAll { predicate(it) }
inline fun <T> MutableList<T>.replaceAll(f: (T) -> T) { for (i in indices) this[i] = f(this[i]) }
fun <T> MutableList<T>.sort(c: Comparator<in T>?) { if (c != null) sortWith(c) else @Suppress("UNCHECKED_CAST") (this as MutableList<Comparable<Any>>).sort() }
fun <K : Comparable<K>, V> sortedMapOf(vararg pairs: Pair<K, V>): java.util.TreeMap<K, V> = java.util.TreeMap<K, V>().also { m -> pairs.forEach { m[it.first] = it.second } }
fun <K : Comparable<K>, V> Map<out K, V>.toSortedMap(): java.util.TreeMap<K, V> = java.util.TreeMap<K, V>().also { it.putAll(this) }
fun <K, V> Map<out K, V>.toSortedMap(c: Comparator<in K>): java.util.TreeMap<K, V> = java.util.TreeMap<K, V>(c).also { it.putAll(this) }
fun <T : Comparable<T>> sortedSetOf(vararg e: T): java.util.TreeSet<T> = java.util.TreeSet<T>().also { s -> e.forEach { s.add(it) } }
fun <T> Iterable<T>.toSortedSet(): java.util.TreeSet<T> = java.util.TreeSet<T>().also { s -> forEach { s.add(it) } }
fun <E> kotlin.collections.ArrayDeque<E>.poll(): E? = removeFirstOrNull()
fun <E> kotlin.collections.ArrayDeque<E>.peek(): E? = firstOrNull()
fun <E> kotlin.collections.ArrayDeque<E>.offer(e: E): Boolean { addLast(e); return true }
fun <E> kotlin.collections.ArrayDeque<E>.push(e: E) = addFirst(e)
fun <E> kotlin.collections.ArrayDeque<E>.pop(): E = removeFirst()
fun <E> kotlin.collections.ArrayDeque<E>.pollFirst(): E? = removeFirstOrNull()
fun <E> kotlin.collections.ArrayDeque<E>.pollLast(): E? = removeLastOrNull()
fun <E> kotlin.collections.ArrayDeque<E>.peekFirst(): E? = firstOrNull()
fun <E> kotlin.collections.ArrayDeque<E>.peekLast(): E? = lastOrNull()
fun <E> kotlin.collections.ArrayDeque<E>.offerLast(e: E): Boolean { addLast(e); return true }
fun <E> kotlin.collections.ArrayDeque<E>.offerFirst(e: E): Boolean { addFirst(e); return true }
fun <E> kotlin.collections.ArrayDeque<E>.element(): E = first()
fun <E> kotlin.collections.ArrayDeque<E>.remove(): E = removeFirst()
fun <E> kotlin.collections.ArrayDeque<E>.descendingIterator(): Iterator<E> = asReversed().iterator()

fun <T> java.util.Enumeration<T>.toList(): List<T> = java.util.Collections.list(this)
fun <T> java.util.Enumeration<T>.asSequence(): Sequence<T> = java.util.Collections.list(this).asSequence()
fun <T> java.util.Enumeration<T>.iterator(): Iterator<T> = this
