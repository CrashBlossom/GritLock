package com.example.fitlock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.TodoTask
import com.example.fitlock.data.WorkoutHistory
import java.text.SimpleDateFormat
import java.util.*

/**
 * StatsScreen shows historical data for both workouts and productivity.
 */
@Composable
fun StatsScreen(
    history: List<WorkoutHistory>,
    tasks: List<TodoTask> // Pass tasks to show productivity analytics
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Allow scrolling the entire screen
    ) {
        Text(
            "Analytics & History",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Track your growth over time",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- PRODUCTIVITY ANALYTICS SECTION ---
        Text(
            "Estimation Accuracy",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Predicted (Blue) vs Actual (Orange) Pomodoros",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ProductivityChart(tasks)

        Spacer(modifier = Modifier.height(32.dp))

        // --- WORKOUT HISTORY SECTION ---
        Text(
            "Recent Workouts",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No workouts recorded yet", color = Color.Gray)
            }
        } else {
            // Since the main screen is already scrollable, we use a Column for history
            // instead of a nested LazyColumn
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                history.take(10).forEach { workout -> // Show last 10
                    WorkoutHistoryItem(workout)
                }
            }
        }
    }
}

/**
 * A custom chart that compares how long you thought a task would take
 * vs how long it actually took. This helps improve your INT level!
 */
@Composable
fun ProductivityChart(tasks: List<TodoTask>) {
    val completedTasks = tasks.filter { it.isCompleted || it.actualPomodoros > 0 }.takeLast(7)
    
    if (completedTasks.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().height(150.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Complete quests to see analytics", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val maxVal = (completedTasks.flatMap { listOf(it.predictedPomodoros, it.actualPomodoros) }.maxOrNull() ?: 1).toFloat()

            completedTasks.forEach { task ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight().weight(1f)
                ) {
                    Box(modifier = Modifier.fillMaxHeight().width(30.dp), contentAlignment = Alignment.BottomCenter) {
                        // Background full height column
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = size.width * 0.4f
                            val spacing = size.width * 0.1f
                            val corner = 4.dp.toPx()

                            // Predicted Bar (Blue)
                            val pHeight = (task.predictedPomodoros / maxVal) * size.height
                            drawRoundRect(
                                color = Color(0xFF4B7BFF),
                                topLeft = Offset(spacing, size.height - pHeight),
                                size = Size(barWidth, pHeight),
                                cornerRadius = CornerRadius(corner, corner)
                            )

                            // Actual Bar (Orange)
                            val aHeight = (task.actualPomodoros / maxVal) * size.height
                            drawRoundRect(
                                color = Color(0xFFFFB34B),
                                topLeft = Offset(barWidth + spacing * 2, size.height - aHeight),
                                size = Size(barWidth, aHeight),
                                cornerRadius = CornerRadius(corner, corner)
                            )
                        }
                    }
                    Text(
                        task.title.take(3) + "..", 
                        fontSize = 8.sp, 
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryItem(workout: WorkoutHistory) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(workout.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(workout.exerciseType, color = Color.White, fontWeight = FontWeight.Bold)
                Text(dateString, color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                "${workout.repsCompleted} reps",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
