package com.tk.quicksearch.searchEngines

import java.util.Locale

data class ParsedAliasMatch<T>(
    val queryWithoutAlias: String,
    val target: T,
)

internal object AliasParser {
    fun <T> detectSuffixAlias(
        query: String,
        aliases: Map<String, T>,
    ): ParsedAliasMatch<T>? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null
        val words = trimmedQuery.split("\\s+".toRegex())
        if (words.size < 2) return null
        val suffix = words.last().lowercase(Locale.getDefault())
        val match = aliases[suffix] ?: return null
        return ParsedAliasMatch(
            queryWithoutAlias = words.dropLast(1).joinToString(" "),
            target = match,
        )
    }

    fun <T> detectPrefixAlias(
        query: String,
        aliases: Map<String, T>,
    ): ParsedAliasMatch<T>? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return null
        val words = trimmedQuery.split("\\s+".toRegex())
        if (words.isEmpty()) return null
        val prefix = words.first().lowercase(Locale.getDefault())
        val match = aliases[prefix] ?: return null
        return ParsedAliasMatch(
            queryWithoutAlias = words.drop(1).joinToString(" "),
            target = match,
        )
    }
}
