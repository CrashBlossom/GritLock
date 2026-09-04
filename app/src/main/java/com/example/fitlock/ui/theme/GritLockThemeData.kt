package com.example.fitlock.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Total Conversion Theme Data.
 */
data class GritLockThemeData(
    val id: String,
    val name: String,
    val tabHome: String = "Home",
    val tabAtlas: String = "Atlas",
    val tabLocks: String = "Locks",
    val tabForge: String = "Forge",
    val tabUrge: String = "Grit",
    val tabAnalytics: String = "Analytics",
    val tabProfile: String = "Profile",
    val logName: String = "Daily Log",
    val primaryColor: Color,
    val surfaceColor: Color = Color.Unspecified
)

val SuperheroTheme = GritLockThemeData(
    id = "SUPERHERO",
    name = "Vigilante",
    tabHome = "Base",
    tabAtlas = "Roadmap",
    tabLocks = "Gear",
    tabForge = "Forge",
    tabUrge = "DEFEND",
    tabAnalytics = "Map",
    tabProfile = "Hero",
    logName = "Intel Log",
    primaryColor = Color(0xFF00FF00) // Neon Green
)

val AgentTheme = GritLockThemeData(
    id = "AGENT",
    name = "Secret Agent",
    tabHome = "HQ",
    tabAtlas = "Mission",
    tabLocks = "Tools",
    tabForge = "Intel",
    tabUrge = "STEALTH",
    tabAnalytics = "Files",
    tabProfile = "Agent",
    logName = "Secret Files",
    primaryColor = Color(0xFFB0B0B0) // Gunmetal
)

val SoloLevelerTheme = GritLockThemeData(
    id = "SOLO",
    name = "Solo Leveler",
    tabHome = "Stats",
    tabAtlas = "World",
    tabLocks = "Skills",
    tabForge = "Store",
    tabUrge = "AWAKEN",
    tabAnalytics = "Logs",
    tabProfile = "Player",
    logName = "Mission Log",
    primaryColor = Color(0xFF00BFFF) // Electric Blue
)

val CozyTheme = GritLockThemeData(
    id = "COZY",
    name = "Cozy Adventurer",
    tabHome = "Camp",
    tabAtlas = "Journey",
    tabLocks = "Pack",
    tabForge = "Craft",
    tabUrge = "BREATHE",
    tabAnalytics = "Log",
    tabProfile = "Self",
    logName = "Travel Log",
    primaryColor = Color(0xFF8B4513) // Saddle Brown
)

val DefaultGritTheme = GritLockThemeData(
    id = "DEFAULT",
    name = "Default",
    tabUrge = "GRIT",
    tabForge = "Forge",
    logName = "Daily Log",
    primaryColor = Color(0xFFD0BCFF)
)

fun getThemeData(id: String): GritLockThemeData {
    return when (id) {
        "SUPERHERO" -> SuperheroTheme
        "AGENT" -> AgentTheme
        "SOLO" -> SoloLevelerTheme
        "COZY" -> CozyTheme
        else -> DefaultGritTheme
    }
}
