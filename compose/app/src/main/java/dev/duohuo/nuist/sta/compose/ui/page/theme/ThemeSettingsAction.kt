// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.theme

import androidx.compose.ui.graphics.Color
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackAnimation
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackExitDirection
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.PaletteStyle
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeMode

sealed interface ThemeSettingsAction {
    data class SetUseBlur(val enable: Boolean) : ThemeSettingsAction
    data class SetThemeMode(val mode: ThemeMode) : ThemeSettingsAction
    data class SetUseAmoled(val use: Boolean) : ThemeSettingsAction
    data class SetPaletteStyle(val style: PaletteStyle) : ThemeSettingsAction
    data class SetColorSpec(val spec: ThemeColorSpec) : ThemeSettingsAction
    data class SetUseDynamicColor(val use: Boolean) : ThemeSettingsAction
    data class SetUseAppleFloatingBar(val use: Boolean) : ThemeSettingsAction
    data class SetSeedColor(val color: Color, val isWallpaperSeedColor: Boolean) : ThemeSettingsAction
    data class SetPredictiveBackAnimation(val animation: PredictiveBackAnimation) : ThemeSettingsAction
    data class SetPredictiveBackExitDirection(val direction: PredictiveBackExitDirection) : ThemeSettingsAction
}
