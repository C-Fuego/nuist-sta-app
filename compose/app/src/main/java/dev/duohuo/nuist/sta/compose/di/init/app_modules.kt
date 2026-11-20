// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.di.init

import dev.duohuo.nuist.sta.compose.di.coreModule
import dev.duohuo.nuist.sta.compose.di.initModule
import dev.duohuo.nuist.sta.compose.di.serializationModule
import dev.duohuo.nuist.sta.compose.di.settingsModule
import dev.duohuo.nuist.sta.compose.di.viewModelModule

val appModules = listOf(
    viewModelModule,
    serializationModule,
    coreModule,
    settingsModule,
    initModule,
)
