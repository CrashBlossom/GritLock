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
import android.os.SystemClock
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
import com.example.fitlock.MainActivity
import com.example.fitlock.R
import com.example.fitlock.data.AppBlockEvent
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.ScheduleInterval
import com.example.fitlock.data.LockStatusManager
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.utils.NetworkUtils
import com.example.fitlock.data.UserStats
import com.example.fitlock.data.WorkoutHistory
import com.example.fitlock.data.StatType
import com.example.fitlock.utils.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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
    private var lastOverlayTriggerTime: Long = 0
    private val OVERLAY_COOLDOWN_MS = 3000L 
    private var lastTriggeredApp: String? = null
    private var dismissalRunnable: Runnable? = null

    // For APP_USAGE tracking
    private var activeAppUsageGroup: Int = -1
    private var activeAppUsageVaultItem: Int = -1
    private var activeAppUsagePackages: List<String> = emptyList()
    private var appUsageTotalSeconds: Int = 0
    private var appUsageSecondsRemaining: Int = 0
    private var lastUsageTick: Long = 0
    private var appUsageTimerRunnable: Runnable? = null
    private var appUsageIsFinal: Boolean = true
    private var appUsageLabel: String = "App"

    // Focus Shield State
    private var focusShieldActive = false
    private var focusBlockGroupId = -1
    private var focusWhitelist = emptyList<String>()

    // Intermittent Lock State
    private var intermittentLockActive = false
    private var intermittentBlockGroupId = -1
    private var intermittentRequirements: List<ExerciseRequirement> = emptyList()

    // Evening Gate State
    private var eveningGateActive = false

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
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
            if (activeApp == this.packageName || 
                activeApp == "com.android.systemui" || 
                activeApp.contains("launcher") ||
                activeApp.contains("inputmethod") ||
                activeApp.contains("keyboard") ||
                activeApp.contains("honeyboard") ||
                activeApp.contains("clock") ||
                activeApp.contains("alarm") ||
                activeApp == "com.urbandroid.sleep") {
                if (currentCountdownApp != null) removeCountdown()
                return
            }

            // 1. Check Intermittent Lock first (highest priority enforcement)
            if (intermittentLockActive && intermittentBlockGroupId != -1) {
                val groupToBlock = allGroups.find { it.id == intermittentBlockGroupId }
                if (groupToBlock != null && groupToBlock.packageNames.contains(activeApp)) {
                    val firstReq = intermittentRequirements.firstOrNull() ?: ExerciseRequirement(ExerciseType.PUSHUP.name, 10)
                    triggerOverlay(activeApp, firstReq.count, firstReq.type, intermittentBlockGroupId)
                    return
                }
            }

            val targetGroup = evaluateAppBlocking(activeApp, rootNode)
            if (targetGroup != null) {
                cancelDismissal()
                if (currentCountdownApp == null) {
                    Log.d("GritLockService", "Block Group Triggered for $activeApp (Name: ${targetGroup.name})")
                    handleGroupMonitoring(activeApp, targetGroup, isKeyword = targetGroup.keywords.isNotEmpty())
                }
            } else if (currentCountdownApp != null) {
                scheduleDismissal()
            }
        }
    }

    private fun scheduleDismissal() {
        if (dismissalRunnable != null) return
        dismissalRunnable = Runnable {
            removeCountdown()
            closeLockOverlay()
            dismissalRunnable = null
            Log.d("GritLockService", "Lock dismissed due to lack of keywords")
        }
        handler.postDelayed(dismissalRunnable!!, 3000)
    }

    private fun cancelDismissal() {
        dismissalRunnable?.let { handler.removeCallbacks(it) }
        dismissalRunnable = null
    }

    private fun checkMorningGate(packageName: String) {
        val today = java.time.LocalDate.now().toString()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Gate is active from 4 AM onwards
        if (hour < 4) return
        
        serviceScope.launch {
            val pledge = db.dao().getPledgeForDate(today)
            if (pledge == null || pledge.status == "PENDING") {
                Log.d("GritLockService", "Morning Gate potentially active for: $packageName")
                // Check whitelist (Settings, Phone, etc.)
                val isWhitelisted = packageName == "com.android.settings" || 
                                    packageName == "com.android.phone" || 
                                    packageName == "com.google.android.dialer" ||
                                    packageName.contains("clock") ||
                                    packageName.contains("alarm") ||
                                    packageName == "com.urbandroid.sleep" ||
                                    packageName == this@GritLockAccessibilityService.packageName
                
                if (!isWhitelisted) {
                    Log.d("GritLockService", "Morning Gate BLOCKING: $packageName")
                    val now = System.currentTimeMillis()
                    if (now - lastOverlayTriggerTime < OVERLAY_COOLDOWN_MS) return@launch
                    if (rootInActiveWindow?.packageName?.toString() == this@GritLockAccessibilityService.packageName) return@launch

                    handler.post {
                        lastOverlayTriggerTime = System.currentTimeMillis()
                        val intent = Intent(this@GritLockAccessibilityService, com.example.fitlock.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("navigate_to", "morning_pledge")
                        }
                        startActivity(intent)
                        Toast.makeText(this@GritLockAccessibilityService, "Morning Gate: Complete your pledge to unlock.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun checkEveningGate(packageName: String) {
        if (!eveningGateActive) return

        // Whitelist same as Morning Gate
        val isWhitelisted = packageName == "com.android.settings" || 
                            packageName == "com.android.phone" || 
                            packageName == "com.google.android.dialer" ||
                            packageName.contains("clock") ||
                            packageName.contains("alarm") ||
                            packageName == "com.urbandroid.sleep" ||
                            packageName == this@GritLockAccessibilityService.packageName
        
        if (!isWhitelisted) {
            val now = System.currentTimeMillis()
            if (now - lastOverlayTriggerTime < OVERLAY_COOLDOWN_MS) return
            if (rootInActiveWindow?.packageName?.toString() == this@GritLockAccessibilityService.packageName) return

            handler.post {
                lastOverlayTriggerTime = System.currentTimeMillis()
                val intent = Intent(this@GritLockAccessibilityService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("navigate_to", "evening_review")
                }
                startActivity(intent)
                Toast.makeText(this@GritLockAccessibilityService, "Evening Gate: Finish your review to unlock.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        
        if (packageName == this.packageName || 
            packageName == "com.android.systemui" || 
            packageName == "com.android.launcher" ||
            packageName.contains("launcher") ||
            packageName.contains("inputmethod") ||
            packageName.contains("keyboard") ||
            packageName.contains("honeyboard") ||
            packageName.contains("clock") ||
            packageName.contains("alarm") ||
            packageName == "com.urbandroid.sleep" ||
            packageName == "com.google.android.permissioncontroller") return

        Log.d("GritLockService", "Event: ${AccessibilityEvent.eventTypeToString(event.eventType)} from $packageName")

        // Settings Protection / Anti-Bypass
        if (packageName == "com.android.settings") {
            checkSettingsProtection(packageName, rootInActiveWindow ?: event.source)
        }

        // 1. Morning Gate enforcement
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            checkMorningGate(packageName)
            checkEveningGate(packageName)
        }

        // Focus Shield enforcement
        if (focusShieldActive && focusBlockGroupId != -1) {
            val groupToBlock = allGroups.find { it.id == focusBlockGroupId }
            if (groupToBlock != null && groupToBlock.packageNames.contains(packageName) && !focusWhitelist.contains(packageName)) {
                triggerOverlay(packageName, 0, ExerciseType.APP_USAGE.name, focusBlockGroupId)
                return
            }
        }

        // Intermittent Lock enforcement
        if (intermittentLockActive && intermittentBlockGroupId != -1) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val groupToBlock = allGroups.find { it.id == intermittentBlockGroupId }
                if (groupToBlock != null && groupToBlock.packageNames.contains(packageName)) {
                    val firstReq = intermittentRequirements.firstOrNull() ?: ExerciseRequirement(ExerciseType.PUSHUP.name, 10)
                    triggerOverlay(packageName, firstReq.count, firstReq.type, intermittentBlockGroupId)
                    return
                }
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
            
            val nodeToSearch = rootInActiveWindow ?: event.source
            checkPositiveFocusApp(packageName, nodeToSearch)

            val targetGroup = evaluateAppBlocking(packageName, nodeToSearch)

            if (targetGroup != null) {
                cancelDismissal()
                if (activeAppUsageGroup == targetGroup.id) {
                    triggerOverlay(packageName, 0, ExerciseType.APP_USAGE.name, targetGroup.id, isKeyword = targetGroup.keywords.isNotEmpty())
                    return
                }
                Log.d("GritLockService", "Block Triggered in onAccessibilityEvent for $packageName (Group: ${targetGroup.name})")
                handleGroupMonitoring(packageName, targetGroup, isKeyword = targetGroup.keywords.isNotEmpty())
                return
            } else if (currentCountdownApp == packageName) {
                scheduleDismissal()
            }
            
            if (currentCountdownApp != null && packageName != currentCountdownApp) {
                removeCountdown()
            }
        }
    }

    private fun completeAppUsageRequirement() {
        stopAppUsageTimer()
        val isFinal = appUsageIsFinal
        val label = appUsageLabel
        val groupId = activeAppUsageGroup
        val vaultId = activeAppUsageVaultItem
        
        if (isFinal) {
            activeAppUsagePackages = emptyList()
            activeAppUsageGroup = -1
            activeAppUsageVaultItem = -1
        }
        
        appUsageSecondsRemaining = 0
        lastUsageTick = 0
        
        updateNotification("GritLock: Requirement Met", "App usage for $label completed!")

        serviceScope.launch {
            if (isFinal) {
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
            }
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
            appUsageIsFinal = intent.getBooleanExtra("is_final", true)
            appUsageLabel = intent.getStringExtra("label") ?: "Required App"
            lastUsageTick = 0
            updateNotification("GritLock: Usage Required", "Open $appUsageLabel to start tracking.")
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
        } else if (intent?.action == "ACTIVATE_INTERMITTENT_LOCK") {
            intermittentLockActive = true
            intermittentBlockGroupId = intent.getIntExtra("block_group_id", -1)
            // Optional: pass specific requirements from worker
            val reqType = intent.getStringExtra("exercise_type")
            val reqCount = intent.getIntExtra("exercise_count", 10)
            if (reqType != null) {
                intermittentRequirements = listOf(ExerciseRequirement(reqType, reqCount))
            }
            Log.d("GritLockService", "Intermittent Lock Activated for group $intermittentBlockGroupId. Req: $reqType x $reqCount")
            handler.post { checkCurrentAppStatus() }
        } else if (intent?.action == "DEACTIVATE_INTERMITTENT_LOCK") {
            intermittentLockActive = false
            intermittentBlockGroupId = -1
            intermittentRequirements = emptyList()
            Log.d("GritLockService", "Intermittent Lock Deactivated")
        } else if (intent?.action == "ACTIVATE_EVENING_GATE") {
            eveningGateActive = true
            Log.d("GritLockService", "Evening Gate Activated")
            handler.post { checkCurrentAppStatus() }
        } else if (intent?.action == "DEACTIVATE_EVENING_GATE") {
            eveningGateActive = false
            Log.d("GritLockService", "Evening Gate Deactivated")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun findKeywordInNode(node: AccessibilityNodeInfo, keywords: List<String>): String? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        // Safety: Ignore common system/browser UI nodes that might contain matching text accidentally
        if (text == "search or type url" || text == "address and search bar") return null

        for (keyword in keywords) {
            val kw = keyword.lowercase()
            if (kw.length < 3) continue // Ignore very short keywords

            if (text.contains(kw) || contentDesc.contains(kw)) {
                Log.d("GritLockService", "KEYWORD HIT: '$keyword' in node '$text' (desc: '$contentDesc') of app ${node.packageName}")
                return keyword
            }
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

    private fun handleGroupMonitoring(packageName: String, group: AppGroup, isKeyword: Boolean = false) {
        val now = System.currentTimeMillis()
        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
        val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
        val timeSinceUnlock = now - lastUnlocked
        val isUnlocked = timeSinceUnlock < unlockDurationMs

        if (!isUnlocked) {
            if (packageName != currentCountdownApp) {
                if (isCountdownShowing) removeCountdown()
                startCountdown(packageName, group, isKeyword)
            }
        } else {
            val remainingMs = unlockDurationMs - timeSinceUnlock
            updateNotification("GritLock: Session Active", "${group.name} expires in ${formatTime((remainingMs/1000).toInt())}")
            
            // Loop to update notification every second
            handler.removeCallbacksAndMessages(group.name)
            val timerRunnable = object : Runnable {
                override fun run() {
                    val currentApp = rootInActiveWindow?.packageName?.toString() ?: ""
                    val groupPackageList = group.packageNames
                    
                    if (groupPackageList.contains(currentApp) || currentApp == this@GritLockAccessibilityService.packageName) {
                        val updatedNow = System.currentTimeMillis()
                        val updatedRemainingMs = unlockDurationMs - (updatedNow - lastUnlocked)
                        if (updatedRemainingMs > 0) {
                            updateNotification("GritLock: Session Active", "${group.name} expires in ${formatTime((updatedRemainingMs/1000).toInt())}")
                            handler.postAtTime(this, group.name, SystemClock.uptimeMillis() + 1000)
                        } else {
                            handleGroupMonitoring(currentApp, group)
                        }
                    } else {
                        // Keep notification but don't loop update as aggressively or stop if user is elsewhere
                        // For now, stop loop to save battery if they exit the blocked group
                        handler.removeCallbacksAndMessages(group.name)
                    }
                }
            }
            handler.postAtTime(timerRunnable, group.name, SystemClock.uptimeMillis() + 1000)
        }
    }

    private fun isGroupActive(group: AppGroup): Boolean {
        val scheduleActive = isScheduleActive(group.schedule)
        val wifiActive = isWifiRestricted(group.restrictedWifiSsids)
        val active = scheduleActive && wifiActive
        if (active && group.keywords.isNotEmpty()) {
            Log.d("GritLockService", "Checking keywords for active group: ${group.name}")
        }
        return active
    }

    private fun isWifiRestricted(restrictedSsids: List<String>): Boolean {
        if (restrictedSsids.isEmpty()) return true
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("GritLockService", "WiFi restriction active but Location permission missing. Defaulting to RESTRICTED.")
            return true // Fail secure: if we can't verify, we block.
        }
        
        val currentSsid = NetworkUtils.getCurrentSsid(this)
        Log.d("GritLockService", "WiFi Check: Current='$currentSsid', Restricted=$restrictedSsids")
        return currentSsid != null && restrictedSsids.contains(currentSsid)
    }

    private fun isScheduleActive(schedule: List<ScheduleInterval>): Boolean {
        if (schedule.isEmpty()) return true 
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK) 
        val ourDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return schedule.any { it.dayOfWeek == ourDayOfWeek && currentMinute >= it.startMinute && currentMinute <= it.endMinute }
    }

    private fun checkBedtimeViolation(packageName: String) {
        val prefs = getSharedPreferences("fitlock_prefs", MODE_PRIVATE)
        val isPenaltyEnabled = prefs.getBoolean("bedtime_penalty_enabled", true)
        if (!isPenaltyEnabled) return

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        // Bedtime window: 10:30 PM (1350 mins) to 4:00 AM (240 mins)
        val isBedtimeWindow = timeInMinutes >= 1350 || timeInMinutes < 240
        if (isBedtimeWindow) {
            val isRestricted = allGroups.any { it.isEnabled && it.packageNames.contains(packageName) }
            if (isRestricted) {
                Log.d("GritLockService", "Bedtime Violation Detected for $packageName!")
                prefs.edit().putBoolean("late_night_violation_occurred", true).apply()
            }
        }
    }

    private fun startCountdown(packageName: String, group: AppGroup, isKeyword: Boolean = false) {
        val prefs = getSharedPreferences("fitlock_prefs", MODE_PRIVATE)
        val useHpDrainMode = prefs.getBoolean("use_hp_drain_mode", false)
        val showPopups = prefs.getBoolean("show_countdown", true)
        val lockoutSeconds = prefs.getInt("lockout_seconds", 10)
        currentCountdownApp = packageName
        persistentSecondsLeft = if (lockoutSeconds > 0) lockoutSeconds else 0

        checkBedtimeViolation(packageName)

        if (useHpDrainMode) {
            handler.post { showHpDrainOverlay(packageName, group, isKeyword) }
            return
        }

        if (lockoutSeconds <= 0) {
            val firstReq = group.exercises.firstOrNull()
            if (firstReq != null) triggerOverlay(packageName, firstReq.count, firstReq.type, group.id, isKeyword)
            return
        }
        if (showPopups) handler.post { showCountdownOverlay(packageName, group, isKeyword) }
        else startBackgroundTimer(packageName, group, isKeyword)
    }

    private fun showCountdownOverlay(targetPackage: String, group: AppGroup, isKeyword: Boolean = false) {
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
                        triggerOverlay(target, reps, type, gId, isKeyword)
                    }
                }
            }
            btnDismiss?.setOnClickListener { removeCountdown(); performGlobalAction(GLOBAL_ACTION_BACK) }
            windowManager?.addView(countdownView, params)
            handler.postDelayed(countdownRunnable!!, 1000)
            isCountdownShowing = true
        } catch (e: Exception) { startBackgroundTimer(targetPackage, group) }
    }

    private fun showHpDrainOverlay(targetPackage: String, group: AppGroup, isKeyword: Boolean = false) {
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

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        try {
            countdownView = inflater.inflate(R.layout.layout_hp_drain_overlay, null)
            val tvHpMessage = countdownView?.findViewById<TextView>(R.id.tv_hp_message)
            val pbHpDrain = countdownView?.findViewById<ProgressBar>(R.id.pb_hp_drain)
            val btnExitApp = countdownView?.findViewById<Button>(R.id.btn_exit_app)

            countdownRunnable = object : Runnable {
                override fun run() {
                    val self = this
                    if (currentCountdownApp != targetPackage) return
                    serviceScope.launch {
                        val stats = db.dao().getUserStats().first() ?: UserStats()
                        val updatedHp = (stats.currentHp - 2).coerceAtLeast(0)
                        db.dao().updateUserStats(stats.copy(currentHp = updatedHp))

                        handler.post {
                            tvHpMessage?.text = "⚡ HEALTH DRAINING: $updatedHp/100 HP"
                            pbHpDrain?.progress = updatedHp
                        }

                        if (updatedHp <= 0) {
                            handler.post {
                                removeCountdown()
                                val firstReq = group.exercises.firstOrNull()
                                triggerOverlay(targetPackage, firstReq?.count ?: 10, firstReq?.type ?: "PUSHUP", group.id, isKeyword)
                            }
                        } else {
                            handler.postDelayed(self, 1000)
                        }
                    }
                }
            }

            btnExitApp?.setOnClickListener { removeCountdown(); performGlobalAction(GLOBAL_ACTION_BACK) }
            windowManager?.addView(countdownView, params)
            handler.postDelayed(countdownRunnable!!, 1000)
            isCountdownShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBackgroundTimer(targetPackage: String, group: AppGroup, isKeyword: Boolean = false) {
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
                    triggerOverlay(target, reps, type, gId, isKeyword)
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

    private fun closeLockOverlay() {
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            action = "FINISH_OVERLAY"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun triggerOverlay(target: String, reps: Int, exercise: String, groupId: Int, isKeyword: Boolean = false) {
        val now = System.currentTimeMillis()
        if (now - lastOverlayTriggerTime < OVERLAY_COOLDOWN_MS) return
        
        // Robust check: Is our own overlay already the active window?
        val currentForeground = rootInActiveWindow?.packageName?.toString()
        if (currentForeground == this.packageName) return
        
        // Prevent triggering for the same app multiple times in very quick succession
        if (target == lastTriggeredApp && now - lastOverlayTriggerTime < 2000) return

        lastOverlayTriggerTime = now
        lastTriggeredApp = target
        Log.d("GritLockService", "TRIGGERING OVERLAY for: $target. Reason: ${if (isKeyword) "Keyword" else "Package"}")
        serviceScope.launch {
            db.dao().insertBlockEvent(AppBlockEvent(packageName = target, reason = if (isKeyword) "Keyword Blocked" else "App Blocked"))
        }
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra("target_app", target)
            putExtra("target_reps", reps)
            putExtra("exercise_type", exercise)
            putExtra("group_id", groupId)
            putExtra("is_keyword_block", isKeyword)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { removeCountdown() }

    override fun onDestroy() { super.onDestroy() }

    private fun checkSettingsProtection(packageName: String, rootNode: AccessibilityNodeInfo?) {
        if (packageName != "com.android.settings") return
        val prefs = getSharedPreferences("fitlock_prefs", MODE_PRIVATE)
        val isStrictMode = prefs.getBoolean("strict_mode", false)
        if (!isStrictMode) return

        if (rootNode != null) {
            val textMatches = findTextInNode(rootNode, listOf("FitLock", "GrittierLock", "Force stop", "Uninstall", "Disable"))
            if (textMatches) {
                Log.d("GritLockService", "Strict Anti-Bypass: Intercepted Settings attempt!")
                handler.post {
                    Toast.makeText(this, "Strict Anti-Bypass: System Settings modification is restricted.", Toast.LENGTH_LONG).show()
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }
    }

    private var lastPositiveTickTime = 0L

    private fun checkPositiveFocusApp(packageName: String, rootNode: AccessibilityNodeInfo?) {
        val enabledPositiveGroups = allGroups.filter { it.isEnabled && it.isProductive && isGroupActive(it) }
        
        val matchingGroup = enabledPositiveGroups.find { group ->
            val matchesPackage = group.packageNames.contains(packageName)
            if (group.keywords.isNotEmpty() && rootNode != null) {
                val kw = findKeywordInNode(rootNode, group.keywords)
                matchesPackage || kw != null
            } else {
                matchesPackage
            }
        }

        if (matchingGroup != null) {
            val now = System.currentTimeMillis()
            if (now - lastPositiveTickTime >= 60_000L) {
                lastPositiveTickTime = now
                serviceScope.launch {
                    val exerciseType = when {
                        packageName == "com.duolingo" || matchingGroup.name.contains("French", ignoreCase = true) -> "FRENCH_STUDY"
                        packageName == "com.ichi2.anki" || matchingGroup.name.contains("Anki", ignoreCase = true) || matchingGroup.name.contains("Flashcard", ignoreCase = true) -> "FLASHCARDS"
                        else -> matchingGroup.rewardStat.name
                    }

                    db.dao().insertWorkout(
                        WorkoutHistory(
                            exerciseType = exerciseType,
                            repsCompleted = 1,
                            appGroupId = matchingGroup.id,
                            xpGained = 10
                        )
                    )

                    val stats = db.dao().getUserStats().first() ?: UserStats()
                    val updatedInt = if (exerciseType == "FRENCH_STUDY" || exerciseType == "FLASHCARDS" || matchingGroup.rewardStat == StatType.INT) stats.intXp + 10 else stats.intXp
                    db.dao().updateUserStats(stats.copy(totalXp = stats.totalXp + 10, intXp = updatedInt))
                    
                    handler.post {
                        Toast.makeText(applicationContext, "[SYSTEM: +10 XP / +10 ${matchingGroup.rewardStat} earned for ${matchingGroup.name}]", Toast.LENGTH_SHORT).show()
                    }

                    WidgetUpdater.updateAllWidgets(applicationContext)
                }
            }
        }
    }

    private fun evaluateAppBlocking(activeApp: String, rootNode: AccessibilityNodeInfo?): AppGroup? {
        val enabledActiveGroups = allGroups.filter { it.isEnabled && isGroupActive(it) && !it.isProductive }
        
        // 1. Full Package Groups (Package matches activeApp, and NO keywords specified)
        val fullPackageGroups = enabledActiveGroups.filter { it.packageNames.contains(activeApp) && it.keywords.isEmpty() }
        val lockedFullGroup = fullPackageGroups.find { isGroupLocked(it) }
        if (lockedFullGroup != null) return lockedFullGroup

        if (rootNode != null) {
            // 2. Package-Scoped Keyword Groups (Package matches activeApp AND keywords specified)
            val scopedKeywordGroups = enabledActiveGroups.filter { it.packageNames.contains(activeApp) && it.keywords.isNotEmpty() }
            val matchingScopedGroup = scopedKeywordGroups.find { group ->
                val foundKeyword = findKeywordInNode(rootNode, group.keywords)
                foundKeyword != null && isGroupLocked(group)
            }
            if (matchingScopedGroup != null) return matchingScopedGroup

            // 3. Global Keyword Groups (Package list is empty, keywords specified)
            val globalKeywordGroups = enabledActiveGroups.filter { it.packageNames.isEmpty() && it.keywords.isNotEmpty() }
            val matchingGlobalGroup = globalKeywordGroups.find { group ->
                val foundKeyword = findKeywordInNode(rootNode, group.keywords)
                foundKeyword != null && isGroupLocked(group)
            }
            if (matchingGlobalGroup != null) return matchingGlobalGroup
        }

        return null
    }

    private fun isGroupLocked(group: AppGroup): Boolean {
        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
        val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
        return (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
    }

    private fun findTextInNode(node: AccessibilityNodeInfo?, targetTexts: List<String>): Boolean {
        if (node == null) return false
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        for (target in targetTexts) {
            if (nodeText.contains(target, ignoreCase = true) || contentDesc.contains(target, ignoreCase = true)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (findTextInNode(child, targetTexts)) return true
            }
        }
        return false
    }
}
