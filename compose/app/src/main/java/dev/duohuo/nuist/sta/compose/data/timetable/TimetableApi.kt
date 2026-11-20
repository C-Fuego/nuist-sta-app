// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.timetable

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.arr
import dev.duohuo.nuist.sta.compose.data.bridge.bool
import dev.duohuo.nuist.sta.compose.data.bridge.numberOrNull
import dev.duohuo.nuist.sta.compose.data.bridge.int
import dev.duohuo.nuist.sta.compose.data.bridge.obj
import dev.duohuo.nuist.sta.compose.data.bridge.objects
import dev.duohuo.nuist.sta.compose.data.bridge.str
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.CourseBlock
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableSnapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

/**
 * 课表数据源。
 *
 * 抓取、周次换算、开学日推算、课段合并全部在 Dart 侧完成（`timetable.display`），
 * 这一层只把结果搬成前端的展示模型，不做任何教务协议处理。
 */
class TimetableApi(private val bridge: CampusBridge) {

    suspend fun snapshot(refresh: Boolean = false): TimetableSnapshot {
        val data = bridge.call(
            "timetable.display",
            if (refresh) mapOf("refresh" to true) else emptyMap(),
        )
        return parse(data)
    }

    /** 一场日程的详情（id 来自周视图的考试块）。 */
    suspend fun eventDetail(id: String): TimetableEventDetail {
        val data = bridge.call("timetable.event", mapOf("id" to id))
        val event = data.obj("event") ?: error("日程不存在")
        return TimetableEventDetail(
            event = parseEvent(event),
            overridden = data.bool("overridden"),
        )
    }

    /** 保存日程（考试/其他）：日期、起止时间、地点、备注都可改。 */
    suspend fun saveEvent(
        id: String,
        title: String,
        date: String,
        start: String,
        end: String,
        location: String,
        note: String,
    ): Boolean = bridge.call(
        "timetable.saveEvent",
        mapOf(
            "id" to id,
            "title" to title,
            "date" to date,
            "start" to start,
            "end" to end,
            "location" to location,
            "note" to note,
        ),
    ).bool("ok")

    /** 新增手动日程（考试 / 其他）；返回新事件的 id，失败返回 null。 */
    suspend fun addEvent(
        kind: String,
        title: String,
        date: String,
        start: String,
        end: String,
        location: String,
        note: String,
        courseCode: String,
    ): String? {
        val data = bridge.call(
            "timetable.addEvent",
            mapOf(
                "kind" to kind,
                "title" to title,
                "date" to date,
                "start" to start,
                "end" to end,
                "location" to location,
                "note" to note,
                "courseCode" to courseCode,
            ),
        )
        if (!data.bool("ok")) return null
        return data.str("id").ifEmpty { null }
    }

    suspend fun deleteEvent(id: String): Boolean =
        bridge.call("timetable.deleteEvent", mapOf("id" to id)).bool("ok")

    /** 分范围恢复：`occurrence` = 单日/按周修改，`position` = 调课记录。 */
    suspend fun restoreChanges(
        courseId: String,
        day: Int,
        startNode: Int,
        endNode: Int,
        scope: String,
    ): Boolean = bridge.call(
        "timetable.restoreChanges",
        mapOf(
            "courseId" to courseId,
            "day" to day,
            "startNode" to startNode,
            "endNode" to endNode,
            "scope" to scope,
        ),
    ).bool("ok")

    suspend fun restoreEvent(id: String): Boolean =
        bridge.call("timetable.restoreEvent", mapOf("id" to id)).bool("ok")

    /** 把一场日程拖到新的星期/节次：日期与起止时间都按作息表换算。 */
    suspend fun moveEvent(id: String, week: Int, day: Int, startNode: Int, span: Int): Boolean =
        bridge.call(
            "timetable.moveEvent",
            mapOf("id" to id, "week" to week, "day" to day, "startNode" to startNode, "span" to span),
        ).bool("ok")

    /** WakeUp 导出的预览（只读课表，不发网络请求）。 */
    suspend fun exportPreview(): TimetableExportPreview {
        val data = bridge.call("timetable.exportPreview")
        return TimetableExportPreview(
            ok = data.bool("ok", false),
            message = data.str("message"),
            name = data.str("name"),
            courseCount = data.int("courseCount"),
            examCount = data.int("examCount"),
            otherCount = data.int("otherCount"),
            skipped = data.arr("skipped").objects().map {
                TimetableExportSkipped(courseName = it.str("courseName"), reason = it.str("reason"))
            },
            warnings = data.arr("warnings").map { it.jsonPrimitive.content },
        )
    }

    /** 让社团服务生成 WakeUp 口令；失败时把可展示的提示带回来。 */
    suspend fun exportWakeup(): TimetableExportShare {
        val data = bridge.call("timetable.exportWakeup")
        return TimetableExportShare(
            ok = data.bool("ok", false),
            message = data.str("message"),
            shareText = data.str("shareText"),
        )
    }

    private fun parse(data: JsonObject): TimetableSnapshot {
        val termCode = data.string("termCode").ifEmpty { "" }
        val periods = data.array("periods").map { row ->
            val item = row.jsonObject
            item.string("start") to item.string("end")
        }
        val courses = data.array("courses").map { row ->
            val item = row.jsonObject
            CourseBlock(
                name = item.string("name"),
                day = item.int("day", 1),
                startNode = item.int("startNode", 1),
                endNode = item.int("endNode", item.int("startNode", 1)),
                location = item.string("location"),
                teacher = item.string("teacher"),
                colorIndex = item.int("colorIndex", 0),
                weeks = item.array("weeks").mapNotNull { it.jsonPrimitive.numberOrNull()?.toInt() }.toSet(),
                weeksLabel = item.string("weeksLabel"),
                isExperimental = item.booleanOrNull("isLab") ?: false,
            )
        }
        val unscheduled = data.array("unscheduled").map { row ->
            val item = row.jsonObject
            CourseBlock(
                name = item.string("name"),
                day = 0,
                startNode = 0,
                endNode = 0,
                location = item.string("location"),
                teacher = item.string("teacher"),
                colorIndex = item.int("colorIndex", 0),
            )
        }
        return TimetableSnapshot(
            termCode = termCode,
            termLabel = data.string("termLabel").ifEmpty { termCode },
            currentWeek = data.int("currentWeek", 1),
            currentDay = data.int("currentDay", 1),
            totalWeeks = data.int("totalWeeks", 20),
            periods = periods,
            courses = courses,
            unscheduled = unscheduled,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    // ---- 控制器视图：状态与单周数据（Dart 侧 TimetableController 为唯一数据源）----

    suspend fun state(): TimetableState = parseState(bridge.call("timetable.state"))

    /**
     * 一次往返同时取回控制器状态与某一周的格子。
     *
     * 桥按队列串行执行，每多一次往返就多一次排队与 JSON 编解码；进课表页只等这一次。
     */
    suspend fun overview(week: Int? = null): Pair<TimetableState, TimetableWeek> {
        val args = if (week != null) mapOf("week" to week) else emptyMap<String, Any?>()
        val data = bridge.call("timetable.overview", args)
        val stateJson = data.obj("state") ?: error("课表状态缺失")
        val weekJson = data.obj("week") ?: error("课表周次缺失")
        return parseState(stateJson) to parseWeek(weekJson)
    }

    suspend fun sync(): TimetableState = parseState(bridge.call("timetable.sync"))

    suspend fun setWeek(week: Int): TimetableState =
        parseState(bridge.call("timetable.setWeek", mapOf("week" to week)))

    /** 本学期全部考试，来自 Dart 控制器的日程表。 */
    suspend fun exams(): List<TimetableEvent> =
        bridge.call("timetable.exams").arr("exams").objects().map(::parseEvent)

    /** 改一批设置：Dart 合并后落盘并返回最新状态。 */
    suspend fun updateSettings(patch: Map<String, Any?>): TimetableState =
        parseState(bridge.call("timetable.updateSettings", mapOf("patch" to patch)))

    suspend fun week(week: Int? = null): TimetableWeek {
        val args = if (week != null) mapOf("week" to week) else emptyMap<String, Any?>()
        return parseWeek(bridge.call("timetable.week", args))
    }

    private fun parseState(data: JsonObject) = TimetableState(
        termCode = data.str("termCode"),
        termLabel = data.str("termLabel"),
        termStartDate = data.str("termStartDate").ifEmpty { null },
        currentWeek = data.int("currentWeek", 1),
        viewingWeek = data.int("viewingWeek", 1),
        totalWeeks = data.int("totalWeeks", 20),
        currentDay = data.int("currentDay", 1),
        lastPeriod = data.int("lastPeriod", 11),
        portalBound = data.bool("portalBound"),
        loading = data.bool("loading"),
        hasCourses = data.bool("hasCourses"),
        weekPending = data.bool("weekPending"),
        syncStatus = data.str("syncStatus", "idle"),
        syncFailure = data.str("syncFailure", "none"),
        statusMessage = data.str("statusMessage").ifEmpty { null },
        periods = data.parsePeriods(),
        settings = data.obj("settings").parseSettings(),
        daySwaps = data.arr("daySwaps").map { row ->
            val item = row.jsonObject
            TimetableDaySwap(
                id = item.str("id"),
                dateA = item.str("dateA"),
                dateB = item.str("dateB"),
                description = item.str("description"),
            )
        },
    )

    /** Dart 的 `TimetableSettings` 只有渲染相关字段会用到，其余原样忽略。 */
    private fun JsonObject?.parseSettings(): TimetableSettings {
        val raw = this ?: return TimetableSettings()
        val fontScale = when (raw.str("fontScale", "standard")) {
            "small" -> 0.9f
            "large" -> 1.15f
            else -> 1f
        }
        return TimetableSettings(
            weekStartsMonday = raw.bool("weekStartsOnMonday", true),
            showSaturday = raw.bool("showSaturday", true),
            showSunday = raw.bool("showSunday", true),
            showNonCurrentWeek = raw.bool("showNonCurrentWeek", false),
            showLocation = raw.bool("showLocation", true),
            showTeacher = raw.bool("showTeacher", false),
            showPeriodTimes = raw.bool("showPeriodTimes", true),
            showGridLines = raw.bool("showGridLines", true),
            cellHeightDp = raw.int("periodHeight", 64),
            fontScale = fontScale,
            cellOpacity = raw.int("cellOpacity", 100),
            headerTextSize = raw.int("headerTextSize", 11),
            examColorArgb = raw.int("examColor", 0xFFC62828.toInt()),
            showExams = raw.bool("showExams", true),
            reminderLeadMinutes = TimetableSettings.sanitizeReminderLead(raw.int("reminderLeadMinutes", 15)),
            nextDayReminderEnabled = raw.bool("nextDayReminderEnabled", true),
            nextDayReminderMinute = TimetableSettings.sanitizeNextDayMinute(
                raw.int("nextDayReminderMinute", 22 * 60),
            ),
            showTomorrowOnHome = raw.bool("showTomorrowOnHome", true),
            homeMaxItems = raw.int("homeMaxItems", 2).coerceIn(1, 4),
        )
    }

    private fun parseCell(item: JsonObject) = TimetableCell(
        courseId = item.str("courseId"),
        name = item.str("name"),
        teacher = item.str("teacher"),
        location = item.str("location"),
        note = item.str("note"),
        day = item.int("day", 1),
        startNode = item.int("startNode", 1),
        endNode = item.int("endNode", item.int("startNode", 1)),
        start = item.str("start").ifEmpty { null },
        end = item.str("end").ifEmpty { null },
        weeks = item.arr("weeks").mapNotNull { it.jsonPrimitive.numberOrNull()?.toInt() }.toSet(),
        weeksLabel = item.str("weeksLabel"),
        origWeeks = item.arr("origWeeks").mapNotNull { it.jsonPrimitive.numberOrNull()?.toInt() }.toSet(),
        activeThisWeek = item.bool("activeThisWeek", true),
        conflictCount = item.int("conflictCount", 0),
        colorIndex = item.int("colorIndex", 0),
        userColor = if (item.str("userColor").isEmpty()) null else item.int("userColor", 0),
        isLab = item.bool("isLab"),
        isManual = item.bool("isManual"),
        kind = item.str("kind", "standard"),
        date = item.str("date").ifEmpty { null },
        editedThisDay = item.bool("editedThisDay"),
        movedAway = item.bool("movedAway"),
        moved = item.bool("moved"),
    )

    /** 一格的完整详情：合并后的格子、是否有本地改动、以及差异分组。 */
    suspend fun cellDetail(
        courseId: String,
        day: Int,
        startNode: Int,
        endNode: Int,
        week: Int,
    ): TimetableCellDetail {
        val data = bridge.call(
            "timetable.cell",
            mapOf(
                "courseId" to courseId,
                "day" to day,
                "startNode" to startNode,
                "endNode" to endNode,
                "week" to week,
            ),
        )
        return TimetableCellDetail(
            cell = parseCell(data),
            hasOverride = data.bool("hasOverride"),
            changes = data.arr("changes").objects().map { group ->
                CourseChangeGroup(
                    scope = group.str("scope"),
                    title = group.str("title"),
                    affectedScope = group.str("affectedScope"),
                    changes = group.arr("changes").objects().map { change ->
                        CourseChange(
                            field = change.str("field"),
                            before = change.str("before"),
                            after = change.str("after"),
                        )
                    },
                )
            },
        )
    }

    /** 保存课程表单：`scope` 为 `thisWeek` 时只改这一次课，`all` 改这门课的全部安排。 */
    suspend fun saveCourse(
        courseId: String?,
        day: Int,
        startNode: Int,
        endNode: Int,
        week: Int,
        scope: String,
        name: String,
        teacher: String,
        location: String,
        note: String,
        kind: String,
        weeks: Set<Int>,
        userColor: Int?,
        colorChanged: Boolean,
        applyWeeks: List<Int>? = null,
    ): Boolean = bridge.call(
        "timetable.saveCourse",
        mapOf(
            "courseId" to courseId,
            "day" to day,
            "startNode" to startNode,
            "endNode" to endNode,
            "week" to week,
            "scope" to scope,
            "name" to name,
            "teacher" to teacher,
            "location" to location,
            "note" to note,
            "kind" to kind,
            "weeks" to weeks.sorted(),
            "userColor" to userColor,
            "colorChanged" to colorChanged,
            "applyWeeks" to applyWeeks?.sorted(),
        ),
    ).bool("changed")

    /** 与候选时段冲突的课程描述（「与『X』第 1-3 周冲突」），没有冲突返回 null。 */
    suspend fun conflicts(
        courseId: String?,
        day: Int,
        startNode: Int,
        endNode: Int,
        weeks: Set<Int>,
    ): String? {
        val data = bridge.call(
            "timetable.conflicts",
            mapOf(
                "courseId" to courseId,
                "day" to day,
                "startNode" to startNode,
                "endNode" to endNode,
                "weeks" to weeks.sorted(),
            ),
        )
        return data.str("text").ifEmpty { null }
    }

    /** 删除某格：`thisWeek` 只删这一次，`weekdayMatch` 删所有同星期的安排，`all` 删整门课。 */
    suspend fun deleteCourse(
        courseId: String,
        day: Int,
        startNode: Int,
        endNode: Int,
        week: Int,
        scope: String,
    ): Boolean = bridge.call(
        "timetable.deleteCourse",
        mapOf(
            "courseId" to courseId,
            "day" to day,
            "startNode" to startNode,
            "endNode" to endNode,
            "week" to week,
            "scope" to scope,
        ),
    ).bool("changed")

    /** 整门课换色（课程级，和自动配色同一份数据）；[userColor] 为 null 表示恢复自动配色。 */
    suspend fun setCourseColor(courseId: String, userColor: Int?): Boolean {
        val data = bridge.call(
            "timetable.setCourseColor",
            mapOf("courseId" to courseId, "userColor" to userColor),
        )
        return data.bool("changed")
    }

    /** 拖动调课：`scope` 为 `thisWeek` / `fromThisWeek` / `allWeeks`。 */
    suspend fun moveCell(
        courseId: String,
        day: Int,
        startNode: Int,
        endNode: Int,
        week: Int,
        toDay: Int,
        toStart: Int,
        scope: String,
    ): String? {
        val data = bridge.call(
            "timetable.moveCell",
            mapOf(
                "courseId" to courseId,
                "day" to day,
                "startNode" to startNode,
                "endNode" to endNode,
                "week" to week,
                "toDay" to toDay,
                "toStart" to toStart,
                "scope" to scope,
            ),
        )
        return if (data.bool("ok")) null else data.str("error", "unknown")
    }

    /** 丢掉这门课在本地做的全部改动，恢复成教务给的样子。 */
    suspend fun restoreCourse(courseId: String): Boolean =
        bridge.call("timetable.restoreCourse", mapOf("courseId" to courseId)).bool("changed")

    /** 整天调课页的初始数据：学期首末日、今天、全部调课记录。 */
    suspend fun daySwaps(): DaySwapOverview {
        val data = bridge.call("timetable.daySwaps")
        return DaySwapOverview(
            semesterFirst = data.str("semesterFirst").toLocalDateOrNull(),
            semesterLast = data.str("semesterLast").toLocalDateOrNull(),
            today = data.str("today").toLocalDateOrNull(),
            swaps = parseDaySwaps(data),
        )
    }

    /** 新增一次整天调课；同一天、超出学期、已调过的校验都在 Dart 侧。 */
    suspend fun addDaySwap(dateA: LocalDate, dateB: LocalDate): DaySwapResult {
        val data = bridge.call(
            "timetable.addDaySwap",
            mapOf("dateA" to dateA.toString(), "dateB" to dateB.toString()),
        )
        return parseDaySwapResult(data)
    }

    /** 撤销一条调课记录；不存在的 id 不会删任何东西。 */
    suspend fun removeDaySwap(id: String): DaySwapResult =
        parseDaySwapResult(bridge.call("timetable.removeDaySwap", mapOf("id" to id)))

    /** 变动记录，最新在前，已按同步批次分组。 */
    suspend fun changeLog(): TimetableChangeLog {
        val data = bridge.call("timetable.changes")
        val groups = data.arr("groups").objects().map { group ->
            TimetableChangeGroup(
                title = group.str("title"),
                items = group.arr("items").objects().map { item ->
                    TimetableChangeItem(
                        id = item.str("id"),
                        courseName = item.str("courseName"),
                        type = item.str("type"),
                        typeLabel = item.str("typeLabel"),
                        scope = item.str("scope"),
                        detail = item.str("detail"),
                    )
                },
            )
        }
        return TimetableChangeLog(
            groups = groups,
            total = data.int("total", groups.sumOf { it.items.size }),
        )
    }

    /** 恢复教务课程：清掉本机对教务课的编辑与删除、全部调课记录和考试覆盖。 */
    suspend fun restoreAllJwxt(): Boolean =
        bridge.call("timetable.restoreAllJwxt").bool("changed")

    private fun parseDaySwaps(data: JsonObject): List<DaySwapEntry> =
        data.arr("swaps").objects().map { item ->
            DaySwapEntry(
                id = item.str("id"),
                dateA = item.str("dateA").toLocalDateOrNull(),
                dateB = item.str("dateB").toLocalDateOrNull(),
                description = item.str("description"),
            )
        }

    private fun parseDaySwapResult(data: JsonObject) = DaySwapResult(
        ok = data.bool("ok"),
        message = data.str("message"),
        swaps = parseDaySwaps(data),
    )

    private fun parseWeek(data: JsonObject): TimetableWeek {
        val periods = data.parsePeriods()
        val days = data.arr("days").mapNotNull { it.jsonPrimitive.numberOrNull()?.toInt() }
        val dateOfDay = buildMap {
            val raw = data.obj("dateOfDay") ?: return@buildMap
            for ((key, value) in raw) {
                val day = key.toIntOrNull() ?: continue
                put(day, value.jsonPrimitive.content)
            }
        }
        val cells = data.arr("cells").objects().map(::parseCell)
        val unscheduled = data.arr("unscheduled").objects().map { item ->
            TimetableUnscheduled(
                courseId = item.str("courseId"),
                name = item.str("name"),
                teacher = item.str("teacher"),
                weeksLabel = item.str("weeksLabel"),
            )
        }
        return TimetableWeek(
            week = data.int("week", 1),
            days = days,
            dateOfDay = dateOfDay,
            monthLabel = data.int("monthLabel"),
            cells = cells,
            events = data.arr("events").objects().map(::parseEvent),
            unplaced = data.arr("unplaced").objects().map(::parseEvent),
            unscheduled = unscheduled,
            periods = periods,
        )
    }

    private fun parseEvent(item: JsonObject) = TimetableEvent(
        id = item.str("id"),
        isExam = item.bool("isExam"),
        title = item.str("title"),
        date = item.str("date"),
        start = item.str("start"),
        end = item.str("end"),
        location = item.str("location"),
        note = item.str("note"),
        courseCode = item.str("courseCode"),
        overridden = item.bool("overridden"),
        examWeek = item.int("examWeek", 0),
        startNode = item.str("startNode").toIntOrNull(),
        endNode = item.str("endNode").toIntOrNull(),
    )

    private fun JsonObject.parsePeriods(): List<Pair<String, String>> =
        arr("periods").objects().map { it.str("start") to it.str("end") }

    private fun JsonObject.array(key: String): JsonArray =
        (this[key] as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.content.orEmpty()

    private fun JsonObject.booleanOrNull(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.int(key: String, fallback: Int): Int =
        this[key]?.jsonPrimitive?.numberOrNull()?.toInt() ?: fallback
}
