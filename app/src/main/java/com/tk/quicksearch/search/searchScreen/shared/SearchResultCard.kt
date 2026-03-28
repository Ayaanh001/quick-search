package com.tk.quicksearch.search.searchScreen.shared

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Search-screen card wrapper (counterpart to [com.tk.quicksearch.settings.shared.SettingsCard]).
 * Used only on the search result surface: sections, suggestions, engine cards, direct search, etc.
 * Styling is centralized via [DesignTokens.SearchResultCardShape], [AppColors.getSearchResultCardColors],
 * and [AppColors.getCardElevation].
 */
@Composable
fun SearchResultCard(
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean,
    overlayContainerColor: Color? = LocalOverlayResultCardColor.current,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppColors.getSearchResultCardColors(showWallpaperBackground, overlayContainerColor)
    val elevation = AppColors.getCardElevation(showWallpaperBackground)
    val shape = DesignTokens.SearchResultCardShape
    if (showWallpaperBackground) {
        Card(
            modifier = modifier,
            colors = colors,
            shape = shape,
            elevation = elevation,
            content = content,
        )
    } else {
        ElevatedCard(
            modifier = modifier,
            colors = colors,
            shape = shape,
            elevation = elevation,
            content = content,
        )
    }
}
