package com.example.mokumetrics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smoke_records")
data class RoomSmokeRecord(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val memo: String
) {
    fun toDomain(): SmokeRecord {
        return SmokeRecord(id = id, timestamp = timestamp, memo = memo)
    }

    companion object {
        fun fromDomain(domain: SmokeRecord): RoomSmokeRecord {
            return RoomSmokeRecord(id = domain.id, timestamp = domain.timestamp, memo = domain.memo)
        }
    }
}
