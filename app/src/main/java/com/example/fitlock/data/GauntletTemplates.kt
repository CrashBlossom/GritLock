package com.example.fitlock.data

object GauntletTemplates {
    data class Template(
        val gauntlet: Gauntlet,
        val habits: List<Habit>
    )

    val templates = listOf(
        Template(
            gauntlet = Gauntlet(name = "Morning Discipline", icon = "wb_sunny", bufferSeconds = 15),
            habits = listOf(
                Habit(gauntletId = 0, name = "Make Bed", estimatedDurationSeconds = 30, orderIndex = 0),
                Habit(gauntletId = 0, name = "Hydrate (500ml)", estimatedDurationSeconds = 20, orderIndex = 1),
                Habit(gauntletId = 0, name = "Stretching Flow", estimatedDurationSeconds = 300, orderIndex = 2),
                Habit(gauntletId = 0, name = "Cold Shower / Splash", estimatedDurationSeconds = 120, orderIndex = 3),
                Habit(gauntletId = 0, name = "Morning Reflection", estimatedDurationSeconds = 300, orderIndex = 4)
            )
        ),
        Template(
            gauntlet = Gauntlet(name = "Deep Focus Session", icon = "psychology", bufferSeconds = 30),
            habits = listOf(
                Habit(gauntletId = 0, name = "Clear Physical Distractions", estimatedDurationSeconds = 60, orderIndex = 0),
                Habit(gauntletId = 0, name = "Close Digital Distractions", estimatedDurationSeconds = 60, orderIndex = 1),
                Habit(gauntletId = 0, name = "Define Single Objective", estimatedDurationSeconds = 120, orderIndex = 2),
                Habit(gauntletId = 0, name = "Deep Work Block", estimatedDurationSeconds = 1500, orderIndex = 3)
            )
        ),
        Template(
            gauntlet = Gauntlet(name = "End of Day Shutdown", icon = "task_alt", bufferSeconds = 10),
            habits = listOf(
                Habit(gauntletId = 0, name = "Clean Workspace", estimatedDurationSeconds = 120, orderIndex = 0),
                Habit(gauntletId = 0, name = "Review Tomorrow's Tasks", estimatedDurationSeconds = 300, orderIndex = 1),
                Habit(gauntletId = 0, name = "Power Down Systems", estimatedDurationSeconds = 30, orderIndex = 2),
                Habit(gauntletId = 0, name = "Transition Meditation", estimatedDurationSeconds = 300, orderIndex = 3)
            )
        ),
        Template(
            gauntlet = Gauntlet(name = "Bathroom Micro-Workout", icon = "fitness_center", bufferSeconds = 5),
            habits = listOf(
                Habit(gauntletId = 0, name = "Deep Squats (x20)", estimatedDurationSeconds = 40, orderIndex = 0),
                Habit(gauntletId = 0, name = "Wall Sit", estimatedDurationSeconds = 60, orderIndex = 1),
                Habit(gauntletId = 0, name = "Calf Raises", estimatedDurationSeconds = 40, orderIndex = 2)
            )
        ),
        Template(
            gauntlet = Gauntlet(name = "Reddit Recommended Routine", icon = "fitness_center", bufferSeconds = 60),
            habits = listOf(
                Habit(gauntletId = 0, name = "Warmup Flow", estimatedDurationSeconds = 600, orderIndex = 0),
                Habit(gauntletId = 0, name = "Pair: Pull-up / Dip", estimatedDurationSeconds = 600, orderIndex = 1),
                Habit(gauntletId = 0, name = "Pair: Squat / Hinge", estimatedDurationSeconds = 600, orderIndex = 2),
                Habit(gauntletId = 0, name = "Pair: Push-up / Row", estimatedDurationSeconds = 600, orderIndex = 3),
                Habit(gauntletId = 0, name = "Core Triplet", estimatedDurationSeconds = 600, orderIndex = 4)
            )
        ),
        Template(
            gauntlet = Gauntlet(name = "Evening Wind-down", icon = "bedtime", bufferSeconds = 20),
            habits = listOf(
                Habit(gauntletId = 0, name = "Digital Sunset (No Screens)", estimatedDurationSeconds = 60, orderIndex = 0),
                Habit(gauntletId = 0, name = "Journaling / Gratitude", estimatedDurationSeconds = 300, orderIndex = 1),
                Habit(gauntletId = 0, name = "Reading", estimatedDurationSeconds = 900, orderIndex = 2),
                Habit(gauntletId = 0, name = "Yoga Nidra", estimatedDurationSeconds = 600, orderIndex = 3)
            )
        )
    )
}
