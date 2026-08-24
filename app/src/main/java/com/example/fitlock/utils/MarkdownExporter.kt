package com.example.fitlock.utils

import android.content.Context
import com.example.fitlock.data.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class MarkdownExporter(private val context: Context) {

    private val db = GritLockDatabase.getDatabase(context)

    suspend fun generateDailyLog(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(date)
        
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endTime = calendar.timeInMillis

        val workouts = db.dao().getHistory().first().filter { it.timestamp in startTime..endTime }
        val blocks = db.dao().getBlockEvents().first().filter { it.timestamp in startTime..endTime }
        val urges = db.dao().getUrgeEvents().first().filter { it.timestamp in startTime..endTime }
        val notes = db.dao().getAllLogNotes().first().filter { it.timestamp in startTime..endTime }

        val sb = StringBuilder()
        sb.append("# GritLock Daily Discipline Log - $dateString\n\n")

        sb.append("## Overview\n")
        sb.append("- Total Workouts: ${workouts.size}\n")
        sb.append("- Total App Blocks: ${blocks.size}\n")
        sb.append("- Urges Resisted: ${urges.count { it.wasResisted }}\n\n")

        sb.append("## Timeline\n\n")

        val allEvents = mutableListOf<TimelineEvent>()
        workouts.forEach { allEvents.add(TimelineEvent(it.timestamp, "EXERCISE", "${it.repsCompleted} ${it.exerciseType} (+${it.xpGained} XP)")) }
        blocks.forEach { allEvents.add(TimelineEvent(it.timestamp, "BLOCK", "Blocked ${it.packageName.split(".").last()}")) }
        urges.forEach { allEvents.add(TimelineEvent(it.timestamp, "URGE", "${it.category} (${it.subCategory ?: "General"}) - ${it.comment ?: "No comment"}")) }
        notes.forEach { allEvents.add(TimelineEvent(it.timestamp, "NOTE", it.content)) }

        allEvents.sortBy { it.timestamp }

        allEvents.forEach { event ->
            sb.append("### [${timeFormat.format(Date(event.timestamp))}] ${event.type}\n")
            sb.append("${event.content}\n\n")
        }

        return sb.toString()
    }

    private data class TimelineEvent(val timestamp: Long, val type: String, val content: String)
}
