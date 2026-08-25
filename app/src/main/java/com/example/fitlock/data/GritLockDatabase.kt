package com.example.fitlock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppGroup::class, 
        WorkoutHistory::class, 
        UserStats::class, 
        Challenge::class, 
        AppRule::class, 
        VaultItem::class,
        Gauntlet::class,
        Habit::class,
        GauntletHistory::class,
        DailyPledge::class,
        UrgeEvent::class,
        UsageBaseline::class,
        MotivationalQuote::class,
        DailyLogNote::class,
        AppBlockEvent::class,
        Flashcard::class,
        WorkoutBlockEntity::class,
        WorkoutExercise::class
    ], 
    version = 22, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GritLockDatabase : RoomDatabase() {
    abstract fun dao(): GritLockDao

    companion object {
        @Volatile
        private var INSTANCE: GritLockDatabase? = null

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create flashcards
                database.execSQL("CREATE TABLE IF NOT EXISTS `flashcards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `deckName` TEXT NOT NULL, `frontText` TEXT NOT NULL, `backText` TEXT NOT NULL, `drawingData` TEXT, `easinessFactor` REAL NOT NULL, `interval` INTEGER NOT NULL, `nextReviewDate` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL)")
                
                // 2. Create workout_blocks
                database.execSQL("CREATE TABLE IF NOT EXISTS `workout_blocks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gauntletId` INTEGER NOT NULL, `blockType` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `restAfterBlock` INTEGER NOT NULL, FOREIGN KEY(`gauntletId`) REFERENCES `gauntlets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_blocks_gauntletId` ON `workout_blocks` (`gauntletId`)")
                
                // 3. Create workout_exercises
                database.execSQL("CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `blockId` INTEGER NOT NULL, `name` TEXT NOT NULL, `sets` INTEGER NOT NULL, `targetReps` TEXT NOT NULL, `restBetweenSets` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `progressionLevel` INTEGER NOT NULL, FOREIGN KEY(`blockId`) REFERENCES `workout_blocks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_blockId` ON `workout_exercises` (`blockId`)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Update urge_events
                database.execSQL("ALTER TABLE urge_events ADD COLUMN category TEXT NOT NULL DEFAULT 'General'")
                database.execSQL("ALTER TABLE urge_events ADD COLUMN subCategory TEXT")
                database.execSQL("ALTER TABLE urge_events ADD COLUMN comment TEXT")

                // 2. Create motivational_quotes
                database.execSQL("CREATE TABLE IF NOT EXISTS `motivational_quotes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `text` TEXT NOT NULL, `category` TEXT NOT NULL, `subCategory` TEXT)")

                // 3. Create daily_log_notes
                database.execSQL("CREATE TABLE IF NOT EXISTS `daily_log_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `content` TEXT NOT NULL, `type` TEXT NOT NULL)")

                // 4. Create app_block_events
                database.execSQL("CREATE TABLE IF NOT EXISTS `app_block_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `packageName` TEXT NOT NULL, `reason` TEXT)")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_stats ADD COLUMN avatarType TEXT NOT NULL DEFAULT 'SEEKER'")
                database.execSQL("ALTER TABLE user_stats ADD COLUMN unlockedGear TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_stats ADD COLUMN activeTheme TEXT NOT NULL DEFAULT 'DEFAULT'")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_flashcards_deckName_frontText_backText` ON `flashcards` (`deckName`, `frontText`, `backText`)")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE flashcards ADD COLUMN allFieldsJson TEXT")
            }
        }

        fun getDatabase(context: Context): GritLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GritLockDatabase::class.java,
                    "gritlock-db"
                )
                .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
