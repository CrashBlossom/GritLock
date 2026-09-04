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
    quests: List<com.example.fitlock.data.QuestWithBlocks>,
    challenges: List<Challenge>,
    currentPledge: DailyPledge?,
    onQuestClick: (com.example.fitlock.data.QuestWithBlocks) -> Unit,
    onChallengeClick: (Challenge) -> Unit,
    onSettingsClick: () -> Unit,
    onUrgeClick: () -> Unit,
    onPledgeClick: () -> Unit,
    onViewLogClick: () -> Unit,
    onStartPlanning: () -> Unit
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
            onViewLogClick = onViewLogClick,
            onStartPlanning = onStartPlanning
        )

        val activeQuest = quests.find { it.quest.type == com.example.fitlock.data.QuestType.DAILY_COMMITMENT && !it.quest.isCompletedToday }
        if (activeQuest != null) {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Current Quest")
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onQuestClick(activeQuest) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(activeQuest.quest.name, fontWeight = FontWeight.Bold)
                        val completedCount = activeQuest.blocks.count { it.isCompleted }
                        val totalBlocks = activeQuest.blocks.size
                        Text("$completedCount / $totalBlocks blocks finished", style = MaterialTheme.typography.bodySmall)
                        if (totalBlocks > 0) {
                            LinearProgressIndicator(
                                progress = { completedCount.toFloat() / totalBlocks.toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (challenges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Elite Challenges")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(challenges) { challenge ->
                    ChallengeCard(challenge = challenge, onClick = { onChallengeClick(challenge) })
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ChallengeCard(challenge: Challenge, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(challenge.title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(challenge.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Color.Gray)
        }
    }
}

@Composable
fun WillpowerDashboard(
    userStats: UserStats?,
    currentPledge: DailyPledge?,
    onUrgeClick: () -> Unit,
    onPledgeClick: () -> Unit,
    onViewLogClick: () -> Unit,
    onStartPlanning: () -> Unit
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
                    Text((userStats?.sobrietyStreak ?: 0).toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Day Streak", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(
                modifier = Modifier.weight(1f).clickable { onViewLogClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.HistoryEdu, null)
                    Text(themeData.logName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            Text(themeData.tabUrge.replace("Urge", "Grit"), fontWeight = FontWeight.Bold)
        }

        if (currentPledge == null || currentPledge.status == "PENDING") {
            Button(
                onClick = onStartPlanning,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.EditCalendar, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Planning Phase", fontWeight = FontWeight.Bold)
            }
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
                        text = if (currentPledge?.status == "PENDING") "Make your Daily Pledge" 
                               else if (currentPledge?.mainObjective != null) "Goal: ${currentPledge.mainObjective}"
                               else "Pledge: ${currentPledge?.status}",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (currentPledge?.status == "COMMITTED") "Focus on your priority today." else "Stay focused. Earn Willpower XP.", 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
