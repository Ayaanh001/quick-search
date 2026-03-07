package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object DeviceSettingsSearchPolicy {
    data class MatchResult(
        val hasMatch: Boolean,
        val hasNicknameMatch: Boolean,
    )

    fun evaluateMatch(
        setting: DeviceSetting,
        query: SearchQueryContext,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): MatchResult {
        val nickname = nicknameCache[setting.id]
        val nicknamePriority = matcher.match(setting.title, query, nickname)
        val hasNicknameMatch = nicknamePriority == 0 || matchingNicknameIds.contains(setting.id)

        val fieldPriority =
            matcher.matchAny(
                query,
                setting.title,
                setting.description.orEmpty(),
                setting.keywords.joinToString(" "),
            )

        return MatchResult(
            hasMatch = matcher.isMatch(fieldPriority) || hasNicknameMatch,
            hasNicknameMatch = hasNicknameMatch,
        )
    }

    fun rankingPriority(
        setting: DeviceSetting,
        matchResult: MatchResult,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int {
        if (matchResult.hasNicknameMatch) return 0

        val normalizedTitle = SearchTextNormalizer.normalizeForSearch(setting.title)
        if (normalizedTitle == query.normalizedQuery) return 1
        if (normalizedTitle.startsWith(query.normalizedQuery)) return 2

        val fieldPriority =
            matcher.matchAny(
                query,
                setting.title,
                setting.description.orEmpty(),
                setting.keywords.joinToString(" "),
            )
        return fieldPriority + 2
    }
}
