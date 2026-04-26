# Implementation Plan: RPG Progression & Smart To-Do System

This plan details the technical roadmap for transforming FitLock into a "Solo Leveling" style RPG while integrating a data-driven To-Do list.

---

## 1. RPG Progression System (Section 1)
**Goal:** Map real-world physical and mental efforts to digital character attributes that provide tangible app benefits.

### Core Stat Mapping
| Stat | Real-World Source | Data Origin | In-App Benefit |
| :--- | :--- | :--- | :--- |
| **Strength (STR)** | Pushups, Dips, Weights | ML Kit / Manual | Increases "Max HP" (How many bypasses allowed before hard-lock). |
| **Agility (AGI)** | Running, Sprints, Speed | Health Connect (Run Data) | Reduces the "Typed Phrase" length in the Emergency Bypass. |
| **Vitality (VIT)** | Sleep Duration & Quality | Health Connect (Sleep Data) | Extends the "Lockout Countdown" (more time to finish what you're doing). |
| **Intelligence (INT)** | To-Do List Completion | FitLock Todo Table | Lowers the rep requirement for "Grind" apps (Work/Study). |
| **Charisma (CHA)** | Anki Reviews, Duolingo | API / Notifications | Increases XP multipliers for all workouts. |

### Technical Achievement
1.  **Database Migration:** Update the `UserStats` entity in `AppGroup.kt` to include `long` values for `strXp`, `agiXp`, `vitXp`, `intXp`, `senXp`, `chaXp`.
2.  **Health Connect Workers:** Create a background service that runs every morning to fetch:
    *   `SleepSessionRecord` for **Vitality**.
    *   `ExerciseSessionRecord` (Type: Running) for **Agility**.
3.  **Stat Logic Helper:** Create a `StatCalculator` class that determines the "Level" of each individual stat using a logarithmic scale (e.g., Level 1 = 100XP, Level 2 = 250XP).

---

## 2. Smart To-Do List & Productivity (Section 3)
**Goal:** A task manager that predicts effort and tracks deep-work focus.

### Features
*   **Task Estimation:** When adding a task, the user provides a "Predicted Pomodoros" (25-min blocks) estimate.
*   **Actual vs. Predicted:** The app uses historical data to show a "Consistency Score" (how good are you at estimating your own speed?).
*   **The Deep Work Overlay:** A Pomodoro timer that, while active, hard-locks all "Distraction" app groups.

### Data Model (`TodoTask` Entity)
```kotlin
@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val predictedPomodoros: Int,
    val actualPomodoros: Int = 0,
    val category: String, // "Intelligence" or "Charisma"
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 3. The Final Design (UI/UX)

### Screen A: The "Hunter Status" (Profile Tab)
*   **Visual:** A blue-tinted holographic interface (Solo Leveling style).
*   **Stat Hexagon:** A spider-chart showing the balance of your STR, AGI, VIT, INT, CHA.
*   **Buff List:** "Active Buff: *Agile Mind* (Bypass phrase reduced by 30% due to morning run)."

### Screen B: The "Quest Board" (To-Do Section)
*   **Current Quest:** The top task in your list.
*   **Start Quest Button:** Launches a 25-minute Pomodoro timer.
*   **Overlay Effect:** While the timer runs, your phone wallpaper changes to a "Gate" or "Dungeon" icon, indicating you are in "Deep Work" mode.

### Screen C: The Predictor Analytics
*   **Graph:** Shows a bar chart where each bar has two colors: **Blue** (Estimated Time) and **Red** (Actual Time). 
*   **Intelligence XP:** Earned based on `(Estimated / Actual) * CompletionMultiplier`.

---

## 4. Learning Roadmap (Step-by-Step for You)

1.  **Week 1: The Foundation**
    *   Modify `UserStats` and learn how to do a "Room Database Migration."
    *   Update the `ProfileScreen` UI to show the new stat counts.

2.  **Week 2: Health Integration**
    *   Extend `HealthConnectManager.kt` to read `SleepSessionRecord`.
    *   Learn how to parse "Duration" objects in Java/Kotlin.

3.  **Week 3: The To-Do List**
    *   Create the `TodoTask` entity and its `Dao`.
    *   Build a simple screen with a `LazyColumn` to show tasks.

4.  **Week 4: The Prediction Logic**
    *   Write a function that calculates the average difference between `predicted` and `actual` Pomodoros.
    *   Connect task completion to the `updateUserStats` function you already have in `MainActivity.kt`.
