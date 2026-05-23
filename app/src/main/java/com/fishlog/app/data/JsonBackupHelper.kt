package com.fishlog.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FishLogBackup(
    val appName: String = "FishLog",
    val backupVersion: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val catchLogs: List<CatchLog>,
    val trips: List<FishingTrip>,
    val photoBackupManifest: List<CloudPhotoBackupItem> = emptyList()
)

object JsonBackupHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun createBackup(
        catchLogs: List<CatchLog>,
        trips: List<FishingTrip>,
        photoManifest: List<CloudPhotoBackupItem> = emptyList()
    ): String {
        val backup = FishLogBackup(
            catchLogs = catchLogs,
            trips = trips,
            photoBackupManifest = photoManifest
        )
        return json.encodeToString(backup)
    }

    fun parseBackup(jsonString: String): FishLogBackup {
        return json.decodeFromString(jsonString)
    }
}

