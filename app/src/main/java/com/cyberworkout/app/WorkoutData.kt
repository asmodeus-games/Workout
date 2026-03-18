package com.cyberworkout.app

import kotlinx.serialization.Serializable

@Serializable
enum class WorkoutMode {
    SEQUENTIAL,
    CIRCUIT
}

@Serializable
data class Exercise(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val sets: Int,
    val restTimeSeconds: Int,
    val target: String
)

@Serializable
data class Routine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<Exercise>,
    val mode: WorkoutMode = WorkoutMode.SEQUENTIAL,
    val circuitBreakSeconds: Int = 30
)

val defaultRoutine = Routine(
    name = "Daily Routine",
    exercises = listOf(
        Exercise(name = "Archer Pushups", sets = 3, restTimeSeconds = 120, target = "8 per side"),
        Exercise(name = "Chin-ups", sets = 3, restTimeSeconds = 120, target = "10 reps"),
        Exercise(name = "L-Sit", sets = 3, restTimeSeconds = 90, target = "20s hold"),
        Exercise(name = "Dead Hang", sets = 2, restTimeSeconds = 60, target = "60s hold")
    )
)
