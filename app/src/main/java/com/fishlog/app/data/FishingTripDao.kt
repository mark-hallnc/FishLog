package com.fishlog.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FishingTripDao {
    @Insert
    suspend fun insertTrip(trip: FishingTrip): Long

    @Update
    suspend fun updateTrip(trip: FishingTrip)

    @Delete
    suspend fun deleteTrip(trip: FishingTrip)

    @Query("SELECT * FROM fishing_trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<FishingTrip>>

    @Query("SELECT * FROM fishing_trips WHERE endTime IS NULL LIMIT 1")
    fun getActiveTrip(): Flow<FishingTrip?>

    @Query("SELECT * FROM fishing_trips WHERE id = :id")
    fun getTripById(id: Long): Flow<FishingTrip?>

    @Query("DELETE FROM fishing_trips")
    suspend fun deleteAllTrips()
}

