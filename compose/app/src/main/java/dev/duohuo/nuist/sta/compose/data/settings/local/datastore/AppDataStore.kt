// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.settings.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppDataStore(private val dataStore: DataStore<Preferences>) {
    /** Expose the raw data flow for synchronous mapping in the repository layer. */
    val data: Flow<Preferences> = dataStore.data

    companion object {
        // Appearance
        val UI_USE_BLUR = booleanPreferencesKey("ui_use_blur")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_USE_AMOLED = booleanPreferencesKey("theme_use_amoled")
        val THEME_PALETTE_STYLE = stringPreferencesKey("theme_palette_style")
        val THEME_COLOR_SPEC = stringPreferencesKey("theme_color_spec")
        val THEME_USE_DYNAMIC_COLOR = booleanPreferencesKey("theme_use_dynamic_color")
        val THEME_SEED_COLOR = intPreferencesKey("theme_seed_color")
        val THEME_WALLPAPER_SEED_COLOR = intPreferencesKey("theme_wallpaper_seed_color")
        val UI_USE_APPLE_FLOATING_BAR = booleanPreferencesKey("ui_use_apple_floating_bar")
        val PREDICTIVE_BACK_ANIMATION = stringPreferencesKey("predictive_back_animation")
        val PREDICTIVE_BACK_EXIT_DIRECTION = stringPreferencesKey("predictive_back_exit_direction")

        // Logging
        val ENABLE_FILE_LOGGING = booleanPreferencesKey("enable_file_logging")
    }

    suspend fun putString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    fun getString(key: Preferences.Key<String>, default: String = ""): Flow<String> = dataStore.data.map { it[key] ?: default }

    suspend fun putInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { it[key] = value }
    }

    fun getInt(key: Preferences.Key<Int>, default: Int = 0): Flow<Int> = dataStore.data.map { it[key] ?: default }

    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean = false): Flow<Boolean> = dataStore.data.map { it[key] ?: default }
}
