package com.fishlog.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "catch_logs")
@Serializable
data class CatchLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val localUuid: String = UUID.randomUUID().toString(),
    val logType: String = "CATCH",
    val species: String,
    val length: String,
    val weight: String,
    val waterTemp: String,
    val depth: String,
    val lengthInches: Double? = null,
    val weightLbs: Double? = null,
    val waterTempF: Double? = null,
    val depthFeet: Double? = null,
    val tripId: Long? = null,
    val photoUri: String? = null,
    val bait: String,
    val notes: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val backupStatus: String = BackupStatus.PENDING_BACKUP
)

