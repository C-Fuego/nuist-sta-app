// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.di

import dev.duohuo.nuist.sta.compose.util.cache.AutoCacheSweeper
import dev.duohuo.nuist.sta.compose.util.timber.LogController
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val initModule = module {
    singleOf(::LogController) { createdAtStart() }
    singleOf(::AutoCacheSweeper) { createdAtStart() }
}
