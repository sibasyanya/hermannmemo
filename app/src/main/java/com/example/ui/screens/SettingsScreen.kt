package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AiProvider
import com.example.data.local.AnswerCheckMode
import com.example.data.remote.UpdateCheckerService
import com.example.ui.notifications.ReminderNotificationManager
import com.example.ui.theme.ExcellentGradeColor
import com.example.ui.theme.ExcellentGradeContainer
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UpdateUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsState()
    val settingsManager = viewModel.settingsManager

    val answerCheckMode by settingsManager.answerCheckMode.collectAsState()
    val strictBacklogBlocking by settingsManager.strictBacklogBlocking.collectAsState()
    val notificationsEnabled by settingsManager.notificationsEnabled.collectAsState()
    val notificationTime by settingsManager.notificationTime.collectAsState()
    val aiProvider by settingsManager.aiProvider.collectAsState()
    val aiApiKey by settingsManager.aiApiKey.collectAsState()
    val aiModel by settingsManager.aiModel.collectAsState()
    val aiEndpoint by settingsManager.aiEndpoint.collectAsState()

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    var providerDropdownExpanded by remember { mutableStateOf(false) }
    var testNotificationSent by remember { mutableStateOf(false) }
    var isTestingAi by remember { mutableStateOf(false) }
    var aiTestResult by remember { mutableStateOf<String?>(null) }
    var isAiTestSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            settingsManager.setNotificationsEnabled(true)
            ReminderNotificationManager.scheduleDailyReminder(context, 10, 0)
            val sent = ReminderNotificationManager.showTestNotification(context)
            if (sent) {
                testNotificationSent = true
                Toast.makeText(context, "🔔 Тестовое уведомление доставлено в шторку Android!", Toast.LENGTH_LONG).show()
            }
        } else {
            settingsManager.setNotificationsEnabled(false)
            Toast.makeText(context, "Разрешение на показ уведомлений отклонено.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки и алгоритм",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Answer Verification Behavior Card (User Request 1)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("answer_check_mode_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Spellcheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Режим проверки ответов",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: MANUAL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { settingsManager.setAnswerCheckMode(AnswerCheckMode.MANUAL) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = answerCheckMode == AnswerCheckMode.MANUAL,
                                onClick = { settingsManager.setAnswerCheckMode(AnswerCheckMode.MANUAL) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Ручной режим (наглядный)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "При неверном ответе ваш вариант отображается зачеркнутым, внизу виден верный ответ, и вы сами выбираете оценку («Тяжелая» / 15 минут).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Option 2: AUTO_RESET
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { settingsManager.setAnswerCheckMode(AnswerCheckMode.AUTO_RESET) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = answerCheckMode == AnswerCheckMode.AUTO_RESET,
                                onClick = { settingsManager.setAnswerCheckMode(AnswerCheckMode.AUTO_RESET) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Автоматический сброс по Эббингаузу",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "При ошибке программа сама сбрасывает карточку в интервал 15 минут без дополнительных нажатий.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Backlog & Topic Progression Policy (User Request 4)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("backlog_policy_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Доступ к новым карточкам",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Блокировать новые карточки при долгах",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (strictBacklogBlocking) {
                                        "Включено: программа требует сначала повторить все просроченные карточки."
                                    } else {
                                        "Выключено (по умолчанию): вы можете свободно учить новые темы и карточки, даже если есть карточки на повторение."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = strictBacklogBlocking,
                                onCheckedChange = { settingsManager.setStrictBacklogBlocking(it) }
                            )
                        }
                    }
                }
            }

            // 3. Push Notifications Card (User Request 5)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("push_notifications_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Push-уведомления и напоминания",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Ежедневные напоминания о повторениях",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Система напомнит, когда карточки подойдут к сроку повторения по кривой Эббингауза.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            settingsManager.setNotificationsEnabled(true)
                                            ReminderNotificationManager.scheduleDailyReminder(context, 10, 0)
                                        }
                                    } else {
                                        settingsManager.setNotificationsEnabled(false)
                                        ReminderNotificationManager.cancelDailyReminder(context)
                                    }
                                }
                            )
                        }

                        if (notificationsEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Время напоминания: $notificationTime (утро)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("09:00", "10:00", "14:00", "19:00", "21:00").forEach { time ->
                                    val isSelected = notificationTime == time
                                    OutlinedButton(
                                        onClick = {
                                            settingsManager.setNotificationTime(time)
                                            val parts = time.split(":")
                                            val hour = parts[0].toIntOrNull() ?: 10
                                            val minute = parts[1].toIntOrNull() ?: 0
                                            ReminderNotificationManager.scheduleDailyReminder(context, hour, minute)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = if (isSelected) {
                                            ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            ButtonDefaults.outlinedButtonColors()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = time, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else if (!ReminderNotificationManager.areNotificationsEnabled(context)) {
                                        Toast.makeText(context, "Уведомления отключены в системе. Открываем настройки...", Toast.LENGTH_LONG).show()
                                        ReminderNotificationManager.openNotificationSettings(context)
                                    } else {
                                        val sent = ReminderNotificationManager.showTestNotification(context)
                                        if (sent) {
                                            testNotificationSent = true
                                            Toast.makeText(context, "🔔 Тестовое уведомление доставлено в верхнюю шторку Android!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Не удалось отправить уведомление. Проверьте системные настройки.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("send_test_notification_button")
                            ) {
                                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (testNotificationSent) "Тестовое уведомление отправлено ✓" else "Отправить тестовое уведомление")
                            }
                        }
                    }
                }
            }

            // 4. AI Integration Card (User Request 7)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("ai_integration_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Интеграция с искусственным интеллектом (AI)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "AI позволяет в один клик генерировать готовые темы и атомарные карточки с вопросами, ответами и мнемониками по любому предмету.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Provider selector
                        ExposedDropdownMenuBox(
                            expanded = providerDropdownExpanded,
                            onExpandedChange = { providerDropdownExpanded = !providerDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = aiProvider.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Провайдер AI") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = providerDropdownExpanded,
                                onDismissRequest = { providerDropdownExpanded = false }
                            ) {
                                AiProvider.values().forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.displayName) },
                                        onClick = {
                                            settingsManager.setAiProvider(provider)
                                            providerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // API Key input
                        OutlinedTextField(
                            value = aiApiKey,
                            onValueChange = { settingsManager.setAiApiKey(it) },
                            label = { Text("API-ключ") },
                            placeholder = { Text(if (aiProvider == AiProvider.GEMINI) "AIzaSy..." else "sk-...") },
                            modifier = Modifier.fillMaxWidth().testTag("ai_api_key_input"),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model input
                        OutlinedTextField(
                            value = aiModel,
                            onValueChange = { settingsManager.setAiModel(it) },
                            label = { Text("Модель") },
                            placeholder = { Text(aiProvider.defaultModel) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_model_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        val modelPresets = when (aiProvider) {
                            AiProvider.GEMINI -> listOf(
                                "gemini-2.5-flash",
                                "gemini-2.0-flash",
                                "gemini-1.5-flash"
                            )
                            AiProvider.GROQ -> listOf(
                                "llama-3.3-70b-versatile",
                                "mixtral-8x7b-32768"
                            )
                            AiProvider.OPENROUTER -> listOf(
                                "google/gemini-2.0-flash-lite-001",
                                "meta-llama/llama-3.3-70b-instruct:free"
                            )
                            AiProvider.OPENAI -> listOf(
                                "gpt-4o-mini",
                                "gpt-4o"
                            )
                            AiProvider.CUSTOM -> emptyList()
                        }

                        if (modelPresets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Быстрый выбор модели:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                modelPresets.forEach { mCode ->
                                    val isCurrent = aiModel == mCode
                                    val shortLabel = mCode.substringAfterLast("/").replace("gemini-", "").replace("-flash", " flash")
                                    Surface(
                                        selected = isCurrent,
                                        onClick = { settingsManager.setAiModel(mCode) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (aiProvider == AiProvider.CUSTOM) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = aiEndpoint,
                                onValueChange = { settingsManager.setAiEndpoint(it) },
                                label = { Text("URL эндпоинта") },
                                placeholder = { Text("http://localhost:11434/v1/chat/completions") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when (aiProvider) {
                                AiProvider.GEMINI -> "Бесплатный ключ Gemini можно получить в консоли Google AI Studio (aistudio.google.com/app/apikey)."
                                AiProvider.OPENROUTER -> "OpenRouter поддерживает десятки бесплатных и платных моделей (OpenAI, Anthropic, Mistral)."
                                AiProvider.GROQ -> "Groq предоставляет бесплатные сверхбыстрые модели Llama 3 и Mixtral (console.groq.com)."
                                else -> "Ключ хранится исключительно локально в зашифрованных настройках приложения."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingAi = true
                                    aiTestResult = null
                                    val res = viewModel.testAiConnection()
                                    isTestingAi = false
                                    res.onSuccess { msg ->
                                        isAiTestSuccess = true
                                        aiTestResult = msg
                                    }.onFailure { err ->
                                        isAiTestSuccess = false
                                        aiTestResult = err.localizedMessage ?: "Сбой подключения к AI"
                                    }
                                }
                            },
                            enabled = !isTestingAi && aiApiKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("test_ai_connection_button")
                        ) {
                            if (isTestingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Проверка связи с моделью...")
                            } else {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Проверить подключение к AI")
                            }
                        }

                        aiTestResult?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAiTestSuccess) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = if (isAiTestSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isAiTestSuccess) Color(0xFF15803D) else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAiTestSuccess) Color(0xFF15803D) else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Version & Auto-update Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("update_checker_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Версия приложения",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "v${UpdateCheckerService.currentVersionName} (Сборка ${UpdateCheckerService.currentVersionCode})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { viewModel.checkForUpdates() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("check_update_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Обновить")
                            }
                        }

                        // Update Status View
                        when (val state = updateState) {
                            is UpdateUiState.Checking -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Проверка наличия обновлений на GitHub...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is UpdateUiState.Success -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                if (state.info.isUpdateAvailable) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ExcellentGradeContainer)
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Доступна новая версия: ${state.info.tagName}!",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ExcellentGradeColor
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = state.info.changelog,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (state.info.downloadUrl.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.info.downloadUrl))
                                                        context.startActivity(intent)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ExcellentGradeColor),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Скачать APK")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "У вас установлена самая актуальная версия приложения.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ExcellentGradeColor
                                    )
                                }
                            }

                            is UpdateUiState.Error -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            UpdateUiState.Idle -> { /* Idle */ }
                        }
                    }
                }
            }

            // 6. Database & Demo Topics Management Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Управление данными",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Все ваши темы, карточки и статистика повторений хранятся локально на устройстве (Room SQLite) и работают 100% офлайн.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("reset_demo_decks_button")
                        ) {
                            Text("Восстановить демонстрационные темы по уровням")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Восстановление демонстрационных тем",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Это действие восстановит исходные темы (Базовые, Продвинутые и Экспертные) со всеми атомарными карточками. Вы уверены?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDemoDecks()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("Восстановить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

