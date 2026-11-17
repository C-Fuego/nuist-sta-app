package dev.duohuo.nuist.sta.compose.features.study

import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseItemContainer
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn

/**
 * 学习页卡片（Compose + MD3E）。对应 Flutter 学习页的「学生身份 + 学业进度」。
 *
 * 结构照搬 InstallerX 的分段列表形态：`SegmentedColumn` 分组，
 * 身份行走 [BaseWidget]，进度条/数据格走 [BaseItemContainer] 自定义内容。
 */

/** 学生身份 + 学期进度组。对应 Flutter `StudentInfoCard`。 */
@Composable
fun StudentInfoSection(info: StudentInfo, onSemesterClick: (() -> Unit)? = null) {
    SegmentedColumn(title = "学生信息") {
        item {
            BaseWidget(
                icon = "person",
                title = info.name,
                description = "${info.studentId} · ${info.orgLine}",
            )
        }
        // 学期切换：标准 BaseWidget 行（图标 + 学期 + 周次 + 右箭头），
        // 与 InstallerX 设置页其他行一致，避免把整块进度条当按钮
        item {
            BaseWidget(
                icon = "calendar_month",
                title = info.semester,
                description = "教学周 · 第 ${info.week} / ${info.totalWeeks} 周",
                onClick = onSemesterClick,
                trailingContent = {
                    if (onSemesterClick != null) {
                        SymbolIcon(
                            name = "chevron_right",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                            fontSize = 20.sp,
                        )
                    }
                },
            )
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
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
                    Spacer(Modifier.height(8.dp))
                    LinearWavyProgressIndicator(
                        progress = { s.creditProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
        item {
            BaseItemContainer {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Stat("班级排名", s.classRank, Modifier.weight(1f))
                    Stat("专业排名", s.majorRank, Modifier.weight(1f))
                    Stat("课程通过率", s.passRate, Modifier.weight(1f))
                    Stat("学分加权平均分", s.weightedAverageScore, Modifier.weight(1f))
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
