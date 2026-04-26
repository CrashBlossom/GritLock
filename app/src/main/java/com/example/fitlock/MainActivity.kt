/**
 * MainActivity is the "Entry Point" of the application.
 * It manages navigation, database state, and core RPG logic.
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
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

    private lateinit var db: GritLockDatabase
    private lateinit var exerciseManager: ExerciseTrackerManager
    
    private var pushupAnalyzer: PushupAnalyzer? = null
    private var squatAnalyzer: SquatAnalyzer? = null
    private var situpAnalyzer: SitupAnalyzer? = null
    private var pullupAnalyzer: PullupAnalyzer? = null
    private var dipAnalyzer: DipAnalyzer? = null
    private var hingeAnalyzer: HingeAnalyzer? = null
    private var rowAnalyzer: RowAnalyzer? = null
    private var plankAnalyzer: PlankAnalyzer? = null
    
    private lateinit var healthConnectManager: HealthConnectManager
    
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var initialStepCount = -1f
    private val _stepCount = mutableIntStateOf(0)

    /**
     * Launchers for requesting system permissions.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        } else true

        if (activityGranted) {
            setupStepSensor()
        }
    }

    private val requestHealthPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            refreshStepsFromHealth()
            syncRpgFromHealth()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Database setup
        db = Room.databaseBuilder(
            applicationContext,
            GritLockDatabase::class.java, "gritlock-db"
        ).fallbackToDestructiveMigration().build()

        healthConnectManager = HealthConnectManager(this)
        
        // Initial setup routines
        initChallenges()
        handleGoalProgression()
        handleBankReset()

        // State for active workout tracking
        val repCountState = mutableIntStateOf(0)
        var currentExerciseType by mutableStateOf(ExerciseType.PUSHUP)
        var currentGoal by mutableIntStateOf(10)

        // Exercise manager initialization
        exerciseManager = ExerciseTrackerManager(
            context = this,
            onRepCountChanged = { count ->
                repCountState.intValue = count
            },
            onWorkoutComplete = { reps ->
                lifecycleScope.launch {
                    val xpGained = reps * 5
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
                        now.minusSeconds(300),
                        now
                    )
                }
            }
        )

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        checkPermissions()
        checkHealthPermissions()

        setContent {
            val prefs = remember { getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
            var appTheme by remember { mutableStateOf(prefs.getString("app_theme", "Default") ?: "Default") }
            
            LaunchedEffect(Unit) {
                while(true) {
                    val currentTheme = prefs.getString("app_theme", "Default") ?: "Default"
                    if (appTheme != currentTheme) {
                        appTheme = currentTheme
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            GritLockTheme(themeName = appTheme) {
                val groups by db.dao().getAllGroups().collectAsState(initial = emptyList())
                val history by db.dao().getHistory().collectAsState(initial = emptyList())
                val userStats by db.dao().getUserStats().collectAsState(initial = null)
                val challenges by db.dao().getChallenges().collectAsState(initial = emptyList())
                val tasks by db.dao().getAllTasks().collectAsState(initial = emptyList())

                val todayTotals = remember(history, _stepCount.intValue) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val startOfDay = cal.timeInMillis

                    val totalsMap = history.filter { it.timestamp >= startOfDay }
                        .groupBy { it.exerciseType }
                        .mapValues { entry -> entry.value.sumOf { it.repsCompleted } }

                    val mutableTotals = totalsMap.toMutableMap()
                    mutableTotals[ExerciseType.STEPS.name] = _stepCount.intValue
                    mutableTotals
                }

                var currentScreen by remember { mutableStateOf("groups") }
                var trackingMode by remember { mutableStateOf(TrackingMode.CAMERA) }
                var editingGroup by remember { mutableStateOf<AppGroup?>(null) }
                var calibrationExercise by remember { mutableStateOf<ExerciseType?>(null) }
                
                Scaffold(
                    bottomBar = {
                        if (currentScreen != "track" && currentScreen != "create" && currentScreen != "calibrate") {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                NavigationItem("Groups", Icons.Default.Home, currentScreen == "groups") { currentScreen = "groups" }
                                NavigationItem("Quests", Icons.Default.Assignment, currentScreen == "quests") { currentScreen = "quests" }
                                NavigationItem("Stats", Icons.AutoMirrored.Filled.ShowChart, currentScreen == "stats") { currentScreen = "stats" }
                                NavigationItem("Profile", Icons.Default.Person, currentScreen == "profile") { currentScreen = "profile" }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
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
                                    onSettingsClick = { currentScreen = "profile" },
                                    onEmergencyBypassClick = { /* Logic */ },
                                    onChallengeClick = { challenge ->
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
                                        currentExerciseType = type
                                        currentGoal = goal
                                        repCountState.intValue = 0

                                        initializeAnalyzers(type, userStats)
                                        exerciseManager.startTracking(type, trackingMode, goal, userStats?.calibrations?.get("${type.name}_${trackingMode.name}"))
                                        currentScreen = "track"
                                    }
                                )
                            }
                            "quests" -> {
                                TodoScreen(
                                    tasks = tasks,
                                    onAddTask = { task ->
                                        lifecycleScope.launch {
                                            db.dao().insertTask(task)
                                        }
                                    },
                                    onToggleTask = { task ->
                                        lifecycleScope.launch {
                                            val updated = task.copy(isCompleted = !task.isCompleted)
                                            db.dao().updateTask(updated)
                                            if (updated.isCompleted) {
                                                val intGain = 50L * task.predictedPomodoros
                                                grantStatXp(StatType.INT, intGain)
                                                Toast.makeText(this@MainActivity, "Quest Complete! +$intGain INT XP", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onDeleteTask = { task ->
                                        lifecycleScope.launch {
                                            db.dao().deleteTask(task)
                                        }
                                    },
                                    onPomodoroComplete = { task ->
                                        lifecycleScope.launch {
                                            db.dao().updateTask(task.copy(actualPomodoros = task.actualPomodoros + 1))
                                        }
                                    }
                                )
                            }
                            "stats" -> {
                                StatsScreen(
                                    history = history,
                                    tasks = tasks
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
                            "track" -> {
                                val exerciseRequirements = remember(currentExerciseType, currentGoal) {
                                    listOf(ExerciseRequirement(currentExerciseType.name, currentGoal))
                                }
                                LockOverlayScreen(
                                    targetApp = "Manual Workout",
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
                                    onEmergencyBypass = {
                                        currentScreen = "groups"
                                    },
                                    onNextExercise = {
                                        currentScreen = "groups"
                                    },
                                    onStopExercise = {
                                        currentScreen = "groups"
                                    },
                                    onUseBankedReps = { type, count ->
                                        lifecycleScope.launch {
                                            val stats = userStats ?: return@launch
                                            val currentBanked = stats.bankedReps[type] ?: 0
                                            if (currentBanked >= count) {
                                                val newBank = stats.bankedReps.toMutableMap()
                                                newBank[type] = currentBanked - count
                                                db.dao().updateUserStats(stats.copy(bankedReps = newBank))
                                                repCountState.intValue += count
                                            }
                                        }
                                    },
                                    userStats = userStats
                                )
                            }
                            "calibrate" -> {
                                CalibrationScreen(
                                    exerciseType = calibrationExercise ?: ExerciseType.PUSHUP,
                                    mode = trackingMode,
                                    onCalibrationComplete = { calibration ->
                                        lifecycleScope.launch {
                                            val currentStats = userStats ?: UserStats()
                                            val updatedCalibrations = currentStats.calibrations.toMutableMap()
                                            updatedCalibrations["${calibrationExercise?.name}_${trackingMode.name}"] = calibration
                                            db.dao().updateUserStats(currentStats.copy(calibrations = updatedCalibrations))
                                            currentScreen = "profile"
                                        }
                                    },
                                    onBack = { currentScreen = "profile" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        } else {
            setupStepSensor()
        }
    }

    private fun checkHealthPermissions() {
        lifecycleScope.launch {
            val permissions = setOf(
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getWritePermission(StepsRecord::class),
                HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                HealthPermission.getWritePermission(ExerciseSessionRecord::class),
                HealthPermission.getReadPermission(SleepSessionRecord::class)
            )
            val granted = healthConnectManager.hasPermissions()
            if (!granted) {
                requestHealthPermissionLauncher.launch(permissions.toTypedArray())
            } else {
                refreshStepsFromHealth()
                syncRpgFromHealth()
            }
        }
    }

    private fun setupStepSensor() {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount < 0) {
                initialStepCount = event.values[0]
            }
            _stepCount.intValue = (event.values[0] - initialStepCount).toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun refreshStepsFromHealth() {
        lifecycleScope.launch {
            val steps = healthConnectManager.readTodaySteps()
            if (steps > _stepCount.intValue) {
                _stepCount.intValue = steps.toInt()
            }
        }
    }

    private fun syncRpgFromHealth() {
        lifecycleScope.launch {
            val sleepMinutes = healthConnectManager.readLastNightSleepDurationMinutes()
            if (sleepMinutes >= 420) { // 7 hours
                grantStatXp(StatType.VIT, 100)
                Log.d("RPG", "Sleep goal met! +100 VIT XP")
            }
        }
    }

    private fun updateUserStats(xp: Int, exerciseType: String, reps: Int) {
        lifecycleScope.launch {
            val currentStats = db.dao().getUserStats().first() ?: UserStats()
            
            // 1. Update XP and Level
            var newTotalXp = currentStats.totalXp + xp
            var newLevel = currentStats.level
            while (newTotalXp >= newLevel * 1000) {
                newTotalXp -= newLevel * 1000
                newLevel++
            }

            // 2. Update Banked Reps
            val newBankedReps = currentStats.bankedReps.toMutableMap()
            val currentBanked = newBankedReps[exerciseType] ?: 0
            newBankedReps[exerciseType] = currentBanked + reps

            // 3. Update primary stats based on exercise
            val newStats = currentStats.copy(
                totalXp = newTotalXp,
                level = newLevel,
                bankedReps = newBankedReps
            )
            
            db.dao().updateUserStats(newStats)
            
            // Specific stat gains
            when(exerciseType) {
                ExerciseType.PUSHUP.name, ExerciseType.PULLUP.name, ExerciseType.DIP.name -> grantStatXp(StatType.STR, xp.toLong())
                ExerciseType.SQUAT.name, ExerciseType.HINGE.name -> grantStatXp(StatType.STR, xp.toLong())
                ExerciseType.STEPS.name -> grantStatXp(StatType.AGI, (reps / 10).toLong())
            }
        }
    }

    private fun grantStatXp(type: StatType, amount: Long) {
        lifecycleScope.launch {
            val currentStats = db.dao().getUserStats().first() ?: UserStats()
            val updated = when(type) {
                StatType.STR -> currentStats.copy(strXp = currentStats.strXp + amount)
                StatType.AGI -> currentStats.copy(agiXp = currentStats.agiXp + amount)
                StatType.VIT -> currentStats.copy(vitXp = currentStats.vitXp + amount)
                StatType.INT -> currentStats.copy(intXp = currentStats.intXp + amount)
                StatType.SEN -> currentStats.copy(senXp = currentStats.senXp + amount)
                StatType.CHA -> currentStats.copy(chaXp = currentStats.chaXp + amount)
            }
            db.dao().updateUserStats(updated)
        }
    }

    private fun initializeAnalyzers(type: ExerciseType, userStats: UserStats?) {
        when (type) {
            ExerciseType.PUSHUP -> pushupAnalyzer = PushupAnalyzer(
                exerciseManager,
                userStats?.calibrations?.get("${type.name}_${TrackingMode.CAMERA.name}")
            )
            ExerciseType.SQUAT -> squatAnalyzer = SquatAnalyzer(
                exerciseManager,
                userStats?.calibrations?.get("${type.name}_${TrackingMode.CAMERA.name}")
            )
            ExerciseType.SITUP -> situpAnalyzer = SitupAnalyzer(exerciseManager)
            ExerciseType.PULLUP -> pullupAnalyzer = PullupAnalyzer(exerciseManager)
            ExerciseType.DIP -> dipAnalyzer = DipAnalyzer(exerciseManager)
            ExerciseType.HINGE -> hingeAnalyzer = HingeAnalyzer(exerciseManager)
            ExerciseType.ROW -> rowAnalyzer = RowAnalyzer(exerciseManager)
            ExerciseType.PLANK -> plankAnalyzer = PlankAnalyzer(exerciseManager)
            else -> {}
        }
    }

    private fun initChallenges() {
        lifecycleScope.launch {
            val existing = db.dao().getChallenges().first()
            if (existing.isEmpty()) {
                db.dao().upsertChallenge(Challenge(
                    id = "morning_warrior",
                    title = "Morning Warrior",
                    description = "Do 20 pushups before 10 AM",
                    requirements = listOf(ExerciseRequirement(ExerciseType.PUSHUP.name, 20)),
                    xpReward = 100
                ))
            }
        }
    }

    private fun handleGoalProgression() {
        // Future logic for dynamic goal scaling
    }

    private fun handleBankReset() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)
            val lastReset = prefs.getLong("last_bank_reset", 0)
            val now = System.currentTimeMillis()
            
            val cal = Calendar.getInstance()
            cal.timeInMillis = lastReset
            val lastResetDay = cal.get(Calendar.DAY_OF_YEAR)
            
            cal.timeInMillis = now
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            if (currentDay != lastResetDay) {
                val stats = db.dao().getUserStats().first()
                stats?.let {
                    db.dao().updateUserStats(it.copy(bankedReps = emptyMap()))
                }
                prefs.edit().putLong("last_bank_reset", now).apply()
            }
        }
    }

    @Composable
    fun RowScope.NavigationItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
        NavigationBarItem(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            icon = { Icon(icon, contentDescription = label) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
