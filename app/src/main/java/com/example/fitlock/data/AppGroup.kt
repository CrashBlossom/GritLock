package com.example.fitlock.data

import androidx.room.*
import java.util.concurrent.ConcurrentHashMap

enum class StatType { STR, AGI, VIT, INT, SEN, CHA }

enum class GauntletTriggerType { MANUAL, TIME_BASED }
enum class PhysicalTriggerType { NONE, NFC, QR }
enum class AvatarType { SEEKER, WARRIOR, MAGE, ROGUE }
enum class HabitMediaType { NONE, IMAGE, GIF, YOUTUBE }
enum class HabitTrackingType { TIME, REPS, CHECKLIST }

@Entity(tableName = "app_groups")
data class AppGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val packageNames: List<String>,
    val exercises: List<ExerciseRequirement>,
    val unlockDurationMinutes: Int,
    val lastUnlockedTimestamp: Long = 0L,
    val isEnabled: Boolean = true,
    val schedule: List<ScheduleInterval> = emptyList(),
    val restrictedWifiSsids: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val icon: String? = null
)

@Entity(tableName = "gauntlets")
data class Gauntlet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val triggerType: GauntletTriggerType = GauntletTriggerType.MANUAL,
    val triggerTime: String? = null, // HH:mm format for time-based
    val bufferSeconds: Int = 10,
    val targetBlockGroupId: Int? = null,
    val whitelistedPackages: List<String> = emptyList(),
    val icon: String? = null,
    val physicalTriggerType: PhysicalTriggerType = PhysicalTriggerType.NONE,
    val physicalTriggerData: String? = null,
    val delayedNudgeMinutes: Int = 5
)

@Entity(tableName = "exercise_definitions")
data class ExerciseDefinition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Links to ExerciseType name
    val defaultSets: Int = 3,
    val defaultTargetReps: String = "10",
    val defaultRestSeconds: Int = 60,
    val icon: String? = null
)

@Entity(tableName = "task_definitions")
data class TaskDefinition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String? = "General",
    val autoCarryOver: Boolean = true
)

@Entity(tableName = "habit_definitions")
data class HabitDefinition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String? = null,
    val mediaUrl: String? = null,
    val mediaType: HabitMediaType = HabitMediaType.NONE,
    val trackingType: HabitTrackingType = HabitTrackingType.TIME,
    val higherIsBetter: Boolean = true,
    val defaultEstimatedDurationSeconds: Int? = 60,
    val defaultSubHabits: List<String> = emptyList()
)

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = Gauntlet::class,
            parentColumns = ["id"],
            childColumns = ["gauntletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HabitDefinition::class,
            parentColumns = ["id"],
            childColumns = ["definitionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("gauntletId"), Index("definitionId")]
)
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gauntletId: Int,
    val definitionId: Int? = null, // Link to library
    val name: String, // Fallback if definitionId is null
    val orderIndex: Int,
    val estimatedDurationSeconds: Int? = 60,
    val icon: String? = null,
    val subHabits: List<String> = emptyList(),
    val mediaUrl: String? = null,
    val mediaType: HabitMediaType = HabitMediaType.NONE,
    val trackingType: HabitTrackingType = HabitTrackingType.TIME,
    val higherIsBetter: Boolean = true,
    val lastCompletionTimestamp: Long = 0L,
    val currentStreak: Int = 0
)

data class HabitWithDefinition(
    @Embedded val habit: Habit,
    @Relation(
        parentColumn = "definitionId",
        entityColumn = "id"
    )
    val definition: HabitDefinition?
) {
    // Helper to get effective name (from definition or local fallback)
    val effectiveName: String get() = definition?.name ?: habit.name
    val effectiveMediaType: HabitMediaType get() = definition?.mediaType ?: habit.mediaType
    val effectiveMediaUrl: String? get() = definition?.mediaUrl ?: habit.mediaUrl
    val effectiveTrackingType: HabitTrackingType get() = definition?.trackingType ?: habit.trackingType
    val effectiveSubHabits: List<String> get() = definition?.defaultSubHabits ?: habit.subHabits
}

@Entity(tableName = "gauntlet_history")
data class GauntletHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gauntletId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val totalTimeSeconds: Int,
    val estimatedTimeSeconds: Int,
    val xpGained: Int,
    val streakCount: Int,
    val habitLogs: List<HabitLog>
)

data class HabitLog(
    val habitId: Int,
    val name: String,
    val actualDurationSeconds: Int,
    val estimatedDurationSeconds: Int?,
    val repsCompleted: Int? = null
)

data class GauntletWithHabits(
    @Embedded val gauntlet: Gauntlet,
    @Relation(
        entity = Habit::class,
        parentColumn = "id",
        entityColumn = "gauntletId"
    )
    val habits: List<HabitWithDefinition>
)

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String, // "Distracting", "Grind", "Essential"
    val requiredTollType: StatType,
    val requiredTollAmount: Int,
    val grindTimeRequirementMinutes: Long = 0,
    val isHardLocked: Boolean = false
)

data class ScheduleInterval(
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int
)

data class ExerciseRequirement(
    val type: String,
    val count: Int,
    val isExtra: Boolean = false,
    val targetPackageNames: List<String> = emptyList(), // Supports multiple apps for usage tracking
    val deckName: String? = null, // For FLASHCARDS requirement: "ALL" or specific deck
    val variantName: String? = null,
    val familyId: String? = null
)

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val imageUri: String? = null,
    val requirements: List<ExerciseRequirement>,
    val unlockDurationMinutes: Int = 5,
    val lastUnlockedTimestamp: Long = 0L,
    val icon: String? = null
)

data class ExerciseCalibration(
    val exerciseType: String,
    val topValue: Double,
    val bottomValue: Double,
    val thresholdValue: Double = 0.0,
    val variantName: String? = null
)

// Singleton to handle real-time unlock status and bypass cache latency
object LockStatusManager {
    private val recentUnlocks = ConcurrentHashMap<Int, Long>()

    fun updateUnlock(groupId: Int, timestamp: Long) {
        recentUnlocks[groupId] = timestamp
    }

    fun getLastUnlocked(groupId: Int, dbTimestamp: Long): Long {
        val memoryTimestamp = recentUnlocks[groupId] ?: 0L
        return if (memoryTimestamp > dbTimestamp) memoryTimestamp else dbTimestamp
    }
}

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val requirements: List<ExerciseRequirement>,
    val xpReward: Int,
    val isCompletedToday: Boolean = false,
    val lastCompletedTimestamp: Long = 0L
)

@Entity(tableName = "workout_history")
data class WorkoutHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val exerciseType: String,
    val repsCompleted: Int,
    val appGroupId: Int,
    val xpGained: Int = 0,
    val statType: StatType = StatType.STR
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val name: String = "Seeker",
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentHp: Int = 100,
    val maxHp: Int = 100,
    val currentStreak: Int = 0,
    val lastWorkoutDate: Long = 0L,
    
    // The 6 Core RPG Stats (Stored as cumulative XP)
    val strXp: Long = 0,
    val agiXp: Long = 0,
    val vitXp: Long = 0,
    val intXp: Long = 0,
    val senXp: Long = 0,
    val chaXp: Long = 0,
    
    // Willpower and Sobriety
    val willpowerXp: Long = 0,
    val sobrietyStreak: Int = 0,
    val longestSobrietyStreak: Int = 0,
    val reclaimedMinutesTotal: Long = 0,
    val lastPledgeDate: String? = null, // YYYY-MM-DD
    
    val activeTheme: String = "DEFAULT",
    
    val avatarType: AvatarType = AvatarType.SEEKER,
    val unlockedGear: List<String> = emptyList(),
    
    val penaltyStateActive: Boolean = false,
    
    // System fields
    val bankedReps: Map<String, Int> = emptyMap(),
    val calibrations: Map<String, ExerciseCalibration> = emptyMap(),
    val familyProgression: Map<String, Int> = emptyMap(), // familyId -> currentLevel
    val bankResetFrequency: String = "Never", 
    val lastBankReset: Long = 0L
)

@Entity(tableName = "urge_events")
data class UrgeEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val intensity: Int, // 1-10
    val category: String = "General", // Voice, Hunger, Boredom, etc.
    val subCategory: String? = null,
    val comment: String? = null,
    val location: String? = null,
    val wasResisted: Boolean = true
)

@Entity(tableName = "motivational_quotes")
data class MotivationalQuote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val category: String,
    val subCategory: String? = null
)

@Entity(tableName = "daily_log_notes")
data class DailyLogNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val content: String,
    val type: String = "MANUAL" // MANUAL, AUTO_BLOCK, AUTO_EXERCISE, URGE, SNOOZE_REASON
)

@Entity(tableName = "app_block_events")
data class AppBlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val reason: String? = null
)

@Entity(tableName = "usage_baselines")
data class UsageBaseline(
    @PrimaryKey val packageName: String,
    val baselineMinutesPerDay: Int
)
