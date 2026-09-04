package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fitlock.data.GauntletWithAdvancedWorkout
import com.example.fitlock.data.GauntletWithHabits
import com.example.fitlock.data.HabitMediaType
import com.example.fitlock.data.HabitTrackingType

@Composable
fun GauntletExecutionScreen(
    gauntlet: GauntletWithHabits,
    advancedWorkout: GauntletWithAdvancedWorkout? = null,
    currentHabitIndex: Int,
    currentBlockIndex: Int = 0,
    currentBlockExerciseIndex: Int = 0,
    currentBlockSet: Int = 1,
    elapsedSeconds: Int,
    currentReps: Int = 0,
    personalBests: Map<Int, Int> = emptyMap(),
    isInBuffer: Boolean = false,
    isResting: Boolean = false,
    bufferRemainingSeconds: Int = 0,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onUpdateReps: (Int) -> Unit = {}
) {
    val currentHabit = gauntlet.habits.getOrNull(currentHabitIndex)
    
    val currentBlock = advancedWorkout?.blocks?.getOrNull(currentBlockIndex)
    val currentExercise = currentBlock?.exercises?.getOrNull(currentBlockExerciseIndex)
    
    val nextHabit = if (currentHabit != null) {
        gauntlet.habits.getOrNull(currentHabitIndex + 1)?.effectiveName ?: advancedWorkout?.blocks?.firstOrNull()?.exercises?.firstOrNull()?.name
    } else null
    
    val nextExercise = if (currentExercise != null) {
        if (currentBlockExerciseIndex < (currentBlock?.exercises?.size ?: 0) - 1) {
            currentBlock?.exercises?.get(currentBlockExerciseIndex + 1)?.name
        } else if (currentBlockSet < (currentBlock?.exercises?.firstOrNull()?.sets ?: 1)) {
            currentBlock?.exercises?.firstOrNull()?.name + " (Set ${currentBlockSet + 1})"
        } else {
            advancedWorkout?.blocks?.getOrNull(currentBlockIndex + 1)?.exercises?.firstOrNull()?.name
        }
    } else null

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (isInBuffer) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = "Get Ready",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Next: ${currentExercise?.name ?: currentHabit?.effectiveName}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = bufferRemainingSeconds.toString(),
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
        } else if (isResting) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = "REST",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Green,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            val restTime = currentBlock?.block?.restAfterBlock ?: 60
            val remaining = (restTime - elapsedSeconds).coerceAtLeast(0)
            Text(
                text = formatTime(remaining),
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Next Set: $currentBlockSet / ${currentExercise?.sets ?: 1}")
        } else {
            Text(
                text = gauntlet.gauntlet.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (currentExercise != null || currentHabit != null) {
                val activeName = currentExercise?.name ?: currentHabit?.effectiveName ?: ""
                val activeSets = currentExercise?.sets
                val activeTarget = currentExercise?.targetReps ?: currentHabit?.habit?.estimatedDurationSeconds?.let { formatTime(it) } ?: "-"

                if (currentBlock != null) {
                    Text(
                        text = "${currentBlock.block.blockType} BLOCK",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                Text(
                    text = activeName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (activeSets != null) {
                    Text(
                        text = "Set $currentBlockSet of $activeSets",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Media Section
                val mediaUrl = currentHabit?.effectiveMediaUrl
                val mediaType = currentHabit?.effectiveMediaType ?: HabitMediaType.NONE
                if (mediaUrl != null && mediaType != HabitMediaType.NONE) {
                    HabitMedia(mediaType, mediaUrl)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                val timerText = formatTime(elapsedSeconds)
                Text(
                    text = "$timerText / $activeTarget",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Ghost Progress Section
                val habitId = currentHabit?.habit?.id ?: -1
                val pb = personalBests[habitId]
                if (pb != null && pb > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ghost (PB): ${if (currentHabit?.effectiveTrackingType == HabitTrackingType.REPS) "$pb Reps" else formatTime(pb)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                    
                    val ghostProgress = when (currentHabit?.effectiveTrackingType) {
                        HabitTrackingType.TIME -> (elapsedSeconds.toFloat() / pb.toFloat()).coerceIn(0f, 1f)
                        HabitTrackingType.REPS -> {
                            val goal = currentHabit.habit.estimatedDurationSeconds ?: 1
                            if (goal > 0) (pb.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
                        }
                        else -> 0f
                    }
                    
                    LinearProgressIndicator(
                        progress = { ghostProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        trackColor = Color.Transparent
                    )
                }

                // Rep Tracking Section
                if (currentExercise != null || currentHabit?.effectiveTrackingType == HabitTrackingType.REPS) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { onUpdateReps((currentReps - 1).coerceAtLeast(0)) }) {
                            Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "$currentReps Reps",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { onUpdateReps(currentReps + 1) }) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (currentHabit?.effectiveSubHabits?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Checklist", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            currentHabit.effectiveSubHabits.forEach { sub ->
                                var checked by remember(currentHabit.habit.id, sub) { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                                    Text(
                                        text = sub,
                                        textDecoration = if (checked) TextDecoration.LineThrough else null,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val nextLabel = nextExercise ?: nextHabit
                if (nextLabel != null) {
                    Text(
                        text = "UP NEXT: $nextLabel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
            
            if (!isInBuffer) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.SkipNext, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (nextHabit == null) "Finish" else "Next")
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun HabitMedia(mediaType: HabitMediaType, mediaUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (mediaType) {
            HabitMediaType.IMAGE, HabitMediaType.GIF -> {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            HabitMediaType.YOUTUBE -> {
                YouTubeWebView(mediaUrl)
            }
            else -> {}
        }
    }
}

@Composable
fun YouTubeWebView(videoUrl: String) {
    val videoId = remember(videoUrl) {
        val reg = Regex("(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})")
        reg.find(videoUrl)?.groups?.get(1)?.value
    }

    if (videoId == null) {
        Text("Invalid YouTube URL", color = Color.White)
        return
    }

    val embedUrl = "https://www.youtube.com/embed/$videoId"
    
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = android.webkit.WebViewClient()
                loadUrl(embedUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
