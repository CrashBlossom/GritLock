package com.example.fitlock.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*

object UsageUtils {
    
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun getAppUsageStats(context: Context): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        
        return stats.map { usageStats ->
            AppUsageInfo(
                packageName = usageStats.packageName,
                totalTimeVisible = usageStats.totalTimeInForeground,
                lastTimeUsed = usageStats.lastTimeUsed,
                launchCount = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // UsageStats.getAppLaunchCount() is hidden before Q but available via reflection or specific APIs
                    // On most modern devices we can try to access it.
                    try {
                        val method = usageStats.javaClass.getMethod("getAppLaunchCount")
                        method.invoke(usageStats) as Int
                    } catch (e: Exception) {
                        0
                    }
                } else 0
            )
        }.filter { it.totalTimeVisible > 0 }
    }
}

data class AppUsageInfo(
    val packageName: String,
    val totalTimeVisible: Long,
    val lastTimeUsed: Long,
    val launchCount: Int = 0
)
