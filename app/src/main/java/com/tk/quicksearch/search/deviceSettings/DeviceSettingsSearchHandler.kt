package com.tk.quicksearch.search.deviceSettings

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import java.util.Locale

private const val RESULT_LIMIT = 25

data class DeviceSettingsSearchResults(
    val pinned: List<DeviceSetting>,
    val excluded: List<DeviceSetting>,
    val results: List<DeviceSetting>,
)

class DeviceSettingsSearchHandler(
    private val context: Context,
    private val repository: DeviceSettingsRepository,
    private val userPreferences: UserAppPreferences,
    private val showToastCallback: (Int) -> Unit,
) {
    private var availableSettings: List<DeviceSetting> = emptyList()

    suspend fun loadShortcuts() {
        availableSettings = repository.loadShortcuts()
    }

    suspend fun getSettingsByIds(ids: Set<String>): Map<String, DeviceSetting> {
        if (ids.isEmpty()) return emptyMap()
        if (availableSettings.isEmpty()) {
            availableSettings = repository.loadShortcuts()
        }
        return availableSettings
            .filter { ids.contains(it.id) }
            .associateBy { it.id }
    }

    fun getAvailableSettings(): List<DeviceSetting> =
        availableSettings.sortedBy { it.title.lowercase(Locale.getDefault()) }

    suspend fun getPinnedAndExcludedOnly(): DeviceSettingsSearchResults {
        if (availableSettings.isEmpty()) {
            availableSettings = repository.loadShortcuts()
        }

        val pinnedIds = userPreferences.getPinnedSettingIds()
        val excludedIds = userPreferences.getExcludedSettingIds()

        val pinned =
            availableSettings
                .filter { pinnedIds.contains(it.id) && !excludedIds.contains(it.id) }
                .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded =
            availableSettings.filter { excludedIds.contains(it.id) }.sortedBy {
                it.title.lowercase(Locale.getDefault())
            }

        return DeviceSettingsSearchResults(pinned, excluded, emptyList())
    }

    fun getSettingsState(
        query: String,
        isSettingsSectionEnabled: Boolean,
    ): DeviceSettingsSearchResults {
        // Cache preference reads to avoid repeated SharedPreferences lookups
        val pinnedIds = userPreferences.getPinnedSettingIds()
        val excludedIds = userPreferences.getExcludedSettingIds()

        val pinned =
            availableSettings
                .filter { pinnedIds.contains(it.id) && !excludedIds.contains(it.id) }
                .sortedBy { it.title.lowercase(Locale.getDefault()) }
        val excluded =
            availableSettings.filter { excludedIds.contains(it.id) }.sortedBy {
                it.title.lowercase(Locale.getDefault())
            }

        val results =
            if (query.isNotBlank() && isSettingsSectionEnabled) {
                searchSettingsInternal(query, excludedIds)
            } else {
                emptyList()
            }

        return DeviceSettingsSearchResults(pinned, excluded, results)
    }

    fun searchSettings(query: String): List<DeviceSetting> = searchSettingsInternal(query, userPreferences.getExcludedSettingIds())

    private fun searchSettingsInternal(
        query: String,
        excludedIds: Set<String>,
    ): List<DeviceSetting> {
        val nicknameMatches =
            userPreferences
                .findSettingsWithMatchingNickname(query.trim())
                .filterNot { excludedIds.contains(it) }
                .toSet()

        val nicknameCache =
            availableSettings.associate { shortcut ->
                shortcut.id to userPreferences.getSettingNickname(shortcut.id)
            }

        return DeviceSettingsSearchAlgorithm.search(
            fullList = availableSettings,
            query = query,
            excludedIds = excludedIds,
            matchingNicknameIds = nicknameMatches,
            nicknameCache = nicknameCache,
            resultLimit = RESULT_LIMIT,
        )
    }

    fun openSetting(setting: DeviceSetting) {
        userPreferences.addRecentItem(RecentSearchEntry.Setting(setting.id))
        runCatching {
            val intent = repository.buildIntent(setting)
            context.startActivity(intent)
        }.onFailure { showToastCallback(R.string.error_open_setting) }
    }
}
