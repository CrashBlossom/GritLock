package com.example.fitlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlock.data.Challenge
import com.example.fitlock.data.DailyPledge
import com.example.fitlock.data.GritLockRepository
import com.example.fitlock.data.UrgeEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GritLockRepository
) : ViewModel() {

    val challenges: StateFlow<List<Challenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPledge: StateFlow<DailyPledge?> = repository.allPledges
        .map { list -> list.find { it.date == LocalDate.now().toString() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logUrge(event: UrgeEvent) {
        viewModelScope.launch {
            repository.insertUrgeEvent(event)
        }
    }

    fun updatePledge(pledge: DailyPledge) {
        viewModelScope.launch {
            repository.upsertPledge(pledge)
        }
    }
}
