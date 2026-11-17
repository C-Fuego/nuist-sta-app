package dev.duohuo.nuist.sta.compose.features.timetable

/**
 * 课表（简化只读周视图）的展示模型。
 *
 * 说明（初开发阶段）：这里只做「周格网 + 课程块」的只读呈现，不包含拖拽调课、
 * 周次切换、考试合并等交互能力——那些留待后续迭代，避免过早引入复杂状态与错误。
 */
data class CourseBlock(
    val name: String,
    val day: Int,          // 1=周一 … 7=周日
    val startNode: Int,    // 起始节（1-based）
    val endNode: Int,      // 结束节（含）
    val location: String = "",
    val teacher: String = "",
    val colorIndex: Int = 0,
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
        CourseBlock("高等数学 A", 1, 1, 2, "文德楼 305", "李老师", 0),
        CourseBlock("大学物理", 1, 3, 4, "博学楼 201", "王老师", 1),
        CourseBlock("程序设计基础", 1, 7, 8, "实验楼 A402", "张老师", 2),
        CourseBlock("线性代数", 2, 1, 2, "文德楼 108", "赵老师", 3),
        CourseBlock("体育", 2, 5, 6, "东操场", "刘老师", 4),
        CourseBlock("高等数学 A", 3, 1, 2, "文德楼 305", "李老师", 0),
        CourseBlock("大学物理", 3, 3, 4, "博学楼 201", "王老师", 1),
        CourseBlock("数据结构", 3, 9, 10, "博学楼 302", "陈老师", 5),
        CourseBlock("程序设计基础", 4, 3, 4, "实验楼 A402", "张老师", 2),
        CourseBlock("线性代数", 4, 7, 8, "文德楼 108", "赵老师", 3),
        CourseBlock("高等数学 A", 5, 1, 2, "文德楼 305", "李老师", 0),
        CourseBlock("数据结构", 5, 5, 6, "博学楼 302", "陈老师", 5),
    )
}
