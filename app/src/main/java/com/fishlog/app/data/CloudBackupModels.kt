package com.fishlog.app.data

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
