// org.json as Android ships it: JSONObject / JSONArray with a parser and writer of their own (insertion order kept).
@file:Suppress("PackageDirectoryMismatch", "unused")

package org.json

open class JSONException(message: String?) : RuntimeException(message)

class JSONObject() {
    private val map = LinkedHashMap<String, Any?>()

    constructor(json: String) : this() {
        val v = JSONTokener(json).nextValue()
        if (v !is JSONObject) throw JSONException("Not a JSON object")
        map.putAll(v.map)
    }
    constructor(tokener: JSONTokener) : this() {
        val v = tokener.nextValue()
        if (v !is JSONObject) throw JSONException("Not a JSON object")
        map.putAll(v.map)
    }
    constructor(m: Map<*, *>) : this() { for ((k, v) in m) map[k.toString()] = wrap(v) }

    fun length() = map.size
    fun has(name: String) = map.containsKey(name)
    fun isNull(name: String) = map[name].let { it == null || it == NULL }
    fun keys(): MutableIterator<String> = map.keys.toMutableList().iterator()
    fun keySet(): Set<String> = map.keys
    fun names(): JSONArray? = if (map.isEmpty()) null else JSONArray(map.keys.toList())
    fun remove(name: String): Any? = map.remove(name)

    fun put(name: String, value: Any?): JSONObject { if (value == null) map.remove(name) else map[name] = checkNum(value); return this }
    fun put(name: String, value: Boolean): JSONObject { map[name] = value; return this }
    fun put(name: String, value: Int): JSONObject { map[name] = value; return this }
    fun put(name: String, value: Long): JSONObject { map[name] = value; return this }
    fun put(name: String, value: Double): JSONObject { map[name] = checkNum(value); return this }
    fun putOpt(name: String?, value: Any?): JSONObject { if (name != null && value != null) put(name, value); return this }
    fun accumulate(name: String, value: Any?): JSONObject {
        val cur = map[name]
        when (cur) { null -> put(name, value); is JSONArray -> cur.put(value); else -> map[name] = JSONArray().put(cur).put(value) }
        return this
    }

    fun opt(name: String): Any? = map[name]
    fun get(name: String): Any = map[name] ?: throw JSONException("No value for $name")
    fun getString(name: String): String = toStr(get(name)) ?: throw JSONException("Value for $name is not a string")
    fun getInt(name: String): Int = toNum(get(name))?.toInt() ?: throw JSONException("Value for $name is not an int")
    fun getLong(name: String): Long = toNum(get(name))?.toLong() ?: throw JSONException("Value for $name is not a long")
    fun getDouble(name: String): Double = toNum(get(name))?.toDouble() ?: throw JSONException("Value for $name is not a double")
    fun getBoolean(name: String): Boolean = toBool(get(name)) ?: throw JSONException("Value for $name is not a boolean")
    fun getJSONObject(name: String): JSONObject = get(name) as? JSONObject ?: throw JSONException("Value for $name is not a JSONObject")
    fun getJSONArray(name: String): JSONArray = get(name) as? JSONArray ?: throw JSONException("Value for $name is not a JSONArray")

    fun optString(name: String): String = optString(name, "")
    fun optString(name: String, fallback: String?): String = map[name].let { if (it == null || it == NULL) fallback ?: "" else toStr(it) ?: fallback ?: "" }
    fun optInt(name: String, fallback: Int = 0): Int = toNum(map[name])?.toInt() ?: fallback
    fun optLong(name: String, fallback: Long = 0L): Long = toNum(map[name])?.toLong() ?: fallback
    fun optDouble(name: String, fallback: Double = Double.NaN): Double = toNum(map[name])?.toDouble() ?: fallback
    fun optBoolean(name: String, fallback: Boolean = false): Boolean = toBool(map[name]) ?: fallback
    fun optJSONObject(name: String): JSONObject? = map[name] as? JSONObject
    fun optJSONArray(name: String): JSONArray? = map[name] as? JSONArray

    override fun toString(): String = StringBuilder().also { write(it, -1, 0) }.toString()
    fun toString(indent: Int): String = StringBuilder().also { write(it, indent, 0) }.toString()
    internal fun write(sb: StringBuilder, indent: Int, level: Int) {
        if (map.isEmpty()) { sb.append("{}"); return }
        sb.append('{')
        var first = true
        for ((k, v) in map) {
            if (!first) sb.append(',')
            first = false
            newline(sb, indent, level + 1)
            sb.append(quote(k)).append(if (indent >= 0) ": " else ":")
            writeValue(sb, v, indent, level + 1)
        }
        newline(sb, indent, level)
        sb.append('}')
    }

    companion object {
        val NULL: Any = object { override fun toString() = "null"; override fun equals(other: Any?) = other == null || other === this; override fun hashCode() = 0 }
        fun quote(s: String?): String {
            if (s == null) return "\"\""
            val sb = StringBuilder("\"")
            for (c in s) when (c) {
                '"' -> sb.append("\\\""); '\\' -> sb.append("\\\\"); '/' -> sb.append("\\/")
                '\n' -> sb.append("\\n"); '\r' -> sb.append("\\r"); '\t' -> sb.append("\\t"); '\b' -> sb.append("\\b"); '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
            }
            return sb.append('"').toString()
        }
        fun wrap(o: Any?): Any? = when (o) {
            null -> NULL
            is JSONObject, is JSONArray, is String, is Boolean, is Number -> o
            is Map<*, *> -> JSONObject(o)
            is Collection<*> -> JSONArray(o)
            is Array<*> -> JSONArray(o.toList())
            is IntArray -> JSONArray(o.toList())
            is Char -> o.toString()
            else -> o.toString()
        }
        fun numberToString(n: Number): String = numStr(n)
        internal fun numStr(n: Number): String = when (n) {
            is Double -> if (n == kotlin.math.floor(n) && !n.isInfinite() && kotlin.math.abs(n) < 1e15) n.toLong().toString() else n.toString()
            is Float -> numStr(n.toDouble())
            else -> n.toString()
        }
        internal fun checkNum(v: Any): Any { if (v is Double && (v.isNaN() || v.isInfinite())) throw JSONException("Forbidden numeric value: $v"); return v }
        internal fun toStr(v: Any?): String? = when (v) { null -> null; is String -> v; NULL -> "null"; is Number -> numStr(v); else -> v.toString() }
        internal fun toNum(v: Any?): Number? = when (v) { is Number -> v; is String -> v.toLongOrNull() ?: v.toDoubleOrNull(); else -> null }
        internal fun toBool(v: Any?): Boolean? = when (v) { is Boolean -> v; is String -> when (v.lowercase()) { "true" -> true; "false" -> false; else -> null }; else -> null }
        internal fun newline(sb: StringBuilder, indent: Int, level: Int) { if (indent >= 0) { sb.append('\n'); repeat(indent * level) { sb.append(' ') } } }
        internal fun writeValue(sb: StringBuilder, v: Any?, indent: Int, level: Int) {
            when (v) {
                null, NULL -> sb.append("null")
                is JSONObject -> v.write(sb, indent, level)
                is JSONArray -> v.write(sb, indent, level)
                is String -> sb.append(quote(v))
                is Number -> sb.append(numStr(v))
                is Boolean -> sb.append(v)
                else -> sb.append(quote(v.toString()))
            }
        }
    }
}

class JSONArray() : Iterable<Any?> {
    private val list = ArrayList<Any?>()
    constructor(json: String) : this() {
        val v = JSONTokener(json).nextValue()
        if (v !is JSONArray) throw JSONException("Not a JSON array")
        list.addAll(v.list)
    }
    constructor(tokener: JSONTokener) : this() {
        val v = tokener.nextValue()
        if (v !is JSONArray) throw JSONException("Not a JSON array")
        list.addAll(v.list)
    }
    constructor(c: Collection<*>) : this() { for (v in c) list.add(JSONObject.wrap(v)) }

    fun length() = list.size
    override fun iterator(): Iterator<Any?> = list.iterator()
    fun put(v: Any?): JSONArray { list.add(v ?: JSONObject.NULL); return this }
    fun put(v: Int): JSONArray { list.add(v); return this }
    fun put(v: Long): JSONArray { list.add(v); return this }
    fun put(v: Double): JSONArray { list.add(JSONObject.checkNum(v)); return this }
    fun put(v: Boolean): JSONArray { list.add(v); return this }
    fun put(index: Int, v: Any?): JSONArray { while (list.size <= index) list.add(JSONObject.NULL); list[index] = v ?: JSONObject.NULL; return this }
    fun remove(index: Int): Any? = if (index in list.indices) list.removeAt(index) else null
    fun isNull(i: Int) = list.getOrNull(i).let { it == null || it == JSONObject.NULL }
    fun opt(i: Int): Any? = list.getOrNull(i)
    fun get(i: Int): Any = list.getOrNull(i) ?: throw JSONException("Index $i out of range")
    fun getString(i: Int): String = JSONObject.toStr(get(i)) ?: throw JSONException("not a string")
    fun getInt(i: Int): Int = JSONObject.toNum(get(i))?.toInt() ?: throw JSONException("not an int")
    fun getLong(i: Int): Long = JSONObject.toNum(get(i))?.toLong() ?: throw JSONException("not a long")
    fun getDouble(i: Int): Double = JSONObject.toNum(get(i))?.toDouble() ?: throw JSONException("not a double")
    fun getBoolean(i: Int): Boolean = JSONObject.toBool(get(i)) ?: throw JSONException("not a boolean")
    fun getJSONObject(i: Int): JSONObject = get(i) as? JSONObject ?: throw JSONException("not a JSONObject")
    fun getJSONArray(i: Int): JSONArray = get(i) as? JSONArray ?: throw JSONException("not a JSONArray")
    fun optString(i: Int, fallback: String = ""): String = list.getOrNull(i).let { if (it == null || it == JSONObject.NULL) fallback else JSONObject.toStr(it) ?: fallback }
    fun optInt(i: Int, fallback: Int = 0): Int = JSONObject.toNum(list.getOrNull(i))?.toInt() ?: fallback
    fun optLong(i: Int, fallback: Long = 0L): Long = JSONObject.toNum(list.getOrNull(i))?.toLong() ?: fallback
    fun optDouble(i: Int, fallback: Double = Double.NaN): Double = JSONObject.toNum(list.getOrNull(i))?.toDouble() ?: fallback
    fun optBoolean(i: Int, fallback: Boolean = false): Boolean = JSONObject.toBool(list.getOrNull(i)) ?: fallback
    fun optJSONObject(i: Int): JSONObject? = list.getOrNull(i) as? JSONObject
    fun optJSONArray(i: Int): JSONArray? = list.getOrNull(i) as? JSONArray
    fun join(sep: String): String = list.joinToString(sep) { StringBuilder().also { sb -> JSONObject.writeValue(sb, it, -1, 0) }.toString() }

    override fun toString(): String = StringBuilder().also { write(it, -1, 0) }.toString()
    fun toString(indent: Int): String = StringBuilder().also { write(it, indent, 0) }.toString()
    internal fun write(sb: StringBuilder, indent: Int, level: Int) {
        if (list.isEmpty()) { sb.append("[]"); return }
        sb.append('[')
        for ((i, v) in list.withIndex()) {
            if (i > 0) sb.append(',')
            JSONObject.newline(sb, indent, level + 1)
            JSONObject.writeValue(sb, v, indent, level + 1)
        }
        JSONObject.newline(sb, indent, level)
        sb.append(']')
    }
}

class JSONTokener(private val s: String) {
    private var i = 0
    private fun skip() { while (i < s.length && (s[i].isWhitespace() || s[i] == '﻿')) i++ }
    fun more(): Boolean { skip(); return i < s.length }
    fun nextValue(): Any? {
        skip()
        if (i >= s.length) throw JSONException("End of input")
        return when (val c = s[i]) {
            '{' -> obj()
            '[' -> arr()
            '"', '\'' -> str(c)
            else -> literal()
        }
    }
    private fun obj(): JSONObject {
        val o = JSONObject(); i++
        skip()
        if (i < s.length && s[i] == '}') { i++; return o }
        while (true) {
            skip()
            val k = nextValue().toString()
            skip()
            if (i >= s.length || (s[i] != ':' && s[i] != '=')) throw JSONException("Expected ':' at $i")
            i++
            if (i < s.length && s[i] == '>') i++
            o.put(k, nextValue() ?: JSONObject.NULL)
            skip()
            if (i >= s.length) throw JSONException("Unterminated object")
            when (s[i]) { ',', ';' -> { i++; skip(); if (i < s.length && s[i] == '}') { i++; return o } }; '}' -> { i++; return o }; else -> throw JSONException("Expected ',' or '}' at $i") }
        }
    }
    private fun arr(): JSONArray {
        val a = JSONArray(); i++
        skip()
        if (i < s.length && s[i] == ']') { i++; return a }
        while (true) {
            a.put(nextValue())
            skip()
            if (i >= s.length) throw JSONException("Unterminated array")
            when (s[i]) { ',' -> { i++; skip(); if (i < s.length && s[i] == ']') { i++; return a } }; ']' -> { i++; return a }; else -> throw JSONException("Expected ',' or ']' at $i") }
        }
    }
    private fun str(q: Char): String {
        i++
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            if (c == q) return sb.toString()
            if (c == '\\' && i < s.length) {
                when (val e = s[i++]) {
                    'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r'); 'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                    'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                    else -> sb.append(e)
                }
            } else sb.append(c)
        }
        throw JSONException("Unterminated string")
    }
    private fun literal(): Any? {
        val start = i
        while (i < s.length && s[i] !in ",:]}/\\\"[{;=#" && !s[i].isWhitespace()) i++
        val t = s.substring(start, i)
        return when {
            t == "true" -> true
            t == "false" -> false
            t == "null" -> JSONObject.NULL
            t.isEmpty() -> throw JSONException("Expected literal at $start")
            else -> t.toIntOrNull() ?: t.toLongOrNull() ?: t.toDoubleOrNull() ?: t
        }
    }
}
