package com.example.fitlock.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.fitlock.MainActivity
import com.example.fitlock.R
import com.example.fitlock.data.GauntletHistory
import com.example.fitlock.data.GauntletWithAdvancedWorkout
import com.example.fitlock.data.GauntletWithHabits
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.HabitLog
import com.example.fitlock.data.HabitTrackingType
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class GauntletService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var db: GritLockDatabase
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private val CHANNEL_ID = "gauntlet_channel"
    private val NOTIFICATION_ID = 2001

    data class GauntletSession(
        val gauntlet: GauntletWithHabits,
        val advancedWorkout: GauntletWithAdvancedWorkout? = null,
        val currentHabitIndex: Int,
        val currentBlockIndex: Int = 0,
        val currentBlockExerciseIndex: Int = 0,
        val currentBlockSet: Int = 1,
        val elapsedSeconds: Int,
        val isInBuffer: Boolean = false,
        val isResting: Boolean = false,
        val bufferElapsedSeconds: Int = 0,
        val startTime: Long = System.currentTimeMillis(),
        val habitLogs: MutableList<HabitLog> = mutableListOf(),
        val currentReps: Int = 0,
        val personalBests: Map<Int, Int> = emptyMap() // Map of habit ID to PB value
    )

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_REPS = "ACTION_UPDATE_REPS"
        const val EXTRA_GAUNTLET_ID = "EXTRA_GAUNTLET_ID"
        const val EXTRA_REPS = "EXTRA_REPS"
        
        private val _sessionState = MutableStateFlow<GauntletSession?>(null)
        val sessionState = _sessionState.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        db = GritLockDatabase.getDatabase(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Immediate startForeground to prevent crash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createPlaceholderNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, createPlaceholderNotification())
        }

        when (intent?.action) {
            ACTION_START -> {
                val gauntletId = intent.getIntExtra(EXTRA_GAUNTLET_ID, -1)
                if (gauntletId != -1) {
                    startGauntlet(gauntletId)
                }
            }
            ACTION_NEXT -> nextHabit()
            ACTION_STOP -> stopGauntlet()
            ACTION_UPDATE_REPS -> {
                val reps = intent.getIntExtra(EXTRA_REPS, 0)
                _sessionState.value = _sessionState.value?.copy(currentReps = reps)
            }
        }
        return START_STICKY
    }

    private fun createPlaceholderNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Gauntlet Starting...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startGauntlet(gauntletId: Int) {
        serviceScope.launch {
            val gauntletWithHabits = withContext(Dispatchers.IO) {
                db.dao().getAllGauntletsWithHabits().first().find { it.gauntlet.id == gauntletId }
            }
            val advancedWorkout = withContext(Dispatchers.IO) {
                db.dao().getAdvancedWorkout(gauntletId).first()
            }
            
            val allHistory = withContext(Dispatchers.IO) { db.dao().getAllHistoryList() }
            val pbs = calculatePBs(gauntletWithHabits, allHistory)

            if (gauntletWithHabits != null) {
                _sessionState.value = GauntletSession(
                    gauntlet = gauntletWithHabits,
                    advancedWorkout = advancedWorkout,
                    currentHabitIndex = 0,
                    elapsedSeconds = 0,
                    personalBests = pbs
                )
                updateNotification() // Update with real data
                startTimer()
                
                // Focus Shield
                if (gauntletWithHabits.gauntlet.targetBlockGroupId != null) {
                    val shieldIntent = Intent(this@GauntletService, GritLockAccessibilityService::class.java).apply {
                        action = "ACTIVATE_FOCUS_SHIELD"
                        putExtra("block_group_id", gauntletWithHabits.gauntlet.targetBlockGroupId)
                        putExtra("whitelist", gauntletWithHabits.gauntlet.whitelistedPackages.toTypedArray())
                    }
                    startService(shieldIntent)
                }
            } else {
                stopSelf() // Stop if gauntlet not found
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timerRunnable = object : Runnable {
            override fun run() {
                val current = _sessionState.value ?: return
                
                if (current.isInBuffer) {
                    val newBufferElapsed = current.bufferElapsedSeconds + 1
                    if (newBufferElapsed >= current.gauntlet.gauntlet.bufferSeconds) {
                        _sessionState.value = current.copy(
                            isInBuffer = false,
                            bufferElapsedSeconds = 0,
                            elapsedSeconds = 0
                        )
                        vibrate(longArrayOf(0, 200, 100, 200)) // Double pulse to start
                    } else {
                        _sessionState.value = current.copy(bufferElapsedSeconds = newBufferElapsed)
                    }
                } else if (current.isResting) {
                    val newElapsed = current.elapsedSeconds + 1
                    val block = current.advancedWorkout?.blocks?.getOrNull(current.currentBlockIndex)
                    val restTime = block?.block?.restAfterBlock ?: 60
                    
                    if (newElapsed >= restTime) {
                        _sessionState.value = current.copy(
                            isResting = false,
                            elapsedSeconds = 0,
                            isInBuffer = true // Prep for next exercise
                        )
                        vibrate(longArrayOf(0, 200, 100, 200))
                    } else {
                        _sessionState.value = current.copy(elapsedSeconds = newElapsed)
                    }
                } else {
                    val newElapsed = current.elapsedSeconds + 1
                    _sessionState.value = current.copy(elapsedSeconds = newElapsed)
                    
                    // Sync to Wear OS
                    val habit = current.gauntlet.habits.getOrNull(current.currentHabitIndex)
                    val block = current.advancedWorkout?.blocks?.getOrNull(current.currentBlockIndex)
                    val exercise = block?.exercises?.getOrNull(current.currentBlockExerciseIndex)
                    
                    val activeName = exercise?.name ?: habit?.effectiveName ?: "Routine"
                    syncToWear(activeName, newElapsed)

                    val estimated = exercise?.targetReps?.toIntOrNull() ?: habit?.habit?.estimatedDurationSeconds
                    if (estimated != null && newElapsed == estimated) {
                        vibrate(200L) // Short pulse when estimation reached
                    }
                }
                
                updateNotification()
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(timerRunnable!!, 1000)
    }

    private fun syncToWear(habitName: String, elapsed: Int) {
        val request = PutDataMapRequest.create("/gauntlet_state").apply {
            dataMap.putString("habit_name", habitName)
            dataMap.putInt("elapsed", elapsed)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }
        val putDataRequest = request.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(putDataRequest)
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private fun vibrate(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun nextHabit() {
        val current = _sessionState.value ?: return
        
        // Handle Habit Stack first
        if (current.currentHabitIndex < current.gauntlet.habits.size) {
            val habitWithDef = current.gauntlet.habits.getOrNull(current.currentHabitIndex)
            if (habitWithDef != null) {
                val habit = habitWithDef.habit
                current.habitLogs.add(HabitLog(
                    habitId = habit.id,
                    name = habitWithDef.effectiveName,
                    actualDurationSeconds = current.elapsedSeconds,
                    estimatedDurationSeconds = habit.estimatedDurationSeconds,
                    repsCompleted = current.currentReps
                ))
                
                serviceScope.launch(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    val lastComp = habit.lastCompletionTimestamp
                    val isWithinWindow = lastComp > 0 && (now - lastComp) < 48 * 60 * 60 * 1000L
                    val newStreak = if (isWithinWindow) habit.currentStreak + 1 else 1
                    db.dao().upsertHabit(habit.copy(lastCompletionTimestamp = now, currentStreak = newStreak))
                }
            }

            if (current.currentHabitIndex < current.gauntlet.habits.size - 1) {
                _sessionState.value = current.copy(
                    currentHabitIndex = current.currentHabitIndex + 1,
                    elapsedSeconds = 0,
                    isInBuffer = true,
                    currentReps = 0
                )
                vibrate(100L)
                updateNotification()
                return
            } else if (current.advancedWorkout?.blocks?.isNotEmpty() == true) {
                // Move to Advanced Workout blocks
                _sessionState.value = current.copy(
                    currentHabitIndex = current.gauntlet.habits.size,
                    currentBlockIndex = 0,
                    currentBlockExerciseIndex = 0,
                    currentBlockSet = 1,
                    elapsedSeconds = 0,
                    isInBuffer = true,
                    currentReps = 0
                )
                vibrate(100L)
                updateNotification()
                return
            }
        } else if (current.advancedWorkout != null) {
            // Handle Advanced Workout Blocks
            val block = current.advancedWorkout.blocks.getOrNull(current.currentBlockIndex)
            if (block != null) {
                val exercise = block.exercises.getOrNull(current.currentBlockExerciseIndex)
                if (exercise != null) {
                    current.habitLogs.add(HabitLog(
                        habitId = -1,
                        name = exercise.name,
                        actualDurationSeconds = current.elapsedSeconds,
                        estimatedDurationSeconds = exercise.targetReps.toIntOrNull(),
                        repsCompleted = current.currentReps
                    ))
                }

                // Logic for PAIR/TRIPLET: move to next exercise in block
                if (current.currentBlockExerciseIndex < block.exercises.size - 1) {
                    _sessionState.value = current.copy(
                        currentBlockExerciseIndex = current.currentBlockExerciseIndex + 1,
                        elapsedSeconds = 0,
                        isInBuffer = true,
                        currentReps = 0
                    )
                } else if (current.currentBlockSet < (block.exercises.firstOrNull()?.sets ?: 1)) {
                    // Back to first exercise of block, increment set
                    _sessionState.value = current.copy(
                        currentBlockExerciseIndex = 0,
                        currentBlockSet = current.currentBlockSet + 1,
                        elapsedSeconds = 0,
                        isResting = true,
                        currentReps = 0
                    )
                } else if (current.currentBlockIndex < current.advancedWorkout.blocks.size - 1) {
                    // Next block
                    _sessionState.value = current.copy(
                        currentBlockIndex = current.currentBlockIndex + 1,
                        currentBlockExerciseIndex = 0,
                        currentBlockSet = 1,
                        elapsedSeconds = 0,
                        isInBuffer = true,
                        currentReps = 0
                    )
                } else {
                    stopGauntlet()
                    return
                }
                vibrate(100L)
                updateNotification()
                return
            }
        }

        stopGauntlet()
    }

    private fun stopGauntlet() {
        stopTimer()
        val current = _sessionState.value
        if (current != null) {
            saveHistory(current)
            // Deactivate Focus Shield
            val shieldIntent = Intent(this, GritLockAccessibilityService::class.java).apply {
                action = "DEACTIVATE_FOCUS_SHIELD"
            }
            startService(shieldIntent)
        }
        _sessionState.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun saveHistory(session: GauntletSession) {
        serviceScope.launch(Dispatchers.IO) {
            val totalTime = session.habitLogs.sumOf { it.actualDurationSeconds }
            val estimatedTime = session.habitLogs.sumOf { it.estimatedDurationSeconds ?: 0 }
            
            // Basic XP logic: performance vs estimation
            val efficiency = if (estimatedTime > 0) totalTime.toFloat() / estimatedTime.toFloat() else 1f
            var xp = session.gauntlet.habits.size * 20
            if (efficiency in 0.8f..1.2f) xp += 50 // Bonus for hitting estimations
            
            val streak = db.dao().getGauntletCountSince(session.gauntlet.gauntlet.id, System.currentTimeMillis() - 24 * 60 * 60 * 1000L) + 1

            val history = GauntletHistory(
                gauntletId = session.gauntlet.gauntlet.id,
                totalTimeSeconds = totalTime,
                estimatedTimeSeconds = estimatedTime,
                xpGained = xp,
                streakCount = streak,
                habitLogs = session.habitLogs
            )
            db.dao().insertGauntletHistory(history)
            
            // Update User Stats
            val stats = db.dao().getUserStats().first()
            if (stats != null) {
                db.dao().updateUserStats(stats.copy(
                    totalXp = stats.totalXp + xp,
                    lastWorkoutDate = System.currentTimeMillis()
                ))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "The Gauntlet",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active routine progress"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val session = _sessionState.value ?: return NotificationCompat.Builder(this, CHANNEL_ID).build()
        
        val title: String
        val content: String

        if (session.isInBuffer) {
            val remaining = session.gauntlet.gauntlet.bufferSeconds - session.bufferElapsedSeconds
            title = "Transitioning..."
            content = "Prep: ${formatTime(remaining)} left"
        } else if (session.isResting) {
            val block = session.advancedWorkout?.blocks?.getOrNull(session.currentBlockIndex)
            val restTime = block?.block?.restAfterBlock ?: 60
            val remaining = (restTime - session.elapsedSeconds).coerceAtLeast(0)
            title = "Resting"
            content = "Next set in ${formatTime(remaining)}"
        } else {
            val habit = session.gauntlet.habits.getOrNull(session.currentHabitIndex)
            val block = session.advancedWorkout?.blocks?.getOrNull(session.currentBlockIndex)
            val exercise = block?.exercises?.getOrNull(session.currentBlockExerciseIndex)
            
            val activeName = exercise?.name ?: habit?.effectiveName ?: "Routine"
            val elapsed = formatTime(session.elapsedSeconds)
            val expected = if (exercise != null) exercise.targetReps
                           else if (habit?.habit?.estimatedDurationSeconds != null) formatTime(habit.habit.estimatedDurationSeconds)
                           else "-"
            
            title = session.gauntlet.gauntlet.name
            content = "$activeName: $elapsed / $expected"
        }
        
        val nextIntent = Intent(this, GauntletService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, GauntletService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Next", nextPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    private fun calculatePBs(gauntlet: GauntletWithHabits?, history: List<GauntletHistory>): Map<Int, Int> {
        if (gauntlet == null) return emptyMap()
        val pbs = mutableMapOf<Int, Int>()
        
        gauntlet.habits.forEach { habitWithDef ->
            val habit = habitWithDef.habit
            val habitLogs = history.flatMap { it.habitLogs }.filter { it.habitId == habit.id }
            
            if (habitLogs.isNotEmpty()) {
                val best = if (habitWithDef.habit.trackingType == HabitTrackingType.REPS) {
                    habitLogs.mapNotNull { it.repsCompleted }.maxOrNull() ?: 0
                } else {
                    // For TIME, logic depends on higherIsBetter
                    if (habitWithDef.habit.higherIsBetter) {
                        habitLogs.maxOf { it.actualDurationSeconds }
                    } else {
                        habitLogs.minOf { it.actualDurationSeconds }
                    }
                }
                pbs[habit.id] = best
            }
        }
        return pbs
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
