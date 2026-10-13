package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.UserStats

data class ChatMessage(val sender: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemmaChatScreen(
    userStats: UserStats?,
    onBack: () -> Unit
) {
    var persona by remember { mutableStateOf("Shadow Monarch") }
    var inputMessage by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateOf(
            listOf(
                ChatMessage("System Oracle", "Greetings, Seeker. I am your system advisor. Ask me for discipline strategy, workout advice, or mindset counsel.")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Oracle (AI Advisor)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            // Persona Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Persona:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                listOf("Shadow Monarch", "Stoic Mentor", "Drill Sergeant").forEach { p ->
                    FilterChip(
                        selected = persona == p,
                        onClick = { persona = p },
                        label = { Text(p, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages.value) { msg ->
                    val isUser = msg.sender == "You"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(msg.sender, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(msg.text, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Ask the Oracle for counsel...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            val userMsg = inputMessage
                            val updatedList = messages.value.toMutableList().apply {
                                add(ChatMessage("You", userMsg))
                            }
                            inputMessage = ""

                            val reply = when (persona) {
                                "Shadow Monarch" -> "Rise. Weakness is a choice. Complete your daily gate defense before resting."
                                "Stoic Mentor" -> "Control what is in your power. External events do not matter; your discipline is your citadel."
                                "Drill Sergeant" -> "No excuses! Drop and give me another 20 reps right now!"
                                else -> "Stay focused on your objective, Seeker."
                            }
                            updatedList.add(ChatMessage("System Oracle ($persona)", reply))
                            messages.value = updatedList
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.Black)
                }
            }
        }
    }
}
