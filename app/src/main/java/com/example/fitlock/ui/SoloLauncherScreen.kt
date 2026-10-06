package com.example.fitlock.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fitlock.LockOverlayActivity
import com.example.fitlock.data.*
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.utils.AppInfoFetcher
import com.example.fitlock.utils.VitalityMetrics
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoloLauncherScreen(
    userStats: UserStats?,
    quests: List<QuestWithBlocks>,
    appGroups: List<AppGroup>,
    challenges: List<Challenge>,
    currentPledge: DailyPledge?,
    vitalityMetrics: VitalityMetrics = VitalityMetrics(),
    unfinishedTasks: List<QuestBlock> = emptyList(),
    completedTasks: List<QuestBlock> = emptyList(),
    unfinishedProjectTasks: List<ProjectTask> = emptyList(),
    countdowns: List<DateCountdown> = emptyList(),
    deckSummaries: List<DeckSummary> = emptyList(),
    quotes: List<MotivationalQuote> = emptyList(),
    history: List<WorkoutHistory> = emptyList(),
    pledges: List<DailyPledge> = emptyList(),
    onAddWater: (Double) -> Unit = {},
    onAddCountdown: (DateCountdown) -> Unit = {},
    onDeleteCountdown: (DateCountdown) -> Unit = {},
    onAddQuote: (MotivationalQuote) -> Unit = {},
    onUpdateQuote: (MotivationalQuote) -> Unit = {},
    onDeleteQuote: (MotivationalQuote) -> Unit = {},
    onAddTask: (String, Int, String) -> Unit = { _, _, _ -> },
    onToggleTask: (QuestBlock) -> Unit = {},
    onStartTaskTracking: (QuestBlock) -> Unit = {},
    onStopTaskTracking: (QuestBlock) -> Unit = {},
    onDeleteTask: (QuestBlock) -> Unit = {},
    onUpdateTask: (QuestBlock) -> Unit = {},
    onDeleteProjectTask: (ProjectTask) -> Unit = {},
    onDeckPlay: (String) -> Unit = {},
    onQuickExerciseClick: (ExerciseType, Int) -> Unit = { _, _ -> },
    onQuickFlashcardClick: () -> Unit = {},
    onUrgeClick: () -> Unit = {},
    onPledgeClick: () -> Unit = {},
    onViewLogClick: () -> Unit = {},
    onStartPlanning: () -> Unit = {},
    onNavigateToLocks: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onQuestClick: (QuestWithBlocks) -> Unit,
    onChallengeClick: (Challenge) -> Unit,
    onSettingsClick: () -> Unit,
    onForgeClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    val initialPage = remember { prefs.getInt("launcher_default_page", 0) }
    val isWinterArc = remember { prefs.getBoolean("winter_arc_enabled", false) }

    // 8 Swipable Launcher Pages
    val pagerState = rememberPagerState(initialPage = initialPage) { 8 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Winter Arc Frosted Snow Effect Overlay
        if (isWinterArc) {
            FrostedSnowOverlay()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Minimal Page Indicator Dots Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 8) {
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.5.dp)
                            .size(if (isSelected) 8.dp else 4.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }

            // Swipable Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> SoloMainHabitsTab(
                        userStats = userStats,
                        quests = quests,
                        history = history,
                        vitalityMetrics = vitalityMetrics,
                        isWinterArc = isWinterArc,
                        onQuickExerciseClick = onQuickExerciseClick,
                        onQuickFlashcardClick = onQuickFlashcardClick,
                        onQuestClick = onQuestClick,
                        onSettingsClick = onSettingsClick,
                        onForgeClick = onForgeClick
                    )

                    1 -> SoloPortalsTab(
                        appGroups = appGroups,
                        onNavigateToLocks = onNavigateToLocks,
                        onNavigateToVault = onNavigateToVault
                    )

                    2 -> SoloWeeklyMatrixTab(
                        history = history,
                        pledges = pledges
                    )

                    3 -> SoloStatsVitalityTab(
                        userStats = userStats,
                        vitalityMetrics = vitalityMetrics,
                        onAddWater = onAddWater,
                        onNavigateToAnalytics = onNavigateToAnalytics
                    )

                    4 -> SoloCalendarTasksTab(
                        unfinishedTasks = unfinishedTasks,
                        completedTasks = completedTasks,
                        unfinishedProjectTasks = unfinishedProjectTasks,
                        countdowns = countdowns,
                        onAddCountdown = onAddCountdown,
                        onDeleteCountdown = onDeleteCountdown,
                        onAddTask = onAddTask,
                        onToggleTask = onToggleTask,
                        onStartTaskTracking = onStartTaskTracking,
                        onStopTaskTracking = onStopTaskTracking,
                        onDeleteTask = onDeleteTask,
                        onUpdateTask = onUpdateTask,
                        onDeleteProjectTask = onDeleteProjectTask
                    )

                    5 -> SoloMentalVaultTab(
                        deckSummaries = deckSummaries,
                        quotes = quotes,
                        onDeckPlay = onDeckPlay,
                        onAddQuote = onAddQuote,
                        onUpdateQuote = onUpdateQuote,
                        onDeleteQuote = onDeleteQuote
                    )

                    6 -> SoloWillpowerShrineTab(
                        userStats = userStats,
                        currentPledge = currentPledge,
                        onUrgeClick = onUrgeClick,
                        onPledgeClick = onPledgeClick,
                        onViewLogClick = onViewLogClick,
                        onStartPlanning = onStartPlanning
                    )

                    7 -> SoloChallengesTab(
                        challenges = challenges,
                        history = history,
                        onChallengeClick = onChallengeClick
                    )
                }
            }
        }
    }
}

@Composable
fun FrostedSnowOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = Random(101)
        for (i in 0 until 50) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val radius = random.nextFloat() * 2.5.dp.toPx() + 1.dp.toPx()
            val alpha = random.nextFloat() * 0.45f + 0.15f
            drawCircle(
                color = Color(0xFFE0F7FA).copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// PAGE 0: DEFAULT MAIN TAB - DAILY QUEST SCREEN & TARGET GOALS
// ---------------------------------------------------------------------------
@Composable
fun SoloMainHabitsTab(
    userStats: UserStats?,
    quests: List<QuestWithBlocks>,
    history: List<WorkoutHistory>,
    vitalityMetrics: VitalityMetrics,
    isWinterArc: Boolean,
    onQuickExerciseClick: (ExerciseType, Int) -> Unit,
    onQuickFlashcardClick: () -> Unit,
    onQuestClick: (QuestWithBlocks) -> Unit,
    onSettingsClick: () -> Unit,
    onForgeClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }

    // Today's completed sums from history
    val todayPushups = remember(history) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        history.filter { it.timestamp >= cal.timeInMillis && it.exerciseType.equals("PUSHUP", ignoreCase = true) }.sumOf { it.repsCompleted }
    }
    val todaySquats = remember(history) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        history.filter { it.timestamp >= cal.timeInMillis && it.exerciseType.equals("SQUAT", ignoreCase = true) }.sumOf { it.repsCompleted }
    }
    val todayPlanks = remember(history) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        history.filter { it.timestamp >= cal.timeInMillis && it.exerciseType.equals("PLANK", ignoreCase = true) }.sumOf { it.repsCompleted }
    }
    val todaySitups = remember(history) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        history.filter { it.timestamp >= cal.timeInMillis && it.exerciseType.equals("SITUP", ignoreCase = true) }.sumOf { it.repsCompleted }
    }
    val todayFlashcards = remember(history) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        history.filter { it.timestamp >= cal.timeInMillis && (it.exerciseType.equals("FLASHCARDS", ignoreCase = true) || it.exerciseType.contains("ANKI", ignoreCase = true)) }.sumOf { it.repsCompleted }
    }

    // Targets from preferences
    val targetPushups = prefs.getInt("goal_PUSHUP", 15)
    val targetSquats = prefs.getInt("goal_SQUAT", 20)
    val targetPlanks = prefs.getInt("goal_PLANK", 60)
    val targetSitups = prefs.getInt("goal_SITUP", 20)
    val targetFlashcards = 10
    val targetWater = 3.0 // Liters
    val targetSteps = 10000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // System Ticker Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isWinterArc) "❄️ [WINTER ARC PROTOCOL ACTIVE]" else "[SYSTEM DIRECTIVE: DAILY QUEST]",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Player ${userStats?.name ?: "Seeker"} • Lv. ${userStats?.level ?: 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, null, tint = Color.Gray)
                }
            }
        }

        if (isWinterArc) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF102838),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AcUnit, null, tint = Color(0xFF81D4FA), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("❄️ Cold Bed Breaker: 3 Pushups required to conquer morning inertia!", color = Color(0xFF81D4FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Daily Target Goals Progress Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("TODAY'S AIM & TARGET GOALS", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                DailyGoalProgressRow("💪 Pushups", todayPushups, targetPushups, "reps") {
                    onQuickExerciseClick(ExerciseType.PUSHUP, targetPushups)
                }
                DailyGoalProgressRow("🦵 Squats", todaySquats, targetSquats, "reps") {
                    onQuickExerciseClick(ExerciseType.SQUAT, targetSquats)
                }
                DailyGoalProgressRow("🛹 Planks", todayPlanks, targetPlanks, "secs") {
                    onQuickExerciseClick(ExerciseType.PLANK, targetPlanks)
                }
                DailyGoalProgressRow("🧘 Situps", todaySitups, targetSitups, "reps") {
                    onQuickExerciseClick(ExerciseType.SITUP, targetSitups)
                }
                DailyGoalProgressRow("🧠 Anki Cards", todayFlashcards, targetFlashcards, "cards") {
                    onQuickFlashcardClick()
                }
                DailyGoalProgressRow("🚣 Rowing", vitalityMetrics.rowingMinutes.toInt(), 20, "mins")
                DailyGoalProgressRow("💧 Water Drink", vitalityMetrics.waterLiters.toInt(), targetWater.toInt(), "L")
                DailyGoalProgressRow("👟 Steps", vitalityMetrics.steps.toInt(), targetSteps.toInt(), "steps")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("QUICK TRAINING PORTALS (DAILY EXERCISE LINKS)", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickTrainingCard("Pushups", Icons.Default.FitnessCenter, Color(0xFFEF5350), modifier = Modifier.weight(1f)) {
                onQuickExerciseClick(ExerciseType.PUSHUP, targetPushups)
            }
            QuickTrainingCard("Squats", Icons.Default.DirectionsRun, Color(0xFF64B5F6), modifier = Modifier.weight(1f)) {
                onQuickExerciseClick(ExerciseType.SQUAT, targetSquats)
            }
            QuickTrainingCard("Plank", Icons.Default.Timer, Color(0xFFFFB74D), modifier = Modifier.weight(1f)) {
                onQuickExerciseClick(ExerciseType.PLANK, targetPlanks)
            }
            QuickTrainingCard("Anki", Icons.Default.Style, Color(0xFF81C784), modifier = Modifier.weight(1f)) {
                onQuickFlashcardClick()
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("TODAY'S DAILY QUESTS & HABITS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        val todayStr = LocalDate.now().toString()
        val activeQuests = quests.filter { q ->
            !q.quest.isCompletedToday && (q.quest.id == "daily_$todayStr" || q.quest.name.contains(todayStr))
        }

        if (activeQuests.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("All Daily Quests Cleared for Today. Gatekeeper defenses secure.", color = Color(0xFF81C784), modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
        } else {
            activeQuests.forEach { q ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onQuestClick(q) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(q.quest.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val completed = q.blocks.count { it.isCompleted }
                            Text("$completed / ${q.blocks.size} blocks completed", color = Color.Gray, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onForgeClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C21)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open The Forge (Training Library)", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun DailyGoalProgressRow(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    onClick: (() -> Unit)? = null
) {
    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val isMet = current >= target

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("$current / $target $unit", color = if (isMet) Color(0xFF81C784) else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = if (isMet) Color(0xFF81C784) else MaterialTheme.colorScheme.primary,
            trackColor = Color(0xFF2C2C35)
        )
    }
}

@Composable
fun QuickTrainingCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// PAGE 7: TIME-GATED DAILY CHALLENGES TAB
// ---------------------------------------------------------------------------
@Composable
fun SoloChallengesTab(
    challenges: List<Challenge>,
    history: List<WorkoutHistory>,
    onChallengeClick: (Challenge) -> Unit
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val context = LocalContext.current

    val todayHistory = remember(history) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        history.filter { it.timestamp >= cal.timeInMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("TIME-GATED DAILY CHALLENGES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Time Window Banner Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Current System Time: $currentHour:00 Hours", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Challenges complete automatically as you perform exercises throughout the day.", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("ELITE DAILY RAIDS & CHALLENGES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        if (challenges.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No elite challenges active.", color = Color(0xFF81C784), modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
        } else {
            challenges.forEach { challenge ->
                val isMorningChallenge = challenge.title.contains("Morning", ignoreCase = true)
                val isEveningChallenge = challenge.title.contains("Evening", ignoreCase = true) || challenge.title.contains("Night", ignoreCase = true)
                
                val isTimeAllowed = when {
                    isMorningChallenge -> currentHour in 4..12
                    isEveningChallenge -> currentHour in 18..23
                    else -> true
                }

                val isCompleted = challenge.isCompletedToday || challenge.requirements.all { req ->
                    val done = todayHistory
                        .filter { 
                            it.exerciseType.equals(req.type, ignoreCase = true) || 
                            (req.type.equals("FLASHCARDS", ignoreCase = true) && (it.exerciseType.contains("ANKI", ignoreCase = true) || it.exerciseType.equals("FLASHCARDS", ignoreCase = true))) ||
                            (req.type.equals("FRENCH_STUDY", ignoreCase = true) && (it.exerciseType.contains("FRENCH", ignoreCase = true) || it.exerciseType.contains("DUOLINGO", ignoreCase = true)))
                        }
                        .sumOf { it.repsCompleted }
                    done >= req.count
                }

                val timeWindowText = when {
                    isMorningChallenge -> "Time Window: Morning (04:00 - 12:00)"
                    isEveningChallenge -> "Time Window: Evening (18:00 - 23:59)"
                    else -> "Time Window: Anytime"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (isCompleted) {
                                Toast.makeText(context, "Challenge already cleared for today!", Toast.LENGTH_SHORT).show()
                            } else if (isTimeAllowed) {
                                onChallengeClick(challenge)
                            } else {
                                Toast.makeText(
                                    context,
                                    "[SYSTEM TIME LOCK]: '${challenge.title}' is locked outside its designated time window ($timeWindowText).",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCompleted -> Color(0xFF14221A)
                            isTimeAllowed -> MaterialTheme.colorScheme.surface
                            else -> Color(0xFF1C1215)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, when {
                        isCompleted -> Color(0xFF81C784).copy(alpha = 0.5f)
                        isTimeAllowed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else -> Color(0xFFEF5350).copy(alpha = 0.4f)
                    })
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(challenge.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                color = (when {
                                    isCompleted -> Color(0xFF81C784)
                                    isTimeAllowed -> Color(0xFF64B5F6)
                                    else -> Color(0xFFEF5350)
                                }).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isCompleted -> "CLEARED ✓"
                                        isTimeAllowed -> "UNLOCKED"
                                        else -> "TIME-LOCKED"
                                    },
                                    color = when {
                                        isCompleted -> Color(0xFF81C784)
                                        isTimeAllowed -> Color(0xFF64B5F6)
                                        else -> Color(0xFFEF5350)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(challenge.description, color = Color.Gray, fontSize = 11.sp)
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        // Requirements breakdown
                        challenge.requirements.forEach { req ->
                            val done = todayHistory
                                .filter { 
                                    it.exerciseType.equals(req.type, ignoreCase = true) || 
                                    (req.type.equals("FLASHCARDS", ignoreCase = true) && (it.exerciseType.contains("ANKI", ignoreCase = true) || it.exerciseType.equals("FLASHCARDS", ignoreCase = true))) ||
                                    (req.type.equals("FRENCH_STUDY", ignoreCase = true) && (it.exerciseType.contains("FRENCH", ignoreCase = true) || it.exerciseType.contains("DUOLINGO", ignoreCase = true)))
                                }
                                .sumOf { it.repsCompleted }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${req.type}:", color = Color.Gray, fontSize = 10.sp)
                                Text("$done / ${req.count}", color = if (done >= req.count) Color(0xFF81C784) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(timeWindowText, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                            Text("+${challenge.xpReward} XP", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PAGE 1: APP PORTALS / DUNGEON GATES
// ---------------------------------------------------------------------------
@Composable
fun SoloPortalsTab(
    appGroups: List<AppGroup>,
    onNavigateToLocks: () -> Unit = {},
    onNavigateToVault: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForAction by remember { mutableStateOf<AppInfo?>(null) }
    var hiddenApps by remember { 
        mutableStateOf(prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()) 
    }

    val allApps = remember { AppInfoFetcher.getInstalledApps(context) }
    val filteredApps = allApps
        .filter { !hiddenApps.contains(it.packageName) }
        .filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.packageName.contains(searchQuery, ignoreCase = true) 
        }

    val sortedApps = filteredApps.sortedWith(compareBy({ app ->
        val group = appGroups.find { g -> g.isEnabled && g.packageNames.contains(app.packageName) }
        when {
            group?.isProductive == true -> 0
            group == null -> 1
            else -> 2
        }
    }, { it.name }))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Text("DUNGEON GATES (APP PORTALS)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search portals...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onNavigateToLocks,
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Lock, contentDescription = "App Blocks", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onNavigateToVault,
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Key, contentDescription = "Secret Vault", tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sortedApps) { app ->
                val matchingGroup = appGroups.find { group ->
                    group.isEnabled && group.packageNames.contains(app.packageName)
                }
                val isProductive = matchingGroup?.isProductive == true
                val isLocked = matchingGroup?.let { group ->
                    if (group.isProductive) false else {
                        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                        val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                        (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
                    }
                } ?: false

                DungeonGateCard(
                    app = app,
                    matchingGroup = matchingGroup,
                    isLocked = isLocked,
                    isProductive = isProductive,
                    onClick = {
                        if (isLocked && matchingGroup != null) {
                            val intent = Intent(context, LockOverlayActivity::class.java).apply {
                                putExtra("group_id", matchingGroup.id)
                                putExtra("target_app", app.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } else {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            }
                        }
                    },
                    onLongClick = {
                        selectedAppForAction = app
                    }
                )
            }
        }
    }

    selectedAppForAction?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForAction = null },
            title = { Text("Manage Portal: ${app.name}") },
            text = { Text("Choose an action for this app portal:") },
            confirmButton = {
                Column {
                    Button(
                        onClick = {
                            onNavigateToLocks()
                            selectedAppForAction = null
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create App Block")
                    }
                    Button(
                        onClick = {
                            val newSet = hiddenApps.toMutableSet()
                            newSet.add(app.packageName)
                            hiddenApps = newSet
                            prefs.edit().putStringSet("hidden_apps", newSet).apply()
                            selectedAppForAction = null
                            Toast.makeText(context, "${app.name} hidden from launcher portals", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C35)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hide App from Launcher")
                    }
                    Button(
                        onClick = {
                            val uninstallIntent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                            context.startActivity(uninstallIntent)
                            selectedAppForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1C1C)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uninstall App", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DungeonGateCard(
    app: AppInfo,
    matchingGroup: AppGroup?,
    isLocked: Boolean,
    isProductive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val rankColor = when {
        isProductive -> Color(0xFF81C784)
        isLocked -> Color(0xFFEF5350)
        else -> Color(0xFF64B5F6)
    }
    val rankLetter = when {
        isProductive -> "P"
        isLocked -> "S"
        else -> "E"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isProductive -> Color(0xFF14221A)
                isLocked -> Color(0xFF1C1215)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, rankColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                when {
                                    isProductive -> Color(0xFF1A3522)
                                    isLocked -> Color(0xFF3B1D22)
                                    else -> Color(0xFF1E2235)
                                },
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    try {
                                        setImageDrawable(ctx.packageManager.getApplicationIcon(app.packageName))
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isProductive && matchingGroup != null) {
                        Text(
                            text = "+${matchingGroup.rewardStat} XP / min",
                            color = Color(0xFF81C784),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isLocked && matchingGroup != null) {
                        val reqsText = matchingGroup.exercises.joinToString(", ") { "${it.count} ${it.type.lowercase()}" }
                        Text(
                            text = "Reqs: $reqsText",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Instant Access",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Surface(
                color = rankColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = rankLetter,
                    color = rankColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PAGE 2: WEEKLY HABIT CONSISTENCY MATRIX
// ---------------------------------------------------------------------------
@Composable
fun SoloWeeklyMatrixTab(
    history: List<WorkoutHistory>,
    pledges: List<DailyPledge>
) {
    val last7Days = remember(history, pledges) {
        (0..6).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTime = cal.timeInMillis
            val endTime = startTime + 24 * 60 * 60 * 1000L
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

            val dayHistory = history.filter { it.timestamp in startTime until endTime }
            val dayPledge = pledges.find { it.date == dateStr }

            val pushups = dayHistory.filter { it.exerciseType.equals("PUSHUP", ignoreCase = true) }.sumOf { it.repsCompleted }
            val squats = dayHistory.filter { it.exerciseType.equals("SQUAT", ignoreCase = true) }.sumOf { it.repsCompleted }
            val flashcards = dayHistory.filter { it.exerciseType.equals("FLASHCARDS", ignoreCase = true) || it.exerciseType.contains("ANKI", ignoreCase = true) }.sumOf { it.repsCompleted }
            val rowingMinutes = dayHistory.filter { it.exerciseType.equals("ROWING", ignoreCase = true) || it.exerciseType.contains("ERG", ignoreCase = true) }.sumOf { it.repsCompleted }
            
            val morningDone = dayPledge?.pledgeTimestamp != null || dayPledge?.status == "COMMITTED" || dayPledge?.status == "SUCCESS"
            val eveningDone = dayPledge?.reviewTimestamp != null || dayPledge?.eveningReflection?.isNotBlank() == true || dayPledge?.status == "SUCCESS"

            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "M"
                Calendar.TUESDAY -> "T"
                Calendar.WEDNESDAY -> "W"
                Calendar.THURSDAY -> "T"
                Calendar.FRIDAY -> "F"
                Calendar.SATURDAY -> "S"
                Calendar.SUNDAY -> "S"
                else -> ""
            }

            WeeklyDayStats(dayName, pushups, squats, flashcards, rowingMinutes, morningDone, eveningDone)
        }.reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("WEEKLY HABIT CONSISTENCY MATRIX", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("HABIT TARGET", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                    last7Days.forEach { day ->
                        Text(day.dayName, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C35), modifier = Modifier.padding(vertical = 8.dp))

                WeeklyMatrixRow("💪 Pushups (15)", last7Days.map { Pair(it.pushups, if (it.pushups >= 15) MatrixCellState.GOLD else if (it.pushups > 0) MatrixCellState.GREEN else MatrixCellState.MISSED) })
                WeeklyMatrixRow("🦵 Squats (20)", last7Days.map { Pair(it.squats, if (it.squats >= 20) MatrixCellState.GOLD else if (it.squats > 0) MatrixCellState.GREEN else MatrixCellState.MISSED) })
                WeeklyMatrixRow("🧠 Anki Cards", last7Days.map { Pair(it.flashcards, if (it.flashcards >= 10) MatrixCellState.GOLD else if (it.flashcards > 0) MatrixCellState.GREEN else MatrixCellState.MISSED) })
                WeeklyMatrixRow("🚣 Rowing (20m)", last7Days.map { Pair(it.rowingMinutes, if (it.rowingMinutes >= 20) MatrixCellState.GOLD else if (it.rowingMinutes >= 5) MatrixCellState.GREEN else MatrixCellState.MISSED) })
                WeeklyMatrixRow("☀️ Morn. Journal", last7Days.map { Pair(if (it.morningDone) 1 else 0, if (it.morningDone) MatrixCellState.GREEN else MatrixCellState.MISSED) })
                WeeklyMatrixRow("🌙 Eve. Review", last7Days.map { Pair(if (it.eveningDone) 1 else 0, if (it.eveningDone) MatrixCellState.GREEN else MatrixCellState.MISSED) })
            }
        }
    }
}

data class WeeklyDayStats(
    val dayName: String,
    val pushups: Int,
    val squats: Int,
    val flashcards: Int,
    val rowingMinutes: Int,
    val morningDone: Boolean,
    val eveningDone: Boolean
)

enum class MatrixCellState { GOLD, GREEN, MISSED }

@Composable
fun WeeklyMatrixRow(label: String, counts: List<Pair<Int, MatrixCellState>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2.2f))
        counts.forEach { (count, state) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                val textColor = when (state) {
                    MatrixCellState.GOLD -> Color(0xFFFFD700)
                    MatrixCellState.GREEN -> Color(0xFF81C784)
                    MatrixCellState.MISSED -> Color(0xFF4A4A58)
                }
                val textStr = if (count > 0) count.toString() else "-"

                Text(
                    text = textStr,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = if (state != MatrixCellState.MISSED) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PAGE 3: PLAYER STATS & HEALTH CONNECT VITALITY
// ---------------------------------------------------------------------------
@Composable
fun SoloStatsVitalityTab(
    userStats: UserStats?,
    vitalityMetrics: VitalityMetrics,
    onAddWater: (Double) -> Unit,
    onNavigateToAnalytics: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("PLAYER STATUS WINDOW & VITALITY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        StatsHeader(userStats)
        Spacer(modifier = Modifier.height(14.dp))

        VitalityHubCard(metrics = vitalityMetrics, onAddWater = onAddWater)
        Spacer(modifier = Modifier.height(14.dp))

        StatRadarCard(userStats)

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onNavigateToAnalytics,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C21)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Full Workout History & Analytics", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun VitalityHubCard(
    metrics: VitalityMetrics,
    onAddWater: (Double) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("VITALITY & RECOVERY (HEALTH CONNECT)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                TextButton(onClick = { onAddWater(0.25) }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+0.25L", fontSize = 10.sp, color = Color(0xFF64B5F6))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VitalityMetricItem("Sleep", String.format(Locale.US, "%.1f hrs", metrics.sleepHours), Icons.Default.Bedtime, Color(0xFF9FA8DA))
                VitalityMetricItem("Water", String.format(Locale.US, "%.1f L", metrics.waterLiters), Icons.Default.WaterDrop, Color(0xFF64B5F6))
                VitalityMetricItem("Food", String.format(Locale.US, "%.0f kcal", metrics.foodCalories), Icons.Default.Restaurant, Color(0xFFFFB74D))
                VitalityMetricItem("Steps", "${metrics.steps}", Icons.AutoMirrored.Filled.DirectionsWalk, Color(0xFF81C784))
            }
        }
    }
}

@Composable
fun VitalityMetricItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(label, color = Color.Gray, fontSize = 9.sp)
    }
}

// ---------------------------------------------------------------------------
// PAGE 4: DAILY CALENDAR, COUNTDOWNS & TO-DO CHECKLIST
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoloCalendarTasksTab(
    unfinishedTasks: List<QuestBlock>,
    completedTasks: List<QuestBlock> = emptyList(),
    unfinishedProjectTasks: List<ProjectTask> = emptyList(),
    countdowns: List<DateCountdown> = emptyList(),
    onAddCountdown: (DateCountdown) -> Unit = {},
    onDeleteCountdown: (DateCountdown) -> Unit = {},
    onAddTask: (String, Int, String) -> Unit = { _, _, _ -> },
    onToggleTask: (QuestBlock) -> Unit = {},
    onStartTaskTracking: (QuestBlock) -> Unit = {},
    onStopTaskTracking: (QuestBlock) -> Unit = {},
    onDeleteTask: (QuestBlock) -> Unit = {},
    onUpdateTask: (QuestBlock) -> Unit = {},
    onDeleteProjectTask: (ProjectTask) -> Unit = {}
) {
    val today = remember { LocalDate.now() }
    val formattedDate = remember(today) { today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")) }
    var showAddCountdownDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showBrainstormDialog by remember { mutableStateOf(false) }
    var selectedTaskForAction by remember { mutableStateOf<QuestBlock?>(null) }
    var taskToEdit by remember { mutableStateOf<QuestBlock?>(null) }

    val activeTrackingTask = unfinishedTasks.find { it.isCurrentlyTracking }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("DAILY CALENDAR & TASKS AGENDA", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Date Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Today, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(formattedDate, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Daily Agenda & System To-Do Items", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }

        // Active Task Tracking Card (If currently tracking)
        activeTrackingTask?.let { task ->
            Spacer(modifier = Modifier.height(12.dp))
            val startMs = task.trackingStartTimestamp ?: System.currentTimeMillis()
            var elapsedSecs by remember { mutableStateOf((System.currentTimeMillis() - startMs) / 1000) }

            LaunchedEffect(startMs) {
                while (true) {
                    delay(1000)
                    elapsedSecs = (System.currentTimeMillis() - startMs) / 1000
                }
            }

            val predictedMins = (task.estimatedDurationSeconds ?: 600) / 60
            val elapsedMins = elapsedSecs / 60
            val elapsedRemSecs = elapsedSecs % 60

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A1E)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TASK IN PROGRESS", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text(
                            String.format(Locale.US, "%02d:%02d / %dm", elapsedMins, elapsedRemSecs, predictedMins),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(task.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onStopTaskTracking(task) }) {
                            Text("Stop", color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onToggleTask(task) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5938))
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Complete", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Countdowns / Countups Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("COUNTDOWNS & MILESTONES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showAddCountdownDialog = true }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Add Event", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (countdowns.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No countdowns set. Add New Year, Birthdays, or Milestones above!", color = Color.Gray, modifier = Modifier.padding(12.dp), fontSize = 11.sp)
            }
        } else {
            countdowns.forEach { cd ->
                val daysDiff = remember(cd.targetDate) {
                    try {
                        val target = LocalDate.parse(cd.targetDate)
                        ChronoUnit.DAYS.between(today, target)
                    } catch (_: Exception) {
                        0L
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Event, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(cd.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Target: ${cd.targetDate}", color = Color.Gray, fontSize = 10.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (daysDiff >= 0) "$daysDiff days left" else "${Math.abs(daysDiff)} days ago",
                                color = if (daysDiff >= 0) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            IconButton(onClick = { onDeleteCountdown(cd) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SYSTEM TO-DO CHECKLIST", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = { showBrainstormDialog = true }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Backlog", fontSize = 10.sp, color = Color(0xFFFFB74D))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showAddTaskDialog = true }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Add Task", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (unfinishedTasks.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No pending tasks for today. Add new tasks above!", color = Color(0xFF81C784), modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
        } else {
            unfinishedTasks.forEach { task ->
                val priorityColor = when (task.priority.uppercase()) {
                    "HIGH" -> Color(0xFFEF5350)
                    "LOW" -> Color(0xFF64B5F6)
                    else -> Color(0xFFFFB74D)
                }
                val estMins = (task.estimatedDurationSeconds ?: 600) / 60

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .combinedClickable(
                            onClick = { onToggleTask(task) },
                            onLongClick = { selectedTaskForAction = task }
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "[${task.priority}]",
                                    color = priorityColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Predicted: ${estMins}m (Hold to manage)", color = Color.Gray, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // COMPLETED TASKS HISTORY SECTION
        if (completedTasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("COMPLETED TASKS HISTORY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            completedTasks.take(5).forEach { task ->
                val estMins = (task.estimatedDurationSeconds ?: 600) / 60
                val actualSecs = task.actualDurationSeconds ?: (estMins * 60)
                val actualMins = actualSecs / 60

                val diffMins = estMins - actualMins
                val comparisonText = when {
                    diffMins > 0 -> "🟢 ${diffMins}m faster than prediction"
                    diffMins < 0 -> "🟡 ${Math.abs(diffMins)}m over prediction"
                    else -> "🟢 Exact prediction match"
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101C14)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${task.name} ✓", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Took ${actualMins}m (Predicted: ${estMins}m) • $comparisonText", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddCountdownDialog) {
        AddCountdownDialog(
            onDismiss = { showAddCountdownDialog = false },
            onConfirm = { countdown ->
                onAddCountdown(countdown)
                showAddCountdownDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, estMins, priority ->
                onAddTask(title, estMins, priority)
                showAddTaskDialog = false
            }
        )
    }

    if (showBrainstormDialog) {
        BrainstormingBacklogDialog(
            tasks = unfinishedProjectTasks,
            onDismiss = { showBrainstormDialog = false },
            onConvertToTask = { taskTitle ->
                onAddTask(taskTitle, 15, "MEDIUM")
                showBrainstormDialog = false
            },
            onDeleteTask = { task -> onDeleteProjectTask(task) }
        )
    }

    selectedTaskForAction?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTaskForAction = null },
            title = { Text("Manage Task") },
            text = { Text("Choose an action for '${task.name}':") },
            confirmButton = {
                Button(
                    onClick = {
                        onStartTaskTracking(task)
                        selectedTaskForAction = null
                    }
                ) {
                    Text("Start Tracking ⏱️")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            taskToEdit = task
                            selectedTaskForAction = null
                        }
                    ) {
                        Text("Edit ✏️")
                    }
                    TextButton(
                        onClick = {
                            onDeleteTask(task)
                            selectedTaskForAction = null
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                }
            }
        )
    }

    taskToEdit?.let { task ->
        var editedName by remember { mutableStateOf(task.name) }
        var editedEstMins by remember { mutableStateOf(((task.estimatedDurationSeconds ?: 600) / 60).toString()) }
        var editedPriority by remember { mutableStateOf(task.priority) }

        AlertDialog(
            onDismissRequest = { taskToEdit = null },
            title = { Text("Edit Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Task Description") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedEstMins,
                        onValueChange = { editedEstMins = it },
                        label = { Text("Predicted Duration (minutes)") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedPriority,
                        onValueChange = { editedPriority = it },
                        label = { Text("Priority (HIGH, MEDIUM, LOW)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            val estSecs = (editedEstMins.toIntOrNull() ?: 10) * 60
                            onUpdateTask(task.copy(name = editedName, estimatedDurationSeconds = estSecs, priority = editedPriority))
                            taskToEdit = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BrainstormingBacklogDialog(
    tasks: List<ProjectTask>,
    onDismiss: () -> Unit,
    onConvertToTask: (String) -> Unit,
    onDeleteTask: (ProjectTask) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Brainstorming Ideas Backlog 💡") },
        text = {
            if (tasks.isEmpty()) {
                Text("No ideas in backlog. Ideas captured during Morning Journal and Evening Review appear here!", color = Color.Gray)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(tasks) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(task.title, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onConvertToTask(task.title) }) {
                                    Text("+ To-Do", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteTask(task) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var estMinsText by remember { mutableStateOf("15") }
    var priority by remember { mutableStateOf("MEDIUM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New To-Do Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description") }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = estMinsText,
                    onValueChange = { estMinsText = it },
                    label = { Text("Predicted Duration (minutes)") }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("Priority (HIGH, MEDIUM, LOW)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val estMins = estMinsText.toIntOrNull() ?: 15
                        onConfirm(title, estMins, priority)
                    }
                }
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (DateCountdown) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf(LocalDate.now().plusDays(30).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Countdown / Milestone") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title (e.g. New Year, Birthday)") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Date (YYYY-MM-DD)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && targetDate.isNotBlank()) {
                        onConfirm(DateCountdown(title = title, targetDate = targetDate))
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// PAGE 5: MENTAL VAULT & FLASHCARDS
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoloMentalVaultTab(
    deckSummaries: List<DeckSummary>,
    quotes: List<MotivationalQuote>,
    onDeckPlay: (String) -> Unit,
    onAddQuote: (MotivationalQuote) -> Unit = {},
    onUpdateQuote: (MotivationalQuote) -> Unit = {},
    onDeleteQuote: (MotivationalQuote) -> Unit = {}
) {
    var showAddQuoteDialog by remember { mutableStateOf(false) }
    var selectedQuoteForAction by remember { mutableStateOf<MotivationalQuote?>(null) }
    var quoteToEdit by remember { mutableStateOf<MotivationalQuote?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("THE MENTAL VAULT & INT TRAINING", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // MOVED TO TOP: STOIC MOTIVATIONAL MAXIMS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STOIC MOTIVATIONAL MAXIMS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showAddQuoteDialog = true }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Add Quote", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (quotes.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No motivational quotes added yet. Add maxims above!", color = Color.Gray, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
        } else {
            quotes.reversed().forEach { quote ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { selectedQuoteForAction = quote }
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("\"${quote.text}\"", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Category: ${quote.category} (Long-press to edit/delete)", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Flashcard Decks & Cognitive Training", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${deckSummaries.size} Decks Available • INT Attribute Booster", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("FLASHCARD DECKS (ANKI SPACED RECOVERY)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        if (deckSummaries.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No flashcard decks imported yet. Import Anki decks in The Forge.", color = Color.Gray, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
        } else {
            deckSummaries.forEach { deck ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onDeckPlay(deck.name) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Style, null, tint = Color(0xFF81C784))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(deck.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${deck.totalCards} cards (${deck.cardsDue} due today)", color = Color.Gray, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onDeckPlay(deck.name) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2D1F)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Review", color = Color(0xFF81C784), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddQuoteDialog) {
        AddQuoteDialog(
            onDismiss = { showAddQuoteDialog = false },
            onConfirm = { quote ->
                onAddQuote(quote)
                showAddQuoteDialog = false
            }
        )
    }

    selectedQuoteForAction?.let { quote ->
        AlertDialog(
            onDismissRequest = { selectedQuoteForAction = null },
            title = { Text("Manage Maxim") },
            text = { Text("What would you like to do with this quote?") },
            confirmButton = {
                Button(
                    onClick = {
                        quoteToEdit = quote
                        selectedQuoteForAction = null
                    }
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteQuote(quote)
                        selectedQuoteForAction = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            }
        )
    }

    quoteToEdit?.let { quote ->
        var editedText by remember { mutableStateOf(quote.text) }
        var editedCategory by remember { mutableStateOf(quote.category) }

        AlertDialog(
            onDismissRequest = { quoteToEdit = null },
            title = { Text("Edit Motivational Maxim") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        label = { Text("Quote Text") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedCategory,
                        onValueChange = { editedCategory = it },
                        label = { Text("Category") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedText.isNotBlank()) {
                            onUpdateQuote(quote.copy(text = editedText, category = editedCategory))
                            quoteToEdit = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { quoteToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddQuoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (MotivationalQuote) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Discipline") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Motivational Maxim") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Quote / Maxim text") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Discipline, Focus)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(MotivationalQuote(text = text, category = category))
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// PAGE 6: WILLPOWER & SOBRIETY SHRINE
// ---------------------------------------------------------------------------
@Composable
fun SoloWillpowerShrineTab(
    userStats: UserStats?,
    currentPledge: DailyPledge?,
    onUrgeClick: () -> Unit,
    onPledgeClick: () -> Unit,
    onViewLogClick: () -> Unit,
    onStartPlanning: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("WILLPOWER & SOBRIETY SHRINE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        WillpowerDashboard(
            userStats = userStats,
            currentPledge = currentPledge,
            onUrgeClick = onUrgeClick,
            onPledgeClick = onPledgeClick,
            onViewLogClick = onViewLogClick,
            onStartPlanning = onStartPlanning
        )
    }
}
