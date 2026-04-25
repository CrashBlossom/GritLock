package com.example.fitlock.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GritLockDao {
    @Query("SELECT * FROM app_groups")
    fun getAllGroups(): Flow<List<AppGroup>>

    @Query("SELECT * FROM app_groups WHERE id = :id")
    suspend fun getGroupById(id: Int): AppGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: AppGroup): Long

    @Update
    suspend fun updateGroup(group: AppGroup)

    @Delete
    suspend fun deleteGroup(group: AppGroup): Int

    // RPG Rule Management
    @Query("SELECT * FROM app_rules")
    fun getAllAppRules(): Flow<List<AppRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRule(rule: AppRule)

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getRuleForApp(packageName: String): AppRule?

    // Workout & XP
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
