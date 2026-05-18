package com.fishlog.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatchLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatch(catchLog: CatchLog)

    @androidx.room.Update
    suspend fun updateCatch(catchLog: CatchLog)

    @Query("SELECT * FROM catch_logs ORDER BY timestamp DESC")
    fun getAllCatches(): Flow<List<CatchLog>>

    @Delete
    suspend fun deleteCatch(catchLog: CatchLog)

    @Query("UPDATE catch_logs SET tripId = NULL WHERE tripId = :tripId")
    suspend fun clearTripIdForLogs(tripId: Long)
}

