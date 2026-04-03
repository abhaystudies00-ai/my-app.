package com.screentimetracker.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Utilities for checking and requesting the PACKAGE_USAGE_STATS permission.
 *
 * This permission is NOT a runtime permission — it cannot be granted
 * programmatically. The user must enable it manually in:
 *   Settings > Digital Wellbeing & parental controls > (or Settings > Apps > Usage Access)
 *
 * We use AppOpsManager to check whether it has been granted.
 */
object PermissionUtils {

    /**
     * Returns true if the app has been granted Usage Access permission.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens the system Usage Access settings screen where the user
     * can grant permission to this app.
     */
    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Some OEMs don't have this screen — fall back to general Settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
