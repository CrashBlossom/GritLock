package com.example.fitlock.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.fitlock.data.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

data class GritLockFullBackup(
    val version: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val userStats: UserStats? = null,
    val appGroups: List<AppGroup> = emptyList(),
    val gauntlets: List<Gauntlet> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitDefinitions: List<HabitDefinition> = emptyList(),
    val gauntletHistory: List<GauntletHistory> = emptyList(),
    val workoutHistory: List<WorkoutHistory> = emptyList(),
    val dailyPledges: List<DailyPledge> = emptyList(),
    val dailyLogNotes: List<DailyLogNote> = emptyList(),
    val urgeEvents: List<UrgeEvent> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val projects: List<Project> = emptyList(),
    val milestones: List<Milestone> = emptyList(),
    val projectTasks: List<ProjectTask> = emptyList(),
    val quests: List<Quest> = emptyList(),
    val questBlocks: List<QuestBlock> = emptyList(),
    val exerciseFamilies: List<ExerciseFamily> = emptyList(),
    val progressionLevels: List<ProgressionLevel> = emptyList(),
    val exerciseDefinitions: List<ExerciseDefinition> = emptyList(),
    val taskDefinitions: List<TaskDefinition> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val appRules: List<AppRule> = emptyList(),
    val vaultItems: List<VaultItem> = emptyList(),
    val quotes: List<MotivationalQuote> = emptyList(),
    val baselines: List<UsageBaseline> = emptyList(),
    val blockEvents: List<AppBlockEvent> = emptyList(),
    val countdowns: List<DateCountdown> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val workoutBlocks: List<WorkoutBlockEntity> = emptyList(),
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    val preferencesMap: Map<String, String> = emptyMap()
)

class DataBackupManager(private val context: Context) {

    private val db = GritLockDatabase.getDatabase(context)
    private val dao = db.dao()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackup(): GritLockFullBackup = withContext(Dispatchers.IO) {
        val userStats = dao.getUserStats().first()
        val appGroups = dao.getAllGroupsList()
        val gauntlets = dao.getAllGauntletsList()
        val habits = dao.getAllHabitsList()
        val habitDefinitions = dao.getAllHabitDefinitionsList()
        val gauntletHistory = dao.getAllHistoryList()
        val workoutHistory = dao.getAllWorkoutHistoryList()
        val dailyPledges = dao.getAllDailyPledgesList()
        val dailyLogNotes = dao.getAllDailyLogNotesList()
        val urgeEvents = dao.getAllUrgeEventsList()
        val goals = dao.getAllGoalsList()
        val projects = dao.getAllProjectsList()
        val milestones = dao.getAllMilestonesList()
        val projectTasks = dao.getAllProjectTasksList()
        val quests = dao.getAllQuestsList()
        val questBlocks = dao.getAllQuestBlocksList()
        val exerciseFamilies = dao.getAllExerciseFamiliesList()
        val progressionLevels = dao.getAllProgressionLevelsList()
        val exerciseDefinitions = dao.getAllExerciseDefinitionsList()
        val taskDefinitions = dao.getAllTaskDefinitionsList()
        val flashcards = dao.getAllFlashcardsList()
        val challenges = dao.getAllChallengesList()
        val appRules = dao.getAllAppRulesList()
        val vaultItems = dao.getAllVaultItemsList()
        val quotes = dao.getAllMotivationalQuotesList()
        val baselines = dao.getAllUsageBaselinesList()
        val blockEvents = dao.getAllAppBlockEventsList()
        val countdowns = dao.getAllDateCountdownsList()
        val achievements = dao.getAllAchievementsList()
        val workoutBlocks = dao.getAllWorkoutBlocksList()
        val workoutExercises = dao.getAllWorkoutExercisesList()

        // Read all SharedPreferences entries
        val prefs = context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val prefsMap = prefs.all.mapValues { it.value?.toString() ?: "" }

        GritLockFullBackup(
            version = 1,
            exportTimestamp = System.currentTimeMillis(),
            userStats = userStats,
            appGroups = appGroups,
            gauntlets = gauntlets,
            habits = habits,
            habitDefinitions = habitDefinitions,
            gauntletHistory = gauntletHistory,
            workoutHistory = workoutHistory,
            dailyPledges = dailyPledges,
            dailyLogNotes = dailyLogNotes,
            urgeEvents = urgeEvents,
            goals = goals,
            projects = projects,
            milestones = milestones,
            projectTasks = projectTasks,
            quests = quests,
            questBlocks = questBlocks,
            exerciseFamilies = exerciseFamilies,
            progressionLevels = progressionLevels,
            exerciseDefinitions = exerciseDefinitions,
            taskDefinitions = taskDefinitions,
            flashcards = flashcards,
            challenges = challenges,
            appRules = appRules,
            vaultItems = vaultItems,
            quotes = quotes,
            baselines = baselines,
            blockEvents = blockEvents,
            countdowns = countdowns,
            achievements = achievements,
            workoutBlocks = workoutBlocks,
            workoutExercises = workoutExercises,
            preferencesMap = prefsMap
        )
    }

    suspend fun exportToFile(uri: Uri): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val backup = createBackup()
            val jsonString = gson.toJson(backup)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val summary = "Exported ${backup.appGroups.size} Gates, ${backup.workoutHistory.size} Workouts, ${backup.dailyPledges.size} Pledges, ${backup.flashcards.size} Flashcards at ${dateFormat.format(Date(backup.exportTimestamp))}."
            Pair(true, summary)
        } catch (e: Exception) {
            Log.e("DataBackupManager", "Export failed: ${e.message}", e)
            Pair(false, "Export failed: ${e.localizedMessage}")
        }
    }

    suspend fun importFromFile(uri: Uri): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Pair(false, "Could not open backup file.")

            val backup = gson.fromJson(jsonString, GritLockFullBackup::class.java)
                ?: return@withContext Pair(false, "Invalid or corrupt backup JSON file.")

            // Restore Room Database Records
            backup.userStats?.let { dao.updateUserStats(it) }
            if (backup.appGroups.isNotEmpty()) dao.insertAppGroups(backup.appGroups)
            if (backup.gauntlets.isNotEmpty()) dao.insertGauntlets(backup.gauntlets)
            if (backup.habits.isNotEmpty()) dao.insertHabits(backup.habits)
            if (backup.habitDefinitions.isNotEmpty()) dao.insertHabitDefinitions(backup.habitDefinitions)
            if (backup.workoutHistory.isNotEmpty()) dao.insertWorkouts(backup.workoutHistory)
            if (backup.dailyPledges.isNotEmpty()) dao.insertPledges(backup.dailyPledges)
            if (backup.dailyLogNotes.isNotEmpty()) dao.insertLogNotes(backup.dailyLogNotes)
            if (backup.urgeEvents.isNotEmpty()) dao.insertUrgeEvents(backup.urgeEvents)
            if (backup.goals.isNotEmpty()) dao.insertGoals(backup.goals)
            if (backup.projects.isNotEmpty()) dao.insertProjects(backup.projects)
            if (backup.milestones.isNotEmpty()) dao.insertMilestones(backup.milestones)
            if (backup.projectTasks.isNotEmpty()) dao.insertProjectTasks(backup.projectTasks)
            if (backup.quests.isNotEmpty()) dao.insertQuests(backup.quests)
            if (backup.questBlocks.isNotEmpty()) dao.upsertQuestBlocks(backup.questBlocks)
            if (backup.exerciseFamilies.isNotEmpty()) dao.insertExerciseFamilies(backup.exerciseFamilies)
            if (backup.progressionLevels.isNotEmpty()) dao.insertProgressionLevels(backup.progressionLevels)
            if (backup.exerciseDefinitions.isNotEmpty()) dao.insertExerciseDefinitions(backup.exerciseDefinitions)
            if (backup.taskDefinitions.isNotEmpty()) dao.insertTaskDefinitions(backup.taskDefinitions)
            if (backup.flashcards.isNotEmpty()) dao.upsertFlashcards(backup.flashcards)
            if (backup.challenges.isNotEmpty()) dao.insertChallenges(backup.challenges)
            if (backup.appRules.isNotEmpty()) dao.insertAppRules(backup.appRules)
            if (backup.vaultItems.isNotEmpty()) dao.insertVaultItems(backup.vaultItems)
            if (backup.quotes.isNotEmpty()) dao.insertQuotes(backup.quotes)
            if (backup.baselines.isNotEmpty()) dao.insertBaselines(backup.baselines)
            if (backup.blockEvents.isNotEmpty()) dao.insertBlockEvents(backup.blockEvents)
            if (backup.countdowns.isNotEmpty()) {
                backup.countdowns.forEach { dao.upsertCountdown(it) }
            }
            if (backup.achievements.isNotEmpty()) dao.upsertAchievements(backup.achievements)
            if (backup.workoutBlocks.isNotEmpty()) dao.insertWorkoutBlocks(backup.workoutBlocks)
            if (backup.workoutExercises.isNotEmpty()) dao.insertWorkoutExercises(backup.workoutExercises)

            // Restore SharedPreferences
            if (backup.preferencesMap.isNotEmpty()) {
                val editor = context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE).edit()
                backup.preferencesMap.forEach { (key, value) ->
                    // Attempt type detection for int/boolean strings
                    val intVal = value.toIntOrNull()
                    val boolVal = value.toBooleanStrictOrNull()
                    when {
                        intVal != null -> editor.putInt(key, intVal)
                        boolVal != null -> editor.putBoolean(key, boolVal)
                        else -> editor.putString(key, value)
                    }
                }
                editor.apply()
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val summary = "Successfully restored ${backup.appGroups.size} Protocol Gates, ${backup.workoutHistory.size} Workouts, ${backup.dailyPledges.size} Pledges, ${backup.flashcards.size} Flashcards, Level ${backup.userStats?.level ?: 1} Stats from ${dateFormat.format(Date(backup.exportTimestamp))}."
            
            // Trigger Widget refresh
            WidgetUpdater.updateAllWidgets(context)

            Pair(true, summary)
        } catch (e: Exception) {
            Log.e("DataBackupManager", "Import failed: ${e.message}", e)
            Pair(false, "Import failed: ${e.localizedMessage}")
        }
    }
}
