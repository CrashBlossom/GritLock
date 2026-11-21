package com.example.fitlock.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.MainActivity
import com.example.fitlock.ui.theme.GritLockTheme

class EmergencyWarningActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("EXTRA_TITLE") ?: "🚨 EMERGENCY STREAK SHIELD"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Your daily habit targets are incomplete."
        val exerciseType = intent.getStringExtra("EXTRA_EXERCISE_TYPE") ?: "PUSHUP"
        val exerciseCount = intent.getIntExtra("EXTRA_EXERCISE_COUNT", 15)
        val targetPackage = intent.getStringExtra("EXTRA_TARGET_PACKAGE") ?: "com.duolingo"

        setContent {
            GritLockTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )

                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action: DO IT NOW
                            Button(
                                onClick = {
                                    val workoutIntent = Intent(this@EmergencyWarningActivity, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        putExtra("START_MINI_WORKOUT", true)
                                        putExtra("EXERCISE_TYPE", exerciseType)
                                        putExtra("EXERCISE_COUNT", exerciseCount)
                                    }
                                    startActivity(workoutIntent)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("DO $exerciseType NOW", fontWeight = FontWeight.Bold)
                            }

                            // Action: OPEN TARGET APP
                            OutlinedButton(
                                onClick = {
                                    val appIntent = packageManager.getLaunchIntentForPackage(targetPackage) ?: Intent(this@EmergencyWarningActivity, MainActivity::class.java)
                                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(appIntent)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("OPEN TARGET APP")
                            }

                            // Action: SNOOZE 30M
                            OutlinedButton(
                                onClick = {
                                    val snoozeIntent = Intent(this@EmergencyWarningActivity, MindfulSnoozeActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                    startActivity(snoozeIntent)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("SNOOZE 30M")
                            }

                            // Action: DISMISS TO TRAY
                            TextButton(
                                onClick = { finish() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dismiss to Notification Tray", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
