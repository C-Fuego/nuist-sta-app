// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 新建考试 / 日程的表单输入。日期为 `yyyy-MM-dd`，时间为 `HH:mm`，与桥参数一致。
 */
internal data class EventFormInput(
    val kind: String,
    val title: String,
    val date: String,
    val start: String,
    val end: String,
    val location: String,
    val note: String,
    val courseCode: String,
)

/**
 * 新建考试 / 其他日程：整页表单，字段与课程表单同一套骨架。
 *
 * [initialKind] 只决定进来时的类型，用户仍可在表单里切换（`"exam"` / `"other"`）。
 * 时间非法时不允许提交；日期不在本学期的提示只是提醒，仍然可以保存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventFormPage(
    initialKind: String = "exam",
    initialDate: LocalDate = LocalDate.now(),
    today: LocalDate = LocalDate.now(),
    currentWeek: Int = 1,
    totalWeeks: Int = 20,
    weekStartsMonday: Boolean = true,
    periods: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (EventFormInput) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var kind by remember(initialKind) { mutableStateOf(if (initialKind == "other") "other" else "exam") }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var start by remember { mutableStateOf(periods.getOrNull(0)?.first ?: "08:00") }
    var end by remember { mutableStateOf(periods.getOrNull(1)?.second ?: "09:40") }
    var location by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<String?>(null) }
    var selectDialog by remember { mutableStateOf(false) }
    var triedSave by remember { mutableStateOf(false) }

    val isExam = kind == "exam"
    val timeValid = end > start
    val outsideTerm = runCatching {
        val week = ExamGrid.weekOf(date, today, currentWeek, weekStartsMonday)
        week < 1 || week > totalWeeks
    }.getOrDefault(false)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = scheme.surfaceContainer) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    Spacer(Modifier.weight(1f))
                    Text(if (isExam) "添加考试" else "添加日程", style = MaterialTheme.typography.titleLarge)
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
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isExam) "考试名称" else "名称") },
                        placeholder = { Text("必填") },
                        singleLine = true,
                        isError = triedSave && title.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    EventPickRow(
                        icon = "sell",
                        label = "类型",
                        value = if (isExam) "考试" else "其他日程",
                        onClick = { selectDialog = true },
                    )
                    EventPickRow(
                        icon = "calendar_month",
                        label = "日期",
                        value = "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
                        onClick = { pickingDate = true },
                    )
                    if (outsideTerm) {
                        Text(
                            "不在本学期，周视图里看不到",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    EventPickRow(
                        icon = "schedule",
                        label = "开始时间",
                        value = start,
                        onClick = { pickingTime = "start" },
                    )
                    EventPickRow(
                        icon = "schedule",
                        label = "结束时间",
                        value = end,
                        onClick = { pickingTime = "end" },
                    )
                    if (!timeValid) {
                        Text(
                            "结束时间需晚于开始时间",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (triedSave) scheme.error else scheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(if (isExam) "考试地点" else "地点") },
                        placeholder = { Text("选填") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注") },
                        placeholder = { Text("选填") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isExam) {
                        OutlinedTextField(
                            value = courseCode,
                            onValueChange = { courseCode = it },
                            label = { Text("课程号") },
                            placeholder = { Text("选填") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                Button(
                    onClick = {
                        triedSave = true
                        if (title.isBlank() || !timeValid) return@Button
                        onSubmit(
                            EventFormInput(
                                kind = kind,
                                title = title.trim(),
                                date = "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth),
                                start = start,
                                end = end,
                                location = location.trim(),
                                note = note.trim(),
                                courseCode = if (isExam) courseCode.trim() else "",
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(if (isExam) "添加考试" else "添加日程")
                }
            }
        }
    }

    if (selectDialog) {
        SingleChoiceDialog(
            title = "类型",
            items = listOf("考试", "其他日程"),
            selectedIndex = if (isExam) 0 else 1,
            onDismiss = { selectDialog = false },
            onSelect = { index ->
                kind = if (index == 0) "exam" else "other"
                selectDialog = false
            },
        )
    }

    if (pickingDate) {
        val initial = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }

    val picking = pickingTime
    if (picking != null) {
        val parts = (if (picking == "start") start else end).split(':')
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = null },
            title = { Text(if (picking == "start") "开始时间" else "结束时间") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val text = "%02d:%02d".format(state.hour, state.minute)
                    if (picking == "start") start = text else end = text
                    pickingTime = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = null }) { Text("取消") } },
        )
    }
}
/** 表单里的一行「标签 + 当前值」，点击后弹出对应的选择器。 */
@Composable
private fun EventPickRow(icon: String, label: String, value: String, onClick: () -> Unit) {
    BaseWidget(
        icon = icon,
        title = label,
        description = value,
        descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick,
    )
}
