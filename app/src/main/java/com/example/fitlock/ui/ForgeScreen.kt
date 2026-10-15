package com.example.fitlock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.*
import com.example.fitlock.exercise.ExerciseType
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeScreen(
    habitDefinitions: List<HabitDefinition> = emptyList(),
    onHabitUpsert: (HabitDefinition) -> Unit = {},
    onHabitDelete: (HabitDefinition) -> Unit = {},
    exerciseDefinitions: List<ExerciseDefinition> = emptyList(),
    onExerciseUpsert: (ExerciseDefinition) -> Unit = {},
    onExerciseDelete: (ExerciseDefinition) -> Unit = {},
    taskDefinitions: List<TaskDefinition> = emptyList(),
    onTaskUpsert: (TaskDefinition) -> Unit = {},
    onTaskDelete: (TaskDefinition) -> Unit = {},
    appGroups: List<AppGroup> = emptyList(),
    challenges: List<Challenge> = emptyList(),
    onEditGate: (AppGroup) -> Unit = {},
    onDeleteGate: (AppGroup) -> Unit = {},
    onAddNewGate: () -> Unit = {},
    onExerciseLog: (ExerciseType, Int, String?, Int, String?) -> Unit,
    onProgressionClick: () -> Unit,
    onStartPlanning: () -> Unit = {},
    allFamilies: List<FamilyWithLevels> = emptyList(),
    currentProgression: Map<String, Int> = emptyMap(),
    title: String = "The Gress (Training)",
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Habit Quests", "Protocol Gates", "Physical Exercises")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tabTitle) }
                    )
                }
            }

            when (selectedTab) {
                0 -> HabitLibraryContent(habitDefinitions, onHabitUpsert, onHabitDelete)
                1 -> ProtocolGatesContent(appGroups, challenges, onEditGate, onDeleteGate, onAddNewGate, onStartPlanning)
                2 -> ExerciseLibraryContent(exerciseDefinitions, onExerciseUpsert, onExerciseDelete, onExerciseLog, onProgressionClick, allFamilies, currentProgression)
            }
        }
    }
}

@Composable
fun ProtocolGatesContent(
    appGroups: List<AppGroup>,
    challenges: List<Challenge>,
    onEditGate: (AppGroup) -> Unit,
    onDeleteGate: (AppGroup) -> Unit,
    onAddNewGate: () -> Unit,
    onStartPlanning: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("🌟 All") }
    val filters = listOf("🌟 All", "📜 Routines", "⚔️ Challenges", "🔒 App Blocks", "🟢 Positive Portals", "🔑 Secret Vaults")

    val challengeAsGroups = remember(challenges) {
        challenges.map { ch ->
            val isMorning = ch.title.contains("Morning", ignoreCase = true)
            val isEvening = ch.title.contains("Evening", ignoreCase = true) || ch.title.contains("Night", ignoreCase = true)
            val timeConstraint = when {
                isMorning -> "04:00-12:00"
                isEvening -> "18:00-23:59"
                else -> null
            }
            AppGroup(
                id = ch.id.hashCode(),
                name = ch.title,
                packageNames = emptyList(),
                exercises = ch.requirements,
                unlockDurationMinutes = 0,
                isEnabled = true,
                isRoutine = false,
                isProductive = false,
                gateMode = GateMode.BACKGROUND_RAID,
                triggerType = if (timeConstraint != null) GauntletTriggerType.TIME_BASED else GauntletTriggerType.MANUAL,
                triggerTime = timeConstraint,
                xpReward = ch.xpReward
            )
        }
    }
    val combinedGroups = remember(appGroups, challengeAsGroups) { appGroups + challengeAsGroups }

    val filteredGroups = remember(combinedGroups, selectedFilter) {
        when {
            selectedFilter.contains("Routines") -> combinedGroups.filter { it.isRoutine }
            selectedFilter.contains("Challenges") -> combinedGroups.filter { it.gateMode == GateMode.BACKGROUND_RAID }
            selectedFilter.contains("App Blocks") -> combinedGroups.filter { !it.isRoutine && !it.isProductive && it.gateMode != GateMode.BACKGROUND_RAID }
            selectedFilter.contains("Positive Portals") -> combinedGroups.filter { it.isProductive }
            selectedFilter.contains("Secret Vaults") -> combinedGroups.filter { !it.secretPassword.isNullOrBlank() || !it.secretLink.isNullOrBlank() }
            else -> combinedGroups
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = onStartPlanning, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.EditCalendar, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Routine & Quest Planner")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (filteredGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No protocol gates found for '$selectedFilter'. Tap '+' to forge one.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(filteredGroups) { group ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onEditGate(group) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val catEmoji = when {
                                        group.isRoutine -> "📜"
                                        group.isProductive -> "🟢"
                                        group.gateMode == GateMode.BACKGROUND_RAID -> "⚔️"
                                        else -> "🔒"
                                    }
                                    val reqSummary = if (group.exercises.isEmpty()) "None" else group.exercises.joinToString(", ") { "${it.count} ${it.type}" }
                                    val constraintsSummary = buildList {
                                        if (group.restrictedWifiSsids.isNotEmpty()) add("WiFi: ${group.restrictedWifiSsids.joinToString(",")}")
                                        if (!group.triggerTime.isNullOrBlank()) add("Time: ${group.triggerTime}")
                                    }.ifEmpty { listOf("Anytime") }.joinToString(" • ")

                                    Text("$catEmoji Trigger: ${group.triggerType} • Constraints: $constraintsSummary", color = Color.Gray, fontSize = 11.sp)
                                    Text("Requirements: $reqSummary", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    if (!group.secretPassword.isNullOrBlank() || !group.secretLink.isNullOrBlank()) {
                                        Text("🔑 Secret Vault Attached", color = Color(0xFFFFB74D), fontSize = 10.sp)
                                    }
                                }
                                IconButton(onClick = { onDeleteGate(group) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddNewGate,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, "Add Protocol Gate")
        }
    }
}

@Composable
fun HabitLibraryContent(
    definitions: List<HabitDefinition>,
    onUpsert: (HabitDefinition) -> Unit,
    onDelete: (HabitDefinition) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<HabitDefinition?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (definitions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No habit definitions configured.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(definitions) { def ->
                    ForgeItemCard(
                        title = def.name,
                        subtitle = "Tracking: ${def.trackingType}",
                        icon = Icons.Default.CheckCircle,
                        onClick = { showEditDialog = def },
                        onDelete = { onDelete(def) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showEditDialog = HabitDefinition(name = "") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Habit")
        }
    }

    showEditDialog?.let { def ->
        EditHabitDefinitionDialog(
            definition = def,
            onDismiss = { showEditDialog = null },
            onConfirm = { updatedDef ->
                onUpsert(updatedDef)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun ExerciseLibraryContent(
    definitions: List<ExerciseDefinition>,
    onUpsert: (ExerciseDefinition) -> Unit,
    onDelete: (ExerciseDefinition) -> Unit,
    onExerciseLog: (ExerciseType, Int, String?, Int, String?) -> Unit,
    onProgressionClick: () -> Unit,
    allFamilies: List<FamilyWithLevels>,
    currentProgression: Map<String, Int>
) {
    var showEditDialog by remember { mutableStateOf<ExerciseDefinition?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Button(onClick = onProgressionClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.TrendingUp, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Skill Progression Trees")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Custom Exercise Definitions (${definitions.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(definitions) { def ->
                val type = try { ExerciseType.valueOf(def.type) } catch (e: Exception) { ExerciseType.PUSHUP }
                ForgeItemCard(
                    title = def.name,
                    subtitle = "Default: ${def.defaultTargetReps} reps (${def.defaultSets} sets)",
                    icon = Icons.Default.FitnessCenter,
                    onClick = { 
                        val target = def.defaultTargetReps.toIntOrNull() ?: 10
                        onExerciseLog(type, target, null, 1, null)
                    },
                    onDelete = { onDelete(def) },
                    actionIcon = Icons.Default.PlayArrow
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { showEditDialog = ExerciseDefinition(name = "", type = "PUSHUP", defaultTargetReps = "15") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Exercise")
        }
    }

    showEditDialog?.let { def ->
        EditExerciseDefinitionDialog(
            definition = def,
            onDismiss = { showEditDialog = null },
            onConfirm = { updatedDef ->
                onUpsert(updatedDef)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun ForgeItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    actionIcon: ImageVector? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit?.invoke() ?: onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (actionIcon != null) {
                IconButton(onClick = onClick) {
                    Icon(actionIcon, "Action", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseDefinitionDialog(
    definition: ExerciseDefinition,
    onDismiss: () -> Unit,
    onConfirm: (ExerciseDefinition) -> Unit
) {
    var name by remember { mutableStateOf(definition.name) }
    var type by remember { mutableStateOf(try { ExerciseType.valueOf(definition.type) } catch(e: Exception) { ExerciseType.PUSHUP }) }
    var sets by remember { mutableStateOf(definition.defaultSets.toString()) }
    var reps by remember { mutableStateOf(definition.defaultTargetReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (definition.id == 0) "New Exercise" else "Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") })
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Type: ${type.name}")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ExerciseType.entries.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { type = it; expanded = false })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Target Reps") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(definition.copy(
                    name = name,
                    type = type.name,
                    defaultSets = sets.toIntOrNull() ?: 3,
                    defaultTargetReps = reps
                ))
            }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditHabitDefinitionDialog(
    definition: HabitDefinition,
    onDismiss: () -> Unit,
    onConfirm: (HabitDefinition) -> Unit
) {
    var name by remember { mutableStateOf(definition.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (definition.id == 0) "New Habit" else "Edit Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Habit Name") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(definition.copy(name = name))
            }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
