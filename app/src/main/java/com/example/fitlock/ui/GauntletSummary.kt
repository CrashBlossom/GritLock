package com.example.fitlock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.GauntletHistory

@Composable
fun GauntletSummaryScreen(
    history: GauntletHistory,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Text(
            text = "Routine Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rewards", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "+${history.xpGained} XP",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("${history.streakCount} Day Streak!", color = MaterialTheme.colorScheme.secondary)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Habit Breakdown", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
        
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(history.habitLogs) { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(log.name, fontWeight = FontWeight.Bold)
                        val est = log.estimatedDurationSeconds
                        if (est != null) {
                            Text("Estimated: ${formatTime(est)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = formatTime(log.actualDurationSeconds),
                        fontWeight = FontWeight.Medium,
                        color = if (log.estimatedDurationSeconds != null && log.actualDurationSeconds > log.estimatedDurationSeconds) Color.Red else Color.Unspecified
                    )
                }
            }
        }
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Back to Hub")
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
