// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package dev.duohuo.nuist.sta.compose.core.theme.material

import androidx.compose.ui.graphics.Color

data class RawColor(val key: String, val color: Color, val displayName: String = key)

/**
 * 预设种子色。蓝色排第一 = 首次运行的默认选中色。
 *
 * 清理记录：InstallerX 原版 18 色中剔除了感知上过近的 5 档
 * （琥珀≈橙色、青色≈青绿、浅蓝≈蓝色、靛蓝≈蓝色/紫色、深紫≈紫色，
 * 另默认 #0082EF 与蓝色重复、橄榄≈酸橙），并按品牌诉求把蓝色提到首位。
 */
val PresetColors = listOf(
    RawColor("blue", Color(0xFF335BBC), "蓝色"),
    RawColor("pink", Color(0xFFB94073), "粉红"),
    RawColor("red", Color(0xFFBA1A1A), "红色"),
    RawColor("orange", Color(0xFF944A00), "橙色"),
    RawColor("yellow", Color(0xFF795900), "黄色"),
    RawColor("lime", Color(0xFF5E6400), "酸橙"),
    RawColor("green", Color(0xFF006D39), "绿色"),
    RawColor("teal", Color(0xFF006A64), "青绿"),
    RawColor("purple", Color(0xFF6750A4), "紫色"),
    RawColor("blue_grey", Color(0xFF575D7E), "蓝灰"),
    RawColor("brown", Color(0xFF7D524A), "棕色"),
    RawColor("grey", Color(0xFF5F6162), "灰色"),
)
