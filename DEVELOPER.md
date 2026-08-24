# GritLock Developer Guide

Welcome to the GritLock codebase! This app is designed with a **Clean Architecture (MVVM)** to make it easy to maintain and extend.

## 🏗 Architectural Layers

### 1. Data Layer (`data/`)
*   **Entities:** Define our database tables (e.g., `AppGroup`, `UserStats`).
*   **DAO (Data Access Object):** Contains the SQL queries to talk to the database.
*   **Repository:** The "Single Source of Truth." All data logic should live here. If you want to fetch data from a new source (like an API), add it to `GritLockRepository`.

### 2. Logic Layer (ViewModels in `ui/`)
*   ViewModels act as the bridge between the Data and the UI.
*   **HomeViewModel:** Logic for the home screen (Pledges, Urges).
*   **LocksViewModel:** Logic for managing App Groups and Gauntlets.
*   **UserViewModel:** Logic for XP, Leveling, and your Avatar.

### 3. UI Layer (`ui/`)
*   All UI is built with **Jetpack Compose**.
*   Try to keep logic out of the UI files. If you need to perform an action (like deleting a group), call a function in the corresponding ViewModel.

## 🛠 Tech Stack
*   **Hilt:** Handles "Dependency Injection." It automatically provides the database and repository to your ViewModels.
*   **Room:** Our local database.
*   **Compose:** Modern Android UI toolkit.
*   **WorkManager:** Handles background tasks like "Smart Nudges."

## 🚀 How to Add a New Feature
1.  **Define the Data:** Add a new `@Entity` in `AppGroup.kt`.
2.  **Update the DAO:** Add a `@Query` or `@Insert` in `GritLockDao.kt`.
3.  **Update the Repository:** Add a method to `GritLockRepository.kt`.
4.  **Create/Update a ViewModel:** Expose the data as a `StateFlow`.
5.  **Build the UI:** Use `hiltViewModel()` to get the data and show it in a Composable.

Happy Coding! Stay Gritty. 
