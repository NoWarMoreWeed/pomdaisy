@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.text.format

object DateFormat {
    fun getTimeFormat(c: android.content.Context): java.text.DateFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
    fun getDateFormat(c: android.content.Context): java.text.DateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT)
    fun is24HourFormat(c: android.content.Context): Boolean = !(platform.Foundation.NSDateFormatter.dateFormatFromTemplate("j", 0u, null) ?: "").contains("a")
    fun format(pattern: CharSequence, ms: Long): CharSequence = java.text.SimpleDateFormat(pattern.toString()).format(java.util.Date(ms))
}
object Formatter { fun formatFileSize(c: android.content.Context?, n: Long): String = if (n >= 1 shl 20) "${n / (1 shl 20)} MB" else "${n / 1024} KB"; fun formatShortFileSize(c: android.content.Context?, n: Long) = formatFileSize(c, n) }
