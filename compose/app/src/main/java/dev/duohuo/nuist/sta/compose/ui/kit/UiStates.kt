// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全应用唯一的加载动画：Material 3 Expressive 的 [LoadingIndicator]（图形形变 + 旋转）。
 *
 * 页内、行内、下拉刷新都用它，避免同一屏出现两种不同的加载样式。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(modifier = modifier.size(size), color = color)
}

/**
 * 页内整屏加载态：容器 + 形变指示器 + 一行说明。
 *
 * 只在「页面上还没有任何内容、正在首次加载」时使用；有内容时的刷新交给下拉指示器，
 * 两者不会同时出现（见 [NuistPullToRefreshBox]）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingState(message: String = "正在加载…", modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ContainedLoadingIndicator(
                modifier = Modifier.size(84.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                indicatorColor = MaterialTheme.colorScheme.primary,
                containerShape = CircleShape,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 空白态的可滚动容器。
 *
 * 空白页本身没有内容可滚，下拉手势就没有来源；这里用一屏高的单个 item 撑起一个
 * [LazyColumn]，让下拉刷新在空白态同样可用（与「轻点任意处」是同一套重试入口）。
 */
@Composable
fun BlankStateArea(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(0.dp)) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = contentAlignment,
                content = content,
            )
        }
    }
}

/**
 * 空白态（加载失败 / 空结果）。
 *
 * 刻意不放按钮：标题栏以下任意位置轻点、或整页下拉，都触发同一个重试。
 * [needsCredential] 时额外给一个「去绑定通行密钥」的行内入口。
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    needsCredential: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onOpenPortal: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = onRetry != null,
                onClick = { onRetry?.invoke() },
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SymbolIcon(
                if (needsCredential) "fingerprint" else "error_outline",
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
                fontSize = 48.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                title ?: if (needsCredential) "需要登录统一门户" else "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (needsCredential && onOpenPortal != null) {
                Spacer(Modifier.height(10.dp))
                val linkInteraction = remember { MutableInteractionSource() }
                Text(
                    "去绑定通行密钥",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(
                            interactionSource = linkInteraction,
                            indication = null,
                            onClick = { onOpenPortal() },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** 空结果：没有数据，但算不上错误（例如「暂无成绩」）。同样支持轻点 / 下拉重试。 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    icon: String = "inbox",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = onRetry != null,
                onClick = { onRetry?.invoke() },
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SymbolIcon(
                icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
                fontSize = 48.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
/**
 * 行内错误提示：页面已有内容、但最近一次刷新失败时用。
 *
 * 不遮挡内容、不放按钮，整条可点即重试。
 */
@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    BaseItemContainer(modifier = modifier, onClick = onRetry) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SymbolIcon(
                "error_outline",
                contentDescription = null,
                tint = scheme.error,
                modifier = Modifier.size(20.dp),
                fontSize = 20.sp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "刷新失败",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
