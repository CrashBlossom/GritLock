package com.example.fitlock.data

import androidx.room.*
import java.util.UUID

@Entity(tableName = "exercise_families")
data class ExerciseFamily(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g., "Pushup Family"
    val description: String? = null,
    val icon: String? = null
)

@Entity(
    tableName = "progression_levels",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseFamily::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("familyId")]
)
data class ProgressionLevel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val familyId: String,
    val level: Int, // 1, 2, 3...
    val variantName: String, // e.g., "Diamond Pushup"
    val exerciseType: String, // Base type (PUSHUP, SQUAT etc for AI choice)
    val tempo: String = "3-1-1-1", // Eccentric-Bottom-Concentric-Top
    val unlockRequirementReps: Int = 15,
    val xpReward: Int = 100,
    val instructions: String? = null
)

data class FamilyWithLevels(
    @Embedded val family: ExerciseFamily,
    @Relation(
        parentColumn = "id",
        entityColumn = "familyId"
    )
    val levels: List<ProgressionLevel>
)
