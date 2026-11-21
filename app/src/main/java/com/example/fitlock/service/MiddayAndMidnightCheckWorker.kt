package com.example.fitlock.service

import android.content.Context
import androidx.work.*
import com.example.fitlock.ai.SystemPromptEngine
import com.example.fitlock.ai.SystemVoiceSynthesizer
import com.example.fitlock.data.EmergencyReminderPreferences
import com.example.fitlock.data.GritLockDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MiddayAndMidnightCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fitlock_penalty_prefs", Context.MODE_PRIVATE)
        val emergencyPrefs = EmergencyReminderPreferences(applicationContext)
        val config = emergencyPrefs.getConfig()

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Only evaluate at 13:00 (1 PM) or 00:00 (Midnight)
        if (currentHour != 13 && currentHour != 0) {
            return Result.success()
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val db = GritLockDatabase.getDatabase(applicationContext)
        val history = db.dao().getHistory().first().filter { it.timestamp in startTime until endTime }

        // Check if minimum matrix goals are met
        var allMet = true
        for (goal in config.matrixGoals) {
            val completed = when (goal.category) {
                com.example.fitlock.data.MatrixGoalCategory.APP_USAGE -> {
                    com.example.fitlock.data.getAppUsageMinutesToday(applicationContext, goal.exerciseTypeOrKey)
                }
                else -> {
                    history.filter { it.exerciseType.equals(goal.exerciseTypeOrKey, ignoreCase = true) }.sumOf { it.repsCompleted }
                }
            }
            if (completed < goal.minTarget) {
                allMet = false
                break
            }
        }

        // Also check Epic Gauntlet Challenge check (100 pushups, 100 squats, 100 situps, 10m plank)
        val totalPushups = history.filter { it.exerciseType.equals("PUSHUP", ignoreCase = true) }.sumOf { it.repsCompleted }
        val totalSquats = history.filter { it.exerciseType.equals("SQUAT", ignoreCase = true) }.sumOf { it.repsCompleted }
        val totalSitups = history.filter { it.exerciseType.equals("SITUP", ignoreCase = true) }.sumOf { it.repsCompleted }
        val totalPlank = history.filter { it.exerciseType.equals("PLANK", ignoreCase = true) }.sumOf { it.repsCompleted }
        val isEpicGauntletComplete = totalPushups >= 100 && totalSquats >= 100 && totalSitups >= 100 && totalPlank >= 600

        val currentDelay = prefs.getInt("penalty_delay_seconds", 0)
        val extremePenaltyEnabled = prefs.getBoolean("extreme_penalty_gate_enabled", false)
        val extremePenaltyGroupId = prefs.getInt("extreme_penalty_group_id", -1)

        if (isEpicGauntletComplete || allMet) {
            // Full penalty clearance when all minimal exercises/matrix goals are met!
            prefs.edit()
                .putInt("penalty_delay_seconds", 0)
                .putBoolean("grayscale_active", false)
                .apply()

            if (extremePenaltyEnabled && extremePenaltyGroupId != -1) {
                val group = db.dao().getGroupById(extremePenaltyGroupId)
                if (group != null) {
                    db.dao().updateGroup(group.copy(lastUnlockedTimestamp = System.currentTimeMillis()))
                }
            }
        } else {
            // 120s base penalty + 30s compounding escalation each missed checkpoint
            val newDelay = if (currentDelay < 120) 120 else currentDelay + 30
            prefs.edit()
                .putInt("penalty_delay_seconds", newDelay)
                .putBoolean("grayscale_active", true)
                .apply()

            if (extremePenaltyEnabled && extremePenaltyGroupId != -1) {
                val group = db.dao().getGroupById(extremePenaltyGroupId)
                if (group != null) {
                    db.dao().updateGroup(group.copy(isEnabled = true))
                }
            }

            // Speak AI warning
            val tts = SystemVoiceSynthesizer(applicationContext)
            tts.speak("Checkpoint failed. Penalty delay increased to $newDelay seconds. Execute your discipline quota immediately.")
        }

        return Result.success()
    }

    companion object {
        suspend fun evaluatePenaltiesNow(context: Context) {
            val prefs = context.getSharedPreferences("fitlock_penalty_prefs", Context.MODE_PRIVATE)
            val emergencyPrefs = EmergencyReminderPreferences(context)
            val config = emergencyPrefs.getConfig()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val db = GritLockDatabase.getDatabase(context)
            val history = db.dao().getHistory().first().filter { it.timestamp in startTime until endTime }

            var allMet = true
            for (goal in config.matrixGoals) {
                val completed = when (goal.category) {
                    com.example.fitlock.data.MatrixGoalCategory.APP_USAGE -> {
                        com.example.fitlock.data.getAppUsageMinutesToday(context, goal.exerciseTypeOrKey)
                    }
                    else -> {
                        history.filter { it.exerciseType.equals(goal.exerciseTypeOrKey, ignoreCase = true) }.sumOf { it.repsCompleted }
                    }
                }
                if (completed < goal.minTarget) {
                    allMet = false
                    break
                }
            }

            val currentDelay = prefs.getInt("penalty_delay_seconds", 0)
            if (allMet) {
                // Full penalty clearance when all minimal exercises/matrix goals are met
                prefs.edit()
                    .putInt("penalty_delay_seconds", 0)
                    .putBoolean("grayscale_active", false)
                    .apply()
            } else {
                // 120s base delay + 30s compounding escalation
                val newDelay = if (currentDelay < 120) 120 else currentDelay + 30
                prefs.edit()
                    .putInt("penalty_delay_seconds", newDelay)
                    .putBoolean("grayscale_active", true)
                    .apply()
            }
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MiddayAndMidnightCheckWorker>(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "midday_midnight_check_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
