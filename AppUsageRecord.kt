package com.screentimetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a daily usage record for a single application.
 * Each row is uniquely identified by (packageName + date).
 * When an app is uninstalled, [isUninstalled] is set to true
 * and its data is preserved for history display.
 */
@Entity(
    tableName = "app_usage_records",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class AppUsageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Android package name, e.g. "com.instagram.android" */
    val packageName: String,

    /** Human-readable app name, e.g. "Instagram" */
    val appName: String,

    /** Date this record covers, in "yyyy-MM-dd" format */
    val date: String,

    /** Total foreground usage in milliseconds for this date */
    val usageTimeMs: Long,

    /** Unix timestamp (ms) of the last time the app was used */
    val lastUsedTimestamp: Long,

    /** Whether the app has been uninstalled from the device */
    val isUninstalled: Boolean = false,

    /** Unix timestamp (ms) when uninstall was detected (null if still installed) */
    val uninstalledAt: Long? = null
)
