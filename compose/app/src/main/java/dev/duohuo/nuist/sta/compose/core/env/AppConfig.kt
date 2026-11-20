// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.core.env

import dev.duohuo.nuist.sta.compose.BuildConfig

/**
 * The build channel of the current APK.
 */
enum class Level {
    STABLE,
    PREVIEW,
    UNSTABLE,
}

object AppConfig {
    val LEVEL: Level = when (BuildConfig.BUILD_LEVEL) {
        1 -> Level.PREVIEW
        2 -> Level.STABLE
        else -> Level.UNSTABLE
    }

    val isDebug: Boolean = BuildConfig.DEBUG
    const val VERSION_NAME: String = BuildConfig.VERSION_NAME
    const val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    val isLogEnabled = LEVEL == Level.PREVIEW || LEVEL == Level.UNSTABLE || isDebug
}
