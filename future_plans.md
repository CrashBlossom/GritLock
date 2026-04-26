# Future Plans for FitLock (The "Solo Leveling" Evolution)

This document outlines ideas for making FitLock more engaging, disciplined, and integrated with other productivity ecosystems.

## 1. Solo Leveling & RPG Elements
Transform the fitness journey into a "Hunter's" progression system.

*   **Core Stat Scaling:**
    *   **Strength (STR):** Gained from Pushups/Dips. Increases HP.
    *   **Agility (AGI):** Gained from Pullups/Sprints. Reduces "Typed Phrase" length in Bypass.
    *   **Vitality (VIT):** Gained from Squats/Planks. Increases "Lockout Delay" (gives you more time before an app blocks).
    *   **Intelligence (INT):** Gained from Anki/Learning tasks. Lowers the rep requirement for "Grind" apps.
*   **The Penalty Quest:**
    *   If a Daily Quest (Challenge) is failed, the app enters a "Penalty State" where *all* non-essential apps are hard-locked for 4 hours unless a massive workout is completed.
*   **Job Classes:** 
    *   At Level 20, choose a class (e.g., Tank: lower reps but longer planks; Assassin: high intensity, low rest).

## 2. API Integrations
Connect FitLock to existing productivity tools to create a unified discipline web.

*   **Anki API:**
    *   **Requirement:** Unlock "Educational" or "Work" apps by completing a specific number of flashcard reviews.
    *   **Status:** Hard-lock distracting apps until the daily Anki deck is cleared.
*   **Beeminder API:**
    *   **Stakes:** If you use the "Emergency Bypass" too many times, FitLock automatically notifies Beeminder to "derail" your goal and charge you money.
    *   **Data Sync:** Automatically send workout reps to Beeminder to track "Graph of Discipline."
*   **RescueTime API:**
    *   **Dynamic Goals:** If RescueTime detects you've spent >2 hours on "Productivity-draining" sites, FitLock automatically doubles all exercise requirements for the rest of the day.
*   **GitHub/Commit API:**
    *   Unlock entertainment apps only after a commit has been pushed to a specific repository.

## 3. To-Do List & Productivity Section
Internalize task management to drive app unlocks.

*   **The "Deep Work" Timer:** 
    *   A Pomodoro-style timer inside FitLock. While active, all apps in the "Social" group are hard-blocked.
*   **Task-Based Unlocks:**
    *   Instead of exercises, some app groups can require checking off high-priority tasks from the internal To-Do list.
*   **Habitica Integration:**
    *   Sync tasks from Habitica. Damaging a boss in Habitica could be the "key" to unlocking your phone for 30 minutes.

## 4. Habitica-Style Social Elements
*   **Party Quests:** Connect with friends. If one person fails their workout, everyone's "distraction apps" get a 20% rep penalty the next day.
*   **Equipment/Inventory:** Buy items with XP that provide permanent buffs (e.g., "Weights of Discipline": +5% XP but all reps count as 0.8).

## 5. Anti-Cheat & Hardening
*   **Device Admin/Knox Integration:** Make it significantly harder to uninstall the app or force-stop the accessibility service without entering a "Cool-down" period.
*   **Location-Based Locks:** Hard-lock streaming apps if the GPS detects you are at the "Gym" until a workout is started.
