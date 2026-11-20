// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.domain.settings.usecase.settings

import dev.duohuo.nuist.sta.compose.domain.settings.repository.AppSettingsRepository
import dev.duohuo.nuist.sta.compose.domain.settings.repository.BooleanSetting
import dev.duohuo.nuist.sta.compose.domain.settings.repository.IntSetting
import dev.duohuo.nuist.sta.compose.domain.settings.repository.StringSetting

class UpdateSettingUseCase(private val appSettingsRepo: AppSettingsRepository) {
    suspend operator fun invoke(setting: BooleanSetting, value: Boolean) =
        appSettingsRepo.putBoolean(setting, value)

    suspend operator fun invoke(setting: StringSetting, value: String) =
        appSettingsRepo.putString(setting, value)

    suspend operator fun invoke(setting: IntSetting, value: Int) =
        appSettingsRepo.putInt(setting, value)
}
