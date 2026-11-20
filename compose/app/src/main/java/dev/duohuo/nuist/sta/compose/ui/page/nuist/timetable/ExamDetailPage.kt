// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableEventDetail
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 一周七天的中文标签，考试、日程、课程表单共用。 */
private val eventWeekdays = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 点开考试块后的详情：整页展示全部字段，编辑也在同一页里完成。
 *
 * 教务考试改过之后会多出「恢复原考试」；手动日程则是普通的编辑/删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExamDetailPage(
    item: ExamGridItem,
    viewModel: TimetableViewModel,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var detail by remember(item.exam.eventId) { mutableStateOf<TimetableEventDetail?>(null) }
    var failed by remember(item.exam.eventId) { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    LaunchedEffect(item.exam.eventId) {
        runCatching { viewModel.eventDetail(item.exam.eventId) }
            .onSuccess { detail = it }
            .onFailure { failed = true }
    }

    val event = detail?.event
    val date = remember(event?.date) { event?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
    val title = event?.title ?: item.exam.title
    val week = event?.examWeek?.takeIf { it > 0 } ?: item.exam.week

    // 编辑仍用整页表单（字段多、要弹日期/时间选择器），详情本身是卡片，和课程详情同一套骨架。
    if (editing) {
        Dialog(
            onDismissRequest = { editing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = scheme.surfaceContainer) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { editing = false }) { Text("关闭") }
                        Spacer(Modifier.weight(1f))
                        Text("编辑考试", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.width(72.dp))
                    }
                    ExamEditForm(
                        event = event,
                        fallbackTitle = title,
                        fallbackDate = date,
                        fallbackStart = item.exam.start,
                        fallbackEnd = item.exam.end,
                        onCancel = { editing = false },
                        onSave = { name, newDate, start, end, location, note ->
                            viewModel.saveEvent(item.exam.eventId, name, newDate, start, end, location, note)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmallEmphasized)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(if (event?.isExam != false) "考试" else "日程")
                        if (week > 0) append(" · 第 $week 周")
                        if (date != null) append(" · 周${eventWeekdays[(date.dayOfWeek.value - 1).coerceIn(0, 6)]}")
                        append(" · 第 ${item.startNode}-${item.endNode} 节")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                )
                if (detail?.overridden == true) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "已修改 · 可恢复成教务原考试",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.tertiary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                EventDetailRow("日期", date?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: "—")
                EventDetailRow("时间", "${item.exam.start} – ${item.exam.end}")
                EventDetailRow("节次", "第 ${item.startNode}-${item.endNode} 节")
                if (item.exam.location.isNotEmpty()) EventDetailRow("地点", item.exam.location)
                if (item.exam.note.isNotEmpty()) EventDetailRow("备注", item.exam.note)
                if (failed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "读取这场考试的记录失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                    )
                } else if (detail == null) {
                    Spacer(Modifier.height(8.dp))
                    Text("读取记录…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            SegmentedColumn(title = "操作") {
                item {
                    BaseWidget(
                        icon = "edit",
                        title = "修改考试",
                        description = "改名称、日期或起止时间",
                        onClick = { editing = true },
                        enabled = event != null,
                    )
                }
                if (detail?.overridden == true) {
                    item {
                        BaseWidget(
                            icon = "restore",
                            title = "恢复教务原考试",
                            description = "丢掉这场考试在本机的改动",
                            onClick = {
                                viewModel.restoreEvent(item.exam.eventId)
                                onDismiss()
                            },
                        )
                    }
                }
                item {
                    BaseWidget(
                        icon = "delete",
                        title = "删除",
                        description = "删除后可以同步课表恢复",
                        isError = true,
                        onClick = { deleting = true },
                    )
                }
            }
        }
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("删除这场考试？") },
            text = { Text("删除后可以同步课表恢复；如果只是想改时间，请用「修改考试」。") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    viewModel.deleteEvent(item.exam.eventId)
                    onDismiss()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } },
        )
    }
}

/** 编辑考试/日程：名称、日期、起止时间、地点、备注。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamEditForm(
    event: dev.duohuo.nuist.sta.compose.data.timetable.TimetableEvent?,
    fallbackTitle: String,
    fallbackDate: LocalDate?,
    fallbackStart: java.time.LocalTime,
    fallbackEnd: java.time.LocalTime,
    onCancel: () -> Unit,
    onSave: (title: String, date: String, start: String, end: String, location: String, note: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(event?.id) { mutableStateOf(event?.title ?: fallbackTitle) }
    var date by remember(event?.id) {
        mutableStateOf(event?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: fallbackDate)
    }
    var start by remember(event?.id) { mutableStateOf(event?.start ?: "%02d:%02d".format(fallbackStart.hour, fallbackStart.minute)) }
    var end by remember(event?.id) { mutableStateOf(event?.end ?: "%02d:%02d".format(fallbackEnd.hour, fallbackEnd.minute)) }
    var location by remember(event?.id) { mutableStateOf(event?.location ?: "") }
    var note by remember(event?.id) { mutableStateOf(event?.note ?: "") }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(12.dp))
        ExamPickRow("日期", date?.let { "${it.year}年${it.monthValue}月${it.dayOfMonth}日" } ?: "请选择") {
            pickingDate = true
        }
        ExamPickRow("开始时间", start) { pickingTime = "start" }
        ExamPickRow("结束时间", end) { pickingTime = "end" }
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("地点") },
            placeholder = { Text("选填") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("备注") },
            placeholder = { Text("选填") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
        Button(
            onClick = {
                val picked = date ?: return@Button
                onSave(
                    title.trim(),
                    "%04d-%02d-%02d".format(picked.year, picked.monthValue, picked.dayOfMonth),
                    start,
                    end,
                    location.trim(),
                    note.trim(),
                )
            },
            modifier = Modifier.weight(1f),
            enabled = title.isNotBlank(),
        ) { Text("保存") }
    }

    if (pickingDate) {
        val initial = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
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

@Composable
private fun EventDetailRow(label: String, value: String, unused: String = "") {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
    }
}

@Composable
private fun ExamPickRow(label: String, value: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = scheme.primary)
    }
}
