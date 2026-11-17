// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
//
// 从 InstallerX ui/animation/predictiveback 移植的**纯数学**部分：
// NavTransitionEasing.kt / NavTransitionGeometry.kt / AospNavTransition.kt /
// ScaleNavTransition.kt 的曲线、常量与像素对齐函数。
// 原实现依赖 Miuix 的 NavTransition/NavGesture 类型，这里剥离为纯函数，
// 由本仓库自己的 PredictiveBackPage 驱动。
package dev.duohuo.nuist.sta.compose.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ==================== 缓动（InstallerX NavTransitionEasing 原文） ====================

/** FastOutExtraSlowIn：两段三次贝塞尔拼接（原文逐字）。 */
internal val FastOutExtraSlowIn: Easing = run {
    val knotX = 0.166666f
    val knotY = 0.4f
    val first = CubicBezierEasing(0.05f / knotX, 0f, 0.133333f / knotX, 0.06f / knotY)
    val second = CubicBezierEasing(
        (0.208333f - knotX) / (1f - knotX),
        (0.82f - knotY) / (1f - knotY),
        (0.25f - knotX) / (1f - knotX),
        (1f - knotY) / (1f - knotY),
    )
    Easing { fraction ->
        if (fraction < knotX) {
            knotY * first.transform(fraction / knotX)
        } else {
            knotY + (1f - knotY) * second.transform((fraction - knotX) / (1f - knotX))
        }
    }
}

/** 手势进度整形曲线（原文）。 */
internal val BackGestureEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

// ==================== 像素对齐（InstallerX NavTransitionGeometry 原文） ====================

internal fun snapScaleToPixelExtent(scale: Float, extent: Float): Float =
    if (extent > 0f) (scale * extent).roundToInt() / extent else scale

internal fun snapTranslationToPixelEdge(
    translation: Float,
    scale: Float,
    extent: Float,
    pivotFraction: Float = 0.5f,
): Float {
    if (extent <= 0f) return translation
    val scaledEdgeOffset = extent * pivotFraction * (1f - scale)
    return (translation + scaledEdgeOffset).roundToInt() - scaledEdgeOffset
}

// ==================== AOSP Cross-Activity 预测返回（常量原文） ====================

/** 原文 CROSS_ACTIVITY_MIN_SCALE。 */
internal const val AospMinScale = 0.9f
internal val AospDrift = 96.dp
internal val AospEdgeMargin = 8.dp

/** 原文 ClassicActivityMotion / CrossActivityPredictive 的提交动效。 */
internal val AospCommitSpec = tween<Float>(durationMillis = 450, easing = FastOutExtraSlowIn)
internal val AospCancelSpec = spring<Float>(stiffness = 1500f)

// ==================== Scale 预测返回（常量原文） ====================

internal const val ScaleMinScale = 0.85f
internal val ScaleCommitSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)
internal val ScaleCancelSpec = spring<Float>(stiffness = 1500f)

/** 页面层变换结果（由 PredictiveBackPage 直接喂给 graphicsLayer）。 */
internal data class BackTransform(
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val alpha: Float,
    val pivotFractionX: Float = 0.5f,
    val pivotFractionY: Float = 0.5f,
)

/**
 * AOSP（Cross-Activity）档变换（公式取原文 CrossActivityPredictive）：
 * - 手势中：scale 0.9→1（BackGestureEasing 整形）、向滑动侧贴边、alpha 保持 1
 * - 提交中：scale 由 committedScale 补到 1 并叠加 post*drift 位移，
 *   alpha 按 1 - 5*提交进度 衰减（原文 settle.elapsedMillis/450 的等价写法）
 */
internal fun aospBackTransform(
    progress: Float,
    releasing: Boolean,
    releaseProgress: Float,
    widthPx: Float,
    driftPx: Float,
    edgeMarginPx: Float,
    sign: Float,
): BackTransform {
    val p = progress.coerceIn(0f, 1f)
    val hugMax = ((widthPx * (1f - AospMinScale) / 2f) - edgeMarginPx).coerceAtLeast(0f)
    return if (releasing) {
        val span = (1f - releaseProgress).coerceAtLeast(0.01f)
        val post = ((p - releaseProgress) / span).coerceIn(0f, 1f)
        val releaseEased = 1f - BackGestureEasing.transform(releaseProgress)
        val committedScale = AospMinScale + (1f - AospMinScale) * releaseEased
        val grown = committedScale + (1f - committedScale) * post
        val scale = snapScaleToPixelExtent(grown, widthPx)
        val tx = (1f - releaseEased) * hugMax * sign + post * driftPx
        BackTransform(
            scale = scale,
            translationX = snapTranslationToPixelEdge(tx, scale, widthPx),
            translationY = 0f,
            alpha = (1f - post).coerceIn(0f, 1f),
        )
    } else {
        // 原文 shapedTopProgress 的等价映射：静止(p=0)=1，拖到底(p=1)=MinScale
        val eased = 1f - BackGestureEasing.transform(p)
        val scale = snapScaleToPixelExtent(AospMinScale + (1f - AospMinScale) * eased, widthPx)
        BackTransform(
            scale = scale,
            translationX = snapTranslationToPixelEdge((1f - eased) * hugMax * sign, scale, widthPx),
            translationY = 0f,
            alpha = 1f,
        )
    }
}

/** Scale 档变换（公式取原文 scaleNavTransition）：缩到 0.85、以滑动侧为轴心，提交时整页滑出。 */
internal fun scaleBackTransform(
    progress: Float,
    releasing: Boolean,
    releaseProgress: Float,
    widthPx: Float,
    sign: Float,
    fromLeftEdge: Boolean,
): BackTransform {
    val p = progress.coerceIn(0f, 1f)
    val pivotX = if (fromLeftEdge) 0.8f else 0.2f
    return if (releasing) {
        val span = (1f - releaseProgress).coerceAtLeast(0.01f)
        val post = ((p - releaseProgress) / span).coerceIn(0f, 1f)
        val releaseEased = 1f - BackGestureEasing.transform(releaseProgress)
        val committedScale = ScaleMinScale + (1f - ScaleMinScale) * releaseEased
        val pageScale = committedScale + (ScaleMinScale - committedScale) * post
        val scale = snapScaleToPixelExtent(pageScale, widthPx)
        BackTransform(
            scale = scale,
            translationX = snapTranslationToPixelEdge(sign * post * widthPx, scale, widthPx, pivotX),
            translationY = 0f,
            alpha = 1f,
            pivotFractionX = pivotX,
        )
    } else {
        val eased = 1f - BackGestureEasing.transform(p)
        val scale = snapScaleToPixelExtent(ScaleMinScale + (1f - ScaleMinScale) * eased, widthPx)
        BackTransform(
            scale = scale,
            translationX = 0f,
            translationY = 0f,
            alpha = 1f,
            pivotFractionX = pivotX,
        )
    }
}

/** Classic 档变换（原文 ClassicActivityClose）：96dp 漂移 + 淡出。 */
internal fun classicBackTransform(
    progress: Float,
    widthPx: Float,
    driftPx: Float,
    sign: Float,
): BackTransform {
    val p = progress.coerceIn(0f, 1f)
    return BackTransform(
        scale = 1f,
        translationX = (1f - p) * driftPx * sign,
        translationY = 0f,
        alpha = (1f - p).coerceIn(0f, 1f),
    )
}
