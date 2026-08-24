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
import com.example.fitlock.data.AppBlockEvent
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
    private var activeAppUsageVaultItem: Int = -1
    private var activeAppUsagePackages: List<String> = emptyList()
    private var appUsageTotalSeconds: Int = 0
    private var appUsageSecondsRemaining: Int = 0
    private var lastUsageTick: Long = 0
    private var appUsageTimerRunnable: Runnable? = null

    // Focus Shield State
    private var focusShieldActive = false
    private var focusBlockGroupId = -1
    private var focusWhitelist = emptyList<String>()

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
            val importance = NotificationManager.IMPORTANCE_DEFAULT
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
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun observeGroups() {
        serviceScope.launch {
            db.dao().getAllGroups().collectLatest { groups ->
                allGroups = groups
                Log.d("GritLockService", "Groups updated: ${groups.size}")
                handler.post { checkCurrentAppStatus() }
            }
        }
    }

    private fun checkCurrentAppStatus() {
        val rootNode = rootInActiveWindow
        val activeApp = currentCountdownApp ?: rootNode?.packageName?.toString()
        
        if (activeApp != null) {
            if (activeApp == this.packageName || activeApp == "com.android.systemui" || activeApp.contains("launcher")) {
                if (currentCountdownApp != null) removeCountdown()
                return
            }

            val enabledGroups = allGroups.filter { it.isEnabled }
            val matchingGroups = enabledGroups.filter { it.packageNames.contains(activeApp) }
            
            if (matchingGroups.isNotEmpty()) {
                val activeGroups = matchingGroups.filter { isScheduleActive(it.schedule) }
                if (activeGroups.isNotEmpty()) {
                    // STACKING: Find first group that is actually locked
                    val lockedGroup = activeGroups.find { group ->
                        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                        val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                        (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
                    }

                    if (lockedGroup != null) {
                        if (currentCountdownApp == null) handleGroupMonitoring(activeApp, lockedGroup)
                    } else if (currentCountdownApp != null) {
                        removeCountdown()
                    }
                } else if (currentCountdownApp != null) {
                    removeCountdown()
                }
            } else {
                var keywordBlockedGroup: AppGroup? = null
                if (rootNode != null) {
                    val activeKeywordGroups = enabledGroups.filter { isScheduleActive(it.schedule) && it.keywords.isNotEmpty() }
                    val keywordsToBlock = activeKeywordGroups.flatMap { it.keywords }
                    if (keywordsToBlock.isNotEmpty()) {
                        val foundKeyword = findKeywordInNode(rootNode, keywordsToBlock)
                        if (foundKeyword != null) {
                            // STACKING for keywords
                            val matchingKeywordGroups = activeKeywordGroups.filter { it.keywords.contains(foundKeyword) }
                            keywordBlockedGroup = matchingKeywordGroups.find { group ->
                                val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                                val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                                (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
                            }
                        }
                    }
                }
                
                if (keywordBlockedGroup != null) {
                    if (currentCountdownApp == null) handleGroupMonitoring(activeApp, keywordBlockedGroup)
                } else if (currentCountdownApp != null) {
                    removeCountdown()
                }
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

        // Focus Shield enforcement
        if (focusShieldActive && focusBlockGroupId != -1) {
            val groupToBlock = allGroups.find { it.id == focusBlockGroupId }
            if (groupToBlock != null && groupToBlock.packageNames.contains(packageName) && !focusWhitelist.contains(packageName)) {
                triggerOverlay(packageName, 0, ExerciseType.APP_USAGE.name, focusBlockGroupId)
                return
            }
        }

        if (activeAppUsagePackages.isNotEmpty()) {
            if (activeAppUsagePackages.contains(packageName)) {
                if (lastUsageTick == 0L) {
                    lastUsageTick = System.currentTimeMillis()
                    startAppUsageTimer()
                }
            } else {
                if (lastUsageTick != 0L) {
                    lastUsageTick = 0L
                    stopAppUsageTimer()
                    updateNotification("GritLock: App Usage Paused", "Return to the required app to continue tracking.")
                }
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val matchingGroups = allGroups.filter { it.packageNames.contains(packageName) && it.isEnabled }
            
            if (matchingGroups.isNotEmpty()) {
                val activeGroups = matchingGroups.filter { isScheduleActive(it.schedule) }
                if (activeGroups.isNotEmpty()) {
                    // DISCIPLINE STACKING: Find the first group that is NOT yet unlocked
                    val lockedGroup = activeGroups.find { group ->
                        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                        val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                        (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
                    }

                    if (lockedGroup != null) {
                        if (activeAppUsageGroup == lockedGroup.id) {
                            triggerOverlay(packageName, 0, ExerciseType.APP_USAGE.name, lockedGroup.id)
                            return
                        }
                        handleGroupMonitoring(packageName, lockedGroup)
                        return
                    } else {
                        // ALL groups for this app are unlocked. Show info for the one expiring soonest.
                        val soonestExpiring = activeGroups.minByOrNull { group ->
                            val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                            lastUnlocked + (group.unlockDurationMinutes * 60 * 1000L)
                        }
                        if (soonestExpiring != null) handleGroupMonitoring(packageName, soonestExpiring)
                        return
                    }
                }
            }

            val nodeToSearch = rootInActiveWindow ?: event.source
            if (nodeToSearch != null) {
                val enabledGroups = allGroups.filter { it.isEnabled && isScheduleActive(it.schedule) }
                val keywordsToBlock = enabledGroups.flatMap { it.keywords }
                
                if (keywordsToBlock.isNotEmpty()) {
                    val foundKeyword = findKeywordInNode(nodeToSearch, keywordsToBlock)
                    if (foundKeyword != null) {
                        val keywordGroups = enabledGroups.filter { it.keywords.contains(foundKeyword) }
                        val lockedKeywordGroup = keywordGroups.find { group ->
                            val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                            val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                            (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
                        }

                        if (lockedKeywordGroup != null) {
                            if (activeAppUsageGroup == lockedKeywordGroup.id) {
                                triggerOverlay(packageName, 0, ExerciseType.APP_USAGE.name, lockedKeywordGroup.id)
                                return
                            }
                            handleGroupMonitoring(packageName, lockedKeywordGroup)
                            return
                        }
                    } else if (currentCountdownApp == packageName && matchingGroups.isEmpty()) {
                        removeCountdown()
                    }
                } else if (currentCountdownApp == packageName && matchingGroups.isEmpty()) {
                    removeCountdown()
                }
            }
            
            if (currentCountdownApp != null && packageName != currentCountdownApp) {
                removeCountdown()
            }
        }
    }

    private fun completeAppUsageRequirement() {
        stopAppUsageTimer()
        val groupId = activeAppUsageGroup
        val vaultId = activeAppUsageVaultItem
        activeAppUsagePackages = emptyList()
        activeAppUsageGroup = -1
        activeAppUsageVaultItem = -1
        appUsageSecondsRemaining = 0
        lastUsageTick = 0
        
        serviceScope.launch {
            if (groupId != -1) {
                val group = db.dao().getGroupById(groupId)
                if (group != null) {
                    val now = System.currentTimeMillis()
                    db.dao().updateGroup(group.copy(lastUnlockedTimestamp = now))
                    LockStatusManager.updateUnlock(groupId, now)
                }
            } else if (vaultId != -1) {
                val item = db.dao().getVaultItemById(vaultId)
                if (item != null) {
                    db.dao().upsertVaultItem(item.copy(lastUnlockedTimestamp = System.currentTimeMillis()))
                }
            }
            cancelNotification()
        }
    }

    private fun startAppUsageTimer() {
        stopAppUsageTimer()
        appUsageTimerRunnable = object : Runnable {
            override fun run() {
                if (lastUsageTick == 0L) return
                val now = System.currentTimeMillis()
                val delta = ((now - lastUsageTick) / 1000).toInt()
                if (delta >= 1) {
                    appUsageSecondsRemaining -= delta
                    lastUsageTick = now
                    if (appUsageSecondsRemaining <= 0) {
                        completeAppUsageRequirement()
                    } else {
                        val spent = appUsageTotalSeconds - appUsageSecondsRemaining
                        updateNotification("GritLock: App Usage Progress", "Progress: ${formatTime(spent)} / ${formatTime(appUsageTotalSeconds)} ($appUsageSecondsRemaining s left)")
                        handler.postDelayed(this, 1000)
                    }
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(appUsageTimerRunnable!!)
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    private fun stopAppUsageTimer() {
        appUsageTimerRunnable?.let { handler.removeCallbacks(it) }
        appUsageTimerRunnable = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_APP_USAGE_TRACKING") {
            stopAppUsageTimer()
            activeAppUsageGroup = intent.getIntExtra("group_id", -1)
            activeAppUsageVaultItem = intent.getIntExtra("vault_id", -1)
            activeAppUsagePackages = intent.getStringArrayExtra("target_packages")?.toList() ?: emptyList()
            appUsageTotalSeconds = intent.getIntExtra("seconds", 60)
            appUsageSecondsRemaining = appUsageTotalSeconds
            lastUsageTick = 0
            updateNotification("GritLock: Usage Required", "Open any of: ${activeAppUsagePackages.take(2).joinToString(", ")} to start tracking.")
        } else if (intent?.action == "ACTIVATE_FOCUS_SHIELD") {
            focusShieldActive = true
            focusBlockGroupId = intent.getIntExtra("block_group_id", -1)
            focusWhitelist = intent.getStringArrayExtra("whitelist")?.toList() ?: emptyList()
            Log.d("GritLockService", "Focus Shield Activated for group $focusBlockGroupId")
        } else if (intent?.action == "DEACTIVATE_FOCUS_SHIELD") {
            focusShieldActive = false
            focusBlockGroupId = -1
            focusWhitelist = emptyList()
            Log.d("GritLockService", "Focus Shield Deactivated")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun findKeywordInNode(node: AccessibilityNodeInfo, keywords: List<String>): String? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        for (keyword in keywords) {
            val kw = keyword.lowercase()
            if (text.contains(kw) || contentDesc.contains(kw) || viewId.contains(kw)) return keyword
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findKeywordInNode(child, keywords)
                child.recycle()
                if (found != null) return found
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
                if (isCountdownShowing) removeCountdown()
                startCountdown(packageName, group)
            }
        } else {
            val remainingMs = unlockDurationMs - timeSinceUnlock
            updateNotification("GritLock: App Unlocked", "Access expires in ${formatTime((remainingMs/1000).toInt())}")
            
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (currentCountdownApp == packageName) {
                        val updatedNow = System.currentTimeMillis()
                        val updatedRemainingMs = unlockDurationMs - (updatedNow - lastUnlocked)
                        if (updatedRemainingMs > 0) {
                            updateNotification("GritLock: App Unlocked", "Access expires in ${formatTime((updatedRemainingMs/1000).toInt())}")
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
        return schedule.any { it.dayOfWeek == ourDayOfWeek && currentMinute >= it.startMinute && currentMinute <= it.endMinute }
    }

    private fun startCountdown(packageName: String, group: AppGroup) {
        val prefs = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val showPopups = prefs.getBoolean("show_countdown", true)
        val lockoutSeconds = prefs.getInt("lockout_seconds", 10)
        currentCountdownApp = packageName
        persistentSecondsLeft = if (lockoutSeconds > 0) lockoutSeconds else 0
        if (lockoutSeconds <= 0) {
            val firstReq = group.exercises.firstOrNull()
            if (firstReq != null) triggerOverlay(packageName, firstReq.count, firstReq.type, group.id)
            return
        }
        if (showPopups) handler.post { showCountdownOverlay(packageName, group) }
        else startBackgroundTimer(packageName, group)
    }

    private fun showCountdownOverlay(targetPackage: String, group: AppGroup) {
        if (isCountdownShowing) {
            countdownView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }
            isCountdownShowing = false
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP; y = 100 }

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
                        if (!smartCountdown || persistentSecondsLeft <= 30) {
                            if (countdownView?.visibility != View.VISIBLE) countdownView?.visibility = View.VISIBLE
                            tvMessage?.text = "GritLock: Exercise required in ${persistentSecondsLeft}s"
                            pbCountdown?.progress = persistentSecondsLeft
                        } else countdownView?.visibility = View.GONE
                        handler.postDelayed(this, 1000)
                    } else {
                        val target = targetPackage
                        val firstReq = group.exercises.firstOrNull()
                        val reps = firstReq?.count ?: 10
                        val type = firstReq?.type ?: "PUSHUP"
                        val gId = group.id
                        
                        handler.post { Toast.makeText(this@GritLockAccessibilityService, "Time's up! Complete requirements to unlock.", Toast.LENGTH_SHORT).show() }
                        removeCountdown()
                        triggerOverlay(target, reps, type, gId)
                    }
                }
            }
            btnDismiss?.setOnClickListener { removeCountdown(); performGlobalAction(GLOBAL_ACTION_BACK) }
            windowManager?.addView(countdownView, params)
            handler.postDelayed(countdownRunnable!!, 1000)
            isCountdownShowing = true
        } catch (e: Exception) { startBackgroundTimer(targetPackage, group) }
    }

    private fun startBackgroundTimer(targetPackage: String, group: AppGroup) {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = object : Runnable {
            override fun run() {
                if (currentCountdownApp != targetPackage) return
                persistentSecondsLeft--
                if (persistentSecondsLeft >= 0) {
                    updateNotification("GritLock: Block Warning", "Blocking in $persistentSecondsLeft seconds!")
                    handler.postDelayed(this, 1000)
                } else {
                    val target = targetPackage
                    val firstReq = group.exercises.firstOrNull()
                    val reps = firstReq?.count ?: 10
                    val type = firstReq?.type ?: "PUSHUP"
                    val gId = group.id
                    
                    removeCountdown()
                    triggerOverlay(target, reps, type, gId)
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000)
    }

    private fun removeCountdown() {
        handler.removeCallbacksAndMessages(null)
        countdownRunnable = null
        countdownView?.let { try { windowManager?.removeView(it) } catch (e: Exception) {} }
        countdownView = null
        isCountdownShowing = false
        currentCountdownApp = null
        persistentSecondsLeft = -1
        cancelNotification()
    }

    private fun triggerOverlay(target: String, reps: Int, exercise: String, groupId: Int) {
        serviceScope.launch {
            db.dao().insertBlockEvent(AppBlockEvent(packageName = target, reason = "Blocked by App Group"))
        }
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra("target_app", target)
            putExtra("target_reps", reps)
            putExtra("exercise_type", exercise)
            putExtra("group_id", groupId)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { removeCountdown() }

    override fun onDestroy() { super.onDestroy() }
}
