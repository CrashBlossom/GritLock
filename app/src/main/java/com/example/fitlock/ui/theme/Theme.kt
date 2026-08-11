package com.example.fitlock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121216),
    surface = Color(0xFF1C1C21)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF0F0F3),
    surface = Color(0xFFFFFFFF)
)

// Emerald
private val DarkEmerald = darkColorScheme(
    primary = Color(0xFF50C878),
    background = Color(0xFF0B120E),
    surface = Color(0xFF152219)
)
private val LightEmerald = lightColorScheme(
    primary = Color(0xFF2E8B57),
    background = Color(0xFFE8F5E9),
    surface = Color(0xFFFFFFFF)
)

// Crimson
private val DarkCrimson = darkColorScheme(
    primary = Color(0xFFDC143C),
    background = Color(0xFF120B0B),
    surface = Color(0xFF221515)
)
private val LightCrimson = lightColorScheme(
    primary = Color(0xFFB71C1C),
    background = Color(0xFFFBE9E7),
    surface = Color(0xFFFFFFFF)
)

// Ocean
private val DarkOcean = darkColorScheme(
    primary = Color(0xFF0077BE),
    background = Color(0xFF0B0E12),
    surface = Color(0xFF151922)
)
private val LightOcean = lightColorScheme(
    primary = Color(0xFF0277BD),
    background = Color(0xFFE1F5FE),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun GritLockTheme(
    themeName: String = "Default",
    darkModeSetting: String = "System", // "System", "Light", "Dark"
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDarkMode = when (darkModeSetting) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeName) {
        "Emerald" -> if (isDarkMode) DarkEmerald else LightEmerald
        "Crimson" -> if (isDarkMode) DarkCrimson else LightCrimson
        "Ocean" -> if (isDarkMode) DarkOcean else LightOcean
        "Material" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDarkMode) DarkColorScheme else LightColorScheme
            }
        }
        else -> if (isDarkMode) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
