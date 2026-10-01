package com.example.fitlock.ui

import android.content.Context
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.Challenge
import com.example.fitlock.data.DailyPledge
import com.example.fitlock.data.UserStats
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.ui.theme.getThemeData
import java.util.Calendar

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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    val isHangoverActive = remember { prefs.getBoolean("bedtime_hangover_active", false) }

    val themeData = getThemeData(userStats?.activeTheme ?: "DEFAULT")
    val isShieldActive = userStats?.floorTargetMetToday == true
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isHangoverActive) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🌙 Late-Night Screen Penalty (-30 HP)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Screen time past 10:30 PM deducted -30 HP from your morning health pool.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Daily Floor Target / Shield Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isShieldActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isShieldActive) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isShieldActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isShieldActive) "🛡️ Daily Shield Active" else "⚡ Floor Target Pending",
                        fontWeight = FontWeight.Bold,
                        color = if (isShieldActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = if (isShieldActive) "Streak preserved & HP protected for today!" else "Do 3 reps / 5 cards to save your streak",
                        fontSize = 12.sp,
                        color = if (isShieldActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

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

        val isEvening = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 17
        val canClickPledge = currentPledge?.status == "PENDING" || (currentPledge?.status == "COMMITTED" && isEvening)

        Card(
            modifier = Modifier.fillMaxWidth().let { 
                if (canClickPledge) it.clickable { onPledgeClick() } else it 
            },
            colors = CardDefaults.cardColors(
                containerColor = if (currentPledge?.status == "PENDING") 
                    MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp).alpha(if (canClickPledge) 1f else 0.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (currentPledge?.status) {
                    "SUCCESS" -> Icons.Default.CheckCircle
                    "RELAPSED" -> Icons.Default.Error
                    "COMMITTED" -> if (isEvening) Icons.Default.NightsStay else Icons.Default.Done
                    else -> Icons.Default.EditCalendar
                }
                val tint = when (currentPledge?.status) {
                    "SUCCESS" -> Color(0xFF4CAF50)
                    "RELAPSED" -> Color(0xFFF44336)
                    "COMMITTED" -> if (isEvening) MaterialTheme.colorScheme.tertiary else Color.Gray
                    else -> MaterialTheme.colorScheme.primary
                }
                Icon(icon, null, tint = tint)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val titleText = when (currentPledge?.status) {
                        "PENDING" -> "Morning Plan Pending"
                        "COMMITTED" -> if (isEvening) "Ready for Evening Review" else "Morning Plan Completed"
                        "SUCCESS" -> "Day Accomplished"
                        "RELAPSED" -> "Day Logged (Relapse)"
                        else -> "Make your Daily Pledge"
                    }
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subText = when (currentPledge?.status) {
                        "PENDING" -> "Commit to your goals before starting."
                        "COMMITTED" -> if (isEvening) "Reflect on your wins and losses." else "Goals locked in. Review opens at 17:00."
                        else -> "Stay focused. Earn Willpower XP."
                    }
                    Text(
                        text = subText, 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
