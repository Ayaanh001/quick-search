package com.tk.quicksearch.tools.aiTools

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.directSearch.DirectSearchClient
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import org.json.JSONObject

class WordClockNotRecognizedException : Exception()

private const val WORD_CLOCK_SYSTEM_INSTRUCTION =
        "You are a world-clock and word-clock formatter. " +
                "Respond with ONLY a single JSON object (no markdown, no code fences). " +
                "Schema: {\"word_clock_text\":\"<text>\",\"time_text\":\"<normalized input time>\"}. " +
                "Set word_clock_text to the resolved local CLOCK TIME in 12-hour format with AM/PM (example: \"2:58 PM\"). " +
                "Set time_text to the resolved local DATE (example: \"Tuesday, March 31, 2026\"). " +
                "Treat location-based requests as valid (e.g., city, country, timezone like \"India\", \"Tokyo\", \"UTC+5:30\"). " +
                "For location requests, resolve the CURRENT local time at that location before formatting. " +
                "If the user query is not a word clock request, respond exactly: {\"error\":\"not_word_clock\"}."

class WordClockHandler(
        private val context: Context,
        private val userPreferences: UserAppPreferences,
) {
    fun parseModelResponse(raw: String): Result<WordClockModelResult> {
        val trimmed =
                raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching {
            val obj = JSONObject(trimmed)
            if (obj.optString("error") == "not_word_clock") {
                throw WordClockNotRecognizedException()
            }
            val wordClockText = obj.getString("word_clock_text").trim()
            val timeText = obj.optString("time_text").trim()
            if (wordClockText.isBlank()) error("invalid")
            WordClockModelResult(
                    wordClockText = wordClockText,
                    sourceTimeText = timeText,
            )
        }
    }

    suspend fun convert(
            confirmed: ConfirmedWordClockQuery,
    ): Result<Pair<WordClockModelResult, String>> {
        val apiKey = userPreferences.getGeminiApiKey()?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return Result.failure(
                    IllegalStateException(context.getString(R.string.direct_search_error_no_key)),
            )
        }
        val modelId =
                userPreferences.getCurrencyConverterModel().trim().ifBlank {
                    GeminiModelCatalog.DEFAULT_MODEL_ID
                }
        val client = DirectSearchClient(apiKey, context)
        val userMessage =
                "Resolve this request into local clock time and date: ${confirmed.timeExpression}. " +
                        "If it is a location, compute the current local time there first. " +
                        "Original user query: ${confirmed.originalQuery}"
        val result =
                client.fetchAnswer(
                        query = userMessage,
                        personalContext = null,
                        modelId = modelId,
                        useGroundingWithGoogleSearch = true,
                        useSystemInstruction = true,
                        systemInstruction = WORD_CLOCK_SYSTEM_INSTRUCTION,
                        responseMimeType = "application/json",
                )
        return result.mapCatching { text ->
            val parsed = parseModelResponse(text).getOrElse { throw it }
            parsed to modelId
        }
    }
}

data class WordClockModelResult(
        val wordClockText: String,
        val sourceTimeText: String,
)
