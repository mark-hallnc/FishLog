package com.fishlog.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class CloudBackupRepository(context: Context) {
    private val TAG = "FishLogCloud"
    private val prefs: SharedPreferences = context.getSharedPreferences("fishlog_cloud_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCOUNT_EMAIL = "account_email"
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

    suspend fun createAccount(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }
            
            Log.d(TAG, "Sending OTP for account creation: $email")
            SupabaseClientProvider.client.auth.signInWith(OTP) {
                this.email = email
                createUser = true
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in createAccount", e)
            Result.failure(Exception("Could not send code. Check your email address and connection."))
        }
    }

    suspend fun signIn(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            Log.d(TAG, "Sending OTP for sign in: $email")
            SupabaseClientProvider.client.auth.signInWith(OTP) {
                this.email = email
                createUser = true
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signIn", e)
            Result.failure(Exception("Could not send code. Check your email address and connection."))
        }
    }

    suspend fun verifyEmailOtp(email: String, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase is not configured."))
            }

            Log.d(TAG, "Verifying OTP for $email with code $code")
            // TODO: Fix unresolved verifyOtp call for Supabase Kotlin 3.1.x
            // SupabaseClientProvider.client.auth.verifyOtp(type = OtpType.Email, email = email, token = code)
            
            // For now, return success to test UI scaffolding
            prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
            
            Result.success(Unit)
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
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in signOut", e)
            Result.failure(e)
        }
    }

    suspend fun backupNow(): Result<Unit> {
        // TODO: Replace with Supabase storage upload of local database or JSON backup
        delay(1000)
        return Result.failure(Exception("Cloud backup upload is coming next."))
    }

    suspend fun restoreFromCloud(): Result<Unit> {
        // TODO: Replace with Supabase storage download and merge into local Room
        delay(1000)
        return Result.failure(Exception("Cloud restore is coming next."))
    }
}
