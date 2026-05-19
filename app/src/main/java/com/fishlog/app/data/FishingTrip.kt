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
    val backupStatus: String = BackupStatus.PENDING_BACKUP,
    val skyCondition: String = "",
    val windCondition: String = "",
    val airTempF: Double? = null,
    val waterClarity: String = "",
    val pressureTrend: String = "",
    val cloudId: String? = null,
    val lastSyncedAt: Long? = null,

    // Weather auto-fill fields
    val weatherAutoFilled: Boolean = false,
    val weatherSource: String = "",
    val weatherFetchedAt: Long? = null,
    val feelsLikeF: Double? = null,
    val humidityPercent: Double? = null,
    val windSpeedMph: Double? = null,
    val windDirectionDegrees: Double? = null,
    val windGustMph: Double? = null,
    val barometricPressureHpa: Double? = null,
    val cloudCoverPercent: Double? = null,
    val precipitationIn: Double? = null,
    val weatherCode: Int? = null,
    val weatherSummary: String = "",

    // Moon phase fields
    val moonAutoFilled: Boolean = false,
    val moonPhaseName: String = "",
    val moonIlluminationPercent: Double? = null,
    val moonAgeDays: Double? = null,
    val moonPhaseFraction: Double? = null,
    val moonWaxing: Boolean? = null,
    val moonCalculatedAt: Long? = null
)

