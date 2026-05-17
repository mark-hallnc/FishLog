package com.fishlog.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FishLogBackup(
    val appName: String = "FishLog",
    val backupVersion: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    val catchLogs: List<CatchLog>,
    val trips: List<FishingTrip>
)

object JsonBackupHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun createBackup(catchLogs: List<CatchLog>, trips: List<FishingTrip>): String {
        val backup = FishLogBackup(
            catchLogs = catchLogs,
            trips = trips
        )
        return json.encodeToString(backup)
    }

    fun parseBackup(jsonString: String): FishLogBackup {
        return json.decodeFromString(jsonString)
    }
}

