package com.example.fitlock.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

data class MorningRitual(
    val lastNightReflection: String? = null,
    val dreams: String? = null,
    val dailyGoals: String? = null,
    val personalGoal: String? = null,
    val workGoal: String? = null,
    val obstacles: String? = null,
    val solutions: String? = null,
    val gratitude: String? = null,
    val awe: String? = null,
    val morningIdeas: List<String> = emptyList(),
    val readListen: String? = null,
    val learn: String? = null
)

data class EveningRitual(
    val summary: String? = null,
    val story: String? = null,
    val accomplishments: String? = null,
    val wins: List<String> = emptyList(),
    val losses: List<String> = emptyList(),
    val resolutions: String? = null,
    val peaceOfMindGoal: String? = null,
    val mood: Int? = null,
    val energy: Int? = null,
    val eveningIdeas: List<String> = emptyList(),
    val meals: String? = null
)

@Entity(tableName = "daily_pledges")
data class DailyPledge(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val pledgeTimestamp: Long? = null,
    val reviewTimestamp: Long? = null,
    val status: String = "PENDING", // PENDING, COMMITTED, SUCCESS, RELAPSED
    
    @Embedded(prefix = "morn_") val morning: MorningRitual = MorningRitual(),
    @Embedded(prefix = "even_") val evening: EveningRitual = EveningRitual(),
    
    // Legacy fields for backward compatibility during migration
    val mainObjective: String? = null,
    val eveningReflection: String? = null
)
