package com.example.fitlock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AppGroup::class, WorkoutHistory::class, UserStats::class, Challenge::class, AppRule::class], version = 12, exportSchema = false)
@TypeConverters(Converters::class)
abstract class GritLockDatabase : RoomDatabase() {
    abstract fun dao(): GritLockDao

    companion object {
        @Volatile
        private var INSTANCE: GritLockDatabase? = null

        fun getDatabase(context: Context): GritLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GritLockDatabase::class.java,
                    "gritlock-db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
