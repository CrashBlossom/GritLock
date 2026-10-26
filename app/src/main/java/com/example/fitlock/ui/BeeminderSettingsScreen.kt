package com.example.fitlock.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import com.example.fitlock.data.BeeminderConfig
import com.example.fitlock.data.BeeminderGoalMapping
import com.example.fitlock.data.BeeminderMetricType
import com.example.fitlock.data.BeeminderPreferences
import com.example.fitlock.service.BeeminderSyncWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeeminderSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { BeeminderPreferences(context) }
    var config by remember { mutableStateOf(prefsManager.getConfig()) }
    val coroutineScope = rememberCoroutineScope()

    var showAddMappingModal by remember { mutableStateOf(false) }

    fun updateConfig(newConfig: BeeminderConfig) {
        config = newConfig
        prefsManager.saveConfig(newConfig)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Beeminder Auto-Sync", fontWeight = FontWeight.Bold)
                        Text("Push workouts, app usage & journal notes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            "Beeminder Sync Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Enable automatic background syncing of goals to Beeminder.",
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

            // Credentials Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Beeminder Account Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = config.username,
                        onValueChange = { updateConfig(config.copy(username = it)) },
                        label = { Text("Beeminder Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = config.authToken,
                        onValueChange = { updateConfig(config.copy(authToken = it)) },
                        label = { Text("Beeminder Auth Token") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                Toast.makeText(context, "Testing Beeminder Sync...", Toast.LENGTH_SHORT).show()
                                val (_, msg) = BeeminderSyncWorker.syncNow(context, config)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Sync All Goals Now")
                    }
                }
            }

            // Goal Mappings Card
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
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Beeminder Goal Mappings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showAddMappingModal = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Mapping", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Map specific workouts, app usage, or journal notes to distinct Beeminder goal slugs:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (config.mappings.isEmpty()) {
                        Text("No goal mappings configured yet. Tap 'Add Mapping' to start.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        config.mappings.forEachIndexed { index, mapping ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Goal Slug: ${mapping.goalSlug}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Metric: ${mapping.metricType.name} • Keys: ${mapping.selectedKeys.joinToString(", ")}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    val updated = config.mappings.toMutableList().apply { removeAt(index) }
                                    updateConfig(config.copy(mappings = updated))
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }
                            if (index < config.mappings.size - 1) {
                                HorizontalDivider(color = Color(0xFF2C2C35), modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMappingModal) {
        AddBeeminderMappingDialog(
            onDismiss = { showAddMappingModal = false },
            onMappingCreated = { newMapping ->
                val updated = config.mappings.toMutableList().apply { add(newMapping) }
                updateConfig(config.copy(mappings = updated))
                showAddMappingModal = false
            }
        )
    }
}

@Composable
private fun AddBeeminderMappingDialog(
    onDismiss: () -> Unit,
    onMappingCreated: (BeeminderGoalMapping) -> Unit
) {
    var goalSlug by remember { mutableStateOf("") }
    var metricType by remember { mutableStateOf(BeeminderMetricType.WORKOUT_REPS) }
    var selectedKeys by remember { mutableStateOf(listOf("PUSHUP")) }

    val exerciseOptions = listOf("PUSHUP", "SQUAT", "SITUP", "PLANK", "ROWING", "PULLUP", "DIP")
    val appOptions = listOf("com.duolingo", "net.ankiweb.anki")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Beeminder Goal Mapping", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = goalSlug,
                    onValueChange = { goalSlug = it },
                    label = { Text("Beeminder Goal Slug (e.g., leg_day)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Metric Type:", fontSize = 12.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BeeminderMetricType.entries.forEach { metric ->
                        FilterChip(
                            selected = metricType == metric,
                            onClick = {
                                metricType = metric
                                selectedKeys = when (metric) {
                                    BeeminderMetricType.WORKOUT_REPS -> listOf("PUSHUP")
                                    BeeminderMetricType.APP_USAGE -> listOf("com.duolingo")
                                    else -> emptyList()
                                }
                            },
                            label = { Text(metric.name, fontSize = 11.sp) }
                        )
                    }
                }

                if (metricType == BeeminderMetricType.WORKOUT_REPS) {
                    Text("Select Exercises (Summed together):", fontSize = 12.sp, color = Color.Gray)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        exerciseOptions.forEach { ex ->
                            val isSelected = selectedKeys.contains(ex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKeys = if (isSelected) {
                                            if (selectedKeys.size > 1) selectedKeys - ex else selectedKeys
                                        } else {
                                            selectedKeys + ex
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ex, fontSize = 13.sp)
                                Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                    selectedKeys = if (checked) selectedKeys + ex else if (selectedKeys.size > 1) selectedKeys - ex else selectedKeys
                                })
                            }
                        }
                    }
                } else if (metricType == BeeminderMetricType.APP_USAGE) {
                    Text("Select Apps (Summed together):", fontSize = 12.sp, color = Color.Gray)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        appOptions.forEach { app ->
                            val isSelected = selectedKeys.contains(app)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKeys = if (isSelected) {
                                            if (selectedKeys.size > 1) selectedKeys - app else selectedKeys
                                        } else {
                                            selectedKeys + app
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(app, fontSize = 13.sp)
                                Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                    selectedKeys = if (checked) selectedKeys + app else if (selectedKeys.size > 1) selectedKeys - app else selectedKeys
                                })
                            }
                        }
                    }
                } else if (metricType == BeeminderMetricType.DAILY_NOTE) {
                    Text("ℹ️ The entire content of today's daily log / reflection note will be pushed as the Beeminder comment (value: 1.0).", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (goalSlug.isNotBlank()) {
                        onMappingCreated(
                            BeeminderGoalMapping(
                                goalSlug = goalSlug.trim(),
                                metricType = metricType,
                                selectedKeys = selectedKeys
                            )
                        )
                    }
                }
            ) {
                Text("Save Mapping")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
