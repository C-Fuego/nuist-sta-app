// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import dev.duohuo.nuist.sta.compose.data.timetable.CourseFormInput
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableCellDetail
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog

/** 课表里的课程色板：与周视图按序号取色的预设保持一致，便于用户挑到「自动」之外的颜色。 */
internal val coursePresetColors = listOf(
    0xFF3B5BDB.toInt(), 0xFF1971C2.toInt(), 0xFF0C8599.toInt(), 0xFF2F9E44.toInt(),
    0xFF66A80F.toInt(), 0xFFE67700.toInt(), 0xFFD9480F.toInt(), 0xFFC2255C.toInt(),
    0xFF9C36B5.toInt(), 0xFF6741D9.toInt(), 0xFF495057.toInt(), 0xFF862E9C.toInt(),
)

/** 三个作用范围：只有这次课 / 这周及以后 / 所有周。 */
internal enum class EditScope(val key: String, val label: String) {
    ThisWeek("thisWeek", "仅这一次课"),
    AllWeeks("all", "这门课的全部安排"),
}

private val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 点开格子后的课程详情：字段、已做的本地改动、以及可执行的操作。
 *
 * 数据由 Dart 控制器给出（`timetable.cell`），这里的按钮只是把用户的意图转成
 * 一次桥调用——真正落盘的判断（能不能改、改成什么）都在 Dart 侧。
 */
/** 点开格子后的课程详情：字段、已做的本地改动、以及可执行的操作。
 *
 * 数据由 Dart 控制器给出（`timetable.cell`），这里的按钮只是把用户的意图转成
 * 一次桥调用——真正落盘的判断（能不能改、改成什么）都在 Dart 侧。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseDetailSheet(
    block: CourseBlock,
    week: Int,
    viewModel: TimetableViewModel,
    onDismiss: () -> Unit,
    onEdit: (TimetableCellDetail) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var detail by remember(block.courseId, week) { mutableStateOf<TimetableCellDetail?>(null) }

    var showDelete by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }
    // 「其他」栏的课没有节次（day = 0），既不能换色也不用显示节次。
    val hasFixedTime = block.day in 1..7 && block.endNode >= block.startNode

    LaunchedEffect(block.courseId, week) {
        runCatching {
            viewModel.cellDetail(block, week)
        }.onSuccess { detail = it }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // 详情字段 + 操作列表一次放不下时要能直接看全：跳过半展开，直接把卡片撑到内容高度。
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(block.name, style = MaterialTheme.typography.headlineSmallEmphasized)
                Spacer(Modifier.height(4.dp))
                // 「其他」栏的课没有节次（day = 0），只留周次那一行。
                if (hasFixedTime) {
                    Text(
                        "第 $week 周 · 周${weekdays.getOrElse(block.day - 1) { "" }} · " +
                            "第 ${block.startNode}-${block.endNode} 节",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (block.teacher.isNotEmpty()) DetailLine("教师", block.teacher)
                if (block.location.isNotEmpty()) DetailLine("地点", block.location)
                if (block.weeksLabel.isNotEmpty()) DetailLine("周次", block.weeksLabel)
                if (block.isManual) DetailLine("来源", "手动添加")
                val current = detail
                if (current != null) {
                    if (current.changes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        current.changes.forEach { group ->
                            Text(
                                group.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.onSurfaceVariant,
                            )
                            group.changes.forEach { change ->
                                DetailLine(changeLabel(change.field), change.after.ifEmpty { "—" })
                            }
                            Text(
                                group.affectedScope,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                            // 每个作用范围各自可恢复：这一天的修改、按周修改、调课后的位置。
                            val restoreLabel = restoreLabelOf(group.scope)
                            if (restoreLabel != null && hasFixedTime) {
                                TextButton(
                                    onClick = {
                                        viewModel.restoreChange(block, group.scope)
                                        onDismiss()
                                    },
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                ) {
                                    Text(restoreLabel)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                } else {
                    // 卡片一打开就有格子自带的信息，取详情只是补颜色名与改动记录，
                    // 失败也不展示报错（诊断信息走日志）。
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("读取详情…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SegmentedColumn(title = "操作") {
                item {
                    BaseWidget(
                        icon = "edit",
                        title = "修改课程",
                        description = "改名称、时间、周次", 
                        onClick = { detail?.let(onEdit) },
                        enabled = detail != null,
                    )
                }
                // 「其他」栏的课没有节次，颜色由自动配色统一给，不给单格换色入口。
                if (hasFixedTime) {
                    item {
                        BaseWidget(
                            icon = "palette",
                            title = "换个颜色",
                            description = "整门课一起换，也可恢复自动配色",
                            onClick = { showColor = true },
                            enabled = detail != null,
                        )
                    }
                }
                if (detail?.hasOverride == true) {
                    item {
                        BaseWidget(
                            icon = "restore",
                            title = "恢复教务课程",
                            description = "丢掉这门课在本机的全部改动",
                            onClick = {
                                viewModel.restoreCourse(block.courseId)
                                onDismiss()
                            },
                        )
                    }
                }
                item {
                    BaseWidget(
                        icon = "delete",
                        title = "删除",
                        description = "可以只删这一次，也可以删整门课",
                        isError = true,
                        onClick = { showDelete = true },
                    )
                }
            }
        }
    }

    if (showDelete) {
        DeleteCourseSheet(
            block = block,
            week = week,
            onDismiss = { showDelete = false },
            onDelete = { scope ->
                viewModel.deleteCourse(block, week, scope)
                showDelete = false
                onDismiss()
            },
        )
    }

    if (showColor) {
        ColorPickerDialog(
            onDismiss = { showColor = false },
            initialColorArgb = block.userColor,
            onPick = { argb ->
                viewModel.setCourseColor(block, argb)
                showColor = false
            },
        )
    }
}

/** 改动分组对应的恢复入口文案；整门课的恢复在「操作」区单独给。 */
private fun restoreLabelOf(scope: String): String? = when (scope) {
    "occurrence" -> "恢复这一天的修改"
    "position" -> "恢复原位置"
    else -> null
}

private fun changeLabel(field: String): String = when (field) {
    "name" -> "名称"
    "teacher" -> "教师"
    "location" -> "地点"
    "note" -> "备注"
    "time" -> "时间"
    "color" -> "颜色"
    "kind" -> "类型"
    "retake" -> "重修"
    else -> field
}

@Composable
private fun DetailLine(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
    }
}

/** 删除的三档作用范围，文案与「改这一次」口径一致。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteCourseSheet(
    block: CourseBlock,
    week: Int,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = scheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "删除「${block.name}」",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(12.dp))
            SegmentedColumn {
                item {
                    BaseWidget(
                        icon = "event_busy",
                        title = "只删这一次",
                        description = "第 $week 周周${weekdays.getOrElse(block.day - 1) { "" }}的这节课",
                        onClick = { onDelete("thisWeek") },
                    )
                }
                item {
                    BaseWidget(
                        icon = "calendar_month",
                        title = "删掉所有同星期的安排",
                        description = "每周周${weekdays.getOrElse(block.day - 1) { "" }}的同一时段",
                        onClick = { onDelete("weekdayMatch") },
                    )
                }
                item {
                    BaseWidget(
                        icon = "delete_forever",
                        title = "删掉整门课",
                        description = if (block.isManual) "手动课程，直接移除" else "教务课程，本机不再显示",
                        isError = true,
                        onClick = { onDelete("all") },
                    )
                }
            }
        }
    }
}

/** 颜色选择：色板一点即生效，另有「自选颜色」打开完整取色面板与「恢复自动」。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onPick: (Int?) -> Unit,
    initialColorArgb: Int? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        dev.duohuo.nuist.sta.compose.ui.kit.ColorPickerSheet(
            title = "自选课程颜色",
            initialColorArgb = initialColorArgb ?: coursePresetColors.first(),
            onDismiss = { picking = false },
            onConfirm = {
                picking = false
                onPick(it)
            },
        )
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = scheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text("课程颜色", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "整门课一起换，点色块立即生效",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(coursePresetColors) { argb ->
                    val selected = initialColorArgb != null && (argb and 0x00FFFFFF) == (initialColorArgb and 0x00FFFFFF)
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .then(
                                if (selected) {
                                    Modifier.border(2.dp, scheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onPick(argb) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BaseWidget(
                icon = "palette",
                title = "自选颜色",
                description = "用取色面板挑一个更合适的颜色",
                onClick = { picking = true },
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onPick(null) }) { Text("恢复自动") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    }
}

/** 拖动调课的作用范围：与参考实现的「调到哪几周」一致。 */
@Composable
internal fun MoveScopeDialog(
    block: CourseBlock,
    day: Int,
    startNode: Int,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调整上课时间") },
        text = {
            Column {
                Text(
                    "把「${block.name}」移到${weekdays.getOrElse(day - 1) { "" }}第 $startNode 节",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                listOf(
                    "thisWeek" to "仅这一次课",
                    "fromThisWeek" to "本周及以后",
                    "allWeeks" to "这门课的全部安排",
                ).forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(key) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
