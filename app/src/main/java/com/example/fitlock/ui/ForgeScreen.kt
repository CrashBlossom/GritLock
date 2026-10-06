package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeScreen(
    habitDefinitions: List<HabitDefinition>,
    onHabitUpsert: (HabitDefinition) -> Unit,
    onHabitDelete: (HabitDefinition) -> Unit,
    exerciseDefinitions: List<ExerciseDefinition>,
    onExerciseUpsert: (ExerciseDefinition) -> Unit,
    onExerciseDelete: (ExerciseDefinition) -> Unit,
    taskDefinitions: List<TaskDefinition> = emptyList(),
    onTaskUpsert: (TaskDefinition) -> Unit = {},
    onTaskDelete: (TaskDefinition) -> Unit = {},
    quests: List<QuestWithBlocks> = emptyList(),
    onUpsertQuest: (Quest) -> Unit = {},
    onDeleteQuest: (Quest) -> Unit = {},
    onUpsertQuestBlock: (QuestBlock) -> Unit = {},
    onDeleteQuestBlock: (QuestBlock) -> Unit = {},
    unfinishedTasks: List<QuestBlock> = emptyList(),
    brainstormingTasks: List<ProjectTask> = emptyList(),
    deckSummaries: List<DeckSummary>,
    onDeleteDeck: (String) -> Unit,
    onExerciseLog: (ExerciseType, Int, String?, Int, String?) -> Unit, // type, reps, familyId, level, variantName
    onDeckPlay: (String) -> Unit,
    onProgressionClick: () -> Unit,
    onStartPlanning: () -> Unit = {},
    allFamilies: List<FamilyWithLevels>,
    currentProgression: Map<String, Int>,
    onAddFlashcard: (Flashcard) -> Unit,
    title: String = "The Forge (Training)",
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Habit Quests", "Routines & Stacks", "Physical Exercises", "Mental (INT)")

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
                1 -> RoutineStacksContent(quests, unfinishedTasks, brainstormingTasks, onStartPlanning, onUpsertQuest, onDeleteQuest, onUpsertQuestBlock, onDeleteQuestBlock)
                2 -> ExerciseLibraryContent(exerciseDefinitions, onExerciseUpsert, onExerciseDelete, onExerciseLog, onProgressionClick, allFamilies, currentProgression)
                3 -> MentalLibraryContent(deckSummaries, onDeleteDeck, onDeckPlay, onAddFlashcard)
            }
        }
    }
}

@Composable
fun RoutineStacksContent(
    quests: List<QuestWithBlocks>,
    unfinishedTasks: List<QuestBlock>,
    brainstormingTasks: List<ProjectTask>,
    onStartPlanning: () -> Unit,
    onUpsertQuest: (Quest) -> Unit,
    onDeleteQuest: (Quest) -> Unit,
    onUpsertQuestBlock: (QuestBlock) -> Unit,
    onDeleteQuestBlock: (QuestBlock) -> Unit
) {
    var showEditQuestDialog by remember { mutableStateOf<Quest?>(null) }
    var managingBlocksQuest by remember { mutableStateOf<QuestWithBlocks?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("PARENT HABIT STACKS & ROUTINES", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Manage automated routines with Time & WiFi triggers, Lock Shield Firewalls, and structured steps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onStartPlanning,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Build, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Interactive Morning/Evening Wizard")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("CONFIGURED ROUTINE STACKS", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (quests.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("No routines found. Tap '+' below to build your first routine stack!", modifier = Modifier.padding(16.dp), color = Color.Gray)
                    }
                }
            } else {
                items(quests) { q ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { managingBlocksQuest = q }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(q.quest.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row {
                                    IconButton(onClick = { showEditQuestDialog = q.quest }) {
                                        Icon(Icons.Default.Edit, "Edit Routine", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onDeleteQuest(q.quest) }) {
                                        Icon(Icons.Default.Delete, "Delete Routine", tint = Color.Red)
                                    }
                                }
                            }
                            Text(
                                "Trigger: ${q.quest.triggerType} (${q.quest.triggerTime ?: "Anytime"}) | WiFi: ${q.quest.physicalTriggerData ?: "Any"} | Lock Shield: ${if (q.quest.isLockShieldEnabled) "ENABLED" else "DISABLED"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (q.blocks.isEmpty()) {
                                Text("No steps added yet. Tap to manage components.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB74D))
                            } else {
                                q.blocks.forEach { block ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircleOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${block.name} (${block.type.name})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { showEditQuestDialog = Quest(name = "New Routine Stack", type = QuestType.ROUTINE) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Routine")
        }
    }

    showEditQuestDialog?.let { quest ->
        EditRoutineDialog(
            quest = quest,
            onDismiss = { showEditQuestDialog = null },
            onConfirm = { updated ->
                onUpsertQuest(updated)
                showEditQuestDialog = null
            }
        )
    }

    managingBlocksQuest?.let { q ->
        ManageRoutineStepsDialog(
            questWithBlocks = q,
            unfinishedTasks = unfinishedTasks,
            brainstormingTasks = brainstormingTasks,
            onDismiss = { managingBlocksQuest = null },
            onAddBlock = { block -> onUpsertQuestBlock(block) },
            onDeleteBlock = { block -> onDeleteQuestBlock(block) }
        )
    }
}

@Composable
fun EditRoutineDialog(
    quest: Quest,
    onDismiss: () -> Unit,
    onConfirm: (Quest) -> Unit
) {
    var name by remember { mutableStateOf(quest.name) }
    var triggerType by remember { mutableStateOf(quest.triggerType) }
    var triggerTime by remember { mutableStateOf(quest.triggerTime ?: "09:00") }
    var wifiSsid by remember { mutableStateOf(quest.physicalTriggerData ?: "") }
    var isLockShield by remember { mutableStateOf(quest.isLockShieldEnabled) }
    var xpReward by remember { mutableStateOf(quest.xpReward.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Routine Stack & Triggers") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Routine Name") }, modifier = Modifier.fillMaxWidth())
                
                Text("Trigger Condition", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(GauntletTriggerType.MANUAL, GauntletTriggerType.TIME_BASED, GauntletTriggerType.WIFI, GauntletTriggerType.TIME_AND_WIFI).forEach { type ->
                        FilterChip(
                            selected = triggerType == type,
                            onClick = { triggerType = type },
                            label = { Text(type.name.replace("_", " ")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (triggerType == GauntletTriggerType.TIME_BASED || triggerType == GauntletTriggerType.TIME_AND_WIFI) {
                    OutlinedTextField(value = triggerTime, onValueChange = { triggerTime = it }, label = { Text("Trigger Time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                }

                if (triggerType == GauntletTriggerType.WIFI || triggerType == GauntletTriggerType.TIME_AND_WIFI) {
                    OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it }, label = { Text("Home WiFi SSID") }, modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(value = xpReward, onValueChange = { xpReward = it }, label = { Text("XP Reward") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Checkbox(checked = isLockShield, onCheckedChange = { isLockShield = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lock Shield Firewall (Strict App Block until cleared)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(quest.copy(
                    name = name,
                    triggerType = triggerType,
                    triggerTime = if (triggerType == GauntletTriggerType.TIME_BASED || triggerType == GauntletTriggerType.TIME_AND_WIFI) triggerTime.ifBlank { null } else null,
                    physicalTriggerData = if (triggerType == GauntletTriggerType.WIFI || triggerType == GauntletTriggerType.TIME_AND_WIFI) wifiSsid.ifBlank { null } else null,
                    isLockShieldEnabled = isLockShield,
                    xpReward = xpReward.toIntOrNull() ?: 10
                ))
            }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ManageRoutineStepsDialog(
    questWithBlocks: QuestWithBlocks,
    unfinishedTasks: List<QuestBlock>,
    brainstormingTasks: List<ProjectTask>,
    onDismiss: () -> Unit,
    onAddBlock: (QuestBlock) -> Unit,
    onDeleteBlock: (QuestBlock) -> Unit
) {
    var showAddStepDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Steps for '${questWithBlocks.quest.name}'") },
        text = {
            Column(modifier = Modifier.heightIn(max = 350.dp)) {
                if (questWithBlocks.blocks.isEmpty()) {
                    Text("No steps in this routine yet.", color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(questWithBlocks.blocks) { block ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(block.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Type: ${block.type}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { onDeleteBlock(block) }) {
                                        Icon(Icons.Default.Delete, "Delete Step", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showAddStepDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Routine Component")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    if (showAddStepDialog) {
        AddRoutineStepDialog(
            questId = questWithBlocks.quest.id,
            unfinishedTasks = unfinishedTasks,
            brainstormingTasks = brainstormingTasks,
            onDismiss = { showAddStepDialog = false },
            onConfirm = { block ->
                onAddBlock(block)
                showAddStepDialog = false
            }
        )
    }
}

@Composable
fun AddRoutineStepDialog(
    questId: String,
    unfinishedTasks: List<QuestBlock>,
    brainstormingTasks: List<ProjectTask>,
    onDismiss: () -> Unit,
    onConfirm: (QuestBlock) -> Unit
) {
    var componentCategory by remember { mutableStateOf("EXERCISE") } // EXERCISE, TODO, WATER, FLASHCARD
    var selectedExerciseType by remember { mutableStateOf(ExerciseType.PUSHUP) }
    var targetReps by remember { mutableStateOf("15") }
    var selectedTodoTitle by remember { mutableStateOf("") }
    var customTaskName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Structured Routine Component") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Component Category", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("EXERCISE", "TODO", "WATER", "FLASHCARD").forEach { cat ->
                        FilterChip(
                            selected = componentCategory == cat,
                            onClick = { componentCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }

                when (componentCategory) {
                    "EXERCISE" -> {
                        Text("Select Exercise Type", style = MaterialTheme.typography.bodySmall)
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Exercise: ${selectedExerciseType.name}")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                ExerciseType.entries.filter { it != ExerciseType.APP_USAGE && it != ExerciseType.FLASHCARDS && it != ExerciseType.STEPS }.forEach { type ->
                                    DropdownMenuItem(text = { Text(type.name) }, onClick = { selectedExerciseType = type; expanded = false })
                                }
                            }
                        }
                        OutlinedTextField(value = targetReps, onValueChange = { targetReps = it }, label = { Text("Target Reps / Duration (secs)") }, modifier = Modifier.fillMaxWidth())
                    }
                    "TODO" -> {
                        Text("Pull from Existing To-Do / Backlog", style = MaterialTheme.typography.bodySmall)
                        val allAvailableTasks = unfinishedTasks.map { it.name } + brainstormingTasks.map { it.title }
                        if (allAvailableTasks.isEmpty()) {
                            OutlinedTextField(value = customTaskName, onValueChange = { customTaskName = it }, label = { Text("Task Name") }, modifier = Modifier.fillMaxWidth())
                        } else {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(selectedTodoTitle.ifBlank { "Select existing task..." })
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    allAvailableTasks.forEach { title ->
                                        DropdownMenuItem(text = { Text(title) }, onClick = { selectedTodoTitle = title; expanded = false })
                                    }
                                }
                            }
                            OutlinedTextField(value = selectedTodoTitle, onValueChange = { selectedTodoTitle = it }, label = { Text("Or Type Custom Task") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    "WATER" -> {
                        Text("Hydration Step: Drink 250ml Water (Logs to Health Connect automatically)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64B5F6))
                    }
                    "FLASHCARD" -> {
                        Text("Cognitive Step: Anki Spaced Repetition Flashcard Review", style = MaterialTheme.typography.bodySmall, color = Color(0xFF81C784))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val blockName = when (componentCategory) {
                    "EXERCISE" -> "$targetReps ${selectedExerciseType.name.lowercase()}"
                    "TODO" -> selectedTodoTitle.ifBlank { customTaskName.ifBlank { "To-Do Task" } }
                    "WATER" -> "Drink 250ml Water"
                    "FLASHCARD" -> "Review Anki Flashcards"
                    else -> "Routine Step"
                }
                val blockType = when (componentCategory) {
                    "EXERCISE" -> BlockType.EXERCISE
                    "WATER" -> BlockType.ACTION
                    "FLASHCARD" -> BlockType.MENTAL
                    else -> BlockType.TASK
                }

                onConfirm(QuestBlock(
                    id = "block_${UUID.randomUUID()}",
                    questId = questId,
                    type = blockType,
                    orderIndex = 99,
                    name = blockName,
                    targetReps = if (componentCategory == "EXERCISE") targetReps else null,
                    actionType = if (componentCategory == "WATER") "LOG_WATER" else null
                ))
            }, enabled = true) { Text("Add Step") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
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
    onAddFlashcard: (Flashcard) -> Unit
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
fun AddFlashcardDialog(onDismiss: () -> Unit, onConfirm: (Flashcard) -> Unit) {
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
                onConfirm(Flashcard(deckName = deckName, frontText = front, backText = back))
            }, enabled = deckName.isNotBlank() && front.isNotBlank() && back.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
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
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
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
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
