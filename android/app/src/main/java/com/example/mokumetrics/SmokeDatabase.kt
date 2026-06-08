package com.example.mokumetrics

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RoomSmokeRecord::class], version = 1, exportSchema = false)
abstract class SmokeDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao

    companion object {
        @Volatile
        private var INSTANCE: SmokeDatabase? = null

        fun getDatabase(context: Context): SmokeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmokeDatabase::class.java,
                    "smoke_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
