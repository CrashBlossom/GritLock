package com.example.fitlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fitlock.MainActivity
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.data.LockStatusManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ExternalCommandReceiver allows apps like Tasker to control GritLock.
 * Action: com.example.fitlock.ACTION_COMMAND
 * Extras: 
 *   - command: "ENABLE_GROUP", "DISABLE_GROUP", "UNLOCK_GROUP"
 *   - group_id: Int
 */
@AndroidEntryPoint
class ExternalCommandReceiver : BroadcastReceiver() {

    @Inject lateinit var db: GritLockDatabase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.fitlock.ACTION_COMMAND") return

        val command = intent.getStringExtra("command") ?: return
        val groupId = intent.getIntExtra("group_id", -1)

        Log.d("ExternalCommand", "Received command: $command for group: $groupId")

        scope.launch {
            when (command) {
                "ENABLE_GROUP" -> {
                    if (groupId == -1) return@launch
                    val group = db.dao().getGroupById(groupId) ?: return@launch
                    db.dao().updateGroup(group.copy(isEnabled = true))
                }
                "DISABLE_GROUP" -> {
                    if (groupId == -1) return@launch
                    val group = db.dao().getGroupById(groupId) ?: return@launch
                    db.dao().updateGroup(group.copy(isEnabled = false))
                }
                "UNLOCK_GROUP" -> {
                    if (groupId == -1) return@launch
                    val group = db.dao().getGroupById(groupId) ?: return@launch
                    val now = System.currentTimeMillis()
                    db.dao().updateGroup(group.copy(lastUnlockedTimestamp = now))
                    LockStatusManager.updateUnlock(groupId, now)
                }
                "START_MORNING_PLAN" -> {
                    val startIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("shortcut_action", "morning_plan")
                    }
                    context.startActivity(startIntent)
                }
                "START_EVENING_REVIEW" -> {
                    val startIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("shortcut_action", "evening_review")
                    }
                    context.startActivity(startIntent)
                }
            }
        }
    }
}
