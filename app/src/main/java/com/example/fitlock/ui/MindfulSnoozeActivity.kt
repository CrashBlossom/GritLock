package com.example.fitlock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fitlock.data.DailyLogNote
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.service.MovementReminderWorker
import com.example.fitlock.ui.theme.GritLockTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * MindfulSnoozeActivity forces the user to reflect on why they are delaying their workout.
 */
@AndroidEntryPoint
class MindfulSnoozeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GritLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    MindfulSnoozeContent(
                        onDismiss = { finish() },
                        onCommit = { reason, duration ->
                            commitSnooze(reason, duration)
                        }
                    )
                }
            }
        }
    }

    private fun commitSnooze(reason: String, duration: Int) {
        val db = GritLockDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            db.dao().insertLogNote(
                DailyLogNote(
                    content = "Snoozed for $duration because: $reason",
                    type = "SNOOZE_REASON"
                )
            )

            val workData = Data.Builder()
                .putBoolean("hardStop", true)
                .build()

            val snoozeWork = OneTimeWorkRequestBuilder<MovementReminderWorker>()
                .setInitialDelay(duration.toLong(), TimeUnit.MINUTES)
                .setInputData(workData)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(snoozeWork)
            
            runOnUiThread { finish() }
        }
    }
}

@Composable
fun MindfulSnoozeContent(
    onDismiss: () -> Unit,
    onCommit: (String, Int) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var snoozeMinutesText by remember { mutableStateOf("15") }
    val snoozeMinutes = snoozeMinutesText.toIntOrNull() ?: 0
    val isDurationValid = snoozeMinutes in 1..15

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Why are you delaying your discipline?") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for delay") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = snoozeMinutesText,
                    onValueChange = { input ->
                        if (input.isEmpty() || (input.all { it.isDigit() } && input.toInt() <= 15)) {
                            snoozeMinutesText = input
                        }
                    },
                    label = { Text("Snooze Duration (1-15 mins)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isDurationValid && snoozeMinutesText.isNotEmpty()
                )
                
                if (!isDurationValid && snoozeMinutesText.isNotEmpty()) {
                    Text(
                        text = "Maximum delay is 15 minutes",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCommit(reason, snoozeMinutes) },
                enabled = reason.isNotBlank() && isDurationValid
            ) {
                Text("Commit Delay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
