package com.tk.quicksearch.search.searchScreen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.shared.ui.theme.AppColors

private const val NEUTRAL_OVERLAY_THEME_INTENSITY = 0.5f
private const val MAX_OVERLAY_THEME_TONE_SHIFT = 0.38f

internal fun overlayGradientColors(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    alpha: Float = 1f,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): List<Color> {
    val baseColors =
        if (isDarkMode) {
            when (theme) {
                OverlayGradientTheme.FOREST ->
                    AppColors.OverlayForestDarkPalette
                OverlayGradientTheme.AURORA ->
                    AppColors.OverlayAuroraDarkPalette
                OverlayGradientTheme.SUNSET ->
                    AppColors.OverlaySunsetDarkPalette
                OverlayGradientTheme.MONOCHROME ->
                    AppColors.OverlayMonochromeDarkPalette
            }
        } else {
            when (theme) {
                OverlayGradientTheme.FOREST ->
                    AppColors.OverlayForestLightPalette
                OverlayGradientTheme.AURORA ->
                    AppColors.OverlayAuroraLightPalette
                OverlayGradientTheme.SUNSET ->
                    AppColors.OverlaySunsetLightPalette
                OverlayGradientTheme.MONOCHROME ->
                    AppColors.OverlayMonochromeLightPalette
            }
        }

    val clampedAlpha = alpha.coerceIn(0f, 1f)
    return baseColors.map { color ->
        adjustOverlayThemeTone(color = color, intensity = intensity).copy(alpha = clampedAlpha)
    }
}

internal fun overlayResultCardColor(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): Color {
    val palette = overlayGradientColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    return if (isDarkMode) palette[1] else palette[0]
}

internal fun overlayDividerColor(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): Color {
    val palette = overlayGradientColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    val base = if (isDarkMode) palette[2] else palette[3]
    return if (isDarkMode) {
        lerp(start = base, stop = Color.White, fraction = 0.18f).copy(alpha = 0.38f)
    } else {
        lerp(start = base, stop = Color.Black, fraction = 0.15f).copy(alpha = 0.24f)
    }
}

internal fun overlayActionColor(
    theme: OverlayGradientTheme,
    isDarkMode: Boolean,
    intensity: Float = NEUTRAL_OVERLAY_THEME_INTENSITY,
): Color {
    val palette = overlayGradientColors(theme = theme, isDarkMode = isDarkMode, intensity = intensity)
    return if (isDarkMode) {
        lerp(start = palette[2], stop = Color.Black, fraction = 0.28f)
    } else {
        lerp(start = palette[1], stop = Color.Black, fraction = 0.38f)
    }
}

private fun adjustOverlayThemeTone(
    color: Color,
    intensity: Float,
): Color {
    val normalizedIntensity =
        ((intensity.coerceIn(0f, 1f) - NEUTRAL_OVERLAY_THEME_INTENSITY) * 2f)
    return when {
        normalizedIntensity > 0f ->
            lerp(
                start = color,
                stop = Color.Black,
                fraction = normalizedIntensity * MAX_OVERLAY_THEME_TONE_SHIFT,
            )
        normalizedIntensity < 0f ->
            lerp(
                start = color,
                stop = Color.White,
                fraction = -normalizedIntensity * MAX_OVERLAY_THEME_TONE_SHIFT,
            )
        else -> color
    }
}
