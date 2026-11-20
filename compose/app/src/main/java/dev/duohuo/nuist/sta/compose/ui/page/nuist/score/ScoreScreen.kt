package dev.duohuo.nuist.sta.compose.ui.page.nuist.score

import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
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
import org.koin.androidx.compose.koinViewModel
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.BlankStateArea
import dev.duohuo.nuist.sta.compose.ui.kit.EmptyState
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorBanner
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorState
import dev.duohuo.nuist.sta.compose.ui.kit.LoadingState
import dev.duohuo.nuist.sta.compose.ui.kit.LocalRefreshFromPull
import dev.duohuo.nuist.sta.compose.ui.kit.NuistPullToRefreshBox
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.BaseItemContainer
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog

/**
 * 成绩查询页面（Compose + MD3E）。对应 Flutter `ScorePage`。
 * 数据来自教务成绩接口；概览卡 + 学期选择 + 课程列表。
 */
@Composable
fun ScoreScreen(vm: ScoreViewModel = koinViewModel()) {
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
    val hasContent = state.report.records.isNotEmpty() || state.terms.isNotEmpty()
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
        NuistPullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            when {
                !hasContent && state.loading && !LocalRefreshFromPull.current -> LoadingState("正在获取成绩…")

                !hasContent && state.error != null -> BlankStateArea {
                    ErrorState(
                        message = state.error.orEmpty(),
                        title = if (state.needsCredential) "需要登录统一门户" else "成绩加载失败",
                        needsCredential = state.needsCredential,
                        onRetry = onRefresh,
                    )
                }

                !hasContent -> BlankStateArea {
                    EmptyState(
                        title = "暂无成绩",
                        subtitle = "教务系统还没有录入成绩",
                        icon = "school",
                        onRetry = onRefresh,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.error?.let { failure ->
                        item {
                            ErrorBanner(message = failure, onRetry = onRefresh)
                        }
                    }
                    if (state.terms.isNotEmpty()) {
                        item {
                            TermSelector(
                                selectedTerm = state.selectedTerm,
                                terms = state.terms,
                                onSelectTerm = onSelectTerm,
                            )
                        }
                    }
                    item {
                        OverviewCard(report = state.report)
                    }
                    // 课程成绩：与首页/公告/设置同构的行式分组（不再用 ElevatedCard）
                    item {
                        SegmentedColumn(title = "课程成绩 · ${state.report.records.size} 门") {
                            if (state.report.records.isEmpty() && !state.loading) {
                                item {
                                    BaseWidget(
                                        icon = "inbox",
                                        title = "本学期暂无成绩",
                                        description = "教务系统还没有录入这个学期的成绩",
                                    )
                                }
                            }
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
private fun OverviewCard(report: ScoreReport) {
    val scheme = MaterialTheme.colorScheme
    val summary = report.summary
    SegmentedColumn(title = "成绩概览") {
        item {
            BaseItemContainer {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Metric("课程", "${summary.total} 门", Modifier.weight(1f))
                Metric("平均分", summary.averageScore?.let { String.format("%.1f", it) } ?: "--", Modifier.weight(1f))
                Metric("通过", "${summary.passed} 门", Modifier.weight(1f))
                Metric("学分", formatCredit(summary.credits), Modifier.weight(1f))
            }
        }
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
