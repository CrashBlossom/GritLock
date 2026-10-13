# Unified Protocol Gate Architecture & Decision Flowchart

> [!NOTE]
> This document details the refined `ProtocolGate` architecture, mapping App Blocks, Positive Focus Portals, Routines, and Challenges into a single cohesive system where **To-Do, App Usage, Exercise, and Habits** form the core requirements.

---

## 1. Unified Protocol Architecture Table (Refined)

| Feature / System | Gate Mode (`GateMode`) | Primary Triggers | Constraints | Requirements | Rewards / Outcomes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **🔒 Restrictive App Blocks** | `BLOCK_SCREEN` | `APP` (Package Names) | Time Range, WiFi SSID | Exercises, Water, Writing, Breathwork, To-Dos, Habit Quests | Unlocks target app for N minutes |
| **☀️ Routines / Habit Stacks** | `BLOCK_SCREEN` | `TIME`, `WIFI`, `MANUAL` | Time Range, WiFi SSID | Sequence of habits, breathwork, and exercises | Blocks screen until routine steps are completed, grants XP |
| **🟢 Positive Focus Portals** | `BACKGROUND_RAID` | `APP` (Package Names) | Time Range | Productive App Usage | Tracks usage passively in background, grants +XP / min & Attribute growth |
| **⚔️ Challenges & Raids** | `BACKGROUND_RAID` | `MANUAL` | None | Cumulative exercise reps or habit completions | Elite badges, XP rewards, Hall of Trophies unlock |
| **💎 Secret Vault** | `BLOCK_SCREEN` | `MANUAL` / `APP` | None | Specific exercise or cognitive toll | Releases secret password, vault note, or private link |

---

## 2. Protocol Gate Execution Flowchart

```mermaid
graph TD
    A[Trigger Event Detected] --> B{Gate Mode?}
    B -->|BLOCK_SCREEN| C[Intercept App Launch / Routine Start & Show LockOverlay]
    B -->|BACKGROUND_RAID| D[Run in Background Continuously Positive Portals & Challenges]

    C --> E{Check Constraints}
    E -->|Time / WiFi Valid| F[Evaluate Requirements]
    E -->|Constraint Failed| G[Instant Access / Bypass]

    F -->|Exercises| H[ML Kit Camera / Pocket Sensor Tracker]
    F -->|Water / Habits| I[Interactive Checkbox Task]
    F -->|Write Words| J[Journal Reflection Prompt]
    F -->|Time-Based Habits| K[Live Timer with Flexible Manual 'Complete' Checkbox]
    F -->|To-Do Checklist| L[Check Off Active Tasks]

    H & I & J & K & L --> M{Requirements Complete?}
    M -->|Yes| N[Grant XP & Stat Points / Release Vault Secret / Unlock Gate]
    M -->|No| F
```

---

## 3. Action Classification Flowchart (User Decision Tree)

```mermaid
flowchart TD
    oneOff{"Is it one-off?"}
    task["ToDo / Task Item"]
    appCapture{"Can it be captured using an app?"}
    appUsage["App Usage (APP_USAGE)"]
    cameraTrack{"Can a camera track the activity?"}
    exercise["Exercise (Pushup, Squat, Plank, Situp, etc.)"]
    habit["Habit (Water, Breathwork, Master Habit)"]

    oneOff -->|Yes| task
    oneOff -->|No| appCapture
    appCapture -->|Yes| appUsage
    appCapture -->|No| cameraTrack
    cameraTrack -->|Yes| exercise
    cameraTrack -->|No| habit
```
