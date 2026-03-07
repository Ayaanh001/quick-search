package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import java.util.Locale

object AppShortcutSearchAlgorithm {
    fun search(
        fullList: List<StaticShortcut>,
        query: String,
        excludedIds: Set<String>,
        disabledIds: Set<String>,
        shortcutNicknames: Map<String, String>,
        minQueryLength: Int = 2,
        resultLimit: Int = 25,
    ): List<StaticShortcut> {
        if (fullList.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < minQueryLength) return emptyList()

        val queryContext = SearchQueryContext.fromRawQuery(trimmed)

        return fullList
            .asSequence()
            .filterNot { excludedIds.contains(shortcutKey(it)) }
            .filterNot { disabledIds.contains(shortcutKey(it)) }
            .mapNotNull { shortcut ->
                val shortcutId = shortcutKey(shortcut)
                val displayName = shortcutDisplayName(shortcut)
                val nickname = shortcutNicknames[shortcutId]
                val priority =
                    AppShortcutSearchPolicy.matchPriority(
                        displayName = displayName,
                        appLabel = shortcut.appLabel,
                        nickname = nickname,
                        query = queryContext,
                    )

                if (!DefaultSearchMatcher.isMatch(priority)) {
                    null
                } else {
                    shortcut to priority
                }
            }.sortedWith(
                compareBy<Pair<StaticShortcut, Int>> { it.second }.thenBy {
                    shortcutDisplayName(it.first).lowercase(Locale.getDefault())
                },
            ).take(resultLimit)
            .map { it.first }
            .toList()
    }
}
