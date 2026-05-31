package com.fishlog.app.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.data.CloudBackupRepository
import com.fishlog.app.data.FishLogDatabase
import com.fishlog.app.data.JsonBackupHelper
import com.fishlog.app.data.PhotoStorageHelper
import kotlinx.coroutines.flow.first

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "FishLogCloud"

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val prefs = AppPreferences(applicationContext)
        val cloudRepo = CloudBackupRepository(applicationContext)

        val now = System.currentTimeMillis()
        prefs.setLastCloudBackupAttempt(now)
        prefs.setAutoBackupStarted(now)
        prefs.setAutoBackupInProgress(true)
        prefs.setAutoBackupWorkerMessage("Backing up")

        // 1. Checks AppPreferences
        if (!prefs.isAutomaticCloudBackupEnabled()) {
            Log.d(TAG, "Auto backup worker: Automatic mode disabled.")
            prefs.setAutoBackupInProgress(false)
            prefs.setAutoBackupWorkerMessage("Automatic backup is off")
            return androidx.work.ListenableWorker.Result.success()
        }

        if (!prefs.getCloudBackupPending()) {
            Log.d(TAG, "Auto backup worker: No pending changes.")
            prefs.setAutoBackupInProgress(false)
            prefs.setAutoBackupWorkerMessage("Up to date")
            return androidx.work.ListenableWorker.Result.success()
        }

        // 2. Checks Supabase session/current user
        if (!cloudRepo.isSignedIn()) {
            Log.d(TAG, "Auto backup worker: Not signed in. Keeping pending flag true.")
            prefs.setAutoBackupInProgress(false)
            prefs.setLastCloudBackupFailure(now, "Please sign in to use cloud backup.")
            prefs.setAutoBackupWorkerMessage("Not signed in")
            return androidx.work.ListenableWorker.Result.success()
        }

        // 2b. Check frequency window
        val lastBackupAt = prefs.getLastCloudBackupAt()
        val freqHours = prefs.getCloudBackupFrequencyHours()
        if (lastBackupAt != null) {
            val nextAllowedAt = lastBackupAt + (freqHours * 60 * 60 * 1000L)
            if (now < nextAllowedAt) {
                Log.d(TAG, "Auto backup worker: Frequency window not reached. Rescheduling.")
                prefs.setAutoBackupInProgress(false)
                prefs.setAutoBackupWorkerMessage("Waiting for backup window")
                AutoBackupScheduler.scheduleAutoBackup(applicationContext)
                return androidx.work.ListenableWorker.Result.success()
            }
        }

        try {
            Log.d(TAG, "Auto backup worker: Fetching logs and trips...")
            
            // 3. Create full backup JSON
            val database = FishLogDatabase.getDatabase(applicationContext)
            val catchLogs = database.catchLogDao().getAllCatches().first()
            val trips = database.fishingTripDao().getAllTrips().first()
            val photoHelper = PhotoStorageHelper(applicationContext)

            // 4. Calls CloudBackupRepository.backupNow()
            val result = cloudRepo.backupNow(catchLogs, trips, photoHelper)

            return if (result.isSuccess) {
                // 5. On success
                prefs.clearCloudBackupPending()
                prefs.setLastCloudBackupSuccess(now)
                prefs.setAutoBackupCompleted(now)
                prefs.setAutoBackupInProgress(false)
                prefs.setAutoBackupWorkerMessage("Backup complete")
                Log.d(TAG, "Auto backup worker: Success.")
                androidx.work.ListenableWorker.Result.success()
            } else {
                // On setup failure/sign-out or transient failure
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Auto backup worker: Failed - $error")
                
                prefs.setAutoBackupInProgress(false)
                if (error.contains("sign in", ignoreCase = true) || error.contains("not set up", ignoreCase = true)) {
                    // setup failure/sign-out: keep pending true, return success so it doesn't endlessly retry
                    prefs.setAutoBackupWorkerMessage("Cloud setup issue")
                    androidx.work.ListenableWorker.Result.success()
                } else {
                    // transient failure
                    prefs.setLastCloudBackupFailure(now, error)
                    prefs.setAutoBackupWorkerMessage("Backup failed")
                    androidx.work.ListenableWorker.Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup worker: Exception during backup", e)
            prefs.setAutoBackupInProgress(false)
            prefs.setLastCloudBackupFailure(now, "Unexpected error during backup.")
            prefs.setAutoBackupWorkerMessage("Error: ${e.message}")
            return androidx.work.ListenableWorker.Result.retry()
        }
    }
}
