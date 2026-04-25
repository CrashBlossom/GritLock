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
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.ScheduleInterval
import com.example.fitlock.data.LockStatusManager
import com.example.fitlock.exercise.ExerciseType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class GritLockAccessibilityService : AccessibilityService() {

    private lateinit var db: GritLockDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var allGroups = listOf<AppGroup>()

    private var windowManager: WindowManager? = null
    private var countdownView: View? = null
    private var isCountdownShowing = false
    private var currentCountdownApp: String? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    
    private var persistentSecondsLeft: Int = -1

    // For APP_USAGE tracking
    private var activeAppUsageGroup: Int = -1
    private var activeAppUsagePackage: String? = null
    private var appUsageSecondsRemaining: Int = 0
    private var lastUsageTick: Long = 0

    private val CHANNEL_ID = "gritlock_countdown_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        Log.d("GritLockService", "Service Created")
        db = GritLockDatabase.getDatabase(applicationContext)
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        observeGroups()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "GritLock Countdown"
            val descriptionText = "Shows countdown before app is blocked or relocked"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun observeGroups() {
        serviceScope.launch {
            db.dao().getAllGroups().collectLatest { groups ->
                allGroups = groups
                Log.d("GritLockService", "Groups updated: ${groups.size}")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        
        if (packageName == this.packageName || 
            packageName == "com.android.systemui" || 
            packageName == "com.android.launcher" ||
            packageName.contains("launcher") ||
            packageName == "com.google.android.permissioncontroller") return

        // Handle APP_USAGE tracking
        if (activeAppUsagePackage != null && packageName == activeAppUsagePackage) {
            val now = System.currentTimeMillis()
            if (lastUsageTick > 0) {
                val delta = ((now - lastUsageTick) / 1000).toInt()
                if (delta >= 1) {
                    appUsageSecondsRemaining -= delta
                    lastUsageTick = now
                    updateNotification("GritLock: App Usage Requirement", "Keep using this app! $appUsageSecondsRemaining seconds left.")
                    
                    if (appUsageSecondsRemaining <= 0) {
                        completeAppUsageRequirement()
                    }
                }
            } else {
                lastUsageTick = now
            }
        } else if (activeAppUsagePackage != null) {
            lastUsageTick = 0 // Pause timer if they leave the required app
        }

        val prefs = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val strictMode = prefs.getBoolean("strict_mode", false)

        if (strictMode && packageName == "com.android.settings") {
            val source = event.source
            if (source != null) {
                val nodes = source.findAccessibilityNodeInfosByText("GritLock")
                val isUninstall = source.findAccessibilityNodeInfosByText("Uninstall").isNotEmpty() || 
                                 source.findAccessibilityNodeInfosByText("Force stop").isNotEmpty()
                
                if (nodes.isNotEmpty() || isUninstall) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Toast.makeText(this, "Strict Mode: GritLock protection cannot be disabled!", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            // Check Package Name blocking
            val matchingGroup = allGroups.find { it.packageNames.contains(packageName) && it.isEnabled }
            
            if (matchingGroup != null) {
                if (isScheduleActive(matchingGroup.schedule)) {
                    // Check if this group is currently being unlocked via APP_USAGE
                    if (activeAppUsageGroup == matchingGroup.id) {
                        // Show overlay to remind them to go back to the required app
                        triggerOverlay(packageName, 0, ExerciseType.APP_USAGE.name, matchingGroup.id)
                        return
                    }

                    Log.d("GritLockService", "Blocking app: $packageName (Group: ${matchingGroup.name})")
                    handleGroupMonitoring(packageName, matchingGroup)
                    return
                }
            }

            // Check Keyword/URL blocking
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val activeGroups = allGroups.filter { it.isEnabled && isScheduleActive(it.schedule) }
                val keywordsToBlock = activeGroups.flatMap { it.keywords }
                
                if (keywordsToBlock.isNotEmpty()) {
                    val foundKeyword = findKeywordInNode(rootNode, keywordsToBlock)
                    if (foundKeyword != null) {
                        val keywordGroup = activeGroups.find { it.keywords.contains(foundKeyword) }
                        if (keywordGroup != null) {
                            if (activeAppUsageGroup == keywordGroup.id) {
                                triggerOverlay(packageName, 0, ExerciseType.APP_USAGE.name, keywordGroup.id)
                                return
                            }
                            Log.d("GritLockService", "Keyword detected: $foundKeyword in $packageName")
                            handleGroupMonitoring(packageName, keywordGroup)
                            return
                        }
                    }
                }
            }
            
            if (currentCountdownApp != null && packageName != currentCountdownApp) {
                removeCountdown()
            }
        }
    }

    private fun completeAppUsageRequirement() {
        val groupId = activeAppUsageGroup
        activeAppUsagePackage = null
        activeAppUsageGroup = -1
        appUsageSecondsRemaining = 0
        lastUsageTick = 0
        
        serviceScope.launch {
            val group = db.dao().getGroupById(groupId)
            if (group != null) {
                val now = System.currentTimeMillis()
                db.dao().updateGroup(group.copy(lastUnlockedTimestamp = now))
                LockStatusManager.updateUnlock(groupId, now)
                handler.post {
                    Toast.makeText(this@GritLockAccessibilityService, "App Usage Requirement Met! Unlocking ${group.name}", Toast.LENGTH_LONG).show()
                }
                cancelNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_APP_USAGE_TRACKING") {
            activeAppUsageGroup = intent.getIntExtra("group_id", -1)
            activeAppUsagePackage = intent.getStringExtra("target_package")
            appUsageSecondsRemaining = intent.getIntExtra("seconds", 60)
            lastUsageTick = 0
            Log.d("GritLockService", "Started APP_USAGE tracking for $activeAppUsagePackage ($appUsageSecondsRemaining s)")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun findKeywordInNode(node: AccessibilityNodeInfo, keywords: List<String>): String? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString()?.lowercase() ?: "" else ""
        
        val viewId = node.viewIdResourceName ?: ""
        
        for (keyword in keywords) {
            val kw = keyword.lowercase()
            if (text.contains(kw) || contentDesc.contains(kw) || hint.contains(kw) || viewId.contains(kw)) {
                return keyword
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findKeywordInNode(child, keywords)
                if (found != null) {
                    child.recycle()
                    return found
                }
                child.recycle()
            }
        }
        return null
    }

    private fun handleGroupMonitoring(packageName: String, group: AppGroup) {
        val now = System.currentTimeMillis()
        val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
        val timeSinceUnlock = now - lastUnlocked
        val isUnlocked = timeSinceUnlock < unlockDurationMs

        if (!isUnlocked) {
            if (packageName != currentCountdownApp) {
                if (isCountdownShowing) {
                    removeCountdown()
                }
                startCountdown(packageName, group)
            }
        } else {
            val remainingMs = unlockDurationMs - timeSinceUnlock
            val remainingMin = (remainingMs / 60000).toInt()
            val remainingSec = ((remainingMs % 60000) / 1000).toInt()
            
            currentCountdownApp = packageName
            updateNotification(
                "GritLock: App Unlocked",
                "Relocking in ${remainingMin}m ${remainingSec}s. Get moving soon!"
            )
            
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (currentCountdownApp == packageName) {
                        val updatedNow = System.currentTimeMillis()
                        val updatedRemainingMs = unlockDurationMs - (updatedNow - lastUnlocked)
                        if (updatedRemainingMs > 0) {
                            val m = (updatedRemainingMs / 60000).toInt()
                            val s = ((updatedRemainingMs % 60000) / 1000).toInt()
                            updateNotification(
                                "GritLock: App Unlocked",
                                "Relocking in ${m}m ${s}s. Get moving soon!"
                            )
                            handler.postDelayed(this, 1000)
                        } else {
                            handleGroupMonitoring(packageName, group)
                        }
                    }
                }
            }, 1000)
        }
    }

    private fun isScheduleActive(schedule: List<ScheduleInterval>): Boolean {
        if (schedule.isEmpty()) return true 
        
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK) 
        val ourDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        return schedule.any { 
            it.dayOfWeek == ourDayOfWeek && currentMinute >= it.startMinute && currentMinute <= it.endMinute 
        }
    }

    private fun startCountdown(packageName: String, group: AppGroup) {
        val prefs = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val showPopups = prefs.getBoolean("show_countdown", true)
        val lockoutSeconds = prefs.getInt("lockout_seconds", 10)
        
        currentCountdownApp = packageName
        persistentSecondsLeft = if (lockoutSeconds > 0) lockoutSeconds else 0

        if (lockoutSeconds <= 0) {
            val firstReq = group.exercises.firstOrNull()
            if (firstReq != null) {
                triggerOverlay(packageName, firstReq.count, firstReq.type, group.id)
            }
            return
        }

        if (showPopups) {
            handler.post {
                showCountdownOverlay(packageName, group)
            }
        } else {
            startBackgroundTimer(packageName, group)
        }
    }

    private fun showCountdownOverlay(targetPackage: String, group: AppGroup) {
        if (isCountdownShowing) {
            countdownView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }
            isCountdownShowing = false
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 100
        }

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val prefs = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val smartCountdown = prefs.getBoolean("smart_countdown", false)
        val lockoutSeconds = prefs.getInt("lockout_seconds", 10)

        try {
            countdownView = inflater.inflate(R.layout.layout_countdown_overlay, null)
            val tvMessage = countdownView?.findViewById<TextView>(R.id.tv_countdown_message)
            val pbCountdown = countdownView?.findViewById<ProgressBar>(R.id.pb_countdown)
            val btnDismiss = countdownView?.findViewById<Button>(R.id.btn_dismiss_countdown)
            
            pbCountdown?.max = lockoutSeconds
            pbCountdown?.progress = persistentSecondsLeft

            updateNotification("GritLock: Block Warning", "Blocking in $persistentSecondsLeft seconds!")

            countdownRunnable = object : Runnable {
                override fun run() {
                    if (currentCountdownApp != targetPackage) return

                    persistentSecondsLeft--
                    
                    if (persistentSecondsLeft >= 0) {
                        updateNotification("GritLock: Block Warning", "Blocking in $persistentSecondsLeft seconds!")
                        
                        val shouldBeVisible = !smartCountdown || persistentSecondsLeft <= 30
                        if (shouldBeVisible) {
                            if (countdownView?.visibility != View.VISIBLE) countdownView?.visibility = View.VISIBLE
                            tvMessage?.text = "GritLock: Exercise required in ${persistentSecondsLeft}s"
                            pbCountdown?.progress = persistentSecondsLeft
                        } else {
                            countdownView?.visibility = View.GONE
                        }
                        
                        handler.postDelayed(this, 1000)
                    } else {
                        removeCountdown()
                        val firstReq = group.exercises.firstOrNull()
                        if (firstReq != null) {
                            triggerOverlay(targetPackage, firstReq.count, firstReq.type, group.id)
                        }
                    }
                }
            }
            
            btnDismiss?.setOnClickListener {
                removeCountdown()
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            windowManager?.addView(countdownView, params)
            handler.postDelayed(countdownRunnable!!, 1000)
            isCountdownShowing = true
            
        } catch (e: Exception) {
            Log.e("GritLockService", "Error showing overlay", e)
            startBackgroundTimer(targetPackage, group)
        }
    }

    private fun startBackgroundTimer(targetPackage: String, group: AppGroup) {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        updateNotification("GritLock: Block Warning", "Blocking in $persistentSecondsLeft seconds!")
        countdownRunnable = object : Runnable {
            override fun run() {
                if (currentCountdownApp != targetPackage) return
                persistentSecondsLeft--
                
                if (persistentSecondsLeft >= 0) {
                    updateNotification("GritLock: Block Warning", "Blocking in $persistentSecondsLeft seconds!")
                    handler.postDelayed(this, 1000)
                } else {
                    removeCountdown()
                    val firstReq = group.exercises.firstOrNull()
                    if (firstReq != null) {
                        triggerOverlay(targetPackage, firstReq.count, firstReq.type, group.id)
                    }
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000)
    }

    private fun removeCountdown() {
        handler.removeCallbacksAndMessages(null)
        countdownRunnable = null
        
        countdownView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
        }
        countdownView = null
        isCountdownShowing = false
        currentCountdownApp = null
        persistentSecondsLeft = -1
        cancelNotification()
    }

    private fun triggerOverlay(target: String, reps: Int, exercise: String, groupId: Int) {
        Log.d("GritLockService", "Triggering block overlay for $target")
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra("target_app", target)
            putExtra("target_reps", reps)
            putExtra("exercise_type", exercise)
            putExtra("group_id", groupId)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        removeCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeCountdown()
    }
}
