// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.domain.settings.provider.SystemEnvProvider
import dev.duohuo.nuist.sta.compose.domain.settings.repository.AppSettingsRepository
import dev.duohuo.nuist.sta.compose.domain.settings.repository.BooleanSetting
import dev.duohuo.nuist.sta.compose.domain.settings.repository.IntSetting
import dev.duohuo.nuist.sta.compose.domain.settings.repository.StringSetting
import dev.duohuo.nuist.sta.compose.domain.settings.usecase.settings.UpdateSettingUseCase
import dev.duohuo.nuist.sta.compose.ui.theme.material.PresetColors
import dev.duohuo.nuist.sta.compose.ui.theme.material.RawColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
) : ViewModel() {

    val state: StateFlow<ThemeSettingsState> = combine(
        appSettingsRepo.preferencesFlow,
        systemEnvProvider.getWallpaperColorsFlow().onStart { emit(emptyList()) },
    ) { prefs, wallpaperColors ->
        val manualSeedColor =
            if (PresetColors.any { it.color.toArgb() == prefs.seedColorInt }) {
                Color(prefs.seedColorInt)
            } else {
                PresetColors[0].color
            }
        val manualWallpaperSeedColor = Color(prefs.wallpaperSeedColorInt)

        val effectiveSeedColor: Color =
            if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (!wallpaperColors.isNullOrEmpty()) {
                    if (wallpaperColors.contains(manualWallpaperSeedColor.toArgb())) {
                        manualWallpaperSeedColor
                    } else {
                        Color(wallpaperColors[0])
                    }
                } else {
                    manualSeedColor
                }
            } else {
                manualSeedColor
            }

        val availableColors: List<RawColor> =
            if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (!wallpaperColors.isNullOrEmpty()) {
                    wallpaperColors.map { colorInt ->
                        RawColor(key = colorInt.toHexString(), color = Color(colorInt))
                    }
                } else {
                    PresetColors
                }
            } else {
                PresetColors
            }

        ThemeSettingsState(
            useBlur = prefs.useBlur,
            themeMode = prefs.themeMode,
            useAmoled = prefs.useAmoled,
            paletteStyle = prefs.paletteStyle,
            colorSpec = prefs.colorSpec,
            useDynamicColor = prefs.useDynamicColor,
            useAppleFloatingBar = prefs.useAppleFloatingBar,
            seedColor = effectiveSeedColor,
            availableColors = availableColors,
            predictiveBackAnimation = prefs.predictiveBackAnimation,
            predictiveBackExitDirection = prefs.predictiveBackExitDirection,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeSettingsState(),
    )

    fun dispatch(action: ThemeSettingsAction) {
        when (action) {
            is ThemeSettingsAction.SetUseBlur -> viewModelScope.launch { updateSetting(BooleanSetting.UiUseBlur, action.enable) }

            is ThemeSettingsAction.SetThemeMode -> viewModelScope.launch {
                updateSetting(StringSetting.ThemeMode, action.mode.name)
            }

            is ThemeSettingsAction.SetUseAmoled -> viewModelScope.launch {
                updateSetting(BooleanSetting.ThemeUseAmoled, action.use)
            }

            is ThemeSettingsAction.SetPaletteStyle -> viewModelScope.launch {
                updateSetting(StringSetting.ThemePaletteStyle, action.style.name)
            }

            is ThemeSettingsAction.SetColorSpec -> viewModelScope.launch {
                updateSetting(StringSetting.ThemeColorSpec, action.spec.name)
            }

            is ThemeSettingsAction.SetUseDynamicColor -> viewModelScope.launch {
                updateSetting(BooleanSetting.ThemeUseDynamicColor, action.use)
            }

            is ThemeSettingsAction.SetUseAppleFloatingBar -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseAppleFloatingBar,
                    action.use,
                )
            }

            is ThemeSettingsAction.SetSeedColor -> viewModelScope.launch {
                updateSetting(
                    if (action.isWallpaperSeedColor) {
                        IntSetting.ThemeWallpaperSeedColor
                    } else {
                        IntSetting.ThemeSeedColor
                    },
                    action.color.toArgb(),
                )
            }

            is ThemeSettingsAction.SetPredictiveBackAnimation -> viewModelScope.launch {
                updateSetting(
                    StringSetting.PredictiveBackAnimation,
                    action.animation.value,
                )
            }

            is ThemeSettingsAction.SetPredictiveBackExitDirection -> viewModelScope.launch {
                updateSetting(
                    StringSetting.PredictiveBackExitDirection,
                    action.direction.value,
                )
            }
        }
    }
}
