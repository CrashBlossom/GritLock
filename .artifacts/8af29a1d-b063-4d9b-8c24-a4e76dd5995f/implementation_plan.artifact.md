# Routine Evolution, Journaling, and Bug Fixes

This plan implements advanced routine features (Overcoming Gravity style), journaling in the Daily Pledge, fixes the Gauntlet crash, and refines the relock notification logic.

## Proposed Changes

### [Fix] Gauntlet Crash and State Management
- Fix redundant `MutableStateFlow` in `GauntletService`.
- Ensure `startForeground` is called immediately with a valid notification.
- Use a more robust state management pattern to prevent lifecycle crashes.

### [Fix] Relock Notification
- Update `GritLockAccessibilityService` to show the specific **App Group Name** in the unlock notification.
- Ensure the timer updates live (every second) in the notification.

### [Feature] Routine Evolution (Overcoming Gravity / Routinery Style)
- Redesign `GauntletExecutionScreen` to use a **vertical task-focused layout**.
- Add circular progress indicators for better visualization of time remaining.
- Support "Routine Blocks" (Pairs/Triplets) in the data model and UI.

### [Feature] Mindful Pledge Journaling
- [NEW] Add `eveningReflection` field to `DailyPledge` entity.
- [MODIFY] Update `PledgeScreen` to include a morning objective prompt and an evening reflection area.
- [MODIFY] Integrate reflection notes into the **Daily Discipline Log**.

## User Review Required
> [!IMPORTANT]
> - The **Gauntlet crash** is likely due to inconsistent state flows between the service and the UI. I will unify these.
> - The **Relock logic** is confirmed to use absolute clock time (30 mins from unlock), but I will refine the notification to make this more transparent to the user.

## Verification Plan

### Automated Tests
- Build and verify database migration.
- Verify notification updates via logcat monitoring.

### Manual Verification
- Start a Gauntlet routine and verify the UI doesn't crash and shows the vertical list.
- Unlock an app and check if the notification shows the group name and counts down live.
- Complete a daily pledge review and verify the reflection appears in the Daily Log.
