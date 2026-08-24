package com.example.fitlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.fitlock.theme.FitLockTheme

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    var habitName by remember { mutableStateOf("None") }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "GritLock",
                style = MaterialTheme.typography.title1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Button(
                onClick = { /* TODO: Log Urge */ },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text("LOG URGE")
            }
        }

        item {
            Text(
                text = "Routine: $habitName",
                style = MaterialTheme.typography.caption2,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        if (habitName != "None") {
            item {
                Text(
                    text = formatTime(elapsedSeconds),
                    style = MaterialTheme.typography.display1
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun FitLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
