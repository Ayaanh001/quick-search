package com.tk.quicksearch.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// ============================================================================
// Color Schemes
// ============================================================================

/**
 * Dark theme color scheme following Material Design 3 specifications.
 */
private val DarkColorScheme =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        outline = md_theme_dark_outline,
    )

// ============================================================================
// Theme Composable
// ============================================================================

/**
 * QuickSearch application theme composable.
 *
 * Provides Material 3 dark theme with custom typography.
 * The app always uses dark mode and does not support theme switching.
 *
 * @param content The composable content to be themed.
 */
@Composable
fun QuickSearchTheme(
    fontScaleMultiplier: Float = 1f,
    content: @Composable () -> Unit,
) {
    val baseDensity = LocalDensity.current
    val appDensity =
        remember(baseDensity, fontScaleMultiplier) {
            Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * fontScaleMultiplier,
            )
        }
    CompositionLocalProvider(LocalDensity provides appDensity) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
