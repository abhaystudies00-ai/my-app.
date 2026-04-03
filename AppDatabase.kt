package com.screentimetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.screentimetracker.data.model.AppUsageRecord

/**
 * Room database singleton for Screen Time Tracker.
 *
 * To add a new entity: add it to [entities], bump [version],
 * and provide a Migration or set fallbackToDestructiveMigration().
 */
@Database(
    entities = [AppUsageRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appUsageDao(): AppUsageDao

    companion object {
        private const val DATABASE_NAME = "screen_time_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton database instance.
         * Thread-safe via double-checked locking.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
