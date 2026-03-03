package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.shared.util.WallpaperUtils

@Composable
internal fun SearchScreenWallpaperLogic(
    state: SearchUiState,
    onWallpaperLoaded: (() -> Unit)? = null,
    isOverlayPresentation: Boolean = false,
): Triple<ImageBitmap?, Boolean, Boolean> {
    val context = LocalContext.current

    val sourceWallpaperBitmap =
        produceState<ImageBitmap?>(
            initialValue = null,
            key1 = state.backgroundSource,
            key2 = state.hasWallpaperPermission,
        ) {
            value =
                if (state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER) {
                    WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                        ?: when (val result = WallpaperUtils.getWallpaperBitmapResult(context)) {
                            is WallpaperUtils.WallpaperLoadResult.Success -> {
                                if (!isOverlayPresentation) {
                                    onWallpaperLoaded?.invoke()
                                }
                                result.bitmap.asImageBitmap()
                            }

                            else -> {
                                null
                            }
                        }
                } else {
                    null
                }
        }
    val sourceCustomBitmap =
        produceState<ImageBitmap?>(
            initialValue = null,
            key1 = state.backgroundSource,
            key2 = state.customImageUri,
        ) {
            value =
                if (state.backgroundSource == BackgroundSource.CUSTOM_IMAGE) {
                    WallpaperUtils.getOverlayCustomImageBitmap(context, state.customImageUri)
                } else {
                    null
                }
        }
    val imageBitmap =
        when (state.backgroundSource) {
            BackgroundSource.SYSTEM_WALLPAPER -> sourceWallpaperBitmap
            BackgroundSource.CUSTOM_IMAGE -> sourceCustomBitmap
            BackgroundSource.THEME -> null
        }
    val useImageBackground =
        state.backgroundSource != BackgroundSource.THEME && imageBitmap != null
    val useMonoThemeFallback =
        !isOverlayPresentation &&
            state.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
            imageBitmap == null

    return Triple(imageBitmap?.value, useImageBackground, useMonoThemeFallback)
}