// What Kotlin/JVM's standard library gives the app's code and Kotlin/Native's doesn't, under the same names (this
// directory is compiled with -Xallow-kotlin-package), so the app's sources compile unchanged.
@file:Suppress("PackageDirectoryMismatch", "NOTHING_TO_INLINE")

package kotlin.text

/** java.lang.String.format's subset the app uses: %[flags][width][.precision](d|i|x|X|o|s|S|c|f|e|g|b|%|n). */
fun String.format(vararg args: Any?): String = pdFormat(this, args)

fun String.Companion.format(format: String, vararg args: Any?): String = pdFormat(format, args)


internal fun pdFormat(fmt: String, args: Array<out Any?>): String {
    val out = StringBuilder()
    var i = 0
    var next = 0
    while (i < fmt.length) {
        val c = fmt[i]
        if (c != '%') { out.append(c); i++; continue }
        i++
        if (i >= fmt.length) break
        // argument index (n$)
        var argIndex = -1
        var j = i
        while (j < fmt.length && fmt[j].isDigit()) j++
        if (j < fmt.length && fmt[j] == '$' && j > i) { argIndex = fmt.substring(i, j).toInt() - 1; i = j + 1 }
        var left = false; var zero = false; var plus = false; var space = false; var comma = false; var alt = false
        loop@ while (i < fmt.length) {
            when (fmt[i]) {
                '-' -> left = true
                '0' -> zero = true
                '+' -> plus = true
                ' ' -> space = true
                ',' -> comma = true
                '#' -> alt = true
                else -> break@loop
            }
            i++
        }
        var width = 0
        while (i < fmt.length && fmt[i].isDigit()) { width = width * 10 + (fmt[i] - '0'); i++ }
        var precision = -1
        if (i < fmt.length && fmt[i] == '.') {
            i++; precision = 0
            while (i < fmt.length && fmt[i].isDigit()) { precision = precision * 10 + (fmt[i] - '0'); i++ }
        }
        if (i >= fmt.length) break
        val conv = fmt[i]; i++
        if (conv == '%') { out.append('%'); continue }
        if (conv == 'n') { out.append('\n'); continue }
        val arg = if (argIndex >= 0) args.getOrNull(argIndex) else args.getOrNull(next++)
        var body: String = when (conv) {
            'd', 'i' -> {
                val v = (arg as? Number)?.toLong() ?: 0L
                var s = (if (v < 0) -v else v).toString()
                if (v == Long.MIN_VALUE) s = "9223372036854775808"
                if (comma) s = s.reversed().chunked(3).joinToString(",").reversed()
                (if (v < 0) "-" else if (plus) "+" else if (space) " " else "") + s
            }
            'x', 'X', 'o' -> {
                val n = arg as? Number
                val v = when (n) {
                    is Byte -> n.toLong() and 0xFF
                    is Short -> n.toLong() and 0xFFFF
                    is Int -> n.toLong() and 0xFFFFFFFFL
                    null -> 0L
                    else -> n.toLong()
                }
                val radix = if (conv == 'o') 8 else 16
                var s = v.toULong().toString(radix)
                if (conv == 'X') s = s.uppercase()
                if (alt) s = (if (radix == 16) (if (conv == 'X') "0X" else "0x") else "0") + s
                s
            }
            'f', 'e', 'E', 'g', 'G' -> {
                val v = (arg as? Number)?.toDouble() ?: 0.0
                val p = if (precision < 0) 6 else precision
                var s = pdFixed(kotlin.math.abs(v), p)
                if (comma) {
                    val dot = s.indexOf('.')
                    val ip = if (dot < 0) s else s.substring(0, dot)
                    s = ip.reversed().chunked(3).joinToString(",").reversed() + (if (dot < 0) "" else s.substring(dot))
                }
                (if (v < 0 || (v == 0.0 && 1.0 / v < 0)) "-" else if (plus) "+" else if (space) " " else "") + s
            }
            'c' -> when (arg) { is Char -> arg.toString(); is Number -> arg.toInt().toChar().toString(); else -> arg.toString() }
            'b', 'B' -> (if (arg == null) "false" else if (arg is Boolean) arg.toString() else "true")
            's', 'S' -> {
                var s = arg.toString()
                if (precision >= 0 && s.length > precision) s = s.substring(0, precision)
                if (conv == 'S') s.uppercase() else s
            }
            else -> arg.toString()
        }
        if (body.length < width) {
            val pad = width - body.length
            body = when {
                left -> body + " ".repeat(pad)
                zero && conv != 's' && conv != 'S' && conv != 'c' -> {
                    val sign = if (body.startsWith("-") || body.startsWith("+") || body.startsWith(" ")) body.substring(0, 1) else ""
                    sign + "0".repeat(pad) + body.substring(sign.length)
                }
                else -> " ".repeat(pad) + body
            }
        }
        out.append(body)
    }
    return out.toString()
}

/** v >= 0 with p decimals, rounded half up. */
internal fun pdFixed(v: Double, p: Int): String {
    if (v.isNaN()) return "NaN"
    if (v.isInfinite()) return "Infinity"
    var scale = 1.0
    repeat(p) { scale *= 10.0 }
    val scaled = kotlin.math.floor(v * scale + 0.5)
    if (scaled >= 9.0e18) return v.toString()
    val n = scaled.toLong()
    if (p == 0) return n.toString()
    val s = n.toString().padStart(p + 1, '0')
    return s.substring(0, s.length - p) + "." + s.substring(s.length - p)
}
