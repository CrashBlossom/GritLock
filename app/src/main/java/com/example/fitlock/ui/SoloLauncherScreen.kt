package com.example.fitlock.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.LockOverlayActivity
import com.example.fitlock.data.*
import com.example.fitlock.utils.AppInfoFetcher

@Composable
fun SoloLauncherScreen(
    userStats: UserStats?,
    quests: List<QuestWithBlocks>,
    appGroups: List<AppGroup>,
    challenges: List<Challenge>,
    currentPledge: DailyPledge?,
    onQuestClick: (QuestWithBlocks) -> Unit,
    onChallengeClick: (Challenge) -> Unit,
    onSettingsClick: () -> Unit,
    onForgeClick: () -> Unit
) {
    var selectedLauncherTab by remember { mutableIntStateOf(0) } // 0: Quests, 1: Portals, 2: Status

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D0D12),
                contentColor = Color(0xFFD0BCFF),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedLauncherTab == 0,
                    onClick = { selectedLauncherTab = 0 },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    label = { Text("Quests") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFD0BCFF), unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = selectedLauncherTab == 1,
                    onClick = { selectedLauncherTab = 1 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text("Portals") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF81C784), unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = selectedLauncherTab == 2,
                    onClick = { selectedLauncherTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Status") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF64B5F6), unselectedIconColor = Color.Gray)
                )
            }
        },
        containerColor = Color(0xFF09090C)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedLauncherTab) {
                0 -> SoloQuestsTab(userStats, quests, challenges, currentPledge, onQuestClick, onChallengeClick, onSettingsClick, onForgeClick)
                1 -> SoloPortalsTab(appGroups)
                2 -> SoloStatusTab(userStats)
            }
        }
    }
}

@Composable
fun SoloQuestsTab(
    userStats: UserStats?,
    quests: List<QuestWithBlocks>,
    challenges: List<Challenge>,
    currentPledge: DailyPledge?,
    onQuestClick: (QuestWithBlocks) -> Unit,
    onChallengeClick: (Challenge) -> Unit,
    onSettingsClick: () -> Unit,
    onForgeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // System Ticker Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFD0BCFF))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("[SYSTEM STATUS: ACTIVE]", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Player ${userStats?.name ?: "Seeker"} • Lv. ${userStats?.level ?: 1}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, null, tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HP & Stamina Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("HP: ${userStats?.currentHp ?: 100}/${userStats?.maxHp ?: 100}", color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Streak: ${userStats?.currentStreak ?: 0} Days", color = Color(0xFFFFB74D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (userStats?.currentHp?.toFloat() ?: 100f) / (userStats?.maxHp?.toFloat() ?: 100f).coerceAtLeast(1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFFEF5350),
                    trackColor = Color(0xFF2C2C35)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("TODAY'S ACTIVE QUESTS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val activeQuests = quests.filter { !it.quest.isCompletedToday }
        if (activeQuests.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("All Quests Cleared for Today. Gatekeeper defenses secure.", color = Color(0xFF81C784), modifier = Modifier.padding(16.dp), fontSize = 13.sp)
            }
        } else {
            activeQuests.forEach { q ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onQuestClick(q) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TaskAlt, null, tint = Color(0xFFD0BCFF))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(q.quest.name, color = Color.White, fontWeight = FontWeight.Bold)
                            val completed = q.blocks.count { it.isCompleted }
                            Text("$completed / ${q.blocks.size} blocks completed", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onForgeClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C21))
        ) {
            Icon(Icons.Default.Build, null, tint = Color(0xFFD0BCFF))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open The Forge (Training Library)", color = Color.White)
        }
    }
}

@Composable
fun SoloPortalsTab(appGroups: List<AppGroup>) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val allApps = remember { AppInfoFetcher.getInstalledApps(context) }
    val filteredApps = allApps.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.packageName.contains(searchQuery, ignoreCase = true) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(16.dp)
    ) {
        Text("DUNGEON GATES (APP PORTALS)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search portals...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF14141B),
                unfocusedContainerColor = Color(0xFF14141B)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredApps) { app ->
                val matchingGroup = appGroups.find { group ->
                    group.isEnabled && group.packageNames.contains(app.packageName)
                }
                val isLocked = matchingGroup?.let { group ->
                    val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                    val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                    (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
                } ?: false

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (isLocked && matchingGroup != null) {
                                val intent = Intent(context, LockOverlayActivity::class.java).apply {
                                    putExtra("group_id", matchingGroup.id)
                                    putExtra("target_app", app.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } else {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                }
                            }
                        }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                if (isLocked) Color(0xFF3B1D22) else Color(0xFF14141B),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLocked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Boss Gate Sealed",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = app.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFFD0BCFF)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = app.name,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SoloStatusTab(userStats: UserStats?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("PLAYER STATUS WINDOW", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        StatsHeader(userStats)
        Spacer(modifier = Modifier.height(24.dp))
        StatRadarCard(userStats)
    }
}
