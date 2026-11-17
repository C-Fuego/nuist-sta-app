package dev.duohuo.nuist.sta.compose.features.electricity

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 电费页状态。对应 Flutter `ElectricityController` 的只读子集。 */
data class ElectricityUiState(
    val room: ElecRoom? = ElecMock.room,
    val latest: ElecReading? = ElecReading(
        java.time.LocalDateTime.of(2026, 11, 16, 13, 40),
        ElecMock.latestKwh,
    ),
    val rangeDays: Int = 7,
    val pickerOpen: Boolean = false,
) {
    val isLow: Boolean get() = (latest?.kwh ?: Double.MAX_VALUE) < ElecMock.lowThreshold
    val isOverdue: Boolean get() = (latest?.kwh ?: 1.0) <= 0.0
}

class ElectricityViewModel : ViewModel() {
    private val _state = MutableStateFlow(ElectricityUiState())
    val state: StateFlow<ElectricityUiState> = _state.asStateFlow()

    fun setRange(days: Int) = _state.update { it.copy(rangeDays = days) }
    fun togglePicker() = _state.update { it.copy(pickerOpen = !it.pickerOpen) }
    fun history(days: Int): List<ElecReading> = ElecMock.history(days)
}
