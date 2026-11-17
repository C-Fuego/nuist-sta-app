package dev.duohuo.nuist.sta.compose.features.score

import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn
import dev.duohuo.nuist.sta.compose.core.theme.settings.SingleChoiceDialog

/**
 * 成绩查询页面（Compose + MD3E）。对应 Flutter `ScorePage`。
 * UI 优先阶段：数据 mock；概览卡 + 学期选择 + 课程列表。
 */
@Composable
fun ScoreScreen(vm: ScoreViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ScoreContent(state = state, onSelectTerm = vm::selectTerm, onRefresh = vm::refresh)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScoreContent(
    state: ScoreUiState,
    onSelectTerm: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("成绩查询", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton() },
                                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TermSelector(
                    selectedTerm = state.selectedTerm,
                    terms = state.terms,
                    onSelectTerm = onSelectTerm,
                )
            }
            item {
                OverviewCard(report = state.report, onRefresh = onRefresh)
            }
            // 课程成绩：与首页/公告/设置同构的行式分组（不再用 ElevatedCard）
            item {
                SegmentedColumn(title = "课程成绩 · ${state.report.records.size} 门") {
                    state.report.records.forEach { r ->
                        item {
                            BaseWidget(
                                icon = "school",
                                title = r.courseName,
                                titleStyle = MaterialTheme.typography.titleMedium,
                                description = details(r),
                                isError = r.failed,
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            r.score.ifEmpty { "--" },
                                            style = MaterialTheme.typography.titleLargeEmphasized,
                                            color = if (r.failed) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                        if (r.passStatus.isNotEmpty()) {
                                            Text(
                                                r.passStatus,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (r.failed) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermSelector(
    selectedTerm: String,
    terms: List<String>,
    onSelectTerm: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SegmentedColumn(title = "学期") {
        item {
            BaseWidget(
                icon = "calendar_month",
                title = selectedTerm,
                description = "学期成绩单",
                trailingContent = {
                    SymbolIcon("chevron_right",
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = { showDialog = true },
            )
        }
    }
    if (showDialog) {
        SingleChoiceDialog(
            title = "选择学期",
            items = terms,
            selectedIndex = terms.indexOf(selectedTerm),
            onDismiss = { showDialog = false },
            onSelect = { index ->
                showDialog = false
                terms.getOrNull(index)?.let(onSelectTerm)
            },
        )
    }
}

@Composable
private fun OverviewCard(report: ScoreReport, onRefresh: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val summary = report.summary
    ElevatedCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon("analytics", contentDescription = null,
                    tint = scheme.primary, modifier = Modifier.size(20.dp), fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("成绩概览", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    SymbolIcon("refresh", contentDescription = "刷新", tint = scheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Metric("课程", "${summary.total} 门", Modifier.weight(1f))
                Metric("平均分", summary.averageScore?.let { String.format("%.1f", it) } ?: "--", Modifier.weight(1f))
                Metric("通过", "${summary.passed} 门", Modifier.weight(1f))
                Metric("学分", formatCredit(summary.credits), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CourseTile(record: ScoreRecord) {
    val scheme = MaterialTheme.colorScheme
    // M3E 取向：常态不上色，只有异常（不及格）才用 error 强调，避免整页红绿橙。
    val color = if (record.failed) scheme.error else scheme.onSurface
    val statusColor = if (record.failed) scheme.error else scheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(record.courseName, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = scheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(details(record), style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(record.score.ifEmpty { "--" }, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = color)
            if (record.passStatus.isNotEmpty()) {
                Text(record.passStatus, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
        }
    }
}

private fun details(r: ScoreRecord): String {
    val v = mutableListOf<String>()
    if (r.credit.isNotEmpty()) v.add("${formatCredit(r.credit.toDoubleOrNull() ?: 0.0)} 学分")
    if (r.gradePoint.isNotEmpty()) v.add("绩点 ${r.gradePoint}")
    if (r.examType.isNotEmpty()) v.add(r.examType)
    if (r.usualScore.isNotEmpty() || r.finalScore.isNotEmpty()) {
        v.add("平时 ${r.usualScore.ifEmpty { "--" }} · 期末 ${r.finalScore.ifEmpty { "--" }}")
    }
    return if (v.isEmpty()) "暂无课程附加信息" else v.joinToString(" · ")
}

private fun formatCredit(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
