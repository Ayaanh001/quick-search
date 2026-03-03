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

    /** Search onboarding scrim gradient start */
    val OnboardingScrimTop = Color.Black.copy(alpha = 0.7f)

    /** Search onboarding scrim gradient middle */
    val OnboardingScrimMiddle = Color.Black.copy(alpha = 0.5f)

    /** Search onboarding scrim gradient lower stop */
    val OnboardingScrimBottom = Color.Black.copy(alpha = 0.2f)

    /** Speech bubble border color used in search onboarding overlays */
    val OnboardingBubbleBorder = Color.White.copy(alpha = 0.3f)

    /** Secondary text color used inside search onboarding bubble */
    val OnboardingBubbleBodyText = Color.White.copy(alpha = 0.9f)

    // ============================================================================
    // SEARCH FIELD WELCOME PALETTES
    // ============================================================================

    val SearchFieldAuroraPalette =
        listOf(
            Color(0xFF00E5FF),
            Color(0xFF2979FF),
            ThemeDeepPurple,
            ThemeNeonPurple,
            Color(0xFF2979FF),
            Color(0xFF00E5FF),
        )

    val SearchFieldElectricPalette =
        listOf(
            ThemeNeonPurple,
            Color(0xFFFF00CC),
            Color(0xFFFF3D00),
            Color(0xFFFF00CC),
            ThemeNeonPurple,
            Color(0xFF2979FF),
            ThemeNeonPurple,
        )

    val SearchFieldGoldenPalette =
        listOf(
            Color(0xFFFFD700),
            Color(0xFFFF9100),
            Color(0xFFFFEA00),
            Color(0xFFFFD700),
            Color(0xFFFFA000),
            Color(0xFFFFD700),
        )

    val SearchFieldGooglePalette =
        listOf(
            Color(0xFF4285F4),
            ThemeIndigo,
            ThemePurple,
            Color(0xFFE91E63),
            Color(0xFFEA4335),
            Color(0xFFFF5722),
            Color(0xFFFF9800),
            Color(0xFFFFC107),
            Color(0xFFFBBC05),
            Color(0xFFD4E157),
            Color(0xFFCDDC39),
            Color(0xFF34A853),
            Color(0xFF00BFA5),
            Color(0xFF00BCD4),
            Color(0xFF03A9F4),
            Color(0xFF4285F4),
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
        )

    // ============================================================================
    // OVERLAY GRADIENT PALETTES
    // ============================================================================

    val OverlayForestDarkPalette =
        listOf(
            Color(0xFF27382F),
            Color(0xFF2F4640),
            Color(0xFF435034),
            Color(0xFF1F3340),
        )

    val OverlayAuroraDarkPalette =
        listOf(
            Color(0xFF1F2E4A),
            Color(0xFF1F4A5A),
            Color(0xFF3A3E6B),
            Color(0xFF2A3150),
        )

    val OverlaySunsetDarkPalette =
        listOf(
            Color(0xFF4A2C34),
            Color(0xFF5A3A2A),
            Color(0xFF5C3046),
            Color(0xFF3E2A3B),
        )

    val OverlayMonochromeDarkPalette =
        listOf(
            AppBackgroundDark,
            Color(0xFF2A2A2A),
            Color(0xFF3E3E3E),
            Color(0xFFE8E8E8),
        )

    val OverlayForestLightPalette =
        listOf(
            Color(0xFFE4ECE7),
            Color(0xFFE4ECE9),
            Color(0xFFEBEEE2),
            Color(0xFFE0E9EC),
        )

    val OverlayAuroraLightPalette =
        listOf(
            Color(0xFFDCE8F8),
            Color(0xFFD8F1F0),
            Color(0xFFE2E2FA),
            Color(0xFFDCE6F4),
        )

    val OverlaySunsetLightPalette =
        listOf(
            Color(0xFFF8E1D8),
            Color(0xFFF8E8D8),
            Color(0xFFF4DCE8),
            Color(0xFFF6E1DF),
        )

    val OverlayMonochromeLightPalette =
        listOf(
            Color(0xFFF0F0F0),
            Color(0xFFE2E2E2),
            Color(0xFFD5D5D5),
            Color(0xFFBFBFBF),
        )

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
