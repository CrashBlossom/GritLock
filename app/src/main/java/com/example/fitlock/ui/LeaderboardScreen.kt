package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.WorkoutHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyLeaderboardEntry(
    val dateString: String,
    val totalReps: Int,
    val totalXp: Int,
    val pushups: Int,
    val squats: Int,
    val planks: Int,
    val breathworkSeconds: Int,
    val exerciseCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    history: List<WorkoutHistory>,
    onBack: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displaySdf = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US) }

    val rankedDays = remember(history) {
        history.groupBy { sdf.format(Date(it.timestamp)) }
            .map { (dateStr, workouts) ->
                val pushups = workouts.filter { it.exerciseType.equals("PUSHUP", ignoreCase = true) }.sumOf { it.repsCompleted }
                val squats = workouts.filter { it.exerciseType.equals("SQUAT", ignoreCase = true) }.sumOf { it.repsCompleted }
                val planks = workouts.filter { it.exerciseType.equals("PLANK", ignoreCase = true) }.sumOf { it.repsCompleted }
                val breathwork = workouts.filter { it.exerciseType.equals("BREATHWORK", ignoreCase = true) }.sumOf { it.repsCompleted }
                val totalReps = workouts.sumOf { it.repsCompleted }
                val totalXp = workouts.sumOf { it.xpGained }

                DailyLeaderboardEntry(
                    dateString = dateStr,
                    totalReps = totalReps,
                    totalXp = totalXp,
                    pushups = pushups,
                    squats = squats,
                    planks = planks,
                    breathworkSeconds = breathwork,
                    exerciseCount = workouts.size
                )
            }
            .sortedByDescending { it.totalReps }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Performance Leaderboard", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121216))
            )
        },
        containerColor = Color(0xFF121216)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("RANKED TRAINING DAYS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Comparing total daily volume & discipline records across time", color = Color.Gray, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(12.dp))

            if (rankedDays.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No workout history recorded yet. Complete workouts to rank your days!", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(rankedDays) { index, entry ->
                        LeaderboardDayCard(rank = index + 1, entry = entry, displaySdf = displaySdf, sdf = sdf)
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
fun LeaderboardDayCard(
    rank: Int,
    entry: DailyLeaderboardEntry,
    displaySdf: SimpleDateFormat,
    sdf: SimpleDateFormat
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold #1 Day
        2 -> Color(0xFFC0C0C0) // Silver #2 Day
        3 -> Color(0xFFCD7F32) // Bronze #3 Day
        else -> MaterialTheme.colorScheme.primary
    }

    val formattedDate = try {
        val parsed = sdf.parse(entry.dateString)
        if (parsed != null) displaySdf.format(parsed) else entry.dateString
    } catch (e: Exception) {
        entry.dateString
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (rank <= 3) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = rankColor, modifier = Modifier.size(24.dp))
                } else {
                    Text("#$rank", fontWeight = FontWeight.Bold, color = rankColor, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(formattedDate, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("${entry.exerciseCount} workout sessions • +${entry.totalXp} XP earned", color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (entry.pushups > 0) Text("💪 ${entry.pushups} Pushups", color = Color(0xFFEF5350), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    if (entry.squats > 0) Text("🦵 ${entry.squats} Squats", color = Color(0xFF64B5F6), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    if (entry.planks > 0) Text("⏱️ ${entry.planks}s Planks", color = Color(0xFFFFB74D), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    if (entry.breathworkSeconds > 0) Text("🫁 ${entry.breathworkSeconds}s Breathwork", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.totalReps}", fontWeight = FontWeight.Bold, color = rankColor, fontSize = 20.sp)
                Text("Total Reps", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}
