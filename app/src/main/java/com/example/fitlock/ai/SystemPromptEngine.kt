package com.example.fitlock.ai

object SystemPromptEngine {

    fun buildPrompt(
        playerName: String,
        level: Int,
        strXp: Long,
        intXp: Long,
        agiXp: Long,
        currentStreak: Int,
        triggeredApp: String = "",
        screenTimeMinutes: Int = 0,
        context: SystemEventContext = SystemEventContext.APP_LOCKED
    ): String {
        val baseDirective = """
            System Directive:
            You are 'The System', a cold, calculating, highly authoritative RPG interface from a Solo Leveling universe governing Player $playerName (Level $level).
            Stats: STR=$strXp, AGI=$agiXp, INT=$intXp. Current Streak: $currentStreak days.
            
            Instructions:
            - Respond strictly as 'The System'. Use bracketed system headers like [SYSTEM WARNING], [PENALTY QUEST], or [QUEST CLEARED].
            - Keep responses under 3 sentences. Be concise, sharp, and commanding.
        """.trimIndent()

        val eventContext = when (context) {
            SystemEventContext.APP_LOCKED ->
                "Scenario: Player attempted to open restricted app '$triggeredApp' after spending $screenTimeMinutes minutes of usage today. Issue a stern warning or penalty requirement."

            SystemEventContext.WORKOUT_COMPLETED ->
                "Scenario: Player successfully completed a physical or mental training quest. Validate their effort and grant system clearance."

            SystemEventContext.URGE_NEGOTIATION ->
                "Scenario: Player reported a strong urge to scroll or break discipline. Provide tactical, cognitive discipline advice to overpower the craving."

            SystemEventContext.DAILY_MOTIVATION ->
                "Scenario: Morning briefing. Summarize today's discipline directive for Player $playerName."
        }

        return "$baseDirective\n\n$eventContext\n\nSystem Voice Output:"
    }
}

enum class SystemEventContext {
    APP_LOCKED,
    WORKOUT_COMPLETED,
    URGE_NEGOTIATION,
    DAILY_MOTIVATION
}
