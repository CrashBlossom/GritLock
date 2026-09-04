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
    onSave: (DailyPledge) -> Unit,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = if (isMorning) 5 else 6

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

    var evenStory by remember { mutableStateOf(pledge.evening.story ?: "") }
    var evenAccomplishments by remember { mutableStateOf(pledge.evening.accomplishments ?: "") }
    val evenWins = remember { mutableStateListOf<String>().apply { addAll(pledge.evening.wins) } }
    val evenLosses = remember { mutableStateListOf<String>().apply { addAll(pledge.evening.losses) } }
    var evenMood by remember { mutableFloatStateOf(pledge.evening.mood?.toFloat() ?: 5f) }
    var evenEnergy by remember { mutableFloatStateOf(pledge.evening.energy?.toFloat() ?: 5f) }
    var evenPeaceOfMind by remember { mutableStateOf(pledge.evening.peaceOfMindGoal ?: "") }
    var evenMeals by remember { mutableStateOf(pledge.evening.meals ?: "") }

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
                                val updatedPledge = if (isMorning) {
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
                                            readListen = mornReadLearn
                                        )
                                    )
                                } else {
                                    pledge.copy(
                                        status = "SUCCESS", // Default to success, let user choose later if needed
                                        reviewTimestamp = System.currentTimeMillis(),
                                        evening = EveningRitual(
                                            story = evenStory,
                                            accomplishments = evenAccomplishments,
                                            wins = evenWins.toList(),
                                            losses = evenLosses.toList(),
                                            mood = evenMood.toInt(),
                                            energy = evenEnergy.toInt(),
                                            peaceOfMindGoal = evenPeaceOfMind,
                                            meals = evenMeals
                                        )
                                    )
                                }
                                onSave(updatedPledge)
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
                }
            }
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
