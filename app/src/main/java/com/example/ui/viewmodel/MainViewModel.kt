package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.algorithm.EbbinghausEngine
import com.example.data.ai.AiCardGeneratorService
import com.example.data.ai.GeneratedCard
import com.example.data.ai.GeneratedDeckResult
import com.example.data.db.AppDatabase
import com.example.data.local.AnswerCheckMode
import com.example.data.local.AppSettingsManager
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    data class Success(val info: AppReleaseInfo) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

sealed interface AiGenerationState {
    object Idle : AiGenerationState
    object Loading : AiGenerationState
    data class Success(val result: GeneratedDeckResult) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
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
    val isLoading: Boolean = false,
    val userTypedAnswer: String = "",
    val hasCheckedAnswer: Boolean = false,
    val isAnswerCorrect: Boolean = false,
    val autoResetToastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = StudyRepository(db.deckDao(), db.flashcardDao(), db.dailyQuotaDao())
    val settingsManager = AppSettingsManager.getInstance(application)
    val aiService = AiCardGeneratorService(settingsManager)

    val decks: StateFlow<List<DeckEntity>> = repository.allDecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLevelFilter = MutableStateFlow("ALL") // "ALL", "BASIC", "ADVANCED", "EXPERT"
    val selectedLevelFilter: StateFlow<String> = _selectedLevelFilter.asStateFlow()

    val filteredDecks: StateFlow<List<DeckEntity>> = kotlinx.coroutines.flow.combine(decks, selectedLevelFilter) { all, filter ->
        if (filter == "ALL") all else all.filter { it.level.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _aiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiState: StateFlow<AiGenerationState> = _aiState.asStateFlow()

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

    fun setSelectedLevelFilter(level: String) {
        _selectedLevelFilter.value = level
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
            val sessionQueue = repository.prepareStudySession(
                deckId = deckId,
                strictBacklogBlocking = settingsManager.strictBacklogBlocking.value
            )
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
                isLoading = false,
                userTypedAnswer = "",
                hasCheckedAnswer = false,
                isAnswerCorrect = false,
                autoResetToastMessage = null
            )
            refreshQuotaAndBacklogStatus()
        }
    }

    fun setUserTypedAnswer(text: String) {
        _sessionState.update { it.copy(userTypedAnswer = text) }
    }

    fun revealAnswer() {
        _sessionState.value = _sessionState.value.copy(isAnswerRevealed = true)
    }

    fun checkTypedAnswer() {
        val state = _sessionState.value
        val card = state.queue.getOrNull(state.currentIndex) ?: return
        val typed = state.userTypedAnswer.trim().lowercase()
        val expected = card.answer.trim().lowercase()

        // Robust matching: exact or clean normalized match
        val cleanTyped = typed.replace(Regex("[^a-zA-Zа-яА-Я0-9]"), "")
        val cleanExpected = expected.replace(Regex("[^a-zA-Zа-яА-Я0-9]"), "")
        val isCorrect = cleanTyped.isNotEmpty() && (cleanTyped == cleanExpected || expected.contains(typed))

        val mode = settingsManager.answerCheckMode.value

        if (mode == AnswerCheckMode.AUTO_RESET && !isCorrect) {
            // Auto reset mode: immediately reset card to Ebbinghaus repetition (BAD rating)
            viewModelScope.launch {
                repository.recordCardReview(card, ReviewGrade.BAD)
                _sessionState.update { current ->
                    current.copy(
                        isAnswerRevealed = true,
                        hasCheckedAnswer = true,
                        isAnswerCorrect = false,
                        autoResetToastMessage = "Ответ неверен. Карточка автоматически сброшена в повторение по Эббингаузу (15 мин)."
                    )
                }
                refreshQuotaAndBacklogStatus()
            }
        } else {
            // Manual mode or answer is correct: reveal answer, user sees comparison
            _sessionState.update {
                it.copy(
                    isAnswerRevealed = true,
                    hasCheckedAnswer = true,
                    isAnswerCorrect = isCorrect,
                    autoResetToastMessage = null
                )
            }
        }
    }

    fun advanceToNextCardAfterAutoReset() {
        _sessionState.update { current ->
            val nextIndex = current.currentIndex + 1
            current.copy(
                currentIndex = nextIndex,
                isAnswerRevealed = false,
                isHintVisible = false,
                isSessionFinished = nextIndex >= current.queue.size,
                reviewedCount = current.reviewedCount + 1,
                userTypedAnswer = "",
                hasCheckedAnswer = false,
                isAnswerCorrect = false,
                autoResetToastMessage = null
            )
        }
        refreshQuotaAndBacklogStatus()
    }

    fun markCurrentCardMastered() {
        val state = _sessionState.value
        val card = state.queue.getOrNull(state.currentIndex) ?: return
        viewModelScope.launch {
            repository.markCardAsMastered(card)
            _sessionState.update { current ->
                val nextIndex = current.currentIndex + 1
                current.copy(
                    currentIndex = nextIndex,
                    isAnswerRevealed = false,
                    isHintVisible = false,
                    isSessionFinished = nextIndex >= current.queue.size,
                    reviewedCount = current.reviewedCount + 1,
                    userTypedAnswer = "",
                    hasCheckedAnswer = false,
                    isAnswerCorrect = false,
                    autoResetToastMessage = null
                )
            }
            refreshQuotaAndBacklogStatus()
        }
    }

    fun toggleHint() {
        _sessionState.value = _sessionState.value.copy(isHintVisible = !_sessionState.value.isHintVisible)
    }

    fun submitCardRating(grade: ReviewGrade) {
        val state = _sessionState.value
        val cardToReview = state.queue.getOrNull(state.currentIndex) ?: return
        viewModelScope.launch {
            repository.recordCardReview(cardToReview, grade)
            _sessionState.update { current ->
                val nextIndex = current.currentIndex + 1
                current.copy(
                    currentIndex = nextIndex,
                    isAnswerRevealed = false,
                    isHintVisible = false,
                    isSessionFinished = nextIndex >= current.queue.size,
                    reviewedCount = current.reviewedCount + 1,
                    userTypedAnswer = "",
                    hasCheckedAnswer = false,
                    isAnswerCorrect = false,
                    autoResetToastMessage = null
                )
            }
            refreshQuotaAndBacklogStatus()
        }
    }

    fun currentCard(): FlashcardEntity? {
        val state = _sessionState.value
        return state.queue.getOrNull(state.currentIndex)
    }

    fun createDeck(
        title: String,
        description: String,
        category: String,
        level: String = "BASIC",
        colorHex: String,
        iconName: String
    ) {
        viewModelScope.launch {
            repository.createDeck(title, description, category, level, colorHex, iconName)
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

    fun markCardAsMastered(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.markCardAsMastered(card)
            refreshQuotaAndBacklogStatus()
        }
    }

    fun markCardMastered(cardId: Long) {
        viewModelScope.launch {
            val card = repository.getCardById(cardId) ?: return@launch
            repository.markCardAsMastered(card)
            refreshQuotaAndBacklogStatus()
        }
    }

    fun resetDemoDecks() {
        viewModelScope.launch {
            repository.resetDemoDecks()
            refreshQuotaAndBacklogStatus()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshAllData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshQuotaAndBacklogStatus()
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }

    suspend fun testAiConnection(): Result<String> {
        return aiService.testConnection()
    }

    suspend fun generateCardAssistance(topic: String, question: String, existingAnswer: String): Result<GeneratedCard> {
        return aiService.generateCardAssistance(topic, question, existingAnswer)
    }

    fun addGeneratedCardsToExistingDeck(deckId: Long, cards: List<GeneratedCard>) {
        viewModelScope.launch {
            val entities = cards.map {
                FlashcardEntity(
                    deckId = deckId,
                    question = it.question,
                    answer = it.answer,
                    hint = it.hint,
                    status = CardStatus.NEW
                )
            }
            repository.createCards(entities)
            _aiState.value = AiGenerationState.Idle
            refreshQuotaAndBacklogStatus()
        }
    }

    fun generateDeckWithAi(prompt: String, level: String, count: Int = 8) {
        viewModelScope.launch {
            _aiState.value = AiGenerationState.Loading
            val result = aiService.generateCardsForTopic(prompt, level, count)
            result.onSuccess { deckResult ->
                _aiState.value = AiGenerationState.Success(deckResult)
            }.onFailure { error ->
                _aiState.value = AiGenerationState.Error(error.localizedMessage ?: "Ошибка генерации")
            }
        }
    }

    fun clearAiState() {
        _aiState.value = AiGenerationState.Idle
    }

    fun saveGeneratedAiDeck(
        deckResult: GeneratedDeckResult,
        colorHex: String = "#8B5CF6",
        iconName: String = "psychology"
    ) {
        viewModelScope.launch {
            val deckId = repository.createDeck(
                title = deckResult.title,
                description = deckResult.description,
                category = deckResult.category,
                level = deckResult.level,
                colorHex = colorHex,
                iconName = iconName
            )
            val cards = deckResult.cards.map {
                FlashcardEntity(
                    deckId = deckId,
                    question = it.question,
                    answer = it.answer,
                    hint = it.hint,
                    status = CardStatus.NEW
                )
            }
            repository.createCards(cards)
            _aiState.value = AiGenerationState.Idle
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
