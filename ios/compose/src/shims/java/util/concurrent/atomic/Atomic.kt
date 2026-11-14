@file:Suppress("PackageDirectoryMismatch", "unused")

package java.util.concurrent.atomic

class AtomicBoolean(initial: Boolean = false) {
    private val a = kotlin.concurrent.AtomicInt(if (initial) 1 else 0)
    fun get() = a.value != 0
    fun set(v: Boolean) { a.value = if (v) 1 else 0 }
    fun lazySet(v: Boolean) = set(v)
    fun getAndSet(v: Boolean) = a.getAndSet(if (v) 1 else 0) != 0
    fun compareAndSet(expect: Boolean, update: Boolean) = a.compareAndSet(if (expect) 1 else 0, if (update) 1 else 0)
    override fun toString() = get().toString()
}

class AtomicInteger(initial: Int = 0) : Number() {
    private val a = kotlin.concurrent.AtomicInt(initial)
    fun get() = a.value
    fun set(v: Int) { a.value = v }
    fun lazySet(v: Int) = set(v)
    fun getAndSet(v: Int) = a.getAndSet(v)
    fun incrementAndGet() = a.incrementAndGet()
    fun decrementAndGet() = a.decrementAndGet()
    fun getAndIncrement() = a.getAndIncrement()
    fun getAndDecrement() = a.getAndDecrement()
    fun addAndGet(d: Int) = a.addAndGet(d)
    fun getAndAdd(d: Int) = a.getAndAdd(d)
    fun compareAndSet(e: Int, u: Int) = a.compareAndSet(e, u)
    fun updateAndGet(f: (Int) -> Int): Int { while (true) { val v = a.value; val n = f(v); if (a.compareAndSet(v, n)) return n } }
    fun getAndUpdate(f: (Int) -> Int): Int { while (true) { val v = a.value; if (a.compareAndSet(v, f(v))) return v } }
    fun accumulateAndGet(x: Int, f: (Int, Int) -> Int): Int { while (true) { val v = a.value; val n = f(v, x); if (a.compareAndSet(v, n)) return n } }
    override fun toByte() = get().toByte()
    override fun toDouble() = get().toDouble()
    override fun toFloat() = get().toFloat()
    override fun toInt() = get()
    override fun toLong() = get().toLong()
    override fun toShort() = get().toShort()
    override fun toString() = get().toString()
}

class AtomicLong(initial: Long = 0) : Number() {
    private val a = kotlin.concurrent.AtomicLong(initial)
    fun get() = a.value
    fun set(v: Long) { a.value = v }
    fun lazySet(v: Long) = set(v)
    fun getAndSet(v: Long) = a.getAndSet(v)
    fun incrementAndGet() = a.incrementAndGet()
    fun decrementAndGet() = a.decrementAndGet()
    fun getAndIncrement() = a.getAndIncrement()
    fun getAndDecrement() = a.getAndDecrement()
    fun addAndGet(d: Long) = a.addAndGet(d)
    fun getAndAdd(d: Long) = a.getAndAdd(d)
    fun compareAndSet(e: Long, u: Long) = a.compareAndSet(e, u)
    fun updateAndGet(f: (Long) -> Long): Long { while (true) { val v = a.value; val n = f(v); if (a.compareAndSet(v, n)) return n } }
    fun accumulateAndGet(x: Long, f: (Long, Long) -> Long): Long { while (true) { val v = a.value; val n = f(v, x); if (a.compareAndSet(v, n)) return n } }
    override fun toByte() = get().toByte()
    override fun toDouble() = get().toDouble()
    override fun toFloat() = get().toFloat()
    override fun toInt() = get().toInt()
    override fun toLong() = get()
    override fun toShort() = get().toShort()
    override fun toString() = get().toString()
}

class AtomicReference<V>(initial: V) {
    constructor() : this(null as V)
    private val a = kotlin.concurrent.AtomicReference(initial)
    fun get(): V = a.value
    fun set(v: V) { a.value = v }
    fun lazySet(v: V) = set(v)
    fun getAndSet(v: V): V = a.getAndSet(v)
    fun compareAndSet(e: V, u: V) = a.compareAndSet(e, u)
    fun updateAndGet(f: (V) -> V): V { while (true) { val v = a.value; val n = f(v); if (a.compareAndSet(v, n)) return n } }
    fun getAndUpdate(f: (V) -> V): V { while (true) { val v = a.value; if (a.compareAndSet(v, f(v))) return v } }
    override fun toString() = get().toString()
}

class AtomicIntegerArray(n: Int) {
    private val a = Array(n) { kotlin.concurrent.AtomicInt(0) }
    fun get(i: Int) = a[i].value
    fun set(i: Int, v: Int) { a[i].value = v }
    fun incrementAndGet(i: Int) = a[i].incrementAndGet()
    fun length() = a.size
}
