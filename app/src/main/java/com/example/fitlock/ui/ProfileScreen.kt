package com.example.fitlock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.Challenge
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.data.UserStats
import com.example.fitlock.exercise.ExerciseType
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
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
        Text("Profile", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Level and XP Progress
        StatsHeader(userStats)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Rep Bank Section (Moved out of header for visibility)
        RepBankSection(userStats)

        Spacer(modifier = Modifier.height(24.dp))
        
        // RPG Stat Radar Chart
        StatRadarCard(userStats)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Challenges", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            var showAddDialog by remember { mutableStateOf(false) }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add Challenge", tint = MaterialTheme.colorScheme.primary)
            }
            if (showAddDialog) {
                AddChallengeDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { challenge ->
                        onAddChallenge(challenge)
                        showAddDialog = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        challenges.forEach { challenge ->
            ChallengeItemMinimal(challenge)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (challenges.isEmpty()) {
            Text("No custom challenges yet.", color = Color.Gray, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatsHeader(stats: UserStats?) {
    val level = stats?.level ?: 1
    val xp = stats?.totalXp ?: 0
    val xpNeeded = level * 100
    val progress = (xp.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(level.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Level $level", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text("$xp / $xpNeeded XP", color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Streak", "${stats?.currentStreak ?: 0}d", Icons.Default.Whatshot)
                val totalBanked = stats?.bankedReps?.values?.sum() ?: 0
                StatItem("Total Banked", totalBanked.toString(), Icons.Default.AccountBalance)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepBankSection(stats: UserStats?) {
    if (stats?.bankedReps?.isNotEmpty() == true && stats.bankedReps.any { it.value > 0 }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Rep Bank", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.bankedReps.filter { it.value > 0 }.forEach { (type, count) ->
                        AssistChip(
                            onClick = {},
                            label = { Text("$count $type") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatRadarCard(stats: UserStats?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ability Scores", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxStatXp = 5000f 
                val scores = listOf(
                    (stats?.strXp?.toFloat()?.div(maxStatXp) ?: 0.1f).coerceIn(0.1f, 1f),
                    (stats?.agiXp?.toFloat()?.div(maxStatXp) ?: 0.1f).coerceIn(0.1f, 1f),
                    (stats?.vitXp?.toFloat()?.div(maxStatXp) ?: 0.1f).coerceIn(0.1f, 1f),
                    (stats?.intXp?.toFloat()?.div(maxStatXp) ?: 0.1f).coerceIn(0.1f, 1f),
                    (stats?.senXp?.toFloat()?.div(maxStatXp) ?: 0.1f).coerceIn(0.1f, 1f),
                    (stats?.chaXp?.toFloat()?.div(maxStatXp) ?: 0.1f).coerceIn(0.1f, 1f)
                )
                
                AbilityRadarChart(scores = scores)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatLabel("STR", "Strength")
                StatLabel("AGI", "Agility")
                StatLabel("VIT", "Vitality")
                StatLabel("INT", "Intel")
                StatLabel("SEN", "Sense")
                StatLabel("CHA", "Charm")
            }
        }
    }
}

@Composable
fun AbilityRadarChart(scores: List<Float>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        val angleStep = (2 * Math.PI / 6).toFloat()
        for (i in 1..4) {
            val r = radius * (i / 4f)
            val path = Path()
            for (j in 0..5) {
                val angle = j * angleStep - Math.PI.toFloat() / 2
                val x = center.x + r * Math.cos(angle.toDouble()).toFloat()
                val y = center.y + r * Math.sin(angle.toDouble()).toFloat()
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = Color.Gray.copy(alpha = 0.2f), style = Stroke(width = 1.dp.toPx()))
        }
        val statPath = Path()
        scores.forEachIndexed { j, score ->
            val angle = j * angleStep - Math.PI.toFloat() / 2
            val r = radius * score
            val x = center.x + r * Math.cos(angle.toDouble()).toFloat()
            val y = center.y + r * Math.sin(angle.toDouble()).toFloat()
            if (j == 0) statPath.moveTo(x, y) else statPath.lineTo(x, y)
        }
        statPath.close()
        drawPath(statPath, color = primaryColor.copy(alpha = 0.4f))
        drawPath(statPath, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun StatLabel(short: String, full: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(short, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(full, color = Color.Gray, fontSize = 8.sp)
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ChallengeItemMinimal(challenge: Challenge) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(challenge.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(challenge.description, color = Color.Gray, fontSize = 12.sp)
            }
            Text("+${challenge.xpReward} XP", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChallengeDialog(onDismiss: () -> Unit, onConfirm: (Challenge) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var xpReward by remember { mutableStateOf("100") }
    var exerciseType by remember { mutableStateOf(ExerciseType.PUSHUP) }
    var count by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Challenge", color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                TextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                TextField(value = xpReward, onValueChange = { xpReward = it }, label = { Text("XP Reward") })
                Text("Exercise Requirement", color = Color.Gray, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        TextButton(onClick = { expanded = true }) { Text(exerciseType.name) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ExerciseType.entries.forEach {
                                DropdownMenuItem(text = { Text(it.name) }, onClick = { exerciseType = it; expanded = false })
                            }
                        }
                    }
                    TextField(value = count, onValueChange = { count = it }, modifier = Modifier.width(80.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(Challenge(id = UUID.randomUUID().toString(), title = title, description = desc, requirements = listOf(ExerciseRequirement(exerciseType.name, count.toIntOrNull() ?: 10)), xpReward = xpReward.toIntOrNull() ?: 100))
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
