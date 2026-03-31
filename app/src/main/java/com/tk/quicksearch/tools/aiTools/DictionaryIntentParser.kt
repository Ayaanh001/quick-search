package com.tk.quicksearch.tools.aiTools

/**
 * Detects dictionary queries. Two-stage: cheap candidate scan, then extraction.
 */
object DictionaryIntentParser {
    private val candidateRegex =
            Regex(
                    """(?i)\b(define|definition|meaning|dictionary|what\s+does)\b""",
            )

    private val definePattern =
            Regex(
                    """(?i)^(?:define|definition(?:\s+of)?|meaning(?:\s+of)?|dictionary(?:\s+of)?)\s+(.+?)\s*\??$""",
            )
    private val whatDoesMeanPattern =
            Regex(
                    """(?i)^what\s+does\s+(.+?)\s+mean\??$""",
            )
    private val suffixMeaningPattern =
            Regex(
                    """(?i)^(.+?)\s+meaning\??$""",
            )

    fun isCandidate(trimmedQuery: String): Boolean = candidateRegex.containsMatchIn(trimmedQuery)

    fun parseConfirmed(trimmedQuery: String): ConfirmedDictionaryQuery? {
        val normalized = trimmedQuery.trim()
        if (normalized.isBlank() || !isCandidate(normalized)) return null
        val term =
                when {
                    definePattern.matches(normalized) ->
                            definePattern.matchEntire(normalized)?.groupValues?.get(1)
                    whatDoesMeanPattern.matches(normalized) ->
                            whatDoesMeanPattern.matchEntire(normalized)?.groupValues?.get(1)
                    suffixMeaningPattern.matches(normalized) ->
                            suffixMeaningPattern.matchEntire(normalized)?.groupValues?.get(1)
                    else -> null
                }
                        ?.trim()
                        ?.trim('"', '\'', '.', ',', ';', ':', '?', '!')
                        ?.trim()
        if (term.isNullOrBlank()) return null
        return ConfirmedDictionaryQuery(
                term = term,
                originalQuery = normalized,
        )
    }
}

data class ConfirmedDictionaryQuery(
        val term: String,
        val originalQuery: String,
)
