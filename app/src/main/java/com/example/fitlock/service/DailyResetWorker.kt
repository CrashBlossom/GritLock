package com.example.fitlock.service

import android.content.Context
import androidx.work.*
import com.example.fitlock.data.BlockType
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.QuestType
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class DailyResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = GritLockDatabase.getDatabase(applicationContext)
        val dao = db.dao()
        
        // 1. Reset completion status for all recurring routines
        dao.resetQuestCompletionStatus()
        dao.resetRecurringQuestBlocks()
        
        // 2. Midnight HP Decay & Streak Shielding
        val today = LocalDate.now().toString()
        val prefs = applicationContext.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val hadLateNightViolation = prefs.getBoolean("late_night_violation_occurred", false)
        prefs.edit().putBoolean("late_night_violation_occurred", false).apply()
        prefs.edit().putBoolean("bedtime_hangover_active", hadLateNightViolation).apply()

        val stats = dao.getUserStats().first()
        if (stats != null) {
            val now = System.currentTimeMillis()
            val isShieldActive = stats.floorTargetMetToday
            val hpGainOrLoss = if (isShieldActive) 10 else -25
            val hangoverDeduction = if (hadLateNightViolation) 30 else 0
            val newHp = (stats.currentHp + hpGainOrLoss - hangoverDeduction).coerceIn(0, stats.maxHp)
            
            val isBrokenShield = newHp == 0
            val updatedStreak = if (isBrokenShield) 0 else stats.currentStreak
            
            // Apply 1% Stat Atrophy for stats not trained in > 3 days
            val threeDaysMs = 3 * 24 * 60 * 60 * 1000L
            val updatedStr = if (now - (stats.statLastTrainedDates["STR"] ?: 0L) > threeDaysMs) (stats.strXp * 0.99).toLong() else stats.strXp
            val updatedAgi = if (now - (stats.statLastTrainedDates["AGI"] ?: 0L) > threeDaysMs) (stats.agiXp * 0.99).toLong() else stats.agiXp
            val updatedVit = if (now - (stats.statLastTrainedDates["VIT"] ?: 0L) > threeDaysMs) (stats.vitXp * 0.99).toLong() else stats.vitXp
            val updatedInt = if (now - (stats.statLastTrainedDates["INT"] ?: 0L) > threeDaysMs) (stats.intXp * 0.99).toLong() else stats.intXp
            val updatedSen = if (now - (stats.statLastTrainedDates["SEN"] ?: 0L) > threeDaysMs) (stats.senXp * 0.99).toLong() else stats.senXp
            val updatedCha = if (now - (stats.statLastTrainedDates["CHA"] ?: 0L) > threeDaysMs) (stats.chaXp * 0.99).toLong() else stats.chaXp

            dao.updateUserStats(
                stats.copy(
                    currentHp = newHp,
                    currentStreak = updatedStreak,
                    penaltyStateActive = isBrokenShield,
                    floorTargetMetToday = false,
                    lastFloorResetDate = today,
                    strXp = updatedStr,
                    agiXp = updatedAgi,
                    vitXp = updatedVit,
                    intXp = updatedInt,
                    senXp = updatedSen,
                    chaXp = updatedCha
                )
            )
        }

        // 3. Carry over unfinished tasks to today's pool
        val unfinishedTasks = dao.getUnfinishedTasks().first()
        
        // Find or create today's Daily Commitment quest
        var todayQuest = dao.getAllQuestsWithBlocks().first()
            .find { it.quest.type == QuestType.DAILY_COMMITMENT && it.quest.name.contains(today) }
            ?.quest
            
        if (todayQuest == null && unfinishedTasks.isNotEmpty()) {
            // If we have unfinished tasks but no today's quest yet, 
            // we'll let the "Planning Mode" UI handle the creation and linking.
            // For now, the worker just ensures the flags are ready.
        }

        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val resetRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateDelayToMidnight(), TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_reset",
                ExistingPeriodicWorkPolicy.KEEP,
                resetRequest
            )
        }

        private fun calculateDelayToMidnight(): Long {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis - now
        }
    }
}
