package com.example.fitlock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.GauntletWithHabits

@Composable
fun GauntletExecutionScreen(
    gauntlet: GauntletWithHabits,
    currentHabitIndex: Int,
    elapsedSeconds: Int,
    isInBuffer: Boolean = false,
    bufferRemainingSeconds: Int = 0,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    val currentHabit = gauntlet.habits.getOrNull(currentHabitIndex)
    val nextHabit = gauntlet.habits.getOrNull(currentHabitIndex + 1)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isInBuffer) {
            Text(
                text = "Get Ready",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Next: ${currentHabit?.name}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = bufferRemainingSeconds.toString(),
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            Text(
                text = gauntlet.gauntlet.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (currentHabit != null) {
                Text(
                    text = currentHabit.name,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val timerText = formatTime(elapsedSeconds)
                val expectedText = if (currentHabit.estimatedDurationSeconds != null) {
                    "/ ${formatTime(currentHabit.estimatedDurationSeconds)}"
                } else "/ -"
                
                Text(
                    text = "$timerText $expectedText",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Medium
                )
                
                if (currentHabit.estimatedDurationSeconds != null) {
                    val progress = (elapsedSeconds.toFloat() / currentHabit.estimatedDurationSeconds.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 16.dp)
                    )
                }

                if (currentHabit.subHabits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Checklist", fontWeight = FontWeight.Bold)
                            currentHabit.subHabits.forEach { sub ->
                                var checked by remember { mutableStateOf(false) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                                    Text(
                                        text = sub,
                                        textDecoration = if (checked) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (nextHabit != null) {
                    Text(
                        text = "Next: ${nextHabit.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, null)
                Text("Stop")
            }
            
            if (!isInBuffer) {
                Button(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, null)
                    Text(if (nextHabit == null) "Finish" else "Next")
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
