# FitLock App Requirements & Architecture

## Overview
FitLock is an Android application that helps users build exercise habits by "locking" specific apps (social media, games, etc.) until a physical exercise requirement is met.

## Core Mechanics
1.  **App Groups**: Users create groups of apps they want to restrict.
2.  **Exercise Requirements**: Each group is associated with an exercise (e.g., 10 pushups).
3.  **Rep Banking**:
    *   Instead of immediate unlocking, users "bank" completed reps.
    *   Example: Doing 50 pushups adds 50 reps to the "Pushup Bank".
    *   Unlocking an app group consumes a specific number of reps from the bank.
    *   The bank can have reset frequencies (Daily, Weekly, Never).
4.  **Tracking Modes**:
    *   **Camera**: Uses ML Kit Pose Detection to automatically count reps.
    *   **Pocket Mode**: Uses the device's accelerometer and other sensors to track movement.
    *   **Pedometer/Health**: Syncs with Health Connect for steps/walking goals.
5.  **Gamification**:
    *   **XP & Leveling**: Earn XP for every rep completed.
    *   **Streaks**: Daily workout streaks.
    *   **Challenges**: Specific goals (e.g., "1000 pushups in a month") for bonus XP.

## Exercise Detection Details

### Camera Mode (ML Kit Pose Detection)
All camera-based exercises use angle calculation between three body landmarks.
*   **Pushups**:
    *   **Judgement**: Angle between Shoulder, Elbow, and Wrist.
    *   **Logic**: A rep is counted when the elbow angle goes below the "bottom" threshold (e.g., 90°) and returns above the "top" threshold (e.g., 160°). It also requires a vertical shoulder displacement (at least 10% of screen height) to ensure the whole body is moving.
*   **Squats**:
    *   **Judgement**: Angle between Hip, Knee, and Ankle.
    *   **Logic**: A rep is counted when the knee angle goes below the "bottom" threshold (deep squat) and returns above the "top" threshold (standing).
*   **Situps**:
    *   **Judgement**: Angle between Shoulder, Hip, and Knee.
    *   **Logic**: A rep is counted when the hip angle goes below the "up" threshold (crouched) and returns above the "down" threshold (lying flat).
*   **Rows**:
    *   **Judgement**: Angle between Shoulder, Elbow, and Wrist (elbow flexion).
    *   **Logic**: A rep is counted when the elbow angle goes below the "pulled" threshold (e.g., 70°) and returns above the "extended" threshold (e.g., 150°).
*   **Plank**:
    *   **Judgement**: Angle between Shoulder, Hip, and Ankle (body straightness).
    *   **Logic**: Instead of reps, it counts seconds. A "tick" (rep) is added for every second the body angle remains above the "straight" threshold (e.g., 150°).
*   **Future Integrations (Yoga / Tai Chi)**:
    *   **Judgement**: Pose matching and stability.
    *   **Logic**: Compare detected landmarks against a target "asana" or movement path. Use confidence scores and hold times to validate the posture.

### Pocket Mode (Multi-Sensor Fusion)
Leverages the device's sensor suite to judge various exercises without visual input.
*   **Accelerometer (`TYPE_LINEAR_ACCELERATION`)**:
    *   **Judgement**: Linear acceleration peaks and dips on the Y-axis.
    *   **Logic (Squats/Pushups/Situps)**:
        *   **Down Phase**: Detected when Y-acceleration drops below a negative threshold (e.g., -2.5 m/s²).
        *   **Up Phase**: Detected when Y-acceleration rises above a positive threshold (e.g., 3.5 m/s²).
        *   A rep is completed when the sequence "Negative Peak -> Positive Peak" is observed.
*   **Gyroscope (`TYPE_GYROSCOPE`)**:
    *   **Judgement**: Angular velocity (rotation).
    *   **Logic (Rows/Yoga)**: Detects the rotation of the arm or torso. For Rows, it can measure the "tilt" of the forearm as it pulls back. In Yoga, it monitors balance and orientation stability.
*   **Barometer (`TYPE_PRESSURE`)**:
    *   **Judgement**: Subtle changes in air pressure.
    *   **Logic (Deep Breathing)**: Can theoretically detect the rise and fall of the chest/abdomen if the phone is held against the body, though microphone/accelerometer fusion is more reliable.
*   **Microphone (Audio Analysis)**:
    *   **Judgement**: Sound frequency and amplitude.
    *   **Logic (Deep Breathing)**: Analyzes the sound profile of inhalation and exhalation to count "reps" of breath.
*   **Magnetometer (`TYPE_MAGNETIC_FIELD`)**:
    *   **Judgement**: Heading and orientation relative to North.
    *   **Logic (Tai Chi)**: Monitors slow, deliberate rotations and shifts in direction which are characteristic of Tai Chi movements.

## Data Model
### `AppGroup`
*   `name`: String
*   `packageNames`: List of Strings
*   `exercises`: List of `ExerciseRequirement` (Type and Count)
*   `unlockDurationMinutes`: How long the app remains unlocked after "spending" reps.
*   `isEnabled`: Boolean
*   `schedule`: List of active times.

### `UserStats`
*   `totalXp`: Total experience earned.
*   `level`: Current user level.
*   `currentStreak`: Current daily streak.
*   `bankedReps`: Map of exercise type to count (e.g., `{"PUSHUP": 45, "SQUAT": 20}`).
*   `bankResetFrequency`: "Daily", "Weekly", "Never".

## Technical Stack
*   **Compose**: UI framework.
*   **Room**: Local database.
*   **ML Kit**: Pose detection for camera-based tracking.
*   **Accessibility Service**: To detect when "locked" apps are opened and show the overlay.
*   **Glance**: For home screen widgets.

// Updated section in requirements.md
*   **Speed & Tempo Tracking**:
    *   **Logic**: Measures the Delta-T between the start of the "Down Phase" and the completion of the "Up Phase".
    *   **Use Case**: Can enforce "Slow Reps" or detect "Explosive Reps" (High-intensity interval training).
*   **Positioning (Gravity/Accelerometer)**:
    *   **Logic**: Monitors the static gravity vector before movement begins.
    *   **Use Case**: Detects if the phone is in a pocket (Vertical Y-axis) vs. strapped to an arm or resting on the back (Horizontal Z/X-axis), automatically adjusting the detection logic.

## System Architecture & Data Flow
The app is built on a "Observe -> Block -> Verify -> Reward" cycle:

1.  **Storage (Room DB)**:
    *   `GritLockDatabase` is the single source of truth.
    *   `AppGroup` table defines the "laws" (which apps are blocked and what they cost).
    *   `UserStats` table tracks global state (XP, Level, Rep Bank).
    *   `WorkoutHistory` records every rep for analytics.

2.  **Monitoring (Accessibility Service)**:
    *   `GritLockAccessibilityService` runs in the background.
    *   It listens for `WindowStateChanged` events.
    *   When a restricted app (or a browser with a blocked URL) is detected, it queries the DB.
    *   If no "Unlock" exists, it triggers the `LockOverlayActivity`.

3.  **Intervention (Overlay & Tracker)**:
    *   `LockOverlayActivity` sits on top of the restricted app.
    *   It uses `ExerciseTrackerManager` to start the Camera or Pocket sensors.
    *   Once the `Analyzer` (e.g., `PushupAnalyzer`) confirms the reps, it updates the DB.

4.  **Reward (Gamification)**:
    *   Successful workouts update `UserStats` with XP.
    *   Leveling up and streak logic are calculated in `MainActivity` and persisted to Room.

## Future Plan & Recommendations
*   **AI Pose Correction**: Integrate a custom TFLite model to give specific feedback (e.g., "Lower your hips during pushups").
*   **Advanced Browser Integration**: Use specific Resource IDs for major browsers (Chrome, Firefox, Samsung Internet) to grab URLs directly from the address bar for 100% accurate web blocking.
*   **Smart Watch Sync**: Use Wear OS sensors to allow the phone to stay on the floor while the watch tracks the reps.
*   **Social Layer**: Implement a Firebase-backed leaderboard for groups or friends to compete in "Rep Battles".
*   **Battery Optimization**: Move sensor processing to a `ForegroundService` with a low-power "batching" mode for all-day step tracking.