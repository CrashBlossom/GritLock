package com.example.fitlock.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fitlock.data.UserStats
import com.example.fitlock.data.MotivationalQuote
import com.example.fitlock.exercise.ExerciseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    userStats: UserStats? = null,
    quotes: List<MotivationalQuote> = emptyList(),
    onQuickExerciseClick: (ExerciseType, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val randomQuote = remember(quotes) { quotes.randomOrNull() ?: MotivationalQuote(text = "Discipline is freedom.", category = "Stoic") }

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 1024) }

    val prefsManager = remember { com.example.fitlock.data.EmergencyReminderPreferences(context) }
    var config by remember { mutableStateOf(prefsManager.getConfig()) }
    var hostedWidgetIds by remember { mutableStateOf(config.hostedWidgetIds) }
    var widgetHeights by remember { mutableStateOf(config.widgetHeights) }

    LaunchedEffect(hostedWidgetIds, widgetHeights) {
        if (config.hostedWidgetIds != hostedWidgetIds || config.widgetHeights != widgetHeights) {
            val updatedConfig = config.copy(hostedWidgetIds = hostedWidgetIds, widgetHeights = widgetHeights)
            config = updatedConfig
            prefsManager.saveConfig(updatedConfig)
        }
    }

    DisposableEffect(Unit) {
        appWidgetHost.startListening()
        onDispose {
            try {
                appWidgetHost.stopListening()
            } catch (e: Exception) {}
        }
    }

    val pickWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val extras = result.data?.extras
            if (extras != null) {
                val appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (appWidgetId != -1) {
                    hostedWidgetIds = hostedWidgetIds + appWidgetId
                    Toast.makeText(context, "Widget added successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Widgets Dashboard", fontWeight = FontWeight.Bold)
                        Text("Built-in & third-party app widgets", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            val appWidgetId = appWidgetHost.allocateAppWidgetId()
                            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            pickWidgetLauncher.launch(pickIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to launch widget picker on this launcher.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Third-Party Widget", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hosted Third-Party Widgets Section
            if (hostedWidgetIds.isNotEmpty()) {
                item {
                    Text("📦 Third-Party App Widgets", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
                items(hostedWidgetIds.size) { index ->
                    val widgetId = hostedWidgetIds[index]
                    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                    val widgetHeight = widgetHeights[widgetId.toString()] ?: 250
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(appWidgetInfo?.loadLabel(context.packageManager) ?: "App Widget (#$widgetId)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Size: ${widgetHeight}dp", fontSize = 10.sp, color = Color.Gray)
                                    IconButton(
                                        onClick = {
                                            val newH = (widgetHeight - 50).coerceAtLeast(100)
                                            widgetHeights = widgetHeights.toMutableMap().apply { put(widgetId.toString(), newH) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Smaller", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val newH = (widgetHeight + 100).coerceAtMost(700)
                                            widgetHeights = widgetHeights.toMutableMap().apply { put(widgetId.toString(), newH) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Larger", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            appWidgetHost.deleteAppWidgetId(widgetId)
                                            hostedWidgetIds = hostedWidgetIds.filter { it != widgetId }
                                            widgetHeights = widgetHeights.toMutableMap().apply { remove(widgetId.toString()) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove Widget", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            AndroidView(
                                factory = { ctx ->
                                    val hostView = appWidgetHost.createView(ctx, widgetId, appWidgetInfo)
                                    hostView
                                },
                                modifier = Modifier.fillMaxWidth().height(widgetHeight.dp)
                            )
                        }
                    }
                }
            }

            // Quick Stats Widget Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Stats Widget", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatWidgetBadge("Level", "${userStats?.level ?: 1}")
                            StatWidgetBadge("XP", "${userStats?.totalXp ?: 0}")
                            StatWidgetBadge("HP", "${userStats?.currentHp ?: 100}%")
                            StatWidgetBadge("Streak", "${userStats?.currentStreak ?: 0}d")
                        }
                    }
                }
            }

            // Quote of the Day Widget Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quote of the Day", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("\"${randomQuote.text}\"", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp, color = Color.White)
                        Text("— ${randomQuote.category}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Quick Workout Launcher Widget Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFFEF5350))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Workout Launcher", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onQuickExerciseClick(ExerciseType.PUSHUP, 15) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                            ) {
                                Text("Pushups", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onQuickExerciseClick(ExerciseType.SQUAT, 20) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))
                            ) {
                                Text("Squats", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onQuickExerciseClick(ExerciseType.PLANK, 60) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D))
                            ) {
                                Text("Plank", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatWidgetBadge(label: String, value: String) {
    Surface(
        color = Color(0xFF2C2C35),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(68.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
