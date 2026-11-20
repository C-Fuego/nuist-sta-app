// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.domain.settings.provider

import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.ThemeState
import kotlinx.coroutines.flow.StateFlow

interface ThemeStateProvider {
    val themeStateFlow: StateFlow<ThemeState>
}
