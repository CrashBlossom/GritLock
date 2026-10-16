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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.*
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.utils.AppInfoFetcher

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProtocolGateBuilderScreen(
    editingGate: AppGroup? = null,
    deckNames: List<String> = emptyList(),
    habitDefinitions: List<HabitDefinition> = emptyList(),
    onBack: () -> Unit,
    onSave: (AppGroup) -> Unit
) {
    val context = LocalContext.current
    var gateName by remember(editingGate) { mutableStateOf(editingGate?.name ?: "") }
    var gateCategory by remember(editingGate) { 
        mutableStateOf(
            when {
                editingGate?.isRoutine == true -> "ROUTINE"
                editingGate?.isProductive == true -> "POSITIVE_PORTAL"
                editingGate?.gateMode == GateMode.BACKGROUND_RAID -> "CHALLENGE"
                else -> "APP_BLOCK"
            }
        ) 
    }
    
    // Execution mode automatically determined by Gate Category
    val gateMode = remember(gateCategory) {
        when (gateCategory) {
            "CHALLENGE", "POSITIVE_PORTAL" -> GateMode.BACKGROUND_RAID
            else -> GateMode.BLOCK_SCREEN
        }
    }

    var triggerType by remember(editingGate) { mutableStateOf(editingGate?.triggerType ?: GauntletTriggerType.MANUAL) }
    var triggerTime by remember(editingGate) { mutableStateOf(editingGate?.triggerTime ?: "") }
    
    var selectedApps by remember(editingGate) { mutableStateOf(editingGate?.packageNames?.toSet() ?: setOf()) }
    var searchQuery by remember { mutableStateOf("") }

    var exerciseRequirements by remember(editingGate) { 
        mutableStateOf(editingGate?.exercises ?: listOf(ExerciseRequirement("PUSHUP", 10))) 
    }
    var unlockDuration by remember(editingGate) { mutableStateOf(editingGate?.unlockDurationMinutes?.toString() ?: "30") }
    var xpReward by remember(editingGate) { mutableStateOf(editingGate?.xpReward?.toString() ?: "100") }

    // Keywords state
    var keywords by remember(editingGate) { mutableStateOf(editingGate?.keywords ?: emptyList()) }
    var newKeyword by remember { mutableStateOf("") }

    // Constraints (WiFi, Time Window, Days of Week & Deadline)
    var restrictedWifiSsids by remember(editingGate) { mutableStateOf(editingGate?.restrictedWifiSsids ?: emptyList()) }
    var newWifiSsid by remember { mutableStateOf("") }
    var showWifiScanner by remember { mutableStateOf(false) }
    val initialTimeParts = remember(editingGate) {
        val scheduleFirst = editingGate?.schedule?.firstOrNull()
        if (scheduleFirst != null) {
            val startH = String.format(java.util.Locale.getDefault(), "%02d:%02d", scheduleFirst.startMinute / 60, scheduleFirst.startMinute % 60)
            val endH = String.format(java.util.Locale.getDefault(), "%02d:%02d", scheduleFirst.endMinute / 60, scheduleFirst.endMinute % 60)
            Pair(startH, endH)
        } else {
            val t = editingGate?.triggerTime ?: "00:00-23:59"
            if (t.contains("-")) {
                val parts = t.split("-")
                Pair(parts.getOrNull(0) ?: "00:00", parts.getOrNull(1) ?: "23:59")
            } else {
                Pair(t.ifBlank { "00:00" }, "23:59")
            }
        }
    }
    var startTimeConstraint by remember(editingGate) { mutableStateOf(initialTimeParts.first) }
    var endTimeConstraint by remember(editingGate) { mutableStateOf(initialTimeParts.second) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) } // 1=Mon, 7=Sun
    var targetDeadline by remember(editingGate) { mutableStateOf(editingGate?.targetDeadline ?: "") }

    var showTriggerTimePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Rewards & Multi-Tier Stat Rewards
    var primaryRewardStat by remember(editingGate) { mutableStateOf(editingGate?.rewardStat ?: StatType.INT) }
    var secondaryStats by remember(editingGate) { mutableStateOf(editingGate?.secondaryStats?.toSet() ?: setOf(StatType.SAN, StatType.CHA)) }
    
    var secretPassword by remember(editingGate) { mutableStateOf(editingGate?.secretPassword ?: "") }
    var secretLink by remember(editingGate) { mutableStateOf(editingGate?.secretLink ?: "") }

    val allApps = remember { AppInfoFetcher.getInstalledApps(context) }
    val filteredApps = allApps.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    val gateThemeColor = when (gateCategory) {
        "APP_BLOCK" -> Color(0xFFAB47BC)
        "ROUTINE" -> Color(0xFFFFB74D)
        "CHALLENGE" -> Color(0xFFEF5350)
        "POSITIVE_PORTAL" -> Color(0xFF81C784)
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingGate == null) "Create Protocol Gate" else "Edit Protocol Gate", color = Color.White) },
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
            // Header Theme Accent Card
            Card(
                colors = CardDefaults.cardColors(containerColor = gateThemeColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CONFIGURATION HUB: $gateCategory",
                        color = gateThemeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when(gateCategory) {
                            "APP_BLOCK" -> "Locks target apps & keywords until requirements are fulfilled."
                            "ROUTINE" -> "Sequenced habit pipeline triggered by time, WiFi, or manual start."
                            "CHALLENGE" -> "Background raid tracking cumulative milestones and consistency."
                            "POSITIVE_PORTAL" -> "Grants XP & stat points per minute of productive app usage."
                            else -> "Unified Protocol Gate Engine"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gate Name
            OutlinedTextField(
                value = gateName,
                onValueChange = { gateName = it },
                label = { Text("Gate Name (e.g. Focus Shield, Morning Ritual)") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C1C21),
                    unfocusedContainerColor = Color(0xFF1C1C21),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Gate Category Selection
            Text("1. GATE TYPE", color = gateThemeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "APP_BLOCK" to "🔒 App Block",
                    "ROUTINE" to "☀️ Routine",
                    "CHALLENGE" to "⚔️ Challenge",
                    "POSITIVE_PORTAL" to "🟢 Positive Portal"
                ).forEach { (cat, label) ->
                    FilterChip(
                        selected = gateCategory == cat,
                        onClick = { gateCategory = cat },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Gate Execution Mode (Auto-Assigned Read-Only)
            Text("2. EXECUTION MODE (AUTO-ASSIGNED)", color = gateThemeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text(if (gateMode == GateMode.BLOCK_SCREEN) "🔒 Block Screen Overlay (Active Intercept)" else "⚔️ Background Raid (Continuous Tracking)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = gateThemeColor.copy(alpha = 0.3f), selectedLabelColor = Color.White)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Triggers Section (3.1, 3.2, 3.3)
            Text("3. TRIGGERS", color = gateThemeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text("3.1 Trigger Mechanism", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("AUTOMATIC", "MANUAL", "TIME_BASED", "WIFI").forEach { trigName ->
                    val isSel = (trigName == "AUTOMATIC" && triggerType == GauntletTriggerType.MANUAL && (gateCategory == "CHALLENGE" || gateCategory == "POSITIVE_PORTAL")) || triggerType.name == trigName
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            triggerType = when(trigName) {
                                "TIME_BASED" -> GauntletTriggerType.TIME_BASED
                                "WIFI" -> GauntletTriggerType.WIFI
                                else -> GauntletTriggerType.MANUAL
                            }
                        },
                        label = { Text(if (trigName == "AUTOMATIC") "⚡ Automatic (24/7)" else trigName, fontSize = 11.sp) }
                    )
                }
            }

            if (triggerType == GauntletTriggerType.TIME_BASED || triggerType == GauntletTriggerType.TIME_AND_WIFI) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (triggerTime.isBlank()) "Tap to select time..." else triggerTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Trigger Time (Clock Dial)") },
                    trailingIcon = {
                        IconButton(onClick = { showTriggerTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = gateThemeColor)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTriggerTimePicker = true },
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3.2 Target Apps
            Text("3.2 Target Apps (${selectedApps.size} Selected)", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search installed apps...", color = Color.Gray) },
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
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(180.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredApps.forEach { app ->
                        val isSelected = selectedApps.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedApps = if (isSelected) selectedApps - app.packageName else selectedApps + app.packageName
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.name, color = Color.White, fontSize = 12.sp)
                                Text(app.packageName, color = Color.Gray, fontSize = 8.sp)
                            }
                        }
                        HorizontalDivider(color = Color(0xFF25252B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3.3 Keywords & URL Terms
            Text("3.3 Keywords & URL Terms (${keywords.size})", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    placeholder = { Text("e.g. youtube.com, gambling", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newKeyword.isNotBlank()) {
                        keywords = keywords + newKeyword.trim()
                        newKeyword = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Keyword", tint = gateThemeColor)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                keywords.forEach { kw ->
                    AssistChip(
                        onClick = { keywords = keywords - kw },
                        label = { Text(kw, fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Constraints (WiFi, Days of Week, Time Window & Deadline)
            Text("4. CONSTRAINTS (WIFI, SCHEDULE & DEADLINE)", color = gateThemeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Days of Week Constraint", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun").forEach { (dayNum, dayLabel) ->
                    FilterChip(
                        selected = selectedDays.contains(dayNum),
                        onClick = {
                            selectedDays = if (selectedDays.contains(dayNum)) selectedDays - dayNum else selectedDays + dayNum
                        },
                        label = { Text(dayLabel, fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("WiFi SSIDs Constraint", color = Color.Gray, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newWifiSsid,
                    onValueChange = { newWifiSsid = it },
                    placeholder = { Text("e.g. Home_WiFi, Office_5G", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showWifiScanner = true }) {
                    Icon(Icons.Default.Wifi, contentDescription = "Scan WiFi", tint = gateThemeColor)
                }
                IconButton(onClick = {
                    if (newWifiSsid.isNotBlank()) {
                        restrictedWifiSsids = restrictedWifiSsids + newWifiSsid.trim()
                        newWifiSsid = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add SSID", tint = gateThemeColor)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                restrictedWifiSsids.forEach { ssid ->
                    AssistChip(
                        onClick = { restrictedWifiSsids = restrictedWifiSsids - ssid },
                        label = { Text(ssid, fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Active Time Window Constraint", color = Color.Gray, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startTimeConstraint,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Time") },
                    trailingIcon = {
                        IconButton(onClick = { showStartTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Pick Start Time", tint = gateThemeColor)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartTimePicker = true },
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = endTimeConstraint,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Time") },
                    trailingIcon = {
                        IconButton(onClick = { showEndTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Pick End Time", tint = gateThemeColor)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndTimePicker = true },
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Target Deadline Date (Optional)", color = Color.Gray, fontSize = 11.sp)
            OutlinedTextField(
                value = targetDeadline,
                onValueChange = { targetDeadline = it },
                label = { Text("Target Deadline (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 7. Requirements Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "7. REQUIREMENTS (${exerciseRequirements.size})",
                    color = gateThemeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { 
                    exerciseRequirements = exerciseRequirements + ExerciseRequirement("PUSHUP", 10) 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Requirement", tint = gateThemeColor)
                }
            }

            exerciseRequirements.forEachIndexed { index, req ->
                RequirementRow(
                    index = index,
                    req = req,
                    onUpdate = { newReq ->
                        exerciseRequirements = exerciseRequirements.toMutableList().apply { this[index] = newReq }
                    },
                    onDelete = {
                        if (exerciseRequirements.size > 1) {
                            exerciseRequirements = exerciseRequirements.toMutableList().apply { removeAt(index) }
                        }
                    },
                    deckNames = deckNames,
                    habitDefinitions = habitDefinitions
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Universal Rewards & Outcomes Section
            Text("8. UNIVERSAL REWARDS & STAT ALLOCATION", color = gateThemeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("XP Reward:", color = Color.White, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = xpReward,
                    onValueChange = { xpReward = it },
                    modifier = Modifier.width(120.dp),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Primary Stat Reward:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatType.entries.forEach { stat ->
                    FilterChip(
                        selected = primaryRewardStat == stat,
                        onClick = {
                            primaryRewardStat = if (primaryRewardStat == stat) StatType.NONE else stat
                        },
                        label = { Text(stat.name, fontSize = 9.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Secondary Stat Rewards (Multi-Select):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatType.entries.forEach { stat ->
                    FilterChip(
                        selected = secondaryStats.contains(stat),
                        onClick = {
                            secondaryStats = if (secondaryStats.contains(stat)) secondaryStats - stat else secondaryStats + stat
                        },
                        label = { Text(stat.name, fontSize = 9.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Secret Vault Release (Optional):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = secretPassword,
                onValueChange = { secretPassword = it },
                label = { Text("Secret Vault Password / Note") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = secretLink,
                onValueChange = { secretLink = it },
                label = { Text("Secret Link URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            // 9. Relock Duration Section (Hidden for Challenges & Positive Portals)
            if (gateCategory != "CHALLENGE" && gateCategory != "POSITIVE_PORTAL") {
                Spacer(modifier = Modifier.height(20.dp))
                Text("9. RELOCK DURATION", color = gateThemeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Duration before gate relocks (min):", color = Color.White, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = unlockDuration,
                        onValueChange = { unlockDuration = it },
                        modifier = Modifier.width(120.dp),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalName = if (gateName.isNotBlank()) gateName.trim() else "Protocol Gate ${System.currentTimeMillis() % 1000}"
                    val isProd = gateCategory == "POSITIVE_PORTAL"
                    val isRout = gateCategory == "ROUTINE"

                    val timeStrToParse = if (triggerType == GauntletTriggerType.TIME_BASED && triggerTime.isNotBlank()) triggerTime else startTimeConstraint
                    val startMin = parseTimeToMinutes(timeStrToParse, 0)
                    val endMin = parseTimeToMinutes(endTimeConstraint, 1439)
                    val scheduleIntervals = selectedDays.map { day ->
                        ScheduleInterval(dayOfWeek = day, startMinute = startMin, endMinute = endMin)
                    }

                    onSave(AppGroup(
                        id = editingGate?.id ?: 0,
                        name = finalName,
                        packageNames = selectedApps.toList(),
                        exercises = exerciseRequirements,
                        unlockDurationMinutes = unlockDuration.toIntOrNull() ?: 30,
                        restrictedWifiSsids = restrictedWifiSsids,
                        keywords = keywords,
                        isEnabled = editingGate?.isEnabled ?: true,
                        isProductive = isProd,
                        rewardStat = primaryRewardStat,
                        secondaryStats = secondaryStats.toList(),
                        isRoutine = isRout,
                        schedule = scheduleIntervals,
                        triggerType = triggerType,
                        triggerTime = timeStrToParse,
                        xpReward = xpReward.toIntOrNull() ?: 100,
                        gateMode = gateMode,
                        secretPassword = secretPassword.ifBlank { null },
                        secretLink = secretLink.ifBlank { null },
                        targetDeadline = targetDeadline.ifBlank { null }
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gateThemeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (editingGate == null) "Forge Protocol Gate" else "Update Protocol Gate", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWifiScanner) {
        GateWifiScannerDialog(
            onDismiss = { showWifiScanner = false },
            onSelect = { ssid: String ->
                if (!restrictedWifiSsids.contains(ssid)) {
                    restrictedWifiSsids = restrictedWifiSsids + ssid
                }
                showWifiScanner = false
            }
        )
    }

    if (showTriggerTimePicker) {
        GateTimePickerDialog(
            title = "Select Trigger Time",
            initialTime = triggerTime.ifBlank { "16:30" },
            onDismiss = { showTriggerTimePicker = false },
            onTimeSelected = { time ->
                triggerTime = time
                startTimeConstraint = time
                showTriggerTimePicker = false
            }
        )
    }

    if (showStartTimePicker) {
        GateTimePickerDialog(
            title = "Select Start Time Constraint",
            initialTime = startTimeConstraint.ifBlank { "00:00" },
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { time ->
                startTimeConstraint = time
                triggerTime = time
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        GateTimePickerDialog(
            title = "Select End Time Constraint",
            initialTime = endTimeConstraint.ifBlank { "23:59" },
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { time ->
                endTimeConstraint = time
                showEndTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateTimePickerDialog(
    title: String = "Select Time",
    initialTime: String = "08:00",
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val formattedTime = java.lang.String.format(java.util.Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                onTimeSelected(formattedTime)
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun parseTimeToMinutes(timeStr: String, defaultVal: Int): Int {
    val parts = timeStr.trim().split(":")
    if (parts.size == 2) {
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }
    return defaultVal
}

@Composable
fun GateWifiScannerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    var nearbySsids by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        nearbySsids = com.example.fitlock.utils.NetworkUtils.getNearbySsids(context)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Available Networks") },
        text = {
            Column(modifier = Modifier.height(300.dp)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (nearbySsids.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No nearby WiFi found. Check permissions.", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn {
                        items(nearbySsids) { ssid ->
                            ListItem(
                                headlineContent = { Text(ssid) },
                                modifier = Modifier.clickable { onSelect(ssid) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequirementRow(
    index: Int,
    req: ExerciseRequirement,
    onUpdate: (ExerciseRequirement) -> Unit,
    onDelete: () -> Unit,
    deckNames: List<String>,
    habitDefinitions: List<HabitDefinition>
) {
    var expanded by remember { mutableStateOf(false) }
    val standardTypes = listOf(
        "PUSHUP" to "Pushups (Reps)",
        "SQUAT" to "Squats (Reps)",
        "PLANK" to "Plank (Seconds)",
        "BREATHWORK" to "Breathwork (Seconds)",
        "SITUP" to "Situps (Reps)",
        "PULLUP" to "Pullups (Reps)",
        "DIP" to "Dips (Reps)",
        "ROW" to "Row (Reps)",
        "HINGE" to "Hinge (Reps)",
        "APP_USAGE" to "App Usage Duration (Seconds)",
        "WATER" to "Water (Glasses/Liters)",
        "CALORIES" to "Calories (Kcal)",
        "WRITE_WORDS" to "Writing Words (Min Words)",
        "COMPLETED_TODO" to "System To-Do Checklist"
    )
    val habitNames = habitDefinitions.map { it.name }
    val allOptions = standardTypes.map { it.first } + habitNames

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Requirement ${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = req.type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Requirement Type / Exercise / Habit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    allOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onUpdate(req.copy(type = option))
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = req.count.toString(),
                onValueChange = { onUpdate(req.copy(count = it.toIntOrNull() ?: 10)) },
                label = { Text(if (req.type == "PLANK" || req.type == "BREATHWORK") "Target Seconds" else if (req.type == "WRITE_WORDS") "Minimum Words" else "Target Reps / Value") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
    }
}
