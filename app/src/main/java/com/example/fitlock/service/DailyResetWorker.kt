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
        
        // 2. Carry over unfinished tasks to today's pool
        // We find any DAILY_COMMITMENT quest from "previous days" that wasn't finished
        // Actually, just find ALL unfinished blocks of type TASK that are part of a DAILY_COMMITMENT
        // and re-assign them to today's planning pool (which we will create if it doesn't exist)
        
        val unfinishedTasks = dao.getUnfinishedTasks().first()
        val today = LocalDate.now().toString()
        
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
