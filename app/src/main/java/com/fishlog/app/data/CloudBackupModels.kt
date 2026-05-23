package com.fishlog.app.data

import kotlinx.serialization.Serializable

enum class AccountStatus {
    SIGNED_OUT,
    WAITING_FOR_CODE,
    SIGNED_IN
}

enum class BackupUiState {
    IDLE,
    AUTH_IN_PROGRESS,
    WAITING_FOR_CODE,
    BACKUP_IN_PROGRESS,
    RESTORE_IN_PROGRESS,
    SUCCESS,
    ERROR
}

data class AccountInfo(
    val userId: String,
    val email: String?
)

data class CloudBackupStatus(
    val isSignedIn: Boolean,
    val email: String?,
    val userId: String?,
    val mode: String,
    val isAutomatic: Boolean,
    val isPending: Boolean,
    val isBackingUp: Boolean,
    val lastBackupAt: Long?,
    val lastAttemptAt: Long?,
    val lastFailedAt: Long?,
    val lastErrorMessage: String?,
    val photosIncluded: Boolean,
    val backupPath: String?
)

data class CloudBackupDiagnosticResult(
    val signedIn: Boolean,
    val bucketReachable: Boolean,
    val canUpload: Boolean,
    val canRead: Boolean,
    val canDeleteTestFile: Boolean,
    val message: String
)

@Serializable
data class CloudPhotoBackupItem(
    val localUuid: String,
    val originalPhotoUri: String,
    val cloudPath: String,
    val fileName: String,
    val uploadedAt: Long
)

data class CloudPhotoRestoreResult(
    val downloadedCount: Int,
    val failedCount: Int,
    val restoredPhotoUris: Map<String, String> // localUuid -> newLocalUri
)

data class CloudBackupResult(
    val dataBackedUp: Boolean,
    val photosFound: Int,
    val photosUploaded: Int,
    val photosFailed: Int,
    val photosIncluded: Boolean,
    val message: String? = null
)
