package com.example.fitlock.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.LockStatusManager
import com.example.fitlock.utils.AppInfoFetcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherPortalScreen(
    appGroups: List<AppGroup>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    var searchQuery by remember { mutableStateOf("") }
    var favoriteApps by remember { mutableStateOf(prefs.getStringSet("favorite_apps", emptySet()) ?: emptySet()) }
    var selectedStatFilter by remember { mutableStateOf("🌟 All") }

    val statFilterChips = listOf(
        "🌟 All", "⭐ Favorites", "💪 STR", "🎯 DEX", "❤️ CON", "🏃 MOV", "🧠 INT", "📜 WIS", "🛡️ WIL", "🧘 SAN", "✨ CHA", "💰 WTH", "⚙️ TEC", "🎲 LCK"
    )

    val allApps = remember { AppInfoFetcher.getInstalledApps(context) }
    val filteredApps = allApps.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.packageName.contains(searchQuery, ignoreCase = true) 
    }

    val filteredByStat = remember(filteredApps, selectedStatFilter, favoriteApps, appGroups) {
        when {
            selectedStatFilter == "⭐ Favorites" -> filteredApps.filter { favoriteApps.contains(it.packageName) }
            selectedStatFilter != "🌟 All" -> {
                val filterStat = when {
                    selectedStatFilter.contains("STR") -> "STR"
                    selectedStatFilter.contains("DEX") -> "DEX"
                    selectedStatFilter.contains("CON") -> "CON"
                    selectedStatFilter.contains("MOV") -> "MOV"
                    selectedStatFilter.contains("INT") -> "INT"
                    selectedStatFilter.contains("WIS") -> "WIS"
                    selectedStatFilter.contains("WIL") -> "WIL"
                    selectedStatFilter.contains("SAN") -> "SAN"
                    selectedStatFilter.contains("CHA") -> "CHA"
                    selectedStatFilter.contains("WTH") -> "WTH"
                    selectedStatFilter.contains("TEC") -> "TEC"
                    selectedStatFilter.contains("LCK") -> "LCK"
                    else -> ""
                }
                filteredApps.filter { app ->
                    val group = appGroups.find { g -> g.isEnabled && g.packageNames.contains(app.packageName) }
                    group?.rewardStat?.name == filterStat || group?.secondaryStats?.any { it.name == filterStat } == true
                }
            }
            else -> filteredApps
        }
    }

    val sortedApps = remember(filteredByStat, favoriteApps, appGroups) {
        filteredByStat.sortedWith(compareByDescending<AppInfo> { favoriteApps.contains(it.packageName) }.thenBy { app ->
            val group = appGroups.find { g -> g.isEnabled && g.packageNames.contains(app.packageName) }
            when {
                group?.isProductive == true -> 0
                group == null -> 1
                else -> 2
            }
        }.thenBy { it.name })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Portal (System Launcher)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search portal apps...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stat & Favorites Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                statFilterChips.forEach { chip ->
                    FilterChip(
                        selected = selectedStatFilter == chip,
                        onClick = { selectedStatFilter = chip },
                        label = { Text(chip, fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedApps) { app ->
                    val matchingGroup = appGroups.find { group ->
                        group.isEnabled && group.packageNames.contains(app.packageName)
                    }
                    val isLocked = matchingGroup?.let { group ->
                        if (group.isProductive) false
                        else if (group.keywords.any { it.isNotBlank() }) false
                        else {
                            val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                            val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                            (System.currentTimeMillis() - lastUnlocked) >= unlockDurationMs
                        }
                    } ?: false

                    val isTemporarilyUnlocked = matchingGroup?.let { group ->
                        if (group.isProductive || group.keywords.any { it.isNotBlank() }) false
                        else {
                            val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                            val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                            val timeSinceUnlock = System.currentTimeMillis() - lastUnlocked
                            timeSinceUnlock < unlockDurationMs && lastUnlocked > 0L
                        }
                    } ?: false

                    val remainingMins = matchingGroup?.let { group ->
                        val lastUnlocked = LockStatusManager.getLastUnlocked(group.id, group.lastUnlockedTimestamp)
                        val unlockDurationMs = group.unlockDurationMinutes * 60 * 1000L
                        val remainingMs = unlockDurationMs - (System.currentTimeMillis() - lastUnlocked)
                        (remainingMs / 60000L).coerceAtLeast(1)
                    } ?: 0L

                    val hasKeywordCombo = matchingGroup?.keywords?.any { it.isNotBlank() } == true
                    val isFavorite = favoriteApps.contains(app.packageName)

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
                                .size(56.dp)
                                .background(
                                    when {
                                        isLocked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        isTemporarilyUnlocked -> Color(0xFF1B3822)
                                        hasKeywordCombo -> Color(0xFFFF9800).copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLocked) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (isTemporarilyUnlocked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.LockOpen,
                                        contentDescription = "Unlocked",
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("${remainingMins}m", fontSize = 9.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = app.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (hasKeywordCombo) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFavorite) {
                                Text("⭐", fontSize = 9.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = app.name,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = if (isTemporarilyUnlocked) Color(0xFF81C784) else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
