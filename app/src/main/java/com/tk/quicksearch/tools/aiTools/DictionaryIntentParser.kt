package com.tk.quicksearch.tools.aiTools

object DictionaryIntentParser {
    private val definePattern = Regex("""(?i)^define\s+(.+)$""")
    private val suffixMeaningPattern = Regex("""(?i)^(.+?)\s+meaning$""")

    fun isCandidate(trimmedQuery: String): Boolean =
            trimmedQuery.startsWith("define ", ignoreCase = true) ||
                    trimmedQuery.endsWith(" meaning", ignoreCase = true)

    fun parseConfirmed(trimmedQuery: String): ConfirmedDictionaryQuery? {
        val normalized = trimmedQuery.trim()
        if (normalized.isBlank()) return null
        val term =
                (definePattern.matchEntire(normalized) ?: suffixMeaningPattern.matchEntire(normalized))
                        ?.groupValues
                        ?.get(1)
                        ?.trim()
        if (term.isNullOrBlank()) return null
        return ConfirmedDictionaryQuery(term = term, originalQuery = normalized)
    }
}

data class ConfirmedDictionaryQuery(
        val term: String,
        val originalQuery: String,
)
