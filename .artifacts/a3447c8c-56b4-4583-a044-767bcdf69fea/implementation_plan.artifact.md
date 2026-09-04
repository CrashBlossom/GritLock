# Flashcard Requirement, Plank Units, and Morning Planning

This plan introduces "Flashcard Review" as an unlock requirement, fixes the unit display for Planks, and ties the Daily Pledge to a Morning Journal/Planning feature.

## Proposed Changes

### [Core Data]

#### [MODIFY] [ExerciseTrackerManager.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/exercise/ExerciseTrackerManager.kt)
- Add `FLASHCARDS` to the `ExerciseType` enum.

#### [MODIFY] [AppGroup.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/data/AppGroup.kt)
- Add `mainObjective: String? = null` to the `DailyPledge` data class.

#### [MODIFY] [GritLockDatabase.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/data/GritLockDatabase.kt)
- Increment DB version to 23.
- Add `MIGRATION_22_23` to add the `mainObjective` column to the `daily_pledges` table.

### [UI / App Groups]

#### [MODIFY] [SharedComponents.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/ui/SharedComponents.kt)
- Update `RequirementRow` label:
    - "Cards" for `FLASHCARDS`.
    - "Seconds" for `PLANK` and `APP_USAGE`.
    - "Reps" for everything else.

#### [MODIFY] [LockOverlay.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/ui/LockOverlay.kt)
- Handle `FLASHCARDS` requirement: show a "Start Review" button and progress.

### [UI / Daily Pledge & Morning Planning]

#### [MODIFY] [HomeViewModel.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/ui/HomeViewModel.kt)
- Implement `initDailyPledgeIfMissing()` to ensure today's pledge exists when the Home screen is opened.
- Update `updatePledge()` to handle committing with an objective.

#### [MODIFY] [PledgeScreen.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/ui/PledgeScreen.kt)
- Overhaul the "PENDING" state into a **Morning Planning** layout.
- Add an `OutlinedTextField` for "Main Objective of the Day".
- Pass the objective text to the `onCommit` callback.

#### [MODIFY] [HomeHub.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/ui/HomeHub.kt)
- Update `WillpowerDashboard` to show the `mainObjective` text (once committed) instead of the generic "Stay focused" text.
- Ensure the pledge card displays properly even if the status is `PENDING`.

### [UI / Main Flow]

#### [MODIFY] [MainActivity.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/MainActivity.kt)
- Handle `FLASHCARDS` exercise type in the `track` screen: navigate to `flashcard_review` and back.
- Call `initDailyPledgeIfMissing()` in `onCreate` or `onResume`.

#### [MODIFY] [FlashcardReviewScreen.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/ui/FlashcardReviewScreen.kt)
- Support `requiredCount` to automatically return once the goal is reached during an app-unlock flow.

## Verification Plan

### Manual Verification
- **App Groups**: Create a group requiring 30s Plank. Verify it says "Seconds".
- **App Groups**: Create a group requiring 5 Flashcards. Verify it shows "Cards" and starts a review session.
- **Pledge**: Open the pledge screen in the morning. Enter a main objective like "Finish the project report".
- **Home Hub**: Verify the Home screen now says "Objective: Finish the project report" on the willpower card.
- **Persistence**: Restart the app and verify the objective and streak are preserved.
