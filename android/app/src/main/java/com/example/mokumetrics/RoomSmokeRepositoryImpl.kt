package com.example.mokumetrics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSmokeRepositoryImpl(private val smokeDao: SmokeDao) : SmokeRepository {
    override fun getAllRecords(): Flow<List<SmokeRecord>> {
        return smokeDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllRecordsSync(): List<SmokeRecord> {
        return smokeDao.getAllRecordsSync().map { it.toDomain() }
    }

    override suspend fun insertRecord(record: SmokeRecord) {
        smokeDao.insertRecord(RoomSmokeRecord.fromDomain(record))
    }

    override suspend fun deleteRecord(id: String) {
        smokeDao.deleteRecord(id)
    }

    override suspend fun updateMemo(id: String, memo: String) {
        smokeDao.updateMemo(id, memo)
    }

    override suspend fun clearAllRecords() {
        smokeDao.clearAllRecords()
    }
}
