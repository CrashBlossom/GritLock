package com.example.fitlock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.fitlock.MainActivity
import com.example.fitlock.R
import com.example.fitlock.data.EmergencyReminderConfig
import com.example.fitlock.data.EmergencyReminderPreferences
import com.example.fitlock.data.EscalationLevel
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.MatrixGoalCategory
import com.example.fitlock.ui.MindfulSnoozeActivity
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class EmergencyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        com.example.fitlock.ui.checkAndNotifyAccessibilityService(applicationContext)

        val prefsManager = EmergencyReminderPreferences(applicationContext)
        val config = prefsManager.getConfig()

        if (!config.isEnabled) return Result.success()

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        if (!prefsManager.shouldSendForHour(currentHour)) {
            return Result.success()
        }

        // Calculate today's time range
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = startTime + 24 * 60 * 60 * 1000L

        val db = GritLockDatabase.getDatabase(applicationContext)
        val history = db.dao().getHistory().first().filter { it.timestamp in startTime until endTime }

        val missingTargets = mutableListOf<String>()

        // Check custom matrix goals
        for (goal in config.matrixGoals) {
            when (goal.category) {
                MatrixGoalCategory.EXERCISE -> {
                    val completed = history
                        .filter { it.exerciseType.equals(goal.exerciseTypeOrKey, ignoreCase = true) }
                        .sumOf { it.repsCompleted }
                    if (completed < goal.minTarget) {
                        val needed = goal.minTarget - completed
                        missingTargets.add("$needed ${goal.unit} ${goal.label}")
                    }
                }
                MatrixGoalCategory.APP_USAGE -> {
                    val usedMinutes = com.example.fitlock.data.getAppUsageMinutesToday(applicationContext, goal.exerciseTypeOrKey)
                    if (usedMinutes < goal.minTarget) {
                        val needed = goal.minTarget - usedMinutes
                        missingTargets.add("$needed min ${goal.label}")
                    }
                }
                MatrixGoalCategory.HABIT_ROUTINE -> {
                    // For routine habits (morning journal, eve review), check if completed today
                    val isDone = when (goal.exerciseTypeOrKey.uppercase()) {
                        "MORNING_JOURNAL" -> history.any { it.exerciseType.equals("MORNING_JOURNAL", ignoreCase = true) }
                        "EVENING_REVIEW" -> history.any { it.exerciseType.equals("EVENING_REVIEW", ignoreCase = true) }
                        else -> false
                    }
                    if (!isDone) {
                        missingTargets.add(goal.label)
                    }
                }
            }
        }

        if (missingTargets.isEmpty()) {
            return Result.success()
        }

        val level = EscalationLevel.getForHour(currentHour)
        val missingSummary = missingTargets.take(3).joinToString(", ")

        sendEscalatingNotification(level, missingSummary, config, applicationContext)
        prefsManager.recordNotificationSent(currentHour)

        return Result.success()
    }

    companion object {
        fun sendSampleNotification(context: Context, level: EscalationLevel, config: EmergencyReminderConfig) {
            val sampleSummary = "15 Pushups, 15m Duolingo, Morning Journal"
            sendNotification(level, sampleSummary, config, context)
            android.widget.Toast.makeText(context, "🚨 [Heads-Up Popup] Sample notification sent for ${level.name}!", android.widget.Toast.LENGTH_LONG).show()
        }

        private fun sendNotification(
            level: EscalationLevel,
            missingSummary: String,
            config: EmergencyReminderConfig,
            context: Context
        ) {
            val channelId = "emergency_reminders"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Emergency Streak Shield"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = "Duolingo-style escalating notifications to protect daily habit streaks."
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val title = level.titlePrefix
            val bodyText = when (level) {
                EscalationLevel.MIDDAY_GENTLE -> "Midday check: Still need $missingSummary to keep your streak intact."
                EscalationLevel.AFTERNOON_NUDGE -> "Don't leave your habits for late tonight! Still missing: $missingSummary."
                EscalationLevel.EVENING_WARNING -> "Evening approaches. Your habit consistency matrix shows: $missingSummary remaining."
                EscalationLevel.NIGHT_IRRITATED -> "Duo and Anki are getting impatient! $missingSummary stand between you and a broken streak!"
                EscalationLevel.HIGH_EMERGENCY -> "CRITICAL STREAK DANGER: Complete $missingSummary before midnight or suffer HP penalty!"
                EscalationLevel.FINAL_COUNTDOWN -> "FINAL WARNING: Streak break in under 1 hour! Complete $missingSummary NOW!"
            }

            val firstExercise = config.selectedExercises.firstOrNull() ?: "PUSHUP"
            val workoutIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("START_MINI_WORKOUT", true)
                putExtra("EXERCISE_TYPE", firstExercise)
                putExtra("EXERCISE_COUNT", config.exerciseTargets[firstExercise] ?: 15)
            }
            val workoutPendingIntent = PendingIntent.getActivity(
                context,
                101,
                workoutIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val targetAppPackage = config.selectedAppPackages.firstOrNull() ?: "com.duolingo"
            val appLaunchIntent = context.packageManager.getLaunchIntentForPackage(targetAppPackage) ?: Intent(context, MainActivity::class.java)
            appLaunchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val appPendingIntent = PendingIntent.getActivity(
                context,
                102,
                appLaunchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, MindfulSnoozeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val snoozePendingIntent = PendingIntent.getActivity(
                context,
                103,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val appLabel = if (targetAppPackage.contains("duolingo")) "DO DUOLINGO" else if (targetAppPackage.contains("anki")) "OPEN ANKI" else "OPEN APP"

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(workoutPendingIntent, true)
                .setOngoing(level == EscalationLevel.FINAL_COUNTDOWN)
                .setAutoCancel(level != EscalationLevel.FINAL_COUNTDOWN)
                .setContentIntent(workoutPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "DO $firstExercise NOW", workoutPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, appLabel, appPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "SNOOZE 30M", snoozePendingIntent)

            notificationManager.notify(5001, builder.build())

            // Launch on-screen popup warning activity overlay
            val warningActivityIntent = Intent(context, com.example.fitlock.ui.EmergencyWarningActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_MESSAGE", bodyText)
                putExtra("EXTRA_EXERCISE_TYPE", firstExercise)
                putExtra("EXTRA_EXERCISE_COUNT", config.exerciseTargets[firstExercise] ?: 15)
                putExtra("EXTRA_TARGET_PACKAGE", targetAppPackage)
            }
            try {
                context.startActivity(warningActivityIntent)
            } catch (e: Exception) {
                // Fallback if background start restricted
            }
        }

        private fun sendEscalatingNotification(
            level: EscalationLevel,
            missingSummary: String,
            config: EmergencyReminderConfig,
            context: Context
        ) {
            sendNotification(level, missingSummary, config, context)
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EmergencyReminderWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "emergency_reminder_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
