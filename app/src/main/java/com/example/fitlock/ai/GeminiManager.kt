package com.example.fitlock.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.example.fitlock.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiManager(apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    private val gson = Gson()

    suspend fun extractFromText(text: String): ExtractionResult = withContext(Dispatchers.IO) {
        val prompt = """
            You are a fitness and productivity expert for the GritLock app. 
            Analyze the provided text and extract entities into a JSON format.
            
            Entity Schema:
            - HabitDefinition: { "name": String, "trackingType": "TIME"|"REPS"|"CHECKLIST", "defaultEstimatedDurationSeconds": Int? }
            - ExerciseDefinition: { "name": String, "type": "PUSHUP"|"SQUAT"|"PULLUP"|"DIP"|"SITUP"|"HINGE"|"ROW"|"PLANK", "defaultSets": Int, "defaultTargetReps": String }
            - TaskDefinition: { "name": String, "category": String }
            - Goal: { "title": String, "category": String, "xpReward": Int }
            - Project: { "title": String, "goalId": String (placeholder), "xpReward": Int }
            - Quest: { "name": String, "type": "ROUTINE"|"CHALLENGE", "blocks": [List of Blocks] }
            - Block: { "type": "HABIT"|"EXERCISE"|"TASK"|"MENTAL", "name": String, "sets": Int?, "targetReps": String? }

            Return ONLY a valid JSON object:
            {
                "habits": [],
                "exercises": [],
                "tasks": [],
                "goals": [],
                "projects": [],
                "quests": [{"quest": {...}, "blocks": []}]
            }
            
            Text: ${text}
        """.trimIndent()

        return@withContext try {
            val response = model.generateContent(prompt)
            val json = response.text?.substringAfter("```json")?.substringBeforeLast("```")?.trim()
            if (json != null) {
                gson.fromJson(json, ExtractionResult::class.java)
            } else {
                ExtractionResult()
            }
        } catch (e: Exception) {
            ExtractionResult()
        }
    }
}

data class ExtractionResult(
    val habits: List<HabitDefinition> = emptyList(),
    val exercises: List<ExerciseDefinition> = emptyList(),
    val tasks: List<TaskDefinition> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val projects: List<Project> = emptyList(),
    val quests: List<QuestWithBlocks> = emptyList()
)
