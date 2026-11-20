// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.domain.settings.provider

import kotlinx.coroutines.flow.Flow

interface SystemEnvProvider {
    val packageName: String

    suspend fun getPackageUid(packageName: String): Int?
    fun isIgnoringBatteryOptimizationsFlow(): Flow<Boolean>
    fun isAdbVerifyEnabledFlow(): Flow<Boolean>
    fun requestIgnoreBatteryOptimization()

    fun setLauncherAliasEnabled(enabled: Boolean)
    suspend fun getLatestLogUri(): String?

    /** 壁纸取色；不支持时返回 null。 */
    fun getWallpaperColorsFlow(): Flow<List<Int>?>
}
