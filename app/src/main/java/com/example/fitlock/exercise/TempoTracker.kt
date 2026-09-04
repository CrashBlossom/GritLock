package com.example.fitlock.exercise

import android.os.Handler
import android.os.Looper

enum class movementPhase { ECCENTRIC, BOTTOM, CONCENTRIC, TOP }

class TempoTracker(
    private val tempo: String, // e.g., "3-1-1-1"
    private val onPhaseSpeak: (String) -> Unit,
    private val onRepComplete: () -> Unit
) {
    private val parts = tempo.split("-").map { it.toIntOrNull() ?: 1 }
    private val eccentricTime = parts[0]
    private val bottomTime = parts[1]
    private val concentricTime = parts[2]
    private val topTime = parts[3]

    private var currentPhase = movementPhase.TOP
    private var phaseStartTime = 0L
    private var lastSecondSpoken = -1

    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null

    fun start() {
        phaseStartTime = System.currentTimeMillis()
        currentPhase = movementPhase.TOP
        startTicker()
    }

    private fun startTicker() {
        ticker = object : Runnable {
            override fun run() {
                update()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(ticker!!)
    }

    fun onMovementDetected(isMovingDown: Boolean, isAtBottom: Boolean, isAtTop: Boolean) {
        val now = System.currentTimeMillis()
        val elapsed = (now - phaseStartTime) / 1000

        when (currentPhase) {
            movementPhase.TOP -> {
                if (isMovingDown) {
                    currentPhase = movementPhase.ECCENTRIC
                    phaseStartTime = now
                    onPhaseSpeak("Down")
                }
            }
            movementPhase.ECCENTRIC -> {
                if (isAtBottom) {
                    currentPhase = movementPhase.BOTTOM
                    phaseStartTime = now
                    onPhaseSpeak("Hold")
                } else if (elapsed.toInt() > lastSecondSpoken && elapsed <= eccentricTime) {
                    lastSecondSpoken = elapsed.toInt()
                    if (lastSecondSpoken > 0) onPhaseSpeak(lastSecondSpoken.toString())
                }
            }
            movementPhase.BOTTOM -> {
                if (!isAtBottom && !isMovingDown) {
                    currentPhase = movementPhase.CONCENTRIC
                    phaseStartTime = now
                    onPhaseSpeak("Up")
                }
            }
            movementPhase.CONCENTRIC -> {
                if (isAtTop) {
                    currentPhase = movementPhase.TOP
                    phaseStartTime = now
                    onRepComplete()
                    onPhaseSpeak("Top")
                }
            }
        }
    }

    private fun update() {
        // Ticker for time-based cues if movement is static or slow
    }

    fun stop() {
        ticker?.let { handler.removeCallbacks(it) }
    }
}
