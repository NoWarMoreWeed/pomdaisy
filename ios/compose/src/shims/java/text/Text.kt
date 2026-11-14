@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package java.text

import platform.Foundation.*

/** Unicode normalisation through Foundation. */
object Normalizer {
    enum class Form { NFD, NFC, NFKD, NFKC }
    fun normalize(s: CharSequence, form: Form): String {
        val ns = NSString.create(string = s.toString())
        return when (form) {
            Form.NFD -> ns.decomposedStringWithCanonicalMapping
            Form.NFC -> ns.precomposedStringWithCanonicalMapping
            Form.NFKD -> ns.decomposedStringWithCompatibilityMapping
            Form.NFKC -> ns.precomposedStringWithCompatibilityMapping
        }
    }
    fun isNormalized(s: CharSequence, form: Form) = normalize(s, form) == s.toString()
}

open class DateFormat internal constructor(private val f: NSDateFormatter) {
    fun format(d: java.util.Date): String = f.stringFromDate(NSDate.dateWithTimeIntervalSince1970(d.time / 1000.0))
    fun format(ms: Long): String = format(java.util.Date(ms))
    fun format(o: Any?): String = when (o) { is java.util.Date -> format(o); is Number -> format(o.toLong()); else -> o.toString() }
    open fun parse(s: String): java.util.Date? = f.dateFromString(s)?.let { java.util.Date((it.timeIntervalSince1970 * 1000).toLong()) }
    var timeZone: java.util.TimeZone? = null
    companion object {
        const val SHORT = 3
        const val MEDIUM = 2
        const val LONG = 1
        const val FULL = 0
        private fun style(s: Int): NSDateFormatterStyle = when (s) { 3 -> NSDateFormatterShortStyle; 2 -> NSDateFormatterMediumStyle; 1 -> NSDateFormatterLongStyle; else -> NSDateFormatterFullStyle }
        fun getDateTimeInstance(date: Int, time: Int): DateFormat = DateFormat(NSDateFormatter().also { it.dateStyle = style(date); it.timeStyle = style(time) })
        fun getDateTimeInstance(date: Int, time: Int, l: java.util.Locale): DateFormat = getDateTimeInstance(date, time)
        fun getDateInstance(date: Int = MEDIUM): DateFormat = DateFormat(NSDateFormatter().also { it.dateStyle = style(date); it.timeStyle = NSDateFormatterNoStyle })
        fun getDateInstance(date: Int, l: java.util.Locale): DateFormat = getDateInstance(date)
        fun getTimeInstance(time: Int = MEDIUM): DateFormat = DateFormat(NSDateFormatter().also { it.dateStyle = NSDateFormatterNoStyle; it.timeStyle = style(time) })
        fun getTimeInstance(time: Int, l: java.util.Locale): DateFormat = getTimeInstance(time)
    }
}

open class SimpleDateFormat(pattern: String, locale: java.util.Locale? = null) :
    DateFormat(NSDateFormatter().also { f -> f.dateFormat = pattern; locale?.let { f.locale = NSLocale(localeIdentifier = it.toLanguageTag()) } })

open class ParseException(message: String?, val errorOffset: Int = 0) : Exception(message)
object NumberFormat {
    fun getInstance(): DecimalFormat = DecimalFormat("#,###")
    fun getInstance(l: java.util.Locale): DecimalFormat = getInstance()
    fun getNumberInstance(): DecimalFormat = getInstance()
    fun getNumberInstance(l: java.util.Locale): DecimalFormat = getInstance()
    fun getIntegerInstance(): DecimalFormat = getInstance()
    fun getPercentInstance(): DecimalFormat = DecimalFormat("0%")
}
class DecimalFormat(private val pattern: String) {
    fun format(n: Long): String = if (pattern.contains(',')) n.toString().reversed().chunked(3).joinToString(",").reversed() else n.toString()
    fun format(n: Int): String = format(n.toLong())
    fun format(n: Double): String = kotlin.text.pdFormat("%.2f", arrayOf(n))
    var maximumFractionDigits = 0
    var minimumFractionDigits = 0
    var isGroupingUsed = true
}

/** Locale-aware comparison through Foundation (PRIMARY strength ignores case and accents). */
class Collator private constructor() : Comparator<Any?> {
    var strength = TERTIARY
    fun compare(a: String, b: String): Int {
        val opts = when (strength) { PRIMARY -> NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch; SECONDARY -> NSCaseInsensitiveSearch; else -> 0UL }
        return NSString.create(string = a).compare(b, opts or NSNumericSearch, platform.Foundation.NSMakeRange(0UL, a.length.toULong()), NSLocale.currentLocale).toInt()
    }
    override fun compare(a: Any?, b: Any?): Int = compare(a.toString(), b.toString())
    fun equals(a: String, b: String) = compare(a, b) == 0
    companion object {
        const val PRIMARY = 0
        const val SECONDARY = 1
        const val TERTIARY = 2
        const val IDENTICAL = 3
        fun getInstance() = Collator()
        fun getInstance(l: java.util.Locale) = Collator()
    }
}
