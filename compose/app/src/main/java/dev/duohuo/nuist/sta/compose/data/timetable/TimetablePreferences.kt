// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.timetable

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 课表渲染设置。
 *
 * 只影响本地呈现（列顺序、格子内容、字号高度、考试色），不改动教务处下发的数据；
 * 默认值与参考实现的外观页保持一致。
 */
data class TimetableSettings(
    /** 一周从周一还是周日开始（只改列顺序）。 */
    val weekStartsMonday: Boolean = true,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = true,
    /** 非本周的课程：关闭时直接隐藏，开启时半透明显示。 */
    val showNonCurrentWeek: Boolean = false,
    val showLocation: Boolean = true,
    val showTeacher: Boolean = false,
    /** 左侧节次栏是否显示每节的上课时间。 */
    val showPeriodTimes: Boolean = true,
    val showGridLines: Boolean = true,
    /** 周视图每一节的行高（48 紧凑 / 56 标准 / 64 宽松 / 72、80 更宽松）。 */
    val cellHeightDp: Int = 72,
    /** 课程块字号缩放（0.9 小 / 1.0 标准 / 1.15 大，与参考实现同档）。 */
    val fontScale: Float = 1f,
    /** 课程块底色不透明度 30–100（只影响底色，文字不透明）。 */
    val cellOpacity: Int = 100,
    /** 周视图表头字号 10–14。 */
    val headerTextSize: Int = 11,
    val listMode: Boolean = false,
    /** 考试颜色，ARGB。与参考实现的 `examColor` 默认值一致。 */
    val examColorArgb: Int = 0xFFC62828.toInt(),
    /** 格内是否标「实验」胶囊（教务 SFSYK 实验时段）。 */
    val markExperimental: Boolean = true,
    /** 格内是否标「单周 / 双周」胶囊。 */
    val markParity: Boolean = true,
    /** 格内是否标「考试」胶囊。 */
    val markExam: Boolean = true,
    /** 周视图里是否画出考试安排（关闭时整块隐藏，与参考实现的 showExams 一致）。 */
    val showExams: Boolean = true,
    /** 列表视图里「星期几」的写法：0 简略「三」、1「周三」、2「星期三」。 */
    val dayLabelStyle: Int = 0,
    /** 上课提醒提前量（分钟），只允许 [REMINDER_LEAD_OPTIONS]。 */
    val reminderLeadMinutes: Int = 15,
    /** 明日课程提醒总开关（时间点由 [nextDayReminderMinute] 决定）。 */
    val nextDayReminderEnabled: Boolean = true,
    /** 明日课程提醒的推送时刻，本地分钟数。 */
    val nextDayReminderMinute: Int = 22 * 60,
    /** 首页「今日概览」的课表卡片是否展示明天的课；由 Dart 侧持久化。 */
    val showTomorrowOnHome: Boolean = true,
    /** 首页课表卡片最多列出的课程条数（1–4）；由 Dart 侧持久化。 */
    val homeMaxItems: Int = DEFAULT_HOME_MAX_ITEMS,
) {
    companion object {
        /** 与 Dart 侧 `kDefaultHomeMaxItems` 对齐。 */
        const val DEFAULT_HOME_MAX_ITEMS = 2

        /** 首页课表卡片可选的最大条数，与 Dart 侧 `sanitizeHomeMaxItems` 的取值域一致。 */
        val HOME_MAX_ITEMS_OPTIONS = listOf(1, 2, 3, 4)

        /** 与 Dart 侧 `reminderLeadOptions` 对齐。 */
        val REMINDER_LEAD_OPTIONS = listOf(5, 10, 15, 30)

        /** 与 Dart 侧 `nextDayReminderOptions` 对齐，21:00–23:30 每半小时。 */
        val NEXT_DAY_MINUTE_OPTIONS = listOf(1260, 1290, 1320, 1350, 1380, 1410)

        fun sanitizeReminderLead(value: Int): Int =
            value.takeIf { it in REMINDER_LEAD_OPTIONS } ?: 15

        fun sanitizeNextDayMinute(value: Int): Int =
            value.takeIf { it in NEXT_DAY_MINUTE_OPTIONS } ?: 22 * 60
    }
}

/**
 * 课表设置的本地持久化。
 *
 * 键前缀 `timetable.*`，与主题设置一样只存 SharedPreferences（不入云端）。
 * 读失败时退化成默认值，不抛异常。
 */
class TimetablePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<TimetableSettings> = _state.asStateFlow()

    fun update(transform: (TimetableSettings) -> TimetableSettings) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putBoolean(KEY_WEEK_STARTS_MONDAY, next.weekStartsMonday)
            .putBoolean(KEY_SHOW_SATURDAY, next.showSaturday)
            .putBoolean(KEY_SHOW_SUNDAY, next.showSunday)
            .putBoolean(KEY_SHOW_NON_CURRENT_WEEK, next.showNonCurrentWeek)
            .putBoolean(KEY_SHOW_LOCATION, next.showLocation)
            .putBoolean(KEY_SHOW_TEACHER, next.showTeacher)
            .putBoolean(KEY_SHOW_PERIOD_TIMES, next.showPeriodTimes)
            .putBoolean(KEY_SHOW_GRID_LINES, next.showGridLines)
            .putInt(KEY_CELL_HEIGHT, next.cellHeightDp)
            .putFloat(KEY_FONT_SCALE, next.fontScale)
            .putBoolean(KEY_LIST_MODE, next.listMode)
            .putInt(KEY_EXAM_COLOR, next.examColorArgb)
            .putBoolean(KEY_MARK_EXPERIMENTAL, next.markExperimental)
            .putBoolean(KEY_MARK_PARITY, next.markParity)
            .putBoolean(KEY_MARK_EXAM, next.markExam)
            .putBoolean(KEY_SHOW_EXAMS, next.showExams)
            .putInt(KEY_DAY_LABEL_STYLE, next.dayLabelStyle)
            .apply()
    }

    /** 只保存纯前端的几项（列表模式、标记开关、日期标签样式），Dart 侧不认识这些字段。 */
    fun updateLocalOnly(
        listMode: Boolean,
        markExperimental: Boolean,
        markParity: Boolean,
        markExam: Boolean,
        dayLabelStyle: Int,
    ) {
        val next = _state.value.copy(
            listMode = listMode,
            markExperimental = markExperimental,
            markParity = markParity,
            markExam = markExam,
            dayLabelStyle = dayLabelStyle,
        )
        _state.value = next
        prefs.edit()
            .putBoolean(KEY_LIST_MODE, next.listMode)
            .putBoolean(KEY_MARK_EXPERIMENTAL, next.markExperimental)
            .putBoolean(KEY_MARK_PARITY, next.markParity)
            .putBoolean(KEY_MARK_EXAM, next.markExam)
            .putInt(KEY_DAY_LABEL_STYLE, next.dayLabelStyle)
            .apply()
    }

    /**
     * 旧版本最高只有 64dp，选到顶的用户其实还想更宽松：
     * 首次读到新版时把「原本选在旧上限」的取值往上一档（64 → 72），其余保持不变。
     */
    private fun readCellHeight(defaults: TimetableSettings): Int {
        if (!prefs.contains(KEY_CELL_HEIGHT)) return defaults.cellHeightDp
        val stored = prefs.getInt(KEY_CELL_HEIGHT, defaults.cellHeightDp)
        val migrated = if (!prefs.contains(KEY_CELL_HEIGHT_VERSION) && stored >= 64) 72 else stored
        if (!prefs.contains(KEY_CELL_HEIGHT_VERSION)) {
            prefs.edit()
                .putInt(KEY_CELL_HEIGHT, migrated)
                .putInt(KEY_CELL_HEIGHT_VERSION, 1)
                .apply()
        }
        return migrated.coerceIn(CELL_HEIGHTS.first()..CELL_HEIGHTS.last())
    }

    private fun read(): TimetableSettings {
        val defaults = TimetableSettings()
        return try {
            TimetableSettings(
                weekStartsMonday = prefs.getBoolean(KEY_WEEK_STARTS_MONDAY, defaults.weekStartsMonday),
                showSaturday = prefs.getBoolean(KEY_SHOW_SATURDAY, defaults.showSaturday),
                showSunday = prefs.getBoolean(KEY_SHOW_SUNDAY, defaults.showSunday),
                showNonCurrentWeek = prefs.getBoolean(KEY_SHOW_NON_CURRENT_WEEK, defaults.showNonCurrentWeek),
                showLocation = prefs.getBoolean(KEY_SHOW_LOCATION, defaults.showLocation),
                showTeacher = prefs.getBoolean(KEY_SHOW_TEACHER, defaults.showTeacher),
                showPeriodTimes = prefs.getBoolean(KEY_SHOW_PERIOD_TIMES, defaults.showPeriodTimes),
                showGridLines = prefs.getBoolean(KEY_SHOW_GRID_LINES, defaults.showGridLines),
                cellHeightDp = readCellHeight(defaults),
                fontScale = prefs.getFloat(KEY_FONT_SCALE, defaults.fontScale)
                    .coerceIn(FONT_SCALES.first(), FONT_SCALES.last()),
                listMode = prefs.getBoolean(KEY_LIST_MODE, defaults.listMode),
                examColorArgb = prefs.getInt(KEY_EXAM_COLOR, defaults.examColorArgb),
                markExperimental = prefs.getBoolean(KEY_MARK_EXPERIMENTAL, defaults.markExperimental),
                markParity = prefs.getBoolean(KEY_MARK_PARITY, defaults.markParity),
                markExam = prefs.getBoolean(KEY_MARK_EXAM, defaults.markExam),
                showExams = prefs.getBoolean(KEY_SHOW_EXAMS, defaults.showExams),
                dayLabelStyle = prefs.getInt(KEY_DAY_LABEL_STYLE, defaults.dayLabelStyle).coerceIn(0, 2),
            )
        } catch (_: Exception) {
            defaults
        }
    }

    companion object {
        const val PREF_NAME = "timetable_settings"
        /** 外观页可选的格子高度。 */
        val CELL_HEIGHTS = listOf(48, 56, 64, 72, 80)
        /** 外观页可选的格子字号缩放。 */
        val FONT_SCALES = listOf(0.85f, 1f, 1.15f)

        private const val KEY_WEEK_STARTS_MONDAY = "timetable.weekStartsMonday"
        private const val KEY_SHOW_SATURDAY = "timetable.showSaturday"
        private const val KEY_SHOW_SUNDAY = "timetable.showSunday"
        private const val KEY_SHOW_NON_CURRENT_WEEK = "timetable.showNonCurrentWeek"
        private const val KEY_SHOW_LOCATION = "timetable.showLocation"
        private const val KEY_SHOW_TEACHER = "timetable.showTeacher"
        private const val KEY_SHOW_PERIOD_TIMES = "timetable.showPeriodTimes"
        private const val KEY_SHOW_GRID_LINES = "timetable.showGridLines"
        private const val KEY_CELL_HEIGHT = "timetable.cellHeightDp"
        private const val KEY_CELL_HEIGHT_VERSION = "timetable.cellHeightVersion"
        private const val KEY_FONT_SCALE = "timetable.fontScale"
        private const val KEY_LIST_MODE = "timetable.listMode"
        private const val KEY_EXAM_COLOR = "timetable.examColor"
        private const val KEY_MARK_EXPERIMENTAL = "timetable.markExperimental"
        private const val KEY_MARK_PARITY = "timetable.markParity"
        private const val KEY_MARK_EXAM = "timetable.markExam"
        private const val KEY_SHOW_EXAMS = "timetable.showExams"
        private const val KEY_DAY_LABEL_STYLE = "timetable.dayLabelStyle"
    }
}