package com.example.fitlock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.DailyPledge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PledgeScreen(
    currentPledge: DailyPledge?,
    onCommit: () -> Unit,
    onSuccess: () -> Unit,
    onRelapse: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Discipline") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentPledge?.status) {
                "PENDING" -> {
                    Text("Today's Pledge", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "I commit to avoiding my distracting habits today and focusing on my growth.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onCommit,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("I COMMIT")
                    }
                }
                "COMMITTED" -> {
                    Text("How is your day going?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onSuccess,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I STAYED SOBER")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onRelapse,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("I SLIPPED UP")
                    }
                }
                "SUCCESS" -> {
                    Text("Victory!", fontSize = 32.sp, fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You conquered the day. Willpower +100 XP")
                }
                "RELAPSED" -> {
                    Text("Keep Moving Forward", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("A slip is just a lesson. Your streak reset, but your growth continues.")
                }
            }
        }
    }
}
