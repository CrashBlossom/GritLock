# GritLock - Future Development Roadmap

This document outlines the vision for expanding GritLock beyond a simple fitness-based app blocker into a comprehensive life-gamification and accountability platform.

## 🎮 1. Advanced Gamification
*   **Character Progression**: Create a custom avatar that levels up and changes appearance as you gain XP. Equipment and "skins" can be purchased with "Sweat Coins" earned through workouts.
*   **Quest System**:
    *   *Daily Quests*: Specific random exercise sets (e.g., "The Morning Grime: 30 Pushups before 9 AM").
    *   *Weekly Bosses*: High-volume requirements to unlock a "treasure chest" of XP or premium app themes.
*   **Guilds & Leaderboards**: Join fitness guilds with friends. If one person fails their daily goal, the entire guild might lose XP or face a "mass lock" on specific apps.
*   **Skill Trees**: Spend points to specialize in "Strength" (Pushups/Pullups), "Endurance" (Steps), or "Zen" (Meditation). Specializing unlocks unique themes or reduced unlock requirements.

### 🛠️ Implementation Strategy
*   **Avatar State**: Expand the `UserStats` Room entity to include `avatarLevel`, `equippedItemsList`, and `currencyBalance`.
*   **Rendering**: Use **Jetpack Compose Runtime** to dynamically swap SVG/PNG layers for the avatar based on equipment IDs.
*   **Quest Engine**: Implement a `QuestManager` singleton that subscribes to the `WorkoutHistory` Flow and evaluates quest completion logic (e.g., time-window checks).
*   **Social Backend**: Integrate **Firebase Firestore** for real-time Guild data and global leaderboards.

## 🐝 2. Beeminder Integration
*   **The Yellow Brick Road**: Connect GritLock to Beeminder so that every rep tracked by the camera or sensors automatically updates a Beeminder goal.
*   **Automated Fines**: If you derail on your fitness goals in Beeminder, GritLock could enter "Hardcore Lock" mode, doubling your exercise requirements until you're back on track.
*   **Real-time Sync**: Use Beeminder's API to show your "days until derailment" directly on the GritLock dashboard.

### 🛠️ Implementation Strategy
*   **API Client**: Use **Retrofit** with a Beeminder-specific interface to handle OAuth2 authentication and data point submission.
*   **Sync Worker**: Create a `BeeminderSyncWorker` using **WorkManager** to batch-upload reps every hour or immediately after a workout.
*   **Enforcement Logic**: Add a `derailmentMultiplier` field to `AppGroup` that is updated via the Beeminder status API.

## 🧠 3. Holistic Wellbeing Goals
Expand the "Unlock" requirements beyond physical exercise to include mental and emotional health:
*   **Mental Workouts**: Integrate a **Dual N-Back** task. To unlock YouTube, you must complete 5 minutes of cognitive training to prove your brain is sharp.
*   **Breathing Exercises**: "Box Breathing" requirement. Use the camera or a simple UI timer to ensure the user performs 2 minutes of focused breathing before accessing social media.
*   **Meditation**: Require a 5-10 minute guided meditation session (tracked by screen-on time or bio-feedback if available) to unlock "high-dopamine" apps.
*   **Hydration**: Require a photo of a full glass of water (via AI object detection) to unlock the next 30 minutes of screen time.

### 🛠️ Implementation Strategy
*   **Cognitive Components**: Build a `DualNBackScreen` using Compose that reports success/failure to the `LockStatusManager`.
*   **Biometrics**: Use **CameraX** to analyze micro-variations in skin tone (photoplethysmography) to estimate heart rate during breathing/meditation.
*   **Object Detection**: Use **ML Kit Object Detection & Tracking** to verify a water glass is present in the camera frame before allowing a "Hydration Unlock."

## 🗡️ 4. Solo Leveling System
Inspired by the "Daily Quest" from Solo Leveling:
*   **The Penalty Zone**: If the Daily Quest (e.g., 100 Pushups, 100 Squats, 100 Situps, 10km run) is not completed by midnight, the user is "transported" to a digital penalty zone where all non-essential apps are locked for 4 hours, regardless of previous unlocks.
*   **Rank-Up Trials**: Every 10 levels, the user must complete a high-intensity "Rank-Up" workout to progress from E-Rank to D-Rank, and eventually S-Rank.
*   **Hidden Quests**: Discoverable by doing specific actions (e.g., doing 50 reps in one go might unlock a "Beru" badge or a secret dark theme).

### 🛠️ Implementation Strategy
*   **Midnight Trigger**: Schedule a `DailyQuestEvaluator` using WorkManager to run at 11:59 PM.
*   **Penalty State**: Use a `GlobalPenaltyStatus` data store that the `GritLockAccessibilityService` checks before any `isUnlocked` logic.
*   **Trial Logic**: Create a `TrialActivity` that overrides all system navigation (Home/Back) until the high-intensity requirement is met.

## ⚖️ 5. Reward & Punishment Logic
*   **Rewards**:
    *   *Streaks*: 7-day streaks unlock "Focus Mode" (a period where requirements are halved).
    *   *Digital Currency*: Earn "Grit" to buy temporary "Pass Cards" for emergencies.
*   **Punishments**:
    *   *The Shame Notification*: If you fail a challenge, the app sends a notification to a "Fitness Partner" or posts to a private group.
    *   *Incremental Hardship*: If you use "Emergency Bypass" too often, the target phrase gets longer and the "Discipline" multiplier increases exercise requirements for the next 24 hours.
    *   *XP Decay*: Not working out for 3 days leads to XP loss, potentially lowering your rank.

### 🛠️ Implementation Strategy
*   **Streak Tracking**: Implement a `StreakManager` that calculates daily activity by querying the `WorkoutHistory` table using SQL `COUNT(DISTINCT date)`.
*   **Social Shaming**: Use the **Twilio API** or **Firebase Cloud Functions** to send automated SMS/push alerts to designated "Discipline Partners" when a bypass occurs.
*   **Dynamic UI**: Inject a `bypassPenaltyMultiplier` into the `EmergencyBypassDialog` to scale the complexity of the required text dynamically.

## 🌐 6. Ecosystem Expansion
*   **Wearable Integration**: Direct sync with Garmin, Apple Watch, and WHOOP for more accurate heart-rate-based requirements.
*   **Web Dashboard**: A browser-based view of your stats to keep you accountable even when you're not on your phone.

### 🛠️ Implementation Strategy
*   **Companion Device Support**: Implement a **Wear OS companion app** to relay heart rate and rep data via the **Data Layer API**.
*   **Web Sync**: Build a **Ktor-based backend** to mirror Room database changes to a web-accessible Postgres instance.
*   **Health Connect Extension**: Leverage the existing `HealthConnectManager` to pull `HeartRateRecord` and `OxygenSaturationRecord` to verify workout intensity.
