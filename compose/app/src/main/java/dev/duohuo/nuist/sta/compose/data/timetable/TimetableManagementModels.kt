// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.timetable

import java.time.LocalDate

/** 桥里的日期是 `yyyy-MM-dd`，解析失败当作缺失，不抛异常。 */
internal fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

/** 一条整天调课记录：`dateA` 与 `dateB` 两天的课程互换（考试、日程留在原日期）。 */
data class DaySwapEntry(
    val id: String,
    val dateA: LocalDate?,
    val dateB: LocalDate?,
    /** 「M月D日与M月D日的课程互换」，由 Dart 侧拼好。 */
    val description: String,
)

/** 整天调课页的初始数据：学期首末日（日期选择器上下界）、今天、已有记录。 */
data class DaySwapOverview(
    val semesterFirst: LocalDate?,
    val semesterLast: LocalDate?,
    val today: LocalDate?,
    val swaps: List<DaySwapEntry>,
)

/** 新增或撤销一次调课的结果；`message` 是可直接展示的一句话。 */
data class DaySwapResult(
    val ok: Boolean,
    val message: String,
    val swaps: List<DaySwapEntry>,
)

/** 变动记录的一条：影响范围与明细的文案都由 Dart 侧算好。 */
data class TimetableChangeItem(
    val id: String,
    val courseName: String,
    val type: String,
    val typeLabel: String,
    val scope: String,
    val detail: String,
)

/** 同一次同步（同一个 fingerprint）产生的变动记录。 */
data class TimetableChangeGroup(
    val title: String,
    val items: List<TimetableChangeItem>,
)

/** 变动记录列表，最新在前。 */
data class TimetableChangeLog(
    val groups: List<TimetableChangeGroup>,
    val total: Int,
)