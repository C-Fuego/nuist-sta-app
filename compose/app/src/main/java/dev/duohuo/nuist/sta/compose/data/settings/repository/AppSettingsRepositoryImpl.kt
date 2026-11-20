// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.settings.repository

import android.os.Build
import androidx.datastore.preferences.core.Preferences
import dev.duohuo.nuist.sta.compose.data.settings.local.datastore.AppDataStore
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.AppPreferences
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackAnimation
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackExitDirection
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.PaletteStyle
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeMode
import dev.duohuo.nuist.sta.compose.domain.settings.repository.AppSettingsRepository
import dev.duohuo.nuist.sta.compose.domain.settings.repository.BooleanSetting
import dev.duohuo.nuist.sta.compose.domain.settings.repository.IntSetting
import dev.duohuo.nuist.sta.compose.domain.settings.repository.StringSetting
import dev.duohuo.nuist.sta.compose.ui.theme.material.DefaultSeedColor
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class AppSettingsRepositoryImpl(
    private val appDataStore: AppDataStore,
    appScope: CoroutineScope,
) : AppSettingsRepository {

    override val preferencesFlow: Flow<AppPreferences> = appDataStore.data
        .map { prefs -> prefs.toAppPreferences() }
        .shareIn(appScope, SharingStarted.Eagerly, replay = 1)

    private fun Preferences.toAppPreferences(): AppPreferences {
        val defaultBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        return AppPreferences(
            themeMode = ThemeMode.fromValueOrDefault(
                this[AppDataStore.THEME_MODE] ?: ThemeMode.SYSTEM.name,
            ),
            useAmoled = this[AppDataStore.THEME_USE_AMOLED] ?: false,
            paletteStyle = PaletteStyle.fromValueOrDefault(
                this[AppDataStore.THEME_PALETTE_STYLE] ?: PaletteStyle.TonalSpot.name,
            ),
            colorSpec = ThemeColorSpec.fromValueOrDefault(
                this[AppDataStore.THEME_COLOR_SPEC] ?: ThemeColorSpec.SPEC_2025.name,
            ),
            useDynamicColor = this[AppDataStore.THEME_USE_DYNAMIC_COLOR] ?: false,
            useAppleFloatingBar = this[AppDataStore.UI_USE_APPLE_FLOATING_BAR] ?: false,
            useBlur = defaultBlur && (this[AppDataStore.UI_USE_BLUR] ?: true),
            seedColorInt = this[AppDataStore.THEME_SEED_COLOR] ?: DefaultSeedColor.toArgb(),
            wallpaperSeedColorInt = this[AppDataStore.THEME_WALLPAPER_SEED_COLOR] ?: DefaultSeedColor.toArgb(),
            predictiveBackAnimation = PredictiveBackAnimation.fromValueOrDefault(
                this[AppDataStore.PREDICTIVE_BACK_ANIMATION] ?: PredictiveBackAnimation.MIUIX.name,
            ),
            predictiveBackExitDirection = PredictiveBackExitDirection.fromValueOrDefault(
                this[AppDataStore.PREDICTIVE_BACK_EXIT_DIRECTION] ?: PredictiveBackExitDirection.ALWAYS_RIGHT.name,
            ),
            enableFileLogging = this[AppDataStore.ENABLE_FILE_LOGGING] ?: true,
        )
    }

    override suspend fun putString(setting: StringSetting, value: String) {
        appDataStore.putString(stringKey(setting), value)
    }

    override fun getString(setting: StringSetting, default: String): Flow<String> = appDataStore.getString(stringKey(setting), default)

    override suspend fun putInt(setting: IntSetting, value: Int) {
        appDataStore.putInt(intKey(setting), value)
    }

    override fun getInt(setting: IntSetting, default: Int): Flow<Int> = appDataStore.getInt(intKey(setting), default)

    override suspend fun putBoolean(setting: BooleanSetting, value: Boolean) {
        appDataStore.putBoolean(booleanKey(setting), value)
    }

    override fun getBoolean(setting: BooleanSetting, default: Boolean): Flow<Boolean> = appDataStore.getBoolean(booleanKey(setting), default)

    private fun stringKey(setting: StringSetting) = when (setting) {
        StringSetting.ThemeMode -> AppDataStore.THEME_MODE
        StringSetting.ThemePaletteStyle -> AppDataStore.THEME_PALETTE_STYLE
        StringSetting.ThemeColorSpec -> AppDataStore.THEME_COLOR_SPEC
        StringSetting.PredictiveBackAnimation -> AppDataStore.PREDICTIVE_BACK_ANIMATION
        StringSetting.PredictiveBackExitDirection -> AppDataStore.PREDICTIVE_BACK_EXIT_DIRECTION
    }

    private fun intKey(setting: IntSetting) = when (setting) {
        IntSetting.ThemeSeedColor -> AppDataStore.THEME_SEED_COLOR
        IntSetting.ThemeWallpaperSeedColor -> AppDataStore.THEME_WALLPAPER_SEED_COLOR
    }

    private fun booleanKey(setting: BooleanSetting) = when (setting) {
        BooleanSetting.UiUseBlur -> AppDataStore.UI_USE_BLUR
        BooleanSetting.ThemeUseAmoled -> AppDataStore.THEME_USE_AMOLED
        BooleanSetting.ThemeUseDynamicColor -> AppDataStore.THEME_USE_DYNAMIC_COLOR
        BooleanSetting.UiUseAppleFloatingBar -> AppDataStore.UI_USE_APPLE_FLOATING_BAR
        BooleanSetting.EnableFileLogging -> AppDataStore.ENABLE_FILE_LOGGING
    }
}
