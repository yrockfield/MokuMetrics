package com.example.mokumetrics

import kotlinx.coroutines.flow.Flow

interface SmokeRepository {
    fun getAllRecords(): Flow<List<SmokeRecord>>
    suspend fun insertRecord(record: SmokeRecord)
    suspend fun deleteRecord(id: String)
    suspend fun updateMemo(id: String, memo: String)
    suspend fun clearAllRecords()
    suspend fun getAllRecordsSync(): List<SmokeRecord> // 非同期かつブロッキング同期取得（ウィジェット用）
}
