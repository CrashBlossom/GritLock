package com.example.fitlock.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.util.UUID

enum class BeeminderMetricType {
    WORKOUT_REPS,
    APP_USAGE,
    STREAK_COMPLETION,
    DAILY_NOTE
}

data class BeeminderGoalMapping(
    val id: String = UUID.randomUUID().toString(),
    val goalSlug: String,
    val metricType: BeeminderMetricType,
    val selectedKeys: List<String>
)

data class BeeminderConfig(
    val isEnabled: Boolean = false,
    val username: String = "",
    val authToken: String = "",
    val mappings: List<BeeminderGoalMapping> = emptyList()
)

class BeeminderPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fitlock_beeminder_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getConfig(): BeeminderConfig {
        val json = prefs.getString("config_json", null) ?: return BeeminderConfig()
        return try {
            gson.fromJson(json, BeeminderConfig::class.java) ?: BeeminderConfig()
        } catch (e: Exception) {
            BeeminderConfig()
        }
    }

    fun saveConfig(config: BeeminderConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString("config_json", json).apply()
    }
}
