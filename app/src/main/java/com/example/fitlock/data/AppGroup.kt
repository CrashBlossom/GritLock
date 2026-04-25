package com.example.fitlock.data

import androidx.room.*
import java.util.concurrent.ConcurrentHashMap

enum class StatType { STR, AGI, VIT, INT, SEN, CHA }

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
    val keywords: List<String> = emptyList()
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
    val targetPackageName: String? = null // New field for app-usage requirements
)

data class ExerciseCalibration(
    val exerciseType: String,
    val topValue: Double,
    val bottomValue: Double,
    val thresholdValue: Double = 0.0
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
    
    val penaltyStateActive: Boolean = false,
    
    // System fields
    val bankedReps: Map<String, Int> = emptyMap(),
    val calibrations: Map<String, ExerciseCalibration> = emptyMap(),
    val bankResetFrequency: String = "Never", 
    val lastBankReset: Long = 0L
)
