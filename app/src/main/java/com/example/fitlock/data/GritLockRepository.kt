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
    val allAdvancedWorkouts: Flow<List<GauntletWithAdvancedWorkout>> = dao.getAllGauntletsWithAdvancedWorkouts()
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

    // Habit Library
    val allHabitDefinitions: Flow<List<HabitDefinition>> = dao.getAllHabitDefinitions()
    suspend fun upsertHabitDefinition(definition: HabitDefinition) = dao.upsertHabitDefinition(definition)
    suspend fun deleteHabitDefinition(definition: HabitDefinition) = dao.deleteHabitDefinition(definition)
    suspend fun getHabitDefinitionById(id: Int) = dao.getHabitDefinitionById(id)

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

    // Quest System
    val allQuests: Flow<List<QuestWithBlocks>> = dao.getAllQuestsWithBlocks()
    fun getQuestById(id: String): Flow<QuestWithBlocks?> = dao.getQuestWithBlocksById(id)
    suspend fun upsertQuest(quest: Quest) = dao.upsertQuest(quest)
    suspend fun deleteQuest(quest: Quest) = dao.deleteQuest(quest)
    suspend fun upsertQuestBlock(block: QuestBlock) = dao.upsertQuestBlock(block)
    suspend fun upsertQuestBlocks(blocks: List<QuestBlock>) = dao.upsertQuestBlocks(blocks)
    suspend fun deleteBlocksForQuest(questId: String) = dao.deleteBlocksForQuest(questId)
    val unfinishedTasks: Flow<List<QuestBlock>> = dao.getUnfinishedTasks()

    // Forge Management
    val allExerciseDefinitions: Flow<List<ExerciseDefinition>> = dao.getAllExerciseDefinitions()
    suspend fun upsertExerciseDefinition(definition: ExerciseDefinition) = dao.upsertExerciseDefinition(definition)
    suspend fun deleteExerciseDefinition(definition: ExerciseDefinition) = dao.deleteExerciseDefinition(definition)

    val allTaskDefinitions: Flow<List<TaskDefinition>> = dao.getAllTaskDefinitions()
    suspend fun upsertTaskDefinition(definition: TaskDefinition) = dao.upsertTaskDefinition(definition)
    suspend fun deleteTaskDefinition(definition: TaskDefinition) = dao.deleteTaskDefinition(definition)

    suspend fun saveGauntletAsQuest(gauntlet: Gauntlet, habits: List<Habit>, blocks: List<WorkoutBlockWithExercises>, tasks: List<String> = emptyList()) {
        val questId = if (gauntlet.id != 0) gauntlet.id.toString() else java.util.UUID.randomUUID().toString()
        val quest = Quest(
            id = questId,
            name = gauntlet.name,
            type = QuestType.ROUTINE,
            triggerType = gauntlet.triggerType,
            triggerTime = gauntlet.triggerTime,
            physicalTriggerType = gauntlet.physicalTriggerType,
            physicalTriggerData = gauntlet.physicalTriggerData,
            isLockShieldEnabled = gauntlet.targetBlockGroupId != null,
            targetBlockGroupId = gauntlet.targetBlockGroupId,
            unlockAtEndOnly = true,
            icon = gauntlet.icon
        )
        dao.upsertQuest(quest)
        dao.deleteBlocksForQuest(questId)
        
        val questBlocks = mutableListOf<QuestBlock>()
        
        // Add Habits
        habits.forEach { h ->
            questBlocks.add(QuestBlock(
                id = if (h.id != 0) h.id.toString() else java.util.UUID.randomUUID().toString(),
                questId = questId,
                type = BlockType.HABIT,
                orderIndex = h.orderIndex,
                name = h.name,
                definitionId = h.definitionId,
                estimatedDurationSeconds = h.estimatedDurationSeconds,
                subHabits = h.subHabits,
                mediaUrl = h.mediaUrl,
                mediaType = h.mediaType,
                trackingType = h.trackingType,
                currentStreak = h.currentStreak,
                lastDoneTimestamp = h.lastCompletionTimestamp,
                higherIsBetter = h.higherIsBetter
            ))
        }
        
        // Add Workout Blocks
        blocks.forEach { b ->
            b.exercises.forEach { e ->
                questBlocks.add(QuestBlock(
                    id = "ex_${e.id}_${java.util.UUID.randomUUID().toString().take(8)}",
                    questId = questId,
                    type = BlockType.EXERCISE,
                    orderIndex = b.block.orderIndex * 100 + e.orderIndex,
                    name = e.name,
                    sets = e.sets,
                    targetReps = e.targetReps,
                    restBetweenSets = e.restBetweenSets,
                    trackingType = HabitTrackingType.REPS,
                    higherIsBetter = true
                ))
            }
        }

        // Add Tasks
        tasks.forEachIndexed { index, t ->
            questBlocks.add(QuestBlock(
                id = "task_${java.util.UUID.randomUUID()}",
                questId = questId,
                type = BlockType.TASK,
                orderIndex = index + 1000,
                name = t,
                autoCarryOver = true
            ))
        }
        
        dao.upsertQuestBlocks(questBlocks)
    }

    // Atlas Management
    val allGoals: Flow<List<GoalWithProjects>> = dao.getAllGoalsWithProjects()
    suspend fun upsertGoal(goal: Goal) = dao.upsertGoal(goal)
    suspend fun deleteGoal(goal: Goal) = dao.deleteGoal(goal)
    suspend fun upsertProject(project: Project) = dao.upsertProject(project)
    suspend fun deleteProject(project: Project) = dao.deleteProject(project)
    suspend fun upsertMilestone(milestone: Milestone) = dao.upsertMilestone(milestone)
    suspend fun deleteMilestone(milestone: Milestone) = dao.deleteMilestone(milestone)
    suspend fun upsertProjectTask(task: ProjectTask) = dao.upsertProjectTask(task)
    suspend fun deleteProjectTask(task: ProjectTask) = dao.deleteProjectTask(task)
    val unfinishedProjectTasks: Flow<List<ProjectTask>> = dao.getUnfinishedProjectTasks()

    suspend fun createDailyQuest(blocks: List<QuestBlock>, isLockShieldEnabled: Boolean) {
        val today = java.time.LocalDate.now().toString()
        val questId = "daily_$today"
        
        val quest = Quest(
            id = questId,
            name = "Daily Quest: $today",
            type = QuestType.DAILY_COMMITMENT,
            isLockShieldEnabled = isLockShieldEnabled,
            targetBlockGroupId = null, // TODO: Allow user to select group in planning
            unlockAtEndOnly = true
        )
        
        dao.upsertQuest(quest)
        
        val preparedBlocks = blocks.mapIndexed { index, block ->
            block.copy(
                id = if (block.type == BlockType.TASK && block.id.startsWith("daily_")) block.id else java.util.UUID.randomUUID().toString(),
                questId = questId,
                orderIndex = index,
                isCompleted = false
            )
        }
        
        dao.upsertQuestBlocks(preparedBlocks)
        
        // Update Pledge
        val pledge = dao.getPledgeForDate(today) ?: DailyPledge(date = today)
        dao.upsertPledge(pledge.copy(status = "COMMITTED", pledgeTimestamp = System.currentTimeMillis()))
    }

    // Progression System
    val allFamilies: Flow<List<FamilyWithLevels>> = dao.getAllFamiliesWithLevels()
    suspend fun getLevelData(familyId: String, level: Int) = dao.getLevelData(familyId, level)
}
