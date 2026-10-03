package com.example.fitlock.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SystemVoiceSynthesizer(context: Context) {
    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(0.85f)      // Deeper, authoritative tone
                tts?.setSpeechRate(1.05f)   // Brisk, crisp delivery
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady && tts != null) {
            // Strip markdown brackets for clean natural voice reading
            val cleanText = text
                .replace(Regex("\\[.*?\\]"), "")
                .replace("*", "")
                .trim()
            if (cleanText.isNotBlank()) {
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "system_voice_id")
            }
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
