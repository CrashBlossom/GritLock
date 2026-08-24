package com.example.fitlock.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitlock.data.GritLockDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar

class SmartNudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = GritLockDatabase.getDatabase(applicationContext)
        val urges = db.dao().getUrgeEvents().first()
        
        if (urges.size < 3) return Result.success()

        // Find high-risk hours
        val riskHours = urges.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.HOUR_OF_DAY)
        }.mapValues { it.value.size }
         .filter { it.value >= 2 }
         .keys

        if (riskHours.isNotEmpty()) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (riskHours.contains(currentHour + 1)) {
                sendNudgeNotification()
            }
            android.util.Log.d("SmartNudge", "High risk hours identified: $riskHours")
        }

        return Result.success()
    }

    private fun sendNudgeNotification() {
        val channelId = "willpower_nudges"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Willpower Nudges", android.app.NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(com.example.fitlock.R.drawable.ic_launcher_foreground)
            .setContentTitle("Willpower Shield Ready")
            .setContentText("You usually feel an urge around this time. Stay strong!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(3001, notification)
    }
}
