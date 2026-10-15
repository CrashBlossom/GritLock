# GressLock App Requirements & Architecture

## Overview
GressLock is an advanced Android application that helps users build exercise habits and digital self-control by locking specific apps until physical exercise, breathwork, journaling, or hydration requirements are met.

## Core Mechanics
1. **Universal Protocol Gates**: Unifies App Blocks, Routines, Challenges, and Positive Portals (`🟢 Positive Portal`) with WiFi SSID scanning and time window constraints.
2. **12 Core RPG Stats**: `STR, DEX, CON, MOV, INT, WIS, WIL, SAN, CHA, WTH, TEC, LCK`.
3. **Hall of Ascendance**: Tiers 0-5 achievements across all exercises, streaks, health, journaling, and focus time.
4. **Permanent App Awareness Notification**: Real-time foreground service notification showing current app status and unlock countdowns.
5. **Settings System**: Expandable/minimizable section accordions, theme color palettes, and granular morning/evening plan template editors.

## Exercise Detection Details

### Camera Mode (ML Kit Pose Detection)
All camera-based exercises use angle calculation between three body landmarks.
*   **Pushups & Squats & Situps & Pullups & Dips & Hinges & Rows**: Full joint angle tracking and vertical displacement validation.
*   **Plank**: Isometric hold timer counting seconds.
*   **Breathwork**: ML Kit face/presence detection and cycle counting with `3-1-1-1` cadence tempo guidance.

### Pocket Mode (Multi-Sensor Fusion)
Leverages the device's sensor suite (`TYPE_LINEAR_ACCELERATION` and `TYPE_ACCELEROMETER`) to judge various exercises without visual input.

## Data Model
*   `AppGroup`: Defines protocol gates (app blocks, routines, challenges, positive portals) with exercise requirements, unlock duration, WiFi SSIDs, and time window constraints.
*   `UserStats`: Tracks XP, level, 12 RPG stats, streaks, and rep bank inventory with configurable clearance frequencies (`Daily`, `Weekly`, `Monthly`, `Never`).
*   `Achievement`: Tracks Hall of Ascendance milestones (Tiers 0-5).

## Technical Stack
*   **Compose**: UI framework with Material 3.
*   **Room**: Local database (v44 migrations).
*   **ML Kit**: Pose detection for camera-based tracking.
*   **Accessibility Service**: `GritLockAccessibilityService` for real-time app blocking, session countdowns, and permanent app awareness notifications.
*   **Health Connect**: Syncs sleep, water, calories, and steps vitality.
