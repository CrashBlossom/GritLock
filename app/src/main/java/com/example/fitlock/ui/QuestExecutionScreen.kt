package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.fitlock.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestExecutionScreen(
    questWithBlocks: QuestWithBlocks,
    onToggleBlock: (QuestBlock) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val allCompleted = questWithBlocks.blocks.all { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(questWithBlocks.quest.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (questWithBlocks.quest.isLockShieldEnabled && !allCompleted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Shield Active: Finish all blocks to unlock.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                items(questWithBlocks.blocks) { block ->
                    QuestBlockExecutionItem(block, onToggle = { onToggleBlock(block) })
                    
                    // Show rest timer if block was just completed
                    if (block.isCompleted && block.type == BlockType.EXERCISE) {
                        RestTimer(duration = block.restBetweenSets ?: 90)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (allCompleted) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Quest (+${questWithBlocks.quest.xpReward} XP)")
                }
            }
        }
    }
}

@Composable
fun QuestBlockExecutionItem(block: QuestBlock, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (block.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = block.isCompleted, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.name,
                    fontWeight = FontWeight.Bold,
                    color = if (block.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = block.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = when(block.type) {
                    BlockType.HABIT -> Icons.Default.Timer
                    BlockType.EXERCISE -> Icons.Default.FitnessCenter
                    BlockType.TASK -> Icons.Default.Assignment
                    else -> Icons.Default.List
                },
                contentDescription = null,
                tint = if (block.isCompleted) Color.LightGray else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RestTimer(duration: Int) {
    var timeLeft by remember { mutableIntStateOf(duration) }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
    }

    if (timeLeft > 0) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 4.dp, bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rest: ${timeLeft}s", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
