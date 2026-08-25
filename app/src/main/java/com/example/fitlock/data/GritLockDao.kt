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

    // Vault
    @Query("SELECT * FROM vault_items")
    fun getAllVaultItems(): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVaultItem(item: VaultItem)

    @Delete
    suspend fun deleteVaultItem(item: VaultItem)

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getVaultItemById(id: Int): VaultItem?

    // Gauntlets
    @Transaction
    @Query("SELECT * FROM gauntlets")
    fun getAllGauntletsWithHabits(): Flow<List<GauntletWithHabits>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGauntlet(gauntlet: Gauntlet): Long

    @Delete
    suspend fun deleteGauntlet(gauntlet: Gauntlet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: Habit): Long

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE gauntletId = :gauntletId")
    suspend fun deleteHabitsForGauntlet(gauntletId: Int)

    // Gauntlet History
    @Query("SELECT * FROM gauntlet_history ORDER BY timestamp DESC")
    fun getGauntletHistory(): Flow<List<GauntletHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGauntletHistory(history: GauntletHistory)

    @Query("SELECT COUNT(*) FROM gauntlet_history WHERE gauntletId = :gauntletId AND timestamp >= :since")
    suspend fun getGauntletCountSince(gauntletId: Int, since: Long): Int

    // Daily Pledge
    @Query("SELECT * FROM daily_pledges WHERE date = :date")
    suspend fun getPledgeForDate(date: String): DailyPledge?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPledge(pledge: DailyPledge)

    @Query("SELECT * FROM daily_pledges ORDER BY date DESC")
    fun getAllPledges(): Flow<List<DailyPledge>>

    // Urge Events
    @Query("SELECT * FROM urge_events ORDER BY timestamp DESC")
    fun getUrgeEvents(): Flow<List<UrgeEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUrgeEvent(event: UrgeEvent)

    // Motivational Quotes
    @Query("SELECT * FROM motivational_quotes")
    fun getAllQuotes(): Flow<List<MotivationalQuote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuote(quote: MotivationalQuote)

    @Query("SELECT * FROM motivational_quotes WHERE category = :category")
    fun getQuotesByCategory(category: String): Flow<List<MotivationalQuote>>

    // Daily Log Notes
    @Query("SELECT * FROM daily_log_notes ORDER BY timestamp DESC")
    fun getAllLogNotes(): Flow<List<DailyLogNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogNote(note: DailyLogNote)

    // App Block Events
    @Query("SELECT * FROM app_block_events ORDER BY timestamp DESC")
    fun getBlockEvents(): Flow<List<AppBlockEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockEvent(event: AppBlockEvent)

    // Usage Baselines
    @Query("SELECT * FROM usage_baselines")
    fun getAllBaselines(): Flow<List<UsageBaseline>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBaseline(baseline: UsageBaseline)

    @Delete
    suspend fun deleteBaseline(baseline: UsageBaseline)

    // Flashcards (Anki-lite)
    @Query("SELECT * FROM flashcards ORDER BY nextReviewDate ASC")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckName = :deckName ORDER BY nextReviewDate ASC")
    fun getFlashcardsByDeck(deckName: String): Flow<List<Flashcard>>

    @Query("SELECT DISTINCT deckName FROM flashcards")
    fun getAllDeckNames(): Flow<List<String>>

    @Query("DELETE FROM flashcards WHERE deckName = :deckName")
    suspend fun deleteFlashcardsByDeck(deckName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashcard(card: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashcards(cards: List<Flashcard>)

    @Delete
    suspend fun deleteFlashcard(card: Flashcard)

    // Advanced Workouts (Overcoming Gravity)
    @Transaction
    @Query("SELECT * FROM gauntlets WHERE id = :gauntletId")
    fun getAdvancedWorkout(gauntletId: Int): Flow<GauntletWithAdvancedWorkout?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutBlock(block: WorkoutBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutExercise(exercise: WorkoutExercise): Long

    @Query("DELETE FROM workout_blocks WHERE gauntletId = :gauntletId")
    suspend fun deleteBlocksForGauntlet(gauntletId: Int)
}
