package com.screentimetracker.data.model

/**
 * Aggregated usage for an app across a date range.
 * Used to build "top apps" lists and per-app detail views.
 */
data class AppSummary(
    val packageName: String,
    val appName: String,
    val totalUsageMs: Long,
    val lastUsedTimestamp: Long,
    val isUninstalled: Boolean
)
