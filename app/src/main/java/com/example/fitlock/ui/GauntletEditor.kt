package com.example.fitlock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.fitlock.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GauntletEditor(
    editingGauntlet: GauntletWithHabits?,
    editingAdvanced: GauntletWithAdvancedWorkout?,
    groups: List<AppGroup>,
    habitDefinitions: List<HabitDefinition> = emptyList(),
    onSave: (Gauntlet, List<Habit>, List<WorkoutBlockWithExercises>, List<String>) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(editingGauntlet?.gauntlet?.name ?: "") }
    var triggerType by remember { mutableStateOf(editingGauntlet?.gauntlet?.triggerType ?: GauntletTriggerType.MANUAL) }
    var triggerTime by remember { mutableStateOf(editingGauntlet?.gauntlet?.triggerTime ?: "08:00") }
    var bufferSeconds by remember { mutableIntStateOf(editingGauntlet?.gauntlet?.bufferSeconds ?: 10) }
    var targetBlockGroupId by remember { mutableStateOf<Int?>(editingGauntlet?.gauntlet?.targetBlockGroupId) }
    var icon by remember { mutableStateOf<String?>(editingGauntlet?.gauntlet?.icon) }
    var physicalTriggerType by remember { mutableStateOf(editingGauntlet?.gauntlet?.physicalTriggerType ?: PhysicalTriggerType.NONE) }
    var physicalTriggerData by remember { mutableStateOf<String?>(editingGauntlet?.gauntlet?.physicalTriggerData) }
    var delayedNudgeMinutes by remember { mutableIntStateOf(editingGauntlet?.gauntlet?.delayedNudgeMinutes ?: 5) }
    
    val habits = remember { 
        mutableStateListOf<Habit>().apply {
            editingGauntlet?.habits?.sortedBy { it.habit.orderIndex }?.map { it.habit }?.let { addAll(it) }
        }
    }

    val blocks = remember {
        mutableStateListOf<WorkoutBlockWithExercises>().apply {
            editingAdvanced?.blocks?.sortedBy { it.block.orderIndex }?.let { addAll(it) }
        }
    }

    val tasks = remember { mutableStateListOf<String>() }

    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddBlockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingGauntlet == null) "New Gauntlet" else "Edit Gauntlet") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    Button(
                        onClick = { 
                            val g = editingGauntlet?.gauntlet?.copy(
                                name = name,
                                triggerType = triggerType,
                                triggerTime = triggerTime,
                                bufferSeconds = bufferSeconds,
                                targetBlockGroupId = targetBlockGroupId,
                                icon = icon,
                                physicalTriggerType = physicalTriggerType,
                                physicalTriggerData = physicalTriggerData,
                                delayedNudgeMinutes = delayedNudgeMinutes
                            ) ?: Gauntlet(
                                name = name, 
                                triggerType = triggerType, 
                                triggerTime = triggerTime,
                                bufferSeconds = bufferSeconds,
                                targetBlockGroupId = targetBlockGroupId,
                                icon = icon,
                                physicalTriggerType = physicalTriggerType,
                                physicalTriggerData = physicalTriggerData,
                                delayedNudgeMinutes = delayedNudgeMinutes
                            )
                            onSave(g, habits.toList(), blocks.toList(), tasks.toList())
                        },
                        enabled = name.isNotBlank() && (habits.isNotEmpty() || blocks.isNotEmpty() || tasks.isNotEmpty())
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Trigger Settings", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = triggerType == GauntletTriggerType.MANUAL,
                        onClick = { triggerType = GauntletTriggerType.MANUAL }
                    )
                    Text("Manual")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = triggerType == GauntletTriggerType.TIME_BASED,
                        onClick = { triggerType = GauntletTriggerType.TIME_BASED }
                    )
                    Text("Time Based")
                }
                
                if (triggerType == GauntletTriggerType.TIME_BASED) {
                    OutlinedTextField(
                        value = triggerTime,
                        onValueChange = { triggerTime = it },
                        label = { Text("Start Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                IconPicker(
                    selectedIcon = icon,
                    onIconSelected = { icon = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Physical Trigger", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = physicalTriggerType == PhysicalTriggerType.NONE,
                        onClick = { physicalTriggerType = PhysicalTriggerType.NONE }
                    )
                    Text("None")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = physicalTriggerType == PhysicalTriggerType.NFC,
                        onClick = { physicalTriggerType = PhysicalTriggerType.NFC }
                    )
                    Text("NFC")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = physicalTriggerType == PhysicalTriggerType.QR,
                        onClick = { physicalTriggerType = PhysicalTriggerType.QR }
                    )
                    Text("QR")
                }
                
                if (physicalTriggerType != PhysicalTriggerType.NONE) {
                    OutlinedTextField(
                        value = physicalTriggerData ?: "",
                        onValueChange = { physicalTriggerData = it },
                        label = { Text(if (physicalTriggerType == PhysicalTriggerType.NFC) "NFC Tag ID" else "QR Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Delayed Nudge (minutes)", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = delayedNudgeMinutes.toFloat(),
                    onValueChange = { delayedNudgeMinutes = it.toInt() },
                    valueRange = 1f..30f,
                    steps = 29
                )
                Text("Nudge after ${delayedNudgeMinutes}m if not started", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Transition Buffer (seconds)", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = bufferSeconds.toFloat(),
                    onValueChange = { bufferSeconds = it.toInt() },
                    valueRange = 0f..60f,
                    steps = 11
                )
                Text("${bufferSeconds}s buffer between tasks", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Focus Shield", style = MaterialTheme.typography.titleMedium)
                var expanded by remember { mutableStateOf(false) }
                val selectedGroup = groups.find { it.id == targetBlockGroupId }
                
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedGroup?.name ?: "No Block Shield")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("No Block Shield") },
                            onClick = { 
                                targetBlockGroupId = null
                                expanded = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    targetBlockGroupId = group.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Text("This group will be blocked while the Gauntlet is running.", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Habit Stack", style = MaterialTheme.typography.titleMedium)
                    Row {
                        IconButton(onClick = { showAddHabitDialog = true }) { Icon(Icons.Default.Add, null) }
                        IconButton(onClick = { showAddTaskDialog = true }) { Icon(Icons.Default.Assignment, null) }
                    }
                }
            }
            
            itemsIndexed(habits) { index, habit ->
                HabitEditItem(
                    habit = habit,
                    onDelete = { habits.removeAt(index) },
                    onMoveUp = if (index > 0) { { 
                        val h = habits.removeAt(index)
                        habits.add(index - 1, h)
                    } } else null,
                    onMoveDown = if (index < habits.size - 1) { { 
                        val h = habits.removeAt(index)
                        habits.add(index + 1, h)
                    } } else null
                )
            }

            item {
                if (tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Tasks", style = MaterialTheme.typography.titleMedium)
                }
            }

            itemsIndexed(tasks) { index, task ->
                TaskEditItem(
                    name = task,
                    onDelete = { tasks.removeAt(index) }
                )
            }

            item {
                if (blocks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Workout Blocks (OG/RRR)", style = MaterialTheme.typography.titleMedium)
                }
            }

            itemsIndexed(blocks) { index, block ->
                WorkoutBlockEditItem(
                    block = block,
                    onDelete = { blocks.removeAt(index) }
                )
            }
        }
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            habitDefinitions = habitDefinitions,
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { habit ->
                habits.add(habit.copy(orderIndex = habits.size))
                showAddHabitDialog = false
            }
        )
    }

    if (showAddBlockDialog) {
        AddWorkoutBlockDialog(
            onDismiss = { showAddBlockDialog = false },
            onConfirm = { block ->
                blocks.add(block.copy(block = block.block.copy(orderIndex = blocks.size)))
                showAddBlockDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        var taskName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Task") },
            text = {
                OutlinedTextField(value = taskName, onValueChange = { taskName = it }, label = { Text("Task Description") })
            },
            confirmButton = {
                Button(onClick = {
                    tasks.add(taskName)
                    showAddTaskDialog = false
                }, enabled = taskName.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TaskEditItem(name: String, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}

@Composable
fun WorkoutBlockEditItem(
    block: WorkoutBlockWithExercises,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(block.block.blockType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Block", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
            block.exercises.forEach { ex ->
                Text("• ${ex.name}: ${ex.sets}x${ex.targetReps}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutBlockDialog(
    onDismiss: () -> Unit,
    onConfirm: (WorkoutBlockWithExercises) -> Unit
) {
    var blockType by remember { mutableStateOf("SINGLE") }
    val exercises = remember { mutableStateListOf<WorkoutExercise>() }
    
    var showAddEx by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Workout Block") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Block Type", style = MaterialTheme.typography.titleSmall)
                Row {
                    listOf("SINGLE", "PAIR", "TRIPLET").forEach { type ->
                        FilterChip(
                            selected = blockType == type,
                            onClick = { blockType = type },
                            label = { Text(type) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Exercises", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showAddEx = true }) { Icon(Icons.Default.Add, null) }
                }
                
                exercises.forEachIndexed { index, ex ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${ex.name} (${ex.sets}x${ex.targetReps})", modifier = Modifier.weight(1f))
                        IconButton(onClick = { exercises.removeAt(index) }) { Icon(Icons.Default.Close, null) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(WorkoutBlockWithExercises(
                    block = WorkoutBlockEntity(gauntletId = 0, blockType = blockType, orderIndex = 0, restAfterBlock = 90),
                    exercises = exercises.toList()
                ))
            }, enabled = exercises.isNotEmpty()) { Text("Add Block") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showAddEx) {
        var name by remember { mutableStateOf("") }
        var sets by remember { mutableStateOf("3") }
        var reps by remember { mutableStateOf("5-8") }

        AlertDialog(
            onDismissRequest = { showAddEx = false },
            title = { Text("Add Exercise to Block") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Exercise Name") })
                    OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") })
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Target Reps (e.g. 5-8)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    exercises.add(WorkoutExercise(blockId = 0, name = name, sets = sets.toIntOrNull() ?: 3, targetReps = reps, restBetweenSets = 90, orderIndex = exercises.size))
                    showAddEx = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddEx = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun HabitEditItem(
    habit: Habit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.Bold)
                val trackingInfo = when (habit.trackingType) {
                    HabitTrackingType.TIME -> "Timed: ${habit.estimatedDurationSeconds}s"
                    HabitTrackingType.REPS -> "Reps: ${habit.estimatedDurationSeconds}"
                    HabitTrackingType.CHECKLIST -> "Checklist only"
                }
                Text(
                    text = trackingInfo,
                    style = MaterialTheme.typography.bodySmall
                )
                if (habit.mediaType != HabitMediaType.NONE) {
                    Text(
                        text = "Media: ${habit.mediaType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (habit.subHabits.isNotEmpty()) {
                    Text(
                        text = "${habit.subHabits.size} sub-tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            if (onMoveUp != null) {
                IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, "Up") }
            }
            if (onMoveDown != null) {
                IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, "Down") }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
        }
    }
}

@Composable
fun AddHabitDialog(
    habitDefinitions: List<HabitDefinition>,
    onDismiss: () -> Unit,
    onConfirm: (Habit) -> Unit
) {
    var showLibraryPicker by remember { mutableStateOf(true) }
    
    var hName by remember { mutableStateOf("") }
    var hDefinitionId by remember { mutableStateOf<Int?>(null) }
    var hDurationSecs by remember { mutableStateOf("60") }
    var hTrackingType by remember { mutableStateOf(HabitTrackingType.TIME) }
    var hMediaType by remember { mutableStateOf(HabitMediaType.NONE) }
    var hMediaUrl by remember { mutableStateOf("") }
    val subHabits = remember { mutableStateListOf<String>() }

    if (showLibraryPicker) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pick from Forge") },
            text = {
                if (habitDefinitions.isEmpty()) {
                    Text("Your Forge is empty. Create habits in the Profile -> Forge first!", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(habitDefinitions) { def ->
                            ListItem(
                                headlineContent = { Text(def.name) },
                                supportingContent = { Text("${def.trackingType} | ${def.mediaType}") },
                                modifier = Modifier.clickable {
                                    hName = def.name
                                    hDefinitionId = def.id
                                    hDurationSecs = (def.defaultEstimatedDurationSeconds ?: 60).toString()
                                    hTrackingType = def.trackingType
                                    hMediaType = def.mediaType
                                    hMediaUrl = def.mediaUrl ?: ""
                                    subHabits.clear()
                                    subHabits.addAll(def.defaultSubHabits)
                                    
                                    showLibraryPicker = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Configure: $hName") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (hTrackingType != HabitTrackingType.CHECKLIST) {
                        OutlinedTextField(
                            value = hDurationSecs,
                            onValueChange = { hDurationSecs = it },
                            label = { Text(if (hTrackingType == HabitTrackingType.TIME) "Duration (s)" else "Target Reps") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (subHabits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sub-tasks", style = MaterialTheme.typography.titleSmall)
                        subHabits.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        onConfirm(Habit(
                            gauntletId = 0,
                            name = hName,
                            orderIndex = 0,
                            estimatedDurationSeconds = hDurationSecs.toIntOrNull(),
                            subHabits = subHabits.toList(),
                            trackingType = hTrackingType,
                            mediaType = hMediaType,
                            mediaUrl = hMediaUrl,
                            definitionId = hDefinitionId
                        ))
                    }
                ) { Text("Add to Stack") }
            },
            dismissButton = {
                TextButton(onClick = { showLibraryPicker = true }) { Text("Back to Library") }
            }
        )
    }
}
