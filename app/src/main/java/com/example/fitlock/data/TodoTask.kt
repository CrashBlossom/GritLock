package com.example.fitlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TodoTask entity represents a "Quest" in our RPG system.
 * Completing these tasks increases the user's Intelligence (INT) stat.
 */
@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    // Prediction logic: how many 25-minute Pomodoro sessions will this take?
    val predictedPomodoros: Int = 1,
    // Tracking logic: how many sessions did it actually take?
    val actualPomodoros: Int = 0,
    val isCompleted: Boolean = false,
    val priority: Int = 0, // 0 = Normal, 1 = Urgent (High Intelligence reward)
    val timestamp: Long = System.currentTimeMillis()
)
