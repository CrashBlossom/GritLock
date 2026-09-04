# Routine Evolution & Advanced Workouts

This document outlines the proposed architecture for integrating the **Reddit Recommended Routine (RRR)**, **Overcoming Gravity (OG)**, and journaling into the **Pledge** and **Gauntlet** systems.

## 1. Advanced Workout Structure (OG/RRR)
To support complex routines like Overcoming Gravity, we need to move beyond simple sequential habits and support **Blocks** (Triplets, Pairs).

### Proposed Data Model Expansion
*   **WorkoutBlock**: A container within a Gauntlet.
    *   `type`: SINGLE, PAIR, TRIPLET.
    *   `restBetweenSets`: Default rest time (e.g., 90s).
*   **WorkoutExercise**: Individual exercises within a block.
    *   `progression`: Link to a progression tree (e.g., Pushup -> Diamond Pushup -> Pseudo Planche).
    *   `targetReps`: Range (e.g., 5-8).

### UI Implementation
*   **Routinery-style Execution**:
    *   Vertical stack of tasks with a clear "Current" focus.
    *   Circular progress for timers.
    *   Large "Done" button at the bottom.
    *   Automatic transition with the "Transition Buffer" we already implemented.

## 2. Journaling in Pledges
The **Daily Pledge** should be more than just a commitment; it should be a mindful reflection.

### Proposed Workflow
1.  **Morning Pledge**:
    *   Commitment: "I will not use [App Group] today."
    *   Journaling: "What is my main objective?" + "Potential obstacles?"
2.  **Evening Review**:
    *   Success/Fail toggle.
    *   Reflection: "How did I handle the Grit Trials today?"
    *   Integration: Automatically include these notes in the **Daily Discipline Log**.

## 3. Habit Presentation (The "Routinery" Look)
To improve the Gauntlet UI:
*   **Compact Mode**: Show routines as cards with a horizontal scroll of icons representing the habits.
*   **Active Routine**: A persistent "Currently Running" card on the Home Hub.
*   **Visual Streaks**: Use a "GitHub-style" contribution grid for habit consistency to visualize the "Never Miss Twice" rule.

## User Feedback Required
> [!IMPORTANT]
> 1. Should the **Overcoming Gravity** routines be separate from "Habit Stacks", or should they just be a "Super-Habit" type within a Gauntlet?
> 2. For journaling, do you prefer a free-text area or a guided set of questions (e.g., Stoic morning reflection)?
> 3. How strictly should the "Never Miss Twice" rule be enforced? (e.g., a "Willpower XP" penalty if broken?)
