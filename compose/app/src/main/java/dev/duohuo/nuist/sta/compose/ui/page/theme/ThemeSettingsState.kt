// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackAnimation
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackExitDirection
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.PaletteStyle
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeMode
import dev.duohuo.nuist.sta.compose.ui.theme.material.DefaultSeedColor
import dev.duohuo.nuist.sta.compose.ui.theme.material.PresetColors
import dev.duohuo.nuist.sta.compose.ui.theme.material.RawColor

data class ThemeSettingsState(
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useAmoled: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Color = DefaultSeedColor,
    val availableColors: List<RawColor> = PresetColors,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.None,
    val predictiveBackExitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.FOLLOW_GESTURE,
)
