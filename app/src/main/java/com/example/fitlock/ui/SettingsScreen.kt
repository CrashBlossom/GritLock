package com.example.fitlock.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.fitlock.utils.DataBackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.ai.Gemma4Manager
import com.example.fitlock.ai.SystemEventContext
import com.example.fitlock.ai.SystemPromptEngine
import com.example.fitlock.ai.SystemVoiceSynthesizer
import com.example.fitlock.data.UserStats
import com.example.fitlock.data.ExerciseCalibration
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.exercise.TrackingMode
import com.example.fitlock.utils.HealthConnectManager

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    trackingMode: TrackingMode,
    onTrackingModeChange: (TrackingMode) -> Unit,
    userStats: UserStats?,
    onUpdateUserStats: (UserStats) -> Unit,
    onNavigateToCalibration: (ExerciseType) -> Unit,
    onNavigateToQuotes: () -> Unit,
    groups: List<com.example.fitlock.data.AppGroup> = emptyList()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    
    // Permission states
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isOverlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasHealthConnectPermissions by remember { mutableStateOf(false) }

    val healthConnectManager = remember { HealthConnectManager(context) }
    
    // Customization states
    var selectedTheme by remember { mutableStateOf(prefs.getString("app_theme", "DEFAULT") ?: "DEFAULT") }
    var customBankMsg by remember { mutableStateOf(prefs.getString("custom_bank_msg", "No reps banked yet. Go sweat!") ?: "No reps banked yet. Go sweat!") }

    // Lockout delay
    var lockoutSeconds by remember { mutableIntStateOf(prefs.getInt("lockout_seconds", 10)) }

    // Strict Mode state
    var strictMode by remember { mutableStateOf(prefs.getBoolean("strict_mode", false)) }
    
    // Active Lockdown on movement reminder trigger
    var intermittentLockEnabled by remember { mutableStateOf(prefs.getBoolean("intermittent_lock_enabled", false)) }

    // HP Drain Mode state
    var useHpDrainMode by remember { mutableStateOf(prefs.getBoolean("use_hp_drain_mode", false)) }
    
    // Master Password state
    var masterPasswordEnabled by remember { mutableStateOf(prefs.getBoolean("master_password_enabled", false)) }
    val masterPassword = remember { prefs.getString("master_password", "") ?: "" }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showVerifyPasswordDialog by remember { mutableStateOf(false) }

    // Refresh permissions
    LaunchedEffect(Unit) {
        while(true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            isOverlayPermissionGranted = Settings.canDrawOverlays(context)
            hasHealthConnectPermissions = healthConnectManager.hasPermissions()
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Setting",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // 1. App Customization & Themes Section
        ExpandableSection("App Customization & Themes", initialExpanded = false) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Theme & Color Scheme", color = Color.Gray, fontSize = 11.sp)
                val themes = listOf("DEFAULT" to "Default", "SUPERHERO" to "Vigilante", "AGENT" to "Secret Agent", "SOLO" to "Solo Leveler", "COZY" to "Cozy Adventurer", "WINTER_ARC" to "Winter Arc")
                themes.forEach { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTheme = id
                                prefs.edit().putString("app_theme", id).apply()
                                Toast.makeText(context, "Theme updated to $name", Toast.LENGTH_SHORT).show()
                                (context as? android.app.Activity)?.recreate()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == id,
                            onClick = {
                                selectedTheme = id
                                prefs.edit().putString("app_theme", id).apply()
                                Toast.makeText(context, "Theme updated to $name", Toast.LENGTH_SHORT).show()
                                (context as? android.app.Activity)?.recreate()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // 2. Morning & Evening Plan Templates Section (Expandable)
        RoutineTemplatesSettingsCard(prefs)

        // 3. Full System Backup & Restore Section (Expandable)
        BackupRestoreSettingsCard(prefs)

        // 3. System Permissions Status Section (Including Health Connect)
        ExpandableSection("System Permissions", initialExpanded = true) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionStatusRow(
                    title = "Accessibility Service",
                    description = "Required to block apps and show countdowns.",
                    isEnabled = isAccessibilityEnabled,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                PermissionStatusRow(
                    title = "Display Over Other Apps",
                    description = "Required to show the lock screen over restricted apps.",
                    isEnabled = isOverlayPermissionGranted,
                    onGrant = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                PermissionStatusRow(
                    title = "Health Connect Integration",
                    description = "Syncs sleep, water, calories, and steps vitality.",
                    isEnabled = hasHealthConnectPermissions,
                    onGrant = {
                        Toast.makeText(context, "Health Connect permissions managed in Android Settings.", Toast.LENGTH_LONG).show()
                    }
                )
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                Button(
                    onClick = {
                        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                        isOverlayPermissionGranted = Settings.canDrawOverlays(context)
                        Toast.makeText(context, "Status Refreshed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check All Status")
                }
            }
        }

        // 4. Daily Goals Section
        ExpandableSection("Daily Goals", initialExpanded = false) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseType.entries.forEach { type ->
                    DailyGoalItem(type, prefs)
                    if (type != ExerciseType.entries.last()) {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                    }
                }
            }
        }

        // 5. Rep Bank Management Section (With Clearance Frequency)
        ExpandableSection("Rep Bank Management", initialExpanded = false) {
            RepBankSettingsCard(userStats, onUpdateUserStats, prefs)
        }

        // 6. Behavior Section (Vertical Layout for Penalty Modes)
        ExpandableSection("Behavior", initialExpanded = false) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Penalty Execution Mode", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            useHpDrainMode = false
                            prefs.edit().putBoolean("use_hp_drain_mode", false).apply()
                        }.padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = !useHpDrainMode, onClick = {
                            useHpDrainMode = false
                            prefs.edit().putBoolean("use_hp_drain_mode", false).apply()
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lockout Countdown", color = Color.White, fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            useHpDrainMode = true
                            prefs.edit().putBoolean("use_hp_drain_mode", true).apply()
                        }.padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = useHpDrainMode, onClick = {
                            useHpDrainMode = true
                            prefs.edit().putBoolean("use_hp_drain_mode", true).apply()
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("HP Drain Mode", color = Color.White, fontSize = 14.sp)
                    }
                }

                if (!useHpDrainMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lockout Countdown Seconds: $lockoutSeconds", color = Color.Gray, fontSize = 12.sp)
                    Slider(
                        value = lockoutSeconds.toFloat(),
                        onValueChange = { 
                            lockoutSeconds = it.toInt()
                            prefs.edit().putInt("lockout_seconds", it.toInt()).apply()
                        },
                        valueRange = 0f..30f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                // Toggles for Strict Mode, Master Password, Active Lockdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Mode", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Prevents bypassing lock screens easily.", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = strictMode,
                        onCheckedChange = {
                            strictMode = it
                            prefs.edit().putBoolean("strict_mode", it).apply()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Lockdown on Movement Reminder", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Locks target app group when intermittent movement check fires.", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = intermittentLockEnabled,
                        onCheckedChange = {
                            intermittentLockEnabled = it
                            prefs.edit().putBoolean("intermittent_lock_enabled", it).apply()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master Password Protection", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Requires master password to disable locks or modify settings.", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = masterPasswordEnabled,
                        onCheckedChange = { isEnabling ->
                            if (isEnabling) {
                                showSetPasswordDialog = true
                            } else {
                                showVerifyPasswordDialog = true
                            }
                        }
                    )
                }
            }
        }

        // 7. Exercise Management Section
        ExpandableSection("Exercise Management", initialExpanded = false) {
            ExerciseSettingsCard(prefs, userStats, onNavigateToCalibration)
        }

        // 8. System AI Section
        ExpandableSection("System AI Engine (Gemma On-Device)", initialExpanded = false) {
            GemmaAiSettingsCard()
        }

        // 9. Tracking Section
        ExpandableSection("Tracking", initialExpanded = false) {
            TrackingModeCard(trackingMode, onTrackingModeChange)
        }

        // 10. System Codex & World Lore Section
        ExpandableSection("System Codex & World Lore", initialExpanded = false) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatReferenceGuideCard()
                HowTheAppWorksCard()
                WorldLoreOriginCard()
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showSetPasswordDialog) {
        SetMasterPasswordDialog(
            onDismiss = { showSetPasswordDialog = false },
            onSave = { newPass ->
                prefs.edit()
                    .putBoolean("master_password_enabled", true)
                    .putString("master_password", newPass)
                    .apply()
                masterPasswordEnabled = true
                showSetPasswordDialog = false
                Toast.makeText(context, "Master Protection Enabled", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    if (showVerifyPasswordDialog) {
        MasterPasswordDialog(
            correctPassword = masterPassword,
            onDismiss = { showVerifyPasswordDialog = false },
            onSuccess = {
                prefs.edit().putBoolean("master_password_enabled", false).apply()
                masterPasswordEnabled = false
                showVerifyPasswordDialog = false
                Toast.makeText(context, "Protection Disabled", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ExpandableSection(title: String, initialExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun RoutineTemplatesSettingsCard(prefs: android.content.SharedPreferences) {
    var morningTitle by remember { mutableStateOf(prefs.getString("morning_template_title", "Morning Plan Objective") ?: "Morning Plan Objective") }
    var morningMinWords by remember { mutableStateOf(prefs.getInt("morning_template_min_words", 5).toString()) }

    // Section 1..6 configurations
    var sec0Title by remember { mutableStateOf(prefs.getString("morning_sec_0_title", "Review & Recaps") ?: "Review & Recaps") }
    var sec0Sub by remember { mutableStateOf(prefs.getString("morning_sec_0_sub", "Reflect on last night.") ?: "Reflect on last night.") }
    var sec0F1 by remember { mutableStateOf(prefs.getString("morning_sec_0_f1", "After Dinner Recap") ?: "After Dinner Recap") }
    var sec0F2 by remember { mutableStateOf(prefs.getString("morning_sec_0_f2", "Dreams / Night Thoughts") ?: "Dreams / Night Thoughts") }

    var sec1Title by remember { mutableStateOf(prefs.getString("morning_sec_1_title", "Intentions") ?: "Intentions") }
    var sec1Sub by remember { mutableStateOf(prefs.getString("morning_sec_1_sub", "Define your victories.") ?: "Define your victories.") }
    var sec1F1 by remember { mutableStateOf(prefs.getString("morning_sec_1_f1", "Personal Objective") ?: "Personal Objective") }
    var sec1F2 by remember { mutableStateOf(prefs.getString("morning_sec_1_f2", "Work Objective") ?: "Work Objective") }

    var sec2Title by remember { mutableStateOf(prefs.getString("morning_sec_2_title", "Anticipation") ?: "Anticipation") }
    var sec2Sub by remember { mutableStateOf(prefs.getString("morning_sec_2_sub", "Prepare for resistance.") ?: "Prepare for resistance.") }
    var sec2F1 by remember { mutableStateOf(prefs.getString("morning_sec_2_f1", "Potential Obstacles") ?: "Potential Obstacles") }
    var sec2F2 by remember { mutableStateOf(prefs.getString("morning_sec_2_f2", "My Solutions") ?: "My Solutions") }

    var sec3Title by remember { mutableStateOf(prefs.getString("morning_sec_3_title", "Spirit") ?: "Spirit") }
    var sec3Sub by remember { mutableStateOf(prefs.getString("morning_sec_3_sub", "Grit through gratitude.") ?: "Grit through gratitude.") }
    var sec3F1 by remember { mutableStateOf(prefs.getString("morning_sec_3_f1", "What am I grateful for?") ?: "What am I grateful for?") }
    var sec3F2 by remember { mutableStateOf(prefs.getString("morning_sec_3_f2", "What am I in awe of?") ?: "What am I in awe of?") }

    var sec4Title by remember { mutableStateOf(prefs.getString("morning_sec_4_title", "Growth") ?: "Growth") }
    var sec4Sub by remember { mutableStateOf(prefs.getString("morning_sec_4_sub", "Continuous learning.") ?: "Continuous learning.") }
    var sec4F1 by remember { mutableStateOf(prefs.getString("morning_sec_4_f1", "Read/Listen To") ?: "Read/Listen To") }

    var sec5Title by remember { mutableStateOf(prefs.getString("morning_sec_5_title", "Ideas & Solutions") ?: "Ideas & Solutions") }
    var sec5Sub by remember { mutableStateOf(prefs.getString("morning_sec_5_sub", "Brainstorm for the future.") ?: "Brainstorm for the future.") }

    var eveningTitle by remember { mutableStateOf(prefs.getString("evening_template_title", "Evening Reflection") ?: "Evening Reflection") }
    var eveningMinWords by remember { mutableStateOf(prefs.getInt("evening_template_min_words", 5).toString()) }
    var eveningQuestions by remember { mutableStateOf(prefs.getString("evening_template_questions", "How did I handle the Grit Trials today?\nWhat could I have improved?") ?: "How did I handle the Grit Trials today?\nWhat could I have improved?") }

    // Evening Section 0..7 configurations
    var evenSec0Title by remember { mutableStateOf(prefs.getString("evening_sec_0_title", "The Story") ?: "The Story") }
    var evenSec0Sub by remember { mutableStateOf(prefs.getString("evening_sec_0_sub", "What happened today?") ?: "What happened today?") }
    var evenSec0F1 by remember { mutableStateOf(prefs.getString("evening_sec_0_f1", "Daily Narrative") ?: "Daily Narrative") }

    var evenSec1Title by remember { mutableStateOf(prefs.getString("evening_sec_1_title", "Inventory") ?: "Inventory") }
    var evenSec1Sub by remember { mutableStateOf(prefs.getString("evening_sec_1_sub", "Own your wins.") ?: "Own your wins.") }
    var evenSec1F1 by remember { mutableStateOf(prefs.getString("evening_sec_1_f1", "Main Accomplishments") ?: "Main Accomplishments") }

    var evenSec2Title by remember { mutableStateOf(prefs.getString("evening_sec_2_title", "Analysis") ?: "Analysis") }
    var evenSec2Sub by remember { mutableStateOf(prefs.getString("evening_sec_2_sub", "3 Wins and 3 Losses.") ?: "3 Wins and 3 Losses.") }

    var evenSec3Title by remember { mutableStateOf(prefs.getString("evening_sec_3_title", "Bio-Metrics") ?: "Bio-Metrics") }
    var evenSec3Sub by remember { mutableStateOf(prefs.getString("evening_sec_3_sub", "Check your stats.") ?: "Check your stats.") }

    var evenSec4Title by remember { mutableStateOf(prefs.getString("evening_sec_4_title", "Peace of Mind") ?: "Peace of Mind") }
    var evenSec4Sub by remember { mutableStateOf(prefs.getString("evening_sec_4_sub", "Setup tomorrow.") ?: "Setup tomorrow.") }
    var evenSec4F1 by remember { mutableStateOf(prefs.getString("evening_sec_4_f1", "One thing for peace of mind") ?: "One thing for peace of mind") }

    var evenSec5Title by remember { mutableStateOf(prefs.getString("evening_sec_5_title", "Fuel") ?: "Fuel") }
    var evenSec5Sub by remember { mutableStateOf(prefs.getString("evening_sec_5_sub", "Log your meals.") ?: "Log your meals.") }
    var evenSec5F1 by remember { mutableStateOf(prefs.getString("evening_sec_5_f1", "Breakfast, Lunch, Dinner") ?: "Breakfast, Lunch, Dinner") }

    var evenSec6Title by remember { mutableStateOf(prefs.getString("evening_sec_6_title", "Evening Ideas") ?: "Evening Ideas") }
    var evenSec6Sub by remember { mutableStateOf(prefs.getString("evening_sec_6_sub", "Don't lose your insights.") ?: "Don't lose your insights.") }

    var evenSec7Title by remember { mutableStateOf(prefs.getString("evening_sec_7_title", "Quest Review") ?: "Quest Review") }
    var evenSec7Sub by remember { mutableStateOf(prefs.getString("evening_sec_7_sub", "How did you perform?") ?: "How did you perform?") }

    ExpandableSection("Morning & Evening Plan Templates", initialExpanded = false) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("☀️ Morning Plan Setup & Requirements", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            OutlinedTextField(
                value = morningTitle,
                onValueChange = { 
                    morningTitle = it
                    prefs.edit().putString("morning_template_title", it).apply()
                },
                label = { Text("Morning Title / Heading") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = morningMinWords,
                onValueChange = { 
                    morningMinWords = it
                    val v = it.toIntOrNull() ?: 5
                    prefs.edit().putInt("morning_template_min_words", v).apply()
                },
                label = { Text("Minimum Characters Required Per Input") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Text("📋 Customize 6 Morning Plan Sections", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)

            // Section 1 Editor
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 1: Recaps", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = sec0Title, onValueChange = { sec0Title = it; prefs.edit().putString("morning_sec_0_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec0Sub, onValueChange = { sec0Sub = it; prefs.edit().putString("morning_sec_0_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec0F1, onValueChange = { sec0F1 = it; prefs.edit().putString("morning_sec_0_f1", it).apply() }, label = { Text("Prompt 1 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec0F2, onValueChange = { sec0F2 = it; prefs.edit().putString("morning_sec_0_f2", it).apply() }, label = { Text("Prompt 2 Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Section 2 Editor
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 2: Intentions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = sec1Title, onValueChange = { sec1Title = it; prefs.edit().putString("morning_sec_1_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec1Sub, onValueChange = { sec1Sub = it; prefs.edit().putString("morning_sec_1_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec1F1, onValueChange = { sec1F1 = it; prefs.edit().putString("morning_sec_1_f1", it).apply() }, label = { Text("Prompt 1 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec1F2, onValueChange = { sec1F2 = it; prefs.edit().putString("morning_sec_1_f2", it).apply() }, label = { Text("Prompt 2 Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Section 3 Editor
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 3: Anticipation", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = sec2Title, onValueChange = { sec2Title = it; prefs.edit().putString("morning_sec_2_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec2Sub, onValueChange = { sec2Sub = it; prefs.edit().putString("morning_sec_2_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec2F1, onValueChange = { sec2F1 = it; prefs.edit().putString("morning_sec_2_f1", it).apply() }, label = { Text("Prompt 1 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec2F2, onValueChange = { sec2F2 = it; prefs.edit().putString("morning_sec_2_f2", it).apply() }, label = { Text("Prompt 2 Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Section 4 Editor
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 4: Spirit & Gratitude", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = sec3Title, onValueChange = { sec3Title = it; prefs.edit().putString("morning_sec_3_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec3Sub, onValueChange = { sec3Sub = it; prefs.edit().putString("morning_sec_3_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec3F1, onValueChange = { sec3F1 = it; prefs.edit().putString("morning_sec_3_f1", it).apply() }, label = { Text("Prompt 1 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec3F2, onValueChange = { sec3F2 = it; prefs.edit().putString("morning_sec_3_f2", it).apply() }, label = { Text("Prompt 2 Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Section 5 Editor
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 5: Growth", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = sec4Title, onValueChange = { sec4Title = it; prefs.edit().putString("morning_sec_4_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec4Sub, onValueChange = { sec4Sub = it; prefs.edit().putString("morning_sec_4_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec4F1, onValueChange = { sec4F1 = it; prefs.edit().putString("morning_sec_4_f1", it).apply() }, label = { Text("Prompt 1 Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Section 6 Editor
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 6: Brainstorming & Ideas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = sec5Title, onValueChange = { sec5Title = it; prefs.edit().putString("morning_sec_5_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sec5Sub, onValueChange = { sec5Sub = it; prefs.edit().putString("morning_sec_5_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

            Text("🌙 Evening Review Setup & Requirements", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            OutlinedTextField(
                value = eveningTitle,
                onValueChange = { 
                    eveningTitle = it
                    prefs.edit().putString("evening_template_title", it).apply()
                },
                label = { Text("Evening Title / Heading") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = eveningMinWords,
                onValueChange = { 
                    eveningMinWords = it
                    val v = it.toIntOrNull() ?: 5
                    prefs.edit().putInt("evening_template_min_words", v).apply()
                },
                label = { Text("Minimum Characters Required Per Input") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Text("📋 Customize Evening Review Sections", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)

            // Evening Section 0
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 1: The Story", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec0Title, onValueChange = { evenSec0Title = it; prefs.edit().putString("evening_sec_0_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec0Sub, onValueChange = { evenSec0Sub = it; prefs.edit().putString("evening_sec_0_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec0F1, onValueChange = { evenSec0F1 = it; prefs.edit().putString("evening_sec_0_f1", it).apply() }, label = { Text("Prompt Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Evening Section 1
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 2: Inventory", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec1Title, onValueChange = { evenSec1Title = it; prefs.edit().putString("evening_sec_1_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec1Sub, onValueChange = { evenSec1Sub = it; prefs.edit().putString("evening_sec_1_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec1F1, onValueChange = { evenSec1F1 = it; prefs.edit().putString("evening_sec_1_f1", it).apply() }, label = { Text("Prompt Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Evening Section 2
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 3: Analysis (Wins & Losses)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec2Title, onValueChange = { evenSec2Title = it; prefs.edit().putString("evening_sec_2_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec2Sub, onValueChange = { evenSec2Sub = it; prefs.edit().putString("evening_sec_2_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Evening Section 3
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 4: Bio-Metrics", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec3Title, onValueChange = { evenSec3Title = it; prefs.edit().putString("evening_sec_3_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec3Sub, onValueChange = { evenSec3Sub = it; prefs.edit().putString("evening_sec_3_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Evening Section 4
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 5: Peace of Mind", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec4Title, onValueChange = { evenSec4Title = it; prefs.edit().putString("evening_sec_4_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec4Sub, onValueChange = { evenSec4Sub = it; prefs.edit().putString("evening_sec_4_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec4F1, onValueChange = { evenSec4F1 = it; prefs.edit().putString("evening_sec_4_f1", it).apply() }, label = { Text("Prompt Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Evening Section 5
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 6: Fuel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec5Title, onValueChange = { evenSec5Title = it; prefs.edit().putString("evening_sec_5_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec5Sub, onValueChange = { evenSec5Sub = it; prefs.edit().putString("evening_sec_5_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec5F1, onValueChange = { evenSec5F1 = it; prefs.edit().putString("evening_sec_5_f1", it).apply() }, label = { Text("Prompt Label") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Evening Section 6
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 7: Evening Ideas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec6Title, onValueChange = { evenSec6Title = it; prefs.edit().putString("evening_sec_6_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec6Sub, onValueChange = { evenSec6Sub = it; prefs.edit().putString("evening_sec_6_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Evening Section 7
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Section 8: Quest Review", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = evenSec7Title, onValueChange = { evenSec7Title = it; prefs.edit().putString("evening_sec_7_title", it).apply() }, label = { Text("Section Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = evenSec7Sub, onValueChange = { evenSec7Sub = it; prefs.edit().putString("evening_sec_7_sub", it).apply() }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                }
            }
            OutlinedTextField(
                value = eveningQuestions,
                onValueChange = { 
                    eveningQuestions = it
                    prefs.edit().putString("evening_template_questions", it).apply()
                },
                label = { Text("Evening Prompts & Questions (one per line)") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
    }
}

@Composable
fun DailyGoalItem(type: ExerciseType, prefs: android.content.SharedPreferences) {
    var goalText by remember { mutableStateOf(prefs.getInt("goal_${type.name}", 10).toString()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(type.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = goalText,
            onValueChange = { 
                goalText = it
                val v = it.toIntOrNull() ?: 10
                prefs.edit().putInt("goal_${type.name}", v).apply()
            },
            modifier = Modifier.width(90.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    description: String,
    isEnabled: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = Color.Gray, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) Color(0xFF81C784).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                contentColor = if (isEnabled) Color(0xFF81C784) else Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(if (isEnabled) "Active" else "Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RepBankSettingsCard(userStats: UserStats?, onUpdateUserStats: (UserStats) -> Unit, prefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    var banked = userStats?.bankedReps?.values?.sum() ?: 0
    var clearanceFreq by remember { mutableStateOf(prefs.getString("rep_bank_frequency", "Weekly") ?: "Weekly") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Total Banked Reps Available: $banked", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text("Banked reps let you bypass locks without doing immediate exercises.", color = Color.Gray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(4.dp))
        Text("Rep Bank Clearance Frequency", color = Color.Gray, fontSize = 11.sp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Daily", "Weekly", "Monthly", "Never").forEach { freq ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        clearanceFreq = freq
                        prefs.edit().putString("rep_bank_frequency", freq).apply()
                        Toast.makeText(context, "Clearance Frequency set to $freq", Toast.LENGTH_SHORT).show()
                    }.padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = clearanceFreq == freq,
                        onClick = {
                            clearanceFreq = freq
                            prefs.edit().putString("rep_bank_frequency", freq).apply()
                            Toast.makeText(context, "Clearance Frequency set to $freq", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(freq, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (userStats != null) {
                    val cleared = userStats.copy(bankedReps = emptyMap())
                    onUpdateUserStats(cleared)
                    Toast.makeText(context, "Rep Bank Cleared", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Rep Bank")
        }
    }
}

@Composable
fun ExerciseSettingsCard(prefs: android.content.SharedPreferences, userStats: UserStats?, onNavigateToCalibration: (ExerciseType) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Exercise Calibration & Detection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text("Calibrate pushups and squats for high accuracy in pocket and camera modes.", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNavigateToCalibration(ExerciseType.PUSHUP) }, modifier = Modifier.weight(1f)) {
                    Text("Calibrate Pushup", fontSize = 11.sp)
                }
                Button(onClick = { onNavigateToCalibration(ExerciseType.SQUAT) }, modifier = Modifier.weight(1f)) {
                    Text("Calibrate Squat", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun GemmaAiSettingsCard() {
    val context = LocalContext.current
    var aiStatus by remember { mutableStateOf("Ready") }
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gemma 2B / Nano On-Device AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text("Provides intelligent lock prompts and motivational coaching locally.", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Status: $aiStatus", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    aiStatus = "Checking / Initializing..."
                    scope.launch {
                        val manager = Gemma4Manager(context)
                        manager.initialize()
                        aiStatus = if (manager.isInitialized) "Active & Initialized" else "Initialized"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Initialize AI Engine")
            }
        }
    }
}

@Composable
fun TrackingModeCard(trackingMode: TrackingMode, onTrackingModeChange: (TrackingMode) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Default Mode", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = trackingMode == TrackingMode.POCKET,
                        onClick = { onTrackingModeChange(TrackingMode.POCKET) }
                    )
                    Text("Pocket", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = trackingMode == TrackingMode.CAMERA,
                        onClick = { onTrackingModeChange(TrackingMode.CAMERA) }
                    )
                    Text("Camera", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StatReferenceGuideCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The 12 Core RPG Stats & Real-Life Drivers", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Text("Every action in FitLock trains your attributes across 12 key domains:", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            val statsInfo = listOf(
                "STR (Strength)" to "Raw muscle & resistance (heavy lifting, resistance volume).",
                "DEX (Dexterity)" to "Motor control & form precision (bodyweight, balance).",
                "CON (Constitution)" to "Recovery & baseline health (sleep quality, cardio/heart rate).",
                "MOV (Movement)" to "Locomotive endurance (running pace, active daily mobility).",
                "INT (Intelligence)" to "Memory & knowledge (cognitive training, deep learning).",
                "WIS (Wisdom)" to "Self-awareness & insight (journaling, reflections).",
                "WIL (Willpower)" to "Focus & impulse control (app-blocker adherence, deep work).",
                "SAN (Sanity)" to "Mental calm & resilience (stress recovery, mindfulness).",
                "CHA (Charisma)" to "Social connection & output (communication, leadership).",
                "WTH (Wealth)" to "Financial leverage (budgeting, net worth growth).",
                "TEC (Technique)" to "Systems & automation (software setups, tool mastery).",
                "LCK (Luck)" to "Compound momentum (derived meta-stat powered by multi-day habit streaks)."
            )
            statsInfo.forEach { (stat, desc) ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text("• $stat: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    Text(desc, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HowTheAppWorksCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("How FitLock Works", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Text("FitLock bridges digital self-control with real-life physical & mental execution:", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            val explanationPoints = listOf(
                "1. Universal Protocol Gates" to "Combine App Blocks, Routines, Challenges, and Positive Portals. Triggered manually, by time schedules, WiFi, app opening, or keywords.",
                "2. Dynamic Requirements" to "Fulfill physical exercises (pushups, squats, breathwork via camera or pocket ML), hydration (water), nutrition (calories), journaling (writing words), or system to-do checklists.",
                "3. XP & 12-Stat Growth" to "Every rep, journal entry, and cleared gate rewards XP and attributes across 12 core RPG attributes.",
                "4. Hall of Ascendance" to "Progress through multi-tiered achievements (Bronze to Legend) across all activities over time."
            )
            explanationPoints.forEach { (title, desc) ->
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    Text(desc, fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun BackupRestoreSettingsCard(prefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { DataBackupManager(context) }
    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val dateStr = remember { SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                val (success, message) = backupManager.exportToFile(uri)
                isProcessing = false
                statusMessage = message
                Toast.makeText(context, if (success) "Export Successful!" else "Export Failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                val (success, message) = backupManager.importFromFile(uri)
                isProcessing = false
                statusMessage = message
                Toast.makeText(context, if (success) "Restore Successful!" else "Restore Failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    ExpandableSection("💾 Full System Backup & Restore (Export / Import)", initialExpanded = true) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Export or restore all app data including protocol gates, workout history, daily pledges, quests, flashcards, OKRs, goals, daily log notes, and custom templates into a portable JSON backup file.",
                color = Color.Gray,
                fontSize = 12.sp
            )

            if (isProcessing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Processing backup file...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (statusMessage.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch("fitlock_backup_$dateStr.json") },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export (JSON)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { importLauncher.launch("*/*") },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore (JSON)", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun WorldLoreOriginCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The Origin of The Gress Engine", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Text(
                "In the digital era, an ancient force known as 'The Distraction Void' consumed humanity's willpower—drowning focus in endless doomscrolling and dopamine traps. " +
                "To fight back, a secret order of seeker engineers constructed 'The Gress Engine' (FitLock), a neural discipline lattice that seals away mindless portals. " +
                "The only way to shatter a lock is through blood, sweat, and truth: completing real-life physical repetitions, mindful breathwork, hydration, and deep daily reflections. " +
                "By forging your body and mind through Universal Protocol Gates, you awaken your 12 Divine RPG Attributes and ascend to true digital sovereignty.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/com.example.fitlock.service.GritLockAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(service, ignoreCase = true) == true
}

fun checkAndNotifyAccessibilityService(context: Context) {
    val isEnabled = isAccessibilityServiceEnabled(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "gresslock_warning_channel"

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(channelId, "GressLock System Warnings", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notifies when Accessibility Service is disabled"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val warningNotificationId = 9901

    if (!isEnabled) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.fitlock.R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ GressLock Service Disabled")
            .setContentText("Accessibility Service is disabled! Tap to enable service and restore app protection.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(warningNotificationId, notification)
    } else {
        notificationManager.cancel(warningNotificationId)
    }
}
