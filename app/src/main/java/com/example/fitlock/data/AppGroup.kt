package com.example.fitlock.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

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

data class ScheduleInterval(
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int
)

data class ExerciseRequirement(
    val type: String,
    val count: Int,
    val isExtra: Boolean = false
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
    val xpGained: Int = 0
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val lastWorkoutDate: Long = 0L,
    val bankedReps: Map<String, Int> = emptyMap(),
    val calibrations: Map<String, ExerciseCalibration> = emptyMap(),
    val bankResetFrequency: String = "Never", // "Daily", "Weekly", "Monthly", "Never"
    val lastBankReset: Long = 0L
)

@Dao
interface GritLockDao {
    @Query("SELECT * FROM app_groups")
    fun getAllGroups(): Flow<List<AppGroup>>

    @Query("SELECT * FROM app_groups WHERE id = :id")
    suspend fun getGroupById(id: Int): AppGroup?

    @Insert
    suspend fun insertGroup(group: AppGroup): Long

    @Update
    suspend fun updateGroup(group: AppGroup)

    @Delete
    suspend fun deleteGroup(group: AppGroup): Int

    @Insert
    suspend fun insertWorkout(workout: WorkoutHistory): Long

    @Query("SELECT * FROM workout_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<WorkoutHistory>>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserStats(stats: UserStats)

    @Query("SELECT * FROM challenges")
    fun getChallenges(): Flow<List<Challenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenge(challenge: Challenge)
}

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toStringList(data: String): List<String> = data.split(",").filter { it.isNotBlank() }

    @TypeConverter
    fun fromExerciseList(list: List<ExerciseRequirement>): String {
        return list.joinToString(";") { "${it.type}:${it.count}:${it.isExtra}" }
    }

    @TypeConverter
    fun toExerciseList(data: String): List<ExerciseRequirement> {
        if (data.isBlank()) return emptyList()
        return data.split(";").map {
            val parts = it.split(":")
            ExerciseRequirement(parts[0], parts[1].toInt(), parts.getOrNull(2)?.toBoolean() ?: false)
        }
    }

    @TypeConverter
    fun fromScheduleList(list: List<ScheduleInterval>): String {
        return list.joinToString(";") { "${it.dayOfWeek}:${it.startMinute}:${it.endMinute}" }
    }

    @TypeConverter
    fun toScheduleList(data: String): List<ScheduleInterval> {
        if (data.isBlank()) return emptyList()
        return data.split(";").map {
            val parts = it.split(":")
            ScheduleInterval(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }

    @TypeConverter
    fun fromRepMap(map: Map<String, Int>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun toRepMap(data: String): Map<String, Int> {
        if (data.isBlank()) return emptyMap()
        return data.split(";").associate {
            val parts = it.split(":")
            parts[0] to parts[1].toInt()
        }
    }

    @TypeConverter
    fun fromCalibrationMap(map: Map<String, ExerciseCalibration>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value.topValue}:${it.value.bottomValue}:${it.value.thresholdValue}" }
    }

    @TypeConverter
    fun toCalibrationMap(data: String): Map<String, ExerciseCalibration> {
        if (data.isBlank()) return emptyMap()
        return data.split(";").associate {
            val parts = it.split(":")
            parts[0] to ExerciseCalibration(parts[0], parts[1].toDouble(), parts[2].toDouble(), parts.getOrNull(3)?.toDouble() ?: 0.0)
        }
    }
}

@Database(entities = [AppGroup::class, WorkoutHistory::class, UserStats::class, Challenge::class], version = 11, exportSchema = false)
@TypeConverters(Converters::class)
abstract class GritLockDatabase : RoomDatabase() {
    abstract fun dao(): GritLockDao
}
