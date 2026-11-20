// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package dev.duohuo.nuist.sta.compose.ui.page.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.twotone.Colorize
import androidx.compose.material.icons.twotone.Contrast
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material.icons.twotone.SmartButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.R
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackAnimation
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackExitDirection
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.PaletteStyle
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.theme.ThemeMode
import dev.duohuo.nuist.sta.compose.ui.icons.AppIcons
import dev.duohuo.nuist.sta.compose.ui.navigation.LocalNavigator
import dev.duohuo.nuist.sta.compose.ui.kit.ColorSwatchPreview
import dev.duohuo.nuist.sta.compose.ui.kit.BaseItemContainer
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.ExpressiveBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.RadioButtonWidget
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SwitchWidget
import dev.duohuo.nuist.sta.compose.ui.theme.getMaterial3AppBarColor
import dev.duohuo.nuist.sta.compose.ui.theme.installerMaterial3BlurEffect
import dev.duohuo.nuist.sta.compose.ui.theme.material.PresetColors
import dev.duohuo.nuist.sta.compose.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsPage(viewModel: ThemeSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState(-154f, -154f) // from debugger
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    var showHideLauncherIconDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showPredictiveBackAnimationDialog by remember { mutableStateOf(false) }
    var showPredictiveBackExitDirectionDialog by remember { mutableStateOf(false) }

    if (showPredictiveBackAnimationDialog) {
        PredictiveBackAnimationDialog(
            currentAnimation = uiState.predictiveBackAnimation,
            onDismiss = { showPredictiveBackAnimationDialog = false },
            onSelect = { animation ->
                viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackAnimation(animation))
                showPredictiveBackAnimationDialog = false
            },
        )
    }

    if (showPredictiveBackExitDirectionDialog) {
        PredictiveBackExitDirectionDialog(
            currentDirection = uiState.predictiveBackExitDirection,
            onDismiss = { showPredictiveBackExitDirectionDialog = false },
            onSelect = { direction ->
                viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackExitDirection(direction))
                showPredictiveBackExitDirectionDialog = false
            },
        )
    }

    if (showPaletteDialog) {
        PaletteStyleDialog(
            currentStyle = uiState.paletteStyle,
            onDismiss = { showPaletteDialog = false },
            onSelect = { style ->
                viewModel.dispatch(ThemeSettingsAction.SetPaletteStyle(style))
                showPaletteDialog = false
            },
        )
    }

    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = uiState.themeMode,
            onDismiss = { showThemeModeDialog = false },
            onSelect = { mode ->
                viewModel.dispatch(ThemeSettingsAction.SetThemeMode(mode))
                showThemeModeDialog = false
            },
        )
    }

    val backdrop = rememberMaterial3BlurBackdrop(uiState.useBlur)

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.theme_settings))
                },
                navigationIcon = {
                    Row {
                        ExpressiveBackButton { navigator.pop() }
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor(),
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = paddingValues,
        ) {
            // --- Group 2: Display options ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.theme_settings_display),
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            SwitchWidget(
                                imageVector = AppIcons.Blur,
                                title = stringResource(R.string.theme_settings_use_blur),
                                description = stringResource(R.string.theme_settings_use_blur_desc),
                                checked = uiState.useBlur,
                                onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseBlur(it)) },
                            )
                        }
                    }
                    item {
                        SwitchWidget(
                            imageVector = Icons.TwoTone.SmartButton,
                            title = stringResource(R.string.theme_settings_use_apple_floating_bar),
                            description = stringResource(R.string.theme_settings_use_apple_floating_bar_desc),
                            checked = uiState.useAppleFloatingBar,
                            onCheckedChange = {
                                viewModel.dispatch(ThemeSettingsAction.SetUseAppleFloatingBar(it))
                            },
                        )
                    }
                    item {
                        BaseWidget(
                            imageVector = Icons.Default.DarkMode,
                            title = stringResource(R.string.theme_settings_theme_mode),
                            description = when (uiState.themeMode) {
                                ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                            },
                            onClick = { showThemeModeDialog = true },
                        )
                    }
                    item {
                        SwitchWidget(
                            imageVector = Icons.TwoTone.Contrast,
                            title = stringResource(R.string.theme_settings_use_amoled),
                            description = stringResource(R.string.theme_settings_use_amoled_desc),
                            checked = uiState.useAmoled,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseAmoled(it)) },
                        )
                    }
                    item {
                        BaseWidget(
                            imageVector = AppIcons.Palette,
                            title = stringResource(R.string.theme_settings_palette_style),
                            description = uiState.paletteStyle.displayName,
                            onClick = { showPaletteDialog = true },
                        )
                    }
                    item { ColorSpecSelector(viewModel) }
                    item {
                        SwitchWidget(
                            imageVector = Icons.TwoTone.InvertColors,
                            title = stringResource(R.string.theme_settings_dynamic_color),
                            description = stringResource(R.string.theme_settings_dynamic_color_desc),
                            checked = uiState.useDynamicColor,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseDynamicColor(it)) },
                        )
                    }
                }
            }

            // --- Group 3: Theme Color (Manual Selection) ---
            item {
                AnimatedVisibility(
                    visible = !uiState.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                        shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)),
                ) {
                    SegmentedColumn(
                        title = stringResource(R.string.theme_settings_theme_color),
                    ) {
                        item {
                            BaseItemContainer {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp),
                                ) {
                                    val itemMinWidth = 88.dp
                                    val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                                    val chunkedColors = uiState.availableColors.chunked(columns)

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        chunkedColors.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                            ) {
                                                rowItems.forEach { rawColor ->
                                                    Box(
                                                        modifier = Modifier.weight(1f),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        ColorSwatchPreview(
                                                            rawColor = rawColor,
                                                            currentStyle = uiState.paletteStyle,
                                                            colorSpec = uiState.colorSpec,
                                                            textStyle = MaterialTheme.typography.labelMedium.copy(
                                                                fontSize = 13.sp,
                                                            ),
                                                            textColor = MaterialTheme.colorScheme.onSurface,
                                                            isSelected =
                                                                uiState.seedColor == rawColor.color,
                                                        ) {
                                                            viewModel.dispatch(
                                                                ThemeSettingsAction.SetSeedColor(
                                                                    rawColor.color,
                                                                    uiState.useDynamicColor &&
                                                                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                                                                        // If useDynamicColor is true but availableColors is PresetColors, the wallpaperColors have no valid values
                                                                        uiState.availableColors != PresetColors,
                                                                ),
                                                            )
                                                        }
                                                    }
                                                }

                                                val remaining = columns - rowItems.size
                                                if (remaining > 0) {
                                                    repeat(remaining) {
                                                        Spacer(Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Group 4: Predictive Back ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.theme_settings_predictive_back),
                    ) {
                        item { PredictiveBackAnimationWidget(uiState) { showPredictiveBackAnimationDialog = true } }
                        item(
                            animatedVisibility =
                                uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale,
                        ) {
                            PredictiveBackAnimationDirectionWidget(uiState) { showPredictiveBackExitDirectionDialog = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaletteStyleDialog(currentStyle: PaletteStyle, onDismiss: () -> Unit, onSelect: (PaletteStyle) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_palette_style_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PaletteStyle.entries.forEach { style ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(style) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (style == currentStyle),
                            onClick = { onSelect(style) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(style.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
fun PredictiveBackAnimationDialog(
    currentAnimation: PredictiveBackAnimation,
    onDismiss: () -> Unit,
    onSelect: (PredictiveBackAnimation) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_predictive_back_animation_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PredictiveBackAnimation.entries.forEach { animation ->
                    val animationText = when (animation) {
                        PredictiveBackAnimation.None -> stringResource(R.string.theme_settings_predictive_back_animation_none)

                        PredictiveBackAnimation.AOSP -> stringResource(R.string.theme_settings_predictive_back_animation_aosp)

                        PredictiveBackAnimation.MIUIX -> stringResource(R.string.theme_settings_predictive_back_animation_miuix)

                        PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)

                        PredictiveBackAnimation.Classic -> stringResource(
                            R.string.theme_settings_predictive_back_animation_ksu_classic,
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(animation) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (animation == currentAnimation),
                            onClick = { onSelect(animation) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(animationText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
fun PredictiveBackExitDirectionDialog(
    currentDirection: PredictiveBackExitDirection,
    onDismiss: () -> Unit,
    onSelect: (PredictiveBackExitDirection) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_predictive_back_exit_direction_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PredictiveBackExitDirection.entries.forEach { direction ->
                    val directionText = when (direction) {
                        PredictiveBackExitDirection.FOLLOW_GESTURE -> stringResource(
                            R.string.theme_settings_predictive_back_exit_direction_follow_gesture,
                        )

                        PredictiveBackExitDirection.ALWAYS_RIGHT -> stringResource(
                            R.string.theme_settings_predictive_back_exit_direction_always_right,
                        )

                        PredictiveBackExitDirection.ALWAYS_LEFT -> stringResource(
                            R.string.theme_settings_predictive_back_exit_direction_always_left,
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(direction) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (direction == currentDirection),
                            onClick = { onSelect(direction) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(directionText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
fun ThemeModeDialog(currentMode: ThemeMode, onDismiss: () -> Unit, onSelect: (ThemeMode) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_theme_mode_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ThemeMode.entries.forEach { mode ->
                    val modeText = when (mode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = { onSelect(mode) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(modeText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
