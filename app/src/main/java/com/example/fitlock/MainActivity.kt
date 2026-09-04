/**
 * MainActivity is the "Entry Point" of the application.
 * When the user taps the app icon, this is the first class that runs.
 * It inherits from ComponentActivity, which provides the foundation for using Jetpack Compose.
 */
package com.example.fitlock

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.fitlock.data.*
import com.example.fitlock.exercise.*
import com.example.fitlock.service.GauntletService
import com.example.fitlock.service.GritLockAccessibilityService
import com.example.fitlock.service.MovementReminderWorker
import com.example.fitlock.ui.*
import com.example.fitlock.ui.theme.GritLockTheme
import com.example.fitlock.utils.HealthConnectManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SensorEventListener {

    @Inject lateinit var db: GritLockDatabase
    @Inject lateinit var repository: GritLockRepository
    
    private lateinit var exerciseManager: ExerciseTrackerManager
    
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
    private var healthBaseSteps = 0L
    private var bankedUsedInSession = 0

    // Session State
    private var activeRequirements by mutableStateOf<List<ExerciseRequirement>>(emptyList())
    private var currentRequirementIndex by mutableIntStateOf(0)
    private val _isStationary = mutableStateOf(false)

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

    private var onAnkiUriReceived: ((android.net.Uri) -> Unit)? = null
    private val importAnkiLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onAnkiUriReceived?.invoke(it) }
    }

    /**
     * onCreate is a "Lifecycle Method".
     * It's called by Android when the Activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Makes the app draw behind the status bar and navigation bar (full screen look)
        enableEdgeToEdge()

        healthConnectManager = HealthConnectManager(this)
        
        // Initial setup routines
        initChallenges()
        initQuotes()
        initStats()
        handleGoalProgression()
        handleBankReset()
        
        startService(Intent(this, com.example.fitlock.service.DailyLogService::class.java))

        // State variables for the active workout session
        val repCountState = mutableIntStateOf(0)
        var currentExerciseType by mutableStateOf<ExerciseType?>(ExerciseType.PUSHUP)
        var currentGoal by mutableIntStateOf(10)

        // Initialize with default
        initializeExerciseManager(onRepCount = { repCountState.intValue = it })

        // Setup the built-in step counter sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Initial checks for permissions
        checkPermissions()
        checkHealthPermissions()

        val usageTracker = com.example.fitlock.utils.UsageTracker(this)
        lifecycleScope.launch {
            usageTracker.syncReclaimedTime()
        }

        // Schedule Smart Nudge
        val workManager = androidx.work.WorkManager.getInstance(this)
        val nudgeRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.fitlock.service.SmartNudgeWorker>(
            1, java.util.concurrent.TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "smart_nudge",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            nudgeRequest
        )

        // Schedule Movement Reminder (Every 2 hours)
        val movementRequest = androidx.work.PeriodicWorkRequestBuilder<MovementReminderWorker>(
            2, java.util.concurrent.TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "movement_reminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            movementRequest
        )

        // Schedule Daily Reset
        com.example.fitlock.service.DailyResetWorker.schedule(this)

        /**
         * setContent defines the UI of the app using Composable functions.
         * Everything inside here is written in declarative Compose syntax.
         */
        setContent {
            // remember {} caches values so they aren't reset when the UI refreshes
            val prefs = remember { getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
            var appTheme by remember { mutableStateOf(prefs.getString("app_theme", "Default") ?: "Default") }
            var darkModeSetting by remember { mutableStateOf(prefs.getString("dark_mode", "System") ?: "System") }
            
            // A side effect that periodically checks if the theme setting changed
            LaunchedEffect(Unit) {
                while(true) {
                    val currentTheme = prefs.getString("app_theme", "Default") ?: "Default"
                    if (appTheme != currentTheme) {
                        appTheme = currentTheme
                    }
                    val currentDarkMode = prefs.getString("dark_mode", "System") ?: "System"
                    if (darkModeSetting != currentDarkMode) {
                        darkModeSetting = currentDarkMode
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            val userStatsFlow = remember { db.dao().getUserStats() }
            val statsState by userStatsFlow.collectAsState(initial = null)

            // Flashcard Import callback
            val homeViewModel: HomeViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                homeViewModel.initDailyPledgeIfMissing()
            }
            onAnkiUriReceived = { uri -> homeViewModel.importAnkiDeck(this@MainActivity, uri) }

            // Wrap the whole UI in our custom Theme
            GritLockTheme(
                themeName = appTheme, 
                darkModeSetting = darkModeSetting,
                activeArchetype = statsState?.activeTheme ?: "DEFAULT"
            ) {
                val locksViewModel: LocksViewModel = hiltViewModel()
                val userViewModel: UserViewModel = hiltViewModel()

                val groups by locksViewModel.appGroups.collectAsState()
                val vaultItems by locksViewModel.vaultItems.collectAsState()
                val gauntlets by locksViewModel.gauntlets.collectAsState()
                val advancedWorkouts by locksViewModel.advancedWorkouts.collectAsState()
                val habitDefinitions by locksViewModel.habitDefinitions.collectAsState()
                
                val userStats = statsState
                val challenges by homeViewModel.challenges.collectAsState()
                val currentPledge by homeViewModel.currentPledge.collectAsState()
                val deckSummaries by homeViewModel.deckSummaries.collectAsState()

                val themeData = com.example.fitlock.ui.theme.getThemeData(userStats?.activeTheme ?: "DEFAULT")
                
                val history by db.dao().getHistory().collectAsState(initial = emptyList())
                val gauntletHistory by db.dao().getGauntletHistory().collectAsState(initial = emptyList())
                val baselines by db.dao().getAllBaselines().collectAsState(initial = emptyList())
                val quotes by db.dao().getAllQuotes().collectAsState(initial = emptyList())
                
                val todayDate = remember { java.time.LocalDate.now().toString() }

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
                var currentScreen by remember { mutableStateOf("home") }
                var selectedDeckName by remember { mutableStateOf<String?>(null) }
                var selectedHabitDetail by remember { mutableStateOf<HabitWithDefinition?>(null) }
                var trackingMode by remember { mutableStateOf(TrackingMode.CAMERA) }
                var editingGroup by remember { mutableStateOf<AppGroup?>(null) }
                var selectedQuestId by remember { mutableStateOf<String?>(null) }
                var calibrationExercise by remember { mutableStateOf<ExerciseType?>(null) }
                var calibrationVariantName by remember { mutableStateOf<String?>(null) }
                var unlockingVaultItem by remember { mutableStateOf<VaultItem?>(null) }
                var editingGauntlet by remember { mutableStateOf<GauntletWithHabits?>(null) }
                var activeRequirements by remember { mutableStateOf<List<ExerciseRequirement>>(emptyList()) }
                var currentRequirementIndex by remember { mutableIntStateOf(0) }
                var showUrgeNegotiation by remember { mutableStateOf(false) }
                var showPostNoteDialog by remember { mutableStateOf(intent.getStringExtra("navigate_to") == "post_note") }
                var generatedLogText by remember { mutableStateOf<String?>(null) }
                var celebrationData by remember { mutableStateOf<Pair<String, String>?>(null) }
                var pendingFlashcardGoal by remember { mutableIntStateOf(0) }
                var returnPackageAfterReview by remember { mutableStateOf<String?>(null) }
                var returnGroupIdAfterReview by remember { mutableIntStateOf(-1) }
                
                LaunchedEffect(userStats?.level) {
                    if (userStats != null && userStats!!.level > 1) {
                        celebrationData = "Level Up!" to "You reached Level ${userStats!!.level}"
                    }
                }

                LaunchedEffect(intent) {
                    // Handle Shortcuts and Deep Links
                    val shortcutAction = intent.getStringExtra("shortcut_action")
                    val data = intent.data
                    
                    if (shortcutAction == "morning_plan" || data?.host == "morning_plan") {
                        currentScreen = "planning"
                    }
                    if (shortcutAction == "log_grit" || data?.host == "log_grit") {
                        showUrgeNegotiation = true
                    }
                    if (shortcutAction == "open_forge" || data?.host == "forge") {
                        currentScreen = "forge"
                    }
                    if (shortcutAction == "recommended_routine" || data?.host == "recommended_routine") {
                        // Launch Recommended Routine Quest
                        lifecycleScope.launch {
                            val quests = repository.allQuests.first()
                            val rr = quests.find { it.quest.name.contains("Recommended Routine", ignoreCase = true) }
                            if (rr != null) {
                                selectedQuestId = rr.quest.id
                                currentScreen = "quest_execution"
                            } else {
                                Toast.makeText(this@MainActivity, "RR Quest not found. Create it in Atlas first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    if (data?.scheme == "fitlock") {
                        val groupId = data.getQueryParameter("id")?.toIntOrNull()
                        if (data.host == "enable_group" && groupId != null) {
                            lifecycleScope.launch {
                                val group = db.dao().getGroupById(groupId)
                                if (group != null) db.dao().updateGroup(group.copy(isEnabled = true))
                            }
                        }
                        if (data.host == "disable_group" && groupId != null) {
                            lifecycleScope.launch {
                                val group = db.dao().getGroupById(groupId)
                                if (group != null) db.dao().updateGroup(group.copy(isEnabled = false))
                            }
                        }
                    }

                    if (intent.getStringExtra("navigate_to") == "post_note") {
                        showPostNoteDialog = true
                    }
                    if (intent.getStringExtra("navigate_to") == "morning_pledge") {
                        currentScreen = "planning"
                    }
                    if (intent.getBooleanExtra("START_MINI_WORKOUT", false)) {
                        val exType = intent.getStringExtra("EXERCISE_TYPE") ?: ExerciseType.SQUAT.name
                        val exCount = intent.getIntExtra("EXERCISE_COUNT", 20)
                        activeRequirements = listOf(ExerciseRequirement(exType, exCount))
                        currentRequirementIndex = 0
                        unlockingVaultItem = null
                        currentScreen = "track"
                    }
                    if (intent.getBooleanExtra("START_FLASHCARD_REVIEW", false)) {
                        pendingFlashcardGoal = intent.getIntExtra("FLASHCARD_COUNT", 0)
                        selectedDeckName = intent.getStringExtra("DECK_NAME") ?: "ALL"
                        returnPackageAfterReview = intent.getStringExtra("RETURN_PACKAGE")
                        returnGroupIdAfterReview = intent.getIntExtra("RETURN_GROUP_ID", -1)
                        currentScreen = "flashcard_review"
                    }
                }
                
                // Active Gauntlet Session State from Service
                val gauntletSession by GauntletService.sessionState.collectAsState()
                var lastCompletedGauntletHistoryId by remember { mutableIntStateOf(-1) }

                LaunchedEffect(gauntletSession) {
                    if (gauntletSession == null && currentScreen == "gauntlet_execution") {
                        // Service stopped, wait for history to update then show summary
                        kotlinx.coroutines.delay(500)
                        val latest = gauntletHistory.firstOrNull()
                        if (latest != null && latest.id != lastCompletedGauntletHistoryId) {
                            lastCompletedGauntletHistoryId = latest.id
                            currentScreen = "gauntlet_summary"
                        } else {
                            currentScreen = "locks"
                        }
                    }
                }
                
                /**
                 * Scaffold is a layout helper that provides slots for common UI parts
                 * like a TopBar, BottomBar, or FloatingActionButton.
                 */
                Scaffold(
                    bottomBar = {
                        // Hide the bottom bar on specific screens
                        val hideBottomBar = listOf("track", "create", "calibrate", "gauntlet_editor", "gauntlet_execution")
                        if (currentScreen !in hideBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationItem(themeData.tabHome, Icons.Default.Home, currentScreen == "home") { currentScreen = "home" }
                                NavigationItem(themeData.tabAtlas, Icons.Default.Explore, currentScreen == "atlas") { currentScreen = "atlas" }
                                NavigationItem(themeData.tabForge, Icons.Default.Build, currentScreen == "forge") { currentScreen = "forge" }
                                NavigationItem(themeData.tabLocks, Icons.Default.Lock, currentScreen == "locks") { currentScreen = "locks" }
                                NavigationItem(themeData.tabAnalytics, Icons.Default.Analytics, currentScreen == "analytics") { currentScreen = "analytics" }
                                NavigationItem(themeData.tabProfile, Icons.Default.Person, currentScreen == "profile") { currentScreen = "profile" }
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
                            "home" -> {
                                val quests by homeViewModel.allQuests.collectAsState()
                                val challenges by homeViewModel.challenges.collectAsState()
                                HomeHub(
                                    userStats = userStats,
                                    quests = quests,
                                    challenges = challenges,
                                    currentPledge = currentPledge,
                                    onQuestClick = { quest ->
                                        selectedQuestId = quest.quest.id
                                        currentScreen = "quest_execution"
                                    },
                                    onChallengeClick = { challenge ->
                                        if (challenge.requirements.isNotEmpty()) {
                                            activeRequirements = challenge.requirements
                                            currentRequirementIndex = 0
                                            unlockingVaultItem = null
                                            currentScreen = "track"
                                        }
                                    },
                                    onSettingsClick = { currentScreen = "settings" },
                                    onUrgeClick = {
                                        showUrgeNegotiation = true
                                    },
                                    onPledgeClick = { 
                                        if (currentPledge?.status == "COMMITTED") {
                                            currentScreen = "evening_review"
                                        } else {
                                            currentScreen = "pledge" 
                                        }
                                    },
                                    onViewLogClick = {
                                        lifecycleScope.launch {
                                            val date = java.util.Date()
                                            val exporter = com.example.fitlock.utils.MarkdownExporter(this@MainActivity)
                                            val logText = exporter.generateDailyLog(date)
                                            generatedLogText = logText
                                            
                                            val file = exporter.saveLogToFile(logText, date)
                                            if (file != null) {
                                                Toast.makeText(this@MainActivity, "Log saved to: Documents/${file.name}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onStartPlanning = { currentScreen = "planning" }
                                )
                            }
                            "pledge" -> {
                                PledgeScreen(
                                    currentPledge = currentPledge,
                                    onCommitWithObjective = { objective ->
                                        lifecycleScope.launch {
                                            db.dao().upsertPledge(currentPledge!!.copy(
                                                status = "COMMITTED", 
                                                pledgeTimestamp = System.currentTimeMillis(),
                                                mainObjective = objective
                                            ))
                                        }
                                    },
                                    onSuccessWithReflection = { reflection ->
                                        lifecycleScope.launch {
                                            val updated = currentPledge!!.copy(
                                                status = "SUCCESS", 
                                                reviewTimestamp = System.currentTimeMillis(),
                                                eveningReflection = reflection
                                            )
                                            db.dao().upsertPledge(updated)
                                            
                                            // Add to Daily Log
                                            if (reflection.isNotBlank()) {
                                                db.dao().insertLogNote(DailyLogNote(content = "Evening Reflection: $reflection", type = "MANUAL"))
                                            }
                                            
                                            val stats = userStats ?: UserStats()
                                            db.dao().updateUserStats(stats.copy(
                                                willpowerXp = stats.willpowerXp + 100,
                                                sobrietyStreak = stats.sobrietyStreak + 1,
                                                longestSobrietyStreak = maxOf(stats.longestSobrietyStreak, stats.sobrietyStreak + 1)
                                            ))
                                        }
                                    },
                                    onRelapseWithReflection = { reflection ->
                                        lifecycleScope.launch {
                                            val updated = currentPledge!!.copy(
                                                status = "RELAPSED", 
                                                reviewTimestamp = System.currentTimeMillis(),
                                                eveningReflection = reflection
                                            )
                                            db.dao().upsertPledge(updated)
                                            
                                            // Add to Daily Log
                                            if (reflection.isNotBlank()) {
                                                db.dao().insertLogNote(DailyLogNote(content = "Evening Reflection (Relapse): $reflection", type = "MANUAL"))
                                            }

                                            val stats = userStats ?: UserStats()
                                            db.dao().updateUserStats(stats.copy(sobrietyStreak = 0))
                                        }
                                    },
                                    onBack = { currentScreen = "home" }
                                )
                            }
                            "locks" -> {
                                LocksHub(
                                    groups = groups,
                                    vaultItems = vaultItems,
                                    gauntlets = gauntlets,
                                    onAddGroup = { 
                                        editingGroup = null
                                        currentScreen = "create" 
                                    },
                                    onEditGroup = { group ->
                                        editingGroup = group
                                        currentScreen = "create"
                                    },
                                    onDeleteGroup = { group ->
                                        locksViewModel.deleteGroup(group)
                                    },
                                    onToggleGroup = { group ->
                                        locksViewModel.toggleGroup(group)
                                    },
                                    onAddVaultItem = { item ->
                                        locksViewModel.upsertVaultItem(item)
                                    },
                                    onDeleteVaultItem = { item ->
                                        locksViewModel.deleteVaultItem(item)
                                    },
                                    onUnlockVaultItem = { item ->
                                        if (item.requirements.isNotEmpty()) {
                                            activeRequirements = item.requirements
                                            currentRequirementIndex = 0
                                            unlockingVaultItem = item
                                            currentScreen = "track"
                                        }
                                    },
                                    onAddGauntlet = {
                                        editingGauntlet = null
                                        currentScreen = "gauntlet_editor"
                                    },
                                    onEditGauntlet = { g ->
                                        editingGauntlet = g
                                        currentScreen = "gauntlet_editor"
                                    },
                                    onDeleteGauntlet = { g ->
                                        locksViewModel.deleteGauntlet(g.gauntlet)
                                    },
                                    onToggleGauntlet = { g ->
                                        locksViewModel.upsertGauntlet(g.gauntlet.copy(isEnabled = !g.gauntlet.isEnabled))
                                    },
                                    onStartGauntlet = { g ->
                                        val intent = Intent(this@MainActivity, GauntletService::class.java).apply {
                                            action = GauntletService.ACTION_START
                                            putExtra(GauntletService.EXTRA_GAUNTLET_ID, g.gauntlet.id)
                                        }
                                        startForegroundService(intent)
                                        currentScreen = "gauntlet_execution"
                                    },
                                    onNavigateToHabitLibrary = {
                                        currentScreen = "habit_library"
                                    },
                                    onHabitClick = { habit ->
                                        selectedHabitDetail = habit
                                        currentScreen = "habit_details"
                                    }
                                )
                            }
                            "habit_details" -> {
                                selectedHabitDetail?.let { habit ->
                                    HabitDetailsScreen(
                                        habit = habit,
                                        history = gauntletHistory,
                                        onBack = { currentScreen = "locks" }
                                    )
                                }
                            }
                            "forge" -> {
                                val exerciseDefs by locksViewModel.exerciseDefinitions.collectAsState()
                                val taskDefs by locksViewModel.taskDefinitions.collectAsState()
                                val allFamilies by repository.allFamilies.collectAsState(initial = emptyList())
                                ForgeScreen(
                                    habitDefinitions = habitDefinitions,
                                    onHabitUpsert = { locksViewModel.upsertHabitDefinition(it) },
                                    onHabitDelete = { locksViewModel.deleteHabitDefinition(it) },
                                    exerciseDefinitions = exerciseDefs,
                                    onExerciseUpsert = { locksViewModel.upsertExerciseDefinition(it) },
                                    onExerciseDelete = { locksViewModel.deleteExerciseDefinition(it) },
                                    taskDefinitions = taskDefs,
                                    onTaskUpsert = { locksViewModel.upsertTaskDefinition(it) },
                                    onTaskDelete = { locksViewModel.deleteTaskDefinition(it) },
                                    deckSummaries = deckSummaries,
                                    onDeleteDeck = { homeViewModel.deleteDeck(it) },
                                    onExerciseLog = { type, count, familyId, level, variantName ->
                                        activeRequirements = listOf(ExerciseRequirement(type.name, count, variantName = variantName, familyId = familyId))
                                        currentRequirementIndex = 0
                                        unlockingVaultItem = null
                                        currentScreen = "track"
                                        
                                        val calibrationKey = if (variantName != null) "${variantName}_${trackingMode.name}" else "${type.name}_${trackingMode.name}"
                                        val calibration = userStats?.calibrations?.get(calibrationKey)
                                        
                                        exerciseManager.startTracking(type, trackingMode, count, calibration, "3-1-1-1", familyId, level, variantName)
                                    },
                                    onDeckPlay = { deckName ->
                                        selectedDeckName = deckName
                                        currentScreen = "flashcard_review"
                                    },
                                    onProgressionClick = { currentScreen = "progression" },
                                    onAddFlashcard = { card ->
                                        lifecycleScope.launch { repository.upsertFlashcard(card) }
                                    },
                                    allFamilies = allFamilies,
                                    currentProgression = userStats?.familyProgression ?: emptyMap(),
                                    onAiImport = { text ->
                                        val apiKey = getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE).getString("gemini_api_key", "") ?: ""
                                        if (apiKey.isBlank()) {
                                            Toast.makeText(this@MainActivity, "Please set Gemini API Key in Settings.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            lifecycleScope.launch {
                                                val aiManager = com.example.fitlock.ai.GeminiManager(apiKey)
                                                val result = aiManager.extractFromText(text)
                                                
                                                // Save everything extracted
                                                result.habits.forEach { repository.upsertHabitDefinition(it) }
                                                result.exercises.forEach { repository.upsertExerciseDefinition(it) }
                                                result.tasks.forEach { repository.upsertTaskDefinition(it) }
                                                result.goals.forEach { repository.upsertGoal(it) }
                                                result.projects.forEach { repository.upsertProject(it) }
                                                result.quests.forEach { repository.upsertQuest(it.quest); repository.upsertQuestBlocks(it.blocks) }
                                                
                                                Toast.makeText(this@MainActivity, "AI Forging Complete!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    title = themeData.tabForge,
                                    onBack = { currentScreen = "profile" }
                                )
                            }
                            "progression" -> {
                                val allFamilies by repository.allFamilies.collectAsState(initial = emptyList())
                                ProgressionScreen(
                                    families = allFamilies,
                                    currentProgression = userStats?.familyProgression ?: emptyMap(),
                                    calibrations = userStats?.calibrations ?: emptyMap(),
                                    onBack = { currentScreen = "forge" }
                                )
                            }
                            "analytics" -> {
                                AnalyticsScreen(history = history, gauntletHistory = gauntletHistory)
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
                                    baselines = baselines,
                                    onAddChallenge = { challenge ->
                                        lifecycleScope.launch {
                                            db.dao().upsertChallenge(challenge)
                                        }
                                    },
                                    onAddBaseline = { baseline ->
                                        lifecycleScope.launch {
                                            db.dao().upsertBaseline(baseline)
                                        }
                                    },
                                    onDeleteBaseline = { baseline ->
                                        lifecycleScope.launch {
                                            db.dao().deleteBaseline(baseline)
                                        }
                                    },
                                    onForgeClick = { currentScreen = "forge" },
                                    forgeTabName = themeData.tabForge
                                )
                            }
                            "planning" -> {
                                val unfinishedTasks by homeViewModel.unfinishedTasks.collectAsState()
                                val routines by homeViewModel.allQuests.collectAsState()
                                val exerciseDefs by locksViewModel.exerciseDefinitions.collectAsState()
                                val taskDefs by locksViewModel.taskDefinitions.collectAsState()
                                val projectTasks by repository.unfinishedProjectTasks.collectAsState(initial = emptyList())
                                currentPledge?.let { pledge ->
                                    PlanningScreen(
                                        currentPledge = pledge,
                                        unfinishedTasks = unfinishedTasks,
                                        routines = routines.filter { it.quest.type == QuestType.ROUTINE },
                                        habitDefinitions = habitDefinitions,
                                        exerciseDefinitions = exerciseDefs,
                                        taskDefinitions = taskDefs,
                                        projectTasks = projectTasks,
                                        onCommit = { updatedPledge, blocks, lockShield ->
                                            lifecycleScope.launch {
                                                repository.upsertPledge(updatedPledge)
                                                repository.createDailyQuest(blocks, lockShield)
                                                
                                                // Broadcast for Beeminder
                                                broadcastEvent("com.example.fitlock.EVENT_PLEDGE_COMMITTED", mapOf(
                                                    "date" to updatedPledge.date,
                                                    "objective" to (updatedPledge.morning.personalGoal ?: "")
                                                ))

                                                // Auto-update the markdown log
                                                val exporter = com.example.fitlock.utils.MarkdownExporter(this@MainActivity)
                                                val logText = exporter.generateDailyLog(java.util.Date())
                                                exporter.saveLogToFile(logText, java.util.Date())
                                                
                                                currentScreen = "home"
                                            }
                                        },
                                        onBack = { currentScreen = "home" }
                                    )
                                }
                            }
                            "atlas" -> {
                                val allQuestsWithBlocks by repository.allQuests.collectAsState(initial = emptyList())
                                val allGoals by repository.allGoals.collectAsState(initial = emptyList())
                                AtlasScreen(
                                    quests = allQuestsWithBlocks,
                                    goals = allGoals,
                                    onAddRoutine = {
                                        editingGauntlet = null
                                        currentScreen = "gauntlet_editor"
                                    },
                                    onEditQuest = { questWithBlocks ->
                                        // Convert Quest back to Gauntlet for the editor (temporary bridge)
                                        // This will be fully unified in next phase
                                        editingGauntlet = GauntletWithHabits(
                                            gauntlet = Gauntlet(
                                                id = questWithBlocks.quest.id.replace("daily_", "").toIntOrNull() ?: 0,
                                                name = questWithBlocks.quest.name,
                                                triggerType = questWithBlocks.quest.triggerType,
                                                triggerTime = questWithBlocks.quest.triggerTime,
                                                targetBlockGroupId = questWithBlocks.quest.targetBlockGroupId,
                                                icon = questWithBlocks.quest.icon,
                                                physicalTriggerType = questWithBlocks.quest.physicalTriggerType,
                                                physicalTriggerData = questWithBlocks.quest.physicalTriggerData
                                            ),
                                            habits = questWithBlocks.blocks.filter { it.type == BlockType.HABIT }.map { b ->
                                                HabitWithDefinition(
                                                    habit = Habit(
                                                        id = b.id.toIntOrNull() ?: 0,
                                                        gauntletId = 0,
                                                        name = b.name,
                                                        orderIndex = b.orderIndex,
                                                        estimatedDurationSeconds = b.estimatedDurationSeconds,
                                                        mediaUrl = b.mediaUrl,
                                                        mediaType = b.mediaType,
                                                        trackingType = b.trackingType,
                                                        definitionId = b.definitionId,
                                                        currentStreak = b.currentStreak,
                                                        lastCompletionTimestamp = b.lastDoneTimestamp
                                                    ),
                                                    definition = null // Will be resolved by editor if needed
                                                )
                                            }
                                        )
                                        currentScreen = "gauntlet_editor"
                                    },
                                    onDeleteQuest = { questWithBlocks ->
                                        lifecycleScope.launch {
                                            repository.deleteQuest(questWithBlocks.quest)
                                        }
                                    },
                                    onToggleQuest = { /* TODO */ },
                                    onStartQuest = { questWithBlocks ->
                                        // Convert QuestWithBlocks to GauntletWithHabits for the existing service
                                        // (Bridge until service is updated)
                                        val intent = Intent(this@MainActivity, GauntletService::class.java).apply {
                                            action = GauntletService.ACTION_START
                                            putExtra(GauntletService.EXTRA_GAUNTLET_ID, questWithBlocks.quest.id.toIntOrNull() ?: -1)
                                        }
                                        startService(intent)
                                        currentScreen = "gauntlet_execution"
                                    },
                                    onUpsertGoal = { goal ->
                                        lifecycleScope.launch { 
                                            repository.upsertGoal(goal)
                                            if (goal.isCompleted) {
                                                userViewModel.updateXp(goal.xpReward)
                                                Toast.makeText(this@MainActivity, "Goal Achieved! +${goal.xpReward} XP", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onDeleteGoal = { goal ->
                                        lifecycleScope.launch { repository.deleteGoal(goal) }
                                    },
                                    onUpsertProject = { project ->
                                        lifecycleScope.launch { repository.upsertProject(project) }
                                    },
                                    onDeleteProject = { project ->
                                        lifecycleScope.launch { repository.deleteProject(project) }
                                    },
                                    onUpsertMilestone = { milestone ->
                                        lifecycleScope.launch { 
                                            repository.upsertMilestone(milestone)
                                            if (milestone.isCompleted) {
                                                userViewModel.updateXp(milestone.xpReward)
                                                Toast.makeText(this@MainActivity, "Milestone Cleared! +${milestone.xpReward} XP", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onDeleteMilestone = { milestone ->
                                        lifecycleScope.launch { repository.deleteMilestone(milestone) }
                                    },
                                    onUpsertProjectTask = { task ->
                                        lifecycleScope.launch { repository.upsertProjectTask(task) }
                                    },
                                    onDeleteProjectTask = { task ->
                                        lifecycleScope.launch { repository.deleteProjectTask(task) }
                                    },
                                    title = themeData.tabAtlas
                                )
                            }
                            "quest_execution" -> {
                                val allQuestsWithBlocks by repository.allQuests.collectAsState(initial = emptyList())
                                val currentQuest = allQuestsWithBlocks.find { it.quest.id == selectedQuestId }
                                if (currentQuest != null) {
                                    QuestExecutionScreen(
                                        questWithBlocks = currentQuest,
                                        onToggleBlock = { block ->
                                            lifecycleScope.launch {
                                                val updatedBlock = block.copy(isCompleted = !block.isCompleted, completionTimestamp = if (!block.isCompleted) System.currentTimeMillis() else null)
                                                repository.upsertQuestBlock(updatedBlock)
                                                
                                                // Sync with ProjectTask if link exists
                                                if (updatedBlock.sourceTaskId != null) {
                                                    // This requires a helper in repository to update ProjectTask status
                                                    repository.allGoals.first().flatMap { it.projects }.flatMap { it.tasks }
                                                        .find { it.id == updatedBlock.sourceTaskId }?.let { pt ->
                                                            repository.upsertProjectTask(pt.copy(isCompleted = updatedBlock.isCompleted))
                                                        }
                                                }
                                            }
                                        },
                                        onFinish = {
                                            lifecycleScope.launch {
                                                repository.upsertQuest(currentQuest.quest.copy(isCompletedToday = true, lastCompletedTimestamp = System.currentTimeMillis()))
                                                userViewModel.updateXp(currentQuest.quest.xpReward)
                                                currentScreen = "home"
                                            }
                                        },
                                        onBack = { currentScreen = "home" }
                                    )
                                }
                            }
                            "evening_review" -> {
                                currentPledge?.let { pledge ->
                                    RitualWizard(
                                        isMorning = false,
                                        pledge = pledge,
                                        onSave = { updated ->
                                            lifecycleScope.launch {
                                                repository.upsertPledge(updated)
                                                
                                                // Auto-update the markdown log
                                                val exporter = com.example.fitlock.utils.MarkdownExporter(this@MainActivity)
                                                val logText = exporter.generateDailyLog(java.util.Date())
                                                exporter.saveLogToFile(logText, java.util.Date())
                                                
                                                currentScreen = "home"
                                            }
                                        },
                                        onBack = { currentScreen = "home" }
                                    )
                                }
                            }
                            "settings" -> {
                                SettingsScreen(
                                    onBack = { currentScreen = "home" },
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
                                    },
                                    onNavigateToQuotes = { currentScreen = "quotes" },
                                    groups = groups
                                )
                            }
                            "quotes" -> {
                                QuotesEditor(
                                    quotes = quotes,
                                    onAddQuote = { quote ->
                                        lifecycleScope.launch {
                                            db.dao().upsertQuote(quote)
                                        }
                                    },
                                    onBack = { currentScreen = "settings" }
                                )
                            }
                            "create" -> {
                                val allDeckNames by repository.allDeckNames.collectAsState(initial = emptyList())
                                CreateGroupScreen(
                                    editingGroup = editingGroup,
                                    deckNames = allDeckNames,
                                    onSave = { group ->
                                        locksViewModel.upsertGroup(group)
                                        currentScreen = "locks"
                                    },
                                    onBack = { currentScreen = "locks" }
                                )
                            }
                            "gauntlet_editor" -> {
                                val currentAdvanced = editingGauntlet?.let { g -> advancedWorkouts.find { it.gauntlet.id == g.gauntlet.id } }
                                GauntletEditor(
                                    editingGauntlet = editingGauntlet,
                                    editingAdvanced = currentAdvanced,
                                    groups = groups,
                                    onSave = { gauntlet, habits, blocks, tasks ->
                                        lifecycleScope.launch {
                                            // Old way (for backward compatibility during transition)
                                            val gId = locksViewModel.upsertGauntlet(gauntlet).await().toInt()
                                            locksViewModel.deleteHabitsForGauntlet(gId)
                                            habits.forEach { locksViewModel.upsertHabit(it.copy(gauntletId = gId)) }
                                            locksViewModel.deleteBlocksForGauntlet(gId)
                                            blocks.forEach { blockWithExercises ->
                                                val bId = locksViewModel.upsertWorkoutBlock(blockWithExercises.block.copy(gauntletId = gId)).await().toInt()
                                                blockWithExercises.exercises.forEach {
                                                    locksViewModel.upsertWorkoutExercise(it.copy(blockId = bId))
                                                }
                                            }
                                            
                                            // New way (Unified Quest System)
                                            repository.saveGauntletAsQuest(gauntlet, habits, blocks, tasks)
                                            
                                            currentScreen = "locks"
                                        }
                                    },
                                    onBack = { currentScreen = "locks" }
                                )
                            }
                            "gauntlet_execution" -> {
                                gauntletSession?.let { session ->
                                    GauntletExecutionScreen(
                                        gauntlet = session.gauntlet,
                                        advancedWorkout = session.advancedWorkout,
                                        currentHabitIndex = session.currentHabitIndex,
                                        currentBlockIndex = session.currentBlockIndex,
                                        currentBlockExerciseIndex = session.currentBlockExerciseIndex,
                                        currentBlockSet = session.currentBlockSet,
                                        elapsedSeconds = session.elapsedSeconds,
                                        currentReps = session.currentReps,
                                        personalBests = session.personalBests,
                                        isInBuffer = session.isInBuffer,
                                        isResting = session.isResting,
                                        bufferRemainingSeconds = session.gauntlet.gauntlet.bufferSeconds - session.bufferElapsedSeconds,
                                        onNext = {
                                            val intent = Intent(this@MainActivity, GauntletService::class.java).apply {
                                                action = GauntletService.ACTION_NEXT
                                            }
                                            startService(intent)
                                        },
                                        onStop = {
                                            val intent = Intent(this@MainActivity, GauntletService::class.java).apply {
                                                action = GauntletService.ACTION_STOP
                                            }
                                            startService(intent)
                                            currentScreen = "locks"
                                        },
                                        onUpdateReps = { reps ->
                                            val intent = Intent(this@MainActivity, GauntletService::class.java).apply {
                                                action = GauntletService.ACTION_UPDATE_REPS
                                                putExtra(GauntletService.EXTRA_REPS, reps)
                                            }
                                            startService(intent)
                                        }
                                    )
                                } ?: run {
                                    // Managed by LaunchedEffect(gauntletSession)
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            "gauntlet_summary" -> {
                                val latest = gauntletHistory.firstOrNull()
                                if (latest != null) {
                                    GauntletSummaryScreen(
                                        history = latest,
                                        onDone = { currentScreen = "locks" }
                                    )
                                } else {
                                    currentScreen = "locks"
                                }
                            }
                            "flashcard_review" -> {
                                val deckToReview = selectedDeckName ?: "Default"
                                FlashcardReviewScreen(
                                    deckName = deckToReview,
                                    repository = repository,
                                    requiredCount = if (pendingFlashcardGoal > 0) pendingFlashcardGoal else null,
                                    onRequirementMet = {
                                        val returnGId = returnGroupIdAfterReview
                                        if (returnGId != -1) {
                                            lifecycleScope.launch {
                                                val now = System.currentTimeMillis()
                                                val group = db.dao().getGroupById(returnGId)
                                                if (group != null) {
                                                    db.dao().updateGroup(group.copy(lastUnlockedTimestamp = now))
                                                    LockStatusManager.updateUnlock(returnGId, now)
                                                }
                                            }
                                        }
                                    },
                                    onFinish = {
                                        if (pendingFlashcardGoal > 0) {
                                            val returnPkg = returnPackageAfterReview
                                            if (returnPkg != null) {
                                                val launchIntent = packageManager.getLaunchIntentForPackage(returnPkg)
                                                if (launchIntent != null) {
                                                    startActivity(launchIntent)
                                                }
                                                pendingFlashcardGoal = 0
                                                returnPackageAfterReview = null
                                                returnGroupIdAfterReview = -1
                                                currentScreen = "locks"
                                            } else {
                                                repCountState.intValue = pendingFlashcardGoal
                                                pendingFlashcardGoal = 0
                                                currentScreen = "track"
                                            }
                                        } else {
                                            currentScreen = "home"
                                            selectedDeckName = null
                                        }
                                    }
                                )
                            }
                            "calibrate" -> {
                                calibrationExercise?.let { type ->
                                    CalibrationScreen(
                                        exerciseType = type,
                                        variantName = calibrationVariantName,
                                        mode = trackingMode,
                                        onCalibrationComplete = { cal ->
                                            lifecycleScope.launch {
                                                val stats = userStats ?: UserStats()
                                                val newCalibrations = stats.calibrations.toMutableMap()
                                                newCalibrations[cal.exerciseType] = cal
                                                db.dao().updateUserStats(stats.copy(calibrations = newCalibrations))
                                                
                                                if (activeRequirements.isNotEmpty()) {
                                                    currentScreen = "track"
                                                } else {
                                                    currentScreen = "settings"
                                                }
                                                calibrationVariantName = null
                                            }
                                        },
                                        onBack = { 
                                            if (activeRequirements.isNotEmpty()) {
                                                currentScreen = "track"
                                            } else {
                                                currentScreen = "settings"
                                            }
                                            calibrationVariantName = null
                                        }
                                    )
                                }
                            }
                            "track" -> {
                                val currentReq = activeRequirements.getOrNull(currentRequirementIndex)
                                if (currentReq != null) {
                                    var isStationaryState by remember { mutableStateOf(false) }
                                    
                                    // Update local tracking state for current requirement
                                    LaunchedEffect(currentRequirementIndex, trackingMode) {
                                        val req = activeRequirements[currentRequirementIndex]
                                        val exerciseType = try { ExerciseType.valueOf(req.type) } catch(e: Exception) { ExerciseType.PUSHUP }
                                        currentExerciseType = exerciseType
                                        currentGoal = req.count
                                        repCountState.intValue = 0
                                        bankedUsedInSession = 0
                                        
                                        initializeExerciseManager(
                                            onRepCount = { repCountState.intValue = it },
                                            onStationaryStatusChanged = { isSteady -> isStationaryState = isSteady }
                                        )

                                        if (exerciseType != ExerciseType.APP_USAGE) {
                                            initializeAnalyzers(exerciseType, userStats)
                                            val calibrationKey = if (req.variantName != null) "${req.variantName}_${trackingMode.name}" else "${exerciseType.name}_${trackingMode.name}"
                                            val calibration = userStats?.calibrations?.get(calibrationKey)
                                            
                                            if (calibration == null && trackingMode == TrackingMode.CAMERA) {
                                                calibrationExercise = exerciseType
                                                calibrationVariantName = req.variantName
                                                currentScreen = "calibrate"
                                            } else {
                                                exerciseManager.startTracking(
                                                    type = exerciseType, 
                                                    mode = trackingMode, 
                                                    goal = currentGoal, 
                                                    calibration = calibration,
                                                    variantName = req.variantName
                                                )
                                            }
                                        } else {
                                            exerciseManager.startTracking(ExerciseType.APP_USAGE, TrackingMode.POCKET, currentGoal)
                                        }
                                    }

                                    DisposableEffect(Unit) {
                                        onDispose {
                                            exerciseManager.shutdown()
                                        }
                                    }

                                    LockOverlayScreen(
                                        targetApp = unlockingVaultItem?.title ?: "Workout Mode",
                                        repCount = repCountState.intValue,
                                        isStationary = isStationaryState,
                                        exerciseRequirements = activeRequirements,
                                        currentExerciseIndex = currentRequirementIndex,
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
                                        onSwitchToPocket = {
                                            trackingMode = TrackingMode.POCKET
                                            exerciseManager.switchToPocketMode()
                                        },
                                        onEmergencyBypass = { 
                                            currentScreen = if (unlockingVaultItem != null) "vault" else "home"
                                            unlockingVaultItem = null
                                        },
                                        onNextExercise = {
                                            lifecycleScope.launch {
                                                val totalReps = repCountState.intValue
                                                val physicalReps = (totalReps - bankedUsedInSession).coerceAtLeast(0)
                                                
                                                if (physicalReps > 0) {
                                                    val xpGained = physicalReps * 2
                                                    
                                                    db.dao().insertWorkout(
                                                        WorkoutHistory(
                                                            exerciseType = currentExerciseType!!.name,
                                                            repsCompleted = physicalReps,
                                                            appGroupId = 0,
                                                            xpGained = xpGained
                                                        )
                                                    )
                                                    updateUserStats(xpGained, currentExerciseType!!.name, physicalReps)
                                                    
                                                    val now = java.time.Instant.now()
                                                    val duration = exerciseManager.getDurationSeconds()
                                                    healthConnectManager.writeExerciseSession(
                                                        currentExerciseType!!.name,
                                                        physicalReps,
                                                        now.minusSeconds(duration),
                                                        now
                                                    )
                                                }

                                                if (currentRequirementIndex < activeRequirements.size - 1) {
                                                    currentRequirementIndex++
                                                } else {
                                                    unlockingVaultItem?.let { item ->
                                                        db.dao().upsertVaultItem(item.copy(lastUnlockedTimestamp = System.currentTimeMillis()))
                                                        currentScreen = "vault"
                                                    } ?: run {
                                                        currentScreen = "home"
                                                    }
                                                    unlockingVaultItem = null
                                                }
                                            }
                                        },
                                        onStopExercise = {
                                            lifecycleScope.launch {
                                                val totalReps = repCountState.intValue
                                                val physicalReps = (totalReps - bankedUsedInSession).coerceAtLeast(0)
                                                
                                                if (physicalReps > 0 && totalReps < currentGoal) {
                                                    val xpGained = physicalReps * 2
                                                    
                                                    db.dao().insertWorkout(
                                                        WorkoutHistory(
                                                            exerciseType = currentExerciseType!!.name,
                                                            repsCompleted = physicalReps,
                                                            appGroupId = 0,
                                                            xpGained = xpGained
                                                        )
                                                    )
                                                    updateUserStats(xpGained, currentExerciseType!!.name, physicalReps)
                                                    
                                                    val now = java.time.Instant.now()
                                                    val duration = exerciseManager.getDurationSeconds()
                                                    healthConnectManager.writeExerciseSession(
                                                        currentExerciseType!!.name,
                                                        physicalReps,
                                                        now.minusSeconds(duration),
                                                        now
                                                    )
                                                }
                                                // Send user to Home screen on stop
                                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                                    addCategory(Intent.CATEGORY_HOME)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                startActivity(homeIntent)

                                                if (unlockingVaultItem != null) {
                                                    currentScreen = "vault"
                                                } else {
                                                    currentScreen = "home"
                                                }
                                                unlockingVaultItem = null
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
                                                    bankedUsedInSession += count
                                                    exerciseManager.addManualReps(count)
                                                }
                                            }
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
                                                    val serviceIntent = Intent(this@MainActivity, GritLockAccessibilityService::class.java).apply {
                                                        action = "START_APP_USAGE_TRACKING"
                                                        putExtra("vault_id", unlockingVaultItem?.id ?: -1)
                                                        putExtra("target_packages", pkgs.toTypedArray())
                                                        putExtra("seconds", currentReq.count)
                                                    }
                                                    startService(serviceIntent)
                                                    startActivity(launchIntent)
                                                }
                                            }
                                        },
                                        onStartFlashcardReview = { count ->
                                            pendingFlashcardGoal = count
                                            currentScreen = "flashcard_review"
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (showUrgeNegotiation) {
                        UrgeNegotiationDialog(
                            onDismiss = { showUrgeNegotiation = false },
                            availableQuotes = quotes,
                            themeUrgeLabel = themeData.tabUrge.replace("Urge", "Grit"),
                            onConfirm = { event ->
                                lifecycleScope.launch {
                                    db.dao().insertUrgeEvent(event)
                                    showUrgeNegotiation = false
                                    
                                    // MANDATORY CHALLENGE: Launch Overlay for "Grit Trial"
                                    val intent = Intent(this@MainActivity, LockOverlayActivity::class.java).apply {
                                        putExtra("target_app", "Grit Trial")
                                        putExtra("exercise_type", ExerciseType.PUSHUP.name)
                                        putExtra("target_reps", 15)
                                    }
                                    startActivity(intent)
                                }
                            }
                        )
                    }

                    if (showPostNoteDialog) {
                        var noteContent by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showPostNoteDialog = false },
                            title = { Text("Daily Log Entry") },
                            text = {
                                OutlinedTextField(
                                    value = noteContent,
                                    onValueChange = { noteContent = it },
                                    label = { Text("What's on your mind?") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (noteContent.isNotBlank()) {
                                        lifecycleScope.launch {
                                            db.dao().insertLogNote(DailyLogNote(content = noteContent))
                                            showPostNoteDialog = false
                                        }
                                    }
                                }) { Text("POST") }
                            },
                            dismissButton = { TextButton(onClick = { showPostNoteDialog = false }) { Text("Cancel") } }
                        )
                    }

                    if (generatedLogText != null) {
                        AlertDialog(
                            onDismissRequest = { generatedLogText = null },
                            title = { Text("Daily Discipline Log") },
                            text = { 
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text(generatedLogText!!, style = MaterialTheme.typography.bodySmall) 
                                }
                            },
                            confirmButton = { Button(onClick = { generatedLogText = null }) { Text("Close") } }
                        )
                    }

                    celebrationData?.let { data ->
                        HeroicCelebration(
                            title = data.first,
                            subtitle = data.second,
                            onDismiss = { celebrationData = null }
                        )
                    }
                }
            }
        }
    }

    private fun initQuotes() {
        lifecycleScope.launch {
            val count = db.dao().getAllQuotes().first().size
            if (count == 0) {
                val defaultQuotes = listOf(
                    MotivationalQuote(text = "You are stronger than your excuses.", category = "Internal Voice", subCategory = "Laziness"),
                    MotivationalQuote(text = "The pain of discipline is far less than the pain of regret.", category = "Internal Voice", subCategory = "Rationalization"),
                    MotivationalQuote(text = "Discipline is doing what needs to be done, even if you don't want to do it.", category = "Internal Voice"),
                    MotivationalQuote(text = "Don't let your short-term desires steal your long-term dreams.", category = "Internal Voice", subCategory = "Greed"),
                    MotivationalQuote(text = "Fear is a liar. Step through it.", category = "Internal Voice", subCategory = "Fear")
                )
                defaultQuotes.forEach { db.dao().upsertQuote(it) }
            }
        }
    }

    private fun initStats() {
        lifecycleScope.launch {
            val stats = db.dao().getUserStats().first()
            if (stats == null) {
                db.dao().updateUserStats(UserStats(id = 1))
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
            
            val opmBase = 10
            val dailyIncrement = 3
            val currentOpmGoal = (opmBase + (dayOfYear * dailyIncrement)).coerceAtMost(100)

            val challenges = listOf(
                Challenge("1", "Morning Pushups", "Do 20 pushups to start your day", listOf(ExerciseRequirement(ExerciseType.PUSHUP.name, 20)), 100),
                Challenge("2", "Squat Master", "Complete 50 squats", listOf(ExerciseRequirement(ExerciseType.SQUAT.name, 50)), 250),
                Challenge("3", "Core Strength", "Hold a plank for 60 seconds", listOf(ExerciseRequirement(ExerciseType.PLANK.name, 60)), 150),
                Challenge("opm_classic", "One Punch Man Challenge", "Saitama's Legend: 100 Pushups, 100 Situps, 100 Squats, and 10km (10,000 steps).", listOf(
                    ExerciseRequirement(ExerciseType.PUSHUP.name, 100),
                    ExerciseRequirement(ExerciseType.SITUP.name, 100),
                    ExerciseRequirement(ExerciseType.SQUAT.name, 100),
                    ExerciseRequirement(ExerciseType.STEPS.name, 10000)
                ), 1000),
                Challenge("solo_leveling", "Solo Leveling: Daily Quest", "Pushups (100), Squats (100), Situps (100), Pullups (20). Don't fail the penalty!", listOf(
                    ExerciseRequirement(ExerciseType.PUSHUP.name, 100),
                    ExerciseRequirement(ExerciseType.SQUAT.name, 100),
                    ExerciseRequirement(ExerciseType.SITUP.name, 100),
                    ExerciseRequirement(ExerciseType.PULLUP.name, 20)
                ), 1000),
                Challenge("rr_routine", "Recommended Routine", "Full body strength following the RR protocol.", listOf(
                    ExerciseRequirement(ExerciseType.PULLUP.name, 15),
                    ExerciseRequirement(ExerciseType.DIP.name, 15),
                    ExerciseRequirement(ExerciseType.SQUAT.name, 15),
                    ExerciseRequirement(ExerciseType.HINGE.name, 15),
                    ExerciseRequirement(ExerciseType.PUSHUP.name, 15),
                    ExerciseRequirement(ExerciseType.ROW.name, 15)
                ), 1500)
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

    private fun initializeExerciseManager(onRepCount: (Int) -> Unit, onStationaryStatusChanged: (Boolean) -> Unit = {}) {
        if (::exerciseManager.isInitialized) exerciseManager.shutdown()
        exerciseManager = ExerciseTrackerManager(
            context = this,
            onRepCountChanged = { count ->
                onRepCount(count)
            },
            onWorkoutComplete = { reps, familyId, level -> 
                lifecycleScope.launch {
                    handleWorkoutSuccess(reps, familyId, level)
                }
            },
            onStationaryStatusChanged = onStationaryStatusChanged
        )
    }

    private suspend fun handleWorkoutSuccess(reps: Int, familyId: String?, level: Int) {
        val stats = db.dao().getUserStats().first() ?: UserStats()
        
        // 1. Basic XP and History
        val xpGained = reps * 2 
        updateUserStats(xpGained, "GENERAL", 0) // Bridging to existing stats logic
        
        // Broadcast for Tasker/Beeminder
        broadcastEvent("com.example.fitlock.EVENT_WORKOUT_COMPLETE", mapOf(
            "reps" to reps,
            "family" to (familyId ?: "NONE"),
            "level" to level,
            "xp" to xpGained
        ))

        // 2. Progression Check
        if (familyId != null) {
            val currentFamilyLevel = stats.familyProgression[familyId] ?: 1
            if (level == currentFamilyLevel) {
                val levelData = repository.getLevelData(familyId, level)
                if (levelData != null && reps >= levelData.unlockRequirementReps) {
                    // Level Up!
                    val newProgression = stats.familyProgression.toMutableMap()
                    newProgression[familyId] = level + 1
                    repository.updateStats(stats.copy(familyProgression = newProgression))
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Skill Level Up! Next variant unlocked.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        // 3. Clear requirements logic
        val currentReq = activeRequirements.getOrNull(currentRequirementIndex)
        if (currentReq != null) {
            if (currentRequirementIndex < activeRequirements.size - 1) {
                currentRequirementIndex++
            } else {
                finishWorkoutSession()
            }
        }
    }

    private fun finishWorkoutSession() {
        // Deactivate Intermittent Lock
        val deactivateIntent = Intent(this, com.example.fitlock.service.GritLockAccessibilityService::class.java).apply {
            action = "DEACTIVATE_INTERMITTENT_LOCK"
        }
        startService(deactivateIntent)

        // Log to Health Connect
        val endTime = java.time.Instant.now()
        val startTime = endTime.minusSeconds(exerciseManager.getDurationSeconds())
        lifecycleScope.launch {
            activeRequirements.forEach { req ->
                healthConnectManager.writeExerciseSession(req.type, req.count, startTime, endTime)
            }
            activeRequirements = emptyList()
            currentRequirementIndex = 0
            // Navigation handled by UI observing activeRequirements empty
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        val missing = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            setupStepSensor()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data whenever the user returns to the app
        refreshStepsFromHealth()
        
        // Ensure Daily Log Service is running
        startService(Intent(this, com.example.fitlock.service.DailyLogService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::exerciseManager.isInitialized) {
            exerciseManager.shutdown()
        }
    }

    /**
     * Checks if we have permissions to read/write from Google Health Connect.
     */
    private fun checkHealthPermissions() {
        lifecycleScope.launch {
            if (!healthConnectManager.hasPermissions()) {
                requestHealthPermissionLauncher.launch(healthConnectManager.requiredPermissions.toTypedArray())
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
            healthBaseSteps = steps
            _stepCount.intValue = steps.toInt()
            initialStepCount = -1f // Reset to pick up new session delta
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
            val totalStepsSinceBoot = event.values[0]
            if (initialStepCount == -1f) {
                initialStepCount = totalStepsSinceBoot
            }
            val delta = (totalStepsSinceBoot - initialStepCount).toLong()
            _stepCount.intValue = (healthBaseSteps + delta).toInt()
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
            label = { 
                Text(
                    text = label,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    overflow = TextOverflow.Visible,
                    softWrap = false
                ) 
            },
            selected = selected,
            onClick = onClick,
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }

    private fun broadcastEvent(action: String, extras: Map<String, Any>) {
        val intent = Intent(action).apply {
            extras.forEach { (key, value) ->
                when (value) {
                    is Int -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                    is String -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                }
            }
        }
        sendBroadcast(intent)
    }
}
