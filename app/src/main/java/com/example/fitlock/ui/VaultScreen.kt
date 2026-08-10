@file:OptIn(ExperimentalLayoutApi::class)
package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.VaultItem
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.exercise.ExerciseType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultScreen(
    vaultItems: List<VaultItem>,
    onAddItem: (VaultItem) -> Unit,
    onDeleteItem: (VaultItem) -> Unit,
    onUnlockItem: (VaultItem) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF121216),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Secret")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "SECRET VAULT",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Lock passwords or notes behind a workout",
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (vaultItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No secrets stored yet.", color = Color.DarkGray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vaultItems) { item ->
                        VaultItemRow(item, onDeleteItem, onUnlockItem)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVaultItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { 
                onAddItem(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun VaultItemRow(item: VaultItem, onDelete: (VaultItem) -> Unit, onUnlock: (VaultItem) -> Unit) {
    val now = System.currentTimeMillis()
    val isUnlocked = now - item.lastUnlockedTimestamp < item.unlockDurationMinutes * 60 * 1000L
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { if (!isUnlocked) onUnlock(item) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) Color.Green else Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDelete(item) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            if (isUnlocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D2D35), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(item.content, color = Color.White, fontSize = 14.sp)
                }
                val remaining = (item.unlockDurationMinutes * 60 * 1000L - (now - item.lastUnlockedTimestamp)) / 1000
                Text(
                    "Relocking in ${remaining / 60}m ${remaining % 60}s",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap to unlock with requirements", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.requirements.forEach { req ->
                        Text(
                            "${req.count} ${req.type}", 
                            color = Color.Gray, 
                            fontSize = 10.sp,
                            modifier = Modifier.background(Color(0xFF2D2D35), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddVaultItemDialog(onDismiss: () -> Unit, onSave: (VaultItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var exerciseRequirements by remember { mutableStateOf(listOf(ExerciseRequirement("PUSHUP", 20))) }
    var unlockDuration by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Secret", color = Color.White) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. PC Password)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Secret Content / Notepad") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Requirements", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { 
                        exerciseRequirements = exerciseRequirements + ExerciseRequirement("PUSHUP", 10) 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                }

                exerciseRequirements.forEachIndexed { index, req ->
                    RequirementRow(
                        index = index,
                        req = req,
                        onUpdate = { newReq ->
                            exerciseRequirements = exerciseRequirements.toMutableList().apply { this[index] = newReq }
                        },
                        onDelete = {
                            if (exerciseRequirements.size > 1) {
                                exerciseRequirements = exerciseRequirements.toMutableList().apply { removeAt(index) }
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = unlockDuration,
                    onValueChange = { unlockDuration = it },
                    label = { Text("Relock after (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(VaultItem(
                            title = title,
                            content = content,
                            requirements = exerciseRequirements,
                            unlockDurationMinutes = unlockDuration.toIntOrNull() ?: 5
                        ))
                    }
                }
            ) {
                Text("Lock It")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF1C1C21)
    )
}
