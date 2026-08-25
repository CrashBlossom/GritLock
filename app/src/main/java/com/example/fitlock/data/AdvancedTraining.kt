package com.example.fitlock.data

import androidx.room.*

@Entity(
    tableName = "flashcards",
    indices = [Index(value = ["deckName", "frontText", "backText"], unique = true)]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckName: String,
    val frontText: String,
    val backText: String,
    val allFieldsJson: String? = null, // Store all fields as JSON for flexible display
    val drawingData: String? = null, // JSON path data for Samsung Pen scribbles
    val easinessFactor: Double = 2.5,
    val interval: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val repetitions: Int = 0
)

@Entity(
    tableName = "workout_blocks",
    foreignKeys = [
        ForeignKey(
            entity = Gauntlet::class,
            parentColumns = ["id"],
            childColumns = ["gauntletId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gauntletId")]
)
data class WorkoutBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gauntletId: Int, // The routine/gauntlet it belongs to
    val blockType: String, // TRIPLET, PAIR, SINGLE
    val orderIndex: Int,
    val restAfterBlock: Int
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("blockId")]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val blockId: Int,
    val name: String,
    val sets: Int,
    val targetReps: String, // e.g., "5-8" or "30s"
    val restBetweenSets: Int,
    val orderIndex: Int,
    val progressionLevel: Int = 1
)

data class WorkoutBlockWithExercises(
    @Embedded val block: WorkoutBlockEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "blockId"
    )
    val exercises: List<WorkoutExercise>
)

data class GauntletWithAdvancedWorkout(
    @Embedded val gauntlet: Gauntlet,
    @Relation(
        entity = WorkoutBlockEntity::class,
        parentColumn = "id",
        entityColumn = "gauntletId"
    )
    val blocks: List<WorkoutBlockWithExercises>
)
