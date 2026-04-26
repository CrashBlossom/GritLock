/**
 * MainActivity is the "Entry Point" of the application.
 * When the user taps the app icon, this is the first class that runs.
 * It inherits from ComponentActivity, which provides the foundation for using Jetpack Compose.
 */
package com.example.fitlock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.fitlock.data.*
import com.example.fitlock.exercise.*
import com.example.fitlock.ui.*
import com.example.fitlock.ui.theme.GritLockTheme
import com.example.fitlock.utils.HealthConnectManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity(), SensorEventListener {

    /** 
     * Properties: These are variables that belong to the class.
     * 'private' means they can't be accessed from outside this class.
     * 'lateinit' tells Kotlin "I'll initialize this later before I use it".
     */
    private lateinit var db: GritLockDatabase // Our local database (Room)
    private lateinit var exerciseManager: ExerciseTrackerManager // Handles exercise logic
    
    // Analyzers for different exercise types (initialized when needed)
    private var pushupAnalyzer: PushupAnalyzer? = null
    private var squatAnalyzer: SquatAnalyzer? = null
    private var situpAnalyzer: SitupAnalyzer? = null
    private var pullupAnalyzer: PullupAnalyzer? = null
    private var dipAnalyzer: DipAnalyzer? = null
    private var hingeAnalyzer: HingeAnalyzer? = null
    private var rowAnalyzer: RowAnalyzer? = null
    private var plankAnalyzer: PlankAnalyzer? = null
    
    private lateinit var healthConnectManager: HealthConnectManager // Manages Google Health Connect
    
    // Hardware sensor variables for step counting
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var initialStepCount = -1f
    
    // State variables: When these change, the UI automatically updates (Recomposition)
    private val _stepCount = mutableIntStateOf(0)

    /**
     * Launchers for requesting system permissions.
     * If the user grants permission, we run specific setup code.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        } else true
        
        if (!cameraGranted) {
            Toast.makeText(this, "Camera permission is required for tracking", Toast.LENGTH_LONG).show()
        }
        if (activityGranted) {
            setupStepSensor()
        }
    }

    private val requestHealthPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Toast.makeText(this, "Health Connect permissions granted!", Toast.LENGTH_SHORT).show()
            refreshStepsFromHealth()
        }
    }

    /**
     * onCreate is a "Lifecycle Method".
     * It's called by Android when the Activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Makes the app draw behind the status bar and navigation bar (full screen look)
        enableEdgeToEdge()

        // Initialize our database connection
        db = Room.databaseBuilder(
            applicationContext,
            GritLockDatabase::class.java, "gritlock-db"
        ).fallbackToDestructiveMigration().build()

        healthConnectManager = HealthConnectManager(this)
        
        // Initial setup routines
        initChallenges()
        handleGoalProgression()
        handleBankReset()

        // State variables for the active workout session
        val repCountState = mutableIntStateOf(0)
        var currentExerciseType by mutableStateOf(ExerciseType.PUSHUP)
        var currentGoal by mutableIntStateOf(10)

        // Initialize the exercise manager with callbacks
        exerciseManager = ExerciseTrackerManager(
            context = this,
            onRepCountChanged = { count ->
                repCountState.intValue = count
            },
            onWorkoutComplete = { reps ->
                // lifecycleScope.launch starts a "Coroutine" (background task)
                lifecycleScope.launch {
                    val xpGained = reps * 5
                    
                    // Save workout to database
                    db.dao().insertWorkout(
                        WorkoutHistory(
                            exerciseType = currentExerciseType.name,
                            repsCompleted = reps,
                            appGroupId = 0,
                            xpGained = xpGained
                        )
                    )
                    // Update user's overall stats (XP and Level)
                    updateUserStats(xpGained, currentExerciseType.name, reps)
                    
                    // Sync this workout to Android's Health Connect system
                    val now = java.time.Instant.now()
                    healthConnectManager.writeExerciseSession(
                        currentExerciseType.name,
                        reps,
                        now.minusSeconds(300),
                        now
                    )
                    
                    Toast.makeText(this@MainActivity, "Workout Saved! +$xpGained XP, Banked $reps reps", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Setup the built-in step counter sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Initial checks for permissions
        checkPermissions()
        checkHealthPermissions()

        /**
         * setContent defines the UI of the app using Composable functions.
         * Everything inside here is written in declarative Compose syntax.
         */
        setContent {
            // remember {} caches values so they aren't reset when the UI refreshes
            val prefs = remember { getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
            var appTheme by remember { mutableStateOf(prefs.getString("app_theme", "Default") ?: "Default") }
            
            // A side effect that periodically checks if the theme setting changed
            LaunchedEffect(Unit) {
                while(true) {
                    val currentTheme = prefs.getString("app_theme", "Default") ?: "Default"
                    if (appTheme != currentTheme) {
                        appTheme = currentTheme
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            // Wrap the whole UI in our custom Theme
            GritLockTheme(themeName = appTheme) {
                // collectAsState converts a Database "Flow" into a Compose "State"
                val groups by db.dao().getAllGroups().collectAsState(initial = emptyList())
                val history by db.dao().getHistory().collectAsState(initial = emptyList())
                val userStats by db.dao().getUserStats().collectAsState(initial = null)
                val challenges by db.dao().getChallenges().collectAsState(initial = emptyList())
                
                // Calculate today's workout totals for the progress bars
                val todayTotals = remember(history, _stepCount.intValue) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val startOfDay = cal.timeInMillis
                    
                    // Filter history to only include items from today and group them by type
                    val totalsMap = history.filter { it.timestamp >= startOfDay }
                        .groupBy { it.exerciseType }
                        .mapValues { entry -> entry.value.sumOf { it.repsCompleted } }
                    
                    val mutableTotals = totalsMap.toMutableMap()
                    mutableTotals[ExerciseType.STEPS.name] = _stepCount.intValue
                    mutableTotals
                }

                // Internal navigation state (which screen are we on?)
                var currentScreen by remember { mutableStateOf("groups") }
                var trackingMode by remember { mutableStateOf(TrackingMode.CAMERA) }
                var editingGroup by remember { mutableStateOf<AppGroup?>(null) }
                var calibrationExercise by remember { mutableStateOf<ExerciseType?>(null) }
                
                /**
                 * Scaffold is a layout helper that provides slots for common UI parts
                 * like a TopBar, BottomBar, or FloatingActionButton.
                 */
                Scaffold(
                    bottomBar = {
                        // Hide the bottom bar on specific screens
                        if (currentScreen != "track" && currentScreen != "create" && currentScreen != "calibrate") {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                NavigationItem("Groups", Icons.Default.Home, currentScreen == "groups") { currentScreen = "groups" }
                                NavigationItem("Analytic", Icons.Default.Analytics, currentScreen == "analytics") { currentScreen = "analytics" }
                                NavigationItem("Stats", Icons.AutoMirrored.Filled.ShowChart, currentScreen == "stats") { currentScreen = "stats" }
                                NavigationItem("Profile", Icons.Default.Person, currentScreen == "profile") { currentScreen = "profile" }
                                NavigationItem("Setting", Icons.Default.Settings, currentScreen == "settings") { currentScreen = "settings" }
                            }
                        }
                    }
                ) { innerPadding ->
                    // Padding is provided by Scaffold to avoid overlapping with bars
                    Box(modifier = Modifier.padding(innerPadding)) {
                        /**
                         * This 'when' block acts as our app's Router.
                         * It decides which Composable to show based on 'currentScreen'.
                         */
                        when (currentScreen) {
                            "groups" -> {
                                GroupsScreen(
                                    groups = groups,
                                    userStats = userStats,
                                    challenges = challenges,
                                    todayTotals = todayTotals,
                                    onAddGroupClick = { 
                                        editingGroup = null
                                        currentScreen = "create" 
                                    },
                                    onEditGroupClick = { group ->
                                        editingGroup = group
                                        currentScreen = "create"
                                    },
                                    onDeleteGroupClick = { group ->
                                        lifecycleScope.launch {
                                            db.dao().deleteGroup(group)
                                        }
                                    },
                                    onToggleGroupClick = { group ->
                                        lifecycleScope.launch {
                                            db.dao().updateGroup(group.copy(isEnabled = !group.isEnabled))
                                        }
                                    },
                                    onSettingsClick = { currentScreen = "settings" },
                                    onEmergencyBypassClick = { /* Logic */ },
                                    onChallengeClick = { challenge ->
                                        // When a challenge is clicked, start the first exercise in it
                                        val req = challenge.requirements.firstOrNull()
                                        if (req != null) {
                                            currentExerciseType = try { ExerciseType.valueOf(req.type) } catch(_: Exception) { ExerciseType.PUSHUP }
                                            currentGoal = req.count
                                            repCountState.intValue = 0
                                            
                                            initializeAnalyzers(currentExerciseType, userStats)
                                            exerciseManager.startTracking(currentExerciseType, trackingMode, currentGoal, userStats?.calibrations?.get("${currentExerciseType.name}_${trackingMode.name}"))
                                            currentScreen = "track"
                                        }
                                    },
                                    onExerciseClick = { type, goal ->
                                        // Manual workout start from the daily goals list
                                        currentExerciseType = type
                                        currentGoal = goal
                                        repCountState.intValue = 0
                                        
                                        initializeAnalyzers(type, userStats)
                                        exerciseManager.startTracking(type, trackingMode, goal, userStats?.calibrations?.get("${type.name}_${trackingMode.name}"))
                                        currentScreen = "track"
                                    }
                                )
                            }
                            "analytics" -> {
                                AnalyticsScreen()
                            }
                            "stats" -> {
                                StatsScreen(
                                    history = history
                                )
                            }
                            "profile" -> {
                                ProfileScreen(
                                    userStats = userStats,
                                    challenges = challenges,
                                    onAddChallenge = { challenge ->
                                        lifecycleScope.launch {
                                            db.dao().upsertChallenge(challenge)
                                        }
                                    }
                                )
                            }
                            "settings" -> {
                                SettingsScreen(
                                    onBack = { currentScreen = "groups" },
                                    trackingMode = trackingMode,
                                    onTrackingModeChange = { trackingMode = it },
                                    userStats = userStats,
                                    onUpdateUserStats = { stats ->
                                        lifecycleScope.launch {
                                            db.dao().updateUserStats(stats)
                                        }
                                    },
                                    onNavigateToCalibration = { exercise ->
                                        calibrationExercise = exercise
                                        currentScreen = "calibrate"
                                    }
                                )
                            }
                            "create" -> {
                                CreateGroupScreen(
                                    editingGroup = editingGroup,
                                    onSave = { group ->
                                        lifecycleScope.launch {
                                            if (group.id == 0) {
                                                db.dao().insertGroup(group)
                                            } else {
                                                db.dao().updateGroup(group)
                                            }
                                            currentScreen = "groups"
                                        }
                                    },
                                    onBack = { currentScreen = "groups" }
                                )
                            }
                            "calibrate" -> {
                                calibrationExercise?.let { type ->
                                    CalibrationScreen(
                                        exerciseType = type,
                                        mode = trackingMode,
                                        onCalibrationComplete = { cal ->
                                            lifecycleScope.launch {
                                                val stats = userStats ?: UserStats()
                                                val newCalibrations = stats.calibrations.toMutableMap()
                                                newCalibrations[cal.exerciseType] = cal
                                                db.dao().updateUserStats(stats.copy(calibrations = newCalibrations))
                                                currentScreen = "settings"
                                            }
                                        },
                                        onBack = { currentScreen = "settings" }
                                    )
                                }
                            }
                            "track" -> {
                                val exerciseRequirements = remember(currentExerciseType, currentGoal) {
                                    listOf(ExerciseRequirement(currentExerciseType.name, currentGoal))
                                }
                                LockOverlayScreen(
                                    targetApp = "Workout Mode",
                                    repCount = repCountState.intValue,
                                    exerciseRequirements = exerciseRequirements,
                                    currentExerciseIndex = 0,
                                    trackingMode = trackingMode,
                                    bankedReps = userStats?.bankedReps ?: emptyMap(),
                                    onModeChange = { trackingMode = it },
                                    onPoseDetected = { pose, width, height ->
                                        if (trackingMode == TrackingMode.CAMERA) {
                                            when (currentExerciseType) {
                                                ExerciseType.PUSHUP -> pushupAnalyzer?.analyzePose(pose, width, height)
                                                ExerciseType.SQUAT -> squatAnalyzer?.analyzePose(pose, width, height)
                                                ExerciseType.SITUP -> situpAnalyzer?.analyzePose(pose, width, height)
                                                ExerciseType.PULLUP -> pullupAnalyzer?.analyzePose(pose, width, height)
                                                ExerciseType.DIP -> dipAnalyzer?.analyzePose(pose, width, height)
                                                ExerciseType.HINGE -> hingeAnalyzer?.analyzePose(pose, width, height)
                                                ExerciseType.ROW -> rowAnalyzer?.analyzePose(pose, width, height)
                                                ExerciseType.PLANK -> plankAnalyzer?.analyzePose(pose, width, height)
                                                else -> {}
                                            }
                                        }
                                    },
                                    onEmergencyBypass = { currentScreen = "groups" },
                                    onNextExercise = {
                                        lifecycleScope.launch {
                                            val reps = repCountState.intValue
                                            if (reps > 0) {
                                                val xpGained = reps * 2
                                                
                                                db.dao().insertWorkout(
                                                    WorkoutHistory(
                                                        exerciseType = currentExerciseType.name,
                                                        repsCompleted = reps,
                                                        appGroupId = 0,
                                                        xpGained = xpGained
                                                    )
                                                )
                                                updateUserStats(xpGained, currentExerciseType.name, reps)
                                                
                                                val now = java.time.Instant.now()
                                                healthConnectManager.writeExerciseSession(
                                                    currentExerciseType.name,
                                                    reps,
                                                    now.minusSeconds(120),
                                                    now
                                                )
                                            }
                                            currentScreen = "groups"
                                        }
                                    },
                                    onStopExercise = {
                                        lifecycleScope.launch {
                                            val reps = repCountState.intValue
                                            if (reps > 0) {
                                                val xpGained = reps * 2
                                                
                                                db.dao().insertWorkout(
                                                    WorkoutHistory(
                                                        exerciseType = currentExerciseType.name,
                                                        repsCompleted = reps,
                                                        appGroupId = 0,
                                                        xpGained = xpGained
                                                    )
                                                )
                                                updateUserStats(xpGained, currentExerciseType.name, reps)
                                                
                                                val now = java.time.Instant.now()
                                                healthConnectManager.writeExerciseSession(
                                                    currentExerciseType.name,
                                                    reps,
                                                    now.minusSeconds(120),
                                                    now
                                                )
                                            }
                                            currentScreen = "groups"
                                        }
                                    },
                                    onUseBankedReps = { type, count ->
                                        lifecycleScope.launch {
                                            val stats = userStats ?: return@launch
                                            val currentBanked = stats.bankedReps[type] ?: 0
                                            if (currentBanked >= count) {
                                                val newBanked = stats.bankedReps.toMutableMap()
                                                newBanked[type] = currentBanked - count
                                                db.dao().updateUserStats(stats.copy(bankedReps = newBanked))
                                                repCountState.intValue += count
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Logic for leveling up goals automatically based on progression settings.
     */
    private fun handleGoalProgression() {
        val prefs = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
        val lastSync = prefs.getLong("last_goal_sync", 0L)
        val now = Calendar.getInstance()
        
        val lastSyncCal = Calendar.getInstance().apply { timeInMillis = lastSync }
        
        if (lastSync == 0L) {
            prefs.edit().putLong("last_goal_sync", now.timeInMillis).apply()
            return
        }

        ExerciseType.entries.forEach { type ->
            val enabled = prefs.getBoolean("prog_enabled_${type.name}", false)
            if (enabled) {
                val lastUpdate = prefs.getLong("last_prog_update_${type.name}", 0L)
                val lastUpdateCal = Calendar.getInstance().apply { timeInMillis = lastUpdate }
                
                // If it's a new day, increment the goal
                if (now.get(Calendar.DAY_OF_YEAR) != lastUpdateCal.get(Calendar.DAY_OF_YEAR) ||
                    now.get(Calendar.YEAR) != lastUpdateCal.get(Calendar.YEAR)) {
                    
                    val currentGoal = prefs.getInt("goal_${type.name}", 10)
                    val increment = prefs.getInt("prog_increment_${type.name}", 1)
                    
                    prefs.edit()
                        .putInt("goal_${type.name}", currentGoal + increment)
                        .putLong("last_prog_update_${type.name}", now.timeInMillis)
                        .apply()
                }
            }
        }
        
        prefs.edit().putLong("last_goal_sync", now.timeInMillis).apply()
    }

    /**
     * Logic for clearing banked reps based on reset frequency (Daily, Weekly, etc.)
     */
    private fun handleBankReset() {
        lifecycleScope.launch {
            val stats = db.dao().getUserStats().first() ?: return@launch
            val now = System.currentTimeMillis()
            val lastReset = stats.lastBankReset
            val frequency = stats.bankResetFrequency
            
            val calendar = Calendar.getInstance()
            val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
            val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            calendar.timeInMillis = lastReset
            val lastDay = calendar.get(Calendar.DAY_OF_YEAR)
            val lastWeek = calendar.get(Calendar.WEEK_OF_YEAR)
            val lastMonth = calendar.get(Calendar.MONTH)
            val lastYear = calendar.get(Calendar.YEAR)
            
            val shouldReset = when (frequency) {
                "Daily" -> currentDay != lastDay || currentYear != lastYear
                "Weekly" -> currentWeek != lastWeek || currentYear != lastYear
                "Monthly" -> currentMonth != lastMonth || currentYear != lastYear
                else -> false
            }
            
            if (shouldReset) {
                db.dao().updateUserStats(stats.copy(bankedReps = emptyMap(), lastBankReset = now))
            }
        }
    }

    /**
     * Seed initial challenges if database is empty.
     */
    private fun initChallenges() {
        lifecycleScope.launch {
            val now = Calendar.getInstance()
            val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
            
            // Gradual OPM Scaling: Start at 10, add 3 every day, cap at 100
            val opmBase = 10
            val dailyIncrement = 3
            val currentOpmGoal = (opmBase + (dayOfYear * dailyIncrement)).coerceAtMost(100)

            val challenges = listOf(
                Challenge("1", "Morning Pushups", "Do 20 pushups to start your day", listOf(ExerciseRequirement(ExerciseType.PUSHUP.name, 20)), 100),
                Challenge("2", "Squat Master", "Complete 50 squats", listOf(ExerciseRequirement(ExerciseType.SQUAT.name, 50)), 250),
                Challenge("3", "Core Strength", "Hold a plank for 60 seconds", listOf(ExerciseRequirement(ExerciseType.PLANK.name, 60)), 150),
                Challenge("opm_gradual", "One Punch Man (Gradual)", "Saitama's Training: Pushups, Squats, Situps ($currentOpmGoal each).", listOf(
                    ExerciseRequirement(ExerciseType.PUSHUP.name, currentOpmGoal),
                    ExerciseRequirement(ExerciseType.SQUAT.name, currentOpmGoal),
                    ExerciseRequirement(ExerciseType.SITUP.name, currentOpmGoal)
                ), 500),
                Challenge("solo_leveling", "Solo Leveling: Daily Quest", "Pushups (100), Squats (100), Situps (100), Pullups (20). Don't fail the penalty!", listOf(
                    ExerciseRequirement(ExerciseType.PUSHUP.name, 100),
                    ExerciseRequirement(ExerciseType.SQUAT.name, 100),
                    ExerciseRequirement(ExerciseType.SITUP.name, 100),
                    ExerciseRequirement(ExerciseType.PULLUP.name, 20)
                ), 1000)
            )
            challenges.forEach { db.dao().upsertChallenge(it) }
        }
    }

    /**
     * Sets up the ML Kit analyzers for camera-based tracking.
     */
    private fun initializeAnalyzers(type: ExerciseType, stats: UserStats?) {
        val cal = stats?.calibrations?.get(type.name)
        when (type) {
            ExerciseType.PUSHUP -> pushupAnalyzer = PushupAnalyzer(exerciseManager, cal)
            ExerciseType.SQUAT -> squatAnalyzer = SquatAnalyzer(exerciseManager, cal)
            ExerciseType.SITUP -> situpAnalyzer = SitupAnalyzer(exerciseManager, cal)
            ExerciseType.PULLUP -> pullupAnalyzer = PullupAnalyzer(exerciseManager, cal)
            ExerciseType.DIP -> dipAnalyzer = DipAnalyzer(exerciseManager, cal)
            ExerciseType.HINGE -> hingeAnalyzer = HingeAnalyzer(exerciseManager)
            ExerciseType.ROW -> rowAnalyzer = RowAnalyzer(exerciseManager)
            ExerciseType.PLANK -> plankAnalyzer = PlankAnalyzer(exerciseManager)
            else -> {}
        }
    }

    /**
     * Updates the user's XP and Level in the local database.
     */
    private suspend fun updateUserStats(xpGained: Int, exerciseType: String, repsToBank: Int) {
        val currentStats = db.dao().getUserStats().first() ?: UserStats()
        
        var newXp = currentStats.totalXp + xpGained
        var newLevel = currentStats.level
        val xpNeeded = newLevel * 100
        
        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
        }

        val newBankedReps = currentStats.bankedReps.toMutableMap()
        newBankedReps[exerciseType] = (newBankedReps[exerciseType] ?: 0) + repsToBank
        
        db.dao().updateUserStats(
            currentStats.copy(
                totalXp = newXp,
                level = newLevel,
                bankedReps = newBankedReps,
                lastWorkoutDate = System.currentTimeMillis()
            )
        )
    }

    /**
     * Requests necessary system permissions like Camera and Activity Recognition.
     */
    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        
        val missing = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            setupStepSensor()
        }
    }

    /**
     * Checks if we have permissions to read/write from Google Health Connect.
     */
    private fun checkHealthPermissions() {
        lifecycleScope.launch {
            if (!healthConnectManager.hasPermissions()) {
                val permissions = setOf(
                    HealthPermission.getReadPermission(StepsRecord::class),
                    HealthPermission.getWritePermission(StepsRecord::class),
                    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                    HealthPermission.getWritePermission(ExerciseSessionRecord::class)
                )
                requestHealthPermissionLauncher.launch(permissions.toTypedArray())
            } else {
                refreshStepsFromHealth()
            }
        }
    }

    /**
     * Fetches today's total steps from the Health Connect database.
     */
    private fun refreshStepsFromHealth() {
        lifecycleScope.launch {
            val steps = healthConnectManager.readTodaySteps()
            _stepCount.intValue = steps.toInt()
        }
    }

    /**
     * Registers a listener to listen for step counter hardware events.
     */
    private fun setupStepSensor() {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Called by the system when a sensor (like the step counter) detects a change.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == -1f) {
                initialStepCount = event.values[0]
            }
            // Use this as a fallback if Health Connect is not available
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * A helper Composable for the bottom navigation menu items.
     */
    @Composable
    fun RowScope.NavigationItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
        NavigationBarItem(
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) },
            selected = selected,
            onClick = onClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
