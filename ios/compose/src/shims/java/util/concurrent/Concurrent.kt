@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package java.util.concurrent

import kotlin.PdMutex

/** A HashMap behind a lock; its views are copies (iteration never races a writer). */
open class ConcurrentHashMap<K, V>(initialCapacity: Int = 16) : AbstractMutableMap<K, V>() {
    private val m = HashMap<K, V>(initialCapacity)
    private val lock = PdMutex()
    private inline fun <R> locked(block: () -> R): R { lock.lock(); try { return block() } finally { lock.unlock() } }
    override val size: Int get() = locked { m.size }
    override fun isEmpty() = locked { m.isEmpty() }
    override fun get(key: K): V? = locked { m[key] }
    override fun containsKey(key: K) = locked { m.containsKey(key) }
    override fun containsValue(value: V) = locked { m.containsValue(value) }
    override fun put(key: K, value: V): V? = locked { m.put(key, value) }
    override fun remove(key: K): V? = locked { m.remove(key) }
    override fun clear() = locked { m.clear() }
    override fun putAll(from: Map<out K, V>) = locked { m.putAll(from) }
    fun putIfAbsent(key: K, value: V): V? = locked { m[key] ?: run { m[key] = value; null } }
    fun computeIfAbsent(key: K, f: (K) -> V): V {
        locked { m[key] }?.let { return it }
        val v = f(key)
        return locked { m[key] ?: v.also { m[key] = it } }
    }
    fun compute(key: K, f: (K, V?) -> V?): V? = locked { val v = f(key, m[key]); if (v == null) m.remove(key) else m[key] = v; v }
    fun merge(key: K, value: V, f: (V, V) -> V?): V? = locked { val old = m[key]; val v = if (old == null) value else f(old, value); if (v == null) m.remove(key) else m[key] = v; v }
    fun getOrDefault(key: K, d: V): V = locked { m[key] ?: d }
    fun remove(key: K, value: V): Boolean = locked { if (m[key] == value) { m.remove(key); true } else false }
    fun replace(key: K, value: V): V? = locked { if (m.containsKey(key)) m.put(key, value) else null }
    fun keys(): Iterator<K> = keys.iterator()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = locked { LinkedHashMap(m) }.let { snap ->
            object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
                override val size get() = snap.size
                override fun add(element: MutableMap.MutableEntry<K, V>): Boolean { put(element.key, element.value); return true }
                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                    val it = snap.entries.iterator()
                    var last: K? = null
                    return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                        override fun hasNext() = it.hasNext()
                        override fun next() = it.next().also { last = it.key }
                        @Suppress("UNCHECKED_CAST") override fun remove() { this@ConcurrentHashMap.remove(last as K) }
                    }
                }
            }
        }
    companion object {
        fun <K> newKeySet(): MutableSet<K> = ConcurrentSet()
    }
}

class ConcurrentSet<E> : AbstractMutableSet<E>() {
    private val m = ConcurrentHashMap<E, Boolean>()
    override val size get() = m.size
    override fun add(element: E) = m.put(element, true) == null
    override fun remove(element: E) = m.remove(element) != null
    override fun contains(element: E) = m.containsKey(element)
    override fun iterator(): MutableIterator<E> = m.keys.iterator()
}

typealias ConcurrentMap<K, V> = ConcurrentHashMap<K, V>
typealias CopyOnWriteArraySet<E> = ConcurrentSet<E>

open class CopyOnWriteArrayList<E>() : AbstractMutableList<E>() {
    constructor(items: Collection<E>) : this() { addAll(items) }
    @kotlin.concurrent.Volatile private var a: List<E> = emptyList()
    private val lock = PdMutex()
    override val size get() = a.size
    override fun get(index: Int) = a[index]
    override fun add(index: Int, element: E) { lock.lock(); try { a = a.toMutableList().also { it.add(index, element) } } finally { lock.unlock() } }
    override fun removeAt(index: Int): E { lock.lock(); try { val l = a.toMutableList(); val r = l.removeAt(index); a = l; return r } finally { lock.unlock() } }
    override fun set(index: Int, element: E): E { lock.lock(); try { val l = a.toMutableList(); val r = l.set(index, element); a = l; return r } finally { lock.unlock() } }
    override fun iterator(): MutableIterator<E> = a.toMutableList().iterator()
}

class ConcurrentLinkedQueue<E> : AbstractMutableCollection<E>() {
    private val q = ArrayDeque<E>()
    private val lock = PdMutex()
    private inline fun <R> locked(b: () -> R): R { lock.lock(); try { return b() } finally { lock.unlock() } }
    override val size get() = locked { q.size }
    override fun add(element: E) = locked { q.addLast(element); true }
    fun offer(e: E) = add(e)
    fun poll(): E? = locked { q.removeFirstOrNull() }
    fun peek(): E? = locked { q.firstOrNull() }
    override fun clear() = locked { q.clear() }
    override fun iterator(): MutableIterator<E> = locked { q.toMutableList() }.iterator()
}
typealias ConcurrentLinkedDeque<E> = ConcurrentLinkedQueue<E>

/** A blocking FIFO (poll with a timeout spins on a short sleep). */
open class LinkedBlockingQueue<E>(private val capacity: Int = Int.MAX_VALUE) : AbstractMutableCollection<E>() {
    private val q = ArrayDeque<E>()
    private val lock = PdMutex()
    private inline fun <R> locked(b: () -> R): R { lock.lock(); try { return b() } finally { lock.unlock() } }
    override val size get() = locked { q.size }
    override fun add(element: E): Boolean { if (!offer(element)) throw IllegalStateException("full"); return true }
    fun offer(e: E): Boolean = locked { if (q.size >= capacity) false else { q.addLast(e); true } }
    fun offer(e: E, timeout: Long, unit: TimeUnit): Boolean = offer(e)
    fun put(e: E) { while (!offer(e)) java.lang.Thread.sleep(1) }
    fun poll(): E? = locked { q.removeFirstOrNull() }
    fun poll(timeout: Long, unit: TimeUnit): E? {
        val end = java.lang.System.currentTimeMillis() + unit.toMillis(timeout)
        while (true) { poll()?.let { return it }; if (java.lang.System.currentTimeMillis() >= end) return null; java.lang.Thread.sleep(1) }
    }
    fun take(): E { while (true) { poll()?.let { return it }; java.lang.Thread.sleep(1) } }
    fun peek(): E? = locked { q.firstOrNull() }
    fun drainTo(c: MutableCollection<in E>): Int = locked { val n = q.size; c.addAll(q); q.clear(); n }
    fun remainingCapacity() = capacity - size
    override fun clear() = locked { q.clear() }
    override fun iterator(): MutableIterator<E> = locked { q.toMutableList() }.iterator()
}
typealias ArrayBlockingQueue<E> = LinkedBlockingQueue<E>
typealias BlockingQueue<E> = LinkedBlockingQueue<E>
typealias LinkedBlockingDeque<E> = LinkedBlockingQueue<E>

enum class TimeUnit(private val nanos: Long) {
    NANOSECONDS(1), MICROSECONDS(1_000), MILLISECONDS(1_000_000), SECONDS(1_000_000_000), MINUTES(60_000_000_000), HOURS(3_600_000_000_000), DAYS(86_400_000_000_000);
    fun toMillis(d: Long) = d * nanos / 1_000_000
    fun toNanos(d: Long) = d * nanos
    fun toMicros(d: Long) = d * nanos / 1_000
    fun toSeconds(d: Long) = d * nanos / 1_000_000_000
    fun toMinutes(d: Long) = d * nanos / 60_000_000_000
    fun toHours(d: Long) = d * nanos / 3_600_000_000_000
    fun toDays(d: Long) = d * nanos / 86_400_000_000_000
    fun sleep(d: Long) = java.lang.Thread.sleep(toMillis(d))
    fun convert(d: Long, u: TimeUnit) = d * u.nanos / nanos
}

fun interface Executor { fun execute(command: java.lang.Runnable) }
fun interface Callable<V> { fun call(): V }
fun interface ThreadFactory { fun newThread(r: java.lang.Runnable): java.lang.Thread }
open class ExecutionException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
open class TimeoutException(message: String? = null) : Exception(message)
open class CancellationException(message: String? = null) : IllegalStateException(message)
open class RejectedExecutionException(message: String? = null) : RuntimeException(message)

interface Future<V> {
    fun get(): V
    fun get(timeout: Long, unit: TimeUnit): V
    fun cancel(mayInterrupt: Boolean): Boolean
    val isDone: Boolean
    val isCancelled: Boolean
}
interface ScheduledFuture<V> : Future<V>, Delayed
interface Delayed { fun getDelay(unit: TimeUnit): Long }

class FutureTask<V>(private val c: Callable<V>) : java.lang.Runnable, ScheduledFuture<V> {
    constructor(r: java.lang.Runnable, result: V) : this(Callable { r.run(); result })
    @kotlin.concurrent.Volatile private var done = false
    @kotlin.concurrent.Volatile private var cancelled = false
    private var value: V? = null
    private var error: Throwable? = null
    override fun run() {
        if (cancelled || done) return
        try { value = c.call() } catch (e: Throwable) { error = e }
        done = true
    }
    @Suppress("UNCHECKED_CAST")
    override fun get(): V {
        while (!done) { if (cancelled) throw CancellationException(); java.lang.Thread.sleep(1) }
        error?.let { throw ExecutionException(it.message, it) }
        return value as V
    }
    @Suppress("UNCHECKED_CAST")
    override fun get(timeout: Long, unit: TimeUnit): V {
        val end = java.lang.System.currentTimeMillis() + unit.toMillis(timeout)
        while (!done) { if (cancelled) throw CancellationException(); if (java.lang.System.currentTimeMillis() >= end) throw TimeoutException(); java.lang.Thread.sleep(1) }
        error?.let { throw ExecutionException(it.message, it) }
        return value as V
    }
    override fun cancel(mayInterrupt: Boolean): Boolean { if (done) return false; cancelled = true; return true }
    override val isDone get() = done || cancelled
    override val isCancelled get() = cancelled
    override fun getDelay(unit: TimeUnit) = 0L
}

interface ExecutorService : Executor {
    fun shutdown()
    fun shutdownNow(): List<java.lang.Runnable>
    val isShutdown: Boolean
    val isTerminated: Boolean
    fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean
    fun <T> submit(task: Callable<T>): Future<T>
    fun submit(task: java.lang.Runnable): Future<*>
}

interface ScheduledExecutorService : ExecutorService {
    fun schedule(command: java.lang.Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*>
    fun scheduleAtFixedRate(command: java.lang.Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture<*>
    fun scheduleWithFixedDelay(command: java.lang.Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*>
}

/** Worker threads taking jobs from one queue (fixed count; "cached" = up to 8). */
open class ThreadPoolExecutor(private val threads: Int, private val factory: ThreadFactory? = null) : ScheduledExecutorService {
    private val queue = LinkedBlockingQueue<java.lang.Runnable>()
    @kotlin.concurrent.Volatile private var stopped = false
    @kotlin.concurrent.Volatile private var running = 0
    private val workers = ArrayList<java.lang.Thread>()
    private val lock = PdMutex()

    private fun ensureWorkers() {
        lock.lock()
        try {
            if (workers.size >= threads) return
            val loop = java.lang.Runnable {
                while (!stopped) {
                    val job = queue.poll(50, TimeUnit.MILLISECONDS) ?: continue
                    running++
                    try { job.run() } catch (e: Throwable) { e.printStackTrace() } finally { running-- }
                }
            }
            val t = factory?.newThread(loop) ?: java.lang.Thread(loop, "pool")
            workers.add(t)
            t.start()
        } finally { lock.unlock() }
    }
    override fun execute(command: java.lang.Runnable) {
        if (stopped) throw RejectedExecutionException()
        queue.offer(command)
        ensureWorkers()
    }
    override fun shutdown() { stopped = true }
    override fun shutdownNow(): List<java.lang.Runnable> { stopped = true; val l = ArrayList<java.lang.Runnable>(); queue.drainTo(l); return l }
    override val isShutdown get() = stopped
    override val isTerminated get() = stopped && running == 0
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        val end = java.lang.System.currentTimeMillis() + unit.toMillis(timeout)
        while (running > 0 || queue.size > 0) { if (java.lang.System.currentTimeMillis() >= end) return false; java.lang.Thread.sleep(2) }
        return true
    }
    override fun <T> submit(task: Callable<T>): Future<T> = FutureTask(task).also { execute(it) }
    override fun submit(task: java.lang.Runnable): Future<*> = FutureTask(task, Unit).also { execute(it) }
    override fun schedule(command: java.lang.Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        val f = FutureTask(command, Unit)
        execute { java.lang.Thread.sleep(unit.toMillis(delay)); f.run() }
        return f
    }
    override fun scheduleAtFixedRate(command: java.lang.Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture<*> =
        scheduleWithFixedDelay(command, initialDelay, period, unit)
    override fun scheduleWithFixedDelay(command: java.lang.Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        val f = FutureTask(command, Unit)
        java.lang.Thread({
            java.lang.Thread.sleep(unit.toMillis(initialDelay))
            while (!stopped && !f.isCancelled) { try { command.run() } catch (e: Throwable) { e.printStackTrace() }; java.lang.Thread.sleep(unit.toMillis(delay)) }
        }).start()
        return f
    }
}
typealias ScheduledThreadPoolExecutor = ThreadPoolExecutor

object Executors {
    fun newSingleThreadExecutor(): ExecutorService = ThreadPoolExecutor(1)
    fun newSingleThreadExecutor(f: ThreadFactory): ExecutorService = ThreadPoolExecutor(1, f)
    fun newFixedThreadPool(n: Int): ExecutorService = ThreadPoolExecutor(n)
    fun newFixedThreadPool(n: Int, f: ThreadFactory): ExecutorService = ThreadPoolExecutor(n, f)
    fun newCachedThreadPool(): ExecutorService = ThreadPoolExecutor(8)
    fun newCachedThreadPool(f: ThreadFactory): ExecutorService = ThreadPoolExecutor(8, f)
    fun newSingleThreadScheduledExecutor(): ScheduledExecutorService = ThreadPoolExecutor(1)
    fun newSingleThreadScheduledExecutor(f: ThreadFactory): ScheduledExecutorService = ThreadPoolExecutor(1, f)
    fun newScheduledThreadPool(n: Int): ScheduledExecutorService = ThreadPoolExecutor(n)
    fun newWorkStealingPool(): ExecutorService = ThreadPoolExecutor(4)
    fun <T> callable(r: java.lang.Runnable, result: T): Callable<T> = Callable { r.run(); result }
}

class CountDownLatch(count: Int) {
    private val n = kotlin.concurrent.AtomicInt(count)
    fun countDown() { if (n.value > 0) n.decrementAndGet() }
    val count: Long get() = n.value.toLong()
    fun await() { while (n.value > 0) java.lang.Thread.sleep(1) }
    fun await(timeout: Long, unit: TimeUnit): Boolean {
        val end = java.lang.System.currentTimeMillis() + unit.toMillis(timeout)
        while (n.value > 0) { if (java.lang.System.currentTimeMillis() >= end) return false; java.lang.Thread.sleep(1) }
        return true
    }
}

class Semaphore(permits: Int) {
    private val n = kotlin.concurrent.AtomicInt(permits)
    fun acquire() { while (true) { val v = n.value; if (v > 0 && n.compareAndSet(v, v - 1)) return; java.lang.Thread.sleep(1) } }
    fun tryAcquire(): Boolean { val v = n.value; return v > 0 && n.compareAndSet(v, v - 1) }
    fun release() { n.incrementAndGet() }
    fun availablePermits() = n.value
}

open class CompletableFuture<T> : Future<T> {
    @kotlin.concurrent.Volatile private var done = false
    private var value: T? = null
    private var error: Throwable? = null
    fun complete(v: T): Boolean { if (done) return false; value = v; done = true; return true }
    fun completeExceptionally(e: Throwable): Boolean { if (done) return false; error = e; done = true; return true }
    @Suppress("UNCHECKED_CAST") override fun get(): T { while (!done) java.lang.Thread.sleep(1); error?.let { throw ExecutionException(it.message, it) }; return value as T }
    @Suppress("UNCHECKED_CAST") override fun get(timeout: Long, unit: TimeUnit): T {
        val end = java.lang.System.currentTimeMillis() + unit.toMillis(timeout)
        while (!done) { if (java.lang.System.currentTimeMillis() >= end) throw TimeoutException(); java.lang.Thread.sleep(1) }
        error?.let { throw ExecutionException(it.message, it) }; return value as T
    }
    override fun cancel(mayInterrupt: Boolean) = completeExceptionally(CancellationException())
    override val isDone get() = done
    override val isCancelled get() = error is CancellationException
    fun join(): T = get()
    companion object {
        fun <T> completedFuture(v: T) = CompletableFuture<T>().also { it.complete(v) }
        fun <T> supplyAsync(f: () -> T): CompletableFuture<T> { val c = CompletableFuture<T>(); java.lang.Thread({ try { c.complete(f()) } catch (e: Throwable) { c.completeExceptionally(e) } }).start(); return c }
    }
}

object ThreadLocalRandom {
    fun current(): java.util.Random = java.util.Random()
}
