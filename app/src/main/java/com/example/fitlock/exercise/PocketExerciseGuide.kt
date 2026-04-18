package com.example.fitlock.exercise

object PocketExerciseGuide {
    fun getPlacementInstruction(type: ExerciseType): String {
        return when (type) {
            ExerciseType.SQUAT -> "Place the phone in your front trouser pocket, screen facing your thigh."
            ExerciseType.PUSHUP -> "Secure the phone to your upper back (e.g., under a tight shirt or sports bra) or upper arm."
            ExerciseType.SITUP -> "Hold the phone against your chest with both hands."
            ExerciseType.HINGE -> "Place the phone in your front trouser pocket."
            ExerciseType.ROW -> "Hold the phone in the hand you are using to perform the row."
            ExerciseType.DIP -> "Place the phone in your trouser pocket or secure to your upper arm."
            ExerciseType.PULLUP -> "Secure the phone to your waist/belt or upper arm."
            ExerciseType.PLANK -> "Place the phone on your lower back or hold against your chest."
            else -> "Keep the phone close to your body."
        }
    }
}
