# Fix Anki Import Boilerplate and Compatibility Issues

The `AnkiImporter` is currently failing to filter out compatibility/boilerplate notes (like "please update to latest anki version") and might be picking the wrong database file in modern `.apkg` files. This plan improves the robustness of the import process.

## User Review Required

> [!NOTE]
> The import process will now be more aggressive in filtering out boilerplate cards. If you have valid cards that happen to contain words like "update to latest anki", they might be skipped.

## Proposed Changes

### [Anki Data Layer]

#### [MODIFY] [AnkiImporter.kt](file:///C:/Users/tjcsc/AndroidStudioProjects/GrittierLock/app/src/main/java/com/example/fitlock/data/AnkiImporter.kt)
- **Improve Database Selection**: Handle both `collection.anki2` and `collection.anki21` files. Try `collection.anki21` first as it's the modern standard, and fall back to `collection.anki2` if needed.
- **Robust Boilerplate Filtering**:
    - Implement a `stripHtml` helper for text checking.
    - Use a more flexible check for "update to latest anki" that isn't broken by small wording changes or HTML tags.
    - Add checks for other common AnkiDroid boilerplate phrases.
- **Deck Name Safety**: Wrap deck ID parsing in a try-catch to avoid crashes on non-standard `col` JSON.
- **Better Field Processing**: Log card counts and types to help debug future import issues.

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors: `./gradlew :app:assembleDebug`

### Manual Verification
- Re-import an `.apkg` file that previously showed the "please update to latest anki version" cards.
- Verify that only actual study cards are imported and deck names are correctly preserved.
