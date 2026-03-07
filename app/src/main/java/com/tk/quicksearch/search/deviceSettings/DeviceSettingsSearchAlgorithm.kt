package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale

object DeviceSettingsSearchAlgorithm {
    fun search(
        fullList: List<DeviceSetting>,
        query: String,
        excludedIds: Set<String>,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
        resultLimit: Int = 25,
    ): List<DeviceSetting> {
        if (fullList.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(trimmed)
        val settingsToSearch = fullList.filterNot { excludedIds.contains(it.id) }

        return settingsToSearch
            .asSequence()
            .mapNotNull { shortcut ->
                val matchResult =
                    checkSettingMatch(
                        setting = shortcut,
                        normalizedQuery = normalizedQuery,
                        matchingNicknameIds = matchingNicknameIds,
                        nicknameCache = nicknameCache,
                    )
                if (!matchResult.hasMatch) return@mapNotNull null

                val priority = calculatePriority(shortcut, matchResult, trimmed)
                shortcut to priority
            }.sortedWith(
                compareBy({ it.second }, { it.first.title.lowercase(Locale.getDefault()) }),
            ).take(resultLimit)
            .map { it.first }
            .toList()
    }

    private data class MatchResult(
        val hasMatch: Boolean,
        val hasNicknameMatch: Boolean,
    )

    private fun checkSettingMatch(
        setting: DeviceSetting,
        normalizedQuery: String,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
    ): MatchResult {
        val nickname = nicknameCache[setting.id]
        val hasNicknameMatch =
            nickname?.let { SearchTextNormalizer.normalizeForSearch(it) }?.contains(normalizedQuery) == true
        val keywordText = setting.keywords.joinToString(" ")
        val hasFieldMatch =
            SearchTextNormalizer.normalizeForSearch(setting.title).contains(normalizedQuery) ||
                (
                    setting.description
                        ?.let { SearchTextNormalizer.normalizeForSearch(it) }
                        ?.contains(normalizedQuery) == true
                ) ||
                SearchTextNormalizer.normalizeForSearch(keywordText).contains(normalizedQuery) ||
                matchingNicknameIds.contains(setting.id)

        return MatchResult(
            hasMatch = hasFieldMatch || hasNicknameMatch,
            hasNicknameMatch = hasNicknameMatch || matchingNicknameIds.contains(setting.id),
        )
    }

    private fun calculatePriority(
        setting: DeviceSetting,
        matchResult: MatchResult,
        trimmedQuery: String,
    ): Int {
        if (matchResult.hasNicknameMatch) return 0

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(trimmedQuery)
        val normalizedTitle = SearchTextNormalizer.normalizeForSearch(setting.title)

        if (normalizedTitle == normalizedQuery) return 1
        if (normalizedTitle.startsWith(normalizedQuery)) return 2

        val keywordText = setting.keywords.joinToString(" ")
        val utilsPriority =
            SearchRankingUtils.getBestMatchPriority(
                trimmedQuery,
                setting.title,
                setting.description ?: "",
                keywordText,
            )
        return utilsPriority + 2
    }
}
