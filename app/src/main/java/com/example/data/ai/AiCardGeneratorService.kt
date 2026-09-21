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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateCardsForTopic(
        topicPrompt: String,
        level: String,
        count: Int = 8
    ): Result<GeneratedDeckResult> = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.aiApiKey.value.trim()
        val provider = settingsManager.aiProvider.value
        val model = settingsManager.aiModel.value.trim().ifBlank { provider.defaultModel }

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

                Верни СТРОГО валидный JSON-объект без лишнего текста и без markdown-разметки:
                {
                  "title": "Краткое название темы",
                  "description": "Описание темы (1-2 предложения)",
                  "category": "Категория темы (например: IT, Языки, Наука, История, и т.д.)",
                  "level": "$level",
                  "cards": [
                    {
                      "question": "Четкий и конкретный вопрос или термин",
                      "answer": "Точный, понятный и емкий ответ",
                      "hint": "Краткая подсказка, мнемоника или этимология"
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
            Result.failure(e)
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPrompt: String
    ): String {
        val cleanModel = model.removePrefix("models/")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nТема: $userPrompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
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
                throw RuntimeException("Ошибка Gemini API (${response.code}): $errorMsg")
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
            put("temperature", 0.4)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Тема: $userPrompt")
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
        // Strip markdown code fences if present
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
}
