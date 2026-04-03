package com.screentimetracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.screentimetracker.data.model.AppUsageRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports app usage data as JSON or CSV and shares via Android share sheet.
 */
object ExportUtils {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Export all records as a JSON file and share it.
     */
    suspend fun exportAsJson(context: Context, records: List<AppUsageRecord>) =
        withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "screen_time_$timestamp.json"
            val file = createExportFile(context, fileName)

            FileWriter(file).use { writer ->
                gson.toJson(records, writer)
            }

            shareFile(context, file, "application/json")
        }

    /**
     * Export all records as a CSV file and share it.
     */
    suspend fun exportAsCsv(context: Context, records: List<AppUsageRecord>) =
        withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "screen_time_$timestamp.csv"
            val file = createExportFile(context, fileName)

            FileWriter(file).use { writer ->
                // Header
                writer.write("Package Name,App Name,Date,Usage (ms),Usage (readable),Last Used,Uninstalled\n")
                // Rows
                for (record in records) {
                    writer.write(
                        "${record.packageName}," +
                                "\"${record.appName}\"," +
                                "${record.date}," +
                                "${record.usageTimeMs}," +
                                "${DateUtils.formatDuration(record.usageTimeMs)}," +
                                "${DateUtils.timestampToDisplay(record.lastUsedTimestamp)}," +
                                "${record.isUninstalled}\n"
                    )
                }
            }

            shareFile(context, file, "text/csv")
        }

    private fun createExportFile(context: Context, fileName: String): File {
        val exportDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped storage — use cache dir; shared via FileProvider
            File(context.cacheDir, "exports").also { it.mkdirs() }
        } else {
            @Suppress("DEPRECATION")
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "ScreenTimeTracker"
            ).also { it.mkdirs() }
        }
        return File(exportDir, fileName)
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Export Screen Time Data").also {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
