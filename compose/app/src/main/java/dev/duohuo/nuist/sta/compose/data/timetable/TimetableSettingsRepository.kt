// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.timetable

import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 课表渲染设置的唯一入口。
 *
 * 设置的持久化与规则都在 Dart 侧（`TimetableSettings` / `settings.json`），这里只保留
 * 一份内存镜像给 Compose 立刻重绘，改动一律回写 Dart 再以返回值覆盖镜像。
 */
class TimetableSettingsRepository(
    private val api: TimetableApi,
    private val local: TimetablePreferences,
) {

    private val _state = MutableStateFlow(
        TimetableSettings().let {
            val stored = local.state.value
            it.copy(
                listMode = stored.listMode,
                markExperimental = stored.markExperimental,
                markParity = stored.markParity,
                markExam = stored.markExam,
                dayLabelStyle = stored.dayLabelStyle,
            )
        },
    )
    val state: StateFlow<TimetableSettings> = _state.asStateFlow()

    /** 用桥返回的状态覆盖镜像（打开课表、同步完、改设置之后都会走这里）。 */
    fun mirror(settings: TimetableSettings) {
        // Dart 不认识纯前端的几项（列表模式、标记开关、日期标签样式），镜像时保留本地值。
        // 首页两项（showTomorrowOnHome / homeMaxItems）由 Dart 持有，直接随返回值覆盖镜像。
        val current = _state.value
        _state.value = settings.copy(
            listMode = current.listMode,
            markExperimental = current.markExperimental,
            markParity = current.markParity,
            markExam = current.markExam,
            dayLabelStyle = current.dayLabelStyle,
        )
    }

    /**
     * 改一项或几项设置：先本地生效保证界面跟手，再把整组字段回写 Dart。
     * 回写失败时把镜像退回 Dart 的真实值，避免本地状态与后端长期不一致。
     */
    suspend fun update(transform: (TimetableSettings) -> TimetableSettings) {
        val next = transform(_state.value)
        _state.value = next
        local.updateLocalOnly(
            listMode = next.listMode,
            markExperimental = next.markExperimental,
            markParity = next.markParity,
            markExam = next.markExam,
            dayLabelStyle = next.dayLabelStyle,
        )
        runCatching { api.updateSettings(next.toPatch()) }
            .onSuccess { mirror(it.settings) }
            .onFailure {
                Timber.w(it, "课表设置回写失败")
                refresh()
            }
    }

    suspend fun refresh() {
        runCatching { api.state() }.onSuccess { mirror(it.settings) }
    }

    /** 只回写 Dart 认识的字段；字号按三档传枚举名。 */
    private fun TimetableSettings.toPatch(): Map<String, Any?> = mapOf(
        "weekStartsOnMonday" to weekStartsMonday,
        "showSaturday" to showSaturday,
        "showSunday" to showSunday,
        "showNonCurrentWeek" to showNonCurrentWeek,
        "showLocation" to showLocation,
        "showTeacher" to showTeacher,
        "showPeriodTimes" to showPeriodTimes,
        "showGridLines" to showGridLines,
        "periodHeight" to cellHeightDp,
        "fontScale" to when {
            fontScale < 0.95f -> "small"
            fontScale > 1.05f -> "large"
            else -> "standard"
        },
        "cellOpacity" to cellOpacity,
        "headerTextSize" to headerTextSize,
        "examColor" to examColorArgb,
        "showExams" to showExams,
        "reminderLeadMinutes" to reminderLeadMinutes,
        "nextDayReminderEnabled" to nextDayReminderEnabled,
        "nextDayReminderMinute" to nextDayReminderMinute,
        "showTomorrowOnHome" to showTomorrowOnHome,
        "homeMaxItems" to homeMaxItems,
    )
}