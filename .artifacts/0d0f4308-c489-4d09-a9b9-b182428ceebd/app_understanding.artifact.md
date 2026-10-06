# GritLock (FitLock) - Up-to-Date App Specification & Architecture Overview

> [!NOTE]
> This document provides a comprehensive architectural and functional summary of **GritLock** (also known as **FitLock**), updated with all recent features including the immersive Solo Leveling Android phone launcher, 8 swipable launcher pages, Health Connect vitality and rowing integration, enhanced To-Do time prediction & active timers, local Gemma AI integration, and the Winter Arc theme with frosted snow effects.

---

## 1. Executive Summary & Recent Changes

**GritLock** is a high-discipline Android productivity and anti-addiction application that transforms the device into a **Solo Leveling** themed system interface. It replaces passive digital consumption with active physical exertion and cognitive training.

### Summary of Recent Changes Since Initial Spec:
- **Solo Leveling 8-Page Swipable Launcher:** Transformed FitLock into a full Android home launcher (`android.intent.category.HOME`) featuring 8 swipable pages (Daily Quest & Targets, Dungeon Gates, Weekly Habit Matrix, Vitality & Health Connect, Calendar & Enhanced To-Do Checklist, Mental Vault, Willpower Shrine, and Time-Gated Daily Challenges).
- **Enhanced To-Do & Task Tracking:** Added time predictions, priority levels (`HIGH`, `MEDIUM`, `LOW`), active task tracking with live timers, brainstorming backlog integration, and completed task prediction vs. actual analytics.
- **Health Connect & Rowing Integration:** Expanded vitality hub to sync sleep, hydration, nutrition, steps, and **Rowing minutes** (ErgData / Concept2 support), with water intake directly logging to Health Connect.
- **On-Device Offline Gemma AI & TTS:** Integrated MediaPipe GenAI (`Gemma4Manager`) for local offline Gemma 4B inference and system voice audio synthesizers.
- **Winter Arc Theme & Frosted Snow Overlay:** Added the `WINTER_ARC` theme with dark midnight sky backgrounds, icy cyan accents, and an interactive falling snow canvas overlay (`FrostedSnowOverlay()`).
- **Structured Routine Builder in The Forge:** Enhanced routine stack configuration to support Time, WiFi, and Time + WiFi triggers (`TIME_BASED`, `WIFI`, `TIME_AND_WIFI`), Lock Shield firewalls, and structured steps (Exercises, Backlog tasks, Water, Flashcards).

---

## 2. Tech Stack & Architecture

- **UI Framework:** [Jetpack Compose](class://androidx.compose.ui.Compose) with Material 3 design and dynamic theme adaptation (`GritLockTheme`).
- **Dependency Injection:** [Hilt](class://dagger.hilt.android.HiltAndroidApp).
- **Persistence:** [Room Database](class://androidx.room.RoomDatabase) (up to version 41 with migrations for countdowns, task timings, priorities, and unique indices).
- **Hardware & Sensors:** CameraX with ML Kit Pose Detection, Android SensorManager, Health Connect API, and Accessibility Services.
- **Modules:** `:app` (mobile) and `:wear` (Wear OS companion).

---

## 3. Core Launcher & Feature Breakdown

### A. The 8-Page Solo Leveling Swipable Launcher (`SoloLauncherScreen`)
1. **Page 0 (Daily Quest & Targets):** System directive header, HP/Streak bars, Today's Aim & Target Goals (Pushups, Squats, Planks, Situps, Anki Cards, Rowing, Water, Steps) with clickable quick-training links, and active daily quests.
2. **Page 1 (Dungeon Gates / App Portals):** Sorted app list with native icons and risk rankings (`[S]` locked, `[P]` productive, `[E]` utility). Long-press popup to create app blocks, hide apps, or uninstall apps.
3. **Page 2 (Weekly Habit Consistency Matrix):** Compressed table showing completed counts and color-coded status for pushups, squats, Anki flashcards, journal, and reviews.
4. **Page 3 (Player Stats & Health Connect Vitality):** Status window, radar attribute chart, live Health Connect vitality hub (Sleep, Water, Food, Steps, Rowing), and direct button to Workout History & Analytics.
5. **Page 4 (Calendar, Countdowns & Enhanced To-Do Checklist):** Date header, custom countdowns/countups (New Year, Birthday, etc.), Brainstorming Ideas Backlog (`💡 Backlog`), Active Task Timer Card, System To-Do Checklist with priority/duration tags, and Completed Tasks History with time predictions.
6. **Page 5 (Mental Vault & Flashcards):** Stoic Motivational Maxims (sorted newest-first, long-press to edit or delete, `[+ Add Quote]` dialog) and Anki spaced-recovery flashcard deck reviews.
7. **Page 6 (Willpower & Shrine):** Sobriety counter, urge negotiation dialog, daily pledges, and morning/evening reviews.
8. **Page 7 (Time-Gated Daily Challenges):** Elite daily raids and challenges enforced by time windows (e.g., Morning Pushup Challenge locked outside 4 AM - 12 PM with system time-lock toasts).

### B. Accessibility & App Blocking Engine
- `GritLockAccessibilityService` enforces app blocking rules, anti-bypass settings, and logs positive focus minutes for productive apps like AnkiDroid and Duolingo French in real time.

### C. The Forge & Routine Builder
- Allows creating and configuring master habits, exercise definitions, flashcard decks, and parent routine stacks with custom triggers (Time, WiFi SSID) and Lock Shield firewalls.
