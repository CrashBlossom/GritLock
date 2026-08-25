package com.example.fitlock.data

import kotlinx.coroutines.flow.Flow

/**
 * GritLockRepository is the "Single Source of Truth" for all data.
 * It sits between the UI (MainActivity/ViewModels) and the Database (GritLockDao).
 * New coders: Always add new data logic here instead of directly in the UI.
 */
class GritLockRepository(private val dao: GritLockDao) {

    val allGroups: Flow<List<AppGroup>> = dao.getAllGroups()
    val userStats: Flow<UserStats?> = dao.getUserStats()
    val allChallenges: Flow<List<Challenge>> = dao.getChallenges()
    val allVaultItems: Flow<List<VaultItem>> = dao.getAllVaultItems()
    val allGauntlets: Flow<List<GauntletWithHabits>> = dao.getAllGauntletsWithHabits()
    val gauntletHistory: Flow<List<GauntletHistory>> = dao.getGauntletHistory()
    val allPledges: Flow<List<DailyPledge>> = dao.getAllPledges()

    suspend fun insertWorkout(workout: WorkoutHistory) = dao.insertWorkout(workout)
    suspend fun updateStats(stats: UserStats) = dao.updateUserStats(stats)
    suspend fun insertUrgeEvent(event: UrgeEvent) = dao.insertUrgeEvent(event)
    suspend fun insertLogNote(note: DailyLogNote) = dao.insertLogNote(note)
    
    // Pledge Management
    suspend fun getPledgeForDate(date: String) = dao.getPledgeForDate(date)
    suspend fun upsertPledge(pledge: DailyPledge) = dao.upsertPledge(pledge)

    // Motivational Quotes
    val allQuotes: Flow<List<MotivationalQuote>> = dao.getAllQuotes()
    suspend fun upsertQuote(quote: MotivationalQuote) = dao.upsertQuote(quote)

    // Gauntlet Management
    suspend fun upsertGauntlet(gauntlet: Gauntlet) = dao.upsertGauntlet(gauntlet)
    suspend fun deleteGauntlet(gauntlet: Gauntlet) = dao.deleteGauntlet(gauntlet)
    suspend fun upsertHabit(habit: Habit) = dao.upsertHabit(habit)
    suspend fun deleteHabitsForGauntlet(gauntletId: Int) = dao.deleteHabitsForGauntlet(gauntletId)

    // Vault Management
    suspend fun upsertVaultItem(item: VaultItem) = dao.upsertVaultItem(item)
    suspend fun deleteVaultItem(item: VaultItem) = dao.deleteVaultItem(item)

    // Usage Baselines
    val allBaselines: Flow<List<UsageBaseline>> = dao.getAllBaselines()
    suspend fun upsertBaseline(baseline: UsageBaseline) = dao.upsertBaseline(baseline)
    suspend fun deleteBaseline(baseline: UsageBaseline) = dao.deleteBaseline(baseline)
    
    // Group Management
    suspend fun upsertGroup(group: AppGroup) = if (group.id == 0) dao.insertGroup(group) else dao.updateGroup(group)
    suspend fun deleteGroup(group: AppGroup) = dao.deleteGroup(group)

    // Flashcards & Advanced Training
    val allFlashcards: Flow<List<Flashcard>> = dao.getAllFlashcards()
    fun getFlashcardsByDeck(deckName: String): Flow<List<Flashcard>> = dao.getFlashcardsByDeck(deckName)
    val allDeckNames: Flow<List<String>> = dao.getAllDeckNames()

    suspend fun deleteFlashcardsByDeck(deckName: String) = dao.deleteFlashcardsByDeck(deckName)

    suspend fun upsertFlashcard(card: Flashcard) = dao.upsertFlashcard(card)
    suspend fun upsertFlashcards(cards: List<Flashcard>) = dao.upsertFlashcards(cards)
    suspend fun deleteFlashcard(card: Flashcard) = dao.deleteFlashcard(card)

    suspend fun upsertWorkoutBlock(block: WorkoutBlockEntity) = dao.upsertWorkoutBlock(block)
    suspend fun upsertWorkoutExercise(exercise: WorkoutExercise) = dao.upsertWorkoutExercise(exercise)
    suspend fun deleteBlocksForGauntlet(gauntletId: Int) = dao.deleteBlocksForGauntlet(gauntletId)
}
