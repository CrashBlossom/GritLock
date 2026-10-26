package com.example.fitlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fitlock.R
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.data.GateMode
import com.example.fitlock.data.GritLockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_PACKAGE_ADDED) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        val prefs = context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val isAutoShieldEnabled = prefs.getBoolean("extreme_autoshield_enabled", true)
        if (!isAutoShieldEnabled) return

        // Ignore if package is GressLock or Chrome or Spin Browser
        if (packageName == context.packageName || packageName == "com.android.chrome" || packageName == "com.spin.browser") return

        val pm = context.packageManager
        val appLabel = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val pkgLower = packageName.lowercase()
        val labelLower = appLabel.lowercase()

        // Check if package or label matches high-risk bypass categories (VPN, Alternative Browsers, AI)
        val isVpn = pkgLower.contains("vpn") || pkgLower.contains("proxy") || pkgLower.contains("tunnel") || 
                    pkgLower.contains("windscribe") || pkgLower.contains("nord") || pkgLower.contains("proton") || 
                    pkgLower.contains("express") || pkgLower.contains("surfshark") || pkgLower.contains("mullvad")

        val isAlternativeBrowserOrAi = pkgLower.contains("browser") || 
                labelLower.contains("browser") || pkgLower.contains("layla") || pkgLower.contains("kiwi") || 
                pkgLower.contains("firefox") || pkgLower.contains("opera") || pkgLower.contains("brave") || 
                pkgLower.contains("edge") || pkgLower.contains("duckduckgo") || pkgLower.contains("tor") || 
                labelLower.contains("ai") || labelLower.contains("incognito") || labelLower.contains("unblock")

        if (isVpn || isAlternativeBrowserOrAi) {
            Log.d("PackageInstallReceiver", "HIGH RISK BYPASS APP DETECTED: $appLabel ($packageName)")

            CoroutineScope(Dispatchers.IO).launch {
                val db = GritLockDatabase.getDatabase(context)
                val allGroups = db.dao().getAllGroups().first()

                // Find or create Extreme Shield Group
                val existingExtremeGroup = allGroups.find { it.name.contains("Extreme Anti-Bypass", ignoreCase = true) }
                val updatedPkgs = ((existingExtremeGroup?.packageNames ?: emptyList()) + packageName).distinct()

                val extremeExercises = listOf(
                    ExerciseRequirement(ExerciseType.PUSHUP.name, 100),
                    ExerciseRequirement(ExerciseType.SQUAT.name, 100),
                    ExerciseRequirement(ExerciseType.SITUP.name, 100)
                )

                val targetGroup = existingExtremeGroup?.copy(
                    packageNames = updatedPkgs,
                    exercises = extremeExercises
                ) ?: AppGroup(
                    name = "🛡️ Extreme Anti-Bypass Gate",
                    packageNames = updatedPkgs,
                    exercises = extremeExercises,
                    unlockDurationMinutes = 15,
                    isEnabled = true,
                    gateMode = GateMode.BACKGROUND_RAID,
                    secretPassword = "Urge Surf Protocol: Take 5 deep mindful breaths. Notice the impulse without reacting, or switch to BorrowBox, Anki, or Kindle instead."
                )

                db.dao().insertGroup(targetGroup)

                // Fire High Priority Notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val channelId = "gresslock_extreme_shield_channel"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(channelId, "Extreme Anti-Bypass Shield", android.app.NotificationManager.IMPORTANCE_HIGH)
                    notificationManager.createNotificationChannel(channel)
                }

                val alertMsg = "Newly installed app '$appLabel' has been auto-sealed behind an Extreme Protocol Gate (100 Pushups, 100 Squats, 100 Situps). Try Urge Surfing or using BorrowBox instead!"

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("🛡️ Extreme Anti-Bypass Auto-Shield!")
                    .setContentText(alertMsg)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(alertMsg))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                notificationManager.notify(9903, notification)
            }
        }
    }
}
