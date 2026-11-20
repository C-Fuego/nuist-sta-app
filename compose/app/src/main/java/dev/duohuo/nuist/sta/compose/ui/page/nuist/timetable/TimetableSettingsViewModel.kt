// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettings
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 课表设置页的状态来源：[TimetableSettingsRepository]。
 *
 * 设置的持久化在 Dart 侧，这里只把改动转成一次设置更新；课表页与设置页共用同一个
 * 单例镜像，改完立刻重绘，不需要重新拉课表。
 */
class TimetableSettingsViewModel(
    private val repository: TimetableSettingsRepository,
) : ViewModel() {

    val state: StateFlow<TimetableSettings> = repository.state

    private fun set(transform: (TimetableSettings) -> TimetableSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    fun setWeekStartsMonday(value: Boolean) = set { it.copy(weekStartsMonday = value) }

    fun setShowSaturday(value: Boolean) = set { it.copy(showSaturday = value) }

    fun setShowSunday(value: Boolean) = set { it.copy(showSunday = value) }

    fun setShowNonCurrentWeek(value: Boolean) = set { it.copy(showNonCurrentWeek = value) }

    fun setShowLocation(value: Boolean) = set { it.copy(showLocation = value) }

    fun setShowTeacher(value: Boolean) = set { it.copy(showTeacher = value) }

    fun setShowPeriodTimes(value: Boolean) = set { it.copy(showPeriodTimes = value) }

    fun setShowGridLines(value: Boolean) = set { it.copy(showGridLines = value) }

    fun setCellHeightDp(value: Int) = set { it.copy(cellHeightDp = value) }

    fun setFontScale(value: Float) = set { it.copy(fontScale = value) }

    fun setCellOpacity(value: Int) = set { it.copy(cellOpacity = value.coerceIn(30, 100)) }

    fun setHeaderTextSize(value: Int) = set { it.copy(headerTextSize = value.coerceIn(10, 14)) }

    fun setListMode(value: Boolean) = set { it.copy(listMode = value) }

    fun setExamColorArgb(value: Int) = set { it.copy(examColorArgb = value) }

    fun setMarkExperimental(value: Boolean) = set { it.copy(markExperimental = value) }

    fun setMarkParity(value: Boolean) = set { it.copy(markParity = value) }

    fun setMarkExam(value: Boolean) = set { it.copy(markExam = value) }

    fun setShowExams(value: Boolean) = set { it.copy(showExams = value) }

    fun setDayLabelStyle(value: Int) = set { it.copy(dayLabelStyle = value.coerceIn(0, 2)) }

    fun setReminderLeadMinutes(value: Int) = set {
        it.copy(reminderLeadMinutes = TimetableSettings.sanitizeReminderLead(value))
    }

    fun setNextDayReminderEnabled(value: Boolean) = set { it.copy(nextDayReminderEnabled = value) }

    fun setNextDayReminderMinute(value: Int) = set {
        it.copy(nextDayReminderMinute = TimetableSettings.sanitizeNextDayMinute(value))
    }

    fun setShowTomorrowOnHome(value: Boolean) = set { it.copy(showTomorrowOnHome = value) }

    fun setHomeMaxItems(value: Int) = set {
        it.copy(homeMaxItems = value.coerceIn(
            TimetableSettings.HOME_MAX_ITEMS_OPTIONS.first(),
            TimetableSettings.HOME_MAX_ITEMS_OPTIONS.last(),
        ))
    }
}