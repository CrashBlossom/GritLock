package com.example.fitlock.data

import androidx.room.*
import java.util.UUID

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val targetDate: Long? = null,
    val category: String = "General",
    val isCompleted: Boolean = false,
    val xpReward: Int = 500
)

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class Project(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val title: String,
    val status: String = "ACTIVE", // ACTIVE, ON_HOLD, ARCHIVED
    val xpReward: Int = 200
)

@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Milestone(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val xpReward: Int = 50
)

@Entity(
    tableName = "project_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ProjectTask(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val xpReward: Int = 20
)

data class ProjectWithDetails(
    @Embedded val project: Project,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val milestones: List<Milestone>,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val tasks: List<ProjectTask>
)

data class GoalWithProjects(
    @Embedded val goal: Goal,
    @Relation(
        entity = Project::class,
        parentColumn = "id",
        entityColumn = "goalId"
    )
    val projects: List<ProjectWithDetails>
)
