// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 骨架屏用的扫光块。
 *
 * 首次加载时用骨架占位代替指示器：此时页面上没有任何内容，骨架本身就是「正在加载」的表达，
 * 因此同一屏只会出现骨架，不会再叠一个页内加载动画。
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, delayMillis = 200)),
        label = "shimmerTranslate",
    )
    Box(
        modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset.Zero,
                    end = Offset(translate.value, translate.value),
                ),
                shape = shape,
            ),
    )
}

/** 列表类页面的首次加载骨架（公告、成绩等行式列表）。 */
@Composable
fun ListSkeleton(
    rows: Int = 6,
    modifier: Modifier = Modifier,
    showLeading: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(rows) { index ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showLeading) {
                    ShimmerBox(Modifier.size(44.dp), RoundedCornerShape(12.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShimmerBox(
                        Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.72f else 0.58f)
                            .height(14.dp),
                        RoundedCornerShape(7.dp),
                    )
                    ShimmerBox(Modifier.fillMaxWidth(0.38f).height(11.dp), RoundedCornerShape(6.dp))
                }
            }
        }
    }
}

/**
 * 课表页面的首次加载骨架：按真实网格排布（周次卡 + 节次栏 + 星期头 + 若干课程块）。
 *
 * 动画是整体呼吸（透明度在 0.45–0.85 之间来回），不是扫光带；块的位置照着周视图的
 * 节次高度与列宽走，所以加载结束切到真实课表时不会有明显的布局跳动。
 */
@Composable
fun TimetableSkeleton(
    modifier: Modifier = Modifier,
    columns: Int = 7,
    rows: Int = 9,
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "timetable-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timetablePulse",
    )
    val cell = 56.dp
    val header = 44.dp
    val blockShape = RoundedCornerShape(8.dp)
    // 占位课程：(列, 起始节, 跨几节)，照着常见课表的疏密排，避免一格一格的均匀方块。
    val placeholderBlocks = listOf(
        Triple(0, 0, 2), Triple(1, 1, 2), Triple(2, 3, 2),
        Triple(3, 0, 2), Triple(4, 2, 3), Triple(5, 4, 2),
        Triple(minOf(6, columns - 1), 1, 2),
    )
    Column(modifier = modifier.fillMaxSize().padding(top = 12.dp)) {
        SkeletonPulse(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            alpha = pulse,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(52.dp)) {
                Spacer(Modifier.height(header))
                repeat(rows) {
                    SkeletonPulse(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            .fillMaxWidth().height(cell - 16.dp),
                        shape = RoundedCornerShape(6.dp),
                        alpha = pulse * 0.7f,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth().height(header)) {
                    repeat(columns) {
                        SkeletonPulse(
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 14.dp)
                                .fillMaxSize(),
                            shape = RoundedCornerShape(6.dp),
                            alpha = pulse * 0.55f,
                        )
                    }
                }
                androidx.compose.foundation.layout.BoxWithConstraints(
                    Modifier.fillMaxWidth().height(cell * rows),
                ) {
                    Column {
                        repeat(rows) {
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(Modifier.height(cell - 1.dp))
                        }
                    }
                    val colW = maxWidth / columns
                    placeholderBlocks.forEach { (column, start, span) ->
                        if (column < columns && start + span <= rows) {
                            SkeletonPulse(
                                modifier = Modifier
                                    .offset(x = colW * column + 2.dp, y = cell * start + 2.dp)
                                    .width(colW - 4.dp)
                                    .height(cell * span - 4.dp),
                                shape = blockShape,
                                alpha = pulse,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 骨架占位块：只做透明度呼吸，不扫光。 */
@Composable
private fun SkeletonPulse(
    modifier: Modifier,
    shape: Shape,
    alpha: Float,
) {
    Box(
        modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha),
            shape = shape,
        ),
    )
}
