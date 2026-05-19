package com.fishlog.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.delay

class CloudBackupRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fishlog_cloud_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCOUNT_EMAIL = "account_email"
    }

    fun isSignedIn(): Boolean = getCurrentAccountEmail() != null

    fun getCurrentAccountEmail(): String? = prefs.getString(KEY_ACCOUNT_EMAIL, null)

    suspend fun createAccount(email: String): Result<Unit> {
        // TODO: Replace with Supabase Auth magic link or email sign up
        delay(1000)
        prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
        return Result.success(Unit)
    }

    suspend fun signIn(email: String): Result<Unit> {
        // TODO: Replace with Supabase Auth magic link
        delay(1000)
        prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
        return Result.success(Unit)
    }

    suspend fun signOut(): Result<Unit> {
        // TODO: Replace with Supabase Auth signOut
        delay(500)
        prefs.edit().remove(KEY_ACCOUNT_EMAIL).apply()
        return Result.success(Unit)
    }

    suspend fun backupNow(): Result<Unit> {
        // TODO: Replace with Supabase storage upload of local database or JSON backup
        delay(2000)
        return Result.success(Unit)
    }

    suspend fun restoreFromCloud(): Result<Unit> {
        // TODO: Replace with Supabase storage download and merge into local Room
        delay(2000)
        return Result.success(Unit)
    }
}
