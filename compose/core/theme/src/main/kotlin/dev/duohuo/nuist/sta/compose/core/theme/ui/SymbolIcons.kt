package dev.duohuo.nuist.sta.compose.core.theme.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.R

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
    "account_balance" to "\uE84F",
    "analytics" to "\uEF3E",
    "apartment" to "\uEA40",
    "arrow_back" to "\uE5C4",
    "badge" to "\uEA67",
    "bolt" to "\uEA0B",
    "bug_report" to "\uE868",
    "calendar_month" to "\uEBCC",
    "calendar_today" to "\uE935",
    "campaign" to "\uEF49",
    "chair" to "\uEFED",
    "check" to "\uE5CA",
    "chevron_backward" to "\uF46B",
    "chevron_forward" to "\uF46A",
    "chevron_right" to "\uE409",
    "close" to "\uE14C",
    "cloud_off" to "\uE2C1",
    "contrast" to "\uEB37",
    "dark_mode" to "\uE51C",
    "description" to "\uE873",
    "directions_bus" to "\uE530",
    "event" to "\uE24F",
    "expand_less" to "\uE5CE",
    "expand_more" to "\uE5CF",
    "fact_check" to "\uF0C5",
    "grid_view" to "\uE9B0",
    "home" to "\uE88A",
    "inbox" to "\uE156",
    "info" to "\uE88E",
    "invert_colors" to "\uE891",
    "layers" to "\uE53B",
    "lightbulb" to "\uE0F0",
    "link" to "\uE157",
    "list" to "\uE896",
    "map" to "\uE55B",
    "meeting_room" to "\uEB4F",
    "menu" to "\uE5D2",
    "menu_open" to "\uE9BD",
    "my_location" to "\uE1B3",
    "palette" to "\uE3B7",
    "person" to "\uE7FD",
    "place" to "\uE0C8",
    "push_pin" to "\uF10D",
    "refresh" to "\uE5D5",
    "school" to "\uE80C",
    "score" to "\uE269",
    "search" to "\uE8B6",
    "search_off" to "\uEA76",
    "topic" to "\uF1C4",
    "tune" to "\uE429",
    "work" to "\uE8F9",
)

/**
 * 用 Material Symbols 字形渲染一个图标（名称如 "home"、"school"、"person"）。
 * 颜色默认取 LocalContentColor（导航项等容器会下发）。
 */
@Composable
fun SymbolIcon(
    name: String,
    filled: Boolean = true,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    fontSize: TextUnit = 24.sp,
    contentDescription: String? = null,
) {
    Text(
        symbolCodepoints[name] ?: name,
        fontFamily = if (filled) SymbolsFilled else SymbolsOutlined,
        fontSize = fontSize,
        // 图标字体作为 Text 渲染时，字形墨水区偏离行盒中心（开关 ✓/×、色板
        // 对勾看起来偏上）。关闭字体额外内边距并裁掉上下行距（Trim.Both），
        // 让字形在盒子里真正居中。
        lineHeight = fontSize,
        style = androidx.compose.ui.text.TextStyle(
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                includeFontPadding = false,
            ),
            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
            ),
        ),
        color = tint,
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
        maxLines = 1,
    )
}
