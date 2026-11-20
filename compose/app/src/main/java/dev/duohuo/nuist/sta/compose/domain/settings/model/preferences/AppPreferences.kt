// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.domain.settings.model.preferences

import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.PaletteStyle
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeMode

/**
 * The aggregated state of all application preferences.
 */
data class AppPreferences(
    val themeMode: ThemeMode,
    val useAmoled: Boolean,
    val paletteStyle: PaletteStyle,
    val colorSpec: ThemeColorSpec,
    val useDynamicColor: Boolean,
    val useAppleFloatingBar: Boolean,
    val useBlur: Boolean,
    val seedColorInt: Int,
    val wallpaperSeedColorInt: Int,
    val predictiveBackAnimation: PredictiveBackAnimation,
    val predictiveBackExitDirection: PredictiveBackExitDirection,
    val enableFileLogging: Boolean,
)
