package com.example.fitlock.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlock.data.AnkiImporter
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

data class DeckSummary(
    val name: String,
    val cardsDue: Int,
    val totalCards: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GritLockRepository
) : ViewModel() {

    val challenges: StateFlow<List<Challenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuests = repository.allQuests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unfinishedTasks = repository.unfinishedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPledge: StateFlow<DailyPledge?> = repository.allPledges
        .map { list -> list.find { it.date == LocalDate.now().toString() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun initDailyPledgeIfMissing() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = repository.getPledgeForDate(today)
            if (existing == null) {
                repository.upsertPledge(DailyPledge(date = today, status = "PENDING"))
            }
        }
    }

    val deckSummaries: StateFlow<List<DeckSummary>> = repository.allFlashcards
        .map { cards ->
            val now = System.currentTimeMillis() + 300000 // 5 min buffer to ensure immediate visibility
            cards.groupBy { it.deckName }
                .map { (name, cardsInDeck) ->
                    DeckSummary(
                        name = name,
                        cardsDue = cardsInDeck.count { it.nextReviewDate <= now },
                        totalCards = cardsInDeck.size
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun importAnkiDeck(context: Context, uri: Uri) {
        viewModelScope.launch {
            val importer = AnkiImporter(context, repository)
            importer.import(uri)
        }
    }

    fun deleteDeck(deckName: String) {
        viewModelScope.launch {
            repository.deleteFlashcardsByDeck(deckName)
        }
    }
}
