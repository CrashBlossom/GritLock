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
import androidx.compose.material.icons.filled.*
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
import com.example.fitlock.data.UserStats
import com.example.fitlock.data.ExerciseCalibration
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.exercise.TrackingMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    trackingMode: TrackingMode,
    onTrackingModeChange: (TrackingMode) -> Unit,
    userStats: UserStats?,
    onUpdateUserStats: (UserStats) -> Unit,
    onNavigateToCalibration: (ExerciseType) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    
    // Permission states
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isOverlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    // Customization states
    var selectedTheme by remember { mutableStateOf(prefs.getString("app_theme", "Default") ?: "Default") }
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", true)) }
    var customBankMsg by remember { mutableStateOf(prefs.getString("custom_bank_msg", "No reps banked yet. Go sweat!") ?: "No reps banked yet. Go sweat!") }

    // Lockout delay
    var lockoutSeconds by remember { mutableIntStateOf(prefs.getInt("lockout_seconds", 10)) }

    // Strict Mode state
    var strictMode by remember { mutableStateOf(prefs.getBoolean("strict_mode", false)) }

    // Refresh permissions
    LaunchedEffect(Unit) {
        while(true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            isOverlayPermissionGranted = Settings.canDrawOverlays(context)
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
        
        Spacer(modifier = Modifier.height(32.dp))

        // System Permissions Status Section
        SectionHeader("System Permissions")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Goals Section
        SectionHeader("Daily Goals")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseType.values().forEach { type ->
                    DailyGoalItem(type, prefs)
                    if (type != ExerciseType.values().last()) {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Behavior Section
        SectionHeader("Behavior")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Lockout Countdown", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Seconds before app is blocked: $lockoutSeconds", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Customization Section
        SectionHeader("Customization")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { 
                            isDarkMode = it
                            prefs.edit().putBoolean("is_dark_mode", it).apply()
                        }
                    )
                }
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                Text("App Color", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeOption("Default", Color(0xFFD0BCFF), selectedTheme == "Default") {
                        selectedTheme = "Default"
                        prefs.edit().putString("app_theme", "Default").apply()
                    }
                    ThemeOption("Emerald", Color(0xFF50C878), selectedTheme == "Emerald") {
                        selectedTheme = "Emerald"
                        prefs.edit().putString("app_theme", "Emerald").apply()
                    }
                    ThemeOption("Crimson", Color(0xFFDC143C), selectedTheme == "Crimson") {
                        selectedTheme = "Crimson"
                        prefs.edit().putString("app_theme", "Crimson").apply()
                    }
                    ThemeOption("Ocean", Color(0xFF0077BE), selectedTheme == "Ocean") {
                        selectedTheme = "Ocean"
                        prefs.edit().putString("app_theme", "Ocean").apply()
                    }
                }
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 16.dp))
                
                Text("Motivational Message", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customBankMsg,
                    onValueChange = { 
                        customBankMsg = it
                        prefs.edit().putString("custom_bank_msg", it).apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("No reps banked yet. Go sweat!") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Health Sync Section
        SectionHeader("Health Connectivity")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Health Connect Sync", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Sync your progress with Android Health Connect.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = Intent("androidx.health.connect.client.ACTION_HEALTH_CONNECT_SETTINGS")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Health Connect not installed or restricted.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Permissions & Sync")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Protection Section
        SectionHeader("Protection & Anti-Cheat")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Mode", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Prevents disabling GritLock in system settings.", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = strictMode,
                        onCheckedChange = { 
                            strictMode = it
                            prefs.edit().putBoolean("strict_mode", it).apply()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rep Bank Management Section
        SectionHeader("Rep Bank Management")
        RepBankSettingsCard(userStats, onUpdateUserStats)

        Spacer(modifier = Modifier.height(24.dp))

        // Exercise Management Section
        SectionHeader("Exercise Management")
        ExerciseSettingsCard(prefs, userStats, onNavigateToCalibration)

        Spacer(modifier = Modifier.height(24.dp))

        // Tracking Section
        SectionHeader("Tracking")
        TrackingModeCard(trackingMode, onTrackingModeChange)
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DailyGoalItem(type: ExerciseType, prefs: android.content.SharedPreferences) {
    var goalText by remember { mutableStateOf(prefs.getInt("goal_${type.name}", 10).toString()) }
    val unit = if (type == ExerciseType.PLANK || type == ExerciseType.APP_USAGE) "s" else ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            type.name.lowercase().replaceFirstChar { it.uppercase() },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = goalText,
                onValueChange = { newValue ->
                    if (newValue.all { char -> char.isDigit() }) {
                        goalText = newValue
                        if (newValue.isNotEmpty()) {
                            try {
                                prefs.edit().putInt("goal_${type.name}", newValue.toInt()).apply()
                            } catch (e: Exception) {}
                        }
                    }
                },
                modifier = Modifier.width(80.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp, 
                    textAlign = TextAlign.Center, 
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                )
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, color = Color.Gray, fontSize = 12.sp)
            }
        }
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(description, color = Color.Gray, fontSize = 12.sp)
        }
        
        if (!isEnabled) {
            TextButton(onClick = onGrant) {
                Text("ACTIVATE")
            }
        } else {
            Icon(Icons.Default.Verified, contentDescription = "Active", tint = Color(0xFF4CAF50).copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ThemeOption(name: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun RepBankSettingsCard(userStats: UserStats?, onUpdate: (UserStats) -> Unit) {
    if (userStats == null) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bank Reset Frequency", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("Controls how often your banked reps are cleared.", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val frequencies = listOf("Daily", "Weekly", "Monthly", "Never")
            var expanded by remember { mutableStateOf(false) }
            
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(userStats.bankResetFrequency, color = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    frequencies.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                onUpdate(userStats.copy(bankResetFrequency = freq))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseSettingsCard(
    prefs: android.content.SharedPreferences,
    userStats: UserStats?,
    onNavigateToCalibration: (ExerciseType) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ExerciseType.values().forEach { type ->
                ExerciseTypeSettingsItem(type, prefs, userStats, onNavigateToCalibration)
                if (type != ExerciseType.values().last()) {
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
fun ExerciseTypeSettingsItem(
    type: ExerciseType,
    prefs: android.content.SharedPreferences,
    userStats: UserStats?,
    onNavigateToCalibration: (ExerciseType) -> Unit
) {
    var isVisible by remember { mutableStateOf(prefs.getBoolean("visible_${type.name}", true)) }
    
    val isCalibrated = userStats?.calibrations?.containsKey(type.name) ?: false

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            
            if (isCalibrated) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Calibrated", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            IconButton(onClick = { onNavigateToCalibration(type) }) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Calibrate", tint = if (isCalibrated) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
            }

            Switch(
                checked = isVisible,
                onCheckedChange = { 
                    isVisible = it
                    prefs.edit().putBoolean("visible_${type.name}", it).apply()
                }
            )
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
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/com.example.fitlock.service.GritLockAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(service) == true
}
