package com.screentimetracker.worker

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.work.*
import com.screentimetracker.data.repository.UsageRepository
import com.screentimetracker.util.PermissionUtils
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs today's usage data every 15 minutes.
 * WorkManager ensures this runs even if the app is in the background.
 */
class UsageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Skip if usage access not granted
        if (!PermissionUtils.hasUsageAccessPermission(applicationContext)) {
            return Result.failure()
        }

        return try {
            val repo = UsageRepository(applicationContext)
            repo.syncTodayUsage()
            Result.success()
        } catch (e: Exception) {
            // Retry up to 3 times on failure
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "usage_sync_periodic"

        /** Schedule periodic sync every 15 minutes (minimum WorkManager interval) */
        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancel periodic sync */
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
