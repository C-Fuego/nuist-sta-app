package dev.duohuo.nuist.sta.compose.features.free_classroom

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
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
 * 筛选卡照搬 InstallerX 的 `SegmentedColumn` + `BaseWidget` 分段卡片形态：
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
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("空教室") },
                subtitle = { Text("按教学楼与时段查空闲教室") },
                navigationIcon = { MiniAppBackButton() },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            FilterCard(state, onSelectBuilding, onFloor, onZone)

            val day = state.day
            if (day == null) {
                EmptyState(
                    title = "选择教学楼后查询",
                    subtitle = "按教学楼与日期查看空闲教室",
                    icon = Icons.Filled.MeetingRoom,
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
                if (matches.isEmpty()) {
                    EmptyState(
                        title = "没有符合条件的空教室",
                        subtitle = "试试放宽楼层、区域或时段筛选",
                        icon = Icons.Filled.SearchOff,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(matches, key = { it.name }) { room ->
                            RoomRow(room = room, groups = day.groups)
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 筛选卡（SegmentedColumn + BaseWidget） ====================

@Composable
private fun FilterCard(
    state: FreeClassroomUiState,
    onSelectBuilding: (Building) -> Unit,
    onFloor: (Int?) -> Unit,
    onZone: (String?) -> Unit,
) {
    var showBuildingDialog by remember { mutableStateOf(false) }
    var showZoneDialog by remember { mutableStateOf(false) }
    var showFloorDialog by remember { mutableStateOf(false) }
    val day = state.day

    val buildingSubtitle = when {
        day != null -> "${day.dateLabel} ${day.weekdayName} · 第${day.week}周 · 已同步"
        state.loading -> "正在查询…"
        else -> "点击选择要查询的教学楼"
    }

    SegmentedColumn(title = "查询条件") {
        item {
            BaseWidget(
                icon = Icons.Filled.Apartment,
                title = state.building.name,
                description = buildingSubtitle,
                trailingContent = { FilterChevron(loading = state.loading) },
                onClick = { showBuildingDialog = true },
            )
        }
        item {
            BaseWidget(
                icon = Icons.Filled.GridView,
                title = state.zone?.let { "$it 区" } ?: "全部区域",
                description = "区域",
                trailingContent = { FilterChevron() },
                onClick = { showZoneDialog = true },
            )
        }
        item {
            BaseWidget(
                icon = Icons.Filled.Layers,
                title = state.floor?.let { if (it == 0) "其他楼层" else "$it 层" } ?: "全部楼层",
                description = "楼层",
                trailingContent = { FilterChevron() },
                onClick = { showFloorDialog = true },
            )
        }
        item {
            BaseWidget(
                icon = Icons.Filled.CalendarToday,
                title = day?.dateLabel ?: "今天",
                description = day?.let { "${it.weekdayName} · 第${it.week}周" } ?: "同步后显示",
            )
        }
    }

    // ---- 单选弹窗（M3 AlertDialog + RadioButton，和主题设置页同一套） ----
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
}

@Composable
private fun FilterChevron(loading: Boolean = false) {
    if (loading) {
        CircularWavyProgressIndicator(modifier = Modifier.size(18.dp))
    } else {
        Icon(
            Icons.Filled.ChevronRight,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${day.groups.size} 个大节 · 空闲 $matched 间",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "教室",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            day.groups.forEachIndexed { i, g ->
                val on = selected.contains(i)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(36.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (on) scheme.primary else scheme.surfaceContainerHighest)
                        .clickable { onToggleGroup(i) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        g.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (on) scheme.onPrimary else scheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }
}

// ==================== 教室行 ====================

@Composable
private fun RoomRow(room: Classroom, groups: List<PeriodGroup>) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
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
        groups.forEachIndexed { i, _ ->
            val occupied = room.periods.getOrNull(i)?.occupied ?: false
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(36.dp)
                    .height(32.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (occupied) scheme.primaryContainer
                        else scheme.surfaceContainerHighest,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (occupied) "满" else "空",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (occupied) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                )
            }
        }
    }
}
