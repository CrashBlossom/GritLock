package com.example.fitlock.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.fitlock.LockOverlayActivity
import com.example.fitlock.R
import com.example.fitlock.data.*
import com.example.fitlock.exercise.ExerciseType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * GritLockAccessibilityService is the "Enforcer." 
 * It now handles Dungeon Mode (Hard-locks) and Charisma XP tracking.
 */
class GritLockAccessibilityService : AccessibilityService() {

    private lateinit var db: GritLockDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var allGroups = listOf<AppGroup>()
    private var userStats: UserStats? = null

    private var windowManager: WindowManager? = null
    private var countdownView: View? = null
    private var isCountdownShowing = false
    private var currentCountdownApp: String? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var persistentSecondsLeft: Int = -1

    // Charisma Tracking
    private var lastCharismaPackage: String? = null
    private var lastCharismaTimestamp: Long = 0

    private val CHANNEL_ID = "gritlock_countdown_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        db = GritLockDatabase.getDatabase(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        observeData()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "GritLock", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
    }

    private fun observeData() {
        serviceScope.launch {
            db.dao().getAllGroups().collectLatest { groups -> allGroups = groups }
        }
        serviceScope.launch {
            db.dao().getUserStats().collectLatest { stats -> userStats = stats }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        if (packageName == this.packageName || packageName.contains("systemui") || packageName.contains("launcher")) return

        val now = System.currentTimeMillis()

        // 1. Charisma (CHA) Tracking Logic
        // If the user uses an app marked as "Charisma Group" (e.g. Duolingo), they gain XP
        val charismaGroup = allGroups.find { it.packageNames.contains(packageName) && it.isCharismaGroup }
        if (charismaGroup != null) {
            if (lastCharismaPackage == packageName) {
                val delta = (now - lastCharismaTimestamp) / 1000
                if (delta >= 60) { // Every 60 seconds of use
                    grantCharismaXp(10) // Grant 10 CHA XP
                    lastCharismaTimestamp = now
                }
            } else {
                lastCharismaPackage = packageName
                lastCharismaTimestamp = now
            }
        } else {
            lastCharismaPackage = null
        }

        // 2. Core Blocking & Dungeon Mode Logic
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val matchingGroup = allGroups.find { it.packageNames.contains(packageName) && it.isEnabled }
            
            if (matchingGroup != null && isScheduleActive(matchingGroup.schedule)) {
                
                // --- DUNGEON MODE CHECK ---
                // If a Pomodoro is running, distractions are HARD-LOCKED (no workouts allowed)
                if (userStats?.isDungeonModeActive == true && matchingGroup.isDistraction) {
                    showDungeonLockOverlay(packageName)
                    return
                }

                handleGroupMonitoring(packageName, matchingGroup)
            }
        }
    }

    private fun grantCharismaXp(amount: Long) {
        serviceScope.launch {
            val stats = userStats ?: return@launch
            db.dao().updateUserStats(stats.copy(chaXp = stats.chaXp + amount))
        }
    }

    private fun showDungeonLockOverlay(packageName: String) {
        // Force the user back to the home screen or Quest Board
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.post {
            Toast.makeText(this, "⚠️ DUNGEON ACTIVE: All distractions are hard-locked!", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleGroupMonitoring(packageName: String, group: AppGroup) {
        val now = System.currentTimeMillis()
        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
        val isUnlocked = (now - lastUnlocked) < (group.unlockDurationMinutes * 60 * 1000L)

        if (!isUnlocked) {
            if (packageName != currentCountdownApp) {
                if (isCountdownShowing) removeCountdown()
                startCountdown(packageName, group)
            }
        }
    }

    private fun isScheduleActive(schedule: List<ScheduleInterval>): Boolean {
        if (schedule.isEmpty()) return true 
        val now = Calendar.getInstance()
        val ourDayOfWeek = if (now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else now.get(Calendar.DAY_OF_WEEK) - 1
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return schedule.any { it.dayOfWeek == ourDayOfWeek && currentMinute >= it.startMinute && currentMinute <= it.endMinute }
    }

    private fun startCountdown(packageName: String, group: AppGroup) {
        val prefs = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val baseLockout = prefs.getInt("lockout_seconds", 10)
        
        // Vitality Buff logic
        val vitalityLevel = ((userStats?.vitXp ?: 0) / 100) + 1
        val totalLockout = baseLockout + (vitalityLevel * 2).toInt()
        
        currentCountdownApp = packageName
        persistentSecondsLeft = totalLockout

        handler.post { showCountdownOverlay(packageName, group, totalLockout) }
    }

    private fun showCountdownOverlay(targetPackage: String, group: AppGroup, maxSeconds: Int) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP; y = 100 }

        try {
            countdownView = LayoutInflater.from(this).inflate(R.layout.layout_countdown_overlay, null)
            val tvMessage = countdownView?.findViewById<TextView>(R.id.tv_countdown_message)
            val pbCountdown = countdownView?.findViewById<ProgressBar>(R.id.pb_countdown)
            pbCountdown?.max = maxSeconds

            countdownRunnable = object : Runnable {
                override fun run() {
                    if (currentCountdownApp != targetPackage) return
                    persistentSecondsLeft--
                    if (persistentSecondsLeft >= 0) {
                        tvMessage?.text = "Exercise required in ${persistentSecondsLeft}s"
                        pbCountdown?.progress = persistentSecondsLeft
                        handler.postDelayed(this, 1000)
                    } else {
                        removeCountdown()
                        triggerOverlay(targetPackage, group)
                    }
                }
            }
            countdownView?.findViewById<Button>(R.id.btn_dismiss_countdown)?.setOnClickListener { 
                removeCountdown()
                performGlobalAction(GLOBAL_ACTION_BACK) 
            }
            windowManager?.addView(countdownView, params)
            handler.postDelayed(countdownRunnable!!, 1000)
            isCountdownShowing = true
        } catch (e: Exception) {}
    }

    private fun removeCountdown() {
        handler.removeCallbacksAndMessages(null)
        countdownView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }
        countdownView = null
        isCountdownShowing = false
        currentCountdownApp = null
    }

    private fun triggerOverlay(target: String, group: AppGroup) {
        val firstReq = group.exercises.firstOrNull() ?: return
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("target_app", target)
            putExtra("target_reps", firstReq.count)
            putExtra("exercise_type", firstReq.type)
            putExtra("group_id", group.id)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { removeCountdown() }
}
