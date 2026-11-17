package dev.duohuo.nuist.sta.compose.features.free_classroom

/**
 * 空教室查询的数据模型（UI 阶段先用本地 mock，后续接 core:network 时替换来源）。
 * 字段与 Flutter 侧 `free_classroom_models.dart` 对齐，便于之后直接接真实解析。
 */

/** 教学楼。 */
data class Building(val code: String, val name: String)

/** 一个大节（如 1-2、3-4），带起止时间。 */
data class PeriodGroup(
    val start: Int,
    val end: Int,
    val startTime: String,
    val endTime: String,
) {
    val label: String get() = "$start-$end"
}

/** 某间教室在某个大节是否空闲。 */
data class PeriodState(val occupied: Boolean, val typeName: String?)

/** 一间教室在某天的占用情况（每个大节一格）。 */
data class Classroom(
    val name: String,
    val floor: Int,
    val zone: String,
    val seats: Int,
    val periods: List<PeriodState>,
) {
    /** 该教室当天是否全天无课。 */
    val fullyBusy: Boolean get() = periods.all { it.occupied }
    val allFree: Boolean get() = periods.none { it.occupied }
}

/** 某天的整体查询结果。 */
data class ClassroomDay(
    val dateLabel: String,
    val weekdayName: String,
    val week: Int,
    val groups: List<PeriodGroup>,
    val rooms: List<Classroom>,
) {
    val floors: List<Int> get() = rooms.map { it.floor }.distinct().sorted()
    val zones: List<String> get() = rooms.map { it.zone }.distinct().sorted()
}

/** Mock 数据源：UI 阶段先用它把界面撑起来。 */
object FreeClassroomMock {
    val buildings = listOf(
        Building("1-120", "文德楼"),
        Building("1-130", "明德楼"),
        Building("1-140", "笃学楼"),
        Building("1-150", "尚贤楼"),
    )

    private val groups = listOf(
        PeriodGroup(1, 2, "08:00", "09:40"),
        PeriodGroup(3, 4, "10:00", "11:40"),
        PeriodGroup(5, 6, "13:30", "15:10"),
        PeriodGroup(7, 8, "15:30", "17:10"),
        PeriodGroup(9, 11, "18:30", "21:00"),
    )

    private val typeNames = listOf("教学", "考试", "借用", "实验教学")

    fun day(building: Building): ClassroomDay {
        val zones = listOf("N", "C", "S")
        val rooms = mutableListOf<Classroom>()
        for (floor in 1..4) {
            for (idx in 0 until 6) {
                val periods = groups.map { g ->
                    // 伪随机：相同输入稳定，便于看效果
                    val seed = (building.code.hashCode() + floor * 31 + idx * 17 + g.start) and 0xFF
                    val occupied = seed % 5 < 2
                    PeriodState(occupied, if (occupied) typeNames[seed % typeNames.size] else null)
                }
                rooms += Classroom(
                    name = "${floor}0${idx + 1}",
                    floor = floor,
                    zone = zones[idx % 3],
                    seats = 30 + (idx * 10),
                    periods = periods,
                )
            }
        }
        return ClassroomDay(
            dateLabel = "今天",
            weekdayName = "周三",
            week = 12,
            groups = groups,
            rooms = rooms,
        )
    }
}
