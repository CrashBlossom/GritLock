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

fun getExerciseEmoji(type: String): String {
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
        val stats = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val stat = stats.find { it.packageName == packageName }
        if (stat != null) (stat.totalTimeInForeground / (1000 * 60)).toInt() else 0
    } catch (e: Exception) {
        0
    }
}

