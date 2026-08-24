package com.example.fitlock.service

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.*

class WearDataService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/gauntlet_state") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val habitName = dataMap.getString("habit_name")
                val elapsed = dataMap.getInt("elapsed")
                Log.d("WearDataService", "Habit: $habitName, Elapsed: $elapsed")
                // TODO: Update Wear UI state (perhaps via a SharedFlow or specific broadcast)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == "/urge_logged") {
            Log.d("WearDataService", "Urge logged from phone")
        }
    }
}
