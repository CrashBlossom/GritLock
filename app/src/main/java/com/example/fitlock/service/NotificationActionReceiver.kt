package com.example.fitlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.fitlock.data.DailyLogNote
import com.example.fitlock.data.GritLockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles text input from the notification bar.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: GritLockRepository
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_POST_NOTE) {
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            if (remoteInput != null) {
                val content = remoteInput.getCharSequence(KEY_TEXT_REPLY)?.toString()
                if (!content.isNullOrBlank()) {
                    scope.launch {
                        repository.insertLogNote(DailyLogNote(content = content))
                        
                        // Update the notification to show success/dismiss
                        val serviceIntent = Intent(context, DailyLogService::class.java)
                        context.startForegroundService(serviceIntent)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_POST_NOTE = "com.example.fitlock.ACTION_POST_NOTE"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
}
