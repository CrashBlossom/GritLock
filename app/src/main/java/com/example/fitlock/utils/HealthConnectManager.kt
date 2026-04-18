package com.example.fitlock.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.metadata.Metadata as HealthMetadata
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.LocalTime
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasPermissions(): Boolean {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        )
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readTodaySteps(): Long {
        val zoneId = ZoneId.systemDefault()
        val startTime = LocalDateTime.now(zoneId).with(LocalTime.MIN)
        val endTime = LocalDateTime.now(zoneId)
        
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun writeSteps(count: Long, startTime: Instant, endTime: Instant) {
        val stepsRecord = StepsRecord(
            count = count,
            startTime = startTime,
            endTime = endTime,
            startZoneOffset = ZonedDateTime.now().offset,
            endZoneOffset = ZonedDateTime.now().offset,
            metadata = HealthMetadata.manualEntry()
        )
        healthConnectClient.insertRecords(listOf(stepsRecord))
    }

    suspend fun writeExerciseSession(type: String, reps: Int, startTime: Instant, endTime: Instant) {
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
    }
}
