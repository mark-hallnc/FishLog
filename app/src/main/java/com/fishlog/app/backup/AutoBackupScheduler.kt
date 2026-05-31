package com.fishlog.app.backup

import android.content.Context
import android.util.Log
import androidx.work.*
import com.fishlog.app.data.AppPreferences
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {

    private const val WORK_NAME = "fishlog_auto_cloud_backup"
    private const val TAG = "FishLogCloud"

    const val AUTO_BACKUP_DELAY_MINUTES = 10L
    const val AUTO_BACKUP_SOON_DELAY_SECONDS = 30L

    fun scheduleAutoBackup(context: Context) {
        val prefs = AppPreferences(context)
        val now = System.currentTimeMillis()
        
        val selectedHours = prefs.getCloudBackupFrequencyHours()
        val lastBackupAt = prefs.getLastCloudBackupAt()
        
        // shortDebounce: 10 minutes (matching existing constant)
        val shortDebounceMillis = AUTO_BACKUP_DELAY_MINUTES * 60 * 1000L
        
        val nextAllowedAt = if (lastBackupAt == null) {
            now + shortDebounceMillis
        } else {
            lastBackupAt + (selectedHours * 60 * 60 * 1000L)
        }
        
        val delayMillis = (nextAllowedAt - now).coerceAtLeast(shortDebounceMillis)
        
        Log.d(TAG, "scheduleAutoBackup: freq=${prefs.getCloudBackupFrequency()}, lastBackupAt=$lastBackupAt, nextAllowedAt=$nextAllowedAt, delayMinutes=${delayMillis / 60000}")
        
        prefs.setAutoBackupScheduled(now)
        prefs.setAutoBackupWorkerMessage("Backup scheduled")
        prefs.markCloudBackupPending()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun runAutoBackupSoon(context: Context) {
        Log.d(TAG, "runAutoBackupSoon called")
        val prefs = AppPreferences(context)
        prefs.setAutoBackupScheduled(System.currentTimeMillis())
        prefs.setAutoBackupWorkerMessage("Backup scheduled soon")
        prefs.markCloudBackupPending()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setConstraints(constraints)
            .setInitialDelay(AUTO_BACKUP_SOON_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAutoBackup(context: Context) {
        Log.d(TAG, "cancelAutoBackup called")
        val prefs = AppPreferences(context)
        prefs.setAutoBackupWorkerMessage("Automatic backup is off")
        prefs.setAutoBackupInProgress(false)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
