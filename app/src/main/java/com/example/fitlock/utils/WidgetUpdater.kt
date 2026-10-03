package com.example.fitlock.utils

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.fitlock.widgets.HabitWidget
import com.example.fitlock.widgets.ChallengeWidget
import com.example.fitlock.widgets.DailyShieldWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {
    suspend fun updateAllWidgets(context: Context) = withContext(Dispatchers.IO) {
        try {
            val manager = GlanceAppWidgetManager(context)
            
            val habitWidgetIds = manager.getGlanceIds(HabitWidget::class.java)
            habitWidgetIds.forEach { id ->
                HabitWidget().update(context, id)
            }
            
            val challengeWidgetIds = manager.getGlanceIds(ChallengeWidget::class.java)
            challengeWidgetIds.forEach { id ->
                ChallengeWidget().update(context, id)
            }

            val shieldWidgetIds = manager.getGlanceIds(DailyShieldWidget::class.java)
            shieldWidgetIds.forEach { id ->
                DailyShieldWidget().update(context, id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
