package com.example.fitlock.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GritLockDao {
    // App Group Management
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

    // Workout & XP Tracking
    @Insert
    suspend fun insertWorkout(workout: WorkoutHistory): Long

    @Query("SELECT * FROM workout_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<WorkoutHistory>>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserStats(stats: UserStats)

    // Challenge Management
    @Query("SELECT * FROM challenges")
    fun getChallenges(): Flow<List<Challenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenge(challenge: Challenge)

    // Todo List & Quest Management
    @Query("SELECT * FROM todo_tasks ORDER BY priority DESC, timestamp ASC")
    fun getAllTasks(): Flow<List<TodoTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TodoTask): Long

    @Update
    suspend fun updateTask(task: TodoTask)

    @Delete
    suspend fun deleteTask(task: TodoTask)

    // NEW: Inventory & Shop Management
    @Query("SELECT * FROM inventory_items")
    fun getInventory(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryItem(item: InventoryItem)

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: String): InventoryItem?
}
