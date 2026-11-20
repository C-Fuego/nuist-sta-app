// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.BlankStateArea
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorBanner
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorState
import dev.duohuo.nuist.sta.compose.ui.kit.LoadingState
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.LocalRefreshFromPull
import dev.duohuo.nuist.sta.compose.ui.kit.NuistPullToRefreshBox
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import org.koin.androidx.compose.koinViewModel

/** 公告详情页（Compose + MD3E）。对应 Flutter `BulletinDetailPage`（正文块 + 附件）。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BulletinDetailScreen(
    url: String,
    onBack: () -> Unit,
    viewModel: BulletinDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(url) { viewModel.load(url) }
    val onRetry: () -> Unit = viewModel::retry
    val onRefresh: () -> Unit = viewModel::refresh
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshError by viewModel.refreshError.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("公告详情", modifier = Modifier.padding(start = 12.dp)) },
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
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            when (val s = state) {
                BulletinDetailUiState.Loading -> if (!LocalRefreshFromPull.current) {
                    LoadingState(message = "正在加载公告…")
                }

                is BulletinDetailUiState.Error -> BlankStateArea {
                    ErrorState(
                        message = s.message,
                        title = "公告加载失败",
                        onRetry = onRetry,
                    )
                }

                is BulletinDetailUiState.Content -> {
                    val detail = s.detail
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        refreshError?.let { message ->
                            item { ErrorBanner(message = message, onRetry = onRefresh) }
                        }
                        item {
                            Text(
                                detail.bulletin.title,
                                style = MaterialTheme.typography.headlineSmallEmphasized,
                                color = scheme.onSurface,
                                lineHeight = 34.sp,
                            )
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    detail.bulletin.publisher.ifEmpty { "学校信息公告栏" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.primary,
                                )
                                Text(
                                    "  ·  ",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant,
                                )
                                Text(
                                    detail.bulletin.publishedAt,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                        item {
                            HorizontalDivider(
                                color = scheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                        if (detail.blocks.isEmpty()) {
                            item {
                                Text(
                                    "该公告正文是附件（PDF / 文档），请打开下方附件或原文查看。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(detail.blocks.size) { i ->
                            when (val block = detail.blocks[i]) {
                                is DetailBlock.Heading -> Text(
                                    block.text,
                                    style = if (block.level <= 2) MaterialTheme.typography.titleMediumEmphasized
                                    else MaterialTheme.typography.titleSmallEmphasized,
                                    color = scheme.onSurface,
                                    modifier = Modifier.padding(top = 8.dp),
                                )

                                is DetailBlock.Paragraph -> Text(
                                    block.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                    lineHeight = 28.sp,
                                )

                                is DetailBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    block.items.forEach { item ->
                                        Row {
                                            Text("•  ", style = MaterialTheme.typography.bodyLarge,
                                                color = scheme.primary)
                                            Text(item, style = MaterialTheme.typography.bodyLarge,
                                                color = scheme.onSurface, lineHeight = 24.sp)
                                        }
                                    }
                                }
                            }
                        }
                        if (detail.attachments.isNotEmpty()) {
                            item {
                                SegmentedColumn(title = "附件") {
                                    detail.attachments.forEach { attachment ->
                                        item {
                                            BaseWidget(
                                                icon = "attach_file",
                                                title = attachment.name,
                                                description = "点击用浏览器打开",
                                                onClick = { runCatching { uriHandler.openUri(attachment.url) } },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SegmentedColumn(title = "来源") {
                                item {
                                    BaseWidget(
                                        icon = "open_in_new",
                                        title = "在学校公告栏中打开原文",
                                        description = detail.bulletin.url,
                                        onClick = {
                                            runCatching { uriHandler.openUri(detail.bulletin.url) }
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