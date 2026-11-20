// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.page.nuist.electricity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.electricity.ElecBinding
import dev.duohuo.nuist.sta.compose.data.electricity.ElecOption
import dev.duohuo.nuist.sta.compose.data.electricity.ElectricityApi
import dev.duohuo.nuist.sta.compose.data.electricity.ElectricityStore
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** 宿舍选择面板当前停在哪一级。 */
enum class PickerStage { Closed, Campus, Building, Room }

/** 电费页状态。 */
data class ElectricityUiState(
    val room: ElecBinding? = null,
    val latest: ElecReading? = null,
    val history: List<ElecReading> = emptyList(),
    val rangeDays: Int = 7,
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
    val pickerStage: PickerStage = PickerStage.Closed,
    val campuses: List<ElecOption> = emptyList(),
    val buildings: List<ElecOption> = emptyList(),
    val rooms: List<ElecOption> = emptyList(),
    val selectedCampus: ElecOption? = null,
    val selectedBuilding: ElecOption? = null,
    val loadingOptions: Boolean = false,
) {
    val isLow: Boolean get() = (latest?.kwh ?: Double.MAX_VALUE) < ElecMock.lowThreshold
    val isOverdue: Boolean get() = (latest?.kwh ?: 1.0) <= 0.0
}

class ElectricityViewModel(
    private val api: ElectricityApi,
    private val store: ElectricityStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ElectricityUiState(room = store.read()))
    val state: StateFlow<ElectricityUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setRange(days: Int) = _state.update { it.copy(rangeDays = days) }

    fun consumeError() = _state.update { it.copy(error = null) }

    /** 有绑定就查一次余额。 */
    fun refresh() {
        val binding = _state.value.room ?: return
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, needsCredential = false) }
            try {
                val kwh = api.balance(binding)
                val reading = ElecReading(LocalDateTime.now(), kwh)
                _state.update {
                    it.copy(
                        loading = false,
                        latest = reading,
                        history = (it.history + reading).takeLast(MAX_HISTORY),
                    )
                }
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    // ==================== 宿舍选择 ====================

    fun openPicker() {
        _state.update {
            it.copy(
                pickerStage = PickerStage.Campus,
                campuses = it.campuses,
                error = null,
            )
        }
        if (_state.value.campuses.isEmpty()) loadCampuses()
    }

    fun closePicker() = _state.update { it.copy(pickerStage = PickerStage.Closed) }

    fun selectCampus(option: ElecOption) {
        _state.update {
            it.copy(
                selectedCampus = option,
                selectedBuilding = null,
                buildings = emptyList(),
                rooms = emptyList(),
                pickerStage = PickerStage.Building,
            )
        }
        loadBuildings(option)
    }

    fun selectBuilding(option: ElecOption) {
        _state.update {
            it.copy(
                selectedBuilding = option,
                rooms = emptyList(),
                pickerStage = PickerStage.Room,
            )
        }
        loadRooms(option)
    }

    fun selectRoom(option: ElecOption) {
        val current = _state.value
        val campus = current.selectedCampus ?: return
        val building = current.selectedBuilding ?: return
        val binding = ElecBinding(campus, building, option)
        store.save(binding)
        _state.update {
            it.copy(room = binding, pickerStage = PickerStage.Closed, latest = null, history = emptyList())
        }
        refresh()
    }

    private fun loadCampuses() = options { api.campuses() }

    private fun loadBuildings(campus: ElecOption) = options { api.buildings(campus) }

    private fun loadRooms(building: ElecOption) = options {
        val campus = _state.value.selectedCampus ?: return@options emptyList()
        api.rooms(campus, building)
    }

    private fun options(loader: suspend () -> List<ElecOption>) {
        viewModelScope.launch {
            _state.update { it.copy(loadingOptions = true, error = null, needsCredential = false) }
            try {
                val items = loader()
                _state.update {
                    when (it.pickerStage) {
                        PickerStage.Campus -> it.copy(campuses = items, loadingOptions = false)
                        PickerStage.Building -> it.copy(buildings = items, loadingOptions = false)
                        PickerStage.Room -> it.copy(rooms = items, loadingOptions = false)
                        PickerStage.Closed -> it.copy(loadingOptions = false)
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loadingOptions = false) }
                fail(e)
            }
        }
    }

    private fun fail(e: Exception) {
        _state.update {
            it.copy(
                loading = false,
                error = e.toUserMessage(),
                needsCredential = e is PortalException.Credential,
            )
        }
    }

    private companion object {
        const val MAX_HISTORY = 60
    }
}