package com.example.fitlock.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * HeroicCelebration provides a high-impact full-screen animation 
 * for major events like Level Ups.
 */
@Composable
fun HeroicCelebration(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
        delay(3000)
        isVisible = false
        delay(500)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(500)) + scaleIn(tween(500, easing = OvershootInterpolator(2f).toEasing())),
        exit = fadeOut(tween(500)) + scaleOut(tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title.uppercase(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = subtitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

// Helper to convert Interpolator to Easing
fun OvershootInterpolator(tension: Float = 2f) = android.view.animation.OvershootInterpolator(tension)
fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
