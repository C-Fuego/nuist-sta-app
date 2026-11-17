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
    // 手势起始边：左缘=True（BackEvent.swipeEdge == 0）
    var swipeFromLeft by remember { androidx.compose.runtime.mutableStateOf(true) }
    // 提交阶段状态（原文 settle.phase == Commit 的等价物）
    var releasing by remember { androidx.compose.runtime.mutableStateOf(false) }
    var releaseProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    if (animation != PredictiveBackAnimation.None) {
        PredictiveBackHandler(enabled = true) { events ->
            try {
                events.collect { backEvent ->
                    // 记录滑动侧（BackEvent.swipeEdge: 0=左缘, 1=右缘）
                    swipeFromLeft = backEvent.swipeEdge == 0
                    releaseProgress = backEvent.progress.coerceIn(0f, 1f)
                    progress.snapTo(releaseProgress)
                }
                releasing = true
                // 手势完成：按原文提交动效补到 1 再返回
                progress.animateTo(
                    1f,
                    when (animation) {
                        PredictiveBackAnimation.Scale -> ScaleCommitSpec
                        else -> AospCommitSpec
                    },
                )
                onBack()
            } catch (e: kotlinx.coroutines.CancellationException) {
                releasing = false
                // 手势取消：按原文回弹弹簧
                progress.animateTo(
                    0f,
                    when (animation) {
                        PredictiveBackAnimation.Scale -> ScaleCancelSpec
                        else -> AospCancelSpec
                    },
                )
            }
        }
    } else {
        // 无预测动画：仍支持标准返回
        androidx.activity.compose.BackHandler(enabled = true) { onBack() }
    }

    val p = progress.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val w = size.width
                val h = size.height
                val sign = when (direction) {
                    PredictiveBackExitDirection.FOLLOW_GESTURE ->
                        if (swipeFromLeft) 1f else -1f
                    PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
                    PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
                }
                val t = when (animation) {
                    PredictiveBackAnimation.AOSP -> aospBackTransform(
                        progress = p,
                        releasing = releasing,
                        releaseProgress = releaseProgress,
                        widthPx = w,
                        driftPx = AospDrift.toPx(),
                        edgeMarginPx = AospEdgeMargin.toPx(),
                        sign = sign,
                    )
                    PredictiveBackAnimation.Scale -> scaleBackTransform(
                        progress = p,
                        releasing = releasing,
                        releaseProgress = releaseProgress,
                        widthPx = w,
                        sign = sign,
                        fromLeftEdge = swipeFromLeft,
                    )
                    PredictiveBackAnimation.Classic -> classicBackTransform(
                        progress = p,
                        releasing = releasing,
                        driftPx = AospDrift.toPx(),
                        sign = sign,
                    )
                    PredictiveBackAnimation.None -> BackTransform(1f, 0f, 0f, 1f)
                }
                scaleX = t.scale
                scaleY = t.scale
                translationX = t.translationX
                translationY = t.translationY
                alpha = t.alpha
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    pivotFractionX = t.pivotFractionX,
                    pivotFractionY = t.pivotFractionY,
                )
            }
            // 退场时轻微加圆角，更接近 AOSP 的窗口退场观感
            .drawWithContent {
                drawContent()
            },
    ) {
        content()
    }
}
