package com.example.fitlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlock.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocksViewModel @Inject constructor(
    private val repository: GritLockRepository
) : ViewModel() {

    val appGroups: StateFlow<List<AppGroup>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultItems: StateFlow<List<VaultItem>> = repository.allVaultItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gauntlets: StateFlow<List<GauntletWithHabits>> = repository.allGauntlets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val advancedWorkouts: StateFlow<List<GauntletWithAdvancedWorkout>> = repository.allAdvancedWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitDefinitions: StateFlow<List<HabitDefinition>> = repository.allHabitDefinitions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseDefinitions: StateFlow<List<ExerciseDefinition>> = repository.allExerciseDefinitions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskDefinitions: StateFlow<List<TaskDefinition>> = repository.allTaskDefinitions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleGroup(group: AppGroup) {
        viewModelScope.launch {
            repository.upsertGroup(group.copy(isEnabled = !group.isEnabled))
        }
    }

    fun deleteGroup(group: AppGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }

    fun upsertVaultItem(item: VaultItem) {
        viewModelScope.launch {
            repository.upsertVaultItem(item)
        }
    }

    fun deleteVaultItem(item: VaultItem) {
        viewModelScope.launch {
            repository.deleteVaultItem(item)
        }
    }

    fun upsertGroup(group: AppGroup) = viewModelScope.launch {
        repository.upsertGroup(group)
    }

    fun upsertGauntlet(gauntlet: Gauntlet) = viewModelScope.async {
        repository.upsertGauntlet(gauntlet)
    }

    fun deleteGauntlet(gauntlet: Gauntlet) = viewModelScope.launch {
        repository.deleteGauntlet(gauntlet)
    }

    fun deleteHabitsForGauntlet(gauntletId: Int) = viewModelScope.launch {
        repository.deleteHabitsForGauntlet(gauntletId)
    }

    fun upsertHabit(habit: Habit) = viewModelScope.launch {
        repository.upsertHabit(habit)
    }

    fun upsertHabitDefinition(definition: HabitDefinition) = viewModelScope.launch {
        repository.upsertHabitDefinition(definition)
    }

    fun deleteHabitDefinition(definition: HabitDefinition) = viewModelScope.launch {
        repository.deleteHabitDefinition(definition)
    }

    fun deleteBlocksForGauntlet(gauntletId: Int) = viewModelScope.launch {
        repository.deleteBlocksForGauntlet(gauntletId)
    }

    fun upsertWorkoutBlock(block: WorkoutBlockEntity) = viewModelScope.async {
        repository.upsertWorkoutBlock(block)
    }

    fun upsertWorkoutExercise(exercise: WorkoutExercise) = viewModelScope.launch {
        repository.upsertWorkoutExercise(exercise)
    }

    fun upsertExerciseDefinition(definition: ExerciseDefinition) = viewModelScope.launch {
        repository.upsertExerciseDefinition(definition)
    }

    fun deleteExerciseDefinition(definition: ExerciseDefinition) = viewModelScope.launch {
        repository.deleteExerciseDefinition(definition)
    }

    fun upsertTaskDefinition(definition: TaskDefinition) = viewModelScope.launch {
        repository.upsertTaskDefinition(definition)
    }

    fun deleteTaskDefinition(definition: TaskDefinition) = viewModelScope.launch {
        repository.deleteTaskDefinition(definition)
    }
}
