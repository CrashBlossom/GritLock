package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.GauntletWithHabits
import com.example.fitlock.data.HabitWithDefinition
import java.util.concurrent.TimeUnit

@Composable
fun GauntletScreen(
    gauntlets: List<GauntletWithHabits>,
    onAdd: () -> Unit,
    onEdit: (GauntletWithHabits) -> Unit,
    onDelete: (GauntletWithHabits) -> Unit,
    onToggle: (GauntletWithHabits) -> Unit,
    onStart: (GauntletWithHabits) -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onHabitClick: (HabitWithDefinition) -> Unit = {}
) {
    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onNavigateToLibrary,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = "Habit Library")
                }
                FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Add Gauntlet")
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (gauntlets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No routines found. Build your first Gauntlet!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(gauntlets) { gauntlet ->
                    GauntletItem(
                        gauntlet = gauntlet,
                        onToggle = { onToggle(gauntlet) },
                        onEdit = { onEdit(gauntlet) },
                        onDelete = { onDelete(gauntlet) },
                        onStart = { onStart(gauntlet) },
                        onHabitClick = onHabitClick
                    )
                }
            }
        }
    }
}

@Composable
fun GauntletItem(
    gauntlet: GauntletWithHabits,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
    onHabitClick: (HabitWithDefinition) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gauntlet.gauntlet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${gauntlet.habits.size} Habits | ${gauntlet.gauntlet.triggerType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onStart) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = MaterialTheme.colorScheme.primary)
                    }
                    Switch(
                        checked = gauntlet.gauntlet.isEnabled,
                        onCheckedChange = { onToggle() }
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
            
            if (gauntlet.habits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Habit Consistency:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gauntlet.habits.forEach { habit ->
                        val daysSinceLast = if (habit.habit.lastCompletionTimestamp > 0L) {
                            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - habit.habit.lastCompletionTimestamp)
                        } else 2L // Never done = missed
                        
                        val streakColor = when {
                            daysSinceLast >= 2 -> Color.Red // Missed twice!
                            daysSinceLast >= 1 -> Color.Yellow // Missed once
                            else -> MaterialTheme.colorScheme.primary // Done today/yesterday
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onHabitClick(habit) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(streakColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = habit.habit.currentStreak.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = streakColor
                                )
                            }
                            Text(
                                text = habit.effectiveName.take(5),
                                fontSize = 8.sp,
                                maxLines = 1,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (gauntlet.habits.any { TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.habit.lastCompletionTimestamp) >= 1 }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠ NEVER MISS TWICE: Some habits at risk!",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
