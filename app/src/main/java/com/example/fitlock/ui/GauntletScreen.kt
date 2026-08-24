package com.example.fitlock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitlock.data.GauntletWithHabits

@Composable
fun GauntletScreen(
    gauntlets: List<GauntletWithHabits>,
    onAdd: () -> Unit,
    onEdit: (GauntletWithHabits) -> Unit,
    onDelete: (GauntletWithHabits) -> Unit,
    onToggle: (GauntletWithHabits) -> Unit,
    onStart: (GauntletWithHabits) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Gauntlet")
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
                        onStart = { onStart(gauntlet) }
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
    onStart: () -> Unit
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = gauntlet.habits.joinToString(" → ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}
