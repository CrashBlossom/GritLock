package com.example.fitlock.ui

import android.content.Intent
import android.content.pm.ResolveInfo
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.EmergencyReminderConfig
import com.example.fitlock.data.EmergencyReminderPreferences
import com.example.fitlock.data.EscalationLevel
import com.example.fitlock.data.MatrixGoalCategory
import com.example.fitlock.data.MatrixGoalConfig
import com.example.fitlock.data.MonitoredAppTarget
import com.example.fitlock.service.EmergencyReminderWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyReminderSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { EmergencyReminderPreferences(context) }
    var config by remember { mutableStateOf(prefsManager.getConfig()) }

    var selectedPreviewLevel by remember { mutableStateOf(EscalationLevel.NIGHT_IRRITATED) }
    var showAppPickerModal by remember { mutableStateOf(false) }
    var showAddGoalModal by remember { mutableStateOf(false) }

    fun updateConfig(newConfig: EmergencyReminderConfig) {
        config = newConfig
        prefsManager.saveConfig(newConfig)
        if (newConfig.isEnabled) {
            EmergencyReminderWorker.schedule(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Emergency Reminders", fontWeight = FontWeight.Bold)
                        Text("Duolingo-style escalating notifications", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Master Switch Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Escalating Streak Shield",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Sends increasingly urgent notifications from midday to 11:15 PM if daily matrix goals are unfulfilled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { isChecked ->
                            updateConfig(config.copy(isEnabled = isChecked))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Escalation Levels Preview Card (With Clean Non-wrapping Layout)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escalation Schedule & Tones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select a level to preview tone:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EscalationLevel.entries.forEach { level ->
                            val isSelected = selectedPreviewLevel == level
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2C2C35),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPreviewLevel = level }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${level.titlePrefix} (${level.hourThreshold}:00)",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF2C2C35),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = level.urgencyLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            EmergencyReminderWorker.sendSampleNotification(context, selectedPreviewLevel, config)
                            Toast.makeText(context, "Sample notification sent to notification tray!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Sample Emergency Notification")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Universal Matrix Goal Thresholds Card (GREEN Min vs GOLD Standard)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consistency Matrix Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showAddGoalModal = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Text("Configure Minimum (GREEN) and Gold Standard (GOLD) targets for exercises, apps, and routines:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    config.matrixGoals.forEachIndexed { index, goal ->
                        DualThresholdGoalItem(
                            goal = goal,
                            onUpdate = { updatedGoal ->
                                val updatedGoals = config.matrixGoals.toMutableList().apply { set(index, updatedGoal) }
                                updateConfig(config.copy(matrixGoals = updatedGoals))
                            },
                            onIconChange = { chosenEmoji ->
                                val updatedIcons = config.customIcons.toMutableMap().apply { put(goal.exerciseTypeOrKey, chosenEmoji) }
                                val textPart = goal.label.substringAfter(" ").ifBlank { goal.label }
                                val updatedLabel = "$chosenEmoji $textPart"
                                val updatedGoals = config.matrixGoals.toMutableList().apply { set(index, goal.copy(label = updatedLabel)) }
                                updateConfig(config.copy(customIcons = updatedIcons, matrixGoals = updatedGoals))
                            },
                            onDelete = {
                                val updatedGoals = config.matrixGoals.toMutableList().apply { removeAt(index) }
                                updateConfig(config.copy(matrixGoals = updatedGoals))
                            }
                        )
                        if (index < config.matrixGoals.size - 1) {
                            HorizontalDivider(color = Color(0xFF2C2C35), modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monitored External Apps Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Monitored External Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showAppPickerModal = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add App", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select external learning/habit apps to monitor for daily usage:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    config.monitoredApps.forEachIndexed { idx, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("${app.packageName} • Target: ${app.targetMinutes} mins", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            IconButton(onClick = {
                                val updatedApps = config.monitoredApps.toMutableList().apply { removeAt(idx) }
                                updateConfig(config.copy(monitoredApps = updatedApps))
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove App", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // App Picker Modal Dialog
    if (showAppPickerModal) {
        InstalledAppPickerDialog(
            onDismiss = { showAppPickerModal = false },
            onAppSelected = { selected ->
                val updatedApps = config.monitoredApps.toMutableList().apply {
                    if (none { it.packageName == selected.packageName }) add(selected)
                }
                val updatedGoals = config.matrixGoals.toMutableList().apply {
                    if (none { it.exerciseTypeOrKey == selected.packageName }) {
                        add(
                            MatrixGoalConfig(
                                label = "📱 ${selected.appName}",
                                category = MatrixGoalCategory.APP_USAGE,
                                exerciseTypeOrKey = selected.packageName,
                                minTarget = selected.targetMinutes,
                                goldTarget = selected.targetMinutes * 2,
                                unit = "minutes"
                            )
                        )
                    }
                }
                updateConfig(config.copy(monitoredApps = updatedApps, matrixGoals = updatedGoals))
                showAppPickerModal = false
            }
        )
    }

    // Add Goal Modal Dialog
    if (showAddGoalModal) {
        AddMatrixGoalDialog(
            onDismiss = { showAddGoalModal = false },
            onGoalCreated = { newGoal ->
                val updatedGoals = config.matrixGoals.toMutableList().apply { add(newGoal) }
                updateConfig(config.copy(matrixGoals = updatedGoals))
                showAddGoalModal = false
            }
        )
    }
}

@Composable
private fun DualThresholdGoalItem(
    goal: MatrixGoalConfig,
    onUpdate: (MatrixGoalConfig) -> Unit,
    onIconChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showIconPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showIconPicker = true }
                )
                IconButton(onClick = { showIconPicker = true }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = when (goal.category) {
                        MatrixGoalCategory.EXERCISE -> Color(0xFF81C784).copy(alpha = 0.2f)
                        MatrixGoalCategory.APP_USAGE -> Color(0xFF64B5F6).copy(alpha = 0.2f)
                        MatrixGoalCategory.HABIT_ROUTINE -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = goal.category.name,
                        fontSize = 9.sp,
                        color = when (goal.category) {
                            MatrixGoalCategory.EXERCISE -> Color(0xFF81C784)
                            MatrixGoalCategory.APP_USAGE -> Color(0xFF64B5F6)
                            MatrixGoalCategory.HABIT_ROUTINE -> Color(0xFFFFB74D)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // GREEN Min Threshold
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🟢 Min (GREEN)", fontSize = 12.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
            Text("${goal.minTarget} ${goal.unit}", fontSize = 12.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = goal.minTarget.toFloat(),
            onValueChange = { onUpdate(goal.copy(minTarget = it.toInt().coerceAtMost(goal.goldTarget))) },
            valueRange = 1f..100f
        )

        // GOLD Standard Threshold
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🟡 Gold Standard (GOLD)", fontSize = 12.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            Text("${goal.goldTarget} ${goal.unit}", fontSize = 12.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = goal.goldTarget.toFloat(),
            onValueChange = { onUpdate(goal.copy(goldTarget = it.toInt().coerceAtLeast(goal.minTarget))) },
            valueRange = 1f..150f
        )

        if (showIconPicker) {
            EmojiPickerDialog(
                currentLabel = goal.label,
                onDismiss = { showIconPicker = false },
                onEmojiSelected = { chosenEmoji ->
                    onIconChange(chosenEmoji)
                    showIconPicker = false
                }
            )
        }
    }
}

@Composable
private fun EmojiPickerDialog(
    currentLabel: String,
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    val quickEmojis = listOf(
        "💪", "🦵", "🏋️", "🏃", "🚴", "🏊", "🤸", "🧘", "⏱️", "🫁", "🚣", "🦾",
        "🦉", "🎴", "🧠", "📚", "💻", "⏱️", "📝", "💡", "🎯", "⭐", "⚡",
        "☀️", "🌙", "🔥", "💧", "👟", "🏆", "💎", "🍎", "☕", "🌿", "📖", "🎮", "📱", "🚀"
    )
    var customEmojiInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Goal Icon / Emoji", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Type any emoji or select from popular icons below for $currentLabel:", fontSize = 12.sp, color = Color.Gray)

                OutlinedTextField(
                    value = customEmojiInput,
                    onValueChange = { customEmojiInput = it },
                    label = { Text("Type custom emoji/symbol") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (customEmojiInput.isNotBlank()) {
                    Button(
                        onClick = { onEmojiSelected(customEmojiInput.trim()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use '${customEmojiInput.trim()}'")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Popular Emojis:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickEmojis.chunked(6).forEach { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowEmojis.forEach { emoji ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2C2C35),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable { onEmojiSelected(emoji) },
                                    tonalElevation = 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InstalledAppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (MonitoredAppTarget) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    val installedApps = remember {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(mainIntent, 0).mapNotNull { resolveInfo: ResolveInfo ->
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName != context.packageName) MonitoredAppTarget(packageName, appName, 15) else null
        }.distinctBy { it.packageName }.sortedBy { it.appName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App to Monitor", fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                LazyColumn {
                    items(installedApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(app.packageName, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        HorizontalDivider(color = Color(0xFF2C2C35))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddMatrixGoalDialog(
    onDismiss: () -> Unit,
    onGoalCreated: (MatrixGoalConfig) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MatrixGoalCategory.EXERCISE) }
    var minTarget by remember { mutableIntStateOf(15) }
    var goldTarget by remember { mutableIntStateOf(30) }

    val presets = listOf(
        Triple("💪 Pushups", "PUSHUP", MatrixGoalCategory.EXERCISE),
        Triple("🦵 Squats", "SQUAT", MatrixGoalCategory.EXERCISE),
        Triple("🧘 Situps", "SITUP", MatrixGoalCategory.EXERCISE),
        Triple("⏱️ Plank", "PLANK", MatrixGoalCategory.EXERCISE),
        Triple("🚣 Rowing", "ROWING", MatrixGoalCategory.EXERCISE),
        Triple("🏋️ Pullups", "PULLUP", MatrixGoalCategory.EXERCISE),
        Triple("🦾 Dips", "DIP", MatrixGoalCategory.EXERCISE),
        Triple("🫁 Breathwork", "BREATHWORK", MatrixGoalCategory.EXERCISE),
        Triple("☀️ Morning Journal", "MORNING_JOURNAL", MatrixGoalCategory.HABIT_ROUTINE),
        Triple("🌙 Evening Review", "EVENING_REVIEW", MatrixGoalCategory.HABIT_ROUTINE),
        Triple("🔥 Daily Pledge", "DAILY_PLEDGE", MatrixGoalCategory.HABIT_ROUTINE)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Matrix Goal Preset", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select from app presets or enter custom below:", fontSize = 12.sp, color = Color.Gray)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                label = preset.first
                                key = preset.second
                                category = preset.third
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(preset.first, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C35), modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Goal Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key / Type (e.g., PUSHUP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MatrixGoalCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.name, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && key.isNotBlank()) {
                        onGoalCreated(
                            MatrixGoalConfig(
                                label = label,
                                category = category,
                                exerciseTypeOrKey = key.uppercase(),
                                minTarget = minTarget,
                                goldTarget = goldTarget,
                                unit = if (category == MatrixGoalCategory.APP_USAGE) "minutes" else if (category == MatrixGoalCategory.EXERCISE) "reps" else "done"
                            )
                        )
                    }
                }
            ) {
                Text("Add Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


