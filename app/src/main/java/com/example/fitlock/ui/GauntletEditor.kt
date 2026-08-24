package com.example.fitlock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
    groups: List<AppGroup>,
    onSave: (Gauntlet, List<Habit>) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(editingGauntlet?.gauntlet?.name ?: "") }
    var triggerType by remember { mutableStateOf(editingGauntlet?.gauntlet?.triggerType ?: GauntletTriggerType.MANUAL) }
    var triggerTime by remember { mutableStateOf(editingGauntlet?.gauntlet?.triggerTime ?: "08:00") }
    var bufferSeconds by remember { mutableIntStateOf(editingGauntlet?.gauntlet?.bufferSeconds ?: 10) }
    var targetBlockGroupId by remember { mutableStateOf(editingGauntlet?.gauntlet?.targetBlockGroupId) }
    var icon by remember { mutableStateOf<String?>(editingGauntlet?.gauntlet?.icon) }
    var physicalTriggerType by remember { mutableStateOf(editingGauntlet?.gauntlet?.physicalTriggerType ?: PhysicalTriggerType.NONE) }
    var physicalTriggerData by remember { mutableStateOf(editingGauntlet?.gauntlet?.physicalTriggerData) }
    var delayedNudgeMinutes by remember { mutableIntStateOf(editingGauntlet?.gauntlet?.delayedNudgeMinutes ?: 5) }
    
    val habits = remember { 
        mutableStateListOf<Habit>().apply {
            editingGauntlet?.habits?.sortedBy { it.orderIndex }?.let { addAll(it) }
        }
    }

    var showAddHabitDialog by remember { mutableStateOf(false) }

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
                            onSave(g, habits.toList())
                        },
                        enabled = name.isNotBlank() && habits.isNotEmpty()
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
                    Button(onClick = { showAddHabitDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Habit")
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
        }
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { habit ->
                habits.add(habit.copy(orderIndex = habits.size))
                showAddHabitDialog = false
            }
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
                Text(
                    text = if (habit.estimatedDurationSeconds != null) 
                        "Estimated: ${habit.estimatedDurationSeconds}s" else "No time estimation",
                    style = MaterialTheme.typography.bodySmall
                )
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
    onDismiss: () -> Unit,
    onConfirm: (Habit) -> Unit
) {
    var hName by remember { mutableStateOf("") }
    var hDurationSecs by remember { mutableStateOf("60") }
    var subHabitText by remember { mutableStateOf("") }
    val subHabits = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Habit") },
        text = {
            Column {
                OutlinedTextField(
                    value = hName,
                    onValueChange = { hName = it },
                    label = { Text("Habit Name") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = hDurationSecs,
                    onValueChange = { hDurationSecs = it },
                    label = { Text("Estimated Duration (seconds)") },
                    placeholder = { Text("Optional") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Sub-tasks (Checklist)", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = subHabitText,
                        onValueChange = { subHabitText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. Put on shoes") }
                    )
                    IconButton(onClick = {
                        if (subHabitText.isNotBlank()) {
                            subHabits.add(subHabitText)
                            subHabitText = ""
                        }
                    }) { Icon(Icons.Default.Add, null) }
                }
                subHabits.forEachIndexed { index, sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• $sub", modifier = Modifier.weight(1f))
                        IconButton(onClick = { subHabits.removeAt(index) }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(Habit(
                        gauntletId = 0, // Set by editor on save
                        name = hName,
                        orderIndex = 0,
                        estimatedDurationSeconds = hDurationSecs.toIntOrNull(),
                        subHabits = subHabits.toList()
                    ))
                },
                enabled = hName.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
