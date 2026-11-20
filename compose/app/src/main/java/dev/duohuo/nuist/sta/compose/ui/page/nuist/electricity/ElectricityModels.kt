package dev.duohuo.nuist.sta.compose.ui.page.nuist.electricity

import java.time.LocalDateTime

/** 一次查询到的剩余电量。对应 Flutter `ElecReading`。 */
data class ElecReading(val time: LocalDateTime, val kwh: Double)

/** 用户绑定的宿舍。对应 Flutter `ElecRoom`。 */
data class ElecRoom(
    val campus: String,
    val building: String,
    val room: String,
) {
    val displayName: String get() = "$campus · $building · $room"
}

/** Mock 数据：UI 阶段撑起界面。 */
object ElecMock {
    val room = ElecRoom("沁园", "36栋标准公寓", "315")

    /** 生成最近 N 天的电量读数（稳定伪随机，便于看曲线）。 */
    fun history(days: Int): List<ElecReading> {
        val now = LocalDateTime.of(2026, 11, 16, 14, 0)
        val base = 42.0
        return (0 until days).map { i ->
            val day = now.minusDays((days - 1 - i).toLong())
            val wave = kotlin.math.sin(i * 0.6) * 6.0
            val drift = -i * 0.35
            ElecReading(day, (base + wave + drift).coerceAtLeast(5.0))
        }
    }

    const val latestKwh: Double = 38.6
    const val lowThreshold: Double = 20.0
}
