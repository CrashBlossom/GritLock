package com.example.fitlock.ui

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.fitlock.service.GauntletService
import com.example.fitlock.data.GritLockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NfcTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent?.action) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null && rawMsgs.isNotEmpty()) {
                val msg = rawMsgs[0] as android.nfc.NdefMessage
                val payload = String(msg.records[0].payload)
                Log.d("NfcTrigger", "Payload: $payload")
                
                // Assuming payload is "gauntlet_id:123"
                if (payload.startsWith("gauntlet_id:")) {
                    val id = payload.substringAfter("gauntlet_id:").toIntOrNull()
                    if (id != null) {
                        startGauntlet(id)
                    }
                }
            }
        }
        finish()
    }

    private fun startGauntlet(id: Int) {
        val serviceIntent = Intent(this, GauntletService::class.java).apply {
            action = GauntletService.ACTION_START
            putExtra(GauntletService.EXTRA_GAUNTLET_ID, id)
        }
        startForegroundService(serviceIntent)
        Toast.makeText(this, "NFC Routine Started", Toast.LENGTH_SHORT).show()
    }
}
