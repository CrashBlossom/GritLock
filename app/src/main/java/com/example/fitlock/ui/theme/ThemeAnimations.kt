package com.example.fitlock.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.sin

private data class Snowflake(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,
    val driftFactor: Float,
    val alpha: Float
)

@Composable
fun FallingSnowEffect() {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val random = remember { Random() }
    val snowflakes = remember(screenWidthPx, screenHeightPx) {
        List(40) {
            Snowflake(
                x = random.nextFloat() * screenWidthPx,
                y = random.nextFloat() * screenHeightPx,
                radius = 2f + random.nextFloat() * 4f,
                speed = 0.4f + random.nextFloat() * 0.8f,
                driftFactor = 0.005f + random.nextFloat() * 0.015f,
                alpha = 0.3f + random.nextFloat() * 0.4f
            )
        }
    }

    var frameTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                frameTimeNanos = time
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (frameTimeNanos == 0L) return@Canvas
        snowflakes.forEach { flake ->
            flake.y += flake.speed
            flake.x += sin((flake.y * flake.driftFactor).toDouble()).toFloat() * 0.5f

            if (flake.y > size.height) {
                flake.y = -flake.radius
                flake.x = random.nextFloat() * size.width
            }
            if (flake.x > size.width) flake.x = 0f
            if (flake.x < 0f) flake.x = size.width

            drawCircle(
                color = Color(0xFFE0F7FA).copy(alpha = flake.alpha),
                radius = flake.radius,
                center = androidx.compose.ui.geometry.Offset(flake.x, flake.y)
            )
        }
    }
}

private data class Firefly(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    val radius: Float,
    val pulseSpeed: Float,
    val baseAlpha: Float
)

@Composable
fun GlowingFirefliesEffect() {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val random = remember { Random() }
    val fireflies = remember(screenWidthPx, screenHeightPx) {
        List(25) {
            Firefly(
                x = random.nextFloat() * screenWidthPx,
                y = random.nextFloat() * screenHeightPx,
                dx = (random.nextFloat() - 0.5f) * 0.3f,
                dy = (random.nextFloat() - 0.5f) * 0.3f,
                radius = 3f + random.nextFloat() * 3.5f,
                pulseSpeed = 0.003f + random.nextFloat() * 0.007f,
                baseAlpha = 0.4f + random.nextFloat() * 0.4f
            )
        }
    }

    var frameCount by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                frameCount = time / 1_000_000L
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (frameCount == 0L) return@Canvas
        fireflies.forEach { fly ->
            fly.x += fly.dx
            fly.y += fly.dy

            if (fly.x <= 0 || fly.x >= size.width) fly.dx *= -1f
            if (fly.y <= 0 || fly.y >= size.height) fly.dy *= -1f

            val pulse = (sin((frameCount * fly.pulseSpeed).toDouble()).toFloat() + 1f) / 2f
            val currentAlpha = (fly.baseAlpha * (0.25f + 0.75f * pulse)).coerceIn(0.1f, 0.9f)

            // Outer soft glow
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = currentAlpha * 0.35f),
                radius = fly.radius * 3f,
                center = androidx.compose.ui.geometry.Offset(fly.x, fly.y)
            )
            // Inner glowing core
            drawCircle(
                color = Color(0xFFFFECB3).copy(alpha = currentAlpha),
                radius = fly.radius,
                center = androidx.compose.ui.geometry.Offset(fly.x, fly.y)
            )
        }
    }
}
