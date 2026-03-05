package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.core.SearchTarget

sealed interface AliasTarget {
    data class Search(val target: SearchTarget) : AliasTarget

    data class Feature(val featureId: String) : AliasTarget
}

fun AliasTarget.asSearchTargetOrNull(): SearchTarget? =
    when (this) {
        is AliasTarget.Search -> target
        is AliasTarget.Feature -> null
    }
