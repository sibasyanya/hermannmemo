package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algorithm.EbbinghausEngine
import com.example.data.local.AnswerCheckMode
import com.example.data.model.ReviewGrade
import com.example.ui.components.BacklogBanner
import com.example.ui.theme.BadGradeColor
import com.example.ui.theme.BadGradeContainer
import com.example.ui.theme.ExcellentGradeColor
import com.example.ui.theme.ExcellentGradeContainer
import com.example.ui.theme.NormalGradeColor
import com.example.ui.theme.NormalGradeContainer
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val currentCard = remember(sessionState.queue, sessionState.currentIndex) {
        sessionState.queue.getOrNull(sessionState.currentIndex)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sessionState.activeDeckTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (sessionState.queue.isNotEmpty()) {
                            Text(
                                text = "Карточка ${sessionState.currentIndex + 1} из ${sessionState.queue.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("session_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Linear Progress Indicator
            if (sessionState.queue.isNotEmpty()) {
                val progress = if (sessionState.queue.isNotEmpty()) {
                    sessionState.currentIndex.toFloat() / sessionState.queue.size.toFloat()
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (sessionState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (sessionState.isSessionFinished || sessionState.queue.isEmpty()) {
                // Finished or empty state
                SessionCompleteView(
                    reviewedCount = sessionState.reviewedCount,
                    isBacklogActive = sessionState.isBacklogActive,
                    onBackToDecks = onNavigateBack
                )
            } else if (currentCard != null) {
                // Active flashcard view
                val scrollState = rememberScrollState()
                LaunchedEffect(sessionState.currentIndex) {
                    scrollState.scrollTo(0)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        if (sessionState.isBacklogActive) {
                            BacklogBanner(
                                overdueCount = sessionState.totalOverdueCount,
                                isBacklogActive = true,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Atomic Flashcard Card with AnimatedContent
                        AnimatedContent(
                            targetState = currentCard,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                                    .togetherWith(fadeOut(animationSpec = tween(100)))
                            },
                            label = "card_progression_animation"
                        ) { card ->
                            key(card.id, sessionState.currentIndex) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .testTag("flashcard_container"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp)
                                    ) {
                                        // Tag bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Атомарный факт",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            if (card.isHeavy) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(BadGradeContainer)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Тяжелая карточка",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = BadGradeColor
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Question
                                        Text(
                                            text = "Вопрос:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = card.question,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 30.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("card_question_text")
                                        )

                                        // Optional Hint
                                        if (card.hint.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            if (!sessionState.isHintVisible) {
                                                OutlinedButton(
                                                    onClick = { viewModel.toggleHint() },
                                                    modifier = Modifier.testTag("toggle_hint_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lightbulb,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Показать мнемонику / подсказку")
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .padding(12.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lightbulb,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = card.hint,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Answer section
                                        Spacer(modifier = Modifier.height(20.dp))

                                        if (!sessionState.isAnswerRevealed) {
                                            // Input field for testing knowledge
                                            Text(
                                                text = "Ваш ответ (для проверки знания):",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = sessionState.userTypedAnswer,
                                                onValueChange = { viewModel.setUserTypedAnswer(it) },
                                                placeholder = { Text("Введите ответ или нажмите «Показать ответ»...") },
                                                modifier = Modifier.fillMaxWidth().testTag("user_answer_input"),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = false,
                                                maxLines = 3
                                            )
                                        } else {
                                            // Answer Revealed UI
                                            if (sessionState.hasCheckedAnswer) {
                                                if (sessionState.isAnswerCorrect) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(ExcellentGradeContainer)
                                                            .border(1.dp, ExcellentGradeColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                            .padding(14.dp)
                                                    ) {
                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = ExcellentGradeColor, modifier = Modifier.size(20.dp))
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "Верно!",
                                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = ExcellentGradeColor
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = sessionState.userTypedAnswer,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    // Incorrect user answer shown with strikethrough (User Request 1)
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(BadGradeContainer)
                                                            .border(1.dp, BadGradeColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                            .padding(14.dp)
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "Ваш ответ (неверно):",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = BadGradeColor
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = sessionState.userTypedAnswer.ifBlank { "(пустой ввод)" },
                                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                                    textDecoration = TextDecoration.LineThrough,
                                                                    fontWeight = FontWeight.SemiBold
                                                                ),
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }

                                            // Correct Answer Box
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                                    .padding(16.dp)
                                                    .testTag("revealed_answer_box")
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Правильный ответ:",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = card.answer,
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            lineHeight = 28.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Controls Area
                    if (!sessionState.isAnswerRevealed) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (sessionState.userTypedAnswer.isNotBlank()) {
                                Button(
                                    onClick = { viewModel.checkTypedAnswer() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("check_answer_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Проверить мой ответ",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.revealAnswer() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("reveal_answer_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = if (sessionState.userTypedAnswer.isBlank()) {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                } else {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Показать ответ",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Mastered Action: User already knows this card (User Request 4)
                            TextButton(
                                onClick = { viewModel.markCurrentCardMastered() },
                                modifier = Modifier.fillMaxWidth().testTag("mark_mastered_direct_button")
                            ) {
                                Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Я уже знаю эту карточку (Освоено)")
                            }
                        }
                    } else {
                        // When answer is revealed
                        if (sessionState.autoResetToastMessage != null) {
                            // Auto reset active banner (User Request 1)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(BadGradeContainer)
                                        .border(1.dp, BadGradeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = sessionState.autoResetToastMessage!!,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = BadGradeColor
                                    )
                                }

                                Button(
                                    onClick = { viewModel.advanceToNextCardAfterAutoReset() },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Следующая карточка →", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        } else {
                            // Rating buttons (Manual mode or correct answer)
                            Column {
                                Text(
                                    text = "Оцените сложность воспоминания:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // BAD / HEAVY
                                    RatingButton(
                                        label = "Тяжелая",
                                        interval = EbbinghausEngine.getIntervalPreview(currentCard, ReviewGrade.BAD),
                                        accentColor = BadGradeColor,
                                        containerColor = BadGradeContainer,
                                        modifier = Modifier.weight(1f).testTag("grade_bad_button"),
                                        onClick = { viewModel.submitCardRating(ReviewGrade.BAD) }
                                    )

                                    // NORMAL
                                    RatingButton(
                                        label = "Нормально",
                                        interval = EbbinghausEngine.getIntervalPreview(currentCard, ReviewGrade.NORMAL),
                                        accentColor = NormalGradeColor,
                                        containerColor = NormalGradeContainer,
                                        modifier = Modifier.weight(1f).testTag("grade_normal_button"),
                                        onClick = { viewModel.submitCardRating(ReviewGrade.NORMAL) }
                                    )

                                    // EXCELLENT
                                    RatingButton(
                                        label = "Отлично",
                                        interval = EbbinghausEngine.getIntervalPreview(currentCard, ReviewGrade.EXCELLENT),
                                        accentColor = ExcellentGradeColor,
                                        containerColor = ExcellentGradeContainer,
                                        modifier = Modifier.weight(1f).testTag("grade_excellent_button"),
                                        onClick = { viewModel.submitCardRating(ReviewGrade.EXCELLENT) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Mastered button: instant mastery without repeats
                                TextButton(
                                    onClick = { viewModel.markCurrentCardMastered() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Знаю назубок (Отметить как освоенную)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    interval: String,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = accentColor
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = accentColor,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = interval,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = accentColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SessionCompleteView(
    reviewedCount: Int,
    isBacklogActive: Boolean,
    onBackToDecks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ExcellentGradeContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                tint = ExcellentGradeColor,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Сессия завершена!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (reviewedCount > 0) {
                "Вы успешно повторили $reviewedCount карточек. Согласно кривой забывания Эббингауза, материал надежно зафиксирован в долговременной памяти."
            } else {
                "На данный момент нет карточек, требующих немедленного повторения. Все повторения выполнены вовремя!"
            },
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Next steps guidance card (CTA)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("session_next_steps_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Что делать дальше?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isBacklogActive) {
                    Text(
                        text = "Включен режим ликвидации завала. Чтобы избежать утомления, следующая дозированная порция повторений станет доступна завтра.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Отдохните и возвращайтесь завтра! Алгоритм рассчитывает оптимальный интервал и предложит карточки ровно тогда, когда это нужно вашему мозгу.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Следующая сессия: завтра в запланированное время",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Primary Action: Return to Decks
        Button(
            onClick = onBackToDecks,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("session_finish_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Вернуться к колодам",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
