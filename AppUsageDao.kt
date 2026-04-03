package com.screentimetracker.data.db

import androidx.room.*
import com.screentimetracker.data.model.AppSummary
import com.screentimetracker.data.model.AppUsageRecord
import com.screentimetracker.data.model.DailyTotal
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for all screen time queries.
 */
@Dao
interface AppUsageDao {

    // ─────────────────────────────────────────────
    // Insert / Upsert
    // ─────────────────────────────────────────────

    /**
     * Insert or replace a usage record.
     * If a record with the same (packageName, date) exists, it is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(record: AppUsageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(records: List<AppUsageRecord>)

    // ─────────────────────────────────────────────
    // Uninstalled-app management
    // ─────────────────────────────────────────────

    /**
     * Mark all records for a package as uninstalled.
     * Called when the package is no longer found on the device.
     */
    @Query("""
        UPDATE app_usage_records
        SET isUninstalled = 1, uninstalledAt = :uninstalledAt
        WHERE packageName = :packageName AND isUninstalled = 0
    """)
    suspend fun markAsUninstalled(packageName: String, uninstalledAt: Long)

    // ─────────────────────────────────────────────
    // Today's usage
    // ─────────────────────────────────────────────

    @Query("""
        SELECT SUM(usageTimeMs) FROM app_usage_records
        WHERE date = :date AND isUninstalled = 0
    """)
    fun getTotalUsageTodayFlow(date: String): Flow<Long?>

    @Query("""
        SELECT SUM(usageTimeMs) FROM app_usage_records WHERE date = :date
    """)
    suspend fun getTotalUsageForDate(date: String): Long?

    // ─────────────────────────────────────────────
    // Per-app summaries (installed)
    // ─────────────────────────────────────────────

    /**
     * Top apps by total usage for a date range.
     * Returns installed apps only.
     */
    @Query("""
        SELECT packageName, appName,
               SUM(usageTimeMs) AS totalUsageMs,
               MAX(lastUsedTimestamp) AS lastUsedTimestamp,
               isUninstalled
        FROM app_usage_records
        WHERE date BETWEEN :startDate AND :endDate
          AND isUninstalled = 0
        GROUP BY packageName
        ORDER BY totalUsageMs DESC
    """)
    fun getTopInstalledApps(startDate: String, endDate: String): Flow<List<AppSummary>>

    /**
     * All usage records for a specific package within a date range.
     */
    @Query("""
        SELECT * FROM app_usage_records
        WHERE packageName = :packageName
          AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    fun getRecordsForPackage(
        packageName: String,
        startDate: String,
        endDate: String
    ): Flow<List<AppUsageRecord>>

    // ─────────────────────────────────────────────
    // Uninstalled apps
    // ─────────────────────────────────────────────

    /**
     * All uninstalled apps, aggregated, newest uninstall first.
     */
    @Query("""
        SELECT packageName, appName,
               SUM(usageTimeMs) AS totalUsageMs,
               MAX(lastUsedTimestamp) AS lastUsedTimestamp,
               isUninstalled
        FROM app_usage_records
        WHERE isUninstalled = 1
        GROUP BY packageName
        ORDER BY MAX(uninstalledAt) DESC
    """)
    fun getUninstalledApps(): Flow<List<AppSummary>>

    // ─────────────────────────────────────────────
    // Daily totals (for charts)
    // ─────────────────────────────────────────────

    @Query("""
        SELECT date, SUM(usageTimeMs) AS totalMs
        FROM app_usage_records
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailyTotals(startDate: String, endDate: String): Flow<List<DailyTotal>>

    // ─────────────────────────────────────────────
    // Export helpers
    // ─────────────────────────────────────────────

    @Query("SELECT * FROM app_usage_records ORDER BY date DESC, usageTimeMs DESC")
    suspend fun getAllRecords(): List<AppUsageRecord>

    // ─────────────────────────────────────────────
    // Package tracking (detect uninstalls)
    // ─────────────────────────────────────────────

    /** Returns distinct package names that are marked as still installed */
    @Query("""
        SELECT DISTINCT packageName FROM app_usage_records WHERE isUninstalled = 0
    """)
    suspend fun getKnownInstalledPackages(): List<String>
}
