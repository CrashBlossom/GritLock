package com.example.fitlock.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import com.example.fitlock.data.GritLockDatabase
import com.example.fitlock.exercise.ExerciseType
import kotlinx.coroutines.flow.first
import java.util.*

class ChallengeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = GritLockDatabase.getDatabase(context)
        val challenges = db.dao().getChallenges().first()
        val history = db.dao().getHistory().first()
        
        // Find today's totals
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis
        val todayTotals = history.filter { it.timestamp >= startOfDay }
            .groupBy { it.exerciseType }
            .mapValues { entry -> entry.value.sumOf { it.repsCompleted } }

        // Logic: Pick the first challenge (or user can pin one later)
        val activeChallenge = challenges.firstOrNull()

        provideContent {
            ChallengeWidgetContent(activeChallenge, todayTotals)
        }
    }

    @Composable
    private fun ChallengeWidgetContent(challenge: com.example.fitlock.data.Challenge?, totals: Map<String, Int>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF121216)))
                .padding(8.dp)
        ) {
            if (challenge == null) {
                Text("No active challenges", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White)))
            } else {
                Text(
                    text = challenge.title.uppercase(),
                    style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFD0BCFF)), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                challenge.requirements.take(3).forEach { req ->
                    val current = totals[req.type] ?: 0
                    val isDone = current >= req.count
                    
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${req.type}: $current/${req.count}",
                            style = TextStyle(
                                color = ColorProvider(if (isDone) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color.White),
                                fontSize = 10.sp
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (isDone) {
                            Text("✓", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF4CAF50)), fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

class ChallengeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChallengeWidget()
}
