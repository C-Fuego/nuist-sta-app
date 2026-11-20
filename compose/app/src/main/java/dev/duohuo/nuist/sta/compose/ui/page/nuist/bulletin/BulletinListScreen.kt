// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.BlankStateArea
import dev.duohuo.nuist.sta.compose.ui.kit.EmptyState
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorBanner
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorState
import dev.duohuo.nuist.sta.compose.ui.kit.InlineLoadingIndicator
import dev.duohuo.nuist.sta.compose.ui.kit.ListSkeleton
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.LocalRefreshFromPull
import dev.duohuo.nuist.sta.compose.ui.kit.NuistPullToRefreshBox
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import org.koin.androidx.compose.koinViewModel

/**
 * 信息公告列表页（Compose + MD3E），数据来自学校信息公告栏。
 *
 * 栏目由服务端分页（[BulletinCategory]），搜索只过滤已经加载的条目；
 * 滚到接近底部时由 [onLoadMore] 追加下一页。
 *
 * 交互约定（全应用统一）：首屏加载是骨架屏，空白态没有按钮（轻点任意处或下拉重试），
 * 有内容时只有下拉刷新，刷新失败在列表顶部出一条可点的错误条。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BulletinListScreen(
    onOpen: (Bulletin) -> Unit = {},
    viewModel: BulletinListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onCategoryChange: (BulletinCategory) -> Unit = viewModel::selectCategory
    val onLoadMore: () -> Unit = viewModel::loadMore
    val onRefresh: () -> Unit = viewModel::refresh
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var query by rememberSaveable { mutableStateOf("") }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }

    val visible = state.items.filter { b ->
        query.isBlank() ||
            b.title.contains(query, ignoreCase = true) ||
            b.publisher.contains(query, ignoreCase = true) ||
            b.officialCategory.contains(query, ignoreCase = true)
    }
    val hasContent = state.items.isNotEmpty()

    val listState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastIndex = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastIndex >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd, state.hasMore, state.isLoading, state.isLoadingMore) {
        if (reachedEnd && state.hasMore && !state.isLoading && !state.isLoadingMore) onLoadMore()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("信息公告", modifier = Modifier.padding(start = 12.dp)) },
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
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            when {
                // 首屏：骨架屏占位，不叠任何加载动画
                !hasContent && state.isLoading && !LocalRefreshFromPull.current -> ListSkeleton(
                    rows = 8,
                    modifier = Modifier.fillMaxSize(),
                )

                // 空白 + 出错：整块可点、可下拉，无按钮
                !hasContent -> {
                    val failure = state.error
                    BlankStateArea {
                        if (failure != null) {
                            ErrorState(
                                message = failure,
                                title = "公告加载失败",
                                onRetry = onRefresh,
                            )
                        } else {
                            EmptyState(
                                title = if (query.isBlank()) "这个栏目暂时没有公告" else "没有匹配的公告",
                                subtitle = if (query.isBlank()) {
                                    "换个栏目，或稍后再来看看"
                                } else {
                                    "换个关键词试试"
                                },
                                icon = "campaign",
                            )
                        }
                    }
                }

                // 有内容：只有下拉刷新；刷新失败在顶部提示一条
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SearchField(query = query, onQueryChange = { query = it })
                    }
                    item {
                        SegmentedColumn(title = "栏目") {
                            item {
                                BaseWidget(
                                    icon = "topic",
                                    title = state.category.label,
                                    description = "学校信息公告栏 · 共 ${state.totalCount} 条",
                                    trailingContent = {
                                        SymbolIcon(
                                            "chevron_right",
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    onClick = { showCategoryDialog = true },
                                )
                            }
                        }
                    }
                    state.error?.let { failure ->
                        item { ErrorBanner(message = failure, onRetry = onRefresh) }
                    }
                    if (visible.isNotEmpty()) {
                        item {
                            SegmentedColumn(title = "公告列表 · ${visible.size} 条") {
                                visible.forEach { b ->
                                    item {
                                        BaseWidget(
                                            icon = if (b.pinned) "push_pin" else "campaign",
                                            title = b.title,
                                            titleStyle = MaterialTheme.typography.titleMedium,
                                            description = buildString {
                                                if (b.officialCategory.isNotEmpty()) {
                                                    append(b.officialCategory).append(" · ")
                                                }
                                                append(b.publisher.ifEmpty { "学校信息公告栏" })
                                                append(" · ")
                                                append(b.publishedAt)
                                            },
                                            onClick = { onOpen(b) },
                                            trailingContent = {
                                                SymbolIcon(
                                                    "chevron_right",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp),
                                                    fontSize = 20.sp,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SymbolIcon(
                                        "search",
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp),
                                        fontSize = 40.sp,
                                    )
                                    Text(
                                        "没有匹配的公告",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (state.isLoadingMore) {
                        item { LoadingRow() }
                    }
                    if (!state.hasMore && hasContent) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "已加载全部 ${state.totalCount} 条",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        SingleChoiceDialog(
            title = "选择栏目",
            items = BulletinCategories.map { it.label },
            selectedIndex = BulletinCategories.indexOfFirst { it == state.category },
            onDismiss = { showCategoryDialog = false },
            onSelect = { index ->
                showCategoryDialog = false
                BulletinCategories.getOrNull(index)?.let(onCategoryChange)
            },
        )
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        InlineLoadingIndicator(size = 24.dp)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = {
            Text(
                "搜索已加载的公告标题 / 单位",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { SymbolIcon("search", contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    SymbolIcon("close", contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun Tag(text: String, highlighted: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val color = if (highlighted) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) scheme.primaryContainer else scheme.surfaceContainerHighest)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}