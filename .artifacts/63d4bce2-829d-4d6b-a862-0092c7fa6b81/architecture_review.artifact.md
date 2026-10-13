# GritLock (FitLock) - Architecture Review & Unification Roadmap

> [!NOTE]
> This document provides a comprehensive breakdown of the current data and architectural models across **Habits**, **Tasks/To-Dos**, **Goals/Projects**, and **Protocol Gates**, updated with the user's insights on unifying analytics, establishing the "North Star" mission, and deprecating redundant tabs like Atlas.

---

## 1. Current Architecture Breakdown & Evaluation

### A. Habits (`HabitDefinition` & `Habit`)
- **Definition (`HabitDefinition`):** Master template stored in Room DB (name, icon, tracking type: `TIME` or `CHECKBOX`, default estimated duration).
- **Instance (`Habit`):** Linked to a Gauntlet/Routine or daily schedule. Tracks completion status, duration, or timestamps.
- **Purpose:** Daily disciplined behaviors that build attribute XP.

### B. Tasks, To-Dos & Brainstorming (`QuestBlock` & `ProjectTask`)
- **Tasks (`QuestBlock`):** Actionable checklist items tied to daily agendas or routine steps. Supports priority (`HIGH`, `MEDIUM`, `LOW`), predicted time, and **active Stopwatch tracking** (`trackingStartTimestamp`, `actualDurationSeconds`).
- **Brainstorming / Project Tasks (`ProjectTask`):** Ideas captured during Morning Journal / Evening Review or project planning backlogs.
- **Purpose:** Task execution and project management with **time estimation vs. actual duration analytics**.

### C. Goals & Projects (`Goal`, `Project`, `Milestone`) - *Deprecated / Subsumed*
- Formerly used for macro-planning. With the new unified architecture, these are now subsumed directly into **Challenges & Raids** (`ProtocolGate`), removing database clutter.

---

## 2. Refined Unification Roadmap

### Proposal 1: Unified Action System (`ProtocolTask`)
- **Concept:** Merge Habits and To-Do/Task items into a single polymorphic action entity.
- **Key Feature:** Preserves **time estimation vs. actual duration analytics** for both persistent habits (e.g., *Meditation 10m*) and one-and-done tasks, tracking efficiency over time.
- **Difference:** Persistence flag (`isPersistent = true` for habits vs `false` for one-and-done tasks).

### Proposal 2: The Challenge & "North Star" Hierarchy
- **Goals ➔ The North Star Mission:** The single biggest active challenge designated as the user's ultimate overarching quest.
- **Projects ➔ Medium-Scale Challenges / Raids:** Structured discipline gates.
- **Milestones ➔ Achievements & Rewards:** Trophies and XP unlocked from progressing through challenges.

### Proposal 3: Retiring the Atlas Tab
- **Verdict:** The separate "Atlas" tab is now fully redundant as its components (goals, roadmaps, planning) are naturally handled by **The Gress**, **Challenges**, and the **Trophy Cabinet**. Phasing out the Atlas tab streamlines the navigation bar into a razor-sharp Solo Leveler interface (Home Hub, Portals/Locks, Matrix, Vitality, Calendar/Tasks, Mental Vault, Shrine, Challenges).
