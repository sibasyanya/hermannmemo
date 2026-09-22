package com.example.data.ai

import com.example.data.local.AiProvider
import com.example.data.local.AppSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class GeneratedCard(
    val question: String,
    val answer: String,
    val hint: String
)

data class GeneratedDeckResult(
    val title: String,
    val description: String,
    val category: String,
    val level: String,
    val cards: List<GeneratedCard>
)

class AiCardGeneratorService(private val settingsManager: AppSettingsManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        fun resolveModel(rawModel: String, provider: AiProvider): String {
            val trimmed = rawModel.trim()
            return when (provider) {
                AiProvider.GEMINI -> {
                    val clean = trimmed.removePrefix("models/")
                    when {
                        clean.isBlank() -> "gemini-2.5-flash"
                        clean.contains("3.6") -> "gemini-2.5-flash" // Автокоррекция опечатки gemini-3.6-flash
                        clean.contains("3.5") -> "gemini-2.5-flash"
                        clean.contains("3.0") -> "gemini-2.5-flash"
                        clean.equals("gemini-flash", ignoreCase = true) -> "gemini-2.5-flash"
                        clean.equals("gemini-pro", ignoreCase = true) -> "gemini-2.5-pro"
                        else -> clean
                    }
                }
                AiProvider.GROQ -> if (trimmed.isBlank()) "llama-3.3-70b-versatile" else trimmed
                AiProvider.OPENROUTER -> if (trimmed.isBlank()) "google/gemini-2.0-flash-lite-001" else trimmed
                AiProvider.OPENAI -> if (trimmed.isBlank()) "gpt-4o-mini" else trimmed
                AiProvider.CUSTOM -> trimmed.ifBlank { "custom-model" }
            }
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.aiApiKey.value.trim()
        val provider = settingsManager.aiProvider.value
        val model = resolveModel(settingsManager.aiModel.value, provider)

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API-ключ не заполнен. Введите ваш ключ в поле выше.")
            )
        }

        try {
            val systemInstruction = "Ответь строго JSON: {\"status\": \"ok\", \"message\": \"connected\"}"
            val testPrompt = "Ping"

            val raw = when (provider) {
                AiProvider.GEMINI -> callGeminiApi(apiKey, model, systemInstruction, testPrompt)
                else -> callOpenAiCompatibleApi(provider, apiKey, model, systemInstruction, testPrompt)
            }

            Result.success("Соединение с AI успешно установлено! Модель: $model")
        } catch (e: Exception) {
            Result.failure(formatUserFriendlyException(e, provider, model))
        }
    }

    suspend fun generateCardsForTopic(
        topicPrompt: String,
        level: String,
        count: Int = 8
    ): Result<GeneratedDeckResult> = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.aiApiKey.value.trim()
        val provider = settingsManager.aiProvider.value
        val model = resolveModel(settingsManager.aiModel.value, provider)

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API-ключ не указан. Откройте «Настройки» -> «Интеграция с AI» и введите ваш ключ (Gemini, OpenRouter, Groq или OpenAI).")
            )
        }

        try {
            val levelDescription = when (level) {
                "BASIC" -> "базовый / фундаментальный (для начинающих)"
                "EXPERT" -> "экспертный / профи (глубокие нюансы, тонкости и редкие случаи)"
                else -> "продвинутый / специалист (уверенное практическое применение)"
            }

            val systemInstruction = """
                Ты — эксперт по методике интервального запоминания Германа Эббингауза.
                Создай набор обучающих карточек (flashcards) по заданной теме.
                Уровень сложности: $levelDescription.
                Количество карточек: $count.

                Каждая карточка должна быть строго атомарной (один факт — один вопрос). Ответ должен быть емким и понятным. В подсказке дай мнемонику, яркую ассоциацию или этимологию.

                Верни СТРОГО валидный JSON-объект без markdown-разметки:
                {
                  "title": "Краткое название темы",
                  "description": "Описание темы (1-2 предложения)",
                  "category": "Категория темы (например: IT, Языки, Наука, История, Медицина)",
                  "level": "$level",
                  "cards": [
                    {
                      "question": "Четкий и конкретный вопрос или термин",
                      "answer": "Точный, понятный и емкий ответ",
                      "hint": "Краткая подсказка, мнемоника или ассоциация"
                    }
                  ]
                }
            """.trimIndent()

            val rawResponse = when (provider) {
                AiProvider.GEMINI -> callGeminiApi(apiKey, model, systemInstruction, topicPrompt)
                AiProvider.OPENROUTER,
                AiProvider.GROQ,
                AiProvider.OPENAI,
                AiProvider.CUSTOM -> callOpenAiCompatibleApi(provider, apiKey, model, systemInstruction, topicPrompt)
            }

            val parsedResult = parseAiResponse(rawResponse, topicPrompt, level)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(formatUserFriendlyException(e, provider, model))
        }
    }

    suspend fun generateCardAssistance(
        topic: String,
        question: String,
        existingAnswer: String
    ): Result<GeneratedCard> = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.aiApiKey.value.trim()
        val provider = settingsManager.aiProvider.value
        val model = resolveModel(settingsManager.aiModel.value, provider)

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API-ключ не указан. Откройте «Настройки» и укажите ключ Gemini.")
            )
        }

        try {
            val systemInstruction = """
                Ты — эксперт по интервальному запоминанию Эббингауза и мнемонике.
                Твоя задача — помочь составить или улучшить обучающую карточку.
                Сделай ответ точным и лаконичным, а подсказку — яркой запоминающейся мнемоникой.
                Верни СТРОГО валидный JSON-объект:
                {
                  "question": "Сформулированный вопрос",
                  "answer": "Конкретный и емкий ответ",
                  "hint": "Мнемоника или ассоциативная подсказка"
                }
            """.trimIndent()

            val userPrompt = if (question.isNotBlank()) {
                "Тема: $topic\nВопрос: $question\nСуществующий ответ (если есть): $existingAnswer\nСформулируй четкий ответ и сильную мнемонику."
            } else {
                "Тема: $topic\nПридумай одну ключевую атомарную карточку по этой теме: вопрос, емкий ответ и мнемонику."
            }

            val raw = when (provider) {
                AiProvider.GEMINI -> callGeminiApi(apiKey, model, systemInstruction, userPrompt)
                else -> callOpenAiCompatibleApi(provider, apiKey, model, systemInstruction, userPrompt)
            }

            var cleaned = raw.trim()
            if (cleaned.startsWith("```json")) cleaned = cleaned.removePrefix("```json")
            if (cleaned.startsWith("```")) cleaned = cleaned.removePrefix("```")
            if (cleaned.endsWith("```")) cleaned = cleaned.removeSuffix("```")
            cleaned = cleaned.trim()

            val json = JSONObject(cleaned)
            val q = json.optString("question", question.ifBlank { "Вопрос по теме $topic" })
            val a = json.optString("answer", existingAnswer)
            val h = json.optString("hint", "")

            Result.success(GeneratedCard(question = q, answer = a, hint = h))
        } catch (e: Exception) {
            Result.failure(formatUserFriendlyException(e, provider, model))
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPrompt: String
    ): String {
        val cleanModel = resolveModel(model, AiProvider.GEMINI)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nЗапрос пользователя: $userPrompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("maxOutputTokens", 2500)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody)
                when (response.code) {
                    404 -> throw RuntimeException("Модель '$cleanModel' не найдена в Google AI Studio (404). В Настройках укажите gemini-2.5-flash или gemini-2.0-flash.")
                    400 -> throw RuntimeException("Некорректный запрос к Gemini (400): $errorMsg")
                    403 -> throw RuntimeException("API-ключ отклонен или не имеет доступа к Gemini API (403). Проверьте ключ в Google AI Studio.")
                    429 -> throw RuntimeException("Превышен лимит запросов Google AI Studio (429 Rate Limit). Подождите 30 секунд.")
                    else -> throw RuntimeException("Ошибка Gemini API (${response.code}): $errorMsg")
                }
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
            throw RuntimeException("Пустой ответ от Gemini API")
        }
    }

    private fun callOpenAiCompatibleApi(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPrompt: String
    ): String {
        val endpoint = if (provider == AiProvider.CUSTOM) {
            val custom = settingsManager.aiEndpoint.value.trim()
            custom.ifBlank { provider.defaultEndpoint }
        } else {
            provider.defaultEndpoint
        }

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("temperature", 0.3)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody)
                throw RuntimeException("Ошибка AI (${response.code}): $errorMsg")
            }

            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                return message?.optString("content", "").orEmpty()
            }
            throw RuntimeException("Пустой ответ от AI")
        }
    }

    private fun parseAiResponse(raw: String, fallbackTitle: String, fallbackLevel: String): GeneratedDeckResult {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        cleaned = cleaned.trim()

        val json = JSONObject(cleaned)
        val title = json.optString("title", fallbackTitle.replaceFirstChar { it.uppercase() })
        val description = json.optString("description", "Обучающий набор карточек по теме $title")
        val category = json.optString("category", "Общее")
        val level = json.optString("level", fallbackLevel)

        val cardsArray = json.optJSONArray("cards") ?: JSONArray()
        val cards = mutableListOf<GeneratedCard>()
        for (i in 0 until cardsArray.length()) {
            val cardObj = cardsArray.optJSONObject(i) ?: continue
            val question = cardObj.optString("question", "").trim()
            val answer = cardObj.optString("answer", "").trim()
            val hint = cardObj.optString("hint", "").trim()
            if (question.isNotBlank() && answer.isNotBlank()) {
                cards.add(GeneratedCard(question = question, answer = answer, hint = hint))
            }
        }

        if (cards.isEmpty()) {
            throw RuntimeException("AI не смог сформировать карточки. Попробуйте уточнить запрос.")
        }

        return GeneratedDeckResult(
            title = title,
            description = description,
            category = category,
            level = level,
            cards = cards
        )
    }

    private fun parseErrorMessage(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            error?.optString("message", responseBody) ?: responseBody
        } catch (_: Exception) {
            responseBody
        }
    }

    private fun formatUserFriendlyException(e: Exception, provider: AiProvider, model: String): Exception {
        return when (e) {
            is SocketTimeoutException -> RuntimeException(
                "Таймаут ожидания ответа AI (Timeout).\n\n" +
                "Рекомендации для исправления:\n" +
                "1. Проверьте модель в Настройках — используйте рекомендованную gemini-2.5-flash (быстрая и стабильная).\n" +
                "2. Убедитесь, что интернет и VPN работают стабильно (Google AI Studio требует прямого доступа).\n" +
                "3. Попробуйте уменьшить количество карточек до 5 шт."
            )
            is UnknownHostException -> RuntimeException(
                "Сетевая ошибка: невозможно связаться с сервером AI (${e.message}). Проверьте подключение к интернету или работу VPN."
            )
            is IOException -> RuntimeException(
                "Ошибка соединения с AI: ${e.localizedMessage ?: "Сбой сети"}. Проверьте соединение."
            )
            else -> e
        }
    }
}
