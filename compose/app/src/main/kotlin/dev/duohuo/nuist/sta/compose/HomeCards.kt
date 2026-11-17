package dev.duohuo.nuist.sta.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.EntryCard
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import dev.duohuo.nuist.sta.compose.features.bulletin.BulletinMock
import dev.duohuo.nuist.sta.compose.features.electricity.ElecMock
import dev.duohuo.nuist.sta.compose.features.timetable.TimetableMock

/**
 * 首页三段式信息区（方案 A）。
 *
 * 视觉统一规则：
 * 1. 页面只允许「SegmentedColumn 分组 + BaseWidget 行」这一种信息形态，
 *    与学习页 / 我的 / 主题设置完全同构；
 * 2. 分组内每行等高（56/72dp 自适应），左侧图标统一 Material Symbols；
 * 3. 数字指标走 titleMedium 强调，正文说明走 bodyMedium / onSurfaceVariant；
 * 4. 并排入口统一用共享 [EntryCard]（与学习页三入口同款）。
 */

/** 组 1：今日概览 —— 下一节课 / 宿舍电费 / 待考科目。 */
@Composable
internal fun HomeTodaySection(
    onOpenTimetable: () -> Unit,
    onOpenElectricity: () -> Unit,
) {
    val todayBlocks = TimetableMock.blocks.filter { it.day == 3 }.sortedBy { it.startNode }
    val next = todayBlocks.firstOrNull()
    val nextTitle = next?.name ?: "今天没有课"
    val nextDesc = next?.let {
        val (start, _) = TimetableMock.periodTimes[it.startNode - 1]
        "$start · ${it.location.ifEmpty { "地点待定" }} · 第 ${it.startNode}-${it.endNode} 节 · 今日共 ${todayBlocks.size} 节"
    } ?: "今日共 0 节"

    val room = ElecMock.room
    val exam = HomeExamMock.exams.first()

    SegmentedColumn(title = "今日概览") {
        item {
            BaseWidget(
                icon = "calendar_month",
                title = nextTitle,
                description = nextDesc,
                onClick = onOpenTimetable,
                trailingContent = { Chevron() },
            )
        }
        item {
            BaseWidget(
                icon = "bolt",
                title = String.format("%.1f 度", ElecMock.latestKwh),
                description = "${room.displayName} · 更新于 11月16日 13:40",
                onClick = onOpenElectricity,
                trailingContent = { Chevron() },
            )
        }
        item {
            BaseWidget(
                icon = "event",
                title = exam.course,
                description = "${exam.date} ${exam.time} · ${exam.location} · ${exam.seat} 座 · 共 ${HomeExamMock.exams.size} 场",
            )
        }
    }
}

/** 组 2：信息公告 —— 最新三条 + 查看全部。 */
@Composable
internal fun HomeBulletinSection(onOpenAll: () -> Unit) {
    val items = BulletinMock.items.take(3)
    SegmentedColumn(title = "信息公告") {
        items.forEach { b ->
            item {
                BaseWidget(
                    icon = "campaign",
                    title = b.title,
                    titleStyle = MaterialTheme.typography.titleMedium,
                    description = "${b.publisher.ifEmpty { "学校信息公告栏" }} · ${b.publishedAt}",
                    onClick = onOpenAll,
                    trailingContent = { Chevron() },
                )
            }
        }
        item {
            BaseWidget(
                icon = "inbox",
                title = "查看全部公告",
                description = "共 ${BulletinMock.items.size} 条 · 按主题与学院筛选",
                onClick = onOpenAll,
                trailingContent = { Chevron() },
            )
        }
    }
}

/** 组 3：校园导览 —— 并排入口（与学习页入口同款）。 */
@Composable
internal fun HomeEntrySection(onOpen: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EntryCard("meeting_room", "空教室", Modifier.weight(1f)) { onOpen("free-classroom") }
        EntryCard("map", "校园地图", Modifier.weight(1f)) { onOpen("map") }
    }
}

@Composable
private fun Chevron() {
    SymbolIcon(
        name = "chevron_right",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

/** 考试安排 mock（初开发阶段仅首页展示，暂无独立页面）。 */
private object HomeExamMock {
    data class Exam(
        val course: String,
        val date: String,
        val time: String,
        val location: String,
        val seat: String,
    )

    val exams = listOf(
        Exam("高等数学 A", "1月12日", "08:00 – 10:00", "文德楼 305", "12"),
        Exam("大学物理", "1月14日", "14:00 – 16:00", "明德楼 201", "07"),
        Exam("程序设计基础", "1月16日", "08:00 – 10:00", "实验楼 A402", "31"),
        Exam("思想道德与法治", "1月19日", "10:30 – 12:30", "文德楼 108", "05"),
    )
}
