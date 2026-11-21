package com.example.fitlock.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class EscalationLevel(
    val hourThreshold: Int,
    val titlePrefix: String,
    val urgencyLabel: String
) {
    MIDDAY_GENTLE(12, "☀️ Midday Check-In", "Gentle Nudge"),
    AFTERNOON_NUDGE(15, "⚡ Afternoon Boost", "Firm Reminder"),
    EVENING_WARNING(18, "🌆 Evening Warning", "Stern Warning"),
    NIGHT_IRRITATED(20, "😠 Duolingo & Anki Alert", "Irritated"),
    HIGH_EMERGENCY(22, "🚨 CRITICAL STREAK DANGER", "High Emergency"),
    FINAL_COUNTDOWN(23, "🔥 FINAL COUNTDOWN", "Streak Break Imminent");

    companion object {
        fun getForHour(hour: Int): EscalationLevel {
            return when {
                hour >= 23 -> FINAL_COUNTDOWN
                hour >= 22 -> HIGH_EMERGENCY
                hour >= 20 -> NIGHT_IRRITATED
                hour >= 18 -> EVENING_WARNING
                hour >= 15 -> AFTERNOON_NUDGE
                else -> MIDDAY_GENTLE
            }
        }
    }
}

enum class MatrixGoalCategory {
    EXERCISE,
    APP_USAGE,
    HABIT_ROUTINE
}

data class MonitoredAppTarget(
    val packageName: String,
    val appName: String,
    val targetMinutes: Int = 15
)

data class MatrixGoalConfig(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val category: MatrixGoalCategory,
    val exerciseTypeOrKey: String,
    val minTarget: Int,
    val goldTarget: Int,
    val unit: String = "reps"
)

fun getExerciseEmoji(type: String, customIcons: Map<String, String>? = null): String {
    if (customIcons != null) {
        customIcons[type]?.let { if (it.isNotBlank()) return it }
        customIcons[type.uppercase()]?.let { if (it.isNotBlank()) return it }
    }
    return when (type.uppercase()) {
        "PUSHUP" -> "💪"
        "SQUAT" -> "🦵"
        "SITUP" -> "🧘"
        "PLANK" -> "⏱️"
        "BREATHWORK" -> "🫁"
        "ROWING", "ERG" -> "🚣"
        "PULLUP" -> "🏋️"
        "DIP" -> "🦾"
        "HINGE" -> "🍑"
        "STEPS" -> "👟"
        "WATER" -> "💧"
        "MORNING_JOURNAL" -> "☀️"
        "EVENING_REVIEW" -> "🌙"
        "DAILY_PLEDGE" -> "🔥"
        "COM.ICHI2.ANKI", "NET.ANKIWEB.ANKI", "ANKI" -> "🎴"
        "COM.DUOLINGO", "DUOLINGO" -> "🦉"
        else -> "🎯"
    }
}

val defaultMonitoredApps = listOf(
    MonitoredAppTarget("com.duolingo", "Duolingo", 15),
    MonitoredAppTarget("com.ichi2.anki", "AnkiDroid", 10)
)

val defaultMatrixGoals = listOf(
    MatrixGoalConfig(id = "pushups", label = "💪 Pushups", category = MatrixGoalCategory.EXERCISE, exerciseTypeOrKey = "PUSHUP", minTarget = 15, goldTarget = 30, unit = "reps"),
    MatrixGoalConfig(id = "squats", label = "🦵 Squats", category = MatrixGoalCategory.EXERCISE, exerciseTypeOrKey = "SQUAT", minTarget = 20, goldTarget = 40, unit = "reps"),
    MatrixGoalConfig(id = "situps", label = "🧘 Situps", category = MatrixGoalCategory.EXERCISE, exerciseTypeOrKey = "SITUP", minTarget = 20, goldTarget = 40, unit = "reps"),
    MatrixGoalConfig(id = "plank", label = "⏱️ Plank", category = MatrixGoalCategory.EXERCISE, exerciseTypeOrKey = "PLANK", minTarget = 60, goldTarget = 120, unit = "seconds"),
    MatrixGoalConfig(id = "duolingo", label = "🦉 Duolingo", category = MatrixGoalCategory.APP_USAGE, exerciseTypeOrKey = "com.duolingo", minTarget = 15, goldTarget = 30, unit = "minutes"),
    MatrixGoalConfig(id = "anki", label = "🎴 AnkiDroid", category = MatrixGoalCategory.APP_USAGE, exerciseTypeOrKey = "com.ichi2.anki", minTarget = 10, goldTarget = 20, unit = "minutes"),
    MatrixGoalConfig(id = "morn_journal", label = "☀️ Morn. Journal", category = MatrixGoalCategory.HABIT_ROUTINE, exerciseTypeOrKey = "MORNING_JOURNAL", minTarget = 1, goldTarget = 1, unit = "done"),
    MatrixGoalConfig(id = "eve_review", label = "🌙 Eve. Review", category = MatrixGoalCategory.HABIT_ROUTINE, exerciseTypeOrKey = "EVENING_REVIEW", minTarget = 1, goldTarget = 1, unit = "done")
)

data class EmergencyReminderConfig(
    val isEnabled: Boolean = true,
    val monitoredApps: List<MonitoredAppTarget> = defaultMonitoredApps,
    val matrixGoals: List<MatrixGoalConfig> = defaultMatrixGoals,
    val customIcons: Map<String, String> = emptyMap(),
    val hostedWidgetIds: List<Int> = emptyList(),
    val widgetHeights: Map<String, Int> = emptyMap(),
    val selectedExercises: List<String> = listOf("PUSHUP", "SQUAT", "SITUP", "PLANK"),
    val selectedAppPackages: List<String> = listOf("com.duolingo", "com.ichi2.anki", "net.ankiweb.anki"),
    val exerciseTargets: Map<String, Int> = mapOf(
        "PUSHUP" to 15,
        "SQUAT" to 20,
        "SITUP" to 20,
        "PLANK" to 60
    ),
    val appTargetsMinutes: Map<String, Int> = mapOf(
        "com.duolingo" to 15,
        "com.ichi2.anki" to 10
    ),
    val ankiCardsTarget: Int = 10,
    val lastNotificationHourSentToday: Int = -1,
    val lastNotificationDateSentToday: String = ""
)

class EmergencyReminderPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fitlock_emergency_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getConfig(): EmergencyReminderConfig {
        val json = prefs.getString("config_json", null) ?: return EmergencyReminderConfig()
        return try {
            val parsed = gson.fromJson(json, EmergencyReminderConfig::class.java)
            if (parsed?.matrixGoals.isNullOrEmpty()) {
                parsed?.copy(matrixGoals = defaultMatrixGoals) ?: EmergencyReminderConfig()
            } else {
                parsed
            }
        } catch (e: Exception) {
            EmergencyReminderConfig()
        }
    }

    fun saveConfig(config: EmergencyReminderConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString("config_json", json).apply()
    }

    fun recordNotificationSent(hour: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val current = getConfig()
        saveConfig(
            current.copy(
                lastNotificationHourSentToday = hour,
                lastNotificationDateSentToday = today
            )
        )
    }

    fun shouldSendForHour(hour: Int): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val current = getConfig()
        if (!current.isEnabled) return false
        if (current.lastNotificationDateSentToday == today && current.lastNotificationHourSentToday == hour) {
            return false // Already sent notification for this specific hour today
        }
        return true
    }
}

fun getAppUsageMinutesToday(context: Context, packageName: String): Int {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return getAppUsageMinutesForRange(context, packageName, calendar.timeInMillis, System.currentTimeMillis())
}

fun getAppUsageMinutesForRange(context: Context, packageName: String, startTime: Long, endTime: Long): Int {
    return try {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        
        // 1. Try queryAndAggregateUsageStats (Most reliable for active time range)
        var aggMinutes = 0
        try {
            val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            val match = statsMap[packageName]
                ?: statsMap.entries.find { it.key.equals(packageName, ignoreCase = true) || it.key.contains(packageName, ignoreCase = true) }?.value
            if (match != null && match.totalTimeInForeground > 0) {
                aggMinutes = (match.totalTimeInForeground / (1000 * 60)).toInt()
            }
        } catch (e: Exception) {}

        // 2. Try queryUsageStats (INTERVAL_DAILY)
        var foregroundMinutes = 0
        try {
            val stats = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            val match = stats.find { it.packageName.equals(packageName, ignoreCase = true) || it.packageName.contains(packageName, ignoreCase = true) }
            if (match != null && match.totalTimeInForeground > 0) {
                foregroundMinutes = (match.totalTimeInForeground / (1000 * 60)).toInt()
            }
        } catch (e: Exception) {}

        // 3. Query Events fallback
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()

        var totalTimeMs = 0L
        var lastResumedTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: ""
            if (pkg.equals(packageName, ignoreCase = true) || pkg.contains(packageName, ignoreCase = true)) {
                when (event.eventType) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastResumedTime = event.timeStamp
                    }
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                    android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (lastResumedTime > 0L) {
                            totalTimeMs += (event.timeStamp - lastResumedTime)
                            lastResumedTime = 0L
                        }
                    }
                }
            }
        }
        if (lastResumedTime > 0L) {
            totalTimeMs += (endTime - lastResumedTime)
        }
        val eventMinutes = (totalTimeMs / (1000 * 60)).toInt()
        maxOf(aggMinutes, foregroundMinutes, eventMinutes)
    } catch (e: Exception) {
        0
    }
}

fun calculateGoalStreak(
    context: Context,
    goal: MatrixGoalConfig,
    history: List<WorkoutHistory>,
    pledges: List<DailyPledge>
): Int {
    var streak = 0
    for (daysAgo in 0..365) {
        val dayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = dayCal.timeInMillis
        val endTime = startTime + 24 * 60 * 60 * 1000L
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dayCal.time)

        val dayHistory = history.filter { it.timestamp in startTime until endTime }
        val dayPledge = pledges.find { it.date == dateStr }

        val count = when (goal.exerciseTypeOrKey.uppercase()) {
            "PUSHUP" -> dayHistory.filter { it.exerciseType.equals("PUSHUP", ignoreCase = true) }.sumOf { it.repsCompleted }
            "SQUAT" -> dayHistory.filter { it.exerciseType.equals("SQUAT", ignoreCase = true) }.sumOf { it.repsCompleted }
            "FLASHCARDS", "ANKI" -> dayHistory.filter { it.exerciseType.equals("FLASHCARDS", ignoreCase = true) || it.exerciseType.contains("ANKI", ignoreCase = true) }.sumOf { it.repsCompleted }
            "ROWING", "ERG" -> dayHistory.filter { it.exerciseType.equals("ROWING", ignoreCase = true) || it.exerciseType.contains("ERG", ignoreCase = true) }.sumOf { it.repsCompleted }
            "MORNING_JOURNAL" -> if (dayPledge?.pledgeTimestamp != null || dayPledge?.status == "COMMITTED" || dayPledge?.status == "SUCCESS") 1 else 0
            "EVENING_REVIEW" -> if (dayPledge?.reviewTimestamp != null || dayPledge?.eveningReflection?.isNotBlank() == true || dayPledge?.status == "SUCCESS") 1 else 0
            else -> {
                if (goal.category == MatrixGoalCategory.APP_USAGE) {
                    getAppUsageMinutesForRange(context, goal.exerciseTypeOrKey, startTime, endTime)
                } else {
                    dayHistory.filter { it.exerciseType.equals(goal.exerciseTypeOrKey, ignoreCase = true) }.sumOf { it.repsCompleted }
                }
            }
        }

        if (count >= goal.minTarget) {
            streak++
        } else {
            if (daysAgo == 0) {
                // Today not met yet, continue checking past days
                continue
            } else {
                break
            }
        }
    }
    return streak
}


