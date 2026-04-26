package com.example.fitlock.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * HealthConnectManager handles all interactions with Android's Health Connect system.
 */
class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    // List of permissions required for the app's RPG features
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class) // NEW: Required for Vitality (VIT)
    )

    suspend fun hasPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads today's total steps.
     */
    suspend fun readTodaySteps(): Long {
        return try {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = LocalDateTime.now(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
            val now = Instant.now()
            
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response.records.sumOf { it.count }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Reads running distance from the last 24 hours to grant Agility (AGI) XP.
     */
    suspend fun readRecentRunDistance(): Double {
        return try {
            val now = Instant.now()
            val yesterday = now.minus(24, ChronoUnit.HOURS)
            
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(yesterday, now)
                )
            )
            // Filter only for running exercises
            response.records
                .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING }
                .size.toDouble() // Using session count for now as a simple metric
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Reads sleep duration from the last night to grant Vitality (VIT) XP.
     */
    suspend fun readLastNightSleepDurationMinutes(): Long {
        return try {
            val now = Instant.now()
            val yesterday = now.minus(24, ChronoUnit.HOURS)
            
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(yesterday, now)
                )
            )
            
            // Sum up duration of all sleep sessions found in the last 24h
            response.records.sumOf { 
                ChronoUnit.MINUTES.between(it.startTime, it.endTime)
            }
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun writeExerciseSession(type: String, reps: Int, startTime: Instant, endTime: Instant) {
        try {
            val session = ExerciseSessionRecord(
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = ZonedDateTime.now().offset,
                endZoneOffset = ZonedDateTime.now().offset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                title = "$reps $type Reps",
                metadata = HealthMetadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(session))
        } catch (e: Exception) {
            // Log error
        }
    }
}
