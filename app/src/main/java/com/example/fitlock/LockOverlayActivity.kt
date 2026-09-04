package com.example.fitlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.UserStats
import com.example.fitlock.data.WorkoutHistory
import com.example.fitlock.data.LockStatusManager
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.exercise.*
import com.example.fitlock.service.GritLockAccessibilityService
import com.example.fitlock.ui.LockOverlayScreen
import com.example.fitlock.ui.theme.GritLockTheme
import com.example.fitlock.utils.HealthConnectManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LockOverlayActivity : ComponentActivity() {
    
    private lateinit var db: GritLockDatabase
    private lateinit var exerciseManager: ExerciseTrackerManager
    private var currentAnalyzer: Any? = null
    
    private var targetApp by mutableStateOf("Unknown")
    private var groupId by mutableIntStateOf(-1)
    private var fallbackReps by mutableIntStateOf(10)
    private var fallbackExercise by mutableStateOf("PUSHUP")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = GritLockDatabase.getDatabase(applicationContext)
        
        handleIntent(intent)

        setContent {
            GritLockTheme {
                val userStats by db.dao().getUserStats().collectAsState(initial = null)
                val allGroups by db.dao().getAllGroups().collectAsState(initial = emptyList())
                
                var currentExerciseIndex by remember { mutableIntStateOf(0) }
                var repCountState by remember { mutableIntStateOf(0) }
                var isStationaryState by remember { mutableStateOf(false) }
                var bankedRepsUsedInSession by remember { mutableIntStateOf(0) }
                var trackingMode by remember { mutableStateOf(TrackingMode.CAMERA) }
                var exerciseRequirements by remember { mutableStateOf<List<ExerciseRequirement>>(emptyList()) }
                var isInitialized by remember { mutableStateOf(false) }

                LaunchedEffect(groupId, fallbackReps, fallbackExercise) {
                    if (groupId != -1) {
                        val group = db.dao().getGroupById(groupId)
                        if (group != null) {
                            exerciseRequirements = group.exercises
                        }
                    }
                    
                    if (exerciseRequirements.isEmpty()) {
                        exerciseRequirements = listOf(ExerciseRequirement(fallbackExercise, fallbackReps))
                    }
                    
                    initializeExerciseManager(
                        onRepCount = { count -> repCountState = count },
                        onStationaryStatusChanged = { isSteady -> isStationaryState = isSteady }
                    )
                    isInitialized = true
                }

                LaunchedEffect(allGroups, groupId, targetApp) {
                    if (groupId != -1) {
                        val group = allGroups.find { it.id == groupId }
                        if (group != null) {
                            val isStillBlocked = group.isEnabled && (group.packageNames.contains(targetApp) || group.keywords.any { targetApp.contains(it, ignoreCase = true) })
                            if (!isStillBlocked) {
                                Log.d("LockOverlay", "Group disabled or app removed. Closing overlay.")
                                finish()
                            }
                        }
                    }
                }

                if (isInitialized && exerciseRequirements.isNotEmpty()) {
                    val currentReq = exerciseRequirements[currentExerciseIndex]
                    val exerciseType = try { ExerciseType.valueOf(currentReq.type) } catch(e: Exception) { ExerciseType.PUSHUP }

                    LaunchedEffect(currentExerciseIndex, trackingMode) {
                        repCountState = 0
                        bankedRepsUsedInSession = 0
                        if (exerciseType != ExerciseType.APP_USAGE) {
                            initializeAnalyzer(exerciseType, userStats)
                            val cal = userStats?.calibrations?.get("${exerciseType.name}_${trackingMode.name}")
                            exerciseManager.startTracking(exerciseType, trackingMode, currentReq.count, cal)
                        } else {
                            exerciseManager.startTracking(ExerciseType.APP_USAGE, TrackingMode.POCKET, currentReq.count)
                        }
                    }

                    LockOverlayScreen(
                        targetApp = targetApp,
                        repCount = repCountState,
                        isStationary = isStationaryState,
                        exerciseRequirements = exerciseRequirements,
                        currentExerciseIndex = currentExerciseIndex,
                        trackingMode = trackingMode,
                        bankedReps = userStats?.bankedReps ?: emptyMap(),
                        onModeChange = { trackingMode = it },
                        onPoseDetected = { pose, width, height ->
                            if (trackingMode == TrackingMode.CAMERA) {
                                when (val analyzer = currentAnalyzer) {
                                    is PushupAnalyzer -> analyzer.analyzePose(pose, width, height)
                                    is SquatAnalyzer -> analyzer.analyzePose(pose, width, height)
                                    is SitupAnalyzer -> analyzer.analyzePose(pose, width, height)
                                    is PullupAnalyzer -> analyzer.analyzePose(pose, width, height)
                                    is DipAnalyzer -> analyzer.analyzePose(pose, width, height)
                                    is HingeAnalyzer -> analyzer.analyzePose(pose, width, height)
                                    is RowAnalyzer -> analyzer.analyzePose(pose, width, height)
                                    is PlankAnalyzer -> analyzer.analyzePose(pose, width, height)
                                }
                            }
                        },
                        onEmergencyBypass = { finish() },
                        onNextExercise = {
                            lifecycleScope.launch {
                                saveRepsToHistory(exerciseType, repCountState, bankedRepsUsedInSession, currentReq.count, groupId)
                                if (currentExerciseIndex == exerciseRequirements.size - 1 && repCountState >= currentReq.count) {
                                    performUnlock(groupId)
                                    finish()
                                } else if (repCountState >= currentReq.count) {
                                    currentExerciseIndex++
                                }
                            }
                        },
                        onStopExercise = {
                            lifecycleScope.launch {
                                saveRepsToHistory(exerciseType, repCountState, bankedRepsUsedInSession, currentReq.count, groupId)
                                
                                // Deactivate Intermittent Lock on stop too
                                val deactivateIntent = Intent(this@LockOverlayActivity, com.example.fitlock.service.GritLockAccessibilityService::class.java).apply {
                                    action = "DEACTIVATE_INTERMITTENT_LOCK"
                                }
                                startService(deactivateIntent)

                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(homeIntent)
                                finish()
                            }
                        },
                        onUseBankedReps = { type, count ->
                            useBankedRep(type, count) { 
                                bankedRepsUsedInSession += count
                                exerciseManager.addManualReps(count)
                            }
                        },
                        onStartFlashcardReview = { count ->
                            val currentReq = exerciseRequirements.getOrNull(currentExerciseIndex)
                            val intent = Intent(this@LockOverlayActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("START_FLASHCARD_REVIEW", true)
                                putExtra("FLASHCARD_COUNT", count)
                                putExtra("DECK_NAME", currentReq?.deckName ?: "ALL")
                                putExtra("RETURN_PACKAGE", targetApp)
                                putExtra("RETURN_GROUP_ID", groupId)
                            }
                            startActivity(intent)
                            finish()
                        },
                        onLaunchRequiredApp = {
                            val pkgs = currentReq.targetPackageNames
                            if (pkgs.isNotEmpty()) {
                                var launchIntent: Intent? = null
                                for (pkg in pkgs) {
                                    launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                                    if (launchIntent != null) break
                                }

                                if (launchIntent != null) {
                                    val serviceIntent = Intent(this@LockOverlayActivity, GritLockAccessibilityService::class.java).apply {
                                        action = "START_APP_USAGE_TRACKING"
                                        putExtra("group_id", groupId)
                                        putExtra("target_packages", pkgs.toTypedArray())
                                        putExtra("seconds", currentReq.count)
                                    }
                                    startService(serviceIntent)
                                    startActivity(launchIntent)
                                    finish()
                                } else {
                                    Toast.makeText(this@LockOverlayActivity, "Could not find any of the required apps", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "FINISH_OVERLAY") {
            finish()
            return
        }
        targetApp = intent?.getStringExtra("target_app") ?: "Unknown"
        groupId = intent?.getIntExtra("group_id", -1) ?: -1
        fallbackReps = intent?.getIntExtra("target_reps", 10) ?: 10
        fallbackExercise = intent?.getStringExtra("exercise_type") ?: "PUSHUP"
    }
    
    private fun initializeExerciseManager(onRepCount: (Int) -> Unit, onStationaryStatusChanged: (Boolean) -> Unit) {
        if (::exerciseManager.isInitialized) exerciseManager.shutdown()
        exerciseManager = ExerciseTrackerManager(
            context = this,
            onRepCountChanged = { count ->
                onRepCount(count)
            },
            onWorkoutComplete = { reps, familyId, level -> },
            onStationaryStatusChanged = onStationaryStatusChanged
        )
    }

    private fun initializeAnalyzer(type: ExerciseType, stats: UserStats?) {
        val cal = stats?.calibrations?.get("${type.name}_CAMERA")
        currentAnalyzer = when (type) {
            ExerciseType.PUSHUP -> PushupAnalyzer(exerciseManager, cal)
            ExerciseType.SQUAT -> SquatAnalyzer(exerciseManager, cal)
            ExerciseType.SITUP -> SitupAnalyzer(exerciseManager, cal)
            ExerciseType.PULLUP -> PullupAnalyzer(exerciseManager, cal)
            ExerciseType.DIP -> DipAnalyzer(exerciseManager, cal)
            ExerciseType.HINGE -> HingeAnalyzer(exerciseManager)
            ExerciseType.ROW -> RowAnalyzer(exerciseManager)
            ExerciseType.PLANK -> PlankAnalyzer(exerciseManager)
            else -> null
        }
    }

    private fun useBankedRep(exerciseType: String, count: Int, onComplete: () -> Unit) {
        lifecycleScope.launch {
            val stats = db.dao().getUserStats().first() ?: return@launch
            val currentBanked = stats.bankedReps[exerciseType] ?: 0
            
            if (currentBanked >= count) {
                val newBanked = stats.bankedReps.toMutableMap()
                newBanked[exerciseType] = currentBanked - count
                db.dao().updateUserStats(stats.copy(bankedReps = newBanked))
                onComplete()
                Toast.makeText(this@LockOverlayActivity, "Used 1 banked rep", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveRepsToHistory(exerciseType: ExerciseType, totalReps: Int, bankedUsed: Int, requirement: Int, groupId: Int) {
        val physicalReps = (totalReps - bankedUsed).coerceAtLeast(0)
        val extraReps = (totalReps - requirement).coerceAtLeast(0)
        val xpGained = physicalReps * 5
        updateUserStats(xpGained, exerciseType.name, extraReps)

        if (physicalReps > 0 && exerciseType != ExerciseType.APP_USAGE) {
            db.dao().insertWorkout(
                WorkoutHistory(
                    exerciseType = exerciseType.name,
                    repsCompleted = physicalReps,
                    appGroupId = groupId,
                    xpGained = xpGained
                )
            )
            val healthConnectManager = HealthConnectManager(this)
            if (healthConnectManager.hasPermissions()) {
                val now = java.time.Instant.now()
                val duration = exerciseManager.getDurationSeconds()
                healthConnectManager.writeExerciseSession(
                    exerciseType.name,
                    physicalReps,
                    now.minusSeconds(duration),
                    now
                )
            }
        }
    }

    private suspend fun performUnlock(groupId: Int) {
        if (groupId != -1) {
            val group = db.dao().getGroupById(groupId)
            if (group != null) {
                val now = System.currentTimeMillis()
                db.dao().updateGroup(group.copy(lastUnlockedTimestamp = now))
                LockStatusManager.updateUnlock(groupId, now)
                Log.d("LockOverlay", "Group $groupId UNLOCKED at $now")

                // Deactivate Intermittent Lock
                val deactivateIntent = Intent(this, com.example.fitlock.service.GritLockAccessibilityService::class.java).apply {
                    action = "DEACTIVATE_INTERMITTENT_LOCK"
                }
                startService(deactivateIntent)
            }
        }
    }

    private suspend fun updateUserStats(xpGained: Int, exerciseType: String, extraReps: Int) {
        val currentStats = db.dao().getUserStats().first() ?: UserStats()
        var newXp = currentStats.totalXp + xpGained
        var newLevel = currentStats.level
        val xpNeeded = newLevel * 100
        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
        }
        val newBankedReps = currentStats.bankedReps.toMutableMap()
        if (extraReps > 0) {
            newBankedReps[exerciseType] = (newBankedReps[exerciseType] ?: 0) + extraReps
        }
        db.dao().updateUserStats(
            currentStats.copy(
                totalXp = newXp,
                level = newLevel,
                bankedReps = newBankedReps,
                lastWorkoutDate = System.currentTimeMillis()
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::exerciseManager.isInitialized) {
            exerciseManager.shutdown()
        }
    }
}
