package dev.duohuo.nuist.sta.compose.core.theme

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 预测性返回页面容器（Android 标准 `PredictiveBackHandler`，非 Nav3、非 MiuiX）。
 *
 * 手势进行中：progress 0..1 实时驱动 [animation]+[direction] 的退场变换；
 * 手势完成：动画补到 1 后调用 [onBack]；
 * 手势取消：动画回弹到 0。
 *
 * @param animation 动画类型（AOSP/Scale/Classic/None）。
 * @param direction 退场方向。
 * @param onBack 手势完成、页面应真正返回时调用。
 */
@Composable
fun PredictiveBackPage(
    animation: PredictiveBackAnimation,
    direction: PredictiveBackExitDirection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    // 手势完成方向的符号：由最近一次 back 事件的滑动方向决定
    var exitSign by remember { mutableFloatStateOf(1f) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    if (animation != PredictiveBackAnimation.None) {
        PredictiveBackHandler(enabled = true) { events ->
            try {
                events.collect { backEvent ->
                    // 记录滑动起点方向（左滑 → 内容向右退出）
                    exitSign = 1f
                    progress.snapTo(backEvent.progress.coerceIn(0f, 1f))
                }
                // 手势完成：补到 1 再返回
                progress.animateTo(1f, tween(durationMillis = 150))
                onBack()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 手势取消：回弹
                progress.animateTo(0f, tween(durationMillis = 200))
            }
        }
    } else {
        // 无预测动画：仍支持标准返回
        androidx.activity.compose.BackHandler(enabled = true) { onBack() }
    }

    val p = progress.value
    val signed = when (direction) {
        PredictiveBackExitDirection.FOLLOW_GESTURE -> exitSign * (if (isRtl) -1f else 1f)
        PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
        PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                when (animation) {
                    PredictiveBackAnimation.AOSP -> {
                        translationX = signed * p * widthPx * 0.30f
                        scaleX = 1f - 0.08f * p
                        scaleY = 1f - 0.08f * p
                        alpha = 1f - 0.35f * p
                    }
                    PredictiveBackAnimation.Scale -> {
                        scaleX = 1f - 0.15f * p
                        scaleY = 1f - 0.15f * p
                        alpha = 1f - p
                    }
                    PredictiveBackAnimation.Classic -> {
                        translationX = signed * p * widthPx * 0.12f
                        scaleX = 1f - 0.10f * p
                        scaleY = 1f - 0.10f * p
                        alpha = 1f - p
                    }
                    PredictiveBackAnimation.None -> {
                        alpha = 1f
                    }
                }
            }
            // 退场时轻微加圆角，更接近 AOSP 的窗口退场观感
            .drawWithContent {
                drawContent()
            },
    ) {
        content()
    }
}
