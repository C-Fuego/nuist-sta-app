// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.duohuo.nuist.sta.compose.data.settings.local.datastore.AppDataStore
import dev.duohuo.nuist.sta.compose.data.settings.provider.SystemEnvProviderImpl
import dev.duohuo.nuist.sta.compose.data.settings.provider.ThemeStateProviderImpl
import dev.duohuo.nuist.sta.compose.data.settings.repository.AppSettingsRepositoryImpl
import dev.duohuo.nuist.sta.compose.domain.settings.provider.SystemEnvProvider
import dev.duohuo.nuist.sta.compose.domain.settings.provider.ThemeStateProvider
import dev.duohuo.nuist.sta.compose.domain.settings.repository.AppSettingsRepository
import dev.duohuo.nuist.sta.compose.domain.settings.usecase.settings.UpdateSettingUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsModule = module {
    // DataStore
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("app_settings")
        }
    }

    singleOf(::AppDataStore)

    single<AppSettingsRepository> {
        AppSettingsRepositoryImpl(
            appDataStore = get(),
            appScope = get(named("AppScope")),
        )
    }

    single<ThemeStateProvider> {
        ThemeStateProviderImpl(
            appSettingsRepo = get<AppSettingsRepository>(),
            appScope = get(named("AppScope")),
        )
    }

    single<SystemEnvProvider> { SystemEnvProviderImpl(context = androidContext()) }

    singleOf(::UpdateSettingUseCase)
}
