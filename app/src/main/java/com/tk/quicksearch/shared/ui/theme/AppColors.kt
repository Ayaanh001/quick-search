package com.tk.quicksearch.shared.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Centralized color definitions for the QuickSearch app.
 * Contains all colors used throughout the application including backgrounds, text, overlays, and theme colors.
 */
object AppColors {
    // ============================================================================
    // THEME COLORS (PURPLE THEME)
    // ============================================================================

    /** Primary purple theme color - Deep Purple */
    val ThemeDeepPurple = Color(0xFF651FFF)

    /** Secondary purple theme color - Neon Purple */
    val ThemeNeonPurple = Color(0xFFD500F9)

    /** Tertiary purple theme color - Indigo (bridge to purple) */
    val ThemeIndigo = Color(0xFF5E35B1)

    /** Quaternary purple theme color - Purple (bridge to red) */
    val ThemePurple = Color(0xFF9C27B0)

    // ============================================================================
    // APP BACKGROUND COLORS
    // ============================================================================

    /** Main app background color (transparent for wallpaper mode) */
    val AppBackgroundTransparent = Color.Transparent

    /** Dark app background color */
    val AppBackgroundDark = Color(0xFF121212)

    // ============================================================================
    // SEARCH BAR COLORS
    // ============================================================================

    /** Search bar background color with transparency */
    val SearchBarBackground = Color.Black.copy(alpha = 0.5f)

    /** Search bar border color with transparency */
    val SearchBarBorder = Color.White.copy(alpha = 0.3f)

    /** Search bar text and icon color for dark backgrounds */
    val SearchBarTextAndIcon = Color(0xFFE0E0E0)

    // ============================================================================
    // SETTINGS COLORS
    // ============================================================================

    /** Settings background color */
    val SettingsBackground = Color.Transparent

    /** Settings option card background color */
    val SettingsCardBackground = Color.Black.copy(alpha = 0.4f)

    /** Settings text color */
    val SettingsText = Color.White

    // ============================================================================
    // OVERLAY AND TRANSPARENCY COLORS
    // ============================================================================

    /** Standard overlay color with low transparency */
    val OverlayLow = Color.Black.copy(alpha = 0.2f)

    /** Medium overlay color */
    val OverlayMedium = Color.Black.copy(alpha = 0.4f)

    /** High overlay color */
    val OverlayHigh = Color.Black.copy(alpha = 0.5f)

    /** Very high overlay color for dialogs */
    val OverlayVeryHigh = Color.Black.copy(alpha = 0.75f)

    /** Dialog background color */
    val DialogBackground = Color.Black

    /** Dialog text color */
    val DialogText = Color.White

    // ============================================================================
    // CARD THEMING UTILITIES
    // ============================================================================

    /**
     * Returns appropriate card colors based on wallpaper background setting.
     * When wallpaper background is enabled, uses a semi-transparent overlay.
     * When disabled, uses standard Material Design surface container color.
     */
    @Composable
    fun getCardColors(showWallpaperBackground: Boolean): CardColors =
        if (showWallpaperBackground) {
            CardDefaults.cardColors(
                containerColor = OverlayMedium,
            )
        } else {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }

    /**
     * Returns appropriate card elevation based on wallpaper background setting.
     * Cards with wallpaper background use no elevation, others use standard elevation.
     */
    @Composable
    fun getCardElevation(showWallpaperBackground: Boolean): CardElevation =
        if (showWallpaperBackground) {
            CardDefaults.cardElevation(defaultElevation = 0.dp)
        } else {
            CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        }

}
