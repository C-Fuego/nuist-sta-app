package dev.duohuo.nuist.sta.compose.features.timetable

import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.layout.size
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
    // 视图切换：false=周格网（默认），true=课程列表
    var listMode by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
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
        floatingActionButton = {
            androidx.compose.material3.ToggleFloatingActionButton(
                checked = listMode,
                onCheckedChange = { listMode = it },
            ) {
                dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon(
                    name = if (listMode) "calendar_month" else "list",
                    contentDescription = if (listMode) "切换到周视图" else "切换到课程列表",
                    modifier = Modifier.size(22.dp),
                    fontSize = 22.sp,
                )
            }
        },
    ) { inner ->
        val scheme = MaterialTheme.colorScheme
        val periodW = 52.dp
        val cellH = 56.dp
        val headerH = 44.dp
        var week by rememberSaveable { mutableIntStateOf(TimetableMock.CURRENT_WEEK) }
        val isCurrentWeek = week == TimetableMock.CURRENT_WEEK
        var selectedBlock by remember { mutableStateOf<CourseBlock?>(null) }

        Column(Modifier.fillMaxSize().padding(inner)) {
        if (listMode) {
            CourseListView(
                week = week,
                onDismissRequest = { },
                onBlockClick = { selectedBlock = it },
            )
            selectedBlock?.let { block ->
                CourseDetailSheet(block = block, week = week, onDismiss = { selectedBlock = null })
            }
            return@Column
        }
        // 周次工具栏（照搬 InstallerX 分段形态；切周只改视图状态，mock 数据不变）
        SegmentedColumn {
            item {
                BaseWidget(
                    icon = "calendar_month",
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
                                SymbolIcon("chevron_backward",
                                    contentDescription = "上一周",
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = { if (week < TimetableMock.TOTAL_WEEKS) week += 1 },
                                enabled = week < TimetableMock.TOTAL_WEEKS,
                            ) {
                                SymbolIcon("chevron_forward",
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
                WeekBody(cellH, TimetableMock.periodTimes.size) { selectedBlock = it }
            }
        }
        }

        selectedBlock?.let { block ->
            CourseDetailSheet(
                block = block,
                week = week,
                onDismiss = { selectedBlock = null },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDetailSheet(block: CourseBlock, week: Int, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val times = TimetableMock.periodTimes
    val startTime = times.getOrNull(block.startNode - 1)?.first ?: ""
    val endTime = times.getOrNull(block.endNode - 1)?.second ?: ""
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(
                block.name,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "第 $week 周 · ${dayNames[block.day - 1]} · 第 ${block.startNode}-${block.endNode} 节",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            DetailRow("时间", if (startTime.isNotEmpty()) "$startTime – $endTime" else "—")
            if (block.location.isNotEmpty()) DetailRow("地点", block.location)
            if (block.teacher.isNotEmpty()) DetailRow("教师", block.teacher)
            DetailRow("节次", TimetableMock.periodTimes.size.let { "第 ${block.startNode}-${block.endNode} 节 / 共 ${it} 节" })
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("关闭") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
    }
}

/** 课程列表视图（ToggleFAB 切换）：按天分组的行式列表，与设置页同构。 */
@Composable
private fun CourseListView(
    week: Int,
    onDismissRequest: () -> Unit,
    onBlockClick: (CourseBlock) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val blocks = TimetableMock.blocks.sortedWith(compareBy({ it.day }, { it.startNode }))
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "第 $week 周 · 共 ${blocks.size} 门课",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.primary,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        blocks.groupBy { it.day }.toSortedMap().forEach { (day, dayBlocks) ->
            item {
                dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn(title = days[day - 1]) {
                    dayBlocks.forEach { b ->
                        item {
                            dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget(
                                icon = "school",
                                title = b.name,
                                description = buildString {
                                    append("第 ${b.startNode}-${b.endNode} 节")
                                    if (b.location.isNotEmpty()) append(" · ${b.location}")
                                    if (b.teacher.isNotEmpty()) append(" · ${b.teacher}")
                                },
                                onClick = { onBlockClick(b) },
                                trailingContent = {
                                    dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon(
                                        "chevron_right",
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                        fontSize = 20.sp,
                                    )
                                },
                            )
                        }
                    }
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
private fun WeekBody(cellH: Dp, nodeCount: Int, onBlockClick: (CourseBlock) -> Unit) {
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
        BoxWithBlocks(cellH, onBlockClick)
    }
}

@Composable
private fun BoxWithBlocks(cellH: Dp, onBlockClick: (CourseBlock) -> Unit) {
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
                    .clickable { onBlockClick(b) }
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
