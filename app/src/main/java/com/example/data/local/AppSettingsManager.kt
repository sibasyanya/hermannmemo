package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AnswerCheckMode {
    MANUAL,     // Пользователь видит свой зачеркнутый ответ и верный вариант внизу, сам нажимает оценку
    AUTO_RESET  // При ошибке система автоматически сбрасывает карточку в повторение по Эббингаузу
}

enum class AiProvider(val displayName: String, val defaultModel: String, val defaultEndpoint: String) {
    GEMINI("Google Gemini (Бесплатный ключ AI Studio)", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta/models/"),
    OPENROUTER("OpenRouter (Бесплатные и платные модели)", "google/gemini-2.0-flash-lite-001", "https://openrouter.ai/api/v1/chat/completions"),
    GROQ("Groq (Сверхбыстрые Llama/Mixtral)", "llama-3.3-70b-versatile", "https://api.groq.com/openai/v1/chat/completions"),
    OPENAI("OpenAI (ChatGPT / GPT-4o)", "gpt-4o-mini", "https://api.openai.com/v1/chat/completions"),
    CUSTOM("Свой OpenAI-совместимый API / Ollama", "custom-model", "http://localhost:11434/v1/chat/completions")
}

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ebbinghaus_settings", Context.MODE_PRIVATE)

    private val _answerCheckMode = MutableStateFlow(
        AnswerCheckMode.valueOf(prefs.getString("answer_check_mode", AnswerCheckMode.MANUAL.name) ?: AnswerCheckMode.MANUAL.name)
    )
    val answerCheckMode: StateFlow<AnswerCheckMode> = _answerCheckMode.asStateFlow()

    private val _strictBacklogBlocking = MutableStateFlow(
        prefs.getBoolean("strict_backlog_blocking", false)
    )
    val strictBacklogBlocking: StateFlow<Boolean> = _strictBacklogBlocking.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean("notifications_enabled", true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationTime = MutableStateFlow(
        prefs.getString("notification_time", "10:00") ?: "10:00"
    )
    val notificationTime: StateFlow<String> = _notificationTime.asStateFlow()

    private val _aiProvider = MutableStateFlow(
        AiProvider.valueOf(prefs.getString("ai_provider", AiProvider.GEMINI.name) ?: AiProvider.GEMINI.name)
    )
    val aiProvider: StateFlow<AiProvider> = _aiProvider.asStateFlow()

    private val _aiApiKey = MutableStateFlow(
        prefs.getString("ai_api_key", "") ?: ""
    )
    val aiApiKey: StateFlow<String> = _aiApiKey.asStateFlow()

    private val _aiModel = MutableStateFlow(
        prefs.getString("ai_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
    )
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    private val _aiEndpoint = MutableStateFlow(
        prefs.getString("ai_endpoint", "") ?: ""
    )
    val aiEndpoint: StateFlow<String> = _aiEndpoint.asStateFlow()

    fun setAnswerCheckMode(mode: AnswerCheckMode) {
        prefs.edit().putString("answer_check_mode", mode.name).apply()
        _answerCheckMode.value = mode
    }

    fun setStrictBacklogBlocking(enabled: Boolean) {
        prefs.edit().putBoolean("strict_backlog_blocking", enabled).apply()
        _strictBacklogBlocking.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setNotificationTime(time: String) {
        prefs.edit().putString("notification_time", time).apply()
        _notificationTime.value = time
    }

    fun setAiProvider(provider: AiProvider) {
        prefs.edit().putString("ai_provider", provider.name).apply()
        _aiProvider.value = provider
        if (_aiModel.value.isBlank() || _aiModel.value == "gemini-2.0-flash" || _aiModel.value == "gpt-4o-mini" || _aiModel.value == "llama-3.3-70b-versatile") {
            setAiModel(provider.defaultModel)
        }
    }

    fun setAiApiKey(key: String) {
        prefs.edit().putString("ai_api_key", key).apply()
        _aiApiKey.value = key
    }

    fun setAiModel(model: String) {
        prefs.edit().putString("ai_model", model).apply()
        _aiModel.value = model
    }

    fun setAiEndpoint(endpoint: String) {
        prefs.edit().putString("ai_endpoint", endpoint).apply()
        _aiEndpoint.value = endpoint
    }

    companion object {
        @Volatile
        private var INSTANCE: AppSettingsManager? = null

        fun getInstance(context: Context): AppSettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AppSettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
