// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.core.theme.material

import androidx.compose.ui.graphics.Color

data class RawColor(val key: String, val color: Color, val displayName: String = key)

/**
 * 预设种子色：完整收录 参考实现的 18 个配色（[RawColor] 原文颜色值）。
 *
 * 蓝色提到首位 = 首次运行/无有效存储时的默认种子色；
 * 参考实现 原 "default"（#4A672D 橄榄绿）保留为 `olive`。
 */
val PresetColors = listOf(
    RawColor("blue", Color(0xFF335BBC), "蓝色"),
    RawColor("pink", Color(0xFFB94073), "粉红"),
    RawColor("red", Color(0xFFBA1A1A), "红色"),
    RawColor("orange", Color(0xFF944A00), "橙色"),
    RawColor("amber", Color(0xFF8C5300), "琥珀"),
    RawColor("yellow", Color(0xFF795900), "黄色"),
    RawColor("lime", Color(0xFF5E6400), "酸橙"),
    RawColor("green", Color(0xFF006D39), "绿色"),
    RawColor("olive", Color(0xFF4A672D), "橄榄绿"),
    RawColor("cyan", Color(0xFF006A64), "青绿"),
    RawColor("teal", Color(0xFF006874), "青色"),
    RawColor("light_blue", Color(0xFF00639B), "浅蓝"),
    RawColor("indigo", Color(0xFF5355A9), "靛蓝"),
    RawColor("purple", Color(0xFF6750A4), "紫色"),
    RawColor("deep_purple", Color(0xFF7E42A4), "深紫"),
    RawColor("blue_grey", Color(0xFF575D7E), "蓝灰"),
    RawColor("brown", Color(0xFF7D524A), "棕色"),
    RawColor("grey", Color(0xFF5F6162), "灰色"),
)
