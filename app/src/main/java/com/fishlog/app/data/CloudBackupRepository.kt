package com.fishlog.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
            if (!SupabaseClientProvider.isConfigured()) return false
            SupabaseClientProvider.client.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
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
     * Uploads the full backup JSON to Supabase Storage.
     * Bucket: fishlog-backups
     * Path: {userId}/fishlog-backup.json
     */
    suspend fun backupNow(jsonBackup: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Please sign in to use cloud backup."))

            val fileName = "${user.id}/fishlog-backup.json"
            val bucket = SupabaseClientProvider.client.storage[BUCKET_NAME]

            Log.d(TAG, "Uploading cloud backup: $fileName")
            bucket.upload(fileName, jsonBackup.toByteArray()) {
                upsert = true
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_BACKUP_AT, now).apply()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud backup failed", e)
            Result.failure(Exception("Cloud backup failed. Your local data is safe."))
        }
    }

    /**
     * Downloads the backup JSON from Supabase Storage.
     */
    suspend fun restoreFromCloud(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Please sign in to restore from cloud."))

            val fileName = "${user.id}/fishlog-backup.json"
            val bucket = SupabaseClientProvider.client.storage[BUCKET_NAME]

            Log.d(TAG, "Downloading cloud backup: $fileName")
            val actualBytes = bucket.downloadPublic(fileName) // Note: Adjust if downloadAuthenticated is preferred/needed
            
            val json = String(actualBytes)
            Result.success(json)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud restore failed", e)
            Result.failure(Exception("No cloud backup found or restore failed."))
        }
    }
}
