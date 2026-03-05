package com.tk.quicksearch.search.data.preferences

internal object AliasPreferenceMigration {
    fun resolveAliasValue(
        aliasValue: String?,
        legacyShortcutValue: String?,
    ): String? = aliasValue ?: legacyShortcutValue
}
