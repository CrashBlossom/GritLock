package com.example.fitlock.utils

import com.example.fitlock.data.Achievement
import com.example.fitlock.data.WorkoutHistory
import com.example.fitlock.data.UserStats
import kotlinx.coroutines.flow.first

object AchievementsManager {
    fun getDefaultAchievements(): List<Achievement> {
        val list = mutableListOf<Achievement>()
        val baseCategories = listOf(
            "STREAK", "REPS", "BREATHWORK", "WRITING", "WATER", "GATES", "LEVEL",
            "PUSHUP", "SQUAT", "PLANK", "PULLUP", "DIP", "SITUP", "HINGE", "ROW",
            "STEPS", "ROWING", "CALORIES", "MORNING_PLAN", "EVENING_REVIEW", "ANKI_FOCUS",
            "PUSHUP_STREAK", "SQUAT_STREAK", "PLANK_STREAK", "BREATHWORK_STREAK"
        )
        
        // Tiers: 0 (Starter), 1 (Bronze), 2 (Silver), 3 (Gold), 4 (Diamond), 5 (Legend)
        val tiers = listOf(
            Triple(0, "Starter", 25),
            Triple(1, "Bronze", 50),
            Triple(2, "Silver", 150),
            Triple(3, "Gold", 500),
            Triple(4, "Diamond", 2000),
            Triple(5, "Legend", 10000)
        )

        baseCategories.forEach { cat ->
            tiers.forEach { (tier, _, xp) ->
                val (target, title, desc) = when (cat) {
                    "STREAK" -> when(tier) {
                        0 -> Triple(2L, "First Spark", "Maintain a 2-day streak")
                        1 -> Triple(3L, "Spark of Will", "Maintain a 3-day streak")
                        2 -> Triple(7L, "Unshakable Routine", "Maintain a 7-day streak")
                        3 -> Triple(30L, "Iron Fortress", "Maintain a 30-day streak")
                        4 -> Triple(100L, "Unbroken Citadel", "Maintain a 100-day streak")
                        else -> Triple(365L, "Eternal Sentinel", "Maintain a 365-day streak")
                    }
                    "REPS" -> when(tier) {
                        0 -> Triple(10L, "First Rep", "Complete 10 lifetime reps")
                        1 -> Triple(100L, "Novice Initiate", "Complete 100 lifetime reps")
                        2 -> Triple(1000L, "Conditioned Muscle", "Complete 1,000 lifetime reps")
                        3 -> Triple(5000L, "Titanium Frame", "Complete 5,000 lifetime reps")
                        4 -> Triple(25000L, "Juggernaut", "Complete 25,000 lifetime reps")
                        else -> Triple(100000L, "Demi-God", "Complete 100,000 lifetime reps")
                    }
                    "PUSHUP" -> when(tier) {
                        0 -> Triple(10L, "First Pushup", "Complete 10 pushups")
                        1 -> Triple(50L, "Pushup Novice", "Complete 50 pushups")
                        2 -> Triple(250L, "Pushup Regular", "Complete 250 pushups")
                        3 -> Triple(1000L, "Pushup Veteran", "Complete 1,000 pushups")
                        4 -> Triple(5000L, "Pushup Elite", "Complete 5,000 pushups")
                        else -> Triple(20000L, "Pushup God", "Complete 20,000 pushups")
                    }
                    "PUSHUP_STREAK" -> when(tier) {
                        0 -> Triple(2L, "Pushup Habit", "Do pushups 2 days in a row")
                        1 -> Triple(3L, "Pushup Streak I", "Do pushups 3 days in a row")
                        2 -> Triple(7L, "Pushup Streak II", "Do pushups 7 days in a row")
                        3 -> Triple(30L, "Pushup Master", "Do pushups 30 days in a row")
                        4 -> Triple(100L, "Pushup Titan", "Do pushups 100 days in a row")
                        else -> Triple(365L, "Pushup Legend", "Do pushups 365 days in a row")
                    }
                    "SQUAT" -> when(tier) {
                        0 -> Triple(10L, "First Squat", "Complete 10 squats")
                        1 -> Triple(50L, "Squat Novice", "Complete 50 squats")
                        2 -> Triple(250L, "Squat Regular", "Complete 250 squats")
                        3 -> Triple(1000L, "Squat Veteran", "Complete 1,000 squats")
                        4 -> Triple(5000L, "Squat Elite", "Complete 5,000 squats")
                        else -> Triple(20000L, "Squat God", "Complete 20,000 squats")
                    }
                    "SQUAT_STREAK" -> when(tier) {
                        0 -> Triple(2L, "Squat Habit", "Do squats 2 days in a row")
                        1 -> Triple(3L, "Squat Streak I", "Do squats 3 days in a row")
                        2 -> Triple(7L, "Squat Streak II", "Do squats 7 days in a row")
                        3 -> Triple(30L, "Squat Master", "Do squats 30 days in a row")
                        4 -> Triple(100L, "Squat Titan", "Do squats 100 days in a row")
                        else -> Triple(365L, "Squat Legend", "Do squats 365 days in a row")
                    }
                    "PLANK" -> when(tier) {
                        0 -> Triple(30L, "First Plank", "Hold plank for 30 seconds")
                        1 -> Triple(180L, "Plank Novice", "Accumulate 3 mins of planks")
                        2 -> Triple(900L, "Plank Regular", "Accumulate 15 mins of planks")
                        3 -> Triple(3600L, "Plank Veteran", "Accumulate 1 hour of planks")
                        4 -> Triple(18000L, "Plank Elite", "Accumulate 5 hours of planks")
                        else -> Triple(72000L, "Core Pillar", "Accumulate 20 hours of planks")
                    }
                    "PLANK_STREAK" -> when(tier) {
                        0 -> Triple(2L, "Plank Habit", "Do planks 2 days in a row")
                        1 -> Triple(3L, "Plank Streak I", "Do planks 3 days in a row")
                        2 -> Triple(7L, "Plank Streak II", "Do planks 7 days in a row")
                        3 -> Triple(30L, "Plank Master", "Do planks 30 days in a row")
                        4 -> Triple(100L, "Plank Titan", "Do planks 100 days in a row")
                        else -> Triple(365L, "Plank Legend", "Do planks 365 days in a row")
                    }
                    "PULLUP" -> when(tier) {
                        0 -> Triple(1L, "First Pullup", "Complete 1 pullup")
                        1 -> Triple(20L, "Pullup Novice", "Complete 20 pullups")
                        2 -> Triple(100L, "Pullup Regular", "Complete 100 pullups")
                        3 -> Triple(500L, "Pullup Veteran", "Complete 500 pullups")
                        4 -> Triple(2000L, "Pullup Elite", "Complete 2,000 pullups")
                        else -> Triple(10000L, "Back of Steel", "Complete 10,000 pullups")
                    }
                    "DIP" -> when(tier) {
                        0 -> Triple(1L, "First Dip", "Complete 1 dip")
                        1 -> Triple(25L, "Dip Novice", "Complete 25 dips")
                        2 -> Triple(150L, "Dip Regular", "Complete 150 dips")
                        3 -> Triple(600L, "Dip Veteran", "Complete 600 dips")
                        4 -> Triple(2500L, "Dip Elite", "Complete 2,500 dips")
                        else -> Triple(10000L, "Triceps of Titan", "Complete 10,000 dips")
                    }
                    "SITUP" -> when(tier) {
                        0 -> Triple(10L, "First Situp", "Complete 10 situps")
                        1 -> Triple(50L, "Situp Novice", "Complete 50 situps")
                        2 -> Triple(250L, "Situp Regular", "Complete 250 situps")
                        3 -> Triple(1000L, "Situp Veteran", "Complete 1,000 situps")
                        4 -> Triple(5000L, "Situp Elite", "Complete 5,000 situps")
                        else -> Triple(20000L, "Iron Abdominals", "Complete 20,000 situps")
                    }
                    "HINGE" -> when(tier) {
                        0 -> Triple(10L, "First Hinge", "Complete 10 hinges")
                        1 -> Triple(50L, "Hinge Novice", "Complete 50 hinges")
                        2 -> Triple(250L, "Hinge Regular", "Complete 250 hinges")
                        3 -> Triple(1000L, "Hinge Veteran", "Complete 1,000 hinges")
                        4 -> Triple(5000L, "Hinge Elite", "Complete 5,000 hinges")
                        else -> Triple(20000L, "Posterior Chain", "Complete 20,000 hinges")
                    }
                    "ROW" -> when(tier) {
                        0 -> Triple(10L, "First Row", "Complete 10 rows")
                        1 -> Triple(50L, "Row Novice", "Complete 50 rows")
                        2 -> Triple(250L, "Row Regular", "Complete 250 rows")
                        3 -> Triple(1000L, "Row Veteran", "Complete 1,000 rows")
                        4 -> Triple(5000L, "Row Elite", "Complete 5,000 rows")
                        else -> Triple(20000L, "Row Master", "Complete 20,000 rows")
                    }
                    "BREATHWORK" -> when(tier) {
                        0 -> Triple(60L, "First Breath", "60 seconds of breathwork")
                        1 -> Triple(300L, "Calm Inhale", "300 seconds of breathwork")
                        2 -> Triple(1800L, "Centered Focus", "1,800 seconds of breathwork")
                        3 -> Triple(7200L, "Zenith Resilience", "7,200 seconds of breathwork")
                        4 -> Triple(30000L, "Stoic Inner Peace", "30,000 seconds of breathwork")
                        else -> Triple(100000L, "Monk of the Void", "100,000 seconds of breathwork")
                    }
                    "BREATHWORK_STREAK" -> when(tier) {
                        0 -> Triple(2L, "Breathing Habit", "Breathwork 2 days in a row")
                        1 -> Triple(3L, "Breathwork Streak I", "Breathwork 3 days in a row")
                        2 -> Triple(7L, "Breathwork Streak II", "Breathwork 7 days in a row")
                        3 -> Triple(30L, "Zenith Streak", "Breathwork 30 days in a row")
                        4 -> Triple(100L, "Inner Peace Master", "Breathwork 100 days in a row")
                        else -> Triple(365L, "Immortal Breath", "Breathwork 365 days in a row")
                    }
                    "WRITING" -> when(tier) {
                        0 -> Triple(50L, "First Sentence", "Write 50 words")
                        1 -> Triple(250L, "Inkling", "Write 250 words")
                        2 -> Triple(1500L, "Chronicler", "Write 1,500 words")
                        3 -> Triple(7500L, "Philosopher", "Write 7,500 words")
                        4 -> Triple(30000L, "Master Historian", "Write 30,000 words")
                        else -> Triple(100000L, "Architect of Lore", "Write 100,000 words")
                    }
                    "WATER" -> when(tier) {
                        0 -> Triple(1L, "First Sip", "Log 1 liter of water")
                        1 -> Triple(10L, "Spring Droplet", "Log 10 liters of water")
                        2 -> Triple(50L, "Stream Runner", "Log 50 liters of water")
                        3 -> Triple(250L, "River Torrent", "Log 250 liters of water")
                        4 -> Triple(1000L, "Oceanic Reservoir", "Log 1,000 liters of water")
                        else -> Triple(5000L, "Source of Life", "Log 5,000 liters of water")
                    }
                    "STEPS" -> when(tier) {
                        0 -> Triple(1000L, "First Steps", "Walk 1,000 steps")
                        1 -> Triple(10000L, "Walker", "Walk 10,000 steps")
                        2 -> Triple(100000L, "Hiker", "Walk 100,000 steps")
                        3 -> Triple(500000L, "Nomad", "Walk 500,000 steps")
                        4 -> Triple(2000000L, "Explorer", "Walk 2,000,000 steps")
                        else -> Triple(10000000L, "Globetrotter", "Walk 10,000,000 steps")
                    }
                    "ROWING" -> when(tier) {
                        0 -> Triple(5L, "First Row Min", "Row for 5 minutes")
                        1 -> Triple(30L, "Row Novice", "Row for 30 minutes")
                        2 -> Triple(150L, "Row Regular", "Row for 150 minutes")
                        3 -> Triple(600L, "Row Veteran", "Row for 10 hours")
                        4 -> Triple(2400L, "Row Elite", "Row for 40 hours")
                        else -> Triple(10000L, "Master Oarsman", "Row for 166 hours")
                    }
                    "CALORIES" -> when(tier) {
                        0 -> Triple(100L, "Spark", "Burn 100 active calories")
                        1 -> Triple(1000L, "Calorie Burner I", "Burn 1,000 active calories")
                        2 -> Triple(10000L, "Calorie Burner II", "Burn 10,000 active calories")
                        3 -> Triple(50000L, "Furnace", "Burn 50,000 active calories")
                        4 -> Triple(200000L, "Inferno", "Burn 200,000 active calories")
                        else -> Triple(1000000L, "Supernova", "Burn 1,000,000 active calories")
                    }
                    "MORNING_PLAN" -> when(tier) {
                        0 -> Triple(1L, "Dawn Awakening", "Complete 1 morning plan")
                        1 -> Triple(7L, "Morning Routine I", "Complete 7 morning plans")
                        2 -> Triple(30L, "Early Bird", "Complete 30 morning plans")
                        3 -> Triple(90L, "Sun Worshipper", "Complete 90 morning plans")
                        4 -> Triple(180L, "Dawn Commander", "Complete 180 morning plans")
                        else -> Triple(365L, "Master of Sunrise", "Complete 365 morning plans")
                    }
                    "EVENING_REVIEW" -> when(tier) {
                        0 -> Triple(1L, "Night Reflection", "Complete 1 evening review")
                        1 -> Triple(7L, "Evening Routine I", "Complete 7 evening reviews")
                        2 -> Triple(30L, "Stoic Evening", "Complete 30 evening reviews")
                        3 -> Triple(90L, "Night Watch", "Complete 90 evening reviews")
                        4 -> Triple(180L, "Twilight Sage", "Complete 180 evening reviews")
                        else -> Triple(365L, "Master of Sunset", "Complete 365 evening reviews")
                    }
                    "ANKI_FOCUS" -> when(tier) {
                        0 -> Triple(300L, "First Recall", "5 minutes in AnkiDroid focus")
                        1 -> Triple(1800L, "Memory Novice", "30 minutes in AnkiDroid focus")
                        2 -> Triple(7200L, "Memory Scholar", "2 hours in AnkiDroid focus")
                        3 -> Triple(36000L, "Memory Expert", "10 hours in AnkiDroid focus")
                        4 -> Triple(108000L, "Memory Master", "30 hours in AnkiDroid focus")
                        else -> Triple(360000L, "Living Archive", "100 hours in AnkiDroid focus")
                    }
                    "GATES" -> when(tier) {
                        0 -> Triple(1L, "First Gate", "Clear 1 protocol gate")
                        1 -> Triple(10L, "Gate Watcher", "Clear 10 protocol gates")
                        2 -> Triple(50L, "Gate Warden", "Clear 50 protocol gates")
                        3 -> Triple(250L, "Master of Portals", "Clear 250 protocol gates")
                        4 -> Triple(1000L, "Realm Architect", "Clear 1,000 protocol gates")
                        else -> Triple(5000L, "Lord of Labyrinth", "Clear 5,000 protocol gates")
                    }
                    else -> when(tier) { // LEVEL
                        0 -> Triple(2L, "Awakening", "Reach overall level 2")
                        1 -> Triple(5L, "Adept", "Reach overall level 5")
                        2 -> Triple(15L, "Expert", "Reach overall level 15")
                        3 -> Triple(30L, "Master", "Reach overall level 30")
                        4 -> Triple(60L, "Grandmaster", "Reach overall level 60")
                        else -> Triple(100L, "Transcendent", "Reach overall level 100")
                    }
                }

                list.add(
                    Achievement(
                        id = "${cat}_T$tier",
                        category = cat,
                        tier = tier,
                        title = title,
                        description = desc,
                        currentProgress = 0L,
                        targetProgress = target,
                        isUnlocked = false,
                        isClaimed = false,
                        xpReward = xp
                    )
                )
            }
        }
        return list
    }

    suspend fun evaluateAndUpdateAchievements(
        dao: com.example.fitlock.data.GritLockDao,
        history: List<WorkoutHistory>,
        stats: UserStats?,
        waterTotalLiters: Double,
        wordCountTotal: Long,
        completedGatesCount: Long,
        morningPlanCount: Long,
        eveningReviewCount: Long,
        ankiFocusSeconds: Long
    ) {
        val existingAchievements = dao.getAllAchievements().first()
        if (existingAchievements.isEmpty()) return

        val pushupTotal = history.filter { it.exerciseType.equals("PUSHUP", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val squatTotal = history.filter { it.exerciseType.equals("SQUAT", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val plankTotal = history.filter { it.exerciseType.equals("PLANK", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val pullupTotal = history.filter { it.exerciseType.equals("PULLUP", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val dipTotal = history.filter { it.exerciseType.equals("DIP", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val situpTotal = history.filter { it.exerciseType.equals("SITUP", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val hingeTotal = history.filter { it.exerciseType.equals("HINGE", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val rowTotal = history.filter { it.exerciseType.equals("ROW", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        val breathworkTotal = history.filter { it.exerciseType.equals("BREATHWORK", ignoreCase = true) }.sumOf { it.repsCompleted }.toLong()
        
        val totalReps = history.sumOf { it.repsCompleted }.toLong()
        val currentStreak = stats?.currentStreak?.toLong() ?: 0L
        val level = stats?.level?.toLong() ?: 1L

        val updated = existingAchievements.map { ach ->
            val progress = when (ach.category) {
                "STREAK" -> currentStreak
                "REPS" -> totalReps
                "PUSHUP" -> pushupTotal
                "SQUAT" -> squatTotal
                "PLANK" -> plankTotal
                "PULLUP" -> pullupTotal
                "DIP" -> dipTotal
                "SITUP" -> situpTotal
                "HINGE" -> hingeTotal
                "ROW" -> rowTotal
                "BREATHWORK" -> breathworkTotal
                "WRITING" -> wordCountTotal
                "WATER" -> waterTotalLiters.toLong()
                "GATES" -> completedGatesCount
                "LEVEL" -> level
                "MORNING_PLAN" -> morningPlanCount
                "EVENING_REVIEW" -> eveningReviewCount
                "ANKI_FOCUS" -> ankiFocusSeconds
                else -> 0L
            }

            val target = ach.targetProgress
            val unlocked = progress >= target
            ach.copy(
                currentProgress = progress,
                isUnlocked = ach.isUnlocked || unlocked
            )
        }

        dao.upsertAchievements(updated)
    }
}
