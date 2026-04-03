package com.screentimetracker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility functions for date formatting and range calculation.
 */
object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val displayFullFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    /** Returns today's date as "yyyy-MM-dd" */
    fun today(): String = dateFormat.format(Date())

    /** Returns "yyyy-MM-dd" string for N days ago */
    fun daysAgo(n: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -n)
        return dateFormat.format(cal.time)
    }

    /** Returns the start-of-day Unix timestamp (ms) for a "yyyy-MM-dd" string */
    fun dateToStartOfDayMs(date: String): Long {
        val cal = Calendar.getInstance()
        cal.time = dateFormat.parse(date) ?: Date()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Returns the end-of-day Unix timestamp (ms) for a "yyyy-MM-dd" string */
    fun dateToEndOfDayMs(date: String): Long {
        val cal = Calendar.getInstance()
        cal.time = dateFormat.parse(date) ?: Date()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /** Returns (startMs, endMs) for a given "yyyy-MM-dd" day */
    fun dayBoundsMs(date: String): Pair<Long, Long> =
        Pair(dateToStartOfDayMs(date), dateToEndOfDayMs(date))

    /** Returns the start of today as Unix ms */
    fun startOfTodayMs(): Long = dateToStartOfDayMs(today())

    /** Returns the current system time as Unix ms */
    fun nowMs(): Long = System.currentTimeMillis()

    /** Formats milliseconds into "Xh Ym" or "Ym Zs" */
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0m"
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /** Short display format "MMM d" */
    fun toDisplayShort(date: String): String {
        val parsed = dateFormat.parse(date) ?: return date
        return displayFormat.format(parsed)
    }

    /** Full display format "MMM d, yyyy" */
    fun toDisplayFull(date: String): String {
        val parsed = dateFormat.parse(date) ?: return date
        return displayFullFormat.format(parsed)
    }

    /** Converts a Unix timestamp (ms) to "MMM d, yyyy HH:mm" */
    fun timestampToDisplay(ms: Long): String {
        if (ms <= 0) return "Never"
        val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(ms))
    }

    /** 7-day range: returns (7 days ago, today) */
    fun weekRange(): Pair<String, String> = Pair(daysAgo(6), today())

    /** 30-day range: returns (30 days ago, today) */
    fun monthRange(): Pair<String, String> = Pair(daysAgo(29), today())
}
