/**
 * ProfileScreen is the "Hunter Status" interface.
 * It's heavily inspired by Solo Leveling and shows your current RPG attributes.
 */
package com.example.fitlock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.Challenge
import com.example.fitlock.data.UserStats
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    userStats: UserStats?,
    challenges: List<Challenge>,
    onAddChallenge: (Challenge) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Solo Leveling Header Style
        Text(
            "HUNTER STATUS", 
            color = MaterialTheme.colorScheme.primary, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Text(
            userStats?.name?.uppercase() ?: "SEEKER",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Stat Hexagon and Level Card
        StatusCard(userStats)
        
        Spacer(modifier = Modifier.height(32.dp))

        // 2. Individual RPG Stats Breakdown
        Text("ATTRIBUTES", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        RpgStatGrid(userStats)

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Daily Quests (Challenges)
        Text("ACTIVE QUESTS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        challenges.forEach { challenge ->
            ChallengeItemMinimal(challenge)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun StatusCard(stats: UserStats?) {
    val level = stats?.level ?: 1
    val xp = stats?.totalXp ?: 0
    val xpNeeded = level * 100
    val progress = (xp.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stat Hexagon Visual
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                StatHexagon(stats)
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Column {
                Text("LEVEL", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(level.toString(), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text("$xp / $xpNeeded XP", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun RpgStatGrid(stats: UserStats?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RpgStatRow("STR", "Strength", (stats?.strXp ?: 0) / 100 + 1, Color(0xFFFF4B4B))
        RpgStatRow("INT", "Intelligence", (stats?.intXp ?: 0) / 100 + 1, Color(0xFF4B7BFF))
        RpgStatRow("AGI", "Agility", (stats?.agiXp ?: 0) / 100 + 1, Color(0xFF4BFFAB))
        RpgStatRow("VIT", "Vitality", (stats?.vitXp ?: 0) / 100 + 1, Color(0xFFFFB34B))
    }
}

@Composable
fun RpgStatRow(code: String, label: String, level: Long, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(code, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, color = Color.White, fontSize = 14.sp)
            }
            Text("LV. $level", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * A custom-drawn radar chart (hexagon) showing stat balance.
 */
@Composable
fun StatHexagon(stats: UserStats?) {
    // Normalizing stats for the visual (max value of 50 for the chart)
    val s = ((stats?.strXp ?: 0) / 100f).coerceIn(5f, 50f)
    val i = ((stats?.intXp ?: 0) / 100f).coerceIn(5f, 50f)
    val a = ((stats?.agiXp ?: 0) / 100f).coerceIn(5f, 50f)
    val v = ((stats?.vitXp ?: 0) / 100f).coerceIn(5f, 50f)

    val color = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = center
        val radius = size.minDimension / 2
        
        // Draw background circles
        drawCircle(Color.White.copy(alpha = 0.05f), radius = radius, center = center)
        drawCircle(Color.White.copy(alpha = 0.05f), radius = radius * 0.5f, center = center)

        // Draw the stat path
        val path = Path().apply {
            moveTo(center.x, center.y - (s / 50f) * radius) // STR (Top)
            lineTo(center.x + (i / 50f) * radius, center.y) // INT (Right)
            lineTo(center.x, center.y + (v / 50f) * radius) // VIT (Bottom)
            lineTo(center.x - (a / 50f) * radius, center.y) // AGI (Left)
            close()
        }
        
        drawPath(path, color.copy(alpha = 0.3f))
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun ChallengeItemMinimal(challenge: Challenge) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(challenge.title.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(challenge.description, color = Color.Gray, fontSize = 11.sp)
            }
            Text("+${challenge.xpReward} XP", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
