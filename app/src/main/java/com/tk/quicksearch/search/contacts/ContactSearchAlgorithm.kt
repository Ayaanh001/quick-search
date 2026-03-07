package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale

object ContactSearchAlgorithm {
    fun search(
        fullList: List<ContactInfo>,
        query: String,
    ): List<ContactInfo> {
        if (fullList.isEmpty()) return emptyList()

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        return fullList
            .mapNotNull { contact ->
                val priority =
                    SearchRankingUtils.calculateMatchPriority(
                        contact.displayName,
                        normalizedQuery,
                        queryTokens,
                    )
                if (SearchRankingUtils.isOtherMatch(priority)) {
                    null
                } else {
                    contact to priority
                }
            }.sortedWith(
                compareBy(
                    { it.second },
                    { it.first.displayName.lowercase(Locale.getDefault()) },
                ),
            ).map { it.first }
    }
}
