package com.example.fitlock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AchievementBadgeCanvas(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isUnlocked: Boolean = true,
    accentColor: Color = Color(0xFFFFD700)
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = size.toPx() / 2f * 0.85f

            // Outer Glow / Shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 1.3f
                ),
                radius = radius * 1.3f,
                center = center
            )

            // Outer Metallic Ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (isUnlocked) listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFFD700)) else listOf(Color.Gray, Color.DarkGray),
                    start = Offset(0f, 0f),
                    end = Offset(size.toPx(), size.toPx())
                ),
                radius = radius,
                center = center,
                style = Stroke(width = 6f)
            )

            // Inner Dark Gem Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isUnlocked) listOf(Color(0xFF1B2A1E), Color(0xFF0D1410)) else listOf(Color(0xFF2C2C35), Color(0xFF15151A)),
                    center = center,
                    radius = radius * 0.9f
                ),
                radius = radius * 0.9f,
                center = center
            )

            // Inner Gold Hex / Star Border
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (isUnlocked) listOf(Color(0xFFFFEE58), Color(0xFFF57F17)) else listOf(Color.LightGray, Color.Gray),
                    start = Offset(size.toPx(), 0f),
                    end = Offset(0f, size.toPx())
                ),
                radius = radius * 0.75f,
                center = center,
                style = Stroke(width = 2f)
            )
        }
    }
}
