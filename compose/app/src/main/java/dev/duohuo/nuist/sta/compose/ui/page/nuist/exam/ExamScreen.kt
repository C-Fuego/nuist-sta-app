// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.data.exam.ExamItem
import dev.duohuo.nuist.sta.compose.ui.kit.BlankStateArea
import dev.duohuo.nuist.sta.compose.ui.kit.EmptyState
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorBanner
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorState
import dev.duohuo.nuist.sta.compose.ui.kit.LoadingState
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.NuistPullToRefreshBox
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.examKey
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("M月d日")
private val weekFormatter = DateTimeFormatter.ofPattern("E")

/**
 * 考试安排：教务处发布的考试都会出现在这里，按「待考 / 已结束」分组。
 *
 * 学期码与课表共用（`TimetableViewModel`），课表里有学期才查得动考试。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExamScreen(
    onBack: () -> Unit,
    onOpenPortal: () -> Unit = {},
    /**
     * 点某场考试后跳到课表对应位置：周次、星期（1 起）、起始节次（1 起）、考试身份。
     *
     * 课表页会用 [examKey] 把那场考试临时画出来并描一圈，即使「显示考试」是关着的。
     */
    onOpenTimetable: (week: Int, day: Int, startNode: Int, examKey: String) -> Unit = { _, _, _, _ -> },
    viewModel: ExamViewModel = koinViewModel(),
    timetableViewModel: TimetableViewModel = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timetable by timetableViewModel.state.collectAsStateWithLifecycle()
    val termCode = timetable.snapshot?.termCode
    val scheme = MaterialTheme.colorScheme
    // 考试跳到课表的位置：与网格里的落位共用同一套规则（时间重叠 → 覆盖节次）。
    val jumpTarget: (ExamItem) -> Triple<Int, Int, Int>? = { exam ->
        val snapshot = timetable.snapshot
        if (snapshot == null) {
            null
        } else {
            val periods = snapshot.periods.ifEmpty { dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableMock.periodTimes }
            val range = dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.ExamGrid.nodeRange(periods, exam.start, exam.end)
            val week = dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.ExamGrid.weekOf(
                date = exam.date,
                today = java.time.LocalDate.now(),
                currentWeek = snapshot.currentWeek,
                weekStartsMonday = true,
            )
            if (range == null || week !in 1..snapshot.totalWeeks) {
                null
            } else {
                Triple(week, exam.date.dayOfWeek.value, range.first)
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 课表状态全应用共享：已有新鲜数据就直接用，不再单独发起一次同步。
    LaunchedEffect(Unit) { timetableViewModel.load() }
    // 考试与课表同源：课表那边编辑完日程会刷新自己的考试列表，这里跟着重读一次，
    // 从课表跳走再回来时看到的就是改后的时间。
    LaunchedEffect(termCode, timetable.exams) { termCode?.let { viewModel.refresh(it) } }

    val now = LocalDateTime.now()
    val upcoming = state.items.filter { LocalDateTime.of(it.date, it.end).isAfter(now) }
    val finished = state.items.filterNot { LocalDateTime.of(it.date, it.end).isAfter(now) }
    val hasContent = state.items.isNotEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("考试安排", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton(onBack = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surfaceContainer,
                    titleContentColor = scheme.onBackground,
                    scrolledContainerColor = scheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        NuistPullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { termCode?.let { viewModel.refresh(it, force = true) } },
        ) {
            when {
                !hasContent && state.loading -> LoadingState("正在获取考试安排…", Modifier.padding(inner))
                !hasContent && state.error != null -> BlankStateArea {
                    ErrorState(
                        message = state.error ?: "",
                        title = if (state.needsCredential) "需要登录统一门户" else "考试安排加载失败",
                        needsCredential = state.needsCredential,
                        onRetry = { termCode?.let { viewModel.refresh(it, force = true) } },
                        onOpenPortal = onOpenPortal,
                    )
                }
                !hasContent && termCode == null -> BlankStateArea {
                    EmptyState(
                        title = "课表还没同步",
                        subtitle = "拿到学期之后才能查考试安排",
                        icon = "event",
                    )
                }
                !hasContent -> BlankStateArea {
                    EmptyState(
                        title = "暂无考试安排",
                        subtitle = "教务处还没有发布本学期的考试",
                        icon = "event",
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.error?.let {
                        item { ErrorBanner(message = it) { termCode?.let { t -> viewModel.refresh(t, force = true) } } }
                    }
                    if (upcoming.isNotEmpty()) {
                        item {
                            SegmentedColumn(title = "待考") {
                                upcoming.forEach { exam ->
                                    item {
                                        ExamRow(exam, onClick = {
                                            jumpTarget(exam)?.let { (week, day, node) ->
                                                onOpenTimetable(week, day, node, examKey(exam))
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    }
                    if (finished.isNotEmpty()) {
                        item {
                            SegmentedColumn(title = "已结束") {
                                finished.forEach { exam ->
                                    item {
                                        ExamRow(exam, dimmed = true, onClick = {
                                            jumpTarget(exam)?.let { (week, day, node) ->
                                                onOpenTimetable(week, day, node, examKey(exam))
                                            }
                                        })
                                    }
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
private fun ExamRow(exam: ExamItem, dimmed: Boolean = false, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget(
        icon = "edit",
        iconColor = if (dimmed) scheme.onSurfaceVariant else scheme.primary,
        title = exam.title,
        titleStyle = MaterialTheme.typography.titleMedium,
        description = buildString {
            append("${exam.date.format(dateFormatter)} ${exam.date.format(weekFormatter)} · ${exam.timeLabel.substringAfter(' ')}")
            if (exam.location.isNotEmpty()) append(" · ${exam.location}")
        },
        onClick = onClick,
    )
}
