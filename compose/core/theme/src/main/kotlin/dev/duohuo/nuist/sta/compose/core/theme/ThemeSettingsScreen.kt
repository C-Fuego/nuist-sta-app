@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.duohuo.nuist.sta.compose.core.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.material.ColorSwatchPreview
import dev.duohuo.nuist.sta.compose.core.theme.material.PaletteStyle
import dev.duohuo.nuist.sta.compose.core.theme.material.PresetColors
import dev.duohuo.nuist.sta.compose.core.theme.material.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseItemContainer
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.ExpressiveBackButton
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn
import dev.duohuo.nuist.sta.compose.core.theme.settings.SwitchWidget

/**
 * 独立主题设置页——**结构逐段照搬参考实现ThemeSettingsPage**，仅把
 * ViewModel 换成回调、R.string 换成中文、AppIcons 换成 Material Icons、去掉 blur/backdrop。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsScreen(
    seedColor: androidx.compose.ui.graphics.Color,
    onSeedColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    colorSpec: ThemeColorSpec,
    onColorSpecChange: (ThemeColorSpec) -> Unit,
    darkModeIndex: Int, // 0 system 1 light 2 dark
    onDarkModeChange: (Int) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    useAmoled: Boolean,
    onUseAmoledChange: (Boolean) -> Unit,
    bottomBarFloating: Boolean,
    onBottomBarFloatingChange: (Boolean) -> Unit,
    pbAnim: PredictiveBackAnimation,
    onPbAnimChange: (PredictiveBackAnimation) -> Unit,
    pbDir: PredictiveBackExitDirection,
    onPbDirChange: (PredictiveBackExitDirection) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var showPaletteDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showPredictiveBackAnimationDialog by remember { mutableStateOf(false) }
    var showPredictiveBackExitDirectionDialog by remember { mutableStateOf(false) }
    var showSpecDialog by remember { mutableStateOf(false) }

    if (showPredictiveBackAnimationDialog) {
        PredictiveBackAnimationDialog(
            currentAnimation = pbAnim,
            onDismiss = { showPredictiveBackAnimationDialog = false },
            onSelect = { onPbAnimChange(it); showPredictiveBackAnimationDialog = false },
        )
    }
    if (showPredictiveBackExitDirectionDialog) {
        PredictiveBackExitDirectionDialog(
            currentDirection = pbDir,
            onDismiss = { showPredictiveBackExitDirectionDialog = false },
            onSelect = { onPbDirChange(it); showPredictiveBackExitDirectionDialog = false },
        )
    }
    if (showPaletteDialog) {
        PaletteStyleDialog(
            currentStyle = paletteStyle,
            onDismiss = { showPaletteDialog = false },
            onSelect = { onPaletteStyleChange(it); showPaletteDialog = false },
        )
    }
    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = darkModeIndex,
            onDismiss = { showThemeModeDialog = false },
            onSelect = { onDarkModeChange(it); showThemeModeDialog = false },
        )
    }
    if (showSpecDialog) {
        ColorSpecDialog(
            currentSpec = colorSpec,
            supportedSpecs = paletteStyle.supportedSpecs,
            onDismiss = { showSpecDialog = false },
            onSelect = { onColorSpecChange(it); showSpecDialog = false },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text("主题设置") },
                navigationIcon = {
                    Row {
                        ExpressiveBackButton { onBack() }
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            // --- Group 1: Google Material 3 ---
            item {
                SegmentedColumn(title = "Google Material 3") {
                    item {
                        BaseWidget(
                            icon = "dark_mode",
                            title = "主题模式",
                            description = listOf("跟随系统主题", "浅色", "深色")[darkModeIndex],
                            onClick = { showThemeModeDialog = true },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "tune",
                            title = "调色板样式",
                            description = paletteStyle.displayName,
                            onClick = { showPaletteDialog = true },
                        )
                    }
                    item {
                        // 照搬参考实现ColorSpecSelector：行内下拉菜单，非弹窗
                        val availableSpecs = paletteStyle.supportedSpecs
                        val activeSpec = paletteStyle.effectiveSpec(colorSpec)
                        dev.duohuo.nuist.sta.compose.core.theme.settings.DropDownMenuWidget(
                            icon = "contrast",
                            title = "颜色规格",
                            description = if (availableSpecs.size == 1) {
                                if (activeSpec == ThemeColorSpec.SPEC_2026) "仅 2026" else "仅 2021"
                            } else {
                                activeSpec.displayName
                            },
                            enabled = availableSpecs.size > 1,
                            choice = availableSpecs.indexOf(activeSpec).coerceAtLeast(0),
                            data = availableSpecs.map { it.displayName },
                            onChoiceChange = { index ->
                                onColorSpecChange(availableSpecs[index])
                            },
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = "invert_colors",
                            title = "动态颜色",
                            description = "基于壁纸的主题颜色",
                            checked = dynamicColor,
                            onCheckedChange = onDynamicColorChange,
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = "dark_mode",
                            title = "纯黑背景",
                            description = "深色主题下使用纯黑背景（AMOLED）",
                            checked = useAmoled,
                            onCheckedChange = onUseAmoledChange,
                        )
                    }
                    item {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = "悬浮底栏",
                            description = "紧凑布局下底部导航悬浮显示",
                            checked = bottomBarFloating,
                            onCheckedChange = onBottomBarFloatingChange,
                        )
                    }
                }
            }

            // --- Group 3: 主题配色（手动选色，非动态颜色时显示，带弹出动画）---
            item {
                // 照搬参考实现ThemeSettingsPage 的 Group 3 动画参数
                androidx.compose.animation.AnimatedVisibility(
                    visible = !dynamicColor,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 300,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                    ) + androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                    ),
                    exit = androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 250,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                    ) + androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 350,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                    ),
                ) {
                    SegmentedColumn(title = "主题配色") {
                        item {
                            BaseItemContainer {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                                ) {
                                    val itemMinWidth = 88.dp
                                    val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                                    val chunkedColors = PresetColors.chunked(columns)
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
                                                            currentStyle = paletteStyle,
                                                            colorSpec = colorSpec,
                                                            textStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                                            textColor = MaterialTheme.colorScheme.onSurface,
                                                            isSelected = seedColor == rawColor.color,
                                                        ) { onSeedColorChange(rawColor.color) }
                                                    }
                                                }
                                                val remaining = columns - rowItems.size
                                                if (remaining > 0) {
                                                    repeat(remaining) { Spacer(Modifier.weight(1f)) }
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

            // --- Group 4: 预测性返回（无 MiuiX 档）---
            // 原文 参考实现 用 SDK >= UPSIDE_DOWN_CAKE(34) 门控
            item {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SegmentedColumn(title = "预测性返回") {
                    item {
                        BaseWidget(
                            icon = "arrow_back",
                            title = "预测性返回动画",
                            description = pbAnim.label,
                            onClick = { showPredictiveBackAnimationDialog = true },
                        )
                    }
                    if (pbAnim != PredictiveBackAnimation.None) {
                        item {
                            BaseWidget(
                                icon = "arrow_back",
                                title = "退场方向",
                                description = pbDir.label,
                                onClick = { showPredictiveBackExitDirectionDialog = true },
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun PaletteStyleDialog(currentStyle: PaletteStyle, onDismiss: () -> Unit, onSelect: (PaletteStyle) -> Unit) {
    // 照搬参考实现ThemeSettingsPage.PaletteStyleDialog：直接遍历 entries，
    // radio + 名称，行距 8dp，内容整列 verticalScroll，底部关闭按钮
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调色板样式") },
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
                Text("关闭")
            }
        },
    )
}

@Composable
fun ColorSpecDialog(
    currentSpec: ThemeColorSpec,
    supportedSpecs: List<ThemeColorSpec>,
    onDismiss: () -> Unit,
    onSelect: (ThemeColorSpec) -> Unit,
) {
    SingleChoiceDialog(
        title = "颜色规格",
        items = supportedSpecs.map { it.displayName },
        selectedIndex = supportedSpecs.indexOf(currentSpec).coerceAtLeast(0),
        onDismiss = onDismiss,
        onSelect = { onSelect(supportedSpecs[it]) },
    )
}

@Composable
fun PredictiveBackAnimationDialog(
    currentAnimation: PredictiveBackAnimation,
    onDismiss: () -> Unit,
    onSelect: (PredictiveBackAnimation) -> Unit,
) {
    // 逐字移植 参考实现 PredictiveBackAnimationDialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("预测性返回动画") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PredictiveBackAnimation.entries.forEach { animation ->
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
                        Text(animation.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
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
    // 逐字移植 参考实现 PredictiveBackExitDirectionDialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("退场方向") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PredictiveBackExitDirection.entries.forEach { direction ->
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
                        Text(direction.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun ThemeModeDialog(currentMode: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    // 逐字移植 参考实现 ThemeModeDialog
    val modes = listOf("跟随系统主题", "浅色", "深色")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题模式") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                modes.forEachIndexed { index, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (index == currentMode),
                            onClick = { onSelect(index) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                items.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(index) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = { onSelect(index) })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
