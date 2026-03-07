package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale

private val WHITESPACE_REGEX = "\\s+".toRegex()

object AppSearchAlgorithm {
    fun findMatches(
        query: String,
        source: List<AppInfo>,
        limit: Int,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        sortAppsByUsageEnabled: Boolean,
    ): List<AppInfo> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

        return source
            .asSequence()
            .mapNotNull { app ->
                calculateAppMatch(
                    app = app,
                    normalizedQuery = normalizedQuery,
                    queryTokens = queryTokens,
                    fuzzySearchStrategy = fuzzySearchStrategy,
                    appNicknames = appNicknames,
                )
            }.sortedWith(createAppComparator(sortAppsByUsageEnabled))
            .map { it.app }
            .take(limit)
            .toList()
    }

    private data class AppMatch(
        val app: AppInfo,
        val priority: Int,
        val fuzzyScore: Int,
        val isFuzzy: Boolean,
    )

    private fun calculateAppMatch(
        app: AppInfo,
        normalizedQuery: String,
        queryTokens: List<String>,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
    ): AppMatch? {
        val nickname = appNicknames[app.packageName]
        val priority =
            SearchRankingUtils.calculateMatchPriorityWithNickname(
                app.appName,
                nickname,
                normalizedQuery,
                queryTokens,
            )
        if (!SearchRankingUtils.isOtherMatch(priority)) {
            if (!queryTokensCoveredByApp(queryTokens, app.appName, nickname, fuzzySearchStrategy)) return null
            return AppMatch(app, priority, 0, false)
        }

        val fuzzyMatches =
            fuzzySearchStrategy.findMatchesWithNicknames(
                normalizedQuery,
                listOf(app),
            ) { appNicknames[it.packageName] }

        return fuzzyMatches.firstOrNull()?.let { match ->
            if (!queryTokensCoveredByApp(queryTokens, app.appName, nickname, fuzzySearchStrategy)) return null
            AppMatch(app, match.priority, match.score, true)
        }
    }

    private fun queryTokensCoveredByApp(
        queryTokens: List<String>,
        appName: String,
        nickname: String?,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
    ): Boolean {
        if (queryTokens.size <= 1) return true
        return queryTokens.all { token ->
            fuzzySearchStrategy.isTokenCoveredByApp(token, appName, nickname)
        }
    }

    private fun createAppComparator(sortAppsByUsageEnabled: Boolean): Comparator<AppMatch> {
        return Comparator { first, second ->
            if (first.isFuzzy != second.isFuzzy) {
                return@Comparator if (first.isFuzzy) 1 else -1
            }

            if (!first.isFuzzy) {
                val priorityCompare = first.priority.compareTo(second.priority)
                if (priorityCompare != 0) {
                    return@Comparator priorityCompare
                }
                return@Comparator compareByUsageOrName(first.app, second.app, sortAppsByUsageEnabled)
            }

            val fuzzyCompare = second.fuzzyScore.compareTo(first.fuzzyScore)
            if (fuzzyCompare != 0) {
                return@Comparator fuzzyCompare
            }
            compareByUsageOrName(first.app, second.app, sortAppsByUsageEnabled)
        }
    }

    private fun compareByUsageOrName(
        first: AppInfo,
        second: AppInfo,
        sortAppsByUsageEnabled: Boolean,
    ): Int =
        if (sortAppsByUsageEnabled) {
            second.launchCount.compareTo(first.launchCount)
        } else {
            first.appName
                .lowercase(Locale.getDefault())
                .compareTo(second.appName.lowercase(Locale.getDefault()))
        }
}
