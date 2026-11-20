// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.exam.ExamItem
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import dev.duohuo.nuist.sta.compose.data.timetable.CourseFormInput
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableApi
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettingsRepository
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableState
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableCellDetail
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableExportPreview
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableExportShare
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TimetableUiState(
    val state: TimetableState? = null,
    val week: TimetableWeek? = null,
    /** [week] 里那批格子属于哪一周；它和界面选中的周可能短暂不同步。 */
    val dataWeek: Int = 0,
    /** 已预取的相邻周格子，滑动时铺在旁边形成连续网格。 */
    val cachedWeeks: Map<Int, TimetableWeek> = emptyMap(),
    /** 本学期全部考试（来自 Dart 控制器，考试接口失败时这里为空，不影响课表）。 */
    val exams: List<ExamItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 是否缺少凭据（界面据此引导去绑定通行密钥）。 */
    val needsCredential: Boolean = false,
    /** 最近一次编辑操作的结果，界面用 Snackbar 消费。 */
    val actionMessage: String? = null,
) {
    /**
     * 渲染用的单周快照：学期与周次来自 Dart 状态，课程格子只包含当前这一周，
     * 界面不需要再自己按 `weeks` 过滤。
     */
    val snapshot: TimetableSnapshot?
        get() {
            val s = state ?: return null
            val w = week ?: return null
            return TimetableSnapshot(
                termCode = s.termCode,
                termLabel = s.termLabel.ifEmpty { s.termCode },
                currentWeek = s.currentWeek,
                currentDay = s.currentDay,
                totalWeeks = s.totalWeeks,
                periods = w.periods.ifEmpty { s.periods },
                courses = w.cells.map { cell ->
                    CourseBlock(
                        courseId = cell.courseId,
                        name = cell.name,
                        day = cell.day,
                        startNode = cell.startNode,
                        endNode = cell.endNode,
                        location = cell.location,
                        teacher = cell.teacher,
                        colorIndex = cell.colorIndex,
                        weeks = cell.weeks + w.week,
                        weeksLabel = cell.weeksLabel,
                        origWeeks = cell.origWeeks + w.week,
                        isExperimental = cell.isLab,
                        isManual = cell.isManual,
                        kind = cell.kind,
                    )
                },
                unscheduled = w.unscheduled.map { item ->
                    CourseBlock(
                        courseId = item.courseId,
                        name = item.name,
                        day = 0,
                        startNode = 0,
                        endNode = 0,
                        teacher = item.teacher,
                        weeksLabel = item.weeksLabel,
                    )
                },
                fetchedAtMillis = System.currentTimeMillis(),
                daySwaps = s.daySwaps,
            )
        }
}

/**
 * 课表页面的状态。
 *
 * 学期、当前周、总周数、同步结果与「哪一周有哪些格子」全部由 Dart 侧的
 * `TimetableController` 算好后经桥返回，这里只保存结果与加载态。
 */
class TimetableViewModel(
    private val api: TimetableApi,
    private val settingsRepository: TimetableSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TimetableUiState())

    /** 已加载过的周次缓存：滑到下一周时直接用，不等桥调用。 */
    private val weekCache = LinkedHashMap<Int, TimetableWeek>()

    /** 当前界面选中的周；异步回包只写回最新选中的那一周。 */
    private var _selectedWeek: Int = 0

    /** 上一次成功从桥取回状态与格子的时刻，用来判断进页面要不要再走一次桥。 */
    private var _lastLoadedAt = 0L

    /** 在途的加载：刷新与切换共用，避免两次并发请求。 */
    private var _loading = false

    val state: StateFlow<TimetableUiState> = _state.asStateFlow()

    companion object {
        /** 数据在这个时间内算新鲜：进页面直接用现有内容，不再走桥。 */
        private const val FRESH_MILLIS = 60_000L
    }

    init {
        load()
        // 「显示非本周课程」这类开关由 Dart 在出格子时生效，所以开关一变就得重新取这一周，
        // 不能只靠镜像里的字段重绘——手里的格子还是按旧设置生成的。
        viewModelScope.launch {
            var lastNonCurrent = settingsRepository.state.value.showNonCurrentWeek
            settingsRepository.state.collect { settings ->
                if (settings.showNonCurrentWeek != lastNonCurrent) {
                    lastNonCurrent = settings.showNonCurrentWeek
                    reloadVisibleWeek()
                }
            }
        }
    }

    /** 清掉缓存的周格子，按当前浏览周重新取一次（设置变化后格子的构成会变）。 */
    private fun reloadVisibleWeek() {
        val week = _selectedWeek.takeIf { it > 0 } ?: _state.value.state?.viewingWeek ?: return
        weekCache.clear()
        viewModelScope.launch {
            runCatching { api.week(week) }
                .onSuccess { data ->
                    weekCache[data.week] = data
                    _state.update {
                        it.copy(
                            week = data,
                            dataWeek = data.week,
                            cachedWeeks = weekCache.toMap(),
                        )
                    }
                    prefetchNeighbours(data.week)
                }
        }
    }

    fun load(force: Boolean = false) {
        if (_loading) return
        val current = _state.value
        val hasContent = current.state != null && current.week != null
        // 手里已经有内容、而且刚取过：进页面直接复用，不再走桥（也不闪骨架屏）。
        if (!force && hasContent &&
            System.currentTimeMillis() - _lastLoadedAt < FRESH_MILLIS
        ) {
            return
        }
        // 已有内容时的刷新是「悄悄刷新」：不把界面退回骨架屏，也不清掉已有错误提示以外的状态。
        val silent = !force && hasContent
        _loading = true
        _state.update { it.copy(isLoading = !silent, error = if (silent) it.error else null) }
        viewModelScope.launch {
            runCatching {
                if (force) {
                    val state = api.sync()
                    state to api.week(state.viewingWeek)
                } else {
                    // 状态与当周一次取回，省掉一次桥往返。
                    api.overview()
                }
            }.onSuccess { (state, week) ->
                _lastLoadedAt = System.currentTimeMillis()
                settingsRepository.mirror(state.settings)
                weekCache[week.week] = week
                _selectedWeek = state.viewingWeek
                _state.update {
                    it.copy(
                        state = state,
                        week = week,
                        dataWeek = week.week,
                        cachedWeeks = weekCache.toMap(),
                        isLoading = false,
                        error = null,
                        needsCredential = false,
                    )
                }
                // 考试不是首屏必需：等这一周先画出来，再单独查，进页面不会多等一次桥调用。
                loadExams()
                prefetchNeighbours(week.week)
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        // 已经有内容时不要用错误把课表盖掉，交给 Snackbar / 诊断日志。
                        error = if (hasContent) it.error else e.toUserMessage(),
                        needsCredential = e is PortalException.Credential,
                    )
                }
            }
            _loading = false
        }
    }

    /** 切周：先让 Dart 记住浏览周，再取那一周的格子。 */
    /**
     * 切到某一周。
     *
     * 相邻周在上一次切换后就已经预取到 [weekCache]，所以左右滑动时新内容立刻可用，
     * 不会出现「滑过去一片空白、再等一次桥调用」。
     */
    fun selectWeek(week: Int) {
        _selectedWeek = week
        weekCache[week]?.let { cached ->
            _state.update {
                it.copy(week = cached, dataWeek = cached.week, cachedWeeks = weekCache.toMap(), error = null)
            }
        }
        viewModelScope.launch {
            runCatching { api.setWeek(week) }
            runCatching { api.week(week) }
                .onSuccess { data ->
                    weekCache[data.week] = data
                    if (_selectedWeek == data.week) {
                        _state.update {
                            it.copy(
                                week = data,
                                dataWeek = data.week,
                                cachedWeeks = weekCache.toMap(),
                                error = null,
                            )
                        }
                    }
                    prefetchNeighbours(data.week)
                }
                .onFailure { e ->
                    if (_selectedWeek == week) {
                        _state.update { it.copy(error = e.toUserMessage()) }
                    }
                }
        }
    }

    private fun loadExams() {
        viewModelScope.launch {
            runCatching { api.exams() }
                .onSuccess { events -> _state.update { it.copy(exams = events.mapNotNull(::toExamItem)) } }
        }
    }

    /** 预取前后各 1 周；只读桥方法，不改变 Dart 侧的浏览周。 */
    private fun prefetchNeighbours(week: Int) {
        val total = _state.value.state?.totalWeeks ?: return
        listOf(week - 1, week + 1)
            .filter { it in 1..total && !weekCache.containsKey(it) }
            .forEach { neighbour ->
                viewModelScope.launch {
                    // 让首帧先画出来，再补两侧的相邻周。
                    kotlinx.coroutines.delay(220)
                    runCatching { api.week(neighbour) }
                        .getOrNull()
                        ?.let {
                            weekCache[it.week] = it
                            _state.update { s -> s.copy(cachedWeeks = weekCache.toMap()) }
                        }
                }
            }
    }

    fun refresh() = load(force = true)

    /** 调课 / 恢复之后重新取当前浏览周（数据本身在 Dart 侧已经变了）。 */
    fun reloadWeek() = reloadVisibleWeek()

    fun consumeActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }

    /** 保存课程表单；`scope` 为 `thisWeek` 时只改这一次课。 */
    /** 表单里的冲突提示：与已有课程的重叠时段，不阻止保存。 */
    suspend fun courseConflicts(input: CourseFormInput): String? = api.conflicts(
        courseId = input.courseId,
        day = input.day,
        startNode = input.startNode,
        endNode = input.endNode,
        weeks = input.weeks,
    )

    /** 拉取某一格的完整详情：字段、已做的本地改动、可恢复项。 */
    /** WakeUp 导出的预览；课表不完整时回来的 `ok = false` 带一句可展示的提示。 */
    suspend fun exportPreview(): TimetableExportPreview = api.exportPreview()

    /** 让社团服务把课表转成 WakeUp 口令。 */
    suspend fun exportWakeup(): TimetableExportShare = api.exportWakeup()

    suspend fun cellDetail(block: CourseBlock, week: Int): TimetableCellDetail = api.cellDetail(
        courseId = block.courseId,
        day = block.day,
        startNode = block.startNode,
        endNode = block.endNode,
        week = week,
    )

    fun saveCourse(
        input: CourseFormInput,
        week: Int,
        scope: String,
        applyWeeks: List<Int>? = null,
    ) = runAction {
        val changed = api.saveCourse(
            courseId = input.courseId,
            day = input.day,
            startNode = input.startNode,
            endNode = input.endNode,
            week = week,
            scope = scope,
            name = input.name,
            teacher = input.teacher,
            location = input.location,
            note = input.note,
            kind = input.kind,
            weeks = input.weeks,
            userColor = input.userColor,
            colorChanged = input.colorChanged,
            applyWeeks = applyWeeks,
        )
        if (changed) "已保存" else "没有需要保存的改动"
    }

    fun deleteCourse(block: CourseBlock, week: Int, scope: String) = runAction {
        val deleted = api.deleteCourse(
            courseId = block.courseId,
            day = block.day,
            startNode = block.startNode,
            endNode = block.endNode,
            week = week,
            scope = scope,
        )
        if (deleted) "已删除" else "没有需要删除的内容"
    }

    /** 换课的颜色：课程级设置（整门课一起变），`argb == null` 表示恢复自动配色。 */
    fun setCourseColor(block: CourseBlock, argb: Int?) = runAction {
        val changed = api.setCourseColor(block.courseId, argb)
        if (changed) {
            // 格子里的颜色来自 Dart 下发的周数据，改完必须重取这一周，否则界面还是旧颜色。
            reloadVisibleWeek()
            if (argb == null) "已恢复自动配色" else "已更新颜色"
        } else {
            "颜色没有变化"
        }
    }

    /** 拖动调课：`scope` 为 `thisWeek` / `fromThisWeek` / `allWeeks`。 */
    fun moveCourse(
        block: CourseBlock,
        week: Int,
        toDay: Int,
        toStart: Int,
        scope: String,
    ) = runAction {
        val error = api.moveCell(
            courseId = block.courseId,
            day = block.day,
            startNode = block.startNode,
            endNode = block.endNode,
            week = week,
            toDay = toDay,
            toStart = toStart,
            scope = scope,
        )
        when (error) {
            null -> "已调整上课时间"
            "notInWeek" -> "这一周没有这门课"
            "pastLastPeriod" -> "超出最后一节"
            "invalidTarget" -> "目标位置无效"
            "invalidSlot" -> "这一格不能移动"
            "noChange" -> "位置没有变化"
            else -> "调课失败"
        }
    }

    /** 一场日程的详情（考试块点开时用）。 */
    suspend fun eventDetail(id: String): dev.duohuo.nuist.sta.compose.data.timetable.TimetableEventDetail =
        api.eventDetail(id)

    fun saveEvent(
        id: String,
        title: String,
        date: String,
        start: String,
        end: String,
        location: String,
        note: String,
    ) = runAction {
        if (api.saveEvent(id, title, date, start, end, location, note)) "已保存日程" else "日程没有变化"
    }

    /** 新增考试 / 其他日程；成功后刷新可见周与考试列表。 */
    fun addEvent(
        kind: String,
        title: String,
        date: String,
        start: String,
        end: String,
        location: String,
        note: String,
        courseCode: String = "",
    ) = runAction {
        val id = api.addEvent(kind, title, date, start, end, location, note, courseCode)
            ?: throw IllegalStateException("没有保存成功，请检查时间后重试")
        load()
        loadExams()
        if (kind == "exam") "已添加考试" else "已添加日程"
    }

    fun deleteEvent(id: String) = runAction {
        if (api.deleteEvent(id)) "已删除日程" else "没有需要删除的内容"
    }

    fun restoreEvent(id: String) = runAction {
        if (api.restoreEvent(id)) "已恢复原考试" else "没有可恢复的改动"
    }

    /** 把考试块拖到新的星期/节次（一场日程是单次事件，没有作用范围）。 */
    fun moveEvent(id: String, week: Int, day: Int, startNode: Int, span: Int) = runAction {
        if (api.moveEvent(id, week, day, startNode, span)) "已调整考试时间" else "位置没有变化"
    }

    /** 课程详情里的「恢复这一天 / 恢复按周修改 / 恢复原位置」。 */
    fun restoreChange(block: CourseBlock, scope: String) = runAction {
        val ok = api.restoreChanges(
            courseId = block.courseId,
            day = block.day,
            startNode = block.startNode,
            endNode = block.endNode,
            scope = scope,
        )
        if (!ok) throw IllegalStateException("恢复失败")
        when (scope) {
            "occurrence" -> "已恢复这一格的修改"
            "position" -> "已恢复原位置"
            else -> "已恢复"
        }
    }

    fun restoreCourse(courseId: String) = runAction {
        if (api.restoreCourse(courseId)) "已恢复成教务课程" else "没有可恢复的改动"
    }

    /** 编辑类操作的统一外壳：执行 → 重取当前周 → 给界面一条反馈。 */
    private fun runAction(action: suspend () -> String) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { message ->
                    _state.update { it.copy(actionMessage = message) }
                    load()
                }
                .onFailure { e ->
                    _state.update { it.copy(actionMessage = e.toUserMessage()) }
                }
        }
    }

    private fun toExamItem(event: dev.duohuo.nuist.sta.compose.data.timetable.TimetableEvent): ExamItem? {
        if (!event.isExam) return null
        return runCatching {
            ExamItem(
                courseCode = event.courseCode,
                title = event.title,
                date = LocalDate.parse(event.date),
                start = LocalTime.parse(event.start),
                end = LocalTime.parse(event.end),
                location = event.location,
                note = event.note,
                week = event.examWeek,
                eventId = event.id,
            )
        }.getOrNull()
    }
}