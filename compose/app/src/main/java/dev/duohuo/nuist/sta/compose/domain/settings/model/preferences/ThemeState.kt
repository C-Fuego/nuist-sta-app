package dev.duohuo.nuist.sta.compose.domain.settings.model.preferences

import android.os.Build
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.PaletteStyle
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeMode

/**
 * A shared data class to hold the theme-related UI state.
 */
data class ThemeState(
    val isLoaded: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useAmoled: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Int = 0xFF335BBC.toInt(),
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.MIUIX,
    val predictiveBackExitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.ALWAYS_RIGHT,
)
