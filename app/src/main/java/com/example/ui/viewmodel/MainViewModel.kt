package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.algorithm.EbbinghausEngine
import com.example.data.db.AppDatabase
import com.example.data.model.CardStatus
import com.example.data.model.DailyQuotaEntity
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.ReviewGrade
import com.example.data.remote.AppReleaseInfo
import com.example.data.remote.UpdateCheckerService
import com.example.data.repository.StudyRepository
import com.example.data.repository.StudySessionQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    data class Success(val info: AppReleaseInfo) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

data class StudySessionState(
    val activeDeckId: Long? = null,
    val activeDeckTitle: String = "Все колоды",
    val queue: List<FlashcardEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val isHintVisible: Boolean = false,
    val isSessionFinished: Boolean = false,
    val reviewedCount: Int = 0,
    val isBacklogActive: Boolean = false,
    val totalOverdueCount: Int = 0,
    val isNewMaterialBlocked: Boolean = false,
    val isLoading: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = StudyRepository(db.deckDao(), db.flashcardDao(), db.dailyQuotaDao())

    val decks: StateFlow<List<DeckEntity>> = repository.allDecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCardsCount: StateFlow<Int> = repository.totalCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val newCardsCount: StateFlow<Int> = repository.newCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val learningCardsCount: StateFlow<Int> = repository.learningCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reviewCardsCount: StateFlow<Int> = repository.reviewCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val masteredCardsCount: StateFlow<Int> = repository.masteredCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _sessionState = MutableStateFlow(StudySessionState())
    val sessionState: StateFlow<StudySessionState> = _sessionState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _todayQuota = MutableStateFlow(DailyQuotaEntity(dateString = repository.getTodayDateString()))
    val todayQuota: StateFlow<DailyQuotaEntity> = _todayQuota.asStateFlow()

    private val _isBacklogActive = MutableStateFlow(false)
    val isBacklogActive: StateFlow<Boolean> = _isBacklogActive.asStateFlow()

    private val _overdueCardsCount = MutableStateFlow(0)
    val overdueCardsCount: StateFlow<Int> = _overdueCardsCount.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializePreloadIfEmpty()
            refreshQuotaAndBacklogStatus()
        }
    }

    fun refreshQuotaAndBacklogStatus() {
        viewModelScope.launch {
            val quota = repository.getTodayQuota()
            _todayQuota.value = quota
            val overdue = repository.getOverdueCount(null)
            _overdueCardsCount.value = overdue
            _isBacklogActive.value = repository.isBacklogActive(null)
        }
    }

    fun startStudySession(deckId: Long?, deckTitle: String = "Все колоды") {
        viewModelScope.launch {
            _sessionState.value = _sessionState.value.copy(isLoading = true)
            val sessionQueue = repository.prepareStudySession(deckId)
            _sessionState.value = StudySessionState(
                activeDeckId = deckId,
                activeDeckTitle = deckTitle,
                queue = sessionQueue.cards,
                currentIndex = 0,
                isAnswerRevealed = false,
                isHintVisible = false,
                isSessionFinished = sessionQueue.cards.isEmpty(),
                reviewedCount = 0,
                isBacklogActive = sessionQueue.isBacklogActive,
                totalOverdueCount = sessionQueue.totalOverdueCount,
                isNewMaterialBlocked = sessionQueue.isNewMaterialBlocked,
                isLoading = false
            )
            refreshQuotaAndBacklogStatus()
        }
    }

    fun revealAnswer() {
        _sessionState.value = _sessionState.value.copy(isAnswerRevealed = true)
    }

    fun toggleHint() {
        _sessionState.value = _sessionState.value.copy(isHintVisible = !_sessionState.value.isHintVisible)
    }

    fun submitCardRating(grade: ReviewGrade) {
        val currentCard = currentCard() ?: return
        viewModelScope.launch {
            repository.recordCardReview(currentCard, grade)
            val nextIndex = _sessionState.value.currentIndex + 1
            val isFinished = nextIndex >= _sessionState.value.queue.size

            _sessionState.value = _sessionState.value.copy(
                currentIndex = nextIndex,
                isAnswerRevealed = false,
                isHintVisible = false,
                isSessionFinished = isFinished,
                reviewedCount = _sessionState.value.reviewedCount + 1
            )
            refreshQuotaAndBacklogStatus()
        }
    }

    fun currentCard(): FlashcardEntity? {
        val state = _sessionState.value
        return state.queue.getOrNull(state.currentIndex)
    }

    fun createDeck(title: String, description: String, category: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.createDeck(title, description, category, colorHex, iconName)
        }
    }

    fun updateDeck(deck: DeckEntity) {
        viewModelScope.launch {
            repository.updateDeck(deck)
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
        }
    }

    fun createCard(deckId: Long, question: String, answer: String, hint: String) {
        viewModelScope.launch {
            repository.createCard(deckId, question, answer, hint)
        }
    }

    fun updateCard(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.updateCard(card)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    fun resetDemoDecks() {
        viewModelScope.launch {
            repository.resetDemoDecks()
            refreshQuotaAndBacklogStatus()
        }
    }

    fun checkForUpdates(repoPath: String = UpdateCheckerService.DEFAULT_REPO_PATH) {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            val result = UpdateCheckerService.checkLatestRelease(repoPath)
            result.onSuccess { info ->
                _updateState.value = UpdateUiState.Success(info)
            }.onFailure { error ->
                _updateState.value = UpdateUiState.Error(error.localizedMessage ?: "Ошибка при проверке обновлений")
            }
        }
    }
}
