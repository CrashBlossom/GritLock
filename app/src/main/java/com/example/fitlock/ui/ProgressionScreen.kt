package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionScreen(
    families: List<FamilyWithLevels>,
    currentProgression: Map<String, Int>, // familyId -> currentLevel
    calibrations: Map<String, ExerciseCalibration>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skill Tree (OG)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text("Your Calisthenics Journey", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Complete required reps to unlock advanced skills.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
            }

            items(families) { familyWithLevels ->
                SkillFamilyCard(familyWithLevels, currentProgression[familyWithLevels.family.id] ?: 1, calibrations)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SkillFamilyCard(familyWithLevels: FamilyWithLevels, currentLevel: Int, calibrations: Map<String, ExerciseCalibration>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(familyWithLevels.family.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            familyWithLevels.levels.sortedBy { it.level }.forEach { level ->
                val isUnlocked = level.level <= currentLevel
                val isNext = level.level == currentLevel + 1
                val hasCalibration = calibrations.containsKey("${level.variantName}_CAMERA")
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 32.dp)
                            .background(
                                color = when {
                                    isUnlocked -> MaterialTheme.colorScheme.primary
                                    isNext -> MaterialTheme.colorScheme.tertiary
                                    else -> Color.Gray
                                },
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(level.level.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = level.variantName,
                                fontWeight = if (level.level == currentLevel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                            if (isUnlocked && !hasCalibration) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "Needs Cal", 
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        if (!isUnlocked) {
                            Text(
                                text = "Req: ${level.unlockRequirementReps} reps of level ${level.level - 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    if (isUnlocked) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    } else if (!isNext) {
                        Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
