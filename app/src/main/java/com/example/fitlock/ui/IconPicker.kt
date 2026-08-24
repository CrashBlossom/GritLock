package com.example.fitlock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

val iconMap = mapOf(
    "Dumbbell" to Icons.Default.FitnessCenter,
    "Book" to Icons.Default.Book,
    "Tooth" to Icons.Default.CleaningServices,
    "Meditation" to Icons.Default.SelfImprovement,
    "Work" to Icons.Default.Work,
    "Social" to Icons.Default.Public,
    "Game" to Icons.Default.SportsEsports,
    "Food" to Icons.Default.Restaurant,
    "Sleep" to Icons.Default.Bedtime,
    "Drink" to Icons.Default.LocalDrink,
    "Running" to Icons.Default.DirectionsRun,
    "Yoga" to Icons.Default.SelfImprovement,
    "Code" to Icons.Default.Code,
    "Music" to Icons.Default.MusicNote,
    "Camera" to Icons.Default.CameraAlt,
    "Lock" to Icons.Default.Lock,
    "Star" to Icons.Default.Star,
    "Fire" to Icons.Default.Whatshot
)

@Composable
fun IconPicker(
    selectedIcon: String?,
    onIconSelected: (String) -> Unit
) {
    Column {
        Text("Select Icon", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier.height(200.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(iconMap.keys.toList()) { name ->
                val icon = iconMap[name]!!
                IconItem(
                    icon = icon,
                    isSelected = selectedIcon == name,
                    onClick = { onIconSelected(name) }
                )
            }
        }
    }
}

@Composable
private fun IconItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .size(48.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
