package com.example.fitlock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitlock.MainActivity
import com.example.fitlock.R
import com.example.fitlock.ui.MindfulSnoozeActivity

/**
 * MovementReminderWorker handles sending periodic discipline checks.
 * It encourages the user to move or perform mental exercises.
 */
class MovementReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val isLockEnabled = prefs.getBoolean("intermittent_lock_enabled", false)
        val blockGroupId = prefs.getInt("intermittent_block_group_id", -1)
        
        val hardStop = inputData.getBoolean("hardStop", false)
        val (exType, exCount) = getSelectedExercise()

        if (isLockEnabled && blockGroupId != -1) {
            activateIntermittentLock(blockGroupId, exType, exCount)
        }

        sendIntermittentTrainingNotification(hardStop, exType, exCount)
        return Result.success()
    }

    private fun getSelectedExercise(): Pair<String, Int> {
        val exercises = listOf(
            "SQUAT" to 20,
            "PUSHUP" to 15,
            "PLANK" to 45,
            "SITUP" to 25
        )
        // Use current hour as seed for stability
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val seed = (day * 100 + hour).toLong()
        return exercises[java.util.Random(seed).nextInt(exercises.size)]
    }

    private fun activateIntermittentLock(groupId: Int, exType: String, exCount: Int) {
        val intent = Intent(applicationContext, GritLockAccessibilityService::class.java).apply {
            action = "ACTIVATE_INTERMITTENT_LOCK"
            putExtra("block_group_id", groupId)
            putExtra("exercise_type", exType)
            putExtra("exercise_count", exCount)
        }
        applicationContext.startService(intent)
    }

    private fun sendIntermittentTrainingNotification(hardStop: Boolean, exType: String, exCount: Int) {
        val channelId = "movement_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Intermittent Training"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance)
            notificationManager.createNotificationChannel(channel)
        }

        // Action: DO IT NOW
        val workoutIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("START_MINI_WORKOUT", true)
            putExtra("EXERCISE_TYPE", exType)
            putExtra("EXERCISE_COUNT", exCount)
        }
        val workoutPendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            workoutIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: SNOOZE
        val snoozeIntent = Intent(applicationContext, MindfulSnoozeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val snoozePendingIntent = PendingIntent.getActivity(
            applicationContext, 
            1, 
            snoozeIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeButtonLabel = if (hardStop) "Final Warning" else "SNOOZE"
        val message = "Ready for a session? Next up: $exCount ${exType.lowercase()}s."

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Intermittent Training")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "DO IT NOW", workoutPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, snoozeButtonLabel, snoozePendingIntent)

        notificationManager.notify(4001, builder.build())
    }
}
