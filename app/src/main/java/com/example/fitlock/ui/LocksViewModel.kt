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
}
