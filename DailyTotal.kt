package com.screentimetracker.data.model

/**
 * Aggregated total screen time for a single day.
 * Used by Room queries and displayed in charts.
 */
data class DailyTotal(
    val date: String,
    val totalMs: Long
)
