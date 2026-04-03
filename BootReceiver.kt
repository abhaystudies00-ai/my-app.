package com.screentimetracker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules the periodic sync worker after the device reboots.
 * WorkManager periodic work survives reboot on its own, but this
 * provides an explicit re-schedule as a safety net.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            UsageSyncWorker.schedulePeriodicSync(context)
        }
    }
}
