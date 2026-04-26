package com.example.fitlock.data

import androidx.room.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Stat types for our RPG system.
 */
enum class StatType { STR, AGI, VIT, INT, SEN, CHA }

/**
 * Hunter Grades/Ranks based on Solo Leveling.
 * Every 10 levels the user can rank up.
 */
enum class HunterRank { E, D, C, B, A, S }

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
    val keywords: List<String> = emptyList(),
    val isCharismaGroup: Boolean = false, // If true, using these apps gains CHA XP
    val isDistraction: Boolean = true     // If true, these apps are hard-locked in Dungeon Mode
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
    val targetPackageName: String? = null
)

data class ExerciseCalibration(
    val exerciseType: String,
    val topValue: Double,
    val bottomValue: Double,
    val thresholdValue: Double = 0.0
)

/**
 * Shop Items that can be bought with Gold.
 */
@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val type: ItemType,
    var quantity: Int = 0
)

enum class ItemType { FOCUS_POTION, WEIGHTS_OF_DISCIPLINE, BYPASS_SCROLL }

// Singleton to handle real-time unlock status
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
    val goldReward: Int = 0,
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
    val gold: Int = 0,
    val currentHp: Int = 100,
    val maxHp: Int = 100,
    val currentStreak: Int = 0,
    val lastWorkoutDate: Long = 0L,
    val rank: HunterRank = HunterRank.E,
    
    // Core RPG Stats
    val strXp: Long = 0,
    val agiXp: Long = 0,
    val vitXp: Long = 0,
    val intXp: Long = 0,
    val senXp: Long = 0,
    val chaXp: Long = 0,
    
    val penaltyStateActive: Boolean = false,
    val isDungeonModeActive: Boolean = false, // Hard-locks distracting apps
    
    // System fields
    val bankedReps: Map<String, Int> = emptyMap(),
    val calibrations: Map<String, ExerciseCalibration> = emptyMap(),
    val bankResetFrequency: String = "Never", 
    val lastBankReset: Long = 0L
)
