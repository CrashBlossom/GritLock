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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class LockOverlayActivity : ComponentActivity() {
    
    private lateinit var db: GritLockDatabase
    private lateinit var exerciseManager: ExerciseTrackerManager
    private var currentAnalyzer: Any? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val targetApp = intent.getStringExtra("target_app") ?: "Unknown"
        val groupId = intent.getIntExtra("group_id", -1)

        db = Room.databaseBuilder(
            applicationContext,
            GritLockDatabase::class.java, "gritlock-db"
        ).fallbackToDestructiveMigration().build()

        setContent {
            GritLockTheme {
                var currentExerciseIndex by remember { mutableIntStateOf(0) }
                var repCountState by remember { mutableIntStateOf(0) }
                var bankedRepsState by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
                var trackingMode by remember { mutableStateOf(TrackingMode.CAMERA) }
                var exerciseRequirements by remember { mutableStateOf<List<ExerciseRequirement>>(emptyList()) }
                var userStats by remember { mutableStateOf<UserStats?>(null) }
                var isInitialized by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val stats = db.dao().getUserStats().first()
                    userStats = stats
                    bankedRepsState = stats?.bankedReps ?: emptyMap()
                    
                    if (groupId != -1) {
                        val group = db.dao().getGroupById(groupId)
                        if (group != null) {
                            exerciseRequirements = group.exercises
                        }
                    }
                    
                    if (exerciseRequirements.isEmpty()) {
                        val targetReps = intent.getIntExtra("target_reps", 10)
                        val exerciseTypeStr = intent.getStringExtra("exercise_type") ?: "PUSHUP"
                        exerciseRequirements = listOf(ExerciseRequirement(exerciseTypeStr, targetReps))
                    }
                    
                    initializeExerciseManager { count ->
                        repCountState = count
                    }
                    
                    isInitialized = true
                }

                if (isInitialized && exerciseRequirements.isNotEmpty()) {
                    val currentReq = exerciseRequirements[currentExerciseIndex]
                    val exerciseType = try { ExerciseType.valueOf(currentReq.type) } catch(e: Exception) { ExerciseType.PUSHUP }

                    LaunchedEffect(currentExerciseIndex, trackingMode) {
                        repCountState = 0
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
                        exerciseRequirements = exerciseRequirements,
                        currentExerciseIndex = currentExerciseIndex,
                        trackingMode = trackingMode,
                        bankedReps = bankedRepsState,
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
                                saveRepsToHistory(exerciseType, repCountState, groupId)

                                val isLastExercise = currentExerciseIndex == exerciseRequirements.size - 1
                                val isRequirementMet = repCountState >= currentReq.count

                                if (isLastExercise && isRequirementMet) {
                                    performUnlock(groupId)
                                    finish()
                                } else if (isRequirementMet) {
                                    currentExerciseIndex++
                                }
                            }
                        },
                        onStopExercise = {
                            lifecycleScope.launch {
                                saveRepsToHistory(exerciseType, repCountState, groupId)
                                finish()
                            }
                        },
                        onUseBankedReps = { type, count ->
                            useBankedRep(type, count, repCountState) { newCount ->
                                repCountState = newCount
                            }
                        },
                        onLaunchRequiredApp = {
                            val pkg = currentReq.targetPackageName
                            if (!pkg.isNullOrBlank()) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                                if (launchIntent != null) {
                                    // Start service tracking
                                    val serviceIntent = Intent(this@LockOverlayActivity, GritLockAccessibilityService::class.java).apply {
                                        action = "START_APP_USAGE_TRACKING"
                                        putExtra("group_id", groupId)
                                        putExtra("target_package", pkg)
                                        putExtra("seconds", currentReq.count)
                                    }
                                    startService(serviceIntent)
                                    startActivity(launchIntent)
                                    finish() // Close overlay so they can use the app
                                } else {
                                    Toast.makeText(this@LockOverlayActivity, "Could not find app: $pkg", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    private fun initializeExerciseManager(onRepCount: (Int) -> Unit) {
        exerciseManager = ExerciseTrackerManager(
            context = this,
            onRepCountChanged = { count ->
                onRepCount(count)
            },
            onWorkoutComplete = { reps -> }
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

    private fun useBankedRep(exerciseType: String, count: Int, currentReps: Int, onUpdate: (Int) -> Unit) {
        lifecycleScope.launch {
            val stats = db.dao().getUserStats().first() ?: return@launch
            val currentBanked = stats.bankedReps[exerciseType] ?: 0
            
            if (currentBanked >= count) {
                val newBanked = stats.bankedReps.toMutableMap()
                newBanked[exerciseType] = currentBanked - count
                db.dao().updateUserStats(stats.copy(bankedReps = newBanked))
                onUpdate(currentReps + count)
                Toast.makeText(this@LockOverlayActivity, "Used banked rep!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveRepsToHistory(exerciseType: ExerciseType, reps: Int, groupId: Int) {
        if (reps > 0 && exerciseType != ExerciseType.APP_USAGE) {
            val xpGained = reps * 5
            updateUserStats(xpGained, exerciseType.name, reps)
            
            db.dao().insertWorkout(
                WorkoutHistory(
                    exerciseType = exerciseType.name,
                    repsCompleted = reps,
                    appGroupId = groupId,
                    xpGained = xpGained
                )
            )
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
            }
        }
    }

    private suspend fun updateUserStats(xpGained: Int, exerciseType: String, repsDone: Int) {
        val currentStats = db.dao().getUserStats().first() ?: UserStats()
        
        var newXp = currentStats.totalXp + xpGained
        var newLevel = currentStats.level
        val xpNeeded = newLevel * 100
        
        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
        }
        
        db.dao().updateUserStats(
            currentStats.copy(
                totalXp = newXp,
                level = newLevel,
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
