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
        WorkoutExercise::class,
        HabitDefinition::class,
        Quest::class,
        QuestBlock::class,
        ExerciseDefinition::class,
        TaskDefinition::class,
        Goal::class,
        Project::class,
        Milestone::class,
        ProjectTask::class,
        ExerciseFamily::class,
        ProgressionLevel::class
    ], 
    version = 35, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GritLockDatabase : RoomDatabase() {
    abstract fun dao(): GritLockDao

    companion object {
        @Volatile
        private var INSTANCE: GritLockDatabase? = null

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_groups ADD COLUMN restrictedWifiSsids TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Pre-populate Remaining families
                val pullupFamilyId = "family_pullup"
                database.execSQL("INSERT OR IGNORE INTO exercise_families (id, name, description) VALUES ('$pullupFamilyId', 'Pullup Family', 'Master vertical pulling.')")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_pullup_1', '$pullupFamilyId', 1, 'Scapular Pulls', 'PULLUP', '3-1-1-1', 15, 100)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_pullup_2', '$pullupFamilyId', 2, 'Negative Pullups', 'PULLUP', '3-1-1-1', 8, 150)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_pullup_3', '$pullupFamilyId', 3, 'Standard Pullup', 'PULLUP', '3-1-1-1', 5, 200)")

                val dipFamilyId = "family_dip"
                database.execSQL("INSERT OR IGNORE INTO exercise_families (id, name, description) VALUES ('$dipFamilyId', 'Dip Family', 'Master vertical pushing.')")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_dip_1', '$dipFamilyId', 1, 'Support Hold', 'DIP', '1-30-1-1', 30, 100)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_dip_2', '$dipFamilyId', 2, 'Negative Dips', 'DIP', '3-1-1-1', 8, 150)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_dip_3', '$dipFamilyId', 3, 'Standard Dip', 'DIP', '3-1-1-1', 5, 200)")

                val rowFamilyId = "family_row"
                database.execSQL("INSERT OR IGNORE INTO exercise_families (id, name, description) VALUES ('$rowFamilyId', 'Row Family', 'Horizontal pulling for a thick back.')")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_row_1', '$rowFamilyId', 1, 'Horizontal Row', 'ROW', '3-1-1-1', 12, 100)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_row_2', '$rowFamilyId', 2, 'Wide Row', 'ROW', '3-1-1-1', 10, 150)")

                val hingeFamilyId = "family_hinge"
                database.execSQL("INSERT OR IGNORE INTO exercise_families (id, name, description) VALUES ('$hingeFamilyId', 'Hinge Family', 'Posterior chain development.')")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_hinge_1', '$hingeFamilyId', 1, 'Romanian Deadlift', 'HINGE', '3-1-1-1', 15, 100)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_hinge_2', '$hingeFamilyId', 2, 'Single Leg RDL', 'HINGE', '3-1-1-1', 10, 150)")
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_families` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `icon` TEXT, PRIMARY KEY(`id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `progression_levels` (`id` TEXT NOT NULL, `familyId` TEXT NOT NULL, `level` INTEGER NOT NULL, `variantName` TEXT NOT NULL, `exerciseType` TEXT NOT NULL, `tempo` TEXT NOT NULL DEFAULT '3-1-1-1', `unlockRequirementReps` INTEGER NOT NULL, `xpReward` INTEGER NOT NULL DEFAULT 100, `instructions` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`familyId`) REFERENCES `exercise_families`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_progression_levels_familyId` ON `progression_levels` (`familyId`)")
                
                // Update user_stats to include familyProgression
                database.execSQL("ALTER TABLE user_stats ADD COLUMN familyProgression TEXT NOT NULL DEFAULT ''")
                
                // Pre-populate Pushup Family
                val pushupFamilyId = "family_pushup"
                database.execSQL("INSERT OR IGNORE INTO exercise_families (id, name, description) VALUES ('$pushupFamilyId', 'Pushup Family', 'Master the art of the floor press.')")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_pushup_1', '$pushupFamilyId', 1, 'Standard Pushup', 'PUSHUP', '3-1-1-1', 15, 100)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_pushup_2', '$pushupFamilyId', 2, 'Diamond Pushup', 'PUSHUP', '3-1-1-1', 12, 150)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_pushup_3', '$pushupFamilyId', 3, 'Archer Pushup', 'PUSHUP', '3-1-1-1', 8, 200)")

                // Pre-populate Squat Family
                val squatFamilyId = "family_squat"
                database.execSQL("INSERT OR IGNORE INTO exercise_families (id, name, description) VALUES ('$squatFamilyId', 'Squat Family', 'Build lower body power.')")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_squat_1', '$squatFamilyId', 1, 'Standard Squat', 'SQUAT', '3-1-1-1', 20, 100)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_squat_2', '$squatFamilyId', 2, 'Cossack Squat', 'SQUAT', '3-1-1-1', 15, 150)")
                database.execSQL("INSERT OR IGNORE INTO progression_levels (id, familyId, level, variantName, exerciseType, tempo, unlockRequirementReps, xpReward) VALUES ('lvl_squat_3', '$squatFamilyId', 3, 'Pistol Squat', 'SQUAT', '3-1-1-1', 5, 200)")
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE quest_blocks ADD COLUMN sourceTaskId TEXT")
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `project_tasks` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `xpReward` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_project_tasks_projectId` ON `project_tasks` (`projectId`)")
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `targetDate` INTEGER, `category` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `xpReward` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `xpReward` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_goalId` ON `projects` (`goalId`)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `milestones` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `xpReward` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_milestones_projectId` ON `milestones` (`projectId`)")
                
                // Add projectId to quests
                database.execSQL("ALTER TABLE quests ADD COLUMN projectId TEXT")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_lastNightReflection TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_dreams TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_dailyGoals TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_personalGoal TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_workGoal TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_obstacles TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_solutions TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_gratitude TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_awe TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_morningIdeas TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_readListen TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN morn_learn TEXT")
                
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_summary TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_story TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_accomplishments TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_wins TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_losses TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_resolutions TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_peaceOfMindGoal TEXT")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_mood INTEGER")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_energy INTEGER")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_eveningIdeas TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN even_meals TEXT")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_definitions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `defaultSets` INTEGER NOT NULL, `defaultTargetReps` TEXT NOT NULL, `defaultRestSeconds` INTEGER NOT NULL, `icon` TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `task_definitions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT, `autoCarryOver` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create quests table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quests` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `goalId` TEXT, 
                        `milestoneId` TEXT, 
                        `triggerType` TEXT NOT NULL, 
                        `triggerTime` TEXT, 
                        `physicalTriggerType` TEXT NOT NULL, 
                        `physicalTriggerData` TEXT, 
                        `isLockShieldEnabled` INTEGER NOT NULL, 
                        `targetBlockGroupId` INTEGER, 
                        `unlockAtEndOnly` INTEGER NOT NULL, 
                        `xpReward` INTEGER NOT NULL, 
                        `icon` TEXT, 
                        `themeOverride` TEXT, 
                        `isCompletedToday` INTEGER NOT NULL, 
                        `lastCompletedTimestamp` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                
                // 2. Create quest_blocks table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quest_blocks` (
                        `id` TEXT NOT NULL, 
                        `questId` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `orderIndex` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `isCompleted` INTEGER NOT NULL, 
                        `completionTimestamp` INTEGER, 
                        `definitionId` INTEGER, 
                        `estimatedDurationSeconds` INTEGER, 
                        `subHabits` TEXT NOT NULL, 
                        `mediaUrl` TEXT, 
                        `mediaType` TEXT NOT NULL, 
                        `trackingType` TEXT NOT NULL, 
                        `sets` INTEGER, 
                        `targetReps` TEXT, 
                        `restBetweenSets` INTEGER, 
                        `autoCarryOver` INTEGER NOT NULL, 
                        `deckId` INTEGER, 
                        `actionType` TEXT, 
                        `currentStreak` INTEGER NOT NULL, 
                        `lastDoneTimestamp` INTEGER NOT NULL, 
                        `higherIsBetter` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_blocks_questId` ON `quest_blocks` (`questId`)")
                
                // 3. Migrate Gauntlets to Quests
                database.execSQL("""
                    INSERT INTO quests (id, name, type, triggerType, triggerTime, physicalTriggerType, physicalTriggerData, isLockShieldEnabled, targetBlockGroupId, unlockAtEndOnly, xpReward, icon, isCompletedToday, lastCompletedTimestamp)
                    SELECT CAST(id AS TEXT), name, 'ROUTINE', triggerType, triggerTime, physicalTriggerType, physicalTriggerData, (CASE WHEN targetBlockGroupId IS NOT NULL THEN 1 ELSE 0 END), targetBlockGroupId, 1, 10, icon, 0, 0 FROM gauntlets
                """.trimIndent())
                
                // 4. Migrate Habits to QuestBlocks
                database.execSQL("""
                    INSERT INTO quest_blocks (id, questId, type, orderIndex, name, definitionId, estimatedDurationSeconds, subHabits, mediaUrl, mediaType, trackingType, currentStreak, lastDoneTimestamp, higherIsBetter, isCompleted, autoCarryOver)
                    SELECT CAST(id AS TEXT), CAST(gauntletId AS TEXT), 'HABIT', orderIndex, name, definitionId, estimatedDurationSeconds, subHabits, mediaUrl, mediaType, trackingType, currentStreak, lastCompletionTimestamp, higherIsBetter, 0, 1 FROM habits
                """.trimIndent())
                
                // 5. Migrate WorkoutExercises to QuestBlocks
                database.execSQL("""
                    INSERT INTO quest_blocks (id, questId, type, orderIndex, name, sets, targetReps, restBetweenSets, isCompleted, autoCarryOver, subHabits, mediaType, trackingType, higherIsBetter, currentStreak, lastDoneTimestamp)
                    SELECT 'ex_' || e.id, CAST(b.gauntletId AS TEXT), 'EXERCISE', (b.orderIndex * 100 + e.orderIndex), e.name, e.sets, e.targetReps, e.restBetweenSets, 0, 1, '', 'NONE', 'REPS', 1, 0, 0
                    FROM workout_exercises e
                    JOIN workout_blocks b ON e.blockId = b.id
                """.trimIndent())
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create habit_definitions
                database.execSQL("CREATE TABLE IF NOT EXISTS `habit_definitions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT, `mediaUrl` TEXT, `mediaType` TEXT NOT NULL, `trackingType` TEXT NOT NULL, `higherIsBetter` INTEGER NOT NULL DEFAULT 1, `defaultEstimatedDurationSeconds` INTEGER, `defaultSubHabits` TEXT NOT NULL)")
                
                // 2. Recreate habits table to include foreign key to habit_definitions
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habits_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `gauntletId` INTEGER NOT NULL, 
                        `definitionId` INTEGER, 
                        `name` TEXT NOT NULL, 
                        `orderIndex` INTEGER NOT NULL, 
                        `estimatedDurationSeconds` INTEGER, 
                        `icon` TEXT, 
                        `subHabits` TEXT NOT NULL, 
                        `mediaUrl` TEXT, 
                        `mediaType` TEXT NOT NULL, 
                        `trackingType` TEXT NOT NULL, 
                        `higherIsBetter` INTEGER NOT NULL, 
                        `lastCompletionTimestamp` INTEGER NOT NULL, 
                        `currentStreak` INTEGER NOT NULL, 
                        FOREIGN KEY(`gauntletId`) REFERENCES `gauntlets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`definitionId`) REFERENCES `habit_definitions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                
                // Copy data from old table. Note: higherIsBetter defaults to 1 (true)
                database.execSQL("""
                    INSERT INTO habits_new (id, gauntletId, name, orderIndex, estimatedDurationSeconds, icon, subHabits, mediaUrl, mediaType, trackingType, lastCompletionTimestamp, currentStreak, higherIsBetter, definitionId)
                    SELECT id, gauntletId, name, orderIndex, estimatedDurationSeconds, icon, subHabits, mediaUrl, mediaType, trackingType, lastCompletionTimestamp, currentStreak, 1, NULL FROM habits
                """.trimIndent())
                
                database.execSQL("DROP TABLE habits")
                database.execSQL("ALTER TABLE habits_new RENAME TO habits")
                
                // 3. Recreate indices
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_gauntletId` ON `habits` (`gauntletId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_definitionId` ON `habits` (`definitionId`)")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN eveningReflection TEXT")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habits ADD COLUMN mediaUrl TEXT")
                database.execSQL("ALTER TABLE habits ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE habits ADD COLUMN trackingType TEXT NOT NULL DEFAULT 'TIME'")
                database.execSQL("ALTER TABLE habits ADD COLUMN lastCompletionTimestamp INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE habits ADD COLUMN currentStreak INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_pledges ADD COLUMN mainObjective TEXT")
            }
        }

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
                .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
