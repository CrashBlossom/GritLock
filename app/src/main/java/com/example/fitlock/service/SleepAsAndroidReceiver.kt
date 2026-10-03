package com.example.fitlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fitlock.MainActivity

/**
 * SleepAsAndroidReceiver catches alarm dismissal broadcasts from Sleep as Android
 * (or system alarm clocks) and immediately triggers GrittierLock's Morning Wake-up Pipeline.
 */
class SleepAsAndroidReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("SleepReceiver", "Alarm dismissal event received: $action")

        val pipelineIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("START_MORNING_PIPELINE", true)
        }
        context.startActivity(pipelineIntent)
    }
}
