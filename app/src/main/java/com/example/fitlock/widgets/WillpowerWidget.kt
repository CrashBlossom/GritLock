package com.example.fitlock.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.fitlock.MainActivity
import com.example.fitlock.data.GritLockDatabase
import kotlinx.coroutines.flow.first

class WillpowerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = GritLockDatabase.getDatabase(context)
        val stats = db.dao().getUserStats().first()
        val streak = stats?.sobrietyStreak ?: 0

        provideContent {
            WillpowerWidgetContent(streak)
        }
    }

    @Composable
    private fun WillpowerWidgetContent(streak: Int) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF121216)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STREAK",
                    style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Gray), fontSize = 10.sp)
                )
                Text(
                    text = "$streak Days",
                    style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = GlanceModifier.width(16.dp))

            // Use Text with action instead of Button if ButtonDefaults is tricky
            Text(
                text = "I FEEL AN URGE",
                modifier = GlanceModifier
                    .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFFF44336)))
                    .padding(8.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class WillpowerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WillpowerWidget()
}
