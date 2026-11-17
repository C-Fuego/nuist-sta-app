package dev.duohuo.nuist.sta.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseItemContainer
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn
import dev.duohuo.nuist.sta.compose.features.bulletin.BulletinMock
import dev.duohuo.nuist.sta.compose.features.electricity.ElecMock
import dev.duohuo.nuist.sta.compose.features.timetable.TimetableMock

/**
 * 首页信息卡片（mock 数据）。单列形态，从上到下：
 * 信息公告 → 宿舍电费 → 今日课表 → 考试安排。
 *
 * 容器样式与学习页一致：`SegmentedColumn`（蓝色分组标题）+ `BaseItemContainer`
 * （surfaceBright 底衬 + 分段圆角），点击整行进入对应页面
 * （考试安排暂无页面，不可点）。全部取色走 colorScheme。
 */

/** 通用首页信息卡：分组标题 + 图标 + 大字指标 + 副标题（可选整行点击）。 */
@Composable
private fun HomeInfoCard(
    title: String,
    icon: ImageVector,
    metric: String,
    metricUnit: String = "",
    subtitle: String,
    onClick: (() -> Unit)? = null,
    metricStyle: TextStyle = MaterialTheme.typography.headlineMediumEmphasized,
    metricMaxLines: Int = 1,
) {
    val scheme = MaterialTheme.colorScheme
    SegmentedColumn(title = title) {
        item {
            BaseItemContainer {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable(enabled = onClick != null) { onClick?.invoke() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
            Box(
                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.large)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = title, tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(metric, style = metricStyle, color = scheme.onSurface,
                        maxLines = metricMaxLines, overflow = TextOverflow.Ellipsis)
                    if (metricUnit.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(metricUnit, style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
                    if (onClick != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null,
                            tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** 信息公告卡：最新一条公告标题 + 单位·日期。 */
@Composable
internal fun HomeBulletinCard(onOpen: () -> Unit) {
    val b = BulletinMock.items.first()
    HomeInfoCard(
        title = "信息公告",
        icon = Icons.Filled.Campaign,
        metric = b.title,
        subtitle = "${b.publisher.ifEmpty { "学校信息公告栏" }} · ${b.publishedAt}",
        onClick = onOpen,
        metricStyle = MaterialTheme.typography.titleLargeEmphasized,
        metricMaxLines = 2,
    )
}

/** 宿舍电费卡：剩余电量 + 宿舍号 + 更新时间。 */
@Composable
internal fun HomeElectricityCard(onOpen: () -> Unit) {
    val room = ElecMock.room
    HomeInfoCard(
        title = "宿舍电费",
        icon = Icons.Filled.Bolt,
        metric = String.format("%.1f", ElecMock.latestKwh),
        metricUnit = "度",
        subtitle = "${room.displayName} · 更新于 11月16日 13:40",
        onClick = onOpen,
    )
}

/** 今日课表卡：今天节数 + 下一节课。 */
@Composable
internal fun HomeTimetableCard(onOpen: () -> Unit) {
    // mock 里"今天"固定为周三（day=3）
    val todayBlocks = TimetableMock.blocks.filter { it.day == 3 }.sortedBy { it.startNode }
    val next = todayBlocks.firstOrNull()
    val nextLine = next?.let {
        val (start, _) = TimetableMock.periodTimes[it.startNode - 1]
        "下一节 ${it.name} $start @${if (it.location.isNotEmpty()) it.location else "待定"}"
    } ?: "今天没有课"
    HomeInfoCard(
        title = "今日课表",
        icon = Icons.Filled.CalendarMonth,
        metric = "${todayBlocks.size}",
        metricUnit = "节课",
        subtitle = "周三 · 第 12 周 · $nextLine",
        onClick = onOpen,
    )
}

/** 考试安排 mock（初开发阶段仅首页卡片展示，暂无独立页面）。 */
private object ExamMock {
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

/** 考试安排卡：下一场考试 + 共 N 场。暂无独立页面，不可点击。 */
@Composable
internal fun HomeExamCard() {
    val next = ExamMock.exams.first()
    HomeInfoCard(
        title = "考试安排",
        icon = Icons.Filled.Event,
        metric = next.course,
        subtitle = "${next.date} ${next.time} · ${next.location} · ${next.seat} 座 · 共 ${ExamMock.exams.size} 场",
    )
}
