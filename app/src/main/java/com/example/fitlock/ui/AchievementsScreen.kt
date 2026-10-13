package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.Achievement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<Achievement>,
    onClaimReward: (Achievement) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    val categories = listOf(
        "ALL", "STREAK", "REPS", "PUSHUP", "PUSHUP_STREAK", "SQUAT", "SQUAT_STREAK", "PLANK", "PLANK_STREAK",
        "PULLUP", "DIP", "SITUP", "HINGE", "ROW", "BREATHWORK", "BREATHWORK_STREAK", "WRITING", "WATER",
        "STEPS", "ROWING", "CALORIES", "MORNING_PLAN", "EVENING_REVIEW", "ANKI_FOCUS", "GATES", "LEVEL"
    )

    val filteredAchievements = remember(achievements, selectedCategory) {
        if (selectedCategory == "ALL") achievements else achievements.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hall of Ascendance (Trophies & Badges)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
        ) {
            // Category Filter Row
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                containerColor = Color.Transparent,
                edgePadding = 0.dp
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredAchievements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No achievements in this category yet.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAchievements) { ach ->
                        AchievementCard(achievement = ach, onClaim = { onClaimReward(ach) })
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, onClaim: () -> Unit) {
    val tierColor = when (achievement.tier) {
        0 -> Color(0xFF78909C) // Starter (Blue Grey)
        1 -> Color(0xFFCD7F32) // Bronze
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFFFD700) // Gold
        4 -> Color(0xFF00E5FF) // Diamond
        else -> Color(0xFFFF4081) // Legend
    }

    val tierName = when (achievement.tier) {
        0 -> "Starter"
        1 -> "Bronze"
        2 -> "Silver"
        3 -> "Gold"
        4 -> "Diamond"
        else -> "Legend"
    }

    val progressFraction = if (achievement.targetProgress > 0) {
        (achievement.currentProgress.toFloat() / achievement.targetProgress.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge Icon Box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(tierColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (achievement.category) {
                        "STREAK", "PUSHUP_STREAK", "SQUAT_STREAK", "PLANK_STREAK", "BREATHWORK_STREAK" -> Icons.Default.LocalFireDepartment
                        "REPS", "PUSHUP", "SQUAT", "PLANK", "PULLUP", "DIP", "SITUP", "HINGE", "ROW" -> Icons.Default.FitnessCenter
                        "BREATHWORK" -> Icons.Default.SelfImprovement
                        "WRITING" -> Icons.Default.EditNote
                        "WATER" -> Icons.Default.Opacity
                        "STEPS" -> Icons.AutoMirrored.Filled.DirectionsWalk
                        "ROWING" -> Icons.Default.Rowing
                        "CALORIES" -> Icons.Default.LocalFireDepartment
                        "MORNING_PLAN" -> Icons.Default.WbSunny
                        "EVENING_REVIEW" -> Icons.Default.NightsStay
                        "ANKI_FOCUS" -> Icons.Default.Psychology
                        "GATES" -> Icons.Default.Shield
                        else -> Icons.Default.EmojiEvents
                    },
                    contentDescription = null,
                    tint = tierColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(achievement.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = tierColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "T${achievement.tier} $tierName",
                            color = tierColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(achievement.description, color = Color.Gray, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = tierColor,
                    trackColor = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${achievement.currentProgress} / ${achievement.targetProgress}", color = Color.Gray, fontSize = 10.sp)
                    Text("+${achievement.xpReward} XP", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (achievement.isUnlocked && !achievement.isClaimed) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = tierColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Claim", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else if (achievement.isClaimed) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = "Claimed", tint = Color.Green, modifier = Modifier.size(24.dp))
            }
        }
    }
}
