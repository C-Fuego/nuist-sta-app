package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import dev.duohuo.nuist.sta.compose.data.timetable.TimetableDaySwap
import kotlinx.serialization.Serializable

/**
 * 课表（简化只读周视图）的展示模型。
 *
 * 说明（初开发阶段）：这里只做「周格网 + 课程块」的只读呈现，不包含拖拽调课、
 * 周次切换、考试合并等交互能力——那些留待后续迭代，避免过早引入复杂状态与错误。
 */
@Serializable
data class CourseBlock(
    /** 控制器里的课程 id：编辑、调课、删除都按它定位。 */
    val courseId: String = "",
    val name: String,
    val day: Int,          // 1=周一 … 7=周日
    val startNode: Int,    // 起始节（1-based）
    val endNode: Int,      // 结束节（含）
    val location: String = "",
    val teacher: String = "",
    val colorIndex: Int = 0,
    /** 这门课出现的教学周（1-based）。空集合表示「未排课/不限周」，由界面决定如何展示。 */
    val weeks: Set<Int> = emptySet(),
    /** 教务给的周次中文，如 "1-16周"；仅用于展示。 */
    val weeksLabel: String = "",
    /** 教务原始周次（未经「只改本周」等覆盖裁剪），单/双周角标按它判断。 */
    val origWeeks: Set<Int> = emptySet(),
    /** 教务实验标记（SFSYK）：格内标「实验」胶囊。 */
    val isExperimental: Boolean = false,
    /** 考试时段：格内标「考试」胶囊。 */
    val isExam: Boolean = false,
    /** 本地手动添加的课程（教务课程为 false）。 */
    val isManual: Boolean = false,
    /** 课程类型：`standard` / `lab` / `pe` / `exam`。 */
    val kind: String = "standard",
    /**
     * 用户自选色的原始 ARGB；为空表示用 [colorIndex] 指向的色板色。
     * 自选色是课程级设置，切周、翻页都不会变。
     */
    val userColor: Int? = null,
    /**
     * 这一周是否真的上这门课。来自 Dart `cellsForDay` 的 `activeThisWeek`：
     * 为空周的格子（开启「显示非本周课程」时）是 false，画成灰格且不参与「N 门」。
     */
    val activeThisWeek: Boolean = true,
)

/**
 * 一次课表抓取的快照。教务只提供「某一学期」的当前状态，缓存它即可，
 * 界面上的周次切换全部在内存里做（不重新请求）。
 */
@Serializable
data class TimetableSnapshot(
    val termCode: String,
    val termLabel: String,
    val currentWeek: Int,
    val currentDay: Int,
    val totalWeeks: Int,
    /** 节次作息：(开始, 结束)，下标 +1 即节次序号。 */
    val periods: List<Pair<String, String>>,
    val courses: List<CourseBlock>,
    val unscheduled: List<CourseBlock>,
    val fetchedAtMillis: Long,
    /** 整天调课记录：表头把被换过的日期标出来，点开可撤销。 */
    val daySwaps: List<TimetableDaySwap> = emptyList(),
)

object TimetableMock {
    /** 节次时间（展示用，取常见作息）。 */
    val periodTimes = listOf(
        "08:00" to "08:45", "08:50" to "09:35", "09:50" to "10:35", "10:40" to "11:25",
        "11:30" to "12:15", "13:30" to "14:15", "14:20" to "15:05", "15:20" to "16:05",
        "16:10" to "16:55", "18:30" to "19:15", "19:20" to "20:05", "20:10" to "20:55",
    )

    val weekLabel = "2026-2027 秋 · 第 12 周"

    /** 学期名（工具栏展示）。 */
    const val TERM_LABEL = "2026-2027 秋"

    /** 当前教学周（mock 固定第 12 周）。 */
    const val CURRENT_WEEK = 12

    /** 学期总周数。 */
    const val TOTAL_WEEKS = 16

    val blocks = listOf(
        CourseBlock(name = "高等数学 A", day = 1, startNode = 1, endNode = 2, location = "文德楼 305", teacher = "李老师", colorIndex = 0),
        CourseBlock(name = "大学物理", day = 1, startNode = 3, endNode = 4, location = "博学楼 201", teacher = "王老师", colorIndex = 1),
        CourseBlock(name = "程序设计基础", day = 1, startNode = 7, endNode = 8, location = "实验楼 A402", teacher = "张老师", colorIndex = 2),
        CourseBlock(name = "线性代数", day = 2, startNode = 1, endNode = 2, location = "文德楼 108", teacher = "赵老师", colorIndex = 3),
        CourseBlock(name = "体育", day = 2, startNode = 5, endNode = 6, location = "东操场", teacher = "刘老师", colorIndex = 4),
        CourseBlock(name = "高等数学 A", day = 3, startNode = 1, endNode = 2, location = "文德楼 305", teacher = "李老师", colorIndex = 0),
        CourseBlock(name = "大学物理", day = 3, startNode = 3, endNode = 4, location = "博学楼 201", teacher = "王老师", colorIndex = 1),
        CourseBlock(name = "数据结构", day = 3, startNode = 9, endNode = 10, location = "博学楼 302", teacher = "陈老师", colorIndex = 5),
        CourseBlock(name = "程序设计基础", day = 4, startNode = 3, endNode = 4, location = "实验楼 A402", teacher = "张老师", colorIndex = 2),
        CourseBlock(name = "线性代数", day = 4, startNode = 7, endNode = 8, location = "文德楼 108", teacher = "赵老师", colorIndex = 3),
        CourseBlock(name = "高等数学 A", day = 5, startNode = 1, endNode = 2, location = "文德楼 305", teacher = "李老师", colorIndex = 0),
        CourseBlock(name = "数据结构", day = 5, startNode = 5, endNode = 6, location = "博学楼 302", teacher = "陈老师", colorIndex = 5),
    )
}
