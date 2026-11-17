package dev.duohuo.nuist.sta.compose.features.free_classroom

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 空教室页状态。对应 Flutter 侧 `FreeClassroomController` 的只读状态子集。
 *
 * 说明（UI 阶段）：数据来自 [FreeClassroomMock]；筛选（时段/楼层/分区/类型/座位）
 * 是纯本地过滤，不发请求。后续接真实 API 时替换 [load] 的数据来源即可。
 */
data class FreeClassroomUiState(
    val buildings: List<Building> = FreeClassroomMock.buildings,
    val building: Building = FreeClassroomMock.buildings.first(),
    val day: ClassroomDay? = null,
    val loading: Boolean = false,
    val selectedGroups: Set<Int> = emptySet(),
    val floor: Int? = null,
    val zone: String? = null,
    val type: String? = null,
    val minSeats: Int = 0,
) {
    /** 应用筛选后的教室列表：只显示所选时段全空的教室。 */
    val matches: List<Classroom>
        get() {
            val d = day ?: return emptyList()
            val sel = selectedGroups
            return d.rooms.filter { room ->
                (floor == null || room.floor == floor) &&
                    (zone == null || room.zone == zone) &&
                    (minSeats == 0 || room.seats >= minSeats) &&
                    (sel.isEmpty() || sel.all { i -> !room.periods[i].occupied })
            }
        }

    val availableCount: Int get() = matches.count { !it.fullyBusy }
}

class FreeClassroomViewModel : ViewModel() {
    private val _state = MutableStateFlow(FreeClassroomUiState())
    val state: StateFlow<FreeClassroomUiState> = _state.asStateFlow()

    init {
        load(_state.value.building)
    }

    private fun load(building: Building) {
        _state.update { it.copy(loading = true) }
        val day = FreeClassroomMock.day(building)
        _state.update {
            it.copy(building = building, day = day, loading = false)
        }
    }

    fun selectBuilding(building: Building) = load(building)

    /** 重新查询当前教学楼（顶栏刷新按钮）。 */
    fun refresh() = load(_state.value.building)

    fun toggleGroup(index: Int) = _state.update {
        val set = it.selectedGroups.toMutableSet()
        if (!set.add(index)) set.remove(index)
        it.copy(selectedGroups = set)
    }

    fun setFloor(floor: Int?) = _state.update { it.copy(floor = floor) }
    fun setZone(zone: String?) = _state.update { it.copy(zone = zone) }
    fun setType(type: String?) = _state.update { it.copy(type = type) }
    fun setMinSeats(seats: Int) = _state.update { it.copy(minSeats = seats) }
}
