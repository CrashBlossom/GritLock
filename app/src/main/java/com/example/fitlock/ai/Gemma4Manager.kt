package com.example.fitlock.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class Gemma4Manager(private val context: Context) {
    private var llmInference: LlmInference? = null
    
    var isInitialized = false
        private set

    val modelFile: File
        get() = File(context.filesDir, "gemma_4b_instruct_q4.task")

    /**
     * Initializes the local on-device Gemma LLM using MediaPipe Tasks GenAI.
     * Returns true if model is present and initialized successfully.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) {
            isInitialized = false
            return@withContext false
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(200)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
            false
        }
    }

    /**
     * Generates a response synchronously on an IO thread using Gemma.
     * If model file is not downloaded or initialization failed, provides an automatic fallback response.
     */
    suspend fun generateSystemVoice(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            return@withContext generateOfflineFallback(prompt)
        }

        return@withContext try {
            val result = llmInference?.generateResponse(prompt)
            if (!result.isNullOrBlank()) {
                result.trim()
            } else {
                generateOfflineFallback(prompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generateOfflineFallback(prompt)
        }
    }

    private fun generateOfflineFallback(prompt: String): String {
        return when {
            prompt.contains("APP_LOCKED") || prompt.contains("Scenario: Player attempted") ->
                "[SYSTEM WARNING] Access restricted. Complete your assigned physical or mental quest to restore clearance."

            prompt.contains("WORKOUT_COMPLETED") || prompt.contains("Scenario: Player successfully") ->
                "[QUEST CLEARED] Effort recognized. Experience points allocated and access temporarily restored."

            prompt.contains("URGE_NEGOTIATION") ->
                "[SYSTEM ANALYSIS] Dopamine craving detected. Take 10 deep breaths and execute 15 physical reps to recalibrate neurochemistry."

            else ->
                "[SYSTEM ACTIVE] Maintain discipline. Victory requires consistency."
        }
    }

    fun close() {
        try {
            llmInference?.close()
        } catch (_: Exception) {}
        llmInference = null
        isInitialized = false
    }
}
