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
import com.example.fitlock.data.GauntletWithHabits
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.HabitLog
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
    
    private val _sessionState = MutableStateFlow<GauntletSession?>(null)
    val sessionState = _sessionState.asStateFlow()

    private val CHANNEL_ID = "gauntlet_channel"
    private val NOTIFICATION_ID = 2001

    data class GauntletSession(
        val gauntlet: GauntletWithHabits,
        val currentHabitIndex: Int,
        val elapsedSeconds: Int,
        val isInBuffer: Boolean = false,
        val bufferElapsedSeconds: Int = 0,
        val startTime: Long = System.currentTimeMillis(),
        val habitLogs: MutableList<HabitLog> = mutableListOf()
    )

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_GAUNTLET_ID = "EXTRA_GAUNTLET_ID"
        
        private val _sessionState = MutableStateFlow<GauntletSession?>(null)
        val sessionState = _sessionState.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        db = GritLockDatabase.getDatabase(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val gauntletId = intent.getIntExtra(EXTRA_GAUNTLET_ID, -1)
                if (gauntletId != -1) {
                    startGauntlet(gauntletId)
                }
            }
            ACTION_NEXT -> nextHabit()
            ACTION_STOP -> stopGauntlet()
        }
        return START_STICKY
    }

    private fun startGauntlet(gauntletId: Int) {
        serviceScope.launch {
            val gauntlet = withContext(Dispatchers.IO) {
                db.dao().getAllGauntletsWithHabits().first().find { it.gauntlet.id == gauntletId }
            }

            if (gauntlet != null) {
                _sessionState.value = GauntletSession(gauntlet, 0, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                startTimer()
                
                // Focus Shield
                if (gauntlet.gauntlet.targetBlockGroupId != null) {
                    val shieldIntent = Intent(this@GauntletService, GritLockAccessibilityService::class.java).apply {
                        action = "ACTIVATE_FOCUS_SHIELD"
                        putExtra("block_group_id", gauntlet.gauntlet.targetBlockGroupId)
                        putExtra("whitelist", gauntlet.gauntlet.whitelistedPackages.toTypedArray())
                    }
                    startService(shieldIntent)
                }
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
                } else {
                    val newElapsed = current.elapsedSeconds + 1
                    _sessionState.value = current.copy(elapsedSeconds = newElapsed)
                    
                    // Sync to Wear OS
                    val habit = current.gauntlet.habits.getOrNull(current.currentHabitIndex)
                    syncToWear(habit?.name ?: "Routine", newElapsed)

                    if (habit?.estimatedDurationSeconds != null && newElapsed == habit.estimatedDurationSeconds) {
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
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun vibrate(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun nextHabit() {
        val current = _sessionState.value ?: return
        val habit = current.gauntlet.habits.getOrNull(current.currentHabitIndex)
        
        if (habit != null) {
            current.habitLogs.add(HabitLog(
                habitId = habit.id,
                name = habit.name,
                actualDurationSeconds = current.elapsedSeconds,
                estimatedDurationSeconds = habit.estimatedDurationSeconds
            ))
        }

        if (current.currentHabitIndex < current.gauntlet.habits.size - 1) {
            if (current.gauntlet.gauntlet.bufferSeconds > 0) {
                _sessionState.value = current.copy(
                    isInBuffer = true,
                    bufferElapsedSeconds = 0,
                    currentHabitIndex = current.currentHabitIndex + 1
                )
            } else {
                _sessionState.value = current.copy(
                    currentHabitIndex = current.currentHabitIndex + 1,
                    elapsedSeconds = 0
                )
            }
            vibrate(100L)
            updateNotification()
        } else {
            stopGauntlet()
        }
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
        
        val content = if (session.isInBuffer) {
            val remaining = session.gauntlet.gauntlet.bufferSeconds - session.bufferElapsedSeconds
            "Prep: ${formatTime(remaining)} left"
        } else {
            val habit = session.gauntlet.habits.getOrNull(session.currentHabitIndex) ?: return NotificationCompat.Builder(this, CHANNEL_ID).build()
            val elapsed = formatTime(session.elapsedSeconds)
            val expected = if (habit.estimatedDurationSeconds != null) {
                "/ ${formatTime(habit.estimatedDurationSeconds)}"
            } else "/ -"
            "${habit.name}: $elapsed $expected"
        }
        
        val title = if (session.isInBuffer) "Transitioning..." else session.gauntlet.gauntlet.name

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
