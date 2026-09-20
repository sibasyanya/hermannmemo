package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BadGradeColor
import com.example.ui.theme.BadGradeContainer
import com.example.ui.theme.ExcellentGradeColor
import com.example.ui.theme.ExcellentGradeContainer
import com.example.ui.theme.NormalGradeColor
import com.example.ui.theme.NormalGradeContainer

data class InstructionSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tagText: String,
    val tagColor: Color,
    val tagContainer: Color,
    val paragraphs: List<String>,
    val bulletPoints: List<Pair<String, String>> = emptyList(),
    val practicalTip: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(
    modifier: Modifier = Modifier
) {
    // All cards are collapsed by default per user request
    val expandedStates = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer

    val sections = remember(primaryColor, primaryContainer, tertiaryColor, tertiaryContainer) {
        listOf(
            InstructionSection(
                id = "ebbinghaus",
                title = "Кривая забывания Эббингауза",
                subtitle = "Фундамент интервальных повторений",
                icon = Icons.Default.Timeline,
                tagText = "Научная база",
                tagColor = primaryColor,
                tagContainer = primaryContainer,
                paragraphs = listOf(
                    "Немецкий психолог Герман Эббингауз экспериментально доказал, что человеческая память угасает экспоненциально: до 60–70% новой информации теряется уже в первые 24 часа после ознакомления, если не было активного припоминания.",
                    "Hermann Memo рассчитывает точный момент, когда факт вот-вот сотрется из памяти, и предлагает его для повторения. Каждое своевременное повторение обращает кривую вспять, увеличивая интервал сохранения (1 день → 3 дня → 7 дней → 16 дней → 35+ дней)."
                ),
                bulletPoints = listOf(
                    "Активное припоминание" to "Пассивное перечитывание не формирует синаптических связей. Мозг запоминает только то, что сам достал из глубин памяти.",
                    "Удлинение интервалов" to "Чем стабильнее воспоминание, тем реже оно требует проверки, экономя часы вашего времени."
                ),
                practicalTip = "Никогда не подсматривайте ответ до попытки припомнить его самостоятельно хотя бы 3-5 секунд."
            ),
            InstructionSection(
                id = "miller",
                title = "Кошелек Миллера (4-7 единиц)",
                subtitle = "Ограничения оперативной памяти",
                icon = Icons.Default.Psychology,
                tagText = "Когнитивная физиология",
                tagColor = NormalGradeColor,
                tagContainer = NormalGradeContainer,
                paragraphs = listOf(
                    "Американский когнитивный психолог Джордж Миллер сформулировал фундаментальное правило: объем кратковременной памяти ограничен 7 ± 2 элементами, а по современным уточненным данным — всего 4–5 смысловыми блоками.",
                    "Попытки выучить длинные списки или громоздкие абзацы перегружают когнитивный буфер, вызывая быстрое умственное утомление и забывание."
                ),
                bulletPoints = listOf(
                    "Чанкинг (блочное мышление)" to "Разделяйте сложные дисциплины на маленькие автономные фрагменты.",
                    "Микро-сессии" to "Сессия из 15-20 карточек усваивается значительно лучше часовой зубрежки."
                ),
                practicalTip = "Если вопрос кажется слишком длинным — разбейте его на две отдельные независимые карточки."
            ),
            InstructionSection(
                id = "atomic_cards",
                title = "Правило атомарных карточек",
                subtitle = "Один факт = одна карточка",
                icon = Icons.Default.AutoAwesome,
                tagText = "Золотой стандарт",
                tagColor = ExcellentGradeColor,
                tagContainer = ExcellentGradeContainer,
                paragraphs = listOf(
                    "Атомарность — главное условие долгосрочного успеха. Карточка должна проверять один неделимый факт с однозначным ответом.",
                    "Если на карточке задан вопрос «Назовите 5 признаков депрессии», вы будете помнить 3 из них, забывать 2, и не сможете объективно оценить свой результат."
                ),
                bulletPoints = listOf(
                    "Однозначность" to "У вопроса должен быть ровно один понятный и четкий ответ без двояких толкований.",
                    "Контекстные подсказки" to "Используйте поле мнемоники для ярких ассоциаций, метафор или аббревиатур, помогающих припоминанию."
                ),
                practicalTip = "Замените перечисления на карточки с контекстными вопросами вида: «Какой признак X проявляется при Y?»"
            ),
            InstructionSection(
                id = "daily_limit",
                title = "Правило 20 новых карточек",
                subtitle = "Защита от информационной лавины",
                icon = Icons.Default.LockClock,
                tagText = "Дневной лимит",
                tagColor = tertiaryColor,
                tagContainer = tertiaryContainer,
                paragraphs = listOf(
                    "В приложении действует строгое ограничение: не более 20 новых карточек в сутки.",
                    "Почему это критично? Каждая новая карточка порождает серию будущих повторений через 1, 3, 7, 14 дней. Если выучить 100 карточек за день, через неделю вас накроет лавина из 300+ карточек, что приведет к неизбежному выгоранию и забрасыванию учебы."
                ),
                bulletPoints = listOf(
                    "Привычка вместо спринта" to "20 новых карточек в день — это 600 новых прочно усвоенных знаний в месяц без стресса.",
                    "Предсказуемая нагрузка" to "Ежедневная сессия занимает стабильные 5–10 минут."
                )
            ),
            InstructionSection(
                id = "backlog_defense",
                title = "Защита от завала (Backlog Defense)",
                subtitle = "Умный разбор пропущенных дней",
                icon = Icons.Default.Security,
                tagText = "Системная защита",
                tagColor = BadGradeColor,
                tagContainer = BadGradeContainer,
                paragraphs = listOf(
                    "Пропустили несколько дней и накопилось много карточек? Hermann Memo автоматически активирует режим защиты от завала (Backlog Defense).",
                    "В этом режиме приложение блокирует изучение новых карточек до тех пор, пока вы не разберете накопившиеся просроченные долги. Долг выдается комфортными порциями, разделяя карточки на критические и текущие."
                ),
                bulletPoints = listOf(
                    "Приоритет долговременной памяти" to "Сохранить уже выученное в 5 раз важнее, чем начать учить новое и забыть старое.",
                    "Без чувства вины" to "Система не наказывает за пропуски, а бережно подстраивает график под ваш текущий темп."
                )
            ),
            InstructionSection(
                id = "heavy_cards",
                title = "«Тяжелые карточки» (Leech Detection)",
                subtitle = "Автоматическое выявление проблемных зон",
                icon = Icons.Default.FitnessCenter,
                tagText = "Диагностика",
                tagColor = BadGradeColor,
                tagContainer = BadGradeContainer,
                paragraphs = listOf(
                    "Если вы нажимаете оценку «Плохо» по одной карточке 3 и более раз подряд, карточка автоматически получает статус «Тяжелая карточка» и помечается значком.",
                    "Тяжелая карточка означает, что формулировка вопроса либо непонятна вашему мозгу, либо содержит сразу несколько смешанных фактов."
                ),
                bulletPoints = listOf(
                    "Переформулируйте вопрос" to "Сделайте вопрос более конкретным или свяжите его с понятной жизненной ситуацией.",
                    "Добавьте мнемонику" to "Используйте абсурдную визуальную ассоциацию или рифму в поле подсказки.",
                    "Разделите на две" to "Превратите сложную карточку в две более простые и атомарные."
                )
            ),
            InstructionSection(
                id = "study_guide",
                title = "Пошаговый процесс сессии обучения",
                subtitle = "Как нажимать кнопки и оценивать себя",
                icon = Icons.Default.CheckCircle,
                tagText = "Практика",
                tagColor = ExcellentGradeColor,
                tagContainer = ExcellentGradeContainer,
                paragraphs = listOf(
                    "Сессия в Hermann Memo построена так, чтобы тренировать нейронные связи без самообмана:"
                ),
                bulletPoints = listOf(
                    "1. Чтение вопроса" to "Внимательно прочитайте вопрос в карточке. Сформулируйте ответ вслух или мысленно.",
                    "2. Подсказка (при необходимости)" to "Если мысль крутится на языке, нажмите «Подсказка». Мнемоника активирует ассоциативную связь.",
                    "3. Показать ответ" to "Нажмите «Показать ответ» и сверьте свои мысли с эталоном.",
                    "4. Градация «Плохо»" to "Вы не смогли вспомнить или ответили неверно. Карточка повторится в ближайшее время (10 минут или 1 день).",
                    "5. Градация «Нормально»" to "Вы вспомнили правильный ответ, затратив некоторое усилие. Интервал умножается на стандартный коэффициент.",
                    "6. Градация «Отлично»" to "Вы вспомнили моментально и были уверены на 100%. Интервал получает бонусное ускорение."
                ),
                practicalTip = "Будьте строги к себе: если сомневались или угадали — выбирайте «Плохо» или «Нормально». Честность гарантирует железную память."
            )
        )
    }

    val areAllExpanded = sections.isNotEmpty() && sections.all { expandedStates[it.id] == true }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Инструкции и методология",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Научные принципы и руководство Hermann Memo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val targetState = !areAllExpanded
                            sections.forEach { section ->
                                expandedStates[section.id] = targetState
                            }
                        },
                        modifier = Modifier.testTag("toggle_all_instructions_button")
                    ) {
                        Icon(
                            imageVector = if (areAllExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                            contentDescription = if (areAllExpanded) "Свернуть все карточки" else "Развернуть все карточки",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Intro Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("instructions_hero_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Система долговечной памяти",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Освойте законы работы мозга за 3 минуты",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Hermann Memo — это не просто программа для карточек, а когнитивный тренажер. Приложение объединяет кривую забывания Эббингауза, теорию кошелька Миллера и атомарное кодирование фактов для гарантированного переноса знаний в долговременную память.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Quick Toggle Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Разделы руководства (${sections.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FilledTonalButton(
                        onClick = {
                            val targetState = !areAllExpanded
                            sections.forEach { section ->
                                expandedStates[section.id] = targetState
                            }
                        },
                        modifier = Modifier.testTag("quick_toggle_all_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (areAllExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (areAllExpanded) "Свернуть все" else "Развернуть все",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Accordion Sections
            items(sections, key = { it.id }) { section ->
                val isExpanded = expandedStates[section.id] ?: false

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { expandedStates[section.id] = !isExpanded }
                        .testTag("instruction_card_${section.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(section.tagContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = null,
                                        tint = section.tagColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(section.tagContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = section.tagText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = section.tagColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = section.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Expanded content
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                section.paragraphs.forEach { paragraph ->
                                    Text(
                                        text = paragraph,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                if (section.bulletPoints.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        section.bulletPoints.forEach { (pointTitle, pointDesc) ->
                                            Row(verticalAlignment = Alignment.Top) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 7.dp)
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(section.tagColor)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = pointTitle,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = pointDesc,
                                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (section.practicalTip != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Практический совет:",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = section.practicalTip,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
