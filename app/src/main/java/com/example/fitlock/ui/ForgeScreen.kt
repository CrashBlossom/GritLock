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
import com.example.fitlock.exercise.ExerciseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeScreen(
    habitDefinitions: List<HabitDefinition>,
    onHabitUpsert: (HabitDefinition) -> Unit,
    onHabitDelete: (HabitDefinition) -> Unit,
    exerciseDefinitions: List<ExerciseDefinition>,
    onExerciseUpsert: (ExerciseDefinition) -> Unit,
    onExerciseDelete: (ExerciseDefinition) -> Unit,
    taskDefinitions: List<TaskDefinition>,
    onTaskUpsert: (TaskDefinition) -> Unit,
    onTaskDelete: (TaskDefinition) -> Unit,
    deckSummaries: List<DeckSummary>,
    onDeleteDeck: (String) -> Unit,
    onExerciseLog: (ExerciseType, Int, String?, Int, String?) -> Unit, // type, reps, familyId, level, variantName
    onDeckPlay: (String) -> Unit,
    onProgressionClick: () -> Unit,
    allFamilies: List<FamilyWithLevels>,
    currentProgression: Map<String, Int>,
    onAddFlashcard: (com.example.fitlock.data.Flashcard) -> Unit,
    onAiImport: (String) -> Unit,
    title: String = "Forge",
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Habits", "Exercises", "Tasks", "Mental")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    var showAiDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, "AI Import", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (showAiDialog) {
                        AiImportDialog(
                            onDismiss = { showAiDialog = false },
                            onConfirm = { text ->
                                onAiImport(text)
                                showAiDialog = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> HabitLibraryContent(habitDefinitions, onHabitUpsert, onHabitDelete)
                1 -> ExerciseLibraryContent(exerciseDefinitions, onExerciseUpsert, onExerciseDelete, onExerciseLog, onProgressionClick, allFamilies, currentProgression)
                2 -> TaskLibraryContent(taskDefinitions, onTaskUpsert, onTaskDelete)
                3 -> MentalLibraryContent(deckSummaries, onDeleteDeck, onDeckPlay, onAddFlashcard)
            }
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
                Text("Your library is empty. Add your first master habit!")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(definitions) { def ->
                    ForgeItemCard(
                        title = def.name,
                        subtitle = "${def.trackingType} | ${def.mediaType}",
                        icon = when(def.trackingType) {
                            HabitTrackingType.TIME -> Icons.Default.Timer
                            HabitTrackingType.REPS -> Icons.Default.FitnessCenter
                            HabitTrackingType.CHECKLIST -> Icons.Default.CheckBox
                        },
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
fun EditHabitDefinitionDialog(
    definition: HabitDefinition,
    onDismiss: () -> Unit,
    onConfirm: (HabitDefinition) -> Unit
) {
    var name by remember { mutableStateOf(definition.name) }
    var trackingType by remember { mutableStateOf(definition.trackingType) }
    var mediaType by remember { mutableStateOf(definition.mediaType) }
    var mediaUrl by remember { mutableStateOf(definition.mediaUrl ?: "") }
    var duration by remember { mutableStateOf(definition.defaultEstimatedDurationSeconds?.toString() ?: "60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (definition.id == 0) "New Master Habit" else "Edit Master Habit") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Habit Name") })
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tracking Type", style = MaterialTheme.typography.titleSmall)
                Row {
                    HabitTrackingType.entries.forEach { type ->
                        FilterChip(
                            selected = trackingType == type,
                            onClick = { trackingType = type },
                            label = { Text(type.name) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                if (trackingType != HabitTrackingType.CHECKLIST) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text(if (trackingType == HabitTrackingType.TIME) "Default Seconds" else "Default Reps") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Media Type", style = MaterialTheme.typography.titleSmall)
                Row {
                    HabitMediaType.entries.forEach { type ->
                        FilterChip(
                            selected = mediaType == type,
                            onClick = { mediaType = type },
                            label = { Text(type.name) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                
                if (mediaType != HabitMediaType.NONE) {
                    OutlinedTextField(value = mediaUrl, onValueChange = { mediaUrl = it }, label = { Text("Media URL") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(definition.copy(
                    name = name,
                    trackingType = trackingType,
                    mediaType = mediaType,
                    mediaUrl = if (mediaUrl.isBlank()) null else mediaUrl,
                    defaultEstimatedDurationSeconds = duration.toIntOrNull()
                ))
            }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExerciseLibraryContent(
    definitions: List<ExerciseDefinition>,
    onUpsert: (ExerciseDefinition) -> Unit,
    onDelete: (ExerciseDefinition) -> Unit,
    onLog: (ExerciseType, Int, String?, Int, String?) -> Unit,
    onProgressionClick: () -> Unit,
    allFamilies: List<FamilyWithLevels>,
    currentProgression: Map<String, Int>
) {
    var showEditDialog by remember { mutableStateOf<ExerciseDefinition?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Skills & Progressions", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onProgressionClick) {
                        Icon(Icons.Default.AccountTree, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Skill Tree")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(allFamilies) { family ->
                val level = currentProgression[family.family.id] ?: 1
                val variant = family.levels.find { it.level == level } ?: family.levels.firstOrNull()
                
                variant?.let { v ->
                    ForgeItemCard(
                        title = v.variantName,
                        subtitle = "Level $level ${family.family.name}",
                        icon = Icons.Default.TrendingUp,
                        onClick = { 
                            try {
                                onLog(ExerciseType.valueOf(v.exerciseType), 10, family.family.id, level, v.variantName)
                            } catch(e: Exception) {
                                onLog(ExerciseType.SQUAT, 10, null, 1, null)
                            }
                        },
                        onDelete = null,
                        actionIcon = Icons.Default.PlayArrow
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Standard Exercises", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(ExerciseType.entries.filter { it != ExerciseType.APP_USAGE && it != ExerciseType.FLASHCARDS && it != ExerciseType.STEPS }) { type ->
                ForgeItemCard(
                    title = type.name,
                    subtitle = "Standard AI Tracked",
                    icon = Icons.Default.FitnessCenter,
                    onClick = { onLog(type, 10, null, 1, null) },
                    onDelete = null, // Can't delete standard
                    actionIcon = Icons.Default.PlayArrow
                )
            }

            if (definitions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Custom Definitions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(definitions) { def ->
                    ForgeItemCard(
                        title = def.name,
                        subtitle = "${def.defaultSets} sets x ${def.defaultTargetReps} ${def.type}",
                        icon = Icons.Default.FitnessCenter,
                        onClick = { 
                            try {
                                onLog(ExerciseType.valueOf(def.type), def.defaultTargetReps.toIntOrNull() ?: 10, null, 1, null)
                            } catch(e: Exception) {
                                onLog(ExerciseType.SQUAT, 10, null, 1, null)
                            }
                        },
                        onDelete = { onDelete(def) },
                        onEdit = { 
                            showEditDialog = def
                        },
                        actionIcon = Icons.Default.PlayArrow
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { showEditDialog = ExerciseDefinition(name = "", type = ExerciseType.SQUAT.name) },
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
fun TaskLibraryContent(
    definitions: List<TaskDefinition>,
    onUpsert: (TaskDefinition) -> Unit,
    onDelete: (TaskDefinition) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<TaskDefinition?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (definitions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No task templates saved.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(definitions) { def ->
                    ForgeItemCard(
                        title = def.name,
                        subtitle = def.category ?: "General",
                        icon = Icons.Default.Assignment,
                        onClick = { showEditDialog = def },
                        onDelete = { onDelete(def) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showEditDialog = TaskDefinition(name = "") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Task Template")
        }
    }

    showEditDialog?.let { def ->
        EditTaskDefinitionDialog(
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
fun MentalLibraryContent(
    deckSummaries: List<DeckSummary>,
    onDeleteDeck: (String) -> Unit,
    onPlay: (String) -> Unit,
    onAddFlashcard: (com.example.fitlock.data.Flashcard) -> Unit
) {
    var deckToDelete by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Flashcard Decks", style = MaterialTheme.typography.titleMedium)
                Text("Add manual cards or import via Home. Delete them here.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(deckSummaries) { deck ->
                ForgeItemCard(
                    title = deck.name,
                    subtitle = "${deck.totalCards} cards (${deck.cardsDue} due)",
                    icon = Icons.Default.Style,
                    onClick = { onPlay(deck.name) },
                    onDelete = { deckToDelete = deck.name },
                    actionIcon = Icons.Default.PlayArrow
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Card")
        }
    }

    if (showAddDialog) {
        AddFlashcardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { card ->
                onAddFlashcard(card)
                showAddDialog = false
            }
        )
    }

    if (deckToDelete != null) {
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Delete Deck") },
            text = { Text("Are you sure you want to delete all cards in '${deckToDelete}'?") },
            confirmButton = {
                Button(onClick = { 
                    onDeleteDeck(deckToDelete!!)
                    deckToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { deckToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AiImportDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Forge Extraction") },
        text = {
            Column {
                Text("Paste a routine, goal list, or project plan. Gemini will parse it into your Forge and Atlas.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    label = { Text("Paste Text Here") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Forge with AI")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddFlashcardDialog(onDismiss: () -> Unit, onConfirm: (com.example.fitlock.data.Flashcard) -> Unit) {
    var deckName by remember { mutableStateOf("") }
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Flashcard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = deckName, onValueChange = { deckName = it }, label = { Text("Deck Name") })
                OutlinedTextField(value = front, onValueChange = { front = it }, label = { Text("Front Text") })
                OutlinedTextField(value = back, onValueChange = { back = it }, label = { Text("Back Text") })
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(com.example.fitlock.data.Flashcard(deckName = deckName, frontText = front, backText = back))
            }, enabled = deckName.isNotBlank() && front.isNotBlank() && back.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ForgeItemCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
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
    var type by remember { mutableStateOf(ExerciseType.valueOf(definition.type)) }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditTaskDefinitionDialog(
    definition: TaskDefinition,
    onDismiss: () -> Unit,
    onConfirm: (TaskDefinition) -> Unit
) {
    var name by remember { mutableStateOf(definition.name) }
    var category by remember { mutableStateOf(definition.category ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (definition.id == 0) "New Task Template" else "Edit Task Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Optional)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(definition.copy(name = name, category = category.ifBlank { null }))
            }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
