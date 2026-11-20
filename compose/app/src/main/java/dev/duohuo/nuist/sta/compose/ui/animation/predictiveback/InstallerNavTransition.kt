// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.animation.predictiveback

import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackAnimation
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackExitDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

fun installerNavTransition(animation: PredictiveBackAnimation, exitDirection: PredictiveBackExitDirection): NavTransition = when (animation) {
    PredictiveBackAnimation.None -> NoPredictiveBackTransition
    PredictiveBackAnimation.MIUIX -> NavTransitions.MiuixDefault
    PredictiveBackAnimation.AOSP -> AospNavTransition
    PredictiveBackAnimation.Scale -> scaleNavTransition(exitDirection)
    PredictiveBackAnimation.Classic -> ClassicNavTransition
}
