package dev.duohuo.nuist.sta.compose.features.free_classroom

import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.duohuo.nuist.sta.compose.core.theme.EmptyState
import dev.duohuo.nuist.sta.compose.core.theme.settings.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn
import dev.duohuo.nuist.sta.compose.core.theme.settings.SingleChoiceDialog

/**
 * 空教室页面（Compose + MD3E 版）。对应 Flutter `FreeClassroomPage`。
 *
 * 筛选卡照搬参考实现的 `SegmentedColumn` + `BaseWidget` 分段卡片形态：
 * 教学楼 / 区域 / 楼层 / 日期各一行，前三行点开 M3 单选弹窗，
 * 与「我的 → 主题设置」的交互保持一致。
 */
@Composable
fun FreeClassroomScreen(vm: FreeClassroomViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    FreeClassroomContent(
        state = state,
        onRefresh = vm::refresh,
        onSelectBuilding = vm::selectBuilding,
        onToggleGroup = vm::toggleGroup,
        onFloor = vm::setFloor,
        onZone = vm::setZone,
        onDate = vm::selectDate,
        onMinSeats = vm::setMinSeats,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreeClassroomContent(
    state: FreeClassroomUiState,
    onRefresh: () -> Unit,
    onSelectBuilding: (Building) -> Unit,
    onToggleGroup: (Int) -> Unit,
    onFloor: (Int?) -> Unit,
    onZone: (String?) -> Unit,
    onDate: (java.time.LocalDate) -> Unit,
    onMinSeats: (Int) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("空教室", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton() },
                actions = {
                    IconButton(onClick = onRefresh) {
                        SymbolIcon("refresh", contentDescription = "刷新")
                    }
                },
                                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            var filterSheetOpen by remember { mutableStateOf(false) }
            FilterBar(state = state, onClick = { filterSheetOpen = true })
            if (filterSheetOpen) {
                FilterSheet(
                    state = state,
                    onSelectBuilding = onSelectBuilding,
                    onFloor = onFloor,
                    onZone = onZone,
                    onDate = onDate,
                    onMinSeats = onMinSeats,
                    onDismiss = { filterSheetOpen = false },
                )
            }

            val day = state.day
            if (day == null) {
                EmptyState(
                    title = "选择教学楼后查询",
                    subtitle = "按教学楼与日期查看空闲教室",
                    icon = "meeting_room",
                )
            } else {
                Spacer(Modifier.height(4.dp))
                PeriodHeader(
                    day = day,
                    matched = state.availableCount,
                    selected = state.selectedGroups,
                    onToggleGroup = onToggleGroup,
                )
                val matches = state.matches
                if (state.loading && matches.isEmpty()) {
                    dev.duohuo.nuist.sta.compose.core.theme.ui.LoadingState(
                        text = "正在查询空教室…",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (matches.isEmpty()) {
                    EmptyState(
                        title = "没有符合条件的空教室",
                        subtitle = "试试放宽楼层、区域或时段筛选",
                        icon = "search_off",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(matches, key = { it.name }) { room ->
                            RoomRow(room = room, groups = day.groups)
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 筛选（摘要条 + 底部工作表） ====================

/** 顶部紧凑摘要条：一行概括全部筛选条件，点开底部筛选表。 */
@Composable
private fun FilterBar(state: FreeClassroomUiState, onClick: () -> Unit) {
    val day = state.day
    val title = buildString {
        append(state.building.name)
        append(" · ")
        append(state.zone?.let { "$it 区" } ?: "全部区域")
        append(" · ")
        append(state.floor?.let { if (it == 0) "其他楼层" else "$it 层" } ?: "全部楼层")
    }
    val desc = buildString {
        append(day?.let { "${it.dateLabel} ${it.weekdayName}" } ?: "今天")
        append(" · 第 ")
        append(day?.week ?: "--")
        append(" 周 · ")
        append(if (state.minSeats <= 0) "不限座位" else "≥ ${state.minSeats} 座")
        if (day != null) {
            append(" · 空闲 ")
            append(state.availableCount)
            append(" 间")
        }
    }
    SegmentedColumn(title = "筛选条件") {
        item {
            BaseWidget(
                icon = "tune",
                title = title,
                description = desc,
                onClick = onClick,
                trailingContent = { FilterChevron(loading = state.loading) },
            )
        }
    }
}

/** 底部筛选表：教学楼 / 区域 / 楼层 / 日期 / 座位数（复用原单选弹窗）。 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    state: FreeClassroomUiState,
    onSelectBuilding: (Building) -> Unit,
    onFloor: (Int?) -> Unit,
    onZone: (String?) -> Unit,
    onDate: (java.time.LocalDate) -> Unit,
    onMinSeats: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var showBuildingDialog by remember { mutableStateOf(false) }
    var showZoneDialog by remember { mutableStateOf(false) }
    var showFloorDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showSeatsDialog by remember { mutableStateOf(false) }
    val day = state.day

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        SegmentedColumn(title = "筛选条件") {
            item {
                BaseWidget(
                    icon = "apartment",
                    title = state.building.name,
                    description = "教学楼",
                    onClick = { showBuildingDialog = true },
                    trailingContent = { FilterChevron() },
                )
            }
            item {
                BaseWidget(
                    icon = "grid_view",
                    title = state.zone?.let { "$it 区" } ?: "全部区域",
                    description = "区域",
                    onClick = { showZoneDialog = true },
                    trailingContent = { FilterChevron() },
                )
            }
            item {
                BaseWidget(
                    icon = "layers",
                    title = state.floor?.let { if (it == 0) "其他楼层" else "$it 层" } ?: "全部楼层",
                    description = "楼层",
                    onClick = { showFloorDialog = true },
                    trailingContent = { FilterChevron() },
                )
            }
            item {
                BaseWidget(
                    icon = "calendar_today",
                    title = day?.dateLabel ?: "今天",
                    description = day?.let { "${it.weekdayName} · 第${it.week}周" } ?: "同步后显示",
                    onClick = { showDateDialog = true },
                    trailingContent = { FilterChevron() },
                )
            }
            item {
                BaseWidget(
                    icon = "chair",
                    title = if (state.minSeats <= 0) "不限座位数" else "≥ ${state.minSeats} 座",
                    description = "座位数",
                    onClick = { showSeatsDialog = true },
                    trailingContent = { FilterChevron() },
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
    }

    if (showBuildingDialog) {
        SingleChoiceDialog(
            title = "选择教学楼",
            items = state.buildings.map { it.name },
            selectedIndex = state.buildings.indexOfFirst { it.code == state.building.code },
            onDismiss = { showBuildingDialog = false },
            onSelect = { index ->
                showBuildingDialog = false
                state.buildings.getOrNull(index)?.let(onSelectBuilding)
            },
        )
    }
    if (showZoneDialog) {
        val zones = day?.zones.orEmpty()
        SingleChoiceDialog(
            title = "区域",
            items = listOf("全部区域") + zones.map { "$it 区" },
            selectedIndex = if (state.zone == null) 0 else zones.indexOf(state.zone) + 1,
            onDismiss = { showZoneDialog = false },
            onSelect = { index ->
                showZoneDialog = false
                onZone(if (index == 0) null else zones.getOrNull(index - 1))
            },
        )
    }
    if (showFloorDialog) {
        val floors = day?.floors.orEmpty()
        SingleChoiceDialog(
            title = "楼层",
            items = listOf("全部楼层") + floors.map { if (it == 0) "其他" else "$it 层" },
            selectedIndex = if (state.floor == null) 0 else floors.indexOf(state.floor) + 1,
            onDismiss = { showFloorDialog = false },
            onSelect = { index ->
                showFloorDialog = false
                onFloor(if (index == 0) null else floors.getOrNull(index - 1))
            },
        )
    }
    if (showDateDialog) {
        val dates = upcomingDates(14)
        val current = dates.indexOfFirst { it.date == state.date }.coerceAtLeast(0)
        SingleChoiceDialog(
            title = "查询日期",
            items = dates.map { d ->
                (if (d.isToday) "今天 · " else "") + "${d.label} ${d.weekday} · 第${d.week}周"
            },
            selectedIndex = current,
            onDismiss = { showDateDialog = false },
            onSelect = { index ->
                showDateDialog = false
                dates.getOrNull(index)?.let { onDate(it.date) }
            },
        )
    }
    if (showSeatsDialog) {
        val options = listOf(0, 30, 60, 90)
        SingleChoiceDialog(
            title = "座位数",
            items = options.map { if (it == 0) "不限" else "≥ $it 座" },
            selectedIndex = options.indexOf(state.minSeats).coerceAtLeast(0),
            onDismiss = { showSeatsDialog = false },
            onSelect = { index ->
                showSeatsDialog = false
                options.getOrNull(index)?.let(onMinSeats)
            },
        )
    }
}

@Composable
private fun FilterChevron(loading: Boolean = false) {
    if (loading) {
        CircularWavyProgressIndicator(modifier = Modifier.size(18.dp))
    } else {
        SymbolIcon("chevron_right",
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ==================== 时段表头 ====================

@Composable
private fun PeriodHeader(
    day: ClassroomDay,
    matched: Int,
    selected: Set<Int>,
    onToggleGroup: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(scheme.surfaceContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${day.groups.size} 个大节 · 空闲 $matched 间",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "教室",
                modifier = Modifier.weight(ROOM_INFO_WEIGHT),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            // 时段列：与教室行格子共用同一权重分配，天然对齐且窄屏不会被折叠
            Row(Modifier.weight(PERIOD_COLUMNS_WEIGHT)) {
                day.groups.forEachIndexed { i, g ->
                    val on = selected.contains(i)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) scheme.primary else scheme.surfaceContainerHighest)
                            .clickable { onToggleGroup(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            g.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (on) scheme.onPrimary else scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }
}

/** 教室信息列 / 时段列 的宽度权重（表头与数据行共用，保证逐列对齐）。 */
private const val ROOM_INFO_WEIGHT = 0.42f
private const val PERIOD_COLUMNS_WEIGHT = 0.58f

// ==================== 教室行 ====================

@Composable
private fun RoomRow(room: Classroom, groups: List<PeriodGroup>) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(ROOM_INFO_WEIGHT)) {
            Text(
                "${room.zone} 区 · ${room.name}",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )
            Text(
                "${room.seats} 座",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        Row(Modifier.weight(PERIOD_COLUMNS_WEIGHT)) {
            groups.forEachIndexed { i, _ ->
                val occupied = room.periods.getOrNull(i)?.occupied ?: false
                // 空 = 可用（primary 强调）；满 = 占用（弱化的中性色）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (occupied) scheme.surfaceContainerHighest
                            else scheme.primaryContainer,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (occupied) "满" else "空",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (occupied) scheme.onSurfaceVariant.copy(alpha = 0.75f)
                        else scheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
