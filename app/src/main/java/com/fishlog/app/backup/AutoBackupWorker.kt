package com.fishlog.app.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.data.CloudBackupRepository
import com.fishlog.app.data.FishLogDatabase
import com.fishlog.app.data.JsonBackupHelper
import kotlinx.coroutines.flow.first

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "FishLogCloud"

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val prefs = AppPreferences(applicationContext)
        val cloudRepo = CloudBackupRepository(applicationContext)

        // 1. Checks AppPreferences
        if (!prefs.isAutomaticCloudBackupEnabled()) {
            return androidx.work.ListenableWorker.Result.success()
        }

        if (!prefs.getCloudBackupPending()) {
            return androidx.work.ListenableWorker.Result.success()
        }

        // 2. Checks Supabase session/current user
        if (!cloudRepo.isSignedIn()) {
            Log.d(TAG, "Auto backup worker: Not signed in. Keeping pending flag true.")
            return androidx.work.ListenableWorker.Result.success()
        }

        try {
            Log.d(TAG, "Auto backup worker: Starting cloud backup...")
            
            // 3. Create full backup JSON
            val database = FishLogDatabase.getDatabase(applicationContext)
            val catchLogs = database.catchLogDao().getAllCatches().first()
            val trips = database.fishingTripDao().getAllTrips().first()
            val jsonBackup = JsonBackupHelper.createBackup(catchLogs, trips)

            // 4. Calls CloudBackupRepository.backupNow()
            val result = cloudRepo.backupNow(jsonBackup)

            return if (result.isSuccess) {
                // 5. On success
                prefs.clearCloudBackupPending()
                prefs.setLastCloudBackupSuccess(System.currentTimeMillis())
                Log.d(TAG, "Auto backup worker: Success.")
                androidx.work.ListenableWorker.Result.success()
            } else {
                // On setup failure/sign-out or transient failure
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Auto backup worker: Failed - $error")
                
                if (error.contains("sign in", ignoreCase = true) || error.contains("not set up", ignoreCase = true)) {
                    // setup failure/sign-out: keep pending true, return success so it doesn't endlessly retry
                    androidx.work.ListenableWorker.Result.success()
                } else {
                    // transient failure
                    prefs.setLastCloudBackupFailure(System.currentTimeMillis(), error)
                    androidx.work.ListenableWorker.Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup worker: Exception during backup", e)
            prefs.setLastCloudBackupFailure(System.currentTimeMillis(), "Unexpected error during backup.")
            return androidx.work.ListenableWorker.Result.retry()
        }
    }
}
