package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algorithm.EbbinghausEngine
import com.example.data.ai.GeneratedDeckResult
import com.example.data.model.DeckEntity
import com.example.ui.components.BacklogBanner
import com.example.ui.theme.BadGradeColor
import com.example.ui.viewmodel.AiGenerationState
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    viewModel: MainViewModel,
    onStartStudy: (deckId: Long?, title: String) -> Unit,
    onOpenDeckDetail: (deckId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val decks by viewModel.decks.collectAsState()
    val filteredDecks by viewModel.filteredDecks.collectAsState()
    val selectedLevelFilter by viewModel.selectedLevelFilter.collectAsState()
    val todayQuota by viewModel.todayQuota.collectAsState()
    val isBacklogActive by viewModel.isBacklogActive.collectAsState()
    val overdueCardsCount by viewModel.overdueCardsCount.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showAiCreateDialog by remember { mutableStateOf(false) }
    var editingDeck by remember { mutableStateOf<DeckEntity?>(null) }
    var deletingDeck by remember { mutableStateOf<DeckEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hermann Memo",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Адаптивные интервальные повторения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiCreateDialog = true },
                        modifier = Modifier.testTag("ai_create_deck_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Создать тему через AI",
                            tint = MaterialTheme.colorScheme.primary
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
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_deck_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Создать колоду")
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshAllData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Cognitive Limits & Miller's Wallet Card
            item {
                MillerWalletHeader(
                    newCardsCount = todayQuota.newCardsCount,
                    maxNewCards = EbbinghausEngine.MAX_NEW_CARDS_PER_DAY,
                    overdueCount = overdueCardsCount
                )
            }

            // Backlog Banner if overdue
            if (overdueCardsCount > 0) {
                item {
                    BacklogBanner(
                        overdueCount = overdueCardsCount,
                        isBacklogActive = isBacklogActive
                    )
                }
            }

            // AI Topic Generation Banner (User Request 6)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAiCreateDialog = true }
                        .testTag("ai_generate_deck_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Создать тему с помощью AI",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Автогенерация атомарных фактов и мнемоник",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Global Practice Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onStartStudy(null, "Все колоды") }
                        .testTag("start_all_session_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Общая сессия повторений",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (overdueCardsCount > 0) {
                                    "К повторению: $overdueCardsCount карточек"
                                } else {
                                    "Повторите материал по кривой Эббингауза"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }

                        Button(
                            onClick = { onStartStudy(null, "Все колоды") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Учить")
                        }
                    }
                }
            }

            // Level Filter Chips (User Request 2 & 8: Basic / Advanced / Expert)
            item {
                Column {
                    Text(
                        text = "Уровень сложности тем:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "Все (${decks.size})",
                            "BASIC" to "Базовый (${decks.count { it.level.equals("BASIC", true) }})",
                            "ADVANCED" to "Специалист (${decks.count { it.level.equals("ADVANCED", true) }})",
                            "EXPERT" to "Профи (${decks.count { it.level.equals("EXPERT", true) }})"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = selectedLevelFilter == code,
                                onClick = { viewModel.setSelectedLevelFilter(code) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = "Темы для изучения (${filteredDecks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Decks List
            if (filteredDecks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "В этой категории пока нет тем",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredDecks, key = { it.id }) { deck ->
                    DeckItemCard(
                        deck = deck,
                        onStartStudy = { onStartStudy(deck.id, deck.title) },
                        onOpenDetail = { onOpenDeckDetail(deck.id) },
                        onEdit = { editingDeck = deck },
                        onDelete = { deletingDeck = deck }
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

    if (showCreateDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, category, level, colorHex, icon ->
                viewModel.createDeck(title, desc, category, colorHex, icon, level)
                showCreateDialog = false
            }
        )
    }

    if (showAiCreateDialog) {
        AiCreateDeckDialog(
            aiState = aiState,
            onDismiss = {
                viewModel.clearAiState()
                showAiCreateDialog = false
            },
            onGenerate = { prompt, level, count ->
                viewModel.generateDeckWithAi(prompt, level, count)
            },
            onSave = { result ->
                viewModel.saveGeneratedAiDeck(result)
                showAiCreateDialog = false
            }
        )
    }

    editingDeck?.let { deck ->
        EditDeckDialog(
            deck = deck,
            onDismiss = { editingDeck = null },
            onConfirm = { title, desc, cat, level ->
                viewModel.updateDeck(deck.copy(title = title, description = desc, category = cat, level = level))
                editingDeck = null
            }
        )
    }

    deletingDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { deletingDeck = null },
            title = {
                Text(
                    text = "Удалить тему?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Вы действительно хотите удалить тему «${deck.title}»? Все карточки темы будут удалены.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDeck(deck.id)
                        deletingDeck = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BadGradeColor)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDeck = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun MillerWalletHeader(
    newCardsCount: Int,
    maxNewCards: Int,
    overdueCount: Int
) {
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
                    text = "Когнитивный кошелек Миллера",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Сегодня",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$newCardsCount / $maxNewCards",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Новых карточек (лимит)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$overdueCount",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Ожидают повторения",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckItemCard(
    deck: DeckEntity,
    onStartStudy: () -> Unit,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val deckColor = try {
        Color(android.graphics.Color.parseColor(deck.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val (levelText, levelTextColor, levelBgColor) = getLevelBadge(deck.level)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenDetail() }
            .testTag("deck_item_${deck.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(deckColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getDeckIcon(deck.iconName),
                        contentDescription = null,
                        tint = deckColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = deck.category,
                            style = MaterialTheme.typography.labelSmall.copy(color = deckColor, fontWeight = FontWeight.SemiBold)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(levelBgColor)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = levelText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = levelTextColor
                            )
                        }
                    }
                }

                // Edit & Delete actions
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp).testTag("deck_edit_button_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать тему",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("deck_delete_button_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить тему",
                        tint = BadGradeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = deck.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenDetail,
                    modifier = Modifier.testTag("deck_cards_button_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Карточки темы")
                }

                Button(
                    onClick = onStartStudy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = deckColor),
                    modifier = Modifier.testTag("deck_study_button_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Учить")
                }
            }
        }
    }
}

@Composable
private fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, category: String, level: String, colorHex: String, icon: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Общее") }
    var level by remember { mutableStateOf("BASIC") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Создать новую тему",
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
                    modifier = Modifier.fillMaxWidth().testTag("deck_title_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("deck_category_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Краткое описание") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("deck_desc_input")
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
                        val isSelected = level == lvlCode
                        Surface(
                            selected = isSelected,
                            onClick = { level = lvlCode },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lvlName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), category.trim(), level, "#4F46E5", "school")
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_deck_button")
            ) {
                Text("Создать")
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
private fun AiCreateDeckDialog(
    aiState: AiGenerationState,
    onDismiss: () -> Unit,
    onGenerate: (prompt: String, level: String, count: Int) -> Unit,
    onSave: (GeneratedDeckResult) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("BASIC") }
    var cardCount by remember { mutableStateOf(8) }

    AlertDialog(
        onDismissRequest = {
            if (aiState !is AiGenerationState.Loading) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI-Генерация темы",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (aiState) {
                    is AiGenerationState.Idle -> {
                        Text(
                            text = "Введите предмет или область знаний. Искусственный интеллект разобьет материал на атомарные факты и подготовит мнемоники для кривой Эббингауза.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            label = { Text("О чем создать тему?") },
                            placeholder = { Text("Например: Корутины в Kotlin, Анатомия сердца, Неправильные глаголы") },
                            modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                            minLines = 2,
                            maxLines = 4
                        )

                        Text(
                            text = "Уровень сложности:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
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
                                val isSelected = level == lvlCode
                                Surface(
                                    selected = isSelected,
                                    onClick = { level = lvlCode },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lvlName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Количество карточек: $cardCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 8, 10, 15).forEach { count ->
                                val isSelected = cardCount == count
                                Surface(
                                    selected = isSelected,
                                    onClick = { cardCount = count },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$count шт.",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is AiGenerationState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "ИИ структурирует факты и создает карточки...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is AiGenerationState.Success -> {
                        val result = aiState.result
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Тема успешно сформирована!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = result.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Карточек в теме: ${result.cards.size} • Категория: ${result.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    is AiGenerationState.Error -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Ошибка генерации",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BadGradeColor
                            )
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Убедитесь, что в Настройках включен AI и указан корректный API-ключ Gemini / Groq / OpenAI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (aiState) {
                is AiGenerationState.Idle -> {
                    Button(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                onGenerate(prompt.trim(), level, cardCount)
                            }
                        },
                        enabled = prompt.isNotBlank(),
                        modifier = Modifier.testTag("ai_confirm_generate_button")
                    ) {
                        Text("Сгенерировать")
                    }
                }
                is AiGenerationState.Success -> {
                    Button(
                        onClick = { onSave(aiState.result) },
                        modifier = Modifier.testTag("ai_save_deck_button")
                    ) {
                        Text("Добавить в коллекцию")
                    }
                }
                is AiGenerationState.Error -> {
                    Button(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                onGenerate(prompt.trim(), level, cardCount)
                            }
                        }
                    ) {
                        Text("Повторить попытку")
                    }
                }
                is AiGenerationState.Loading -> { /* no-op */ }
            }
        },
        dismissButton = {
            if (aiState !is AiGenerationState.Loading) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        }
    )
}

fun getDeckIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "translate" -> Icons.Default.Translate
        "history" -> Icons.Default.History
        "calculate" -> Icons.Default.Calculate
        "biotech" -> Icons.Default.Biotech
        "public" -> Icons.Default.Public
        "code" -> Icons.Default.Code
        "science" -> Icons.Default.Science
        "psychology" -> Icons.Default.Psychology
        "telescope" -> Icons.Default.School
        "palette" -> Icons.Default.Palette
        else -> Icons.Default.School
    }
}
