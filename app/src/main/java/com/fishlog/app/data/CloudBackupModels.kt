package com.fishlog.app.data

enum class AccountStatus {
    SIGNED_OUT,
    SIGNED_IN
}

enum class BackupUiState {
    IDLE,
    BACKUP_IN_PROGRESS,
    RESTORE_IN_PROGRESS,
    SUCCESS,
    ERROR
}
