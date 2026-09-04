package com.example.fitlock.utils

import android.content.Context
import android.os.Environment
import com.example.fitlock.data.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
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

        val pledge = db.dao().getPledgeForDate(dateString)
        val questId = "daily_$dateString"
        val quest = db.dao().getAllQuestsWithBlocks().first().find { it.quest.id == questId }

        val workouts = db.dao().getHistory().first().filter { it.timestamp in startTime..endTime }
        val blocks = db.dao().getBlockEvents().first().filter { it.timestamp in startTime..endTime }
        val urges = db.dao().getUrgeEvents().first().filter { it.timestamp in startTime..endTime }
        val notes = db.dao().getAllLogNotes().first().filter { it.timestamp in startTime..endTime }

        val sb = StringBuilder()
        sb.append("# GritLock Daily Discipline Log - $dateString\n\n")

        pledge?.let { p ->
            sb.append("## Daily Status: ${p.status}\n\n")
            sb.append("## Morning Ritual\n")
            if (p.morning.personalGoal != null) sb.append("- **Personal Objective**: ${p.morning.personalGoal}\n")
            if (p.morning.workGoal != null) sb.append("- **Work Objective**: ${p.morning.workGoal}\n")
            if (p.morning.gratitude != null) sb.append("- **Gratitude**: ${p.morning.gratitude}\n")
            if (p.morning.obstacles != null) sb.append("- **Obstacles**: ${p.morning.obstacles}\n")
            if (p.morning.solutions != null) sb.append("- **Solutions**: ${p.morning.solutions}\n")
            if (p.morning.awe != null) sb.append("- **Awe**: ${p.morning.awe}\n")
            if (p.morning.readListen != null) sb.append("- **Read/Listen**: ${p.morning.readListen}\n")
            sb.append("\n")
        }

        quest?.let { q ->
            sb.append("## Daily Quest: ${q.quest.name}\n")
            q.blocks.forEach { b ->
                val status = if (b.isCompleted) "[x]" else "[ ]"
                sb.append("$status ${b.name} (${b.type})\n")
            }
            sb.append("\n")
        }

        pledge?.let { p ->
            if (p.reviewTimestamp != null) {
                sb.append("## Evening Review\n")
                if (p.evening.accomplishments != null) sb.append("- **Main Accomplishments**: ${p.evening.accomplishments}\n")
                if (p.evening.wins.isNotEmpty()) sb.append("- **Wins**: ${p.evening.wins.joinToString(", ")}\n")
                if (p.evening.losses.isNotEmpty()) sb.append("- **Losses**: ${p.evening.losses.joinToString(", ")}\n")
                if (p.evening.mood != null) sb.append("- **Mood**: ${p.evening.mood}/10\n")
                if (p.evening.energy != null) sb.append("- **Energy**: ${p.evening.energy}/10\n")
                if (p.evening.meals != null) sb.append("- **Meals**: ${p.evening.meals}\n")
                if (p.evening.peaceOfMindGoal != null) sb.append("- **Peace of Mind Goal**: ${p.evening.peaceOfMindGoal}\n")
                sb.append("\n")
            }
        }

        sb.append("## Exercise & Discipline Overview\n")
        sb.append("- Total Workouts: ${workouts.size}\n")
        sb.append("- Total App Blocks: ${blocks.size}\n")
        sb.append("- Grit Resisted: ${urges.count { it.wasResisted }}\n\n")

        sb.append("## Timeline\n\n")

        val allEvents = mutableListOf<TimelineEvent>()
        workouts.forEach { allEvents.add(TimelineEvent(it.timestamp, "EXERCISE", "${it.repsCompleted} ${it.exerciseType} (+${it.xpGained} XP)")) }
        blocks.forEach { allEvents.add(TimelineEvent(it.timestamp, "BLOCK", "Blocked ${it.packageName.split(".").last()}")) }
        urges.forEach { allEvents.add(TimelineEvent(it.timestamp, "GRIT", "${it.category} (${it.subCategory ?: "General"}) - ${it.comment ?: "No comment"}")) }
        notes.forEach { allEvents.add(TimelineEvent(it.timestamp, "NOTE", it.content)) }

        allEvents.sortBy { it.timestamp }

        allEvents.forEach { event ->
            sb.append("### [${timeFormat.format(Date(event.timestamp))}] ${event.type}\n")
            sb.append("${event.content}\n\n")
        }

        return sb.toString()
    }

    fun saveLogToFile(content: String, date: Date): File? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = "gritlock_log_${dateFormat.format(date)}.md"
        
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (dir?.exists() == false) dir.mkdirs()
            
            val file = File(dir, fileName)
            FileOutputStream(file).use { 
                it.write(content.toByteArray())
            }
            file
        } catch (e: Exception) {
            android.util.Log.e("MarkdownExporter", "Error saving log: ${e.message}")
            null
        }
    }

    private data class TimelineEvent(val timestamp: Long, val type: String, val content: String)
}
