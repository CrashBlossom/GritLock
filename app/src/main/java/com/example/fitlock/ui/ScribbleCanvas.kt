package com.example.fitlock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.google.gson.Gson

@Composable
fun ScribbleCanvas(
    modifier: Modifier = Modifier,
    initialScribbleData: String? = null,
    onScribbleUpdated: (String) -> Unit,
    transparentBackground: Boolean = true
) {
    var currentPathPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    val allPaths = remember { mutableStateListOf<List<Pair<Float, Float>>>() }
    val gson = remember { Gson() }
    
    // Load initial data if provided
    LaunchedEffect(initialScribbleData) {
        if (!initialScribbleData.isNullOrBlank()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<List<Pair<Float, Float>>>>() {}.type
                val loadedPaths: List<List<Pair<Float, Float>>> = gson.fromJson(initialScribbleData, type)
                allPaths.clear()
                allPaths.addAll(loadedPaths)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            allPaths.clear()
        }
    }

    Box(modifier = modifier.then(if (!transparentBackground) Modifier.background(Color.White) else Modifier)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPathPoints = listOf(offset.x to offset.y)
                        },
                        onDrag = { change, _ ->
                            currentPathPoints = currentPathPoints + (change.position.x to change.position.y)
                        },
                        onDragEnd = {
                            allPaths.add(currentPathPoints)
                            currentPathPoints = emptyList()
                            onScribbleUpdated(gson.toJson(allPaths.toList()))
                        }
                    )
                }
        ) {
            allPaths.forEach { points ->
                if (points.isNotEmpty()) {
                    val path = Path()
                    path.moveTo(points[0].first, points[0].second)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].first, points[i].second)
                    }
                    drawPath(path, Color.Red.copy(alpha = 0.7f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            
            if (currentPathPoints.isNotEmpty()) {
                val path = Path()
                path.moveTo(currentPathPoints[0].first, currentPathPoints[0].second)
                for (i in 1 until currentPathPoints.size) {
                    path.lineTo(currentPathPoints[i].first, currentPathPoints[i].second)
                }
                drawPath(path, Color.Red.copy(alpha = 0.4f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}
