package io.github.yashkasera.alohomora.common

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utility for common date and time formatting operations.
 *
 * Usage:
 * ```kotlin
 * // Format as HH:mm:ss
 * DateUtils.format(timestamp, DateTimeFormat.HH_MM_SS)
 *
 * // Format with milliseconds, timestamp is in seconds
 * DateUtils.format(timestamp, DateTimeFormat.HH_MM_SS_SSS, TimeUnit.SECONDS)
 *
 * // Full date-time format
 * DateUtils.format(timestamp, DateTimeFormat.ISO_DATE_TIME)
 * ```
 */
object DateUtils {

    /**
     * Time unit for the input timestamp.
     */
    enum class TimeUnit {
        MILLISECONDS,
        SECONDS
    }

    /**
     * Predefined date and time formats.
     */
    enum class Format(internal val defaultUnit: TimeUnit = TimeUnit.MILLISECONDS) {
        /** HH:mm:ss - Example: "14:30:45" */
        HH_MM_SS,

        /** HH:mm:ss.SS - 2 digit ms - Example: "14:30:45.42" */
        HH_MM_SS_2MS,

        /** HH:mm:ss.SSS - 3 digit ms - Example: "14:30:45.421" */
        HH_MM_SS_3MS,

        /** MMM dd, HH:mm:ss - Example: "Jan 13, 14:30:45" */
        MONTH_DAY_TIME,

        /** yyyy-MM-dd HH:mm:ss.SSS - Example: "2026-03-13 14:30:45.421" */
        ISO_DATE_TIME,

        /** yyyy-MM-dd HH:mm:ss - Example: "2026-03-13 14:30:45" (typically for epoch seconds) */
        ISO_DATE_TIME_SECONDS(TimeUnit.SECONDS),

        /** MMM dd, yyyy • HH:mm - Example: "Jan 13, 2026 • 14:30" (typically for epoch seconds) */
        READABLE_DATE_TIME(TimeUnit.SECONDS);
    }

    /**
     * Formats a timestamp according to the specified format.
     *
     * @param timestamp The timestamp value
     * @param format The desired output format
     * @param unit The time unit of the input timestamp (defaults to format's default unit)
     * @return Formatted date-time string, or fallback value if formatting fails
     */
    fun format(
        timestamp: Long,
        format: Format,
        unit: TimeUnit = format.defaultUnit
    ): String = try {
        val instant = when (unit) {
            TimeUnit.MILLISECONDS -> Instant.fromEpochMilliseconds(timestamp)
            TimeUnit.SECONDS -> Instant.fromEpochSeconds(timestamp)
        }
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        when (format) {
            Format.HH_MM_SS -> "${dt.hh()}:${dt.mm()}:${dt.ss()}"
            Format.HH_MM_SS_2MS -> "${dt.hh()}:${dt.mm()}:${dt.ss()}.${dt.ms2()}"
            Format.HH_MM_SS_3MS -> "${dt.hh()}:${dt.mm()}:${dt.ss()}.${dt.ms3()}"
            Format.MONTH_DAY_TIME -> "${dt.monthShort()} ${dt.day}, ${dt.hh()}:${dt.mm()}:${dt.ss()}"
            Format.ISO_DATE_TIME -> "${dt.yyyy()}-${dt.monthNum()}-${dt.dd()} ${dt.hh()}:${dt.mm()}:${dt.ss()}.${dt.ms3()}"
            Format.ISO_DATE_TIME_SECONDS -> "${dt.yyyy()}-${dt.monthNum()}-${dt.dd()} ${dt.hh()}:${dt.mm()}:${dt.ss()}"
            Format.READABLE_DATE_TIME -> "${dt.monthShort()} ${dt.day}, ${dt.yyyy()} • ${dt.hh()}:${dt.mm()}"
        }
    } catch (_: Exception) {
        format.fallback
    }

    // Extension functions for concise formatting
    private fun kotlinx.datetime.LocalDateTime.yyyy() = year.toString()
    private fun kotlinx.datetime.LocalDateTime.monthNum() = (month.ordinal + 1).toString().padStart(2, '0')
    private fun kotlinx.datetime.LocalDateTime.dd() = day.toString().padStart(2, '0')
    private fun kotlinx.datetime.LocalDateTime.hh() = hour.toString().padStart(2, '0')
    private fun kotlinx.datetime.LocalDateTime.mm() = minute.toString().padStart(2, '0')
    private fun kotlinx.datetime.LocalDateTime.ss() = second.toString().padStart(2, '0')
    private fun kotlinx.datetime.LocalDateTime.ms2() = (nanosecond / 1_000_000).toString().padStart(3, '0').take(2)
    private fun kotlinx.datetime.LocalDateTime.ms3() = (nanosecond / 1_000_000).toString().padStart(3, '0')
    private fun kotlinx.datetime.LocalDateTime.monthShort() = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

    private val Format.fallback: String
        get() = when (this) {
            Format.HH_MM_SS -> "00:00:00"
            Format.HH_MM_SS_2MS -> "00:00:00.00"
            Format.HH_MM_SS_3MS -> "00:00:00.000"
            Format.MONTH_DAY_TIME -> "Unknown"
            Format.ISO_DATE_TIME -> "Invalid timestamp"
            Format.ISO_DATE_TIME_SECONDS -> "Unknown"
            Format.READABLE_DATE_TIME -> "Unknown date"
        }
}
