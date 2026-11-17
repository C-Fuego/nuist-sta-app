package dev.duohuo.nuist.sta.compose.features.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn

/**
 * 课程块配色：直接用种子色派生出的 `colorScheme` 容器角色（6 组循环）。
 *
 * 与 InstallerX 一致——它只用 materialkolor 生成 ColorScheme，不手搓色调板；
 * 这里也只取 role，保证跟用户选的色板完全一致、不过饱和。
 */
@Composable
private fun courseColors(): List<Pair<Color, Color>> {
    val s = MaterialTheme.colorScheme
    // 深/浅、蓝/紫交错，把同色相的角色隔开，保证相邻课程块可区分
    return listOf(
        s.onPrimaryContainer to s.primaryContainer,
        s.onTertiaryFixed to s.tertiaryFixed,
        s.onSecondaryContainer to s.secondaryContainer,
        s.onPrimaryFixed to s.primaryFixed,
        s.onTertiaryContainer to s.tertiaryContainer,
        s.onSecondaryFixed to s.secondaryFixed,
    )
}

private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 课表（Compose + MD3E，简化只读周视图）。对应 Flutter `WeekGrid` 的只读呈现。
 *
 * 布局：左侧节次时间列（固定宽） + 右侧 7 天列；每列用固定行高堆叠课程块，
 * 课程块按 `startNode..endNode` 合并成一块，用绝对偏移定位。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetableScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text("课表", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton() },
                                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        val scheme = MaterialTheme.colorScheme
        val periodW = 52.dp
        val cellH = 56.dp
        val headerH = 44.dp
        var week by rememberSaveable { mutableIntStateOf(TimetableMock.CURRENT_WEEK) }
        val isCurrentWeek = week == TimetableMock.CURRENT_WEEK

        Column(Modifier.fillMaxSize().padding(inner)) {
        // 周次工具栏（照搬 InstallerX 分段形态；切周只改视图状态，mock 数据不变）
        SegmentedColumn {
            item {
                BaseWidget(
                    icon = Icons.Filled.CalendarMonth,
                    title = "第 $week 周",
                    description = TimetableMock.TERM_LABEL +
                        " · 共 ${TimetableMock.TOTAL_WEEKS} 周" +
                        if (isCurrentWeek) " · 本周" else "",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.IconButton(
                                onClick = { if (week > 1) week -= 1 },
                                enabled = week > 1,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.NavigateBefore,
                                    contentDescription = "上一周",
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = { if (week < TimetableMock.TOTAL_WEEKS) week += 1 },
                                enabled = week < TimetableMock.TOTAL_WEEKS,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = "下一周",
                                )
                            }
                        }
                    },
                )
            }
        }

        Row(Modifier.weight(1f)) {
            // 左侧节次列
            Column(Modifier.width(periodW)) {
                Box(Modifier.height(headerH))
                TimetableMock.periodTimes.forEachIndexed { i, (start, end) ->
                    val top = i % 2 == 0
                    Column(
                        modifier = Modifier.height(cellH).fillMaxWidth()
                            .background(if (top) Color.Transparent else scheme.surfaceContainerLow.copy(alpha = 0.4f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text(start, style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant)
                    }
                }
            }
            VerticalDivider(color = scheme.outlineVariant)
            // 右侧 7 天
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                WeekHeader(headerH, highlightToday = isCurrentWeek)
                WeekBody(cellH, TimetableMock.periodTimes.size)
            }
        }
        }
    }
}

@Composable
private fun WeekHeader(height: Dp, highlightToday: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(height)) {
        dayNames.forEachIndexed { i, d ->
            Column(
                modifier = Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("周$d", style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (i == 2 && highlightToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == 2 && highlightToday) scheme.primary else scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeekBody(cellH: Dp, nodeCount: Int) {
    val scheme = MaterialTheme.colorScheme
    val totalH = cellH * nodeCount
    Box(Modifier.fillMaxWidth().height(totalH)) {
        // 横向分隔线（每节一行）+ 纵向分隔线（每天一列）
        Column {
            repeat(nodeCount) { i ->
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(cellH - 1.dp))
            }
        }
        Row(Modifier.fillMaxSize()) {
            repeat(7) { i ->
                if (i > 0) VerticalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.weight(1f))
            }
        }
        // 课程块
        BoxWithBlocks(cellH)
    }
}

@Composable
private fun BoxWithBlocks(cellH: Dp) {
    // 用与 WeekBody 相同的宽度比例给每列定位：这里依靠 BoxWithConstraints 不方便，
    // 直接按「每列 weight」不行（Box 内是绝对定位）——因此用 fillMaxWidth 的横排各占 1/7。
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val colW = maxWidth / 7
        val colors = courseColors()
        TimetableMock.blocks.forEach { b ->
            val (fg, bg) = colors[b.colorIndex % colors.size]
            val top = cellH * (b.startNode - 1)
            val span = b.endNode - b.startNode + 1
            val height = cellH * span
            Box(
                modifier = Modifier
                    .offset(x = colW * (b.day - 1) + 1.dp, y = top + 1.dp)
                    .width(colW - 2.dp)
                    .height(height - 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .padding(4.dp),
            ) {
                Column {
                    Text(b.name, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = fg, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    if (b.location.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(b.location, style = MaterialTheme.typography.labelSmall,
                            color = fg.copy(alpha = 0.8f), maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
