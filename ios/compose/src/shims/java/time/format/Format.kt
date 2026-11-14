@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package java.time.format

import platform.Foundation.*

/** A date pattern through NSDateFormatter (its patterns are the same Unicode ones). */
class DateTimeFormatter private constructor(private val pattern: String, private val locale: java.util.Locale?) {
    fun format(d: java.time.LocalDate): String {
        val f = NSDateFormatter()
        f.dateFormat = pattern
        locale?.let { f.locale = NSLocale(localeIdentifier = it.toLanguageTag()) }
        val c = NSDateComponents()
        c.year = d.year.toLong(); c.month = d.monthValue.toLong(); c.day = d.dayOfMonth.toLong()
        val date = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)!!.dateFromComponents(c) ?: return d.toString()
        return f.stringFromDate(date)
    }
    fun withLocale(l: java.util.Locale) = DateTimeFormatter(pattern, l)
    companion object {
        fun ofPattern(p: String) = DateTimeFormatter(p, null)
        fun ofPattern(p: String, l: java.util.Locale) = DateTimeFormatter(p, l)
        val ISO_LOCAL_DATE = DateTimeFormatter("yyyy-MM-dd", null)
    }
}
open class DateTimeParseException(message: String?, text: CharSequence? = null, index: Int = 0) : java.time.DateTimeException(message)
