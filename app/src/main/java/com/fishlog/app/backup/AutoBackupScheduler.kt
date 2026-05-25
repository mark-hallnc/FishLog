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
        Log.d(TAG, "scheduleAutoBackup called")
        val prefs = AppPreferences(context)
        prefs.setAutoBackupScheduled(System.currentTimeMillis())
        prefs.setAutoBackupWorkerMessage("Backup scheduled")
        prefs.markCloudBackupPending()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setConstraints(constraints)
            .setInitialDelay(AUTO_BACKUP_DELAY_MINUTES, TimeUnit.MINUTES)
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
