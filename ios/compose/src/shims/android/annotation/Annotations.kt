@file:Suppress("PackageDirectoryMismatch", "unused", "UNUSED_PARAMETER")

package android.annotation

@Retention(AnnotationRetention.SOURCE) annotation class SuppressLint(vararg val value: String)
@Retention(AnnotationRetention.SOURCE) annotation class TargetApi(val value: Int)
@Retention(AnnotationRetention.SOURCE) annotation class SuppressWarnings(vararg val value: String)
