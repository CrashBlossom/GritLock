package com.example.fitlock.utils

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.units.Volume
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZoneId

data class VitalityMetrics(
    val sleepHours: Double = 0.0,
    val waterLiters: Double = 0.0,
    val foodCalories: Double = 0.0,
    val steps: Long = 0L,
    val workoutRepsToday: Int = 0
)

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    /**
     * List of all permissions required by the app for Health Connect.
     */
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class)
    )

    suspend fun hasPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error checking permissions", e)
            false
        }
    }

    suspend fun readTodaySleepHours(): Double {
        return try {
            val zoneId = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zoneId)
            val startTime = now.minusDays(1).toLocalDate().atStartOfDay(zoneId).toInstant()
            val endTime = now.toInstant()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val totalSeconds = response.records.sumOf { 
                Duration.between(it.startTime, it.endTime).seconds
            }
            totalSeconds / 3600.0
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading sleep", e)
            0.0
        }
    }

    suspend fun readTodayWaterLiters(): Double {
        return try {
            val zoneId = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zoneId)
            val startTime = now.toLocalDate().atStartOfDay(zoneId).toInstant()
            val endTime = now.toInstant()

            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val volume = response[HydrationRecord.VOLUME_TOTAL]
            volume?.inLiters ?: 0.0
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading hydration", e)
            0.0
        }
    }

    suspend fun readTodayNutritionCalories(): Double {
        return try {
            val zoneId = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zoneId)
            val startTime = now.toLocalDate().atStartOfDay(zoneId).toInstant()
            val endTime = now.toInstant()

            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(NutritionRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val energy = response[NutritionRecord.ENERGY_TOTAL]
            energy?.inKilocalories ?: 0.0
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading nutrition", e)
            0.0
        }
    }

    suspend fun writeWaterIntake(liters: Double) {
        try {
            val now = Instant.now()
            val record = HydrationRecord(
                startTime = now.minusSeconds(60),
                endTime = now,
                startZoneOffset = ZonedDateTime.now().offset,
                endZoneOffset = ZonedDateTime.now().offset,
                volume = Volume.liters(liters),
                metadata = HealthMetadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error writing hydration", e)
        }
    }

    /**
     * Reads the total step count for the current day (since midnight).
     */
    suspend fun readTodaySteps(): Long {
        return try {
            val zoneId = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zoneId)
            val startOfDay = now.toLocalDate().atStartOfDay(zoneId).toInstant()
            val endTime = now.toInstant()
            
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endTime)
                )
            )
            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            Log.d("HealthConnect", "Read today's steps: $steps (from $startOfDay to $endTime)")
            steps
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading steps", e)
            0L
        }
    }

    suspend fun writeSteps(count: Long, startTime: Instant, endTime: Instant) {
        try {
            val stepsRecord = StepsRecord(
                count = count,
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endZoneOffset = ZonedDateTime.now().offset,
                metadata = HealthMetadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
            Log.d("HealthConnect", "Wrote $count steps to Health Connect")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error writing steps", e)
        }
    }

    /**
     * Writes an exercise session and associated calorie burn to Health Connect.
     * Including calories ensures the session is visible in apps like Google Fit.
     */
    suspend fun writeExerciseSession(type: String, reps: Int, startTime: Instant, endTime: Instant) {
        try {
            val exerciseType = when (type.uppercase()) {
                "SQUAT", "PUSHUP", "SITUP", "PULLUP", "DIP", "HINGE", "ROW", "PLANK" -> 
                    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
                else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            }

            val session = ExerciseSessionRecord(
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endZoneOffset = ZonedDateTime.now().offset,
                exerciseType = exerciseType,
                title = "$reps $type Reps (FitLock)",
                metadata = HealthMetadata.manualEntry()
            )

            // Estimate: ~0.5 kcal per rep for high-intensity bodyweight exercises
            val calories = TotalCaloriesBurnedRecord(
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endZoneOffset = ZonedDateTime.now().offset,
                energy = Energy.kilocalories(reps * 0.5),
                metadata = HealthMetadata.manualEntry()
            )
            
            healthConnectClient.insertRecords(listOf<Record>(session, calories))
            Log.d("HealthConnect", "Successfully pushed $type session ($reps reps, ${reps * 0.5} kcal)")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error writing exercise session", e)
        }
    }
}
