// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.jwxt.FreeClassroomApi
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 空教室页状态。对应 Flutter 侧 `FreeClassroomController` 的只读状态子集。
 *
 * 数据来自 [FreeClassroomApi]（教务 kxjas 应用）；筛选（时段/楼层/分区/座位）
 * 仍是纯本地过滤，切换日期或教学楼时才发请求。
 */
data class FreeClassroomUiState(
    val buildings: List<Building> = emptyList(),
    val building: Building = Building("", "教学楼"),
    val date: java.time.LocalDate = java.time.LocalDate.now(),
    val day: ClassroomDay? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
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

class FreeClassroomViewModel(private val api: FreeClassroomApi) : ViewModel() {
    private val _state = MutableStateFlow(FreeClassroomUiState())
    val state: StateFlow<FreeClassroomUiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.buildings() }
                .onSuccess { list ->
                    val building = list.firstOrNull()
                    if (building == null) {
                        fail(IOException("教务没有返回教学楼列表"))
                        return@onSuccess
                    }
                    _state.update { it.copy(buildings = list, building = building) }
                    select(building, _state.value.date)
                }
                .onFailure(::fail)
        }
    }

    private var dayJob: Job? = null

    /** 已经查过的 (教学楼, 日期)，回切时直接命中，不再等网络。最多留 8 天。 */
    private val dayCache = object :
        LinkedHashMap<Pair<String, java.time.LocalDate>, ClassroomDay>(8, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Pair<String, java.time.LocalDate>, ClassroomDay>?,
        ): Boolean = size > 8
    }

    /**
     * 切换教学楼 / 日期：先把选择同步落到状态里，筛选弹窗立刻显示新值、立刻关掉，
     * 网络请求在后台跑；命中缓存则连加载态都不出现。
     */
    private fun select(building: Building, date: java.time.LocalDate, force: Boolean = false) {
        // 连续点筛选 / 下拉刷新时只保留最后一次请求，避免旧结果覆盖新结果
        dayJob?.cancel()
        val key = building.code to date
        val cached = if (force) null else dayCache[key]
        _state.update {
            it.copy(
                building = building,
                date = date,
                day = cached,
                loading = cached == null,
                error = null,
                needsCredential = false,
            )
        }
        if (cached != null) return
        dayJob = viewModelScope.launch {
            runCatching { api.day(building, date) }
                .onSuccess { day ->
                    dayCache[key] = day
                    _state.update {
                        if (it.building.code == building.code && it.date == date) {
                            it.copy(day = day, loading = false)
                        } else {
                            it
                        }
                    }
                }
                .onFailure(::fail)
        }
    }

    private fun fail(error: Throwable) {
        val credential = error is PortalException.Credential
        _state.update {
            it.copy(
                loading = false,
                error = error.toUserMessage(),
                needsCredential = credential,
            )
        }
    }

    /** 切换查询日期（真实日历）。 */
    fun selectDate(date: java.time.LocalDate) {
        if (date == _state.value.date) return
        select(_state.value.building, date)
    }

    fun selectBuilding(building: Building) {
        if (building.code == _state.value.building.code) return
        select(building, _state.value.date)
    }

    /** 重新查询当前教学楼（下拉刷新）。 */
    fun refresh() {
        if (_state.value.buildings.isEmpty()) {
            bootstrap()
        } else {
            select(_state.value.building, _state.value.date, force = true)
        }
    }

    fun toggleGroup(index: Int) = _state.update {
        val set = it.selectedGroups.toMutableSet()
        if (!set.add(index)) set.remove(index)
        it.copy(selectedGroups = set)
    }

    fun setFloor(floor: Int?) = _state.update { it.copy(floor = floor) }
    fun setMinSeats(seats: Int) = _state.update { it.copy(minSeats = seats) }
    fun setZone(zone: String?) = _state.update { it.copy(zone = zone) }
    fun setType(type: String?) = _state.update { it.copy(type = type) }
}