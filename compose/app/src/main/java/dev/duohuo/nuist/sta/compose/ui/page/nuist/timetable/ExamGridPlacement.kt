// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import dev.duohuo.nuist.sta.compose.data.exam.ExamItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 考试在周视图网格里的落位。
 *
 * 与参考实现一致：考试不是固定节次，而是按「时间区间与节次时间是否重叠（哪怕 1 分钟）」
 * 落到覆盖到的最小..最大节次；与任何节次都不重叠的考试不画进网格。
 */
/** 考试的稳定身份：教务不给 id，用「课程码 + 日期 + 开始时间」定位（跳转与描边都靠它）。 */
internal fun examKey(exam: ExamItem): String =
    "${exam.courseCode}|${exam.date}|${exam.start}"

internal data class ExamGridItem(
    val exam: ExamItem,
    val day: Int,
    val startNode: Int,
    val endNode: Int,
    /** false 表示这是「更早的周」里的幽灵考试（灰显）。 */
    val activeThisWeek: Boolean,
)

internal object ExamGrid {

    /** 把 `HH:mm` 解析成分钟数，解析不了返回 null。 */
    private fun minutesOf(text: String): Int? {
        val parts = text.trim().split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    /**
     * 按时间重叠求覆盖节次（1 起）。[periods] 为每节的 (开始, 结束) 文本。
     *
     * 重叠判定照参考实现：起止相同的「零长度」考试用 `ps <= st && st < pe`，
     * 正常区间用 `ps < en && st < pe`（首尾相接不算重叠）。
     */
    fun nodeRange(
        periods: List<Pair<String, String>>,
        start: LocalTime,
        end: LocalTime,
    ): Pair<Int, Int>? {
        if (periods.isEmpty()) return null
        val startMin = start.hour * 60 + start.minute
        val endMinRaw = end.hour * 60 + end.minute
        val endMin = if (endMinRaw < startMin) startMin else endMinRaw
        var first = Int.MAX_VALUE
        var last = Int.MIN_VALUE
        periods.forEachIndexed { index, (periodStartText, periodEndText) ->
            val periodStart = minutesOf(periodStartText) ?: return@forEachIndexed
            val periodEnd = minutesOf(periodEndText) ?: return@forEachIndexed
            val overlaps = if (startMin == endMin) {
                periodStart <= startMin && startMin < periodEnd
            } else {
                periodStart < endMin && startMin < periodEnd
            }
            if (overlaps) {
                val node = index + 1
                if (node < first) first = node
                if (node > last) last = node
            }
        }
        // 与任何一节都不重叠（课间、午休、晚上 21:30 之后）：不落进网格，
        // 由表头日期下的圆点提示（参考实现同样不取整到最近节次，避免把块的纵向位置画错）。
        if (first == Int.MAX_VALUE) return null
        return first to last
    }

    /** 某一周的周一（或周日，按设置），用于算周差。 */
    private fun weekStart(date: LocalDate, weekStartsMonday: Boolean): LocalDate =
        if (weekStartsMonday) {
            date.minusDays((date.dayOfWeek.value - 1).toLong())
        } else {
            date.minusDays((date.dayOfWeek.value % 7).toLong())
        }

    /** 某日期属于第几周（以 [currentWeek] / [today] 为基准换算）。 */
    fun weekOf(
        date: LocalDate,
        today: LocalDate,
        currentWeek: Int,
        weekStartsMonday: Boolean,
    ): Int {
        val days = ChronoUnit.DAYS.between(weekStart(today, weekStartsMonday), weekStart(date, weekStartsMonday))
        return currentWeek + (days / 7).toInt()
    }

    /**
     * 取当前 [week] 要画进网格的考试。
     *
     * 规则照参考实现：本周的考试正常显示；更早周只在「显示非本周」开着时灰显；
     * 更晚周不画；[showExams] 关闭时全部隐藏。
     */
    fun itemsFor(
        week: Int,
        currentWeek: Int,
        today: LocalDate,
        weekStartsMonday: Boolean,
        days: List<Int>,
        exams: List<ExamItem>,
        periods: List<Pair<String, String>>,
        showExams: Boolean,
        showNonCurrentWeek: Boolean,
        /** 从考试安排页跳过来的那场考试：即使没开“显示考试”也临时画出来。 */
        focusedKey: String? = null,
    ): List<ExamGridItem> {
        if (exams.isEmpty() || days.isEmpty()) return emptyList()
        if (!showExams && focusedKey == null) return emptyList()
        val result = ArrayList<ExamGridItem>()
        exams.forEach { exam ->
            val focused = focusedKey != null && examKey(exam) == focusedKey
            if (!showExams && !focused) return@forEach
            val day = exam.date.dayOfWeek.value
            if (day !in days) return@forEach
            val examWeek = if (exam.week > 0) exam.week else weekOf(exam.date, today, currentWeek, weekStartsMonday)
            val active = examWeek == week
            if (!active) {
                // 幽灵只在「考试还在后面」的周、且开着「显示非本周」时出现；
                // 已经考完的不再画；跳转聚焦的那一场也只在自己的考试周里画，
                // 不靠幽灵逻辑，否则它会跟着翻周一直留在网格上。
                if (focused || examWeek <= week || !showNonCurrentWeek) return@forEach
            }
            val range = nodeRange(periods, exam.start, exam.end) ?: return@forEach
            result += ExamGridItem(
                exam = exam,
                day = day,
                startNode = range.first,
                endNode = range.second,
                activeThisWeek = active,
            )
        }
        return result
    }
}