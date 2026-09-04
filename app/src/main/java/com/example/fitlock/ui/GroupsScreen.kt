/**
 * This file contains the main user interface for the "Groups" screen.
 * It uses Jetpack Compose, which is Android's modern toolkit for building native UI.
 */
@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.example.fitlock.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.Challenge
import com.example.fitlock.data.UserStats
import com.example.fitlock.exercise.ExerciseType

/**
 * Main screen showing exercise groups, challenges, and daily goals.
 */
@Composable
fun GroupsScreen(
    groups: List<AppGroup>, // List of app groups created by the user
    userStats: UserStats?,   // User's current statistics (XP, level, banked reps)
    challenges: List<Challenge>, // List of active challenges
    todayTotals: Map<String, Int>, // Exercise counts performed today
    onAddGroupClick: () -> Unit,   // Callback for adding a new app group
    onEditGroupClick: (AppGroup) -> Unit, // Callback for editing a group
    onDeleteGroupClick: (AppGroup) -> Unit, // Callback for deleting a group
    onToggleGroupClick: (AppGroup) -> Unit, // Callback for enabling/disabling a group
    onSettingsClick: () -> Unit, // Callback to navigate to settings
    onEmergencyBypassClick: () -> Unit, // Callback for the emergency bypass feature
    onChallengeClick: (Challenge) -> Unit, // Callback when a challenge is clicked
    onExerciseClick: (ExerciseType, Int) -> Unit // Callback when a daily goal is clicked
) {
    val context = LocalContext.current
    // Accessing shared preferences to read app settings
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    
    // Check if the user has locked editing to prevent impulsive changes
    val isEditingLocked = prefs.getBoolean("lock_editing", false)
    val customBankMessage = prefs.getString("custom_bank_msg", "No reps banked yet. Go sweat!") ?: "No reps banked yet. Go sweat!"

    // UI state variables
    var showEditLockDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var selectedChallengeForDetails by remember { mutableStateOf<Challenge?>(null) }

    /**
     * Helper function to handle user actions (like edit/delete).
     * If editing is locked, it shows a confirmation dialog first.
     */
    val handleAction = { action: () -> Unit ->
        if (isEditingLocked) {
            pendingAction = action
            showEditLockDialog = true
        } else {
            action()
        }
    }

    // Main layout container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // App Header (Title and Settings icon)
        Header(onSettingsClick)
        
        // LazyColumn is like a RecyclerView - it only renders items currently visible on screen
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Rep Bank Card: Shows exercises saved for later use
            item {
                RepBankCard(userStats?.bankedReps ?: emptyMap(), customBankMessage, onEmergencyBypassClick)
            }

            // 2. Daily Goals Section: Progress bars for daily exercise targets
            item {
                SectionTitle("Daily Goals")
                DailyGoalsList(prefs, todayTotals, onExerciseClick)
            }
            
            // 3. Active Challenges Section: Special tasks for extra XP
            item {
                SectionTitle("Active Challenges")
                if (challenges.isEmpty()) {
                    ChallengePlaceholder()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        challenges.forEach { challenge ->
                            ChallengeItem(
                                challenge = challenge,
                                todayTotals = todayTotals, // Pass today's data to show progress
                                onClick = { onChallengeClick(challenge) },
                                onLongClick = { selectedChallengeForDetails = challenge }
                            )
                        }
                    }
                }
            }

            // 4. App Groups Section: List of apps that are restricted
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("App Groups")
                    IconButton(onClick = { handleAction(onAddGroupClick) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Group", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Render each app group item
            items(groups) { group ->
                GroupItem(
                    group = group,
                    onToggle = { onToggleGroupClick(group) },
                    onLongClick = { handleAction { onEditGroupClick(group) } },
                    onDelete = { handleAction { onDeleteGroupClick(group) } }
                )
            }
        }
    }

    // Dialog shown when editing is locked (requires typing a phrase to unlock)
    if (showEditLockDialog) {
        EditLockDialog(
            onDismiss = { showEditLockDialog = false },
            onUnlockConfirmed = {
                showEditLockDialog = false
                pendingAction?.invoke()
            }
        )
    }
    
    // Detailed view for a challenge showing specific requirements and progress
    selectedChallengeForDetails?.let { challenge ->
        ChallengeDetailsDialog(
            challenge = challenge,
            todayTotals = todayTotals,
            onDismiss = { selectedChallengeForDetails = null },
            onStart = {
                onChallengeClick(challenge)
                selectedChallengeForDetails = null
            }
        )
    }
}

/**
 * A dialog that pops up when a user long-presses a challenge.
 * Shows detailed progress bars for every requirement in that challenge.
 */
@Composable
fun ChallengeDetailsDialog(
    challenge: Challenge,
    todayTotals: Map<String, Int>,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(challenge.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(challenge.description, color = Color.Gray, fontSize = 14.sp)
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                Text("Progress:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                
                // Show progress for each requirement (e.g. 50/100 Pushups)
                challenge.requirements.forEach { req ->
                    val progress = todayTotals[req.type] ?: 0
                    val percent = if (req.count > 0) (progress.toFloat() / req.count.toFloat()).coerceIn(0f, 1f) else 0f
                    
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(req.type, fontSize = 13.sp)
                            Text("$progress / ${req.count}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Visual bar showing how close the user is to completing the requirement
                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (percent >= 1f) Color.Green else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                // The reward the user gets upon completion
                Text("Reward: ${challenge.xpReward} XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onStart) {
                Text("START WORKOUT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Card showing the "Rep Bank" - exercises the user did extra that can be used to unlock apps later.
 */
@Composable
fun RepBankCard(bankedReps: Map<String, Int>, customMessage: String, onEmergencyBypassClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEmergencyBypassClick() }, // Clicking the bank takes you to the bypass if needed
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header for the bank
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Rep Bank", color = Color.Gray, fontSize = 12.sp)
                    Text("Stored Exercises", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show either the banked reps or a motivational message if the bank is empty
            if (bankedReps.isEmpty() || bankedReps.all { it.value == 0 }) {
                Text(customMessage, color = Color.Gray, fontSize = 13.sp)
            } else {
                // FlowRow wraps items to the next line if there isn't enough width
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bankedReps.filter { it.value > 0 }.forEach { (type, count) ->
                        AssistChip(
                            onClick = {},
                            label = { Text("$count $type") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A security dialog that forces the user to type a long phrase to prove they really want to edit their goals.
 */
@Composable
fun EditLockDialog(onDismiss: () -> Unit, onUnlockConfirmed: () -> Unit) {
    val targetPhrase = "I understand that editing these groups is a commitment to my future self. I am not changing them out of weakness or a desire to escape my goals. I am focused, disciplined, and in control of my time."
    var inputText by remember { mutableStateOf("") }
    // Button is only enabled if the typed text matches perfectly
    val isCorrect = inputText.trim() == targetPhrase.trim()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒 EDITING LOCKED 🔒", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Type the following paragraph to unlock group settings:", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    // The target text for the user to copy
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Text(targetPhrase, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Where the user types
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("Start typing...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onUnlockConfirmed,
                        enabled = isCorrect,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                    ) {
                        Text("UNLOCK EDITING", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}

/**
 * Standard title for sections in the screen.
 */
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * List of daily exercise goals with progress bars.
 */
@Composable
fun DailyGoalsList(
    prefs: android.content.SharedPreferences, 
    todayTotals: Map<String, Int>,
    onExerciseClick: (ExerciseType, Int) -> Unit
) {
    // Filter exercises that are marked as visible in settings
    val enabledExercises = ExerciseType.entries.filter { 
        prefs.getBoolean("visible_${it.name}", true) 
    }

    if (enabledExercises.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Enable exercises in Setting to see daily goals", 
                color = Color.Gray, 
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            enabledExercises.forEach { type ->
                // Fetch the goal amount from settings (default to 10)
                val goal = prefs.getInt("goal_${type.name}", 10)
                // Fetch current progress from the todayTotals map
                val progress = todayTotals[type.name] ?: 0
                // Calculate percentage (0.0 to 1.0) for the progress bar
                val percent = if (goal > 0) (progress.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
                
                Card(
                    modifier = Modifier.clickable { onExerciseClick(type, goal) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Choose the right icon based on exercise type
                            val icon = when(type) {
                                ExerciseType.PUSHUP -> Icons.Default.FitnessCenter
                                ExerciseType.SQUAT -> Icons.Default.AccessibilityNew
                                ExerciseType.PULLUP -> Icons.Default.VerticalAlignTop
                                ExerciseType.DIP -> Icons.Default.ArrowDownward
                                ExerciseType.SITUP -> Icons.Default.ArrowUpward
                                ExerciseType.HINGE -> Icons.Default.Accessibility
                                ExerciseType.ROW -> Icons.Default.LineWeight
                                ExerciseType.PLANK -> Icons.Default.Timer
                                ExerciseType.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
                                ExerciseType.APP_USAGE -> Icons.Default.Apps
                                ExerciseType.FLASHCARDS -> Icons.Default.HistoryEdu
                            }
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Progress Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                // Format text (e.g. 5s / 30s or 10 / 20)
                                val progressText = if (type == ExerciseType.PLANK || type == ExerciseType.APP_USAGE) {
                                    "${progress}s / ${goal}s"
                                } else {
                                    "$progress / $goal"
                                }
                                Text(progressText, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Visual progress bar
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top portion of the screen with the app name.
 */
@Composable
fun Header(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("GritLock", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("Sweat to scroll", color = Color.Gray, fontSize = 12.sp)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
        }
    }
}

/**
 * UI shown when there are no challenges available.
 */
@Composable
fun ChallengePlaceholder() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No active challenges", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

/**
 * A single challenge row item. Now shows a summary progress bar.
 */
@Composable
fun ChallengeItem(
    challenge: Challenge,
    todayTotals: Map<String, Int>, // Today's totals to calculate progress
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Calculate overall progress for the challenge
    val totalGoal = challenge.requirements.sumOf { it.count }
    val currentProgress = challenge.requirements.sumOf { req -> 
        (todayTotals[req.type] ?: 0).coerceAtMost(req.count)
    }
    val overallPercent = if (totalGoal > 0) (currentProgress.toFloat() / totalGoal.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    val status = if (challenge.isCompletedToday || overallPercent >= 1f) "Completed!" else "${challenge.requirements.size} requirements"
                    Text(status, color = if (challenge.isCompletedToday || overallPercent >= 1f) Color.Green else Color.Gray, fontSize = 11.sp)
                }
                Text("+${challenge.xpReward} XP", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Overall challenge progress bar
            LinearProgressIndicator(
                progress = { overallPercent },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = if (overallPercent >= 1f) Color.Green else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        }
    }
}

/**
 * A single row representing an app group (a collection of apps that are currently blocked).
 */
@Composable
fun GroupItem(
    group: AppGroup,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {}, // Normal click does nothing
                onLongClick = onLongClick // Long click to edit
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (group.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row with Icon, Name, and Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (group.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (group.isEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (group.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            group.name,
                            color = if (group.isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${group.packageNames.size} apps restricted",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    // Switch to enable/disable the blocking rules for this group
                    Switch(
                        checked = group.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            
            // Show summary of blocked keywords if any
            if (group.keywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("${group.keywords.size} Keywords/URLs Blocked", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
            }

            // Show requirement chips (e.g. "10 pushups")
            if (group.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Requirements:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.exercises.forEach { req ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${req.count} ${req.type.lowercase()}", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
    }
}
