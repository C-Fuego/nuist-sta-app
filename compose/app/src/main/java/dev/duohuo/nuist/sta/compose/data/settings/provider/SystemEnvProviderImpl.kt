// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.data.settings.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.kieronquinn.monetcompat.core.MonetCompat
import dev.duohuo.nuist.sta.compose.domain.settings.provider.SystemEnvProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class SystemEnvProviderImpl(private val context: Context) : SystemEnvProvider {

    override val packageName: String
        get() = context.packageName

    override suspend fun getPackageUid(packageName: String): Int? = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0).uid
    }.getOrNull()

    override fun isIgnoringBatteryOptimizationsFlow(): Flow<Boolean> = flow {
        val power = context.getSystemService(PowerManager::class.java)
        emit(power?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }.flowOn(Dispatchers.IO)

    override fun isAdbVerifyEnabledFlow(): Flow<Boolean> = flowOf(false)

    /**
     * 打开系统的电池优化列表，由用户自己放行本应用。
     *
     * 不申请 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`：本应用不需要这条权限，
     * 用系统列表页 + 应用详情页兜底就够，机型不认前一个入口时也能落到应用详情。
     */
    override fun requestIgnoreBatteryOptimization() {
        val opened = runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
        if (opened) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override suspend fun authenticateBiometric(isInstaller: Boolean): Boolean = true

    override fun setLauncherAliasEnabled(enabled: Boolean) = Unit

    override suspend fun getLatestLogUri(): String? = null

    override fun getWallpaperColorsFlow(): Flow<List<Int>?> = flow {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            val colors = runCatching {
                MonetCompat.getInstance().getAvailableWallpaperColors()
            }.getOrNull()
            emit(colors)
        } else {
            // Android 12+ 走系统动态取色，不再从壁纸库提取
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
}
