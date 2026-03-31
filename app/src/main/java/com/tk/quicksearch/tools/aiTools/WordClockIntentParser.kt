package com.tk.quicksearch.tools.aiTools

/**
 * Detects "word clock" requests. Two-stage: cheap candidate scan, then extraction.
 */
object WordClockIntentParser {
    private val timeInPrefixRegex =
            Regex("""(?i)^time\s+in\s+(.+)$""")

    fun isCandidate(trimmedQuery: String): Boolean =
            timeInPrefixRegex.matches(trimmedQuery.trim())

    fun parseConfirmed(trimmedQuery: String): ConfirmedWordClockQuery? {
        val normalized = trimmedQuery.trim()
        val match = timeInPrefixRegex.matchEntire(normalized) ?: return null
        val extracted = match.groupValues[1].trim()
        if (extracted.isBlank()) return null
        return ConfirmedWordClockQuery(
                timeExpression = extracted,
                originalQuery = normalized,
        )
    }
}

data class ConfirmedWordClockQuery(
        val timeExpression: String,
        val originalQuery: String,
)
