// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.timetable

import kotlinx.serialization.Serializable

/**
 * 课表的全局状态。学期、当前周、总周数、同步态全部由 Dart 侧的控制器给出，
 * 这一层只搬运，不在 Kotlin 里重算任何规则。
 */
data class TimetableState(
    val termCode: String,
    val termLabel: String,
    val termStartDate: String?,
    val currentWeek: Int,
    val viewingWeek: Int,
    val totalWeeks: Int,
    val currentDay: Int,
    val lastPeriod: Int,
    val portalBound: Boolean,
    val loading: Boolean,
    val hasCourses: Boolean,
    val weekPending: Boolean,
    val syncStatus: String,
    val syncFailure: String,
    val statusMessage: String?,
    val periods: List<Pair<String, String>>,
    /** 渲染设置，唯一来源是 Dart 侧 `TimetableSettings`。 */
    val settings: TimetableSettings = TimetableSettings(),
    /** 整天调课记录：表头据此标出被换过的日期。 */
    val daySwaps: List<TimetableDaySwap> = emptyList(),
) {
    val syncFailed: Boolean get() = syncFailure != "none"
}

/** 一条整天调课记录：两天之间的课程互换。 */
@Serializable
data class TimetableDaySwap(
    val id: String,
    /** 原日期（`yyyy-MM-dd`）。 */
    val dateA: String,
    /** 目标日期（`yyyy-MM-dd`）。 */
    val dateB: String,
    val description: String = "",
)

/**
 * 网格里的一格：已经套用过调课与单日修改，`activeThisWeek` 由 Dart 判定，
 * 界面只按它决定正常色还是灰格。
 */
data class TimetableCell(
    val courseId: String,
    val name: String,
    val teacher: String,
    val location: String,
    val note: String,
    val day: Int,
    val startNode: Int,
    val endNode: Int,
    val start: String?,
    val end: String?,
    val weeks: Set<Int>,
    val weeksLabel: String,
    /** 教务原始周次（未套用「只改本周」等覆盖）：单/双周角标按它判断。 */
    val origWeeks: Set<Int> = emptySet(),
    val activeThisWeek: Boolean,
    val conflictCount: Int,
    val colorIndex: Int,
    /** 用户给这一格挑的颜色（null = 自动配色）。 */
    val userColor: Int? = null,
    val isLab: Boolean,
    val isManual: Boolean,
    /** 课程类型：`standard` / `lab` / `pe` / `exam`。 */
    val kind: String = "standard",
    val date: String?,
    val editedThisDay: Boolean,
    /** 这一格本周已被「只改本周」挪走（原位置不再画灰影）。 */
    val movedAway: Boolean = false,
    val moved: Boolean,
)

/** 该周的一场日程或考试。`startNode/endNode` 为空表示它不落在任何节次内。 */
data class TimetableEvent(
    val id: String,
    val isExam: Boolean,
    /** 教务考试在本机被改过（日期/时间/地点/备注），可「恢复原考试」。 */
    val overridden: Boolean = false,
    val title: String,
    val date: String,
    val start: String,
    val end: String,
    val location: String,
    val note: String,
    /** 教务考试对应的课程号；与考试页的 [ExamItem] 拼 key 时必须一致，否则描边定位不到。 */
    val courseCode: String = "",
    val examWeek: Int,
    val startNode: Int?,
    val endNode: Int?,
)

/** 某一周的渲染数据。 */
data class TimetableWeek(
    val week: Int,
    val days: List<Int>,
    val dateOfDay: Map<Int, String>,
    /** 该周所属月份（1..12），表头交叉格显示「N 月」。 */
    val monthLabel: Int,
    val cells: List<TimetableCell>,
    val events: List<TimetableEvent>,
    val unplaced: List<TimetableEvent>,
    val unscheduled: List<TimetableUnscheduled>,
    val periods: List<Pair<String, String>>,
)
/** 课程详情里的一个字段改动。 */
data class CourseChange(
    val field: String,
    val before: String,
    val after: String,
)

/** 差异分组：同一作用范围的改动放在一组，对应一个「恢复」操作。 */
data class CourseChangeGroup(
    val scope: String,
    val title: String,
    val affectedScope: String,
    val changes: List<CourseChange>,
)

/** 点开某一格时的完整数据。 */
data class TimetableCellDetail(
    val cell: TimetableCell,
    val hasOverride: Boolean,
    val changes: List<CourseChangeGroup>,
)

/** 「其他课程」：没有固定时间或未排课，详情页里单独列一段。 */
data class TimetableUnscheduled(
    val courseId: String,
    val name: String,
    val teacher: String,
    /** 周次摘要，如「1-16周」；桥里按这门课的各个无节次课段拼出来。 */
    val weeksLabel: String = "",
)

/**
 * 课程表单提交的内容。`courseId` 为空表示新建一门手动课程；
 * 非空时光编辑已有课程（是否只改这一次由调用方挑 `scope`）。
 */
data class CourseFormInput(
    val courseId: String? = null,
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val note: String = "",
    val kind: String = "standard",
    val day: Int = 1,
    val startNode: Int = 1,
    val endNode: Int = 1,
    val weeks: Set<Int> = emptySet(),
    val userColor: Int? = null,
    val colorChanged: Boolean = false,
)

/** 导出时被跳过的时段：课程名 + 原因（`noFixedTime` / `emptyWeeks`）。 */
data class TimetableExportSkipped(
    val courseName: String,
    val reason: String,
)

/**
 * WakeUp 导出的预览：只读现有课表，不产生网络请求。
 *
 * [ok] 为 false 时 [message] 是可直接展示的一句话（例如「请先同步课表」）。
 */
data class TimetableExportPreview(
    val ok: Boolean,
    val message: String,
    val name: String,
    val courseCount: Int,
    val examCount: Int,
    val otherCount: Int,
    val skipped: List<TimetableExportSkipped>,
    val warnings: List<String>,
)

/** 生成口令的结果；[shareText] 为口令原文，界面负责复制。 */
data class TimetableExportShare(
    val ok: Boolean,
    val message: String,
    val shareText: String,
)

/** 一场日程（考试/其他）的详情：事件本体 + 是否有本地改动。 */
data class TimetableEventDetail(
    val event: TimetableEvent,
    val overridden: Boolean,
)
