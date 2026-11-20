// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DesignServices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.R
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackAnimation
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackExitDirection
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.ui.icons.AppIcons
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.DropDownMenuWidget

@Composable
fun ColorSpecSelector(viewModel: ThemeSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val availableSpecs = uiState.paletteStyle.supportedSpecs
    val activeSpec = uiState.paletteStyle.effectiveSpec(uiState.colorSpec)
    val descriptionText = if (availableSpecs.size == 1) {
        stringResource(
            id = when (activeSpec) {
                ThemeColorSpec.SPEC_2026 -> R.string.theme_settings_color_spec_only_2026
                else -> R.string.theme_settings_color_spec_only_2021
            },
        )
    } else {
        activeSpec.displayName
    }

    DropDownMenuWidget(
        imageVector = Icons.TwoTone.DesignServices,
        title = stringResource(id = R.string.theme_settings_color_spec),
        description = descriptionText,
        enabled = availableSpecs.size > 1,
        choice = availableSpecs.indexOf(activeSpec).coerceAtLeast(0),
        data = availableSpecs.map { it.displayName },
        onChoiceChange = { index ->
            val selectedSpec = availableSpecs[index]
            viewModel.dispatch(ThemeSettingsAction.SetColorSpec(selectedSpec))
        },
    )
}

@Composable
fun PredictiveBackAnimationWidget(uiState: ThemeSettingsState, onClick: () -> Unit) {
    BaseWidget(
        imageVector = AppIcons.PredictiveBack,
        title = stringResource(R.string.theme_settings_predictive_back_animation),
        description = when (uiState.predictiveBackAnimation) {
            PredictiveBackAnimation.None -> stringResource(R.string.theme_settings_predictive_back_animation_none)
            PredictiveBackAnimation.AOSP -> stringResource(R.string.theme_settings_predictive_back_animation_aosp)
            PredictiveBackAnimation.MIUIX -> stringResource(R.string.theme_settings_predictive_back_animation_miuix)
            PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)
            PredictiveBackAnimation.Classic -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_classic)
        },
        onClick = onClick,
    ) {}
}

@Composable
fun PredictiveBackAnimationDirectionWidget(uiState: ThemeSettingsState, onClick: () -> Unit) {
    BaseWidget(
        imageVector = AppIcons.PredictiveBackDirection,
        title = stringResource(R.string.theme_settings_predictive_back_exit_direction),
        description = when (uiState.predictiveBackExitDirection) {
            PredictiveBackExitDirection.FOLLOW_GESTURE -> stringResource(
                R.string.theme_settings_predictive_back_exit_direction_follow_gesture,
            )

            PredictiveBackExitDirection.ALWAYS_RIGHT -> stringResource(
                R.string.theme_settings_predictive_back_exit_direction_always_right,
            )

            PredictiveBackExitDirection.ALWAYS_LEFT -> stringResource(
                R.string.theme_settings_predictive_back_exit_direction_always_left,
            )
        },
        onClick = onClick,
    ) {}
}
