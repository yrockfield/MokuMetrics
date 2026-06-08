package com.example.mokumetrics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmokeDao {
    @Query("SELECT * FROM smoke_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<RoomSmokeRecord>>

    @Query("SELECT * FROM smoke_records ORDER BY timestamp DESC")
    fun getAllRecordsSync(): List<RoomSmokeRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RoomSmokeRecord)

    @Query("DELETE FROM smoke_records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("UPDATE smoke_records SET memo = :memo WHERE id = :id")
    suspend fun updateMemo(id: String, memo: String)

    @Query("DELETE FROM smoke_records")
    suspend fun clearAllRecords()
}
