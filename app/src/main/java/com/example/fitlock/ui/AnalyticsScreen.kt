package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import com.example.fitlock.data.GauntletHistory
import com.example.fitlock.data.WorkoutHistory
import com.example.fitlock.utils.AppUsageInfo
import com.example.fitlock.utils.UsageUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun AnalyticsScreen(history: List<WorkoutHistory>, gauntletHistory: List<GauntletHistory> = emptyList()) {
    val context = LocalContext.current
    var usageStats by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    val hasPermission = remember { UsageUtils.hasUsageStatsPermission(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            usageStats = UsageUtils.getAppUsageStats(context)
                .filter { it.totalTimeVisible > 0 }
                .sortedByDescending { it.totalTimeVisible }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            "Analytic",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Usage Stats permission required", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Apps") },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Exercises") },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Totals") },
                    icon = { Icon(Icons.Default.Language, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Routines") },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> AppUsageList(usageStats)
                1 -> ExerciseHistoryList(history)
                2 -> DailyTotalsLeaderboard(history)
                3 -> GauntletHistoryList(gauntletHistory)
            }
        }
    }
}

@Composable
fun ExerciseHistoryList(history: List<WorkoutHistory>) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No exercise history yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(history) { workout ->
                AnalyticsWorkoutHistoryItem(workout)
            }
        }
    }
}

@Composable
fun AnalyticsWorkoutHistoryItem(workout: WorkoutHistory) {
    val date = remember(workout.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(workout.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.exerciseType, color = Color.White, fontWeight = FontWeight.Bold)
                Text(date, color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${workout.repsCompleted} Reps", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("+${workout.xpGained} XP", color = Color.Green, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AppUsageList(usageStats: List<AppUsageInfo>) {
    val totalScreenTime = usageStats.sumOf { it.totalTimeVisible }
    val totalHours = TimeUnit.MILLISECONDS.toHours(totalScreenTime)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalScreenTime) % 60

    Column {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Screen Time Today", color = Color.Gray, fontSize = 12.sp)
                Text("${totalHours}h ${totalMinutes}m", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text("Most Used Apps", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(usageStats) { app ->
                AppUsageItem(app)
            }
        }
    }
}

@Composable
fun DailyTotalsLeaderboard(history: List<WorkoutHistory>) {
    val dailyTotals = remember(history) {
        history.groupBy { 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
        }.mapValues { entry ->
            entry.value.sumOf { it.repsCompleted }
        }.toList().sortedByDescending { it.first }
    }

    if (dailyTotals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No exercise data found.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "Past Performance",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(dailyTotals) { (dateStr, totalReps) ->
                val displayDate = remember(dateStr) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                    SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(date ?: Date())
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(displayDate, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Total Reps", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text(
                            "$totalReps",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GauntletHistoryList(history: List<GauntletHistory>) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No routine history yet.", color = Color.Gray)
        }
    } else {
        Column {
            val totalSeconds = history.sumOf { it.totalTimeSeconds }
            val focusMinutes = totalSeconds / 60
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Focus Time", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp)
                    Text("${focusMinutes} Minutes", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(history) { record ->
                    GauntletHistoryItem(record)
                }
            }
        }
    }
}

@Composable
fun GauntletHistoryItem(record: GauntletHistory) {
    val date = remember(record.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Routine Session", color = Color.White, fontWeight = FontWeight.Bold)
                Text(date, color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                val timeStr = if (record.totalTimeSeconds > 3600) {
                    "${record.totalTimeSeconds / 3600}h ${(record.totalTimeSeconds % 3600) / 60}m"
                } else {
                    "${record.totalTimeSeconds / 60}m ${record.totalTimeSeconds % 60}s"
                }
                Text(timeStr, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${record.habitLogs.size} Habits", color = Color.Gray, fontSize = 10.sp)
                Text("+${record.xpGained} XP", color = Color.Green, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AppUsageItem(app: AppUsageInfo) {
    val hours = TimeUnit.MILLISECONDS.toHours(app.totalTimeVisible)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(app.totalTimeVisible) % 60
    val timeStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val appName = remember { app.packageName.split(".").last().replaceFirstChar { it.uppercase() } }
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(app.packageName, color = Color.Gray, fontSize = 10.sp)
            }
            Text(timeStr, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
