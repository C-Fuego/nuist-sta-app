// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.kit

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** 下拉触发的刷新如果迟迟没有进入加载态，最多让指示器驻留这么久，避免卡住不动。 */
private const val MaxHoldMillis = 8_000L

/**
 * 统一的下拉刷新容器。
 *
 * 逻辑与参考实现一致：**只有手势下拉才会点亮顶部指示器**。
 *
 * - 下拉 → 松手 → 指示器驻留在顶部不动（不回弹、不收起），直到这次刷新真的结束才收回；
 * - 点击 / 页面自动刷新走页内 [LoadingState]（骨架屏或居中指示器），顶部指示器全程不出现；
 * - 首屏加载（页面还没有内容）同样只走页内加载态。
 *
 * 指示器直接用 Material 3 Expressive 的原件 [PullToRefreshDefaults.LoadingIndicator]：
 * 48dp 容器内的形变指示器，下拉过程中由拖动距离驱动形变、超过阈值后整体旋转，驻留时是不定态。
 *
 * 页内加载态通过 [LocalRefreshFromPull] 拿到「这次刷新是下拉触发的」标记并让位，
 * 因此两种加载表现互斥，不会同屏叠加。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NuistPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    // 这次刷新是不是下拉触发的。松手瞬间就置位（不等数据层回报 isLoading），
    // 否则指示器会先被收回、再被点亮，看起来「回弹一次」。
    var pulled by remember { mutableStateOf(false) }
    // 是否真正观察到过一轮加载：加载结束后才允许收起指示器。
    var sawLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            sawLoading = true
        } else if (sawLoading) {
            sawLoading = false
            pulled = false
        }
    }
    LaunchedEffect(pulled) {
        if (pulled) {
            delay(MaxHoldMillis)
            pulled = false
        }
    }

    PullToRefreshBox(
        isRefreshing = pulled,
        onRefresh = {
            pulled = true
            onRefresh()
        },
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = pulled,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        CompositionLocalProvider(LocalRefreshFromPull provides pulled) {
            content()
        }
    }
}

/**
 * 当前这次刷新是否由下拉手势触发。
 *
 * 为 true 时页面不要再渲染 [LoadingState] / 骨架屏：顶部下拉指示器就是这次刷新的唯一加载表现。
 */
val LocalRefreshFromPull = compositionLocalOf { false }