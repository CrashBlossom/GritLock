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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.data.ScheduleInterval
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.utils.AppInfoFetcher

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    editingGroup: AppGroup? = null,
    onBack: () -> Unit,
    onSave: (AppGroup) -> Unit
) {
    val context = LocalContext.current
    var groupName by remember(editingGroup) { mutableStateOf(editingGroup?.name ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedApps by remember(editingGroup) { mutableStateOf(editingGroup?.packageNames?.toSet() ?: setOf()) }
    var exerciseRequirements by remember(editingGroup) { 
        mutableStateOf(editingGroup?.exercises ?: listOf(ExerciseRequirement("PUSHUP", 10))) 
    }
    var unlockDuration by remember(editingGroup) { mutableStateOf(editingGroup?.unlockDurationMinutes?.toString() ?: "30") }
    
    // Schedule state
    var schedule by remember(editingGroup) { mutableStateOf(editingGroup?.schedule ?: emptyList()) }
    
    // Keywords state
    var keywords by remember(editingGroup) { mutableStateOf(editingGroup?.keywords ?: emptyList()) }
    var newKeyword by remember { mutableStateOf("") }

    val allApps = remember { AppInfoFetcher.getInstalledApps(context) }
    val filteredApps = allApps.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingGroup == null) "Create Group" else "Edit Group", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121216))
            )
        },
        containerColor = Color(0xFF121216)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C1C21),
                    unfocusedContainerColor = Color(0xFF1C1C21),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Apps Section
            Text(
                "APPS TO BLOCK (${selectedApps.size} SELECTED)",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C1C21),
                    unfocusedContainerColor = Color(0xFF1C1C21),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(150.dp).fillMaxWidth()
            ) {
                LazyColumn {
                    items(filteredApps) { app ->
                        val isSelected = selectedApps.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedApps = if (isSelected) {
                                        selectedApps - app.packageName
                                    } else {
                                        selectedApps + app.packageName
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD0BCFF))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(app.name, color = Color.White, fontSize = 14.sp)
                                Text(app.packageName, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        HorizontalDivider(color = Color(0xFF25252B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Keywords Section
            Text(
                "KEYWORDS / URLS (BLOCKING)",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    placeholder = { Text("e.g. youtube.com, gambling", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C21),
                        unfocusedContainerColor = Color(0xFF1C1C21),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                IconButton(onClick = {
                    if (newKeyword.isNotBlank()) {
                        keywords = keywords + newKeyword.trim()
                        newKeyword = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Keyword", tint = Color(0xFFD0BCFF))
                }
            }
            
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keywords.forEach { kw ->
                    AssistChip(
                        onClick = { keywords = keywords - kw },
                        label = { Text(kw, fontSize = 11.sp) },
                        trailingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Color.White,
                            containerColor = Color(0xFF2D2D35)
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Schedule Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SCHEDULE",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { 
                    schedule = schedule + ScheduleInterval(1, 1080, 1260) 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Schedule", tint = Color.White)
                }
            }

            if (schedule.isEmpty()) {
                Text("Always active (no schedule)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
            schedule.forEachIndexed { index, interval ->
                ScheduleRow(interval, onUpdate = { newInterval ->
                    schedule = schedule.toMutableList().apply { this[index] = newInterval }
                }, onDelete = {
                    schedule = schedule.toMutableList().apply { removeAt(index) }
                })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Requirements Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "REQUIREMENTS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { 
                    exerciseRequirements = exerciseRequirements + ExerciseRequirement("PUSHUP", 10) 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise", tint = Color.White)
                }
            }

            exerciseRequirements.forEachIndexed { index, req ->
                RequirementRow(index, req, onUpdate = { newReq ->
                    exerciseRequirements = exerciseRequirements.toMutableList().apply { this[index] = newReq }
                }, onDelete = {
                    if (exerciseRequirements.size > 1) {
                        exerciseRequirements = exerciseRequirements.toMutableList().apply { removeAt(index) }
                    }
                })
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Relock after (min):", color = Color.White, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = unlockDuration,
                    onValueChange = { unlockDuration = it },
                    modifier = Modifier.width(100.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C21),
                        unfocusedContainerColor = Color(0xFF1C1C21),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (groupName.isNotBlank() && (selectedApps.isNotEmpty() || keywords.isNotEmpty())) {
                        onSave(AppGroup(
                            id = editingGroup?.id ?: 0,
                            name = groupName,
                            packageNames = selectedApps.toList(),
                            exercises = exerciseRequirements,
                            unlockDurationMinutes = unlockDuration.toIntOrNull() ?: 30,
                            schedule = schedule,
                            isEnabled = editingGroup?.isEnabled ?: true,
                            keywords = keywords
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (editingGroup == null) "Create Group" else "Update Group", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ScheduleRow(interval: ScheduleInterval, onUpdate: (ScheduleInterval) -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        var expandedDay by remember { mutableStateOf(false) }
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { expandedDay = true }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(days[interval.dayOfWeek - 1], color = Color.White, fontSize = 10.sp)
            }
            DropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                days.forEachIndexed { i, day ->
                    DropdownMenuItem(text = { Text(day) }, onClick = { 
                        onUpdate(interval.copy(dayOfWeek = i + 1))
                        expandedDay = false 
                    })
                }
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        TimeInput(interval.startMinute, "Start", Modifier.weight(1.2f)) { onUpdate(interval.copy(startMinute = it)) }
        Text("-", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
        TimeInput(interval.endMinute, "End", Modifier.weight(1.2f)) { onUpdate(interval.copy(endMinute = it)) }
        
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun TimeInput(minutes: Int, label: String, modifier: Modifier, onValueChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(String.format("%02d:%02d", minutes / 60, minutes % 60)) }
    OutlinedTextField(
        value = text,
        onValueChange = { 
            text = it
            val parts = it.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                onValueChange(h * 60 + m)
            }
        },
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp),
        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
}
