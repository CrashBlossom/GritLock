package com.example.fitlock.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.fitlock.MainActivity
import com.example.fitlock.data.GritLockDatabase
import kotlinx.coroutines.flow.first

class DailyShieldWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = GritLockDatabase.getDatabase(context)
        val stats = db.dao().getUserStats().first()
        val streak = stats?.currentStreak ?: 0
        val hp = stats?.currentHp ?: 100
        val maxHp = stats?.maxHp ?: 100
        val isShieldActive = stats?.floorTargetMetToday == true

        provideContent {
            val appIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            DailyShieldWidgetContent(streak, hp, maxHp, isShieldActive, appIntent)
        }
    }

    @Composable
    private fun DailyShieldWidgetContent(
        streak: Int,
        hp: Int,
        maxHp: Int,
        isShieldActive: Boolean,
        appIntent: Intent
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF121216)))
                .padding(12.dp)
                .clickable(actionStartActivity(appIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "🔥 STREAK: $streak DAYS",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "HP: $hp / $maxHp",
                    style = TextStyle(
                        color = ColorProvider(if (hp <= 25) androidx.compose.ui.graphics.Color(0xFFF44336) else androidx.compose.ui.graphics.Color(0xFF4CAF50)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            val badgeColor = if (isShieldActive) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFE65100)
            val badgeText = if (isShieldActive) "🛡️ SHIELD ACTIVE" else "⚡ TARGET PENDING"

            Text(
                text = badgeText,
                modifier = GlanceModifier
                    .background(ColorProvider(badgeColor))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class DailyShieldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyShieldWidget()
}
