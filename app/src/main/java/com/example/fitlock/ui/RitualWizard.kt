package com.example.fitlock.ui

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    val mornMinChars = remember { prefs.getInt("morning_template_min_words", 5) }
    val evenMinChars = remember { prefs.getInt("evening_template_min_words", 5) }
    val activeMinChars = if (isMorning) mornMinChars else evenMinChars

    // Dynamic Morning Section Titles & Field Labels from Settings
    val sec0Title = remember { prefs.getString("morning_sec_0_title", "Review & Recaps") ?: "Review & Recaps" }
    val sec0Sub = remember { prefs.getString("morning_sec_0_sub", "Reflect on last night.") ?: "Reflect on last night." }
    val sec0F1 = remember { prefs.getString("morning_sec_0_f1", "After Dinner Recap") ?: "After Dinner Recap" }
    val sec0F2 = remember { prefs.getString("morning_sec_0_f2", "Dreams / Night Thoughts") ?: "Dreams / Night Thoughts" }

    val sec1Title = remember { prefs.getString("morning_sec_1_title", "Intentions") ?: "Intentions" }
    val sec1Sub = remember { prefs.getString("morning_sec_1_sub", "Define your victories.") ?: "Define your victories." }
    val sec1F1 = remember { prefs.getString("morning_sec_1_f1", "Personal Objective") ?: "Personal Objective" }
    val sec1F2 = remember { prefs.getString("morning_sec_1_f2", "Work Objective") ?: "Work Objective" }

    val sec2Title = remember { prefs.getString("morning_sec_2_title", "Anticipation") ?: "Anticipation" }
    val sec2Sub = remember { prefs.getString("morning_sec_2_sub", "Prepare for resistance.") ?: "Prepare for resistance." }
    val sec2F1 = remember { prefs.getString("morning_sec_2_f1", "Potential Obstacles") ?: "Potential Obstacles" }
    val sec2F2 = remember { prefs.getString("morning_sec_2_f2", "My Solutions") ?: "My Solutions" }

    val sec3Title = remember { prefs.getString("morning_sec_3_title", "Spirit") ?: "Spirit" }
    val sec3Sub = remember { prefs.getString("morning_sec_3_sub", "Grit through gratitude.") ?: "Grit through gratitude." }
    val sec3F1 = remember { prefs.getString("morning_sec_3_f1", "What am I grateful for?") ?: "What am I grateful for?" }
    val sec3F2 = remember { prefs.getString("morning_sec_3_f2", "What am I in awe of?") ?: "What am I in awe of?" }

    val sec4Title = remember { prefs.getString("morning_sec_4_title", "Growth") ?: "Growth" }
    val sec4Sub = remember { prefs.getString("morning_sec_4_sub", "Continuous learning.") ?: "Continuous learning." }
    val sec4F1 = remember { prefs.getString("morning_sec_4_f1", "Read/Listen To") ?: "Read/Listen To" }

    val sec5Title = remember { prefs.getString("morning_sec_5_title", "Ideas & Solutions") ?: "Ideas & Solutions" }
    val sec5Sub = remember { prefs.getString("morning_sec_5_sub", "Brainstorm for the future.") ?: "Brainstorm for the future." }

    // Dynamic Evening Section Titles & Field Labels from Settings
    val evenSec0Title = remember { prefs.getString("evening_sec_0_title", "The Story") ?: "The Story" }
    val evenSec0Sub = remember { prefs.getString("evening_sec_0_sub", "What happened today?") ?: "What happened today?" }
    val evenSec0F1 = remember { prefs.getString("evening_sec_0_f1", "Daily Narrative") ?: "Daily Narrative" }

    val evenSec1Title = remember { prefs.getString("evening_sec_1_title", "Inventory") ?: "Inventory" }
    val evenSec1Sub = remember { prefs.getString("evening_sec_1_sub", "Own your wins.") ?: "Own your wins." }
    val evenSec1F1 = remember { prefs.getString("evening_sec_1_f1", "Main Accomplishments") ?: "Main Accomplishments" }

    val evenSec2Title = remember { prefs.getString("evening_sec_2_title", "Analysis") ?: "Analysis" }
    val evenSec2Sub = remember { prefs.getString("evening_sec_2_sub", "3 Wins and 3 Losses.") ?: "3 Wins and 3 Losses." }

    val evenSec3Title = remember { prefs.getString("evening_sec_3_title", "Bio-Metrics") ?: "Bio-Metrics" }
    val evenSec3Sub = remember { prefs.getString("evening_sec_3_sub", "Check your stats.") ?: "Check your stats." }

    val evenSec4Title = remember { prefs.getString("evening_sec_4_title", "Peace of Mind") ?: "Peace of Mind" }
    val evenSec4Sub = remember { prefs.getString("evening_sec_4_sub", "Setup tomorrow.") ?: "Setup tomorrow." }
    val evenSec4F1 = remember { prefs.getString("evening_sec_4_f1", "One thing for peace of mind") ?: "One thing for peace of mind" }

    val evenSec5Title = remember { prefs.getString("evening_sec_5_title", "Fuel") ?: "Fuel" }
    val evenSec5Sub = remember { prefs.getString("evening_sec_5_sub", "Log your meals.") ?: "Log your meals." }
    val evenSec5F1 = remember { prefs.getString("evening_sec_5_f1", "Breakfast, Lunch, Dinner") ?: "Breakfast, Lunch, Dinner" }

    val evenSec6Title = remember { prefs.getString("evening_sec_6_title", "Evening Ideas") ?: "Evening Ideas" }
    val evenSec6Sub = remember { prefs.getString("evening_sec_6_sub", "Don't lose your insights.") ?: "Don't lose your insights." }

    val evenSec7Title = remember { prefs.getString("evening_sec_7_title", "Quest Review") ?: "Quest Review" }
    val evenSec7Sub = remember { prefs.getString("evening_sec_7_sub", "How did you perform?") ?: "How did you perform?" }

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

    // Mandatory Text Entry Validation for Active Step
    val isStepValid = if (isMorning) {
        when (currentStep) {
            0 -> mornReflection.trim().length >= mornMinChars && mornDreams.trim().length >= mornMinChars
            1 -> mornPersonal.trim().length >= mornMinChars && mornWork.trim().length >= mornMinChars
            2 -> mornObstacles.trim().length >= mornMinChars && mornSolutions.trim().length >= mornMinChars
            3 -> mornGratitude.trim().length >= mornMinChars && mornAwe.trim().length >= mornMinChars
            4 -> mornReadLearn.trim().length >= mornMinChars
            5 -> true
            else -> true
        }
    } else {
        when (currentStep) {
            0 -> evenStory.trim().length >= evenMinChars
            1 -> evenAccomplishments.trim().length >= evenMinChars
            2 -> evenWins.isNotEmpty() || evenLosses.isNotEmpty()
            3 -> true
            4 -> evenPeaceOfMind.trim().length >= evenMinChars
            5 -> evenMeals.trim().length >= evenMinChars
            6 -> true
            7 -> true
            else -> true
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
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (!isStepValid) {
                        Text(
                            text = "⚠️ Complete all required text fields (min $activeMinChars chars) to proceed.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentStep < totalSteps - 1) {
                            Button(
                                onClick = { if (isStepValid) currentStep++ },
                                enabled = isStepValid,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Next Step")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!isStepValid) return@Button
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
                                enabled = isStepValid,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Finish Ritual")
                            }
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
                    0 -> StepContent(sec0Title, sec0Sub) {
                        RitualTextField(sec0F1, mornReflection, minChars = mornMinChars) { mornReflection = it }
                        RitualTextField(sec0F2, mornDreams, minChars = mornMinChars) { mornDreams = it }
                    }
                    1 -> StepContent(sec1Title, sec1Sub) {
                        RitualTextField(sec1F1, mornPersonal, minChars = mornMinChars) { mornPersonal = it }
                        RitualTextField(sec1F2, mornWork, minChars = mornMinChars) { mornWork = it }
                    }
                    2 -> StepContent(sec2Title, sec2Sub) {
                        RitualTextField(sec2F1, mornObstacles, minChars = mornMinChars) { mornObstacles = it }
                        RitualTextField(sec2F2, mornSolutions, minChars = mornMinChars) { mornSolutions = it }
                    }
                    3 -> StepContent(sec3Title, sec3Sub) {
                        RitualTextField(sec3F1, mornGratitude, minChars = mornMinChars) { mornGratitude = it }
                        RitualTextField(sec3F2, mornAwe, minChars = mornMinChars) { mornAwe = it }
                    }
                    4 -> StepContent(sec4Title, sec4Sub) {
                        RitualTextField(sec4F1, mornReadLearn, minChars = mornMinChars) { mornReadLearn = it }
                    }
                    5 -> StepContent(sec5Title, sec5Sub) {
                        Text("New Ideas", style = MaterialTheme.typography.titleSmall)
                        ListEditor(mornIdeas)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ideas captured here will be saved to your Brainstorming backlog.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                when (currentStep) {
                    0 -> StepContent(evenSec0Title, evenSec0Sub) {
                        RitualTextField(evenSec0F1, evenStory, minChars = evenMinChars) { evenStory = it }
                    }
                    1 -> StepContent(evenSec1Title, evenSec1Sub) {
                        RitualTextField(evenSec1F1, evenAccomplishments, minChars = evenMinChars) { evenAccomplishments = it }
                    }
                    2 -> StepContent(evenSec2Title, evenSec2Sub) {
                        Text("Top 3 Wins", style = MaterialTheme.typography.titleSmall)
                        ListEditor(evenWins)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Top 3 Losses", style = MaterialTheme.typography.titleSmall)
                        ListEditor(evenLosses)
                    }
                    3 -> StepContent(evenSec3Title, evenSec3Sub) {
                        Text("Mood: ${evenMood.toInt()}/10")
                        Slider(value = evenMood, onValueChange = { evenMood = it }, valueRange = 1f..10f, steps = 8)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Energy: ${evenEnergy.toInt()}/10")
                        Slider(value = evenEnergy, onValueChange = { evenEnergy = it }, valueRange = 1f..10f, steps = 8)
                    }
                    4 -> StepContent(evenSec4Title, evenSec4Sub) {
                        RitualTextField(evenSec4F1, evenPeaceOfMind, minChars = evenMinChars) { evenPeaceOfMind = it }
                    }
                    5 -> StepContent(evenSec5Title, evenSec5Sub) {
                        RitualTextField(evenSec5F1, evenMeals, minChars = evenMinChars) { evenMeals = it }
                    }
                    6 -> StepContent(evenSec6Title, evenSec6Sub) {
                        ListEditor(evenIdeas)
                        Text("Captured ideas go to your Brainstorming project.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    7 -> StepContent(evenSec7Title, evenSec7Sub) {
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
fun RitualTextField(
    label: String,
    value: String,
    minChars: Int = 5,
    isRequired: Boolean = true,
    onValueChange: (String) -> Unit
) {
    val trimmed = value.trim()
    val isValid = !isRequired || trimmed.length >= minChars

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isRequired && !isValid && value.isNotEmpty(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        minLines = 3,
        supportingText = {
            if (isRequired) {
                if (trimmed.length < minChars) {
                    Text(
                        text = "Required: ${trimmed.length}/$minChars characters minimum",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "✓ Valid entry (${trimmed.length} chars)",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }
        }
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
