@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package java.time

import platform.Foundation.*

enum class Month { JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;
    val value: Int get() = ordinal + 1
    fun getValue() = value
    companion object { fun of(m: Int) = entries[m - 1] }
}
enum class DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY; val value get() = ordinal + 1 }

/** A date without a zone, as days since 1970-01-01 (proleptic Gregorian). */
class LocalDate private constructor(val year: Int, val monthValue: Int, val dayOfMonth: Int) : Comparable<LocalDate> {
    val month: Month get() = Month.of(monthValue)
    val dayOfWeek: DayOfWeek get() = DayOfWeek.entries[((toEpochDay() + 3).mod(7L)).toInt()]
    val dayOfYear: Int get() = (toEpochDay() - of(year, 1, 1).toEpochDay()).toInt() + 1
    fun lengthOfMonth(): Int = daysIn(year, monthValue)
    fun lengthOfYear(): Int = if (leap(year)) 366 else 365
    val isLeapYear: Boolean get() = leap(year)
    fun toEpochDay(): Long {
        val y = if (monthValue <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (monthValue + 9) % 12
        val doy = (153 * mp + 2) / 5 + dayOfMonth - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468
    }
    fun plusDays(n: Long) = ofEpochDay(toEpochDay() + n)
    fun minusDays(n: Long) = ofEpochDay(toEpochDay() - n)
    fun plusMonths(n: Long): LocalDate { val t = year * 12L + (monthValue - 1) + n; val y = (t / 12).toInt(); val m = (t % 12).toInt() + 1; return of(y, m, minOf(dayOfMonth, daysIn(y, m))) }
    fun minusMonths(n: Long) = plusMonths(-n)
    fun plusYears(n: Long) = of(year + n.toInt(), monthValue, minOf(dayOfMonth, daysIn(year + n.toInt(), monthValue)))
    fun minusYears(n: Long) = plusYears(-n)
    fun plusWeeks(n: Long) = plusDays(n * 7)
    fun withDayOfMonth(d: Int) = of(year, monthValue, d)
    fun withMonth(m: Int) = of(year, m, minOf(dayOfMonth, daysIn(year, m)))
    fun withYear(y: Int) = of(y, monthValue, minOf(dayOfMonth, daysIn(y, monthValue)))
    fun isBefore(o: LocalDate) = this < o
    fun isAfter(o: LocalDate) = this > o
    fun isEqual(o: LocalDate) = this == o
    override fun compareTo(other: LocalDate) = toEpochDay().compareTo(other.toEpochDay())
    override fun equals(other: Any?) = other is LocalDate && other.toEpochDay() == toEpochDay()
    override fun hashCode() = toEpochDay().hashCode()
    override fun toString() = "${year.toString().padStart(4, '0')}-${monthValue.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"
    fun format(f: java.time.format.DateTimeFormatter) = f.format(this)
    fun atStartOfDay(): LocalDateTime = LocalDateTime(this, LocalTime.of(0, 0))
    companion object {
        private fun leap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        private fun daysIn(y: Int, m: Int) = when (m) { 2 -> if (leap(y)) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
        fun of(y: Int, m: Int, d: Int): LocalDate {
            if (m !in 1..12 || d !in 1..daysIn(y, m)) throw DateTimeException("Invalid date $y-$m-$d")
            return LocalDate(y, m, d)
        }
        fun of(y: Int, m: Month, d: Int) = of(y, m.value, d)
        fun ofEpochDay(e: Long): LocalDate {
            val z = e + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
            val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
            return LocalDate((if (m <= 2) y + 1 else y).toInt(), m, d)
        }
        fun now(): LocalDate = now(NSDate())
        internal fun now(d: NSDate): LocalDate {
            val c = NSCalendar.currentCalendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, d)
            return LocalDate(c.year.toInt(), c.month.toInt(), c.day.toInt())
        }
        fun parse(s: String): LocalDate { val p = s.split('-'); return of(p[0].toInt(), p[1].toInt(), p[2].toInt()) }
        val MIN = LocalDate(-999999, 1, 1)
        val MAX = LocalDate(999999, 12, 31)
    }
}

class LocalTime private constructor(val hour: Int, val minute: Int, val second: Int = 0) : Comparable<LocalTime> {
    override fun compareTo(other: LocalTime) = toSecondOfDay().compareTo(other.toSecondOfDay())
    fun toSecondOfDay() = hour * 3600 + minute * 60 + second
    override fun toString() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    companion object {
        fun of(h: Int, m: Int, s: Int = 0) = LocalTime(h, m, s)
        fun now(): LocalTime {
            val c = NSCalendar.currentCalendar.components(NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond, NSDate())
            return LocalTime(c.hour.toInt(), c.minute.toInt(), c.second.toInt())
        }
    }
}

class LocalDateTime(private val date: LocalDate, private val time: LocalTime) {
    val year get() = date.year
    val monthValue get() = date.monthValue
    val dayOfMonth get() = date.dayOfMonth
    val hour get() = time.hour
    val minute get() = time.minute
    val second get() = time.second
    fun toLocalDate() = date
    fun toLocalTime() = time
    companion object { fun now() = LocalDateTime(LocalDate.now(), LocalTime.now()) }
}

class Instant private constructor(private val ms: Long) : Comparable<Instant> {
    fun toEpochMilli() = ms
    val epochSecond: Long get() = ms / 1000
    override fun compareTo(other: Instant) = ms.compareTo(other.ms)
    override fun toString() = ms.toString()
    companion object {
        fun now() = Instant(java.lang.System.currentTimeMillis())
        fun ofEpochMilli(ms: Long) = Instant(ms)
        fun ofEpochSecond(s: Long) = Instant(s * 1000)
    }
}

class Duration private constructor(private val ms: Long) {
    fun toMillis() = ms
    val seconds: Long get() = ms / 1000
    fun toMinutes() = ms / 60000
    fun toHours() = ms / 3600000
    fun toDays() = ms / 86400000
    companion object {
        fun ofMillis(ms: Long) = Duration(ms)
        fun ofSeconds(s: Long) = Duration(s * 1000)
        fun ofMinutes(m: Long) = Duration(m * 60000)
        fun ofHours(h: Long) = Duration(h * 3600000)
        fun ofDays(d: Long) = Duration(d * 86400000)
        fun between(a: Instant, b: Instant) = Duration(b.toEpochMilli() - a.toEpochMilli())
    }
}

open class DateTimeException(message: String?) : RuntimeException(message)
