package com.screentimetracker.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.screentimetracker.data.db.AppDatabase
import com.screentimetracker.data.model.AppSummary
import com.screentimetracker.data.model.AppUsageRecord
import com.screentimetracker.data.model.DailyTotal
import com.screentimetracker.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth for all screen-time data.
 *
 * Reads live usage from [UsageStatsManager] and persists it in the
 * Room database so data is available offline and for uninstalled-app history.
 */
class UsageRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.appUsageDao()
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ─────────────────────────────────────────────
    // Live data flows (consumed by ViewModels)
    // ─────────────────────────────────────────────

    fun getTodayTotalFlow(): Flow<Long?> =
        dao.getTotalUsageTodayFlow(DateUtils.today())

    fun getTopInstalledApps(startDate: String, endDate: String): Flow<List<AppSummary>> =
        dao.getTopInstalledApps(startDate, endDate)

    fun getUninstalledApps(): Flow<List<AppSummary>> =
        dao.getUninstalledApps()

    fun getDailyTotals(startDate: String, endDate: String): Flow<List<DailyTotal>> =
        dao.getDailyTotals(startDate, endDate)

    // ─────────────────────────────────────────────
    // Sync logic (called from WorkManager or ViewModel)
    // ─────────────────────────────────────────────

    /**
     * Fetch today's usage from UsageStatsManager and persist to Room.
     * Also detects and marks uninstalled apps.
     *
     * @return number of app records saved
     */
    suspend fun syncTodayUsage(): Int = withContext(Dispatchers.IO) {
        val today = DateUtils.today()
        val (startMs, endMs) = DateUtils.dayBoundsMs(today)

        // Query UsageStats for today
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startMs,
            endMs
        )

        if (stats.isNullOrEmpty()) return@withContext 0

        // Get installed package names for uninstall-detection
        val installedPackages = getInstalledPackageNames()
        val seenPackages = mutableSetOf<String>()

        val records = mutableListOf<AppUsageRecord>()

        for (usageStat in stats) {
            val pkg = usageStat.packageName
            if (usageStat.totalTimeInForeground <= 0) continue

            val appName = getAppName(pkg)
            seenPackages.add(pkg)

            records.add(
                AppUsageRecord(
                    packageName = pkg,
                    appName = appName,
                    date = today,
                    usageTimeMs = usageStat.totalTimeInForeground,
                    lastUsedTimestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        usageStat.lastTimeVisible
                    } else {
                        usageStat.lastTimeUsed
                    },
                    isUninstalled = false
                )
            )
        }

        dao.insertOrReplaceAll(records)

        // Detect uninstalled apps: packages in DB that are no longer installed AND not seen today
        detectAndMarkUninstalled(installedPackages, seenPackages)

        records.size
    }

    /**
     * Sync usage for a specific date range (useful for backfilling weekly/monthly data).
     */
    suspend fun syncUsageRange(startDate: String, endDate: String) = withContext(Dispatchers.IO) {
        val (startMs, endMs) = Pair(
            DateUtils.dateToStartOfDayMs(startDate),
            DateUtils.dateToEndOfDayMs(endDate)
        )

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startMs,
            endMs
        ) ?: return@withContext

        val records = mutableListOf<AppUsageRecord>()
        val installedPackages = getInstalledPackageNames()

        for (stat in stats) {
            if (stat.totalTimeInForeground <= 0) continue

            // Determine which day this stat belongs to
            val statDate = dateFormat.format(Date(stat.lastTimeUsed.takeIf { it > 0 } ?: startMs))
            val appName = getAppName(stat.packageName)
            val isInstalled = stat.packageName in installedPackages

            records.add(
                AppUsageRecord(
                    packageName = stat.packageName,
                    appName = appName,
                    date = statDate,
                    usageTimeMs = stat.totalTimeInForeground,
                    lastUsedTimestamp = stat.lastTimeUsed,
                    isUninstalled = !isInstalled,
                    uninstalledAt = if (!isInstalled) System.currentTimeMillis() else null
                )
            )
        }

        dao.insertOrReplaceAll(records)
    }

    // ─────────────────────────────────────────────
    // Export
    // ─────────────────────────────────────────────

    suspend fun getAllRecordsForExport(): List<AppUsageRecord> =
        withContext(Dispatchers.IO) { dao.getAllRecords() }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // App not installed — use package name as fallback label
            packageName.substringAfterLast('.')
                .replaceFirstChar { it.uppercaseChar() }
        }
    }

    private fun getInstalledPackageNames(): Set<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }.map { it.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun detectAndMarkUninstalled(
        installedPackages: Set<String>,
        seenToday: Set<String>
    ) {
        val knownInDb = dao.getKnownInstalledPackages()
        val now = System.currentTimeMillis()

        for (pkg in knownInDb) {
            // If not installed AND not seen in today's usage stats → mark uninstalled
            if (pkg !in installedPackages && pkg !in seenToday) {
                dao.markAsUninstalled(pkg, now)
            }
        }
    }
}
