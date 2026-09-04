package com.example.fitlock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.UrgeEvent
import com.example.fitlock.data.MotivationalQuote

@Composable
fun UrgeNegotiationDialog(
    onDismiss: () -> Unit,
    onConfirm: (UrgeEvent) -> Unit,
    availableQuotes: List<MotivationalQuote>,
    themeUrgeLabel: String = "Grit Trial"
) {
    var intensity by remember { mutableFloatStateOf(5f) }
    var selectedCategory by remember { mutableStateOf("Internal Voice") }
    var subCategory by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    
    val categories = listOf("Internal Voice", "Hunger", "Boredom", "Social Pressure", "Stress")
    val subCategories = mapOf(
        "Internal Voice" to listOf("Laziness", "Fear", "Self-Doubt", "Rationalization", "Greed"),
        "Stress" to listOf("Work", "Family", "Health", "Financial")
    )

    val displayedQuote = remember(selectedCategory, subCategory, availableQuotes) {
        val filtered = availableQuotes.filter { 
            it.category == selectedCategory && (subCategory.isEmpty() || it.subCategory == subCategory)
        }
        if (filtered.isNotEmpty()) filtered.random().text 
        else "You are stronger than your excuses. Stay the course."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(themeUrgeLabel) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("How intense is the grit requirement?", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text("Intensity: ${intensity.toInt()}/10", modifier = Modifier.align(Alignment.End), style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("What triggered this?", style = MaterialTheme.typography.titleSmall)
                var catExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategory)
                    }
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { 
                                selectedCategory = cat
                                subCategory = ""
                                catExpanded = false 
                            })
                        }
                    }
                }

                if (subCategories.containsKey(selectedCategory)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Specific Type", style = MaterialTheme.typography.titleSmall)
                    var subExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { subExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (subCategory.isEmpty()) "Select Type" else subCategory)
                        }
                        DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }) {
                            subCategories[selectedCategory]?.forEach { sub ->
                                DropdownMenuItem(text = { Text(sub) }, onClick = { 
                                    subCategory = sub
                                    subExpanded = false 
                                })
                            }
                        }
                    }
                }

                if (selectedCategory == "Internal Voice") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("What is the voice saying?", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Just 5 more minutes...") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Motivational Reminder
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displayedQuote,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(UrgeEvent(
                    intensity = intensity.toInt(),
                    category = selectedCategory,
                    subCategory = subCategory,
                    comment = comment,
                    wasResisted = true
                ))
            }) {
                Text("LOG & RESIST")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
