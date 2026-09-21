package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algorithm.EbbinghausEngine
import com.example.data.model.CardStatus
import com.example.data.model.DeckEntity
import com.example.data.model.FlashcardEntity
import com.example.ui.theme.BadGradeColor
import com.example.ui.theme.BadGradeContainer
import com.example.ui.theme.ExcellentGradeColor
import com.example.ui.theme.ExcellentGradeContainer
import com.example.ui.theme.NormalGradeColor
import com.example.ui.theme.NormalGradeContainer
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onStartStudy: (deckId: Long, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val deckFlow = remember(deckId) { viewModel.repository.getDeckById(deckId) }
    val deck by deckFlow.collectAsState(initial = null)

    val cardsFlow = remember(deckId) { viewModel.repository.getCardsForDeck(deckId) }
    val cards by cardsFlow.collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf<CardStatus?>(null) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showEditDeckDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<FlashcardEntity?>(null) }

    val filteredCards = if (selectedFilter == null) {
        cards
    } else {
        cards.filter { it.status == selectedFilter }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = deck?.title ?: "Тема",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            deck?.let { onStartStudy(it.id, it.title) }
                        },
                        modifier = Modifier.testTag("study_from_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Учить тему",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showEditDeckDialog = true },
                        modifier = Modifier.testTag("edit_deck_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать тему"
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("delete_deck_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить тему",
                            tint = BadGradeColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCardDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_card_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить карточку")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Deck Overview Info with Level
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = deck?.category ?: "",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val (levelLabel, levelTextCol, levelBgCol) = getLevelBadge(deck?.level ?: "BASIC")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(levelBgCol)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = levelLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = levelTextCol
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = deck?.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoBadge("Всего карточек", "${cards.size}")
                            InfoBadge("Новых", "${cards.count { it.status == CardStatus.NEW }}")
                            InfoBadge("В архиве", "${cards.count { it.status == CardStatus.MASTERED }}")
                        }
                    }
                }
            }

            // Progress Forecast Card (User Request 3)
            item {
                val forecast = remember(cards) { EbbinghausEngine.calculateProgressForecast(cards) }
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("deck_progress_forecast_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Прогноз изучения темы",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${forecast.progressPercentage}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { forecast.progressPercentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = forecast.forecastDescription,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Новые: ${forecast.newCards}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("В процессе: ${forecast.learningCards}", style = MaterialTheme.typography.labelSmall, color = NormalGradeColor)
                            Text("Повторение: ${forecast.reviewCards}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Освоено: ${forecast.masteredCards}", style = MaterialTheme.typography.labelSmall, color = ExcellentGradeColor)
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Все (${cards.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == CardStatus.NEW,
                        onClick = { selectedFilter = CardStatus.NEW },
                        label = { Text("Новые") }
                    )
                    FilterChip(
                        selected = selectedFilter == CardStatus.REVIEW,
                        onClick = { selectedFilter = CardStatus.REVIEW },
                        label = { Text("Повторение") }
                    )
                    FilterChip(
                        selected = selectedFilter == CardStatus.MASTERED,
                        onClick = { selectedFilter = CardStatus.MASTERED },
                        label = { Text("В архиве") }
                    )
                }
            }

            // Cards List
            if (filteredCards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "В этой категории пока нет карточек",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredCards, key = { it.id }) { card ->
                    CardListItem(
                        card = card,
                        onEdit = { editingCard = card },
                        onDelete = { viewModel.deleteCard(card.id) },
                        onMarkMastered = { viewModel.markCardAsMastered(card) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddCardDialog) {
        CardEditDialog(
            title = "Добавить атомарную карточку",
            initialQuestion = "",
            initialAnswer = "",
            initialHint = "",
            onDismiss = { showAddCardDialog = false },
            onConfirm = { q, a, h ->
                viewModel.createCard(deckId, q, a, h)
                showAddCardDialog = false
            }
        )
    }

    editingCard?.let { card ->
        CardEditDialog(
            title = "Редактировать карточку",
            initialQuestion = card.question,
            initialAnswer = card.answer,
            initialHint = card.hint,
            onDismiss = { editingCard = null },
            onConfirm = { q, a, h ->
                viewModel.updateCard(card.copy(question = q, answer = a, hint = h))
                editingCard = null
            }
        )
    }

    val currentDeck = deck
    if (showEditDeckDialog && currentDeck != null) {
        EditDeckDialog(
            deck = currentDeck,
            onDismiss = { showEditDeckDialog = false },
            onConfirm = { title, desc, cat, level ->
                viewModel.updateDeck(currentDeck.copy(title = title, description = desc, category = cat, level = level))
                showEditDeckDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog && currentDeck != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Удалить тему?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Вы действительно хотите удалить тему «${currentDeck.title}» и все входящие в неё карточки (${cards.size} шт.)? Это действие нельзя отменить.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDeck(deckId)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BadGradeColor)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

fun getLevelBadge(level: String): Triple<String, Color, Color> {
    return when (level.uppercase()) {
        "EXPERT" -> Triple("Эксперт (Профи)", Color(0xFF6B21A8), Color(0xFFF3E8FF))
        "ADVANCED" -> Triple("Продвинутый (Специалист)", Color(0xFFC2410C), Color(0xFFFFEDD5))
        else -> Triple("Элементарный (Базовый)", Color(0xFF0369A1), Color(0xFFE0F2FE))
    }
}

@Composable
fun EditDeckDialog(
    deck: DeckEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, category: String, level: String) -> Unit
) {
    var title by remember { mutableStateOf(deck.title) }
    var description by remember { mutableStateOf(deck.description) }
    var category by remember { mutableStateOf(deck.category) }
    var selectedLevel by remember { mutableStateOf(deck.level.ifBlank { "BASIC" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Редактировать тему",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название темы") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание темы") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Уровень сложности:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "BASIC" to "Базовый",
                        "ADVANCED" to "Специалист",
                        "EXPERT" to "Профи"
                    ).forEach { (lvlCode, lvlName) ->
                        FilterChip(
                            selected = selectedLevel.equals(lvlCode, ignoreCase = true),
                            onClick = { selectedLevel = lvlCode },
                            label = { Text(lvlName, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), category.trim(), selectedLevel)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun InfoBadge(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CardListItem(
    card: FlashcardEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkMastered: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(card.status)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMarkMastered,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Отметить как освоенную",
                            tint = if (card.status == CardStatus.MASTERED) ExcellentGradeColor else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = BadGradeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = card.question,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = card.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (card.hint.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Подсказка: ${card.hint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: CardStatus) {
    val (bgColor, textColor, text) = when (status) {
        CardStatus.NEW -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Новая")
        CardStatus.LEARNING -> Triple(NormalGradeContainer, NormalGradeColor, "В процессе")
        CardStatus.REVIEW -> Triple(NormalGradeContainer, NormalGradeColor, "Повторение")
        CardStatus.MASTERED -> Triple(ExcellentGradeContainer, ExcellentGradeColor, "В архиве (90+ дн)")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
private fun CardEditDialog(
    title: String,
    initialQuestion: String,
    initialAnswer: String,
    initialHint: String,
    onDismiss: () -> Unit,
    onConfirm: (question: String, answer: String, hint: String) -> Unit
) {
    var question by remember { mutableStateOf(initialQuestion) }
    var answer by remember { mutableStateOf(initialAnswer) }
    var hint by remember { mutableStateOf(initialHint) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Вопрос (узкий, атомарный факт)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("card_question_input")
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Точный конкретный ответ") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("card_answer_input")
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Мнемоника или подсказка (необязательно)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("card_hint_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && answer.isNotBlank()) {
                        onConfirm(question.trim(), answer.trim(), hint.trim())
                    }
                },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                modifier = Modifier.testTag("save_card_button")
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
