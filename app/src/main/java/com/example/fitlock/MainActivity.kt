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
                var trackingMode by remember { mutableStateOf(TrackingMode.CAMERA) }
                var editingGroup by remember { mutableStateOf<AppGroup?>(null) }
                var calibrationExercise by remember { mutableStateOf<ExerciseType?>(null) }
                var unlockingVaultItem by remember { mutableStateOf<VaultItem?>(null) }
                var editingGauntlet by remember { mutableStateOf<GauntletWithHabits?>(null) }
                var activeRequirements by remember { mutableStateOf<List<ExerciseRequirement>>(emptyList()) }
                var currentRequirementIndex by remember { mutableIntStateOf(0) }
                var showUrgeNegotiation by remember { mutableStateOf(false) }
                var showPostNoteDialog by remember { mutableStateOf(intent.getStringExtra("navigate_to") == "post_note") }
                var generatedLogText by remember { mutableStateOf<String?>(null) }
                var celebrationData by remember { mutableStateOf<Pair<String, String>?>(null) }
                
                LaunchedEffect(userStats?.level) {
                    if (userStats != null && userStats!!.level > 1) {
                        celebrationData = "Level Up!" to "You reached Level ${userStats!!.level}"
                    }
                }

                LaunchedEffect(intent) {
                    if (intent.getStringExtra("navigate_to") == "post_note") {
                        showPostNoteDialog = true
                    }
                    if (intent.getBooleanExtra("START_MINI_WORKOUT", false)) {
                        val exType = intent.getStringExtra("EXERCISE_TYPE") ?: ExerciseType.SQUAT.name
                        val exCount = intent.getIntExtra("EXERCISE_COUNT", 20)
                        activeRequirements = listOf(ExerciseRequirement(exType, exCount))
                        currentRequirementIndex = 0
                        unlockingVaultItem = null
                        currentScreen = "track"
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
                                NavigationItem(themeData.tabLocks, Icons.Default.Lock, currentScreen == "locks") { currentScreen = "locks" }
                                
                                // PROMINENT CENTRAL BUTTON
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { showUrgeNegotiation = true },
                                    icon = { 
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.error, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    label = { 
                                        Text(
                                            text = themeData.tabUrge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1,
                                            softWrap = false
                                        ) 
                                    },
                                    alwaysShowLabel = true
                                )

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
                                HomeHub(
                                    userStats = userStats,
                                    challenges = challenges,
                                    todayTotals = todayTotals,
                                    currentPledge = currentPledge,
                                    deckSummaries = deckSummaries,
                                    onChallengeClick = { challenge ->
                                        if (challenge.requirements.isNotEmpty()) {
                                            activeRequirements = challenge.requirements
                                            currentRequirementIndex = 0
                                            unlockingVaultItem = null
                                            currentScreen = "track"
                                        }
                                    },
                                    onExerciseClick = { type, goal ->
                                        activeRequirements = listOf(ExerciseRequirement(type.name, goal))
                                        currentRequirementIndex = 0
                                        unlockingVaultItem = null
                                        currentScreen = "track"
                                    },
                                    onSettingsClick = { currentScreen = "settings" },
                                    onUrgeClick = {
                                        showUrgeNegotiation = true
                                    },
                                    onPledgeClick = { currentScreen = "pledge" },
                                    onViewLogClick = {
                                        lifecycleScope.launch {
                                            generatedLogText = com.example.fitlock.utils.MarkdownExporter(this@MainActivity).generateDailyLog(java.util.Date())
                                        }
                                    },
                                    onImportAnkiClick = {
                                        importAnkiLauncher.launch("*/*")
                                    },
                                    onDeckClick = { deckName ->
                                        selectedDeckName = deckName
                                        currentScreen = "flashcard_review"
                                    },
                                    onDeleteDeck = { deckName ->
                                        homeViewModel.deleteDeck(deckName)
                                    }
                                )
                            }
                            "pledge" -> {
                                PledgeScreen(
                                    currentPledge = currentPledge,
                                    onCommit = {
                                        lifecycleScope.launch {
                                            db.dao().upsertPledge(currentPledge!!.copy(status = "COMMITTED", pledgeTimestamp = System.currentTimeMillis()))
                                        }
                                    },
                                    onSuccess = {
                                        lifecycleScope.launch {
                                            db.dao().upsertPledge(currentPledge!!.copy(status = "SUCCESS", reviewTimestamp = System.currentTimeMillis()))
                                            val stats = userStats ?: UserStats()
                                            db.dao().updateUserStats(stats.copy(
                                                willpowerXp = stats.willpowerXp + 100,
                                                sobrietyStreak = stats.sobrietyStreak + 1,
                                                longestSobrietyStreak = maxOf(stats.longestSobrietyStreak, stats.sobrietyStreak + 1)
                                            ))
                                        }
                                    },
                                    onRelapse = {
                                        lifecycleScope.launch {
                                            db.dao().upsertPledge(currentPledge!!.copy(status = "RELAPSED", reviewTimestamp = System.currentTimeMillis()))
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
                                    }
                                )
                            }
                            "analytics" -> {
                                AnalyticsScreen(history = history)
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
                                    }
                                )
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
                                    onNavigateToQuotes = { currentScreen = "quotes" }
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
                                CreateGroupScreen(
                                    editingGroup = editingGroup,
                                    onSave = { group ->
                                        locksViewModel.upsertGroup(group)
                                        currentScreen = "locks"
                                    },
                                    onBack = { currentScreen = "locks" }
                                )
                            }
                            "gauntlet_editor" -> {
                                GauntletEditor(
                                    editingGauntlet = editingGauntlet,
                                    groups = groups,
                                    onSave = { gauntlet, habits ->
                                        lifecycleScope.launch {
                                            val gId = locksViewModel.upsertGauntlet(gauntlet).await().toInt()
                                            locksViewModel.deleteHabitsForGauntlet(gId)
                                            habits.forEach { locksViewModel.upsertHabit(it.copy(gauntletId = gId)) }
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
                                        currentHabitIndex = session.currentHabitIndex,
                                        elapsedSeconds = session.elapsedSeconds,
                                        isInBuffer = session.isInBuffer,
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
                                selectedDeckName?.let { deckName ->
                                    FlashcardReviewScreen(
                                        deckName = deckName,
                                        repository = repository,
                                        onFinish = {
                                            currentScreen = "home"
                                            selectedDeckName = null
                                        }
                                    )
                                }
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
                                            exerciseManager.startTracking(exerciseType, trackingMode, currentGoal, userStats?.calibrations?.get("${exerciseType.name}_${trackingMode.name}"))
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
                            themeUrgeLabel = themeData.tabUrge,
                            onConfirm = { event ->
                                lifecycleScope.launch {
                                    db.dao().insertUrgeEvent(event)
                                    showUrgeNegotiation = false
                                    
                                    // Also trigger the physical challenge as per plan
                                    activeRequirements = listOf(
                                        ExerciseRequirement(ExerciseType.PUSHUP.name, 15),
                                        ExerciseRequirement(ExerciseType.SQUAT.name, 20)
                                    )
                                    currentRequirementIndex = 0
                                    unlockingVaultItem = null
                                    currentScreen = "track"
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

    private fun initializeExerciseManager(onRepCount: (Int) -> Unit, onStationaryStatusChanged: (Boolean) -> Unit = {}) {
        if (::exerciseManager.isInitialized) exerciseManager.shutdown()
        exerciseManager = ExerciseTrackerManager(
            context = this,
            onRepCountChanged = { count ->
                onRepCount(count)
            },
            onWorkoutComplete = { reps -> },
            onStationaryStatusChanged = onStationaryStatusChanged
        )
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
}
