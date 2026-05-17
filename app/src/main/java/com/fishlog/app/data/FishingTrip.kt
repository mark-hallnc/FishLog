package com.fishlog.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "fishing_trips")
@Serializable
data class FishingTrip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val localUuid: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val waterBody: String,
    val startTime: Long,
    val endTime: Long? = null,
    val notes: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val backupStatus: String = BackupStatus.PENDING_BACKUP
)

