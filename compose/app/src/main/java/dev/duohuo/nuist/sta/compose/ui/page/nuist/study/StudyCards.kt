package dev.duohuo.nuist.sta.compose.ui.page.nuist.study

import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.ui.kit.BaseItemContainer
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn

/**
 * 学习页卡片（Compose + MD3E）。对应 Flutter 学习页的「学生身份 + 学业进度」。
 *
 * 结构照搬参考实现的分段列表形态：`SegmentedColumn` 分组，
 * 身份行走 [BaseWidget]，进度条/数据格走 [BaseItemContainer] 自定义内容。
 */

/** 教学周：学期名 + 第几周 / 总周数。学生身份已统一收到「统一门户」页，这里不再重复。 */
@Composable
fun SemesterSection(
    semester: String,
    week: Int,
    totalWeeks: Int,
    onSemesterClick: (() -> Unit)? = null,
) {
    val progress = if (week > 0 && totalWeeks > 0) {
        (week.toFloat() / totalWeeks.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    SegmentedColumn(title = "教学周") {
        item {
            BaseItemContainer(onClick = onSemesterClick) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SymbolIcon(
                            "calendar_month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                            fontSize = 22.sp,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                semester,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (week > 0 && totalWeeks > 0) {
                                    "第 $week / $totalWeeks 周"
                                } else {
                                    "周次未同步"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (onSemesterClick != null) {
                            SymbolIcon(
                                name = "chevron_right",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                                fontSize = 20.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
    }
}


/** 学业进度组。对应 Flutter `AcademicSummaryCard`。 */
@Composable
fun AcademicSection(s: AcademicSummary) {
    SegmentedColumn(title = "学业进度") {
        item {
            BaseItemContainer {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Metric("平均绩点", s.averageGradePoint, Modifier.weight(1f))
                    Metric("GPA", s.gpa, Modifier.weight(1f))
                    Metric("平均分", s.averageScore, Modifier.weight(1f))
                }
            }
        }
        item {
            BaseItemContainer {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "学分进度",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "已获 ${s.earnedCredits} / 应修 ${s.requiredCredits}　${(s.creditProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearWavyProgressIndicator(
                        progress = { s.creditProgress },
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
        item {
            BaseItemContainer {
                // 两行两列：窄屏下 4 列会把「学分加权平均分」挤成两行
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("班级排名", s.classRank, Modifier.weight(1f))
                        Stat("专业排名", s.majorRank, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Stat("课程通过率", s.passRate, Modifier.weight(1f))
                        Stat("学分加权平均分", s.weightedAverageScore, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(end = 12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value.ifEmpty { "--" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
