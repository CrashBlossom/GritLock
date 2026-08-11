package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.Challenge
import com.example.fitlock.data.UserStats
import com.example.fitlock.exercise.ExerciseType

@Composable
fun HomeHub(
    userStats: UserStats?,
    challenges: List<Challenge>,
    todayTotals: Map<String, Int>,
    onChallengeClick: (Challenge) -> Unit,
    onExerciseClick: (ExerciseType, Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Header(onSettingsClick)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // RPG Stats (Spider Chart)
        StatRadarCard(userStats)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SectionTitle("Daily Quest")
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
