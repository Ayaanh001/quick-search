package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.fuzzy.BaseFuzzySearchStrategy
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.fuzzy.FuzzySearchStrategy
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.SearchTextNormalizer

/**
 * Fuzzy search strategy specifically for app search.
 * Handles fuzzy matching of app names and nicknames.
 */
class FuzzyAppSearchStrategy(
    override val config: FuzzySearchConfig,
) : BaseFuzzySearchStrategy<AppInfo>() {
    /**
     * Finds fuzzy matches for apps based on the query.
     * Searches both app names and nicknames.
     */
    override fun findMatches(
        query: String,
        candidates: List<AppInfo>,
    ): List<FuzzySearchStrategy.Match<AppInfo>> {
        return findMatchesWithNicknames(query, candidates) { null }
    }

    /**
     * Creates matches with nickname support.
     * This is the main method AppSearchManager will use.
     */
    fun findMatchesWithNicknames(
        query: String,
        candidates: List<AppInfo>,
        nicknameProvider: (AppInfo) -> String?,
    ): List<FuzzySearchStrategy.Match<AppInfo>> {
        if (query.isBlank()) return emptyList()

        return candidates
            .mapNotNull { app ->
                val nickname = nicknameProvider(app)
                val score = engine.computeScore(query, app.appName, nickname, config.minQueryLength)
                if (score >= config.matchThreshold) {
                    FuzzySearchStrategy.Match(
                        item = app,
                        score = score,
                        priority = config.priority,
                        isFuzzyMatch = true,
                    )
                } else {
                    null
                }
            }.sortedByDescending { it.score }
    }

    fun isTokenCoveredByApp(token: String, appName: String, nickname: String?): Boolean {
        val tokenLower = SearchTextNormalizer.normalizeForSearch(token)
        val nameLower = SearchTextNormalizer.normalizeForSearch(appName)
        if (nameLower.contains(tokenLower)) return true
        nickname?.let { nick ->
            if (SearchTextNormalizer.normalizeForSearch(nick).contains(tokenLower)) return true
        }
        val score = engine.computeScore(token, appName, nickname, config.minQueryLength)
        return score >= config.matchThreshold
    }
}
