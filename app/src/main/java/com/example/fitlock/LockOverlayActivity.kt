/**
 * LockOverlayActivity is the screen that "pops up" to block the user from using a restricted app.
 * It stays on top until the user completes the required exercises.
 */
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
    
    private lateinit var db: GritLockDatabase // Reference to our local Room database
    private lateinit var exerciseManager: ExerciseTrackerManager // Component that handles counting reps
    private var currentAnalyzer: Any? = null // The ML Kit component that "analyzes" camera frames
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Retrieve data passed in from the Accessibility Service (which app is being blocked)
        val targetApp = intent.getStringExtra("target_app") ?: "Unknown"
        val groupId = intent.getIntExtra("group_id", -1)

        // Initialize database
        db = Room.databaseBuilder(
            applicationContext,
            GritLockDatabase::class.java, "gritlock-db"
        ).fallbackToDestructiveMigration().build()

        /**
         * setContent starts the Compose UI.
         */
        setContent {
            GritLockTheme {
                // UI State variables
                var currentExerciseIndex by remember { mutableIntStateOf(0) }
                var repCountState by remember { mutableIntStateOf(0) }
                var bankedRepsState by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
                var trackingMode by remember { mutableStateOf(TrackingMode.CAMERA) }
                var exerciseRequirements by remember { mutableStateOf<List<ExerciseRequirement>>(emptyList()) }
                var userStats by remember { mutableStateOf<UserStats?>(null) }
                var isInitialized by remember { mutableStateOf(false) }

                /**
                 * LaunchedEffect(Unit) runs once when the screen first loads.
                 * We use it to load data from the database.
                 */
                LaunchedEffect(Unit) {
                    val stats = db.dao().getUserStats().first()
                    userStats = stats
                    bankedRepsState = stats?.bankedReps ?: emptyMap()
                    
                    // If we have a group ID, load the specific exercises for that group
                    if (groupId != -1) {
                        val group = db.dao().getGroupById(groupId)
                        if (group != null) {
                            exerciseRequirements = group.exercises
                        }
                    }
                    
                    // Fallback if no specific requirements were found
                    if (exerciseRequirements.isEmpty()) {
                        val targetReps = intent.getIntExtra("target_reps", 10)
                        val exerciseTypeStr = intent.getStringExtra("exercise_type") ?: "PUSHUP"
                        exerciseRequirements = listOf(ExerciseRequirement(exerciseTypeStr, targetReps))
                    }
                    
                    // Set up the listener for when a rep is successfully counted
                    initializeExerciseManager { count ->
                        repCountState = count
                    }
                    
                    isInitialized = true
                }

                // If loading is finished and we have exercises to do:
                if (isInitialized && exerciseRequirements.isNotEmpty()) {
                    val currentReq = exerciseRequirements[currentExerciseIndex]
                    val exerciseType = try { ExerciseType.valueOf(currentReq.type) } catch(e: Exception) { ExerciseType.PUSHUP }

                    /**
                     * LaunchedEffect that triggers whenever the exercise or tracking mode changes.
                     * It restarts the tracking logic.
                     */
                    LaunchedEffect(currentExerciseIndex, trackingMode) {
                        repCountState = 0
                        if (exerciseType != ExerciseType.APP_USAGE) {
                            initializeAnalyzer(exerciseType, userStats)
                            val cal = userStats?.calibrations?.get("${exerciseType.name}_${trackingMode.name}")
                            // Tell the manager to start listening for movements
                            exerciseManager.startTracking(exerciseType, trackingMode, currentReq.count, cal)
                        } else {
                            // Specialized logic for "spending time in another app" requirement
                            exerciseManager.startTracking(ExerciseType.APP_USAGE, TrackingMode.POCKET, currentReq.count)
                        }
                    }

                    // The actual Composable function that draws the UI elements
                    LockOverlayScreen(
                        targetApp = targetApp,
                        repCount = repCountState,
                        exerciseRequirements = exerciseRequirements,
                        currentExerciseIndex = currentExerciseIndex,
                        trackingMode = trackingMode,
                        bankedReps = bankedRepsState,
                        onModeChange = { trackingMode = it },
                        onPoseDetected = { pose, width, height ->
                            // This callback is triggered for every camera frame
                            if (trackingMode == TrackingMode.CAMERA) {
                                // Send the skeleton data to our specific exercise analyzer
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
                        onEmergencyBypass = { finish() }, // "finish()" closes this activity
                        onNextExercise = {
                            lifecycleScope.launch {
                                // Save what we've done so far to history
                                saveRepsToHistory(exerciseType, repCountState, groupId)

                                val isLastExercise = currentExerciseIndex == exerciseRequirements.size - 1
                                val isRequirementMet = repCountState >= currentReq.count

                                if (isLastExercise && isRequirementMet) {
                                    // If everything is done, unlock the app and close the overlay
                                    performUnlock(groupId)
                                    finish()
                                } else if (isRequirementMet) {
                                    // Move to the next exercise in the list
                                    currentExerciseIndex++
                                }
                            }
                        },
                        onStopExercise = {
                            lifecycleScope.launch {
                                saveRepsToHistory(exerciseType, repCountState, groupId)
                                finish() // Just close without unlocking
                            }
                        },
                        onUseBankedReps = { type, count ->
                            // User "paid" using previously stored reps
                            useBankedRep(type, count, repCountState) { newCount ->
                                repCountState = newCount
                            }
                        },
                        onLaunchRequiredApp = {
                            // Specialized logic for "APP_USAGE" requirements
                            val pkg = currentReq.targetPackageName
                            if (!pkg.isNullOrBlank()) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                                if (launchIntent != null) {
                                    // Start a background tracking service to count seconds
                                    val serviceIntent = Intent(this@LockOverlayActivity, GritLockAccessibilityService::class.java).apply {
                                        action = "START_APP_USAGE_TRACKING"
                                        putExtra("group_id", groupId)
                                        putExtra("target_package", pkg)
                                        putExtra("seconds", currentReq.count)
                                    }
                                    startService(serviceIntent)
                                    startActivity(launchIntent)
                                    finish() // Close overlay so they can use the required app
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
    
    /**
     * Initializes the component that manages exercise sessions.
     */
    private fun initializeExerciseManager(onRepCount: (Int) -> Unit) {
        exerciseManager = ExerciseTrackerManager(
            context = this,
            onRepCountChanged = { count ->
                onRepCount(count)
            },
            onWorkoutComplete = { reps -> }
        )
    }

    /**
     * Chooses the right analyzer logic based on what exercise the user is doing.
     */
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

    /**
     * Deducts stored reps from the user's "bank" to satisfy the current lock.
     */
    private fun useBankedRep(exerciseType: String, count: Int, currentReps: Int, onUpdate: (Int) -> Unit) {
        lifecycleScope.launch {
            val stats = db.dao().getUserStats().first() ?: return@launch
            val currentBanked = stats.bankedReps[exerciseType] ?: 0
            
            if (currentBanked >= count) {
                val newBanked = stats.bankedReps.toMutableMap()
                newBanked[exerciseType] = currentBanked - count
                // Update the database with the new balance
                db.dao().updateUserStats(stats.copy(bankedReps = newBanked))
                onUpdate(currentReps + count)
                Toast.makeText(this@LockOverlayActivity, "Used banked rep!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Saves the reps the user just performed into their workout history.
     */
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

    /**
     * Marks an app group as "Unlocked" so the Accessibility service allows it to run.
     */
    private suspend fun performUnlock(groupId: Int) {
        if (groupId != -1) {
            val group = db.dao().getGroupById(groupId)
            if (group != null) {
                val now = System.currentTimeMillis()
                // Update database
                db.dao().updateGroup(group.copy(lastUnlockedTimestamp = now))
                // Update the memory-only manager for faster response
                LockStatusManager.updateUnlock(groupId, now)
                Log.d("LockOverlay", "Group $groupId UNLOCKED at $now")
            }
        }
    }

    /**
     * Updates the user's XP and checks if they've leveled up.
     */
    private suspend fun updateUserStats(xpGained: Int, exerciseType: String, repsDone: Int) {
        val currentStats = db.dao().getUserStats().first() ?: UserStats()
        
        var newXp = currentStats.totalXp + xpGained
        var newLevel = currentStats.level
        val xpNeeded = newLevel * 100
        
        // Level up logic (every 100 XP is a level)
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

    /**
     * Cleanup code called when the screen is closed.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (::exerciseManager.isInitialized) {
            exerciseManager.shutdown() // Stop sensors and release resources
        }
    }
}
