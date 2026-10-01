package com.example.fitlock.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitualWizard(
    isMorning: Boolean,
    pledge: DailyPledge,
    dailyQuest: QuestWithBlocks? = null,
    onSave: (DailyPledge, List<QuestBlock>, List<String>) -> Unit, // updatedBlocks, newIdeas
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = if (isMorning) 6 else 8

    // State for all fields
    var mornReflection by remember { mutableStateOf(pledge.morning.lastNightReflection ?: "") }
    var mornDreams by remember { mutableStateOf(pledge.morning.dreams ?: "") }
    var mornPersonal by remember { mutableStateOf(pledge.morning.personalGoal ?: "") }
    var mornWork by remember { mutableStateOf(pledge.morning.workGoal ?: "") }
    var mornObstacles by remember { mutableStateOf(pledge.morning.obstacles ?: "") }
    var mornSolutions by remember { mutableStateOf(pledge.morning.solutions ?: "") }
    var mornGratitude by remember { mutableStateOf(pledge.morning.gratitude ?: "") }
    var mornAwe by remember { mutableStateOf(pledge.morning.awe ?: "") }
    var mornReadLearn by remember { mutableStateOf(pledge.morning.readListen ?: "") }
    val mornIdeas = remember { mutableStateListOf<String>().apply { addAll(pledge.morning.morningIdeas) } }

    var evenStory by remember { mutableStateOf(pledge.evening.story ?: "") }
    var evenAccomplishments by remember { mutableStateOf(pledge.evening.accomplishments ?: "") }
    val evenWins = remember { mutableStateListOf<String>().apply { addAll(pledge.evening.wins) } }
    val evenLosses = remember { mutableStateListOf<String>().apply { addAll(pledge.evening.losses) } }
    var evenMood by remember { mutableFloatStateOf(pledge.evening.mood?.toFloat() ?: 5f) }
    var evenEnergy by remember { mutableFloatStateOf(pledge.evening.energy?.toFloat() ?: 5f) }
    var evenPeaceOfMind by remember { mutableStateOf(pledge.evening.peaceOfMindGoal ?: "") }
    var evenMeals by remember { mutableStateOf(pledge.evening.meals ?: "") }
    val evenIdeas = remember { mutableStateListOf<String>().apply { addAll(pledge.evening.eveningIdeas) } }

    // Quest Review State (Local copies of blocks to modify)
    val questBlocks = remember(dailyQuest) { 
        mutableStateListOf<QuestBlock>().apply { 
            dailyQuest?.blocks?.let { addAll(it) } 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMorning) "Morning Plan" else "Evening Review") },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep > 0) currentStep-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    Text("${currentStep + 1} / $totalSteps", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge)
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next Step")
                        }
                    } else {
                        Button(
                            onClick = {
                                val newIdeas = mutableListOf<String>()
                                val updatedPledge = if (isMorning) {
                                    newIdeas.addAll(mornIdeas)
                                    pledge.copy(
                                        status = "COMMITTED",
                                        pledgeTimestamp = System.currentTimeMillis(),
                                        morning = MorningRitual(
                                            lastNightReflection = mornReflection,
                                            dreams = mornDreams,
                                            personalGoal = mornPersonal,
                                            workGoal = mornWork,
                                            obstacles = mornObstacles,
                                            solutions = mornSolutions,
                                            gratitude = mornGratitude,
                                            awe = mornAwe,
                                            morningIdeas = mornIdeas.toList(),
                                            readListen = mornReadLearn
                                        )
                                    )
                                } else {
                                    newIdeas.addAll(evenIdeas)
                                    pledge.copy(
                                        status = "SUCCESS", 
                                        reviewTimestamp = System.currentTimeMillis(),
                                        evening = EveningRitual(
                                            story = evenStory,
                                            accomplishments = evenAccomplishments,
                                            wins = evenWins.toList(),
                                            losses = evenLosses.toList(),
                                            mood = evenMood.toInt(),
                                            energy = evenEnergy.toInt(),
                                            peaceOfMindGoal = evenPeaceOfMind,
                                            eveningIdeas = evenIdeas.toList(),
                                            meals = evenMeals
                                        )
                                    )
                                }
                                onSave(updatedPledge, questBlocks.toList(), newIdeas)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Finish Ritual")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isMorning) {
                when (currentStep) {
                    0 -> StepContent("Review & Recaps", "Reflect on last night.") {
                        RitualTextField("After Dinner Recap", mornReflection) { mornReflection = it }
                        RitualTextField("Dreams / Night Thoughts", mornDreams) { mornDreams = it }
                    }
                    1 -> StepContent("Intentions", "Define your victories.") {
                        RitualTextField("Personal Objective", mornPersonal) { mornPersonal = it }
                        RitualTextField("Work Objective", mornWork) { mornWork = it }
                    }
                    2 -> StepContent("Anticipation", "Prepare for resistance.") {
                        RitualTextField("Potential Obstacles", mornObstacles) { mornObstacles = it }
                        RitualTextField("My Solutions", mornSolutions) { mornSolutions = it }
                    }
                    3 -> StepContent("Spirit", "Grit through gratitude.") {
                        RitualTextField("What am I grateful for?", mornGratitude) { mornGratitude = it }
                        RitualTextField("What am I in awe of?", mornAwe) { mornAwe = it }
                    }
                    4 -> StepContent("Growth", "Continuous learning.") {
                        RitualTextField("Read/Listen To", mornReadLearn) { mornReadLearn = it }
                    }
                    5 -> StepContent("Ideas & Solutions", "Brainstorm for the future.") {
                        Text("New Ideas", style = MaterialTheme.typography.titleSmall)
                        ListEditor(mornIdeas)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ideas captured here will be saved to your Brainstorming backlog.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                when (currentStep) {
                    0 -> StepContent("The Story", "What happened today?") {
                        RitualTextField("Daily Narrative", evenStory) { evenStory = it }
                    }
                    1 -> StepContent("Inventory", "Own your wins.") {
                        RitualTextField("Main Accomplishments", evenAccomplishments) { evenAccomplishments = it }
                    }
                    2 -> StepContent("Analysis", "3 Wins and 3 Losses.") {
                        Text("Top 3 Wins", style = MaterialTheme.typography.titleSmall)
                        // Simple list editor
                        ListEditor(evenWins)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Top 3 Losses", style = MaterialTheme.typography.titleSmall)
                        ListEditor(evenLosses)
                    }
                    3 -> StepContent("Bio-Metrics", "Check your stats.") {
                        Text("Mood: ${evenMood.toInt()}/10")
                        Slider(value = evenMood, onValueChange = { evenMood = it }, valueRange = 1f..10f, steps = 8)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Energy: ${evenEnergy.toInt()}/10")
                        Slider(value = evenEnergy, onValueChange = { evenEnergy = it }, valueRange = 1f..10f, steps = 8)
                    }
                    4 -> StepContent("Peace of Mind", "Setup tomorrow.") {
                        RitualTextField("One thing for peace of mind", evenPeaceOfMind) { evenPeaceOfMind = it }
                    }
                    5 -> StepContent("Fuel", "Log your meals.") {
                        RitualTextField("Breakfast, Lunch, Dinner", evenMeals) { evenMeals = it }
                    }
                    6 -> StepContent("Evening Ideas", "Don't lose your insights.") {
                        ListEditor(evenIdeas)
                        Text("Captured ideas go to your Brainstorming project.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    7 -> StepContent("Quest Review", "How did you perform?") {
                        if (questBlocks.isEmpty()) {
                            Text("No active quest found for today.", color = Color.Gray)
                        } else {
                            questBlocks.forEachIndexed { index, block ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(block.name, fontWeight = FontWeight.Bold)
                                    if (block.type == BlockType.TASK) {
                                        TaskProgressSelector(
                                            currentStatus = block.workStatus,
                                            onStatusChange = { newStatus ->
                                                questBlocks[index] = block.copy(
                                                    workStatus = newStatus,
                                                    isCompleted = newStatus == "COMPLETED",
                                                    completionTimestamp = if (newStatus == "COMPLETED") System.currentTimeMillis() else null
                                                )
                                            }
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = block.isCompleted,
                                                onCheckedChange = { checked ->
                                                    questBlocks[index] = block.copy(
                                                        isCompleted = checked,
                                                        completionTimestamp = if (checked) System.currentTimeMillis() else null,
                                                        workStatus = if (checked) "COMPLETED" else "NO_PROGRESS"
                                                    )
                                                }
                                            )
                                            Text(if (block.isCompleted) "Done" else "Not Done")
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskProgressSelector(currentStatus: String, onStatusChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf(
            "COMPLETED" to "Done",
            "WORKED_ON" to "Worked",
            "NO_PROGRESS" to "Skip"
        )
        options.forEach { (valStr, label) ->
            FilterChip(
                selected = currentStatus == valStr,
                onClick = { onStatusChange(valStr) },
                label = { Text(label, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
fun StepContent(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}

@Composable
fun RitualTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        minLines = 3
    )
}

@Composable
fun ListEditor(items: MutableList<String>) {
    var newItem by remember { mutableStateOf("") }
    Column {
        items.forEachIndexed { index, s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("• $s", modifier = Modifier.weight(1f))
                IconButton(onClick = { items.removeAt(index) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newItem, onValueChange = { newItem = it }, placeholder = { Text("Add item...") }, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (newItem.isNotBlank()) { items.add(newItem); newItem = "" } }) {
                Icon(Icons.Default.Add, null)
            }
        }
    }
}
