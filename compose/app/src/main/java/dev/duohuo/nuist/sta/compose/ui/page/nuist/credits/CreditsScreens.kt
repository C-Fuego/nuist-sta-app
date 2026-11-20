// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.credits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.data.credits.CreditItem
import dev.duohuo.nuist.sta.compose.data.credits.InnovationCreditData
import dev.duohuo.nuist.sta.compose.data.credits.LaborResultData
import dev.duohuo.nuist.sta.compose.data.credits.LaborScoreData
import dev.duohuo.nuist.sta.compose.ui.kit.BaseItemContainer
import dev.duohuo.nuist.sta.compose.ui.kit.BlankStateArea
import dev.duohuo.nuist.sta.compose.ui.kit.EmptyState
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorBanner
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorState
import dev.duohuo.nuist.sta.compose.ui.kit.LoadingState
import dev.duohuo.nuist.sta.compose.ui.kit.LocalRefreshFromPull
import dev.duohuo.nuist.sta.compose.ui.kit.NuistPullToRefreshBox
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import org.koin.androidx.compose.koinViewModel

/** 双创学分页面。对应参考实现的 `InnovationCreditPage`。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InnovationCreditScreen(viewModel: InnovationCreditViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("双创学分", modifier = Modifier.padding(start = 12.dp)) },
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
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            val data = state.data
            when {
                data == null && state.loading && !LocalRefreshFromPull.current ->
                    LoadingState(message = "正在获取双创学分…")

                data == null && state.error != null -> BlankStateArea {
                    ErrorState(
                        message = state.error.orEmpty(),
                        title = if (state.needsCredential) "需要登录统一门户" else "双创学分加载失败",
                        needsCredential = state.needsCredential,
                        onRetry = { viewModel.refresh() },
                    )
                }

                data == null -> BlankStateArea {
                    EmptyState(
                        title = "暂无双创学分数据",
                        subtitle = "平台还没有为本人生成学分记录",
                        icon = "lightbulb",
                        onRetry = { viewModel.refresh() },
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.error?.let { message ->
                        item { ErrorBanner(message = message, onRetry = { viewModel.refresh() }) }
                    }
                    item { InnovationSummarySection(data) }
                    if (data.items.isEmpty()) {
                        item { EmptyCard("还没有已认定的学分项目") }
                    } else {
                        item {
                            SegmentedColumn(title = "认定明细") {
                                data.items.forEach { item -> item { BaseItemContainer { CreditItemTile(item) } } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InnovationSummarySection(data: InnovationCreditData) {
    SegmentedColumn(title = "总览") {
        item {
            BaseWidget(
                icon = "lightbulb",
                title = "${data.total.ifEmpty { "--" }} 分",
                description = listOf(
                    "已认定 ${data.items.size} 项",
                    data.grade,
                ).filter { it.isNotEmpty() }.joinToString(" · "),
            )
        }
    }
}

/** 劳动积分页面。对应参考实现的 `LaborScorePage`。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LaborScoreScreen(viewModel: LaborScoreViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("劳动积分", modifier = Modifier.padding(start = 12.dp)) },
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
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            val data = state.data
            when {
                data == null && state.loading && !LocalRefreshFromPull.current ->
                    LoadingState(message = "正在获取劳动积分…")

                data == null && state.error != null -> BlankStateArea {
                    ErrorState(
                        message = state.error.orEmpty(),
                        title = if (state.needsCredential) "需要登录统一门户" else "劳动积分加载失败",
                        needsCredential = state.needsCredential,
                        onRetry = { viewModel.refresh() },
                    )
                }

                data == null -> BlankStateArea {
                    EmptyState(
                        title = "暂无劳动积分数据",
                        subtitle = "平台还没有为本人生成核算结果",
                        icon = "work",
                        onRetry = { viewModel.refresh() },
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.error?.let { message ->
                        item { ErrorBanner(message = message, onRetry = { viewModel.refresh() }) }
                    }
                    item { LaborSummarySection(data) }
                    data.official?.let { official ->
                        item {
                            SegmentedColumn(title = "积分构成") {
                                item { BreakdownItem("理论", official.theory) }
                                item { BreakdownItem("生活", official.life) }
                                item { BreakdownItem("服务", official.service) }
                                item {
                                    BreakdownItem(
                                        "专业（课程 ${official.majorCourse.ifEmpty { "--" }} + 竞赛 ${official.majorContest.ifEmpty { "--" }}）",
                                        official.majorTotal,
                                    )
                                }
                            }
                        }
                    }
                    if (data.liveLife != null || data.liveService != null || data.liveMajor != null) {
                        item {
                            SegmentedColumn(title = "实时统计") {
                                data.liveLife?.let { value -> item { BreakdownItem("生活劳动", value) } }
                                data.liveService?.let { value -> item { BreakdownItem("服务劳动", value) } }
                                data.liveMajor?.let { major ->
                                    item { BreakdownItem("专业劳动累计", major.total) }
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
private fun LaborSummarySection(data: LaborScoreData) {
    val official = data.official
    SegmentedColumn(title = "总览") {
        item {
            BaseWidget(
                icon = "work",
                title = "${official?.total?.ifEmpty { "--" } ?: "--"} 分",
                description = buildList {
                    if (official != null) {
                        if (official.confirmed.isNotEmpty()) add(official.confirmed)
                        if (official.updatedShort.isNotEmpty()) add("核算于 ${official.updatedShort}")
                    }
                    if (isEmpty()) add("平台还没有为本人生成核算结果")
                }.joinToString(" · "),
            )
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun CreditItemTile(item: CreditItem) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, color = scheme.onSurface)
            if (item.categoryPath.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(item.categoryPath, style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOf(item.date, item.batch).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.status.isNotEmpty()) {
                    Tag(
                        item.status,
                        when {
                            item.rejected -> scheme.error
                            item.approved -> scheme.primary
                            else -> scheme.tertiary
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "+${item.score.ifEmpty { "--" }}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
            )
            if (item.isDiscounted) {
                Text("参照 ${item.standardScore}", style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BreakdownItem(label: String, value: String) {
    BaseItemContainer {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(value.ifEmpty { "--" }, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
