package dev.duohuo.nuist.sta.compose.features.electricity

import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.duohuo.nuist.sta.compose.core.theme.settings.MiniAppBackButton

/**
 * 宿舍电费页面（Compose 版）。对应 Flutter `ElectricityPage`。
 * UI 优先阶段：剩余电量与历史曲线均为 mock。
 */
@Composable
fun ElectricityScreen(vm: ElectricityViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ElectricityContent(
        state = state,
        history = vm.history(state.rangeDays),
        onTogglePicker = vm::togglePicker,
        onRange = vm::setRange,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ElectricityContent(
    state: ElectricityUiState,
    history: List<ElecReading>,
    onTogglePicker: () -> Unit,
    onRange: (Int) -> Unit,
    onRefresh: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("宿舍电费", modifier = Modifier.padding(start = 12.dp)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            // 组 1：当前余额（数字指标行 + 更换宿舍行，统一 参考实现 卡型）
            dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn(title = "宿舍电费") {
                item {
                    dev.duohuo.nuist.sta.compose.core.theme.settings.BaseItemContainer {
                        val latest = state.latest
                        val valueColor = when {
                            state.isOverdue -> scheme.error
                            state.isLow -> scheme.tertiary
                            else -> scheme.onSurface
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        if (latest == null) "--" else formatKwh(latest.kwh),
                                        style = MaterialTheme.typography.headlineMediumEmphasized,
                                        color = valueColor,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "度",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    state.room?.displayName ?: "未绑定宿舍",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    latest?.let { "更新于 11月16日 13:40" } ?: "尚未查询",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = onRefresh) {
                                SymbolIcon(
                                    "refresh",
                                    contentDescription = "刷新",
                                    tint = scheme.primary,
                                    modifier = Modifier.size(22.dp),
                                    fontSize = 22.sp,
                                )
                            }
                        }
                    }
                }
                item {
                    dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget(
                        icon = "home",
                        title = if (state.room == null) "绑定宿舍" else "更换宿舍",
                        description = "从区域、楼栋、房间列表中选择",
                        onClick = onTogglePicker,
                        trailingContent = { SymbolIcon("expand_more", tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp), fontSize = 20.sp) },
                    )
                }
            }
            if (state.pickerOpen) {
                Text(
                    "（宿舍选择面板：区域 / 楼栋 / 房间级联，UI 优先阶段占位）",
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            // 组 2：历史电量（范围切换 + 折线，同一卡型）
            if (state.room != null) {
                Spacer(Modifier.height(12.dp))
                dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn(title = "历史电量") {
                    item {
                        dev.duohuo.nuist.sta.compose.core.theme.settings.BaseItemContainer {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "近 ${state.rangeDays} 天",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    RangeSelector(state.rangeDays, onRange)
                                }
                                Spacer(Modifier.height(12.dp))
                                HistoryChart(history, scheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(value: Int, onChanged: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceContainerHighest)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (d in listOf(7, 30)) {
            val selected = d == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) scheme.surface else Color.Transparent)
                    .clickable { onChanged(d) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "近 $d 天",
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                )
            }
        }
    }
}
@Composable
private fun HistoryChart(readings: List<ElecReading>, lineColor: Color) {
    val scheme = MaterialTheme.colorScheme
    if (readings.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("暂无历史数据", color = scheme.onSurfaceVariant, fontSize = 12.sp)
        }
        return
    }
    val minV = readings.minOf { it.kwh }
    val maxV = readings.maxOf { it.kwh }
    val span = (maxV - minV).takeIf { it > 0.001 } ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width
        val h = size.height
        val padTop = 8f
        val padBottom = 8f
        val usable = h - padTop - padBottom
        val stepX = if (readings.size > 1) w / (readings.size - 1) else w
        val pts = readings.mapIndexed { i, r ->
            val x = i * stepX
            val y = padTop + usable * (1f - ((r.kwh - minV) / span).toFloat())
            Offset(x, y)
        }
        // 网格基线
        val gridColor = scheme.outlineVariant.copy(alpha = 0.6f)
        for (k in 0..3) {
            val y = padTop + usable * k / 3f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        // 折线
        val path = Path()
        pts.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f))
        // 端点
        pts.forEach { drawCircle(lineColor, radius = 4f, center = it) }
    }
}

private fun formatKwh(v: Double): String = String.format("%.1f", v)
