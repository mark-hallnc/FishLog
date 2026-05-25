package com.fishlog.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Required Supabase setup:
 * 1. Create private Storage bucket: fishlog-backups
 * 2. Add RLS policies on storage.objects so authenticated users can read/write only their own folder:
 *    folder name must equal auth.uid()
 * 3. Do not use service_role key in the Android app.
 */
class CloudBackupRepository(context: Context) {
    private val TAG = "FishLogCloud"
    private val prefs: SharedPreferences = context.getSharedPreferences("fishlog_cloud_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_LAST_BACKUP_AT = "last_cloud_backup_at"
        private const val BUCKET_NAME = "fishlog-backups"
    }

    fun isSignedIn(): Boolean {
        return try {
            if (!SupabaseClientProvider.isConfigured()) {
                Log.d(TAG, "isSignedIn: Supabase not configured")
                return false
            }
            val session = SupabaseClientProvider.client.auth.currentSessionOrNull()
            Log.d(TAG, "isSignedIn: Session is ${if (session != null) "active" else "null"}")
            session != null
        } catch (e: Exception) {
            Log.e(TAG, "isSignedIn: Error checking session", e)
            false
        }
    }

    fun getCurrentAccountEmail(): String? {
        return try {
            if (!SupabaseClientProvider.isConfigured()) return prefs.getString(KEY_ACCOUNT_EMAIL, null)
            SupabaseClientProvider.client.auth.currentUserOrNull()?.email 
                ?: prefs.getString(KEY_ACCOUNT_EMAIL, null)
        } catch (e: Exception) {
            prefs.getString(KEY_ACCOUNT_EMAIL, null)
        }
    }

    fun getLastBackupAt(): Long? {
        val last = prefs.getLong(KEY_LAST_BACKUP_AT, 0L)
        return if (last == 0L) null else last
    }

    suspend fun getCurrentUserEmail(): String? = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.isConfigured()) return@withContext null
        try {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            null
        }
    }

    suspend fun refreshAccountState(): Result<AccountInfo?> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) return@withContext Result.success(null)
            
            // This will attempt to refresh the session if needed
            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
            if (user != null) {
                prefs.edit().putString(KEY_ACCOUNT_EMAIL, user.email).apply()
                Result.success(AccountInfo(user.id, user.email))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing account state", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a one-time sign-in code to the provided email address.
     * If the user doesn't exist, an account will be created automatically.
     */
    suspend fun sendSignInCode(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }
            
            Log.d(TAG, "Requesting OTP code for: $email")
            SupabaseClientProvider.client.auth.signInWith(OTP) {
                this.email = email
                this.createUser = true 
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendSignInCode", e)
            Result.failure(Exception("Could not send code. Check your email address and connection."))
        }
    }

    /** wrappers for compatibility if needed elsewhere */
    suspend fun createAccount(email: String): Result<Unit> = sendSignInCode(email)
    suspend fun signIn(email: String): Result<Unit> = sendSignInCode(email)

    /**
     * Verifies the OTP code sent via email. Handles both signup and magiclink flows.
     */
    suspend fun verifyEmailOtp(email: String, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            Log.d(TAG, "Verifying OTP for $email with code $code")
            
            // Try verifying as Magic Link first (login)
            val result = try {
                SupabaseClientProvider.client.auth.verifyEmailOtp(
                    type = OtpType.Email.MAGIC_LINK,
                    email = email,
                    token = code
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.d(TAG, "MagicLink verification failed, trying Signup type")
                try {
                    // Try as Signup confirmation (new user)
                    SupabaseClientProvider.client.auth.verifyEmailOtp(
                        type = OtpType.Email.SIGNUP,
                        email = email,
                        token = code
                    )
                    Result.success(Unit)
                } catch (e2: Exception) {
                    Log.e(TAG, "Both verification types failed", e2)
                    Result.failure(e2)
                }
            }
            
            if (result.isSuccess) {
                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                if (user?.email != null) {
                    prefs.edit().putString(KEY_ACCOUNT_EMAIL, user.email).apply()
                } else {
                    prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not verify code. Check the code and try again."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in verifyEmailOtp", e)
            Result.failure(Exception("Could not verify code. Try again."))
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (SupabaseClientProvider.isConfigured()) {
                SupabaseClientProvider.client.auth.signOut()
            }
            prefs.edit().remove(KEY_ACCOUNT_EMAIL).apply()
            prefs.edit().remove(KEY_LAST_BACKUP_AT).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signOut", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads the full backup JSON and photos to Supabase Storage.
     * Bucket: fishlog-backups
     * Path: {userId}/fishlog-backup.json
     * Photos: {userId}/photos/{fileName}
     */
    suspend fun backupNow(
        catchLogs: List<CatchLog>,
        trips: List<FishingTrip>,
        photoStorageHelper: PhotoStorageHelper
    ): Result<CloudBackupResult> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Please sign in to use cloud backup."))

            val bucket = SupabaseClientProvider.client.storage.from(BUCKET_NAME)
            val manifest = mutableListOf<CloudPhotoBackupItem>()
            
            var photosFound = 0
            var photosUploaded = 0
            var photosFailed = 0

            // 1. Upload Photos
            catchLogs.filter { !it.photoUri.isNullOrBlank() }.forEach { log ->
                photosFound++
                val photoFile = photoStorageHelper.getPhotoFile(log.photoUri)
                if (photoFile != null && photoFile.exists()) {
                    val ext = photoFile.extension.ifBlank { "jpg" }
                    val fileName = "${log.localUuid}.$ext"
                    val cloudPath = "${user.id}/photos/$fileName"
                    
                    try {
                        Log.d(TAG, "Uploading photo: $cloudPath")
                        bucket.upload(cloudPath, photoFile.readBytes()) {
                            upsert = true
                        }
                        
                        manifest.add(CloudPhotoBackupItem(
                            localUuid = log.localUuid,
                            originalPhotoUri = log.photoUri!!,
                            cloudPath = cloudPath,
                            fileName = fileName,
                            uploadedAt = System.currentTimeMillis()
                        ))
                        photosUploaded++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to upload photo: $cloudPath", e)
                        photosFailed++
                    }
                } else {
                    Log.w(TAG, "Photo file not found or unreadable: ${log.photoUri}")
                    photosFailed++
                }
            }

            // 2. Generate and Upload Backup JSON
            val jsonBackup = JsonBackupHelper.createBackup(catchLogs, trips, manifest)
            val backupFileName = "${user.id}/fishlog-backup.json"

            Log.d(TAG, "Uploading cloud backup JSON to $BUCKET_NAME/$backupFileName")
            bucket.upload(backupFileName, jsonBackup.toByteArray()) {
                upsert = true
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_BACKUP_AT, now).apply()

            Result.success(CloudBackupResult(
                dataBackedUp = true,
                photosFound = photosFound,
                photosUploaded = photosUploaded,
                photosFailed = photosFailed,
                photosIncluded = true
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Cloud backup failed", e)
            val msg = e.message ?: ""
            if (msg.contains("Bucket not found", ignoreCase = true)) {
                Result.failure(Exception("Cloud backup is not set up yet."))
            } else {
                Result.failure(Exception("Cloud backup failed. Your local data is safe."))
            }
        }
    }

    /**
     * Downloads the backup JSON and restores data/photos.
     */
    suspend fun restoreFromCloud(
        photoStorageHelper: PhotoStorageHelper,
        onJsonParsed: suspend (FishLogBackup) -> Unit
    ): Result<CloudPhotoRestoreResult> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Please sign in to restore from cloud."))

            val fileName = "${user.id}/fishlog-backup.json"
            val bucket = SupabaseClientProvider.client.storage.from(BUCKET_NAME)

            Log.d(TAG, "Downloading cloud backup from $BUCKET_NAME/$fileName")
            val actualBytes = bucket.downloadAuthenticated(fileName)
            val json = String(actualBytes)
            val backup = JsonBackupHelper.parseBackup(json)

            // Let the caller handle the Room data import (logs, trips)
            onJsonParsed(backup)

            // Restore Photos
            var downloadedCount = 0
            var failedCount = 0
            val restoredUris = mutableMapOf<String, String>()
            
            backup.photoBackupManifest.forEach { item ->
                try {
                    Log.d(TAG, "Restoring photo: ${item.cloudPath}")
                    val photoBytes = bucket.downloadAuthenticated(item.cloudPath)
                    val newLocalUri = photoStorageHelper.savePhotoBytes(photoBytes, item.fileName)
                    
                    if (newLocalUri != null) {
                        restoredUris[item.localUuid] = newLocalUri
                        downloadedCount++
                    } else {
                        failedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore photo: ${item.cloudPath}", e)
                    failedCount++
                }
            }

            Result.success(CloudPhotoRestoreResult(
                downloadedCount = downloadedCount,
                failedCount = failedCount,
                restoredPhotoUris = restoredUris
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Cloud restore failed", e)
            val msg = e.message ?: ""
            when {
                msg.contains("404") || msg.contains("not found", ignoreCase = true) -> 
                    Result.failure(Exception("No cloud backup found."))
                msg.contains("Bucket not found", ignoreCase = true) ->
                    Result.failure(Exception("Cloud backup is not set up yet."))
                else -> 
                    Result.failure(Exception("Cloud restore failed. Your local data was not changed."))
            }
        }
    }

    /**
     * Diagnostic tool to check if cloud backup is correctly configured for the current user.
     */
    suspend fun testCloudBackupSetup(): Result<CloudBackupDiagnosticResult> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: return@withContext Result.success(CloudBackupDiagnosticResult(
                    signedIn = false,
                    bucketReachable = false,
                    canUpload = false,
                    canRead = false,
                    canDeleteTestFile = false,
                    message = "Please sign in before testing cloud backup."
                ))

            val testPath = "${user.id}/diagnostics/test-backup-check.json"
            val bucket = SupabaseClientProvider.client.storage.from(BUCKET_NAME)
            val testData = "{\"test\":true,\"createdAt\":${System.currentTimeMillis()}}"

            Log.d(TAG, "Starting cloud backup diagnostic for user: ${user.id}")

            // 1. Test Upload
            try {
                bucket.upload(testPath, testData.toByteArray()) {
                    upsert = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Diagnostic upload failed", e)
                return@withContext Result.success(CloudBackupDiagnosticResult(
                    signedIn = true,
                    bucketReachable = false,
                    canUpload = false,
                    canRead = false,
                    canDeleteTestFile = false,
                    message = "Cloud backup is not set up correctly yet (Upload failed)."
                ))
            }

            // 2. Test Read
            var readBackWorks = false
            try {
                val bytes = bucket.downloadAuthenticated(testPath)
                if (String(bytes).contains("\"test\":true")) {
                    readBackWorks = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Diagnostic read failed", e)
            }

            if (!readBackWorks) {
                return@withContext Result.success(CloudBackupDiagnosticResult(
                    signedIn = true,
                    bucketReachable = true,
                    canUpload = true,
                    canRead = false,
                    canDeleteTestFile = false,
                    message = "Cloud backup is not set up correctly yet (Read failed)."
                ))
            }

            // 3. Test Delete (Cleanup)
            var deleteWorks = false
            try {
                bucket.delete(listOf(testPath))
                deleteWorks = true
            } catch (e: Exception) {
                Log.e(TAG, "Diagnostic delete failed", e)
            }

            val finalMsg = if (deleteWorks) {
                "Cloud backup setup looks good."
            } else {
                "Upload/read works. Test file cleanup failed."
            }

            Result.success(CloudBackupDiagnosticResult(
                signedIn = true,
                bucketReachable = true,
                canUpload = true,
                canRead = true,
                canDeleteTestFile = deleteWorks,
                message = finalMsg
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Cloud diagnostic encountered an error", e)
            Result.failure(Exception("Could not reach cloud backup. Check your connection and try again."))
        }
    }
}
