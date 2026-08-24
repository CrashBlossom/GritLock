package com.example.fitlock.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Total Conversion Theme Data.
 */
data class GritLockThemeData(
    val id: String,
    val name: String,
    val tabHome: String = "Home",
    val tabLocks: String = "Locks",
    val tabUrge: String = "Grit",
    val tabAnalytics: String = "Analytics",
    val tabProfile: String = "Profile",
    val primaryColor: Color,
    val surfaceColor: Color = Color.Unspecified
)

val SuperheroTheme = GritLockThemeData(
    id = "SUPERHERO",
    name = "Vigilante",
    tabHome = "Safe House",
    tabLocks = "Arsenal",
    tabUrge = "DEFEND",
    tabAnalytics = "Intel Map",
    tabProfile = "Identity",
    primaryColor = Color(0xFF00FF00) // Neon Green
)

val AgentTheme = GritLockThemeData(
    id = "AGENT",
    name = "Secret Agent",
    tabHome = "Briefing",
    tabLocks = "Gadgets",
    tabUrge = "STEALTH",
    tabAnalytics = "Intel",
    tabProfile = "Agent",
    primaryColor = Color(0xFFB0B0B0) // Gunmetal
)

val SoloLevelerTheme = GritLockThemeData(
    id = "SOLO",
    name = "Solo Leveler",
    tabHome = "Status",
    tabLocks = "Skills",
    tabUrge = "AWAKEN",
    tabAnalytics = "Quests",
    tabProfile = "Player",
    primaryColor = Color(0xFF00BFFF) // Electric Blue
)

val CozyTheme = GritLockThemeData(
    id = "COZY",
    name = "Cozy Adventurer",
    tabHome = "Camp",
    tabLocks = "Satchel",
    tabUrge = "BREATHE",
    tabAnalytics = "Journal",
    tabProfile = "Traveler",
    primaryColor = Color(0xFF8B4513) // Saddle Brown
)

val DefaultGritTheme = GritLockThemeData(
    id = "DEFAULT",
    name = "Default",
    tabUrge = "GRIT",
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
