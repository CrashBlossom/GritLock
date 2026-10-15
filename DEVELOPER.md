# GressLock Developer Guide

Welcome to the GressLock codebase! This app is designed with a **Clean Architecture (MVVM)** to make it easy to maintain and extend.

## 🏗 Architectural Layers

### 1. Data Layer (`data/`)
*   **Entities:** Define our database tables (e.g., `AppGroup`, `UserStats`, `Achievement`).
*   **DAO (Data Access Object):** Contains the SQL queries to talk to the database.
*   **Repository:** The "Single Source of Truth." All data logic should live here (`GritLockRepository`).

### 2. Logic Layer (ViewModels in `ui/`)
*   ViewModels act as the bridge between the Data and the UI.
*   **HomeViewModel:** Logic for the home screen (Pledges, Urges, Challenges).
*   **LocksViewModel:** Logic for managing App Groups and Protocol Gates.
*   **UserViewModel:** Logic for XP, Leveling, and 12 RPG Attributes.

### 3. UI Layer (`ui/`)
*   All UI is built with **Jetpack Compose** and Material 3.
*   Try to keep logic out of the UI files. If you need to perform an action, call a function in the corresponding ViewModel.

## 🛠 Tech Stack
*   **Hilt:** Handles Dependency Injection.
*   **Room:** Local database (v44 migrations).
*   **Compose:** Modern Android UI toolkit.
*   **WorkManager & Accessibility Service:** Background monitoring and app blocking (`GritLockAccessibilityService`).
*   **ML Kit:** Pose detection and camera tracking.

## 🚀 How to Add a New Feature
1.  **Define the Data:** Add a new `@Entity` in `AppGroup.kt`.
2.  **Update the DAO:** Add a `@Query` or `@Insert` in `GritLockDao.kt`.
3.  **Update the Repository:** Add a method to `GritLockRepository.kt`.
4.  **Create/Update a ViewModel:** Expose the data as a `StateFlow`.
5.  **Build the UI:** Use `hiltViewModel()` to get data in your Composables.

Happy Coding! Stay Gritty.
