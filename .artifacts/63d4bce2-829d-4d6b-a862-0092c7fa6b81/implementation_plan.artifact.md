# Implementation Plan - Unifying Protocol Gates & Specialized Requirement Handlers

This plan details the architecture and implementation steps to unify App Blocks (`AppGroup`) and Routines (`Quest`) into a single, cohesive **"Discipline Gates"** hub in **The Forge**, alongside specialized handlers for `WATER`, `WRITE_WORDS`, and `BREATHWORK`.

## User Review Required

> [!IMPORTANT]
> **Unifying App Blocks & Routines in The Forge:** We will merge app blocks and routines into a single unified management screen in **The Forge** under Tab 1 ("Routines & Stacks / Discipline Gates"). Both will support time, WiFi, and **App Triggers** (`packageNames`), allowing app blocks to look and behave just like routines with full step sequences.

> [!NOTE]
> **Specialized Requirement Handlers:**
> - **`WATER`:** Renders as a simple checkbox in unlock/routine screens.
> - **`WRITE_WORDS`:** Forces the user to write a journal entry / daily log reflection before clearing.
> - **`BREATHWORK`:** Launches the camera tracking screen to count seconds while facing the screen.

---

## Proposed Changes

### 1. Data Layer & Repository (`AppGroup.kt`)
- Ensure `AppGroup` supports app package triggers (`packageNames`) alongside time/WiFi triggers and unified exercise requirements.

### 2. The Forge UI (`ForgeScreen.kt`)
- Upgrade Tab 1 ("Routines & Stacks" ➔ "Discipline Gates") to display and manage **both** Time/WiFi routine stacks and App-triggered discipline blocks in one unified view.
- Add App Trigger selector (selecting restricted/watched apps) directly into the routine/block editor.

### 3. Specialized Requirement Execution Handlers (`MainActivity.kt` & Lock/Routine Screens)
- **`WATER`:** Checkbox interaction.
- **`WRITE_WORDS`:** Daily log writing prompt dialog/overlay.
- **`BREATHWORK`:** Camera timer tracking facing the screen.

---

## Verification Plan

### Automated Tests
- Run `gradle_build("app:assembleDebug")` to ensure zero compilation errors.

### Manual Verification
- Verify unified discipline gates in The Forge.
- Test `WATER`, `WRITE_WORDS`, and `BREATHWORK` interactive requirements.
