package dev.duohuo.nuist.sta.compose.ui.page.nuist.electricity

import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
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
import org.koin.androidx.compose.koinViewModel
import dev.duohuo.nuist.sta.compose.ui.kit.BaseItemContainer
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.BlankStateArea
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorBanner
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorState
import dev.duohuo.nuist.sta.compose.ui.kit.LoadingState
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.LocalRefreshFromPull
import dev.duohuo.nuist.sta.compose.ui.kit.NuistPullToRefreshBox
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog

/**
 * 宿舍电费页面（Compose 版）。对应 Flutter `ElectricityPage`。
 * UI 优先阶段：剩余电量与历史曲线均为 mock。
 */
@Composable
fun ElectricityScreen(vm: ElectricityViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ElectricityContent(
        state = state,
        history = state.history,
        onOpenPicker = vm::openPicker,
        onRange = vm::setRange,
        onRefresh = vm::refresh,
    )
    val stage = state.pickerStage
    if (stage == PickerStage.Campus) {
        SingleChoiceDialog(
            title = "选择区域",
            items = state.campuses.map { it.name },
            selectedIndex = state.campuses.indexOfFirst { it.value == state.selectedCampus?.value },
            loading = state.loadingOptions,
            emptyText = "暂无选项",
            onDismiss = vm::closePicker,
            onSelect = { index -> state.campuses.getOrNull(index)?.let(vm::selectCampus) },
        )
    }
    if (stage == PickerStage.Building) {
        SingleChoiceDialog(
            title = "${state.selectedCampus?.name.orEmpty()} · 选择楼栋",
            items = state.buildings.map { it.name },
            selectedIndex = state.buildings.indexOfFirst { it.value == state.selectedBuilding?.value },
            loading = state.loadingOptions,
            emptyText = "暂无选项",
            onDismiss = vm::closePicker,
            onSelect = { index -> state.buildings.getOrNull(index)?.let(vm::selectBuilding) },
        )
    }
    if (stage == PickerStage.Room) {
        SingleChoiceDialog(
            title = "${state.selectedBuilding?.name.orEmpty()} · 选择房间",
            items = state.rooms.map { it.name },
            selectedIndex = state.rooms.indexOfFirst { it.value == state.room?.room?.value },
            loading = state.loadingOptions,
            emptyText = "暂无选项",
            onDismiss = vm::closePicker,
            onSelect = { index -> state.rooms.getOrNull(index)?.let(vm::selectRoom) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ElectricityContent(
    state: ElectricityUiState,
    history: List<ElecReading>,
    onOpenPicker: () -> Unit,
    onRange: (Int) -> Unit,
    onRefresh: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val pristine = state.latest == null && state.room == null
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
        NuistPullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            when {
                pristine && state.loading && !LocalRefreshFromPull.current -> LoadingState("正在查询电费…")

                pristine && state.error != null -> BlankStateArea {
                    ErrorState(
                        message = state.error.orEmpty(),
                        title = if (state.needsCredential) "需要登录统一门户" else "电费查询失败",
                        needsCredential = state.needsCredential,
                        onRetry = onRefresh,
                    )
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 12.dp, bottom = 16.dp),
                ) {
                    // 组 1：当前余额（数字指标行 + 更换宿舍行，统一 参考实现 卡型）
                    SegmentedColumn(title = "宿舍电费") {
                        item {
                            BaseItemContainer {
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
                                            latest?.let { "更新于 ${formationTime(it.time)}" }
                                                ?: if (state.loading) "正在查询…" else "尚未查询",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            BaseWidget(
                                icon = "home",
                                title = if (state.room == null) "绑定宿舍" else "更换宿舍",
                                description = "从区域、楼栋、房间列表中选择",
                                onClick = onOpenPicker,
                                trailingContent = {
                                    SymbolIcon(
                                        "expand_more",
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                        fontSize = 20.sp,
                                    )
                                },
                            )
                        }
                    }
                    state.error?.let { message ->
                        Spacer(Modifier.height(12.dp))
                        ErrorBanner(message = message, onRetry = onRefresh)
                    }
                    // 组 2：历史电量（范围切换 + 折线，同一卡型）
                    if (state.room != null) {
                        Spacer(Modifier.height(12.dp))
                        SegmentedColumn(title = "历史电量") {
                            item {
                                BaseItemContainer {
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

private fun formationTime(time: java.time.LocalDateTime): String =
    String.format("%d月%d日 %02d:%02d", time.monthValue, time.dayOfMonth, time.hour, time.minute)
