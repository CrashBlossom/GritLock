package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    currentPledge: DailyPledge,
    unfinishedTasks: List<QuestBlock>,
    routines: List<QuestWithBlocks>,
    habitDefinitions: List<HabitDefinition>,
    exerciseDefinitions: List<ExerciseDefinition>,
    taskDefinitions: List<TaskDefinition>,
    projectTasks: List<ProjectTask>,
    onCommit: (DailyPledge, List<QuestBlock>, Boolean) -> Unit, // Expanded to include the ritual data
    onBack: () -> Unit
) {
    var currentStage by remember { mutableIntStateOf(0) } // 0: Ritual, 1: Quest Builder
    var ritualPledge by remember { mutableStateOf(currentPledge) }

    if (currentStage == 0) {
        RitualWizard(
            isMorning = true,
            pledge = ritualPledge,
            onSave = { updated -> 
                ritualPledge = updated
                currentStage = 1
            },
            onBack = onBack
        )
    } else {
        val selectedBlocks = remember { mutableStateListOf<QuestBlock>() }
        var isLockShieldEnabled by remember { mutableStateOf(true) }

        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Planning") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isLockShieldEnabled, onCheckedChange = { isLockShieldEnabled = it })
                        Text("Enable Lock Shield (unlock at the end only)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onCommit(ritualPledge, selectedBlocks.toList(), isLockShieldEnabled) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedBlocks.isNotEmpty()
                    ) {
                        Text("Commit to Quest")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            item {
                Text("Carried Over Tasks", style = MaterialTheme.typography.titleMedium)
                Text("Unfinished business from yesterday.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (unfinishedTasks.isEmpty()) {
                item {
                    Text("No tasks to carry over.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(unfinishedTasks) { task ->
                    val isSelected = selectedBlocks.any { it.id == task.id }
                    TaskSelectionItem(task, isSelected) {
                        if (isSelected) selectedBlocks.removeAll { it.id == task.id }
                        else selectedBlocks.add(task)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Available Routines", style = MaterialTheme.typography.titleMedium)
                Text("Slot your recurring disciplines into today's quest.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(routines) { routine ->
                RoutineSelectionItem(routine) {
                    // Add all blocks from the routine as new blocks for the daily quest
                    routine.blocks.forEach { block ->
                        selectedBlocks.add(block.copy(id = java.util.UUID.randomUUID().toString(), questId = "")) 
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Project Tasks", style = MaterialTheme.typography.titleMedium)
                Text("Specific tasks from your backlog.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (projectTasks.isEmpty()) {
                item { Text("No project tasks available.", color = Color.Gray) }
            } else {
                items(projectTasks) { task ->
                    ForgeSelectionItem(task.title, "PROJECT TASK", Icons.Default.Assignment) {
                        selectedBlocks.add(QuestBlock(
                            id = java.util.UUID.randomUUID().toString(),
                            questId = "",
                            type = BlockType.TASK,
                            orderIndex = selectedBlocks.size,
                            name = task.title,
                            autoCarryOver = true,
                            sourceTaskId = task.id
                        ))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Habit Library", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(habitDefinitions) { def ->
                ForgeSelectionItem(def.name, "HABIT", Icons.Default.Timer) {
                    selectedBlocks.add(QuestBlock(
                        id = java.util.UUID.randomUUID().toString(),
                        questId = "",
                        type = BlockType.HABIT,
                        orderIndex = selectedBlocks.size,
                        name = def.name,
                        definitionId = def.id,
                        estimatedDurationSeconds = def.defaultEstimatedDurationSeconds,
                        mediaUrl = def.mediaUrl,
                        mediaType = def.mediaType,
                        trackingType = def.trackingType
                    ))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Exercise Library", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(exerciseDefinitions) { def ->
                ForgeSelectionItem(def.name, "${def.defaultSets}x${def.defaultTargetReps} ${def.type}", Icons.Default.FitnessCenter) {
                    selectedBlocks.add(QuestBlock(
                        id = java.util.UUID.randomUUID().toString(),
                        questId = "",
                        type = BlockType.EXERCISE,
                        orderIndex = selectedBlocks.size,
                        name = def.name,
                        sets = def.defaultSets,
                        targetReps = def.defaultTargetReps,
                        restBetweenSets = def.defaultRestSeconds,
                        trackingType = HabitTrackingType.REPS
                    ))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Task Templates", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(taskDefinitions) { def ->
                ForgeSelectionItem(def.name, def.category ?: "General", Icons.Default.Assignment) {
                    selectedBlocks.add(QuestBlock(
                        id = java.util.UUID.randomUUID().toString(),
                        questId = "",
                        type = BlockType.TASK,
                        orderIndex = selectedBlocks.size,
                        name = def.name,
                        autoCarryOver = def.autoCarryOver
                    ))
                }
            }
        }
    }
}
}

@Composable
fun ForgeSelectionItem(name: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, null)
            }
        }
    }
}

@Composable
fun TaskSelectionItem(task: QuestBlock, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Text(task.name, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RoutineSelectionItem(routine: QuestWithBlocks, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(routine.quest.name, fontWeight = FontWeight.Bold)
                Text("${routine.blocks.size} blocks", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, null)
                Text("Add")
            }
        }
    }
}
