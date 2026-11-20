// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: SYSTEM
    }
}
