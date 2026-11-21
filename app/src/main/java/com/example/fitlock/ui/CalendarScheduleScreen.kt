package com.example.fitlock.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import com.example.fitlock.data.QuestWithBlocks
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class CalendarEvent(
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val location: String
)

data class CustomTimeblock(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: String,
    val endTime: String,
    val dateStr: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScheduleScreen(
    quests: List<QuestWithBlocks> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Calendar permission required to read device events", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedDateCal by remember { mutableStateOf(Calendar.getInstance()) }
    val dateStrFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val currentDateStr = dateStrFormat.format(selectedDateCal.time)

    // Load custom timeblocks for selected date from SharedPreferences
    val prefs = remember(currentDateStr) { context.getSharedPreferences("fitlock_timeblocks", Context.MODE_PRIVATE) }
    var timeblocksJson by remember(currentDateStr) { mutableStateOf(prefs.getString(currentDateStr, "[]") ?: "[]") }
    val gson = remember { Gson() }
    val customTimeblocks = remember(timeblocksJson) {
        try {
            val type = object : TypeToken<List<CustomTimeblock>>() {}.type
            gson.fromJson<List<CustomTimeblock>>(timeblocksJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    var showAddTimeblockDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newStartTime by remember { mutableStateOf("09:00") }
    var newEndTime by remember { mutableStateOf("10:00") }

    val dayEvents = remember(hasCalendarPermission, selectedDateCal) {
        if (hasCalendarPermission) getCalendarEventsForDay(context, selectedDateCal) else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calendar & Schedule", fontWeight = FontWeight.Bold)
                        Text("Device events, timeblocks & daily quests", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddTimeblockDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Timeblock", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Date Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    selectedDateCal = (selectedDateCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = displayFormat.format(selectedDateCal.time),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                IconButton(onClick = {
                    selectedDateCal = (selectedDateCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasCalendarPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text("Read Device Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Grant permission to sync device calendar events directly into your FitLock schedule.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { launcher.launch(Manifest.permission.READ_CALENDAR) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Grant Permission")
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Open App Settings")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SCHEDULE TIMELINE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { showAddTimeblockDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Timeblock", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (dayEvents.isEmpty() && customTimeblocks.isEmpty() && quests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No events, timeblocks, or quests scheduled for this date.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (customTimeblocks.isNotEmpty()) {
                        item {
                            Text("⏰ Custom Timeblocks (${customTimeblocks.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                        }
                        items(customTimeblocks) { tb ->
                            CustomTimeblockItem(tb) {
                                val updated = customTimeblocks.filter { it.id != tb.id }
                                timeblocksJson = gson.toJson(updated)
                                prefs.edit().putString(currentDateStr, timeblocksJson).apply()
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("📅 Device Calendar Events (${dayEvents.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    }

                    if (dayEvents.isEmpty()) {
                        item {
                            Text("No device calendar events for this date.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        }
                    } else {
                        items(dayEvents) { event ->
                            CalendarEventItem(event)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🎯 Linked FitLock Quests (${quests.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    }

                    if (quests.isEmpty()) {
                        item {
                            Text("No quests scheduled.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        }
                    } else {
                        items(quests) { quest ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(quest.quest.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${quest.blocks.size} Habit Blocks", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTimeblockDialog) {
        AlertDialog(
            onDismissRequest = { showAddTimeblockDialog = false },
            title = { Text("Add Custom Timeblock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Timeblock Title (e.g. Deep Work)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newStartTime,
                        onValueChange = { newStartTime = it },
                        label = { Text("Start Time (e.g. 09:00)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEndTime,
                        onValueChange = { newEndTime = it },
                        label = { Text("End Time (e.g. 11:00)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val newTb = CustomTimeblock(
                                title = newTitle.trim(),
                                startTime = newStartTime.trim(),
                                endTime = newEndTime.trim(),
                                dateStr = currentDateStr
                            )
                            val updated = customTimeblocks + newTb
                            timeblocksJson = gson.toJson(updated)
                            prefs.edit().putString(currentDateStr, timeblocksJson).apply()
                            newTitle = ""
                            showAddTimeblockDialog = false
                        }
                    }
                ) {
                    Text("Add Timeblock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTimeblockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CalendarEventItem(event: CalendarEvent) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeRange = "${timeFormat.format(Date(event.startTimeMs))} - ${timeFormat.format(Date(event.endTimeMs))}"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(timeRange, fontSize = 11.sp, color = Color.Gray)
                if (event.location.isNotBlank()) {
                    Text("📍 ${event.location}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun CustomTimeblockItem(tb: CustomTimeblock, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tb.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${tb.startTime} - ${tb.endTime}", fontSize = 11.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Timeblock", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

internal fun getCalendarEventsForDay(context: Context, cal: Calendar): List<CalendarEvent> {
    val events = mutableListOf<CalendarEvent>()
    val dayCal = (cal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startTime = dayCal.timeInMillis
    val endTime = startTime + 24 * 60 * 60 * 1000L

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    android.content.ContentUris.appendId(builder, startTime)
    android.content.ContentUris.appendId(builder, endTime)

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.EVENT_LOCATION
    )

    try {
        val cursor = context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )
        cursor?.use { c ->
            val titleIdx = c.getColumnIndex(CalendarContract.Instances.TITLE)
            val startIdx = c.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = c.getColumnIndex(CalendarContract.Instances.END)
            val locIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

            while (c.moveToNext()) {
                val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "Event" else "Event"
                val start = if (startIdx >= 0) c.getLong(startIdx) else startTime
                val end = if (endIdx >= 0) c.getLong(endIdx) else start + 3600000L
                val loc = if (locIdx >= 0) c.getString(locIdx) ?: "" else ""
                events.add(CalendarEvent(title, start, end, loc))
            }
        }
    } catch (e: Exception) {
        // Handle permission or query error gracefully
    }
    return events
}
