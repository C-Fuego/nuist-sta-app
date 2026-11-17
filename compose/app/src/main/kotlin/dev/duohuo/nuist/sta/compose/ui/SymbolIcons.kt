package dev.duohuo.nuist.sta.compose.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.R

/**
 * Material Symbols Rounded 可变字体（FILL/GRAD/opsz/wght 四轴）。
 * 同一个 TTF 通过 FILL 轴切换实心/空心——MD3 导航图标的规范做法，
 * 选中 FILL=1（实心）、未选中 FILL=0（空心）。
 */
private fun symbolsFont(fill: Float) = FontFamily(
    Font(
        R.font.material_symbols_rounded,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("FILL", fill),
            FontVariation.Setting("GRAD", 0f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("wght", 400f),
        ),
    )
)

private val SymbolsFilled = symbolsFont(1f)
private val SymbolsOutlined = symbolsFont(0f)

/**
 * 图标名 → Material Symbols 码位。字体已按实际使用的图标子集化
 * （15.2MB → 约 10KB，保留 FILL/GRAD/opsz/wght 可变轴），
 * 因此改为直接渲染码位而不是 ligature。
 */
private val symbolCodepoints = mapOf(
    "home" to "\uE88A",
    "school" to "\uE80C",
    "person" to "\uE7FD",
)

/**
 * 用 Material Symbols 字形渲染一个图标（名称如 "home"、"school"、"person"）。
 * 颜色默认取 LocalContentColor（导航项等容器会下发）。
 */
@Composable
fun SymbolIcon(
    name: String,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    fontSize: TextUnit = 24.sp,
) {
    Text(
        symbolCodepoints[name] ?: name,
        fontFamily = if (filled) SymbolsFilled else SymbolsOutlined,
        fontSize = fontSize,
        color = tint,
        modifier = modifier,
        maxLines = 1,
    )
}
