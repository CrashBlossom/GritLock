package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailsScreen(
    habit: HabitWithDefinition,
    history: List<GauntletHistory>,
    onBack: () -> Unit
) {
    val habitLogs = remember(history) {
        history.flatMap { h -> h.habitLogs.map { it.copy() to h.timestamp } }
            .filter { it.first.habitId == habit.habit.id }
            .sortedByDescending { it.second }
    }

    val pb = remember(habitLogs) {
        if (habit.effectiveTrackingType == HabitTrackingType.REPS) {
            habitLogs.mapNotNull { it.first.repsCompleted }.maxOrNull() ?: 0
        } else {
            if (habit.habit.higherIsBetter) habitLogs.maxOfOrNull { it.first.actualDurationSeconds } ?: 0
            else habitLogs.minOfOrNull { it.first.actualDurationSeconds } ?: 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.effectiveName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                HabitStatsHeader(habit, pb, habitLogs.size)
            }

            item {
                Text("Consistency (Last 90 Days)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                HabitHeatmap(habitLogs.map { it.second })
            }

            item {
                Text("Recent Performance", style = MaterialTheme.typography.titleMedium)
            }

            items(habitLogs.take(10)) { (log, timestamp) ->
                PerformanceItem(log, timestamp, habit.effectiveTrackingType)
            }
        }
    }
}

@Composable
fun HabitStatsHeader(habit: HabitWithDefinition, pb: Int, totalCompletions: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Personal Best", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = if (habit.effectiveTrackingType == HabitTrackingType.REPS) "$pb Reps" else formatTime(pb),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(48.dp), tint = Color(0xFFFFD700))
        }
    }
}

@Composable
fun HabitHeatmap(completionTimestamps: List<Long>) {
    val calendar = Calendar.getInstance()
    val today = calendar.timeInMillis
    val ninetyDaysAgo = today - (90L * 24 * 60 * 60 * 1000L)
    
    val completedDates = completionTimestamps.map {
        val c = Calendar.getInstance()
        c.timeInMillis = it
        "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }.toSet()

    Column {
        // Simple grid for heatmap
        val columns = 13 // ~13 weeks
        val rows = 7
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(columns) { col ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(rows) { row ->
                        val dayOffset = (col * 7) + row
                        val dayTimestamp = today - (dayOffset.toLong() * 24 * 60 * 60 * 1000L)
                        
                        if (dayTimestamp >= ninetyDaysAgo) {
                            val c = Calendar.getInstance()
                            c.timeInMillis = dayTimestamp
                            val dateKey = "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
                            val isCompleted = completedDates.contains(dateKey)
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isCompleted) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceItem(log: HabitLog, timestamp: Long, type: HabitTrackingType) {
    val date = remember(timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(date, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = if (type == HabitTrackingType.REPS) "${log.repsCompleted ?: 0} Reps" else formatTime(log.actualDurationSeconds),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
