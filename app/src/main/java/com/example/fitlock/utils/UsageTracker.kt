package com.example.fitlock.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.UsageBaseline
import kotlinx.coroutines.flow.first
import java.util.Calendar

class UsageTracker(private val context: Context) {

    private val db = GritLockDatabase.getDatabase(context)

    suspend fun calculateReclaimedMinutesToday(): Long {
        val baselines = db.dao().getAllBaselines().first()
        if (baselines.isEmpty()) return 0

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        var totalReclaimed = 0L

        for (baseline in baselines) {
            val actualMs = stats[baseline.packageName]?.totalTimeInForeground ?: 0L
            val actualMins = actualMs / 1000 / 60
            val reclaimed = (baseline.baselineMinutesPerDay - actualMins).coerceAtLeast(0)
            totalReclaimed += reclaimed
        }

        return totalReclaimed
    }

    suspend fun syncReclaimedTime() {
        val todayReclaimed = calculateReclaimedMinutesToday()
        val stats = db.dao().getUserStats().first()
        if (stats != null) {
            // This is a simple total, in a real app we'd track per day to avoid double counting if run multiple times
            // For now let's just update the total stat
            db.dao().updateUserStats(stats.copy(reclaimedMinutesTotal = stats.reclaimedMinutesTotal + todayReclaimed))
        }
    }
}
