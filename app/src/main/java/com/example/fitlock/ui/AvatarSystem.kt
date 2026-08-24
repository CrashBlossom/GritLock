package com.example.fitlock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.fitlock.data.UserStats
import com.example.fitlock.data.AvatarType

@Composable
fun AvatarSystem(
    userStats: UserStats?,
    modifier: Modifier = Modifier
) {
    val level = userStats?.level ?: 1
    val type = userStats?.avatarType ?: AvatarType.SEEKER
    
    // Determine primary color based on type
    val avatarColor = when (type) {
        AvatarType.WARRIOR -> Color(0xFFDC143C) // Crimson
        AvatarType.MAGE -> Color(0xFF0077BE)    // Ocean
        AvatarType.ROGUE -> Color(0xFF50C878)   // Emerald
        else -> MaterialTheme.colorScheme.primary
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aura Effect for higher levels
        if (level >= 20) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(avatarColor.copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                        radius = size.width / 2
                    ),
                    radius = size.width / 2
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            color = avatarColor.copy(alpha = 0.1f),
            border = if (level >= 10) androidx.compose.foundation.BorderStroke(2.dp, avatarColor) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(48.dp),
                    tint = avatarColor
                )
                
                // Add "Gear" indicators based on level
                if (level >= 15) {
                    // Small "Armor" indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(12.dp)
                            .background(avatarColor, CircleShape)
                    )
                }
            }
        }
        
        // Level Badge
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp
        ) {
            Text(
                text = "Lvl $level",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}
