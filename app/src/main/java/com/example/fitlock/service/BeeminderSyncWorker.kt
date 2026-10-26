package com.example.fitlock.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitlock.data.BeeminderConfig
import com.example.fitlock.data.BeeminderGoalMapping
import com.example.fitlock.data.BeeminderMetricType
import com.example.fitlock.data.BeeminderPreferences
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.getAppUsageMinutesToday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

class BeeminderSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = BeeminderPreferences(applicationContext)
        val config = prefs.getConfig()

        if (!config.isEnabled || config.username.isBlank() || config.authToken.isBlank() || config.mappings.isEmpty()) {
            return Result.success()
        }

        val success = executeSyncAll(applicationContext, config)
        return if (success) Result.success() else Result.retry()
    }

    companion object {
        suspend fun syncNow(context: Context, config: BeeminderConfig): Pair<Boolean, String> {
            return try {
                val success = executeSyncAll(context, config)
                if (success) Pair(true, "Successfully synced all Beeminder goal mappings!") else Pair(false, "Some or all Beeminder syncs failed. Check goal slugs.")
            } catch (e: Exception) {
                Pair(false, "Error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }

        private suspend fun executeSyncAll(context: Context, config: BeeminderConfig): Boolean = withContext(Dispatchers.IO) {
            if (config.username.isBlank() || config.authToken.isBlank() || config.mappings.isEmpty()) {
                return@withContext false
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val db = GritLockDatabase.getDatabase(context)
            val history = db.dao().getHistory().first().filter { it.timestamp in startTime until endTime }

            var allSuccess = true

            for (mapping in config.mappings) {
                if (mapping.goalSlug.isBlank()) continue

                var value = 1.0
                var comment = "FitLock Sync"

                when (mapping.metricType) {
                    BeeminderMetricType.WORKOUT_REPS -> {
                        val reps = history
                            .filter { hist -> mapping.selectedKeys.any { key -> hist.exerciseType.equals(key, ignoreCase = true) } }
                            .sumOf { it.repsCompleted }
                        value = reps.toDouble()
                        comment = "FitLock: $reps reps for [${mapping.selectedKeys.joinToString(", ")}]"
                    }
                    BeeminderMetricType.APP_USAGE -> {
                        val totalMins = mapping.selectedKeys.sumOf { pkg -> getAppUsageMinutesToday(context, pkg) }
                        value = totalMins.toDouble()
                        comment = "FitLock: $totalMins mins on apps [${mapping.selectedKeys.joinToString(", ")}]"
                    }
                    BeeminderMetricType.STREAK_COMPLETION -> {
                        val stats = db.dao().getUserStats().first()
                        val metToday = stats?.floorTargetMetToday == true
                        value = if (metToday) 1.0 else 0.0
                        comment = "FitLock daily streak goal met: $metToday"
                    }
                    BeeminderMetricType.DAILY_NOTE -> {
                        val notes = db.dao().getAllDailyLogNotesList()
                        val fullContent = notes.joinToString("\n\n") { it.content }
                        value = 1.0
                        comment = if (fullContent.isNotBlank()) fullContent else "FitLock Daily Reflection"
                    }
                }

                val posted = postDatapoint(config.username, mapping.goalSlug, config.authToken, value, comment)
                if (!posted) allSuccess = false
            }

            return@withContext allSuccess
        }

        private fun postDatapoint(username: String, goalSlug: String, authToken: String, value: Double, comment: String): Boolean {
            return try {
                val urlString = "https://www.beeminder.com/api/v1/users/$username/goals/$goalSlug/datapoints.json"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val timestamp = System.currentTimeMillis() / 1000L
                val postData = StringBuilder().apply {
                    append("auth_token=").append(URLEncoder.encode(authToken, "UTF-8"))
                    append("&value=").append(URLEncoder.encode(value.toString(), "UTF-8"))
                    append("&comment=").append(URLEncoder.encode(comment, "UTF-8"))
                    append("&timestamp=").append(URLEncoder.encode(timestamp.toString(), "UTF-8"))
                }.toString()

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                responseCode in 200..299
            } catch (e: Exception) {
                false
            }
        }
    }
}
