// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.exam

import dev.duohuo.nuist.sta.compose.data.timetable.TimetableApi
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableEvent
import java.time.LocalDate
import java.time.LocalTime

/** 一场已排定时间的考试（字段只取展示所需，学号/教师/内部 ID 一律不读）。 */
data class ExamItem(
    val courseCode: String,
    val title: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val location: String,
    val note: String,
    /** 第几教学周（Dart 控制器算好的），0 表示未知，退回按日期换算。 */
    val week: Int = 0,
    /** 对应 Dart 侧日程的 id（编辑/删除/恢复都用它定位）。 */
    val eventId: String = "",
) {
    /** 「11月20日 08:00 – 10:00」这样的时间描述。 */
    val timeLabel: String
        get() = "${date.monthValue}月${date.dayOfMonth}日 ${hhmm(start)} – ${hhmm(end)}"
}

private fun hhmm(t: LocalTime): String = String.format("%02d:%02d", t.hour, t.minute)

/**
 * 考试安排：直接读课表的日程表（`timetable.exams`），不再单独查教务的考试接口。
 *
 * 考试与课表同源——用户在课表里改过时间/地点/备注的考试，这里显示的就是改后的值，
 * 两个页面也不会各存一份互相打架的数据。
 */
class ExamApi(private val timetable: TimetableApi) {

    /**
     * 本学期全部考试，已去重并按开考时间升序。
     *
     * 一条教务考试若被本机改过（[TimetableEvent.overridden]），日程表里回的就是改后的值，
     * 这里不做任何还原，直接照抄。
     */
    suspend fun fetch(): List<ExamItem> {
        val events = timetable.exams()
        val items = events.filter { it.isExam }.mapNotNull(::parseEvent)
        return mergeExamItems(items).sortedBy { it.date.atTime(it.start) }
    }

    /**
     * 把日程事件映射成界面模型；[ExamItem.eventId] 保留日程 id，缺省为空串。
     *
     * 旧数据没有 id 也能正常展示与跳转：列表排序、点击定位用的是
     * 「课程号 + 日期 + 开始时间」拼出的身份，不依赖 id。
     */
    private fun parseEvent(event: TimetableEvent): ExamItem? {
        val date = parseDate(event.date) ?: return null
        val start = parseTime(event.start) ?: return null
        val end = parseTime(event.end) ?: return null
        if (end < start) return null
        return ExamItem(
            courseCode = event.courseCode,
            title = event.title.ifEmpty { event.courseCode.ifEmpty { "考试" } },
            date = date,
            start = start,
            end = end,
            location = event.location,
            note = event.note,
            week = event.examWeek,
            eventId = event.id,
        )
    }

    /** 桥里统一成「2026-11-20」，这里兼容纯数字写法。 */
    private fun parseDate(raw: String): LocalDate? {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 8) return null
        return runCatching {
            LocalDate.of(digits.take(4).toInt(), digits.substring(4, 6).toInt(), digits.substring(6, 8).toInt())
        }.getOrNull()
    }

    /** 时间形如「08:00」「0800」或「08:00:00」。 */
    private fun parseTime(raw: String): LocalTime? {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 4) return null
        return runCatching {
            LocalTime.of(digits.take(2).toInt(), digits.substring(2, 4).toInt())
        }.getOrNull()
    }
}

private val blankPattern = Regex("[\\s\\u3000]+")

/** 标题归一化：去掉所有空白，半角括号统一成全角，避免同一条考试因排版差异被当成两场。 */
private fun normalizedTitle(title: String): String =
    title.replace(blankPattern, "").replace('(', '（').replace(')', '）')

/** 两场考试的时间是否有交集；端点相接（上一场结束即下一场开始）不算同一场。 */
private fun overlaps(a: ExamItem, b: ExamItem): Boolean = a.start < b.end && b.start < a.end

/** 同一场考试：同一天、归一化标题相同、时间有交集。 */
private fun sameExam(a: ExamItem, b: ExamItem): Boolean =
    a.date == b.date && normalizedTitle(a.title) == normalizedTitle(b.title) && overlaps(a, b)

/** 代表条目缺的字段从被并掉的条目补全；日期、时间、标题一律以先出现的为准。 */
private fun ExamItem.fillFrom(other: ExamItem): ExamItem = copy(
    courseCode = courseCode.ifEmpty { other.courseCode },
    location = location.ifEmpty { other.location },
    note = note.ifEmpty { other.note },
    eventId = eventId.ifEmpty { other.eventId },
    week = if (week != 0) week else other.week,
)

/**
 * 合并同一天同一场考试的重复条目（教务条目与本机日程可能各回一条），保持原有顺序。
 *
 * 与参考实现一致：逐条与已保留的代表比较，重复时保留先出现的一条并用另一条补空字段。
 */
internal fun mergeExamItems(items: List<ExamItem>): List<ExamItem> {
    if (items.size < 2) return items
    val merged = ArrayList<ExamItem>(items.size)
    for (item in items) {
        val index = merged.indexOfFirst { sameExam(it, item) }
        if (index < 0) {
            merged.add(item)
        } else {
            merged[index] = merged[index].fillFrom(item)
        }
    }
    return merged
}