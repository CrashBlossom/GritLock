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

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = Room.databaseBuilder(context, GritLockDatabase::class.java, "gritlock-db").build()
        val history = db.dao().getHistory().first()
        val prefs = context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE)

        val last7Days = (0..6).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTime = cal.timeInMillis
            val endTime = startTime + 24 * 60 * 60 * 1000L
            
            val dayHistory = history.filter { it.timestamp in startTime until endTime }
            val totals = dayHistory.groupBy { it.exerciseType }
                .mapValues { entry -> entry.value.sumOf { it.repsCompleted } }
            
            val dayName = when(cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "M"
                Calendar.TUESDAY -> "T"
                Calendar.WEDNESDAY -> "W"
                Calendar.THURSDAY -> "T"
                Calendar.FRIDAY -> "F"
                Calendar.SATURDAY -> "S"
                Calendar.SUNDAY -> "S"
                else -> ""
            }
            DayStats(dayName, totals)
        }.reversed()

        provideContent {
            HabitWidgetContent(last7Days, prefs)
        }
    }

    @Composable
    private fun HabitWidgetContent(stats: List<DayStats>, prefs: android.content.SharedPreferences) {
        val exerciseMapping = mapOf(
            ExerciseType.PUSHUP.name to "PSH",
            ExerciseType.SQUAT.name to "SQT",
            ExerciseType.SITUP.name to "SIT",
            ExerciseType.PULLUP.name to "PLL",
            ExerciseType.DIP.name to "DIP",
            ExerciseType.ROW.name to "ROW",
            ExerciseType.HINGE.name to "HNG",
            ExerciseType.PLANK.name to "PLK",
            ExerciseType.STEPS.name to "STP"
        )
        // Show the top 4 exercises commonly used
        val selectedTypes = listOf(
            ExerciseType.PUSHUP.name,
            ExerciseType.SQUAT.name,
            ExerciseType.SITUP.name,
            ExerciseType.PLANK.name
        )

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF121216)))
                .padding(8.dp)
        ) {
            Text(
                text = "WEEKLY HABITS",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Row(modifier = GlanceModifier.fillMaxSize()) {
                // Exercise Labels
                Column(modifier = GlanceModifier.padding(end = 4.dp)) {
                    Spacer(modifier = GlanceModifier.height(16.dp)) // Offset for day names
                    selectedTypes.forEach { typeName ->
                        val label = exerciseMapping[typeName] ?: typeName.take(3)
                        Box(modifier = GlanceModifier.height(20.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Gray), fontSize = 8.sp))
                        }
                    }
                }
                
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    stats.forEach { day ->
                        DayColumn(day, selectedTypes, prefs)
                    }
                }
            }
        }
    }

    @Composable
    private fun DayColumn(day: DayStats, types: List<String>, prefs: android.content.SharedPreferences) {
        Column(
            modifier = GlanceModifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(day.name, style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Gray), fontSize = 10.sp))
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            types.forEach { typeName ->
                val count = day.totals[typeName] ?: 0
                val goal = prefs.getInt("goal_$typeName", 10) 
                
                val color = when {
                    count >= goal && goal > 0 -> androidx.compose.ui.graphics.Color(0xFFFFD700)
                    count > 0 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    else -> androidx.compose.ui.graphics.Color(0xFFF44336)
                }
                
                Box(
                    modifier = GlanceModifier
                        .size(18.dp)
                        .background(ColorProvider(color))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if(count > 99) "++" else count.toString(),
                        style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Black), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }

    data class DayStats(val name: String, val totals: Map<String, Int>)
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
