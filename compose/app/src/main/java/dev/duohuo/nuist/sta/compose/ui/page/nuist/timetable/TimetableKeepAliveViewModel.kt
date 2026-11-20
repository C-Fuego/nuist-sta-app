// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.domain.settings.provider.SystemEnvProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 后台运行引导页：查系统是否已放行本应用，并打开对应系统设置。
 *
 * 系统是否放行由原生侧读，页面不做机型判断、也不猜当前品牌。
 */
class TimetableKeepAliveViewModel(private val systemEnv: SystemEnvProvider) : ViewModel() {

    private val _ignoringBatteryOptimizations = MutableStateFlow<Boolean?>(null)

    /** `null` 表示还没读到；`true` 表示系统已不再限制本应用后台运行。 */
    val ignoringBatteryOptimizations: StateFlow<Boolean?> =
        _ignoringBatteryOptimizations.asStateFlow()

    init {
        refresh()
    }

    /** 从系统设置回到页面时再查一次，让按钮状态跟上。 */
    fun refresh() {
        viewModelScope.launch {
            _ignoringBatteryOptimizations.value = runCatching {
                systemEnv.isIgnoringBatteryOptimizationsFlow().first()
            }.getOrNull()
        }
    }

    fun openBatteryOptimizationSettings() = systemEnv.requestIgnoreBatteryOptimization()
}