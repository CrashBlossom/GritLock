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
import kotlinx.coroutines.flow.first

class LockTimerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = GritLockDatabase.getDatabase(context)
        val stats = db.dao().getUserStats().first()
        val totalBanked = stats?.bankedReps?.values?.sum() ?: 0

        provideContent {
            LockTimerWidgetContent(totalBanked)
        }
    }

    @Composable
    private fun LockTimerWidgetContent(reps: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF121216)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "REP BANK",
                    style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "$reps reps",
                    style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFD0BCFF)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Simulating a progress bar
            val progress = (reps.toFloat() / 100f).coerceIn(0f, 1f)
            
            Row(modifier = GlanceModifier.fillMaxWidth().height(8.dp).background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1C1C21)))) {
                if (progress > 0) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFFD0BCFF)))
                    ) {}
                }
            }
        }
    }
}

class LockTimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LockTimerWidget()
}
