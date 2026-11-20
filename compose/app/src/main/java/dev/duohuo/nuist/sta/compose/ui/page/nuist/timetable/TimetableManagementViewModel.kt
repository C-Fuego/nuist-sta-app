// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.lifecycle.ViewModel
import dev.duohuo.nuist.sta.compose.data.timetable.DaySwapEntry
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableApi
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableChangeLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** 整天调课页的界面状态；学期首末日同时是日期选择器的上下界。 */
data class DaySwapUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val semesterFirst: LocalDate? = null,
    val semesterLast: LocalDate? = null,
    val today: LocalDate? = null,
    val swaps: List<DaySwapEntry> = emptyList(),
)

/**
 * 课表设置「课表管理」三项的读写入口。
 *
 * 能不能换、换完怎么记、恢复清掉什么，规则全在 Dart 侧；这里只搬运结构化结果
 * 和那句可以直接展示的话，不在前端重算。
 */
class TimetableManagementViewModel(private val api: TimetableApi) : ViewModel() {

    private val _daySwaps = MutableStateFlow(DaySwapUiState())
    val daySwaps: StateFlow<DaySwapUiState> = _daySwaps.asStateFlow()

    private val _changeLog = MutableStateFlow<TimetableChangeLog?>(null)
    val changeLog: StateFlow<TimetableChangeLog?> = _changeLog.asStateFlow()

    private val _changeLogLoading = MutableStateFlow(false)
    val changeLogLoading: StateFlow<Boolean> = _changeLogLoading.asStateFlow()

    /** 最近一次动作的一句话结果，页面弹完 Snackbar 后调 [clearMessage] 清空。 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    suspend fun loadDaySwaps() {
        _daySwaps.value = _daySwaps.value.copy(loading = true, failed = false)
        val data = runCatching { api.daySwaps() }.getOrNull()
        _daySwaps.value = if (data == null) {
            _daySwaps.value.copy(loading = false, failed = true)
        } else {
            DaySwapUiState(
                loading = false,
                semesterFirst = data.semesterFirst,
                semesterLast = data.semesterLast,
                today = data.today,
                swaps = data.swaps,
            )
        }
    }

    /** 新增一次互换；返回是否成功，[message] 里是可直接展示的一句话。 */
    suspend fun addDaySwap(a: LocalDate, b: LocalDate): Boolean {
        val result = runCatching { api.addDaySwap(a, b) }.getOrNull()
        if (result == null) {
            _message.value = "调课失败"
            return false
        }
        _daySwaps.value = _daySwaps.value.copy(swaps = result.swaps)
        _message.value = result.message.ifEmpty { if (result.ok) "已互换" else "调课失败" }
        return result.ok
    }

    /** 撤销一条调课记录；不存在时不会删任何东西。 */
    suspend fun removeDaySwap(id: String): Boolean {
        val result = runCatching { api.removeDaySwap(id) }.getOrNull()
        if (result == null) {
            _message.value = "恢复失败"
            return false
        }
        _daySwaps.value = _daySwaps.value.copy(swaps = result.swaps)
        _message.value = result.message.ifEmpty { if (result.ok) "已恢复" else "恢复失败" }
        return result.ok
    }

    suspend fun loadChangeLog() {
        _changeLogLoading.value = true
        _changeLog.value = runCatching { api.changeLog() }.getOrNull()
        _changeLogLoading.value = false
    }

    /** 恢复教务课程：`true` 表示清掉了东西，`false` 表示没有可恢复的，`null` 表示失败。 */
    suspend fun restoreAllJwxt(): Boolean? = runCatching { api.restoreAllJwxt() }.getOrNull()
}