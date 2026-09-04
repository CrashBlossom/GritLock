package com.example.fitlock.data

import androidx.room.*
import java.util.UUID

enum class QuestType {
    ROUTINE,          // Recurring (like the old Gauntlets)
    PROJECT_PHASE,    // One-off (part of a larger Goal/Project)
    DAILY_COMMITMENT, // The interactive "Daily Quest" built in Planning Mode
    CHALLENGE         // Specific high-reward trials
}

enum class BlockType {
    HABIT,            // Atomic recurring discipline (Floss, Cold Shower)
    EXERCISE,         // Physical requirements (AI/Sensor tracked)
    MENTAL,           // Cognitive tasks (Flashcards, Meditation)
    TASK,             // One-off to-do items (Automatic carry-over)
    ACTION            // System triggers (Start focus timer)
}

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: QuestType = QuestType.ROUTINE,
    val goalId: String? = null,      // Link to long-term Goal
    val projectId: String? = null,   // Link to concrete Project
    val milestoneId: String? = null, // Link to specific Milestone
    
    // Triggering & Scheduling
    val triggerType: GauntletTriggerType = GauntletTriggerType.MANUAL,
    val triggerTime: String? = null, // HH:mm
    val physicalTriggerType: PhysicalTriggerType = PhysicalTriggerType.NONE,
    val physicalTriggerData: String? = null,
    
    // The Physical Firewall (Lock Shield)
    val isLockShieldEnabled: Boolean = false,
    val targetBlockGroupId: Int? = null,
    val unlockAtEndOnly: Boolean = true,
    
    // RPG & Theming
    val xpReward: Int = 10,
    val icon: String? = null,
    val themeOverride: String? = null, // e.g., "Mission", "Rite"
    
    // Tracking for Challenges
    val isCompletedToday: Boolean = false,
    val lastCompletedTimestamp: Long = 0L
)

@Entity(
    tableName = "quest_blocks",
    foreignKeys = [
        ForeignKey(
            entity = Quest::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questId")]
)
data class QuestBlock(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val questId: String,
    val type: BlockType,
    val orderIndex: Int,
    
    // Core Identity
    val name: String,
    val isCompleted: Boolean = false,
    val completionTimestamp: Long? = null,
    
    // Type-Specific Data
    val definitionId: Int? = null,     // For HABIT
    val estimatedDurationSeconds: Int? = 60, // For HABIT
    val subHabits: List<String> = emptyList(), // For HABIT
    val mediaUrl: String? = null,      // For HABIT
    val mediaType: HabitMediaType = HabitMediaType.NONE, // For HABIT
    val trackingType: HabitTrackingType = HabitTrackingType.TIME, // For HABIT
    
    val sets: Int? = null,             // For EXERCISE
    val targetReps: String? = null,    // For EXERCISE
    val restBetweenSets: Int? = 90,    // For EXERCISE
    
    val autoCarryOver: Boolean = true, // For TASK
    val sourceTaskId: String? = null,  // Link back to ProjectTask
    
    val deckId: Int? = null,           // For MENTAL (Flashcards)
    val actionType: String? = null,    // For ACTION (START_TIMER, etc.)
    
    // Persistence & Stats
    val currentStreak: Int = 0,
    val lastDoneTimestamp: Long = 0L,
    val higherIsBetter: Boolean = true
)

data class QuestWithBlocks(
    @Embedded val quest: Quest,
    @Relation(
        parentColumn = "id",
        entityColumn = "questId"
    )
    val blocks: List<QuestBlock>
)
