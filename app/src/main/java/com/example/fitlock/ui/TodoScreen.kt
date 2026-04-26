/**
 * TodoScreen is the "Quest Board."
 * It now features a real-time Pomodoro timer to facilitate "Deep Work" sessions.
 */
package com.example.fitlock.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.TodoTask
import kotlinx.coroutines.delay

@Composable
fun TodoScreen(
    tasks: List<TodoTask>,
    onAddTask: (TodoTask) -> Unit,
    onToggleTask: (TodoTask) -> Unit,
    onDeleteTask: (TodoTask) -> Unit,
    onPomodoroComplete: (TodoTask) -> Unit // Grants INT XP when a full session finishes
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    // --- POMODORO TIMER STATE ---
    var activeTaskForTimer by remember { mutableStateOf<TodoTask?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // This "LaunchedEffect" handles the clock ticking every second
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (secondsRemaining > 0 && isTimerRunning) {
                delay(1000)
                secondsRemaining--
            }
            if (secondsRemaining <= 0) {
                isTimerRunning = false
                activeTaskForTimer?.let { onPomodoroComplete(it) }
                activeTaskForTimer = null
                secondsRemaining = 25 * 60 // Reset for next session
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Quest Board", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Focus to gain Intelligence", color = Color.Gray, fontSize = 14.sp)
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Quest", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. ACTIVE POMODORO OVERLAY
        AnimatedVisibility(
            visible = activeTaskForTimer != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            PomodoroTimerCard(
                taskTitle = activeTaskForTimer?.title ?: "",
                secondsRemaining = secondsRemaining,
                isRunning = isTimerRunning,
                onToggle = { isTimerRunning = !isTimerRunning },
                onCancel = {
                    activeTaskForTimer = null
                    isTimerRunning = false
                    secondsRemaining = 25 * 60
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Quest List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tasks) { task ->
                TodoItem(
                    task = task,
                    isBeingTimed = activeTaskForTimer?.id == task.id,
                    onToggle = { onToggleTask(task) },
                    onDelete = { onDeleteTask(task) },
                    onStartTimer = {
                        if (activeTaskForTimer == null) {
                            activeTaskForTimer = task
                            isTimerRunning = true
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddQuestDialog(onDismiss = { showAddDialog = false }, onConfirm = onAddTask)
    }
}

/**
 * Visual card for the countdown timer.
 */
@Composable
fun PomodoroTimerCard(
    taskTitle: String,
    secondsRemaining: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onCancel: () -> Unit
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    val progress = secondsRemaining.toFloat() / (25f * 60f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FOCUSING ON:", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(taskTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(120.dp),
                    color = Color.White,
                    strokeWidth = 8.dp,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
                Text(timeText, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Text(if (isRunning) " PAUSE" else " RESUME")
                }
                TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("ABANDON")
                }
            }
        }
    }
}

@Composable
fun TodoItem(
    task: TodoTask,
    isBeingTimed: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onStartTimer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isBeingTimed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(task.title, fontWeight = FontWeight.Bold, color = if (task.isCompleted) Color.Gray else Color.White)
                Text("${task.actualPomodoros}/${task.predictedPomodoros} Pomos", fontSize = 12.sp, color = Color.Gray)
            }
            if (!task.isCompleted && !isBeingTimed) {
                IconButton(onClick = onStartTimer) {
                    Icon(Icons.Default.Timer, contentDescription = "Start Timer", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun AddQuestDialog(onDismiss: () -> Unit, onConfirm: (TodoTask) -> Unit) {
    var title by remember { mutableStateOf("") }
    var predicted by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Quest") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") })
                OutlinedTextField(value = predicted, onValueChange = { predicted = it }, label = { Text("Predicted Pomodoros") })
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(TodoTask(title = title, predictedPomodoros = predicted.toIntOrNull() ?: 1))
                onDismiss()
            }) { Text("Accept") }
        }
    )
}
