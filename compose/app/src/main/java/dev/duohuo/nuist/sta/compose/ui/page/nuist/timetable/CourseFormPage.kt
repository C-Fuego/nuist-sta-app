// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.duohuo.nuist.sta.compose.data.timetable.CourseFormInput
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon

/** 课程类型：与参考实现同一套枚举名，Dart 侧按名字解析。 */
private val courseKinds = listOf(
    "standard" to "普通课",
    "lab" to "实验课",
    "pe" to "体育课",
    "exam" to "考试",
)

private val formWeekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** 保存时的作用范围，对应参考实现「将修改应用到」面板。 */
private enum class SaveScope(val label: String) {
    Once("仅这次课"),
    Future("本周及以后"),
    All("这一上课安排的所有周"),
    Custom("自选周次"),
}

/**
 * 课程表单（整页）。结构对齐参考实现：标题 + 关闭，中间可滚动字段，
 * 底部一个大按钮；周次、颜色、作用范围都走次级面板。
 *
 * 所有判断（能不能存、存到哪几周）都由 Dart 控制器做，这里只负责收集字段。
 */
@Composable
internal fun CourseFormPage(
    initial: CourseFormInput,
    title: String,
    totalWeeks: Int,
    periods: List<Pair<String, String>>,
    isEdit: Boolean,
    currentWeek: Int = 1,
    conflictsProvider: suspend (CourseFormInput) -> String?,
    onDismiss: () -> Unit,
    onSubmit: (CourseFormInput, List<Int>?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var name by remember(initial) { mutableStateOf(initial.name) }
    var teacher by remember(initial) { mutableStateOf(initial.teacher) }
    var location by remember(initial) { mutableStateOf(initial.location) }
    var note by remember(initial) { mutableStateOf(initial.note) }
    var kind by remember(initial) { mutableStateOf(initial.kind) }
    var day by remember(initial) { mutableStateOf(initial.day.coerceIn(1, 7)) }
    var start by remember(initial) { mutableStateOf(initial.startNode.coerceAtLeast(1)) }
    var end by remember(initial) { mutableStateOf(initial.endNode.coerceAtLeast(initial.startNode)) }
    var weeks by remember(initial) { mutableStateOf(initial.weeks) }
    val originalWeeks = remember(initial) { initial.weeks }
    var userColor by remember(initial) { mutableStateOf(initial.userColor) }
    var colorChanged by remember(initial) { mutableStateOf(initial.colorChanged) }

    var selectDialog by remember { mutableStateOf<String?>(null) }
    var weeksSheet by remember { mutableStateOf(false) }
    var colorSheet by remember { mutableStateOf(false) }
    var scopeSheet by remember { mutableStateOf(false) }
    var customPick by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    var weeksConfirm by remember { mutableStateOf(false) }
    var conflicts by remember { mutableStateOf<String?>(null) }
    var triedSave by remember { mutableStateOf(false) }

    val isExam = kind == "exam" && isEdit
    val input = CourseFormInput(
        courseId = initial.courseId,
        name = name.trim(),
        teacher = teacher.trim(),
        location = location.trim(),
        note = note.trim(),
        kind = kind,
        day = day,
        startNode = start,
        endNode = maxOf(start, end),
        weeks = weeks,
        userColor = userColor,
        colorChanged = colorChanged,
    )
    val clock = periods.getOrNull(start - 1)?.first to periods.getOrNull(maxOf(start, end) - 1)?.second
    val clockText = if (clock.first.isNullOrEmpty() || clock.second.isNullOrEmpty()) {
        "暂无对应作息时间"
    } else {
        "${clock.first} – ${clock.second}"
    }
    val dirty = input.copy(courseId = null) != initial.copy(courseId = null)

    LaunchedEffect(day, start, end, weeks, kind) {
        conflicts = if (weeks.isEmpty() || isExam) null else conflictsProvider(input)
    }

    Dialog(
        onDismissRequest = { if (dirty) confirmClose = true else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = scheme.surfaceContainer) {
            Column(Modifier.fillMaxSize()) {
                // 标题栏：关闭在左，标题居中，与其它整页表单一致。
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { if (dirty) confirmClose = true else onDismiss() }) {
                        Text("关闭")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    // 占位，保持标题居中。
                    Spacer(Modifier.width(72.dp))
                }

                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isExam) "考试名称" else "课程名称") },
                        placeholder = { Text("必填") },
                        singleLine = true,
                        isError = triedSave && name.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    FormSelectRow(
                        label = "课程类型",
                        value = courseKinds.firstOrNull { it.first == kind }?.second ?: "普通课",
                        onClick = { selectDialog = "kind" },
                    )
                    if (!isExam) {
                        OutlinedTextField(
                            value = teacher,
                            onValueChange = { teacher = it },
                            label = { Text("任课教师") },
                            placeholder = { Text("选填") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(if (isExam) "考试地点" else "上课地点") },
                        placeholder = { Text("教室、实验室或场地") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        if (isExam) "考试安排" else "上课安排",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    FormSelectRow(
                        label = "星期",
                        value = formWeekdays.getOrElse(day - 1) { "" },
                        onClick = { selectDialog = "day" },
                    )
                    FormSelectRow(
                        label = "开始节次",
                        value = "第 $start 节",
                        onClick = { selectDialog = "start" },
                    )
                    FormSelectRow(
                        label = "结束节次",
                        value = "第 ${maxOf(start, end)} 节",
                        onClick = { selectDialog = "end" },
                    )
                    FormTextRow(label = "实际时间", value = clockText)
                    if (isExam) {
                        FormTextRow(
                            label = "考试周次",
                            value = if (initial.courseId == null) "请从对应周的课程详情进入" else
                                "第 ${weeks.firstOrNull() ?: 1} 周（仅这次课）",
                        )
                    } else {
                        FormSelectRow(
                            label = "上课周次",
                            value = weeksSummary(weeks),
                            isError = triedSave && weeks.isEmpty(),
                            onClick = { weeksSheet = true },
                        )
                    }
                    if (conflicts != null && !isExam) {
                        Row(verticalAlignment = Alignment.Top) {
                            SymbolIcon(
                                name = "info",
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                tint = scheme.tertiary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$conflicts，仍可保存。",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (triedSave && weeks.isEmpty() && !isExam) {
                        Text(
                            "请至少选择一周",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.error,
                        )
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注（选填）") },
                        placeholder = { Text("例如：记得带实验报告") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // 编辑已有课程时颜色在详情卡的「换个颜色」里改，这里只有新建才选色。
                    if (!isExam && !isEdit) {
                        FormColorRow(
                            label = "课程颜色",
                            argb = userColor,
                            onClick = { colorSheet = true },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        triedSave = true
                        if (name.isBlank()) return@Button
                        if (!isExam && weeks.isEmpty()) return@Button
                        if (!isEdit) {
                            onSubmit(input, null)
                            return@Button
                        }
                        if (!dirty) {
                            onDismiss()
                            return@Button
                        }
                        if (weeks != originalWeeks) {
                            weeksConfirm = true
                            return@Button
                        }
                        scopeSheet = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                ) {
                    Text(if (isEdit) "保存修改" else "添加课程")
                }
            }
        }
    }

    // 下拉选择：类型 / 星期 / 节次
    when (selectDialog) {
        "kind" -> SingleChoiceDialog(
            title = "课程类型",
            items = courseKinds.filter { !(it.first == "exam") || isEdit }.map { it.second },
            selectedIndex = courseKinds
                .filter { !(it.first == "exam") || isEdit }
                .indexOfFirst { it.first == kind }.coerceAtLeast(0),
            onDismiss = { selectDialog = null },
            onSelect = { index ->
                kind = courseKinds.filter { !(it.first == "exam") || isEdit }[index].first
                selectDialog = null
            },
        )
        "day" -> SingleChoiceDialog(
            title = "星期",
            items = formWeekdays,
            selectedIndex = day - 1,
            onDismiss = { selectDialog = null },
            onSelect = { index ->
                day = index + 1
                selectDialog = null
            },
        )
        "start" -> SingleChoiceDialog(
            title = "开始节次",
            items = (1..periods.size.coerceAtLeast(1)).map { "第 $it 节" },
            selectedIndex = (start - 1).coerceAtLeast(0),
            onDismiss = { selectDialog = null },
            onSelect = { index ->
                start = index + 1
                if (end < start) end = start
                selectDialog = null
            },
        )
        "end" -> SingleChoiceDialog(
            title = "结束节次",
            items = (start..periods.size.coerceAtLeast(start)).map { "第 $it 节" },
            selectedIndex = (end - start).coerceAtLeast(0),
            onDismiss = { selectDialog = null },
            onSelect = { index ->
                end = start + index
                selectDialog = null
            },
        )
    }

    if (weeksSheet) {
        WeeksDialog(
            title = "选择上课周次",
            totalWeeks = totalWeeks,
            initial = weeks,
            onDismiss = { weeksSheet = false },
            onConfirm = {
                weeks = it.toSet()
                weeksSheet = false
            },
        )
    }
    if (colorSheet) {
        ColorPickerDialog(
            onDismiss = { colorSheet = false },
            initialColorArgb = userColor,
            onPick = { argb ->
                userColor = argb
                colorChanged = true
                colorSheet = false
            },
        )
    }
    if (scopeSheet) {
        SaveScopeDialog(
            currentWeekLabel = "第 $currentWeek 周",
            canScope = currentWeek in weeks,
            hasFuture = weeks.any { it >= currentWeek },
            onDismiss = { scopeSheet = false },
            onPick = { scope ->
                scopeSheet = false
                when (scope) {
                    SaveScope.Once -> onSubmit(input, listOf(currentWeek))
                    SaveScope.Future -> onSubmit(input, weeks.filter { it >= currentWeek }.sorted())
                    SaveScope.All -> onSubmit(input, null)
                    SaveScope.Custom -> customPick = true
                }
            },
        )
    }
    if (customPick) {
        WeeksDialog(
            title = "自选周次",
            totalWeeks = totalWeeks,
            initial = weeks,
            onDismiss = { customPick = false },
            onConfirm = { picked ->
                customPick = false
                onSubmit(input, picked)
            },
        )
    }
    if (weeksConfirm) {
        AlertDialog(
            onDismissRequest = { weeksConfirm = false },
            title = { Text("调整上课周次") },
            text = {
                Column {
                    Text(
                        "原来：${weeksSummary(originalWeeks)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("调整为：${weeksSummary(weeks)}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "只作用于这一上课安排，同一门课的其他时间段不变。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    weeksConfirm = false
                    scopeSheet = true
                }) { Text("确认保存") }
            },
            dismissButton = { TextButton(onClick = { weeksConfirm = false }) { Text("再改改") } },
        )
    }
    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("放弃未保存的修改？") },
            text = { Text("离开后，这次填写的内容不会保存。") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("放弃修改") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) { Text("继续编辑") }
            },
        )
    }
}

@Composable
private fun FormSelectRow(
    label: String,
    value: String,
    isError: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
            modifier = Modifier.width(96.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) scheme.error else scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        SymbolIcon(name = "chevron_right", tint = scheme.onSurfaceVariant)
    }
}

@Composable
private fun FormTextRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(96.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun FormColorRow(label: String, argb: Int?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(96.dp))
        Box(
            Modifier.size(14.dp).clip(CircleShape)
                .background(if (argb != null) Color(argb) else scheme.surfaceContainerHighest)
                .padding(0.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (argb == null) "自动" else "自选颜色",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        SymbolIcon(name = "chevron_right", tint = scheme.onSurfaceVariant)
    }
}

/** 周次选择面板：全部 / 单周 / 双周 / 清空 + 逐周点选。 */
@Composable
private fun WeeksDialog(
    title: String,
    totalWeeks: Int,
    initial: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var selection by remember(initial) { mutableStateOf(initial) }
    val weeks = (1..totalWeeks).toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selection = weeks.toSet() }) { Text("全部") }
                    TextButton(onClick = { selection = weeks.filter { it % 2 == 1 }.toSet() }) { Text("单周") }
                    TextButton(onClick = { selection = weeks.filter { it % 2 == 0 }.toSet() }) { Text("双周") }
                    TextButton(onClick = { selection = emptySet() }) { Text("清空") }
                }
                Text(
                    if (selection.isEmpty()) "未选择周次" else weeksSummary(selection),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                weeks.chunked(8).forEach { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chunk.forEach { w ->
                            FilterChip(
                                selected = w in selection,
                                onClick = {
                                    selection = if (w in selection) selection - w else selection + w
                                },
                                label = { Text("$w", fontFamily = FontFamily.Monospace) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selection.sorted()) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 「将修改应用到」：与参考实现同一组选项。 */
@Composable
private fun SaveScopeDialog(
    currentWeekLabel: String,
    canScope: Boolean,
    hasFuture: Boolean,
    onDismiss: () -> Unit,
    onPick: (SaveScope) -> Unit,
) {
    val scopes = SaveScope.entries.filter { scope ->
        when (scope) {
            SaveScope.Once -> canScope
            SaveScope.Future -> hasFuture
            else -> true
        }
    }
    // 默认「仅这一次课」：改一天不该顺手改掉整门课（参考实现同此默认）。
    var picked by remember { mutableStateOf(if (canScope) SaveScope.Once else SaveScope.All) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("将修改应用到") },
        text = {
            Column {
                scopes.forEach { scope ->
                    Row(
                        Modifier.fillMaxWidth().clickable { picked = scope }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = picked == scope, onClick = { picked = scope })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(scope.label)
                            if (scope == SaveScope.Once) {
                                Text(
                                    currentWeekLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(picked) }) { Text("确认保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 周次摘要：连续段折叠成「1-5, 7, 9-11 周」，与参考实现一致。 */
internal fun weeksSummary(weeks: Set<Int>): String {
    val sorted = weeks.sorted()
    if (sorted.isEmpty()) return "请选择周次"
    val parts = mutableListOf<String>()
    var begin = sorted.first()
    var prev = sorted.first()
    for (w in sorted.drop(1)) {
        if (w == prev + 1) {
            prev = w
            continue
        }
        parts += if (begin == prev) "$begin" else "$begin-$prev"
        begin = w
        prev = w
    }
    parts += if (begin == prev) "$begin" else "$begin-$prev"
    return parts.joinToString(", ") + " 周"
}
