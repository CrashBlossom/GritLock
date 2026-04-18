package com.example.fitlock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun EmergencyBypassScreen(onBypassSuccess: () -> Unit) {
    val annoyingText = remember { generateAnnoyingText() }
    var userInput by remember { mutableStateOf("") }
    val isMatch = userInput.trim() == annoyingText.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("EMERGENCY BYPASS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Type the text below perfectly to unlock for 5 minutes.", style = MaterialTheme.typography.bodySmall)
        
        Card(
            modifier = Modifier.padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = annoyingText,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
        }

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Type here...") },
            isError = userInput.isNotEmpty() && !annoyingText.startsWith(userInput)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBypassSuccess,
            enabled = isMatch,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("UNLOCK")
        }
    }
}

fun generateAnnoyingText(): String {
    return """
        I acknowledge that I am bypassing my physical commitments. 
        I understand that discipline is the bridge between goals and accomplishment. 
        By typing this, I am choosing temporary comfort over long-term growth. 
        I will return to my workout as soon as possible. 
        GritLock is designed to help me, not to hinder me.
        Persistence beats resistance.
    """.trimIndent()
}
