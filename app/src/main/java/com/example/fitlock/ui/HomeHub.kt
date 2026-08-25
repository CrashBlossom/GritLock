package com.example.fitlock.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.Challenge
import com.example.fitlock.data.DailyPledge
import com.example.fitlock.data.UserStats
import com.example.fitlock.exercise.ExerciseType

@Composable
fun HomeHub(
    userStats: UserStats?,
    challenges: List<Challenge>,
    todayTotals: Map<String, Int>,
    currentPledge: DailyPledge?,
    deckSummaries: List<DeckSummary>,
    onChallengeClick: (Challenge) -> Unit,
    onExerciseClick: (ExerciseType, Int) -> Unit,
    onSettingsClick: () -> Unit,
    onUrgeClick: () -> Unit,
    onPledgeClick: () -> Unit,
    onViewLogClick: () -> Unit,
    onImportAnkiClick: () -> Unit,
    onDeckClick: (String) -> Unit,
    onDeleteDeck: (String) -> Unit
) {
    val themeData = com.example.fitlock.ui.theme.getThemeData(userStats?.activeTheme ?: "DEFAULT")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Header(onSettingsClick)
            AvatarSystem(userStats = userStats)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        WillpowerDashboard(
            userStats = userStats,
            currentPledge = currentPledge,
            onUrgeClick = onUrgeClick,
            onPledgeClick = onPledgeClick,
            onViewLogClick = onViewLogClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        CognitiveForgeSection(
            deckSummaries = deckSummaries,
            onDeckClick = onDeckClick,
            onImportClick = onImportAnkiClick,
            onDeleteDeck = onDeleteDeck
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        SectionTitle(themeData.tabAnalytics.replace("Analytics", "Daily Quest").replace("Crime Map", "Bounty").replace("Quest Log", "Main Quest").replace("Journal", "Adventure"))
        val dailyQuest = challenges.find { it.id == "opm_classic" || it.id == "solo_leveling" }
        if (dailyQuest != null) {
            ChallengeItem(
                challenge = dailyQuest,
                todayTotals = todayTotals,
                onClick = { onChallengeClick(dailyQuest) },
                onLongClick = {}
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No quest active. Check back later!", modifier = Modifier.padding(16.dp), color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SectionTitle("Daily Goals")
        val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("fitlock_prefs", android.content.Context.MODE_PRIVATE)
        DailyGoalsList(prefs, todayTotals, onExerciseClick)
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CognitiveForgeSection(
    deckSummaries: List<DeckSummary>,
    onDeckClick: (String) -> Unit,
    onImportClick: () -> Unit,
    onDeleteDeck: (String) -> Unit
) {
    var deckToDelete by remember { mutableStateOf<String?>(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Cognitive Forge")
            IconButton(onClick = onImportClick) {
                Icon(Icons.Default.Add, contentDescription = "Import Anki Deck")
            }
        }
        
        if (deckSummaries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("No decks imported. Tap '+' to start.", color = Color.Gray)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(deckSummaries) { deck ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(100.dp)
                            .combinedClickable(
                                onClick = { onDeckClick(deck.name) },
                                onLongClick = { deckToDelete = deck.name }
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(deck.name, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${deck.cardsDue} cards due", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (deckToDelete != null) {
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Delete Pack") },
            text = { Text("Remove all cards from '${deckToDelete}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDeck(deckToDelete!!)
                        deckToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deckToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun WillpowerDashboard(
    userStats: UserStats?,
    currentPledge: DailyPledge?,
    onUrgeClick: () -> Unit,
    onPledgeClick: () -> Unit,
    onViewLogClick: () -> Unit
) {
    val themeData = com.example.fitlock.ui.theme.getThemeData(userStats?.activeTheme ?: "DEFAULT")
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF5722))
                    Text("${userStats?.sobrietyStreak ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Day Streak", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(
                modifier = Modifier.weight(1f).clickable { onViewLogClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.HistoryEdu, null)
                    Text(themeData.tabAnalytics.replace("Analytics", "Daily Log").replace("Crime Map", "Intel Log").replace("Quest Log", "Mission Log").replace("Journal", "Travel Log"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("View your progress", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(
            onClick = onUrgeClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.FlashOn, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(themeData.tabUrge, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onPledgeClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (currentPledge?.status == "PENDING") 
                    MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (currentPledge?.status) {
                    "SUCCESS" -> Icons.Default.CheckCircle
                    "RELAPSED" -> Icons.Default.Error
                    else -> Icons.Default.EditCalendar
                }
                val tint = when (currentPledge?.status) {
                    "SUCCESS" -> Color(0xFF4CAF50)
                    "RELAPSED" -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.primary
                }
                Icon(icon, null, tint = tint)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (currentPledge?.status == "PENDING") "Make your Daily Pledge" else "Pledge: ${currentPledge?.status}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Stay focused. Earn Willpower XP.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
