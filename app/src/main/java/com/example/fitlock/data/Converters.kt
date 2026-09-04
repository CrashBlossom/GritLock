package com.example.fitlock.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toStringList(data: String): List<String> = data.split(",").filter { it.isNotBlank() }

    @TypeConverter
    fun fromExerciseList(list: List<ExerciseRequirement>): String {
        return list.joinToString(";") { 
            "${it.type}:${it.count}:${it.isExtra}:${it.targetPackageNames.joinToString(",")}:${it.deckName ?: ""}:${it.variantName ?: ""}:${it.familyId ?: ""}" 
        }
    }

    @TypeConverter
    fun toExerciseList(data: String): List<ExerciseRequirement> {
        if (data.isBlank()) return emptyList()
        return data.split(";").map {
            val parts = it.split(":")
            ExerciseRequirement(
                type = parts[0],
                count = parts[1].toInt(),
                isExtra = parts.getOrNull(2)?.toBoolean() ?: false,
                targetPackageNames = parts.getOrNull(3)?.split(",")?.filter { pkg -> pkg.isNotBlank() } ?: emptyList(),
                deckName = parts.getOrNull(4)?.let { if (it.isEmpty()) null else it },
                variantName = parts.getOrNull(5)?.let { if (it.isEmpty()) null else it },
                familyId = parts.getOrNull(6)?.let { if (it.isEmpty()) null else it }
            )
        }
    }

    @TypeConverter
    fun fromScheduleList(list: List<ScheduleInterval>): String {
        return list.joinToString(";") { "${it.dayOfWeek}:${it.startMinute}:${it.endMinute}" }
    }

    @TypeConverter
    fun toScheduleList(data: String): List<ScheduleInterval> {
        if (data.isBlank()) return emptyList()
        return data.split(";").map {
            val parts = it.split(":")
            ScheduleInterval(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }

    @TypeConverter
    fun fromRepMap(map: Map<String, Int>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun toRepMap(data: String): Map<String, Int> {
        if (data.isBlank()) return emptyMap()
        return data.split(";").associate {
            val parts = it.split(":")
            parts[0] to parts[1].toInt()
        }
    }

    @TypeConverter
    fun fromCalibrationMap(map: Map<String, ExerciseCalibration>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value.topValue}:${it.value.bottomValue}:${it.value.thresholdValue}:${it.value.variantName ?: ""}" }
    }

    @TypeConverter
    fun toCalibrationMap(data: String): Map<String, ExerciseCalibration> {
        if (data.isBlank()) return emptyMap()
        return data.split(";").associate {
            val parts = it.split(":")
            val variant = parts.getOrNull(4)?.let { if (it.isEmpty()) null else it }
            parts[0] to ExerciseCalibration(parts[0], parts[1].toDouble(), parts[2].toDouble(), parts.getOrNull(3)?.toDouble() ?: 0.0, variant)
        }
    }

    @TypeConverter
    fun fromStatType(statType: StatType): String = statType.name

    @TypeConverter
    fun toStatType(value: String): StatType = StatType.valueOf(value)

    @TypeConverter
    fun fromGauntletTriggerType(type: GauntletTriggerType): String = type.name

    @TypeConverter
    fun toGauntletTriggerType(value: String): GauntletTriggerType = GauntletTriggerType.valueOf(value)

    @TypeConverter
    fun fromPhysicalTriggerType(type: PhysicalTriggerType): String = type.name

    @TypeConverter
    fun toPhysicalTriggerType(value: String): PhysicalTriggerType = PhysicalTriggerType.valueOf(value)

    @TypeConverter
    fun fromAvatarType(type: AvatarType): String = type.name

    @TypeConverter
    fun toAvatarType(value: String): AvatarType = AvatarType.valueOf(value)

    @TypeConverter
    fun fromHabitMediaType(type: HabitMediaType): String = type.name

    @TypeConverter
    fun toHabitMediaType(value: String): HabitMediaType = HabitMediaType.valueOf(value)

    @TypeConverter
    fun fromHabitTrackingType(type: HabitTrackingType): String = type.name

    @TypeConverter
    fun toHabitTrackingType(value: String): HabitTrackingType = HabitTrackingType.valueOf(value)

    @TypeConverter
    fun fromHabitLogList(list: List<HabitLog>): String {
        return list.joinToString(";") { 
            "${it.habitId}|${it.name}|${it.actualDurationSeconds}|${it.estimatedDurationSeconds ?: -1}|${it.repsCompleted ?: -1}" 
        }
    }

    @TypeConverter
    fun toHabitLogList(data: String): List<HabitLog> {
        if (data.isBlank()) return emptyList()
        return data.split(";").map {
            val parts = it.split("|")
            HabitLog(
                habitId = parts[0].toInt(),
                name = parts[1],
                actualDurationSeconds = parts[2].toInt(),
                estimatedDurationSeconds = parts[3].toInt().let { if (it == -1) null else it },
                repsCompleted = parts.getOrNull(4)?.toInt()?.let { if (it == -1) null else it }
            )
        }
    }

    @TypeConverter
    fun fromQuestType(type: QuestType): String = type.name

    @TypeConverter
    fun toQuestType(value: String): QuestType = QuestType.valueOf(value)

    @TypeConverter
    fun fromBlockType(type: BlockType): String = type.name

    @TypeConverter
    fun toBlockType(value: String): BlockType = BlockType.valueOf(value)
}
