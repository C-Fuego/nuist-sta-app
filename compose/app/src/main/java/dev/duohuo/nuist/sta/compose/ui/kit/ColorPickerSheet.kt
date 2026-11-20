// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 颜色选择面板（`ModalBottomSheet`）。
 *
 * 轨道沿用 MD3E 滑杆的造型：一条圆角长条被一根竖立的圆角方块「切断」，
 * 拖动时方块随手指移动、轨道在方块两侧留出缺口；色相 / 饱和度 / 明度三条轨道各自画渐变。
 * 底部给一排当前主题色板，避免和全应用配色打架。输出为 ARGB（不透明）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    title: String,
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val hsv = remember(initialColorArgb) { argbToHsv(initialColorArgb) }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    val preview = Color.hsv(hue, saturation, value, 1f)
    val pureHue = Color.hsv(hue.coerceAtLeast(0.01f), 1f, 1f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerHigh,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(preview)
                        .border(1.dp, scheme.outlineVariant, CircleShape),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = scheme.onSurface)
                    Text(
                        argbToHex(preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            ColorSlider(
                label = "色相",
                display = "${hue.toInt()}°",
                value = hue,
                range = 0f..359.9f,
                onValueChange = { hue = it },
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                        Color.Blue, Color.Magenta, Color.Red,
                    ),
                ),
                gapColor = scheme.surfaceContainerHigh,
            )
            ColorSlider(
                label = "饱和度",
                display = "${(saturation * 100).toInt()}%",
                value = saturation,
                range = 0f..1f,
                onValueChange = { saturation = it },
                brush = Brush.horizontalGradient(listOf(Color.White, pureHue)),
                gapColor = scheme.surfaceContainerHigh,
            )
            ColorSlider(
                label = "明度",
                display = "${(value * 100).toInt()}%",
                value = value,
                range = 0f..1f,
                onValueChange = { value = it },
                brush = Brush.horizontalGradient(listOf(Color.Black, pureHue)),
                gapColor = scheme.surfaceContainerHigh,
            )
            Spacer(Modifier.height(12.dp))
            Text("色板", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            // 预设取自当前主题色板，不用彩虹色，保证和全应用配色一致。
            val presets = listOf(
                scheme.primary,
                scheme.secondary,
                scheme.tertiary,
                scheme.error,
                scheme.primaryContainer,
                scheme.secondaryContainer,
                scheme.tertiaryContainer,
                scheme.outline,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { preset ->
                            val selected = preset.toArgb() == preview.toArgb()
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(preset)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) scheme.onSurface else scheme.outlineVariant,
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        val h = FloatArray(3)
                                        android.graphics.Color.colorToHSV(preset.toArgb(), h)
                                        hue = h[0]
                                        saturation = h[1]
                                        value = h[2]
                                    },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { onConfirm(Color.hsv(hue, saturation, value, 1f).toArgb()) }) {
                    Text("确定")
                }
            }
        }
    }
}

/**
 * MD3E 式滑杆：轨道是圆角长条，拇指是一根竖立的圆角方块，方块所在位置把轨道「切断」。
 */
@Composable
private fun ColorSlider(
    label: String,
    display: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    brush: Brush,
    gapColor: Color,
) {
    val scheme = MaterialTheme.colorScheme
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(display, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pointerInput(range) {
                    // 轨道两端各留出半个拇指宽度，保证拖到端点时拇指不会被裁掉。
                    val thumbW = 6.dp.toPx()
                    fun emit(x: Float) {
                        val usable = (size.width - thumbW).coerceAtLeast(1f)
                        val fraction = ((x - thumbW / 2f) / usable).coerceIn(0f, 1f)
                        onValueChange(range.start + fraction * span)
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        emit(down.position.x)
                        drag(down.id) { change ->
                            emit(change.position.x)
                            change.consume()
                        }
                    }
                },
        ) {
            val trackH = 24.dp.toPx()
            val thumbW = 8.dp.toPx()
            val thumbH = 40.dp.toPx()
            val gap = 6.dp.toPx()
            val usable = size.width - thumbW
            val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
            val cx = thumbW / 2f + usable * fraction
            val cy = size.height / 2f

            // 轨道（圆角长条 + 渐变）
            clipRect {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(size.width, trackH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f),
                )
            }
            // 拇指两侧留缺口：用面板底色盖掉轨道
            drawRect(
                color = gapColor,
                topLeft = Offset(cx - thumbW / 2f - gap, cy - trackH / 2f - 1f),
                size = Size(thumbW + gap * 2f, trackH + 2f),
            )
            // 拇指：竖立的圆角方块
            drawRoundRect(
                color = scheme.onSurface,
                topLeft = Offset(cx - thumbW / 2f, cy - thumbH / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumbW / 2f),
            )
        }
    }
}

private fun argbToHsv(colorArgb: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(colorArgb, hsv)
    return hsv
}

private fun argbToHex(color: Color): String {
    val argb = color.toArgb()
    return "#%02X%02X%02X".format(
        android.graphics.Color.red(argb),
        android.graphics.Color.green(argb),
        android.graphics.Color.blue(argb),
    )
}
