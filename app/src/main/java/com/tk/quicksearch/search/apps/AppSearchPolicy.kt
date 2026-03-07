package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

object AppSearchPolicy {
    fun matchPriority(
        appName: String,
        nickname: String?,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int = matcher.match(primaryText = appName, query = query, nickname = nickname)

    fun hasMatch(
        priority: Int,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Boolean = matcher.isMatch(priority)

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        appName: String,
        nickname: String?,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
    ): Boolean {
        if (query.tokens.size <= 1) return true
        return query.tokens.all { token ->
            fuzzySearchStrategy.isTokenCoveredByApp(token, appName, nickname)
        }
    }
}
