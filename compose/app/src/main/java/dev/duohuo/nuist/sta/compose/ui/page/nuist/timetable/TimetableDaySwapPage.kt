// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 正在挑哪一格日期。 */
private enum class SwapDateSlot { From, To }

/** 「M月D日」，与参考实现的日期标签一致。 */
private fun formatDay(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun utcMillisToDate(millis: Long): LocalDate? = runCatching {
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}.getOrNull()

/**
 * 整天调课：选两天互换，下方列出已调过的记录并可逐条恢复。
 *
 * 校验（同一天、超出学期、已调过）与写入都在 Dart 侧；本页只负责选日期、
 * 二次确认和把结果那句话弹出来。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimetableDaySwapPage(
    onDismiss: () -> Unit,
    /** 长按周视图日期表头时带入的「原日期」，进页面就填好。 */
    initialFrom: LocalDate? = null,
    viewModel: TimetableManagementViewModel = koinViewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val state by viewModel.daySwaps.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var from by remember { mutableStateOf(initialFrom) }
    var to by remember { mutableStateOf<LocalDate?>(null) }
    var picking by remember { mutableStateOf<SwapDateSlot?>(null) }
    var confirming by remember { mutableStateOf(false) }
    // 重新读取时 +1，LaunchedEffect 跟着重跑。
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) { viewModel.loadDaySwaps() }
    TimetableBridgeMessageEffect(message, snackbar, viewModel::clearMessage)

    TimetableDialogPage(title = "整天调课", onDismiss = onDismiss, snackbar = snackbar) {
        Surface(color = scheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("两天课程互换", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SwapDateBlock(
                        modifier = Modifier.weight(1f),
                        label = "原日期",
                        date = from,
                        onClick = { picking = SwapDateSlot.From },
                    )
                    SwapDateBlock(
                        modifier = Modifier.weight(1f),
                        label = "目标日期",
                        date = to,
                        onClick = { picking = SwapDateSlot.To },
                    )
                }
                Button(
                    onClick = { confirming = true },
                    enabled = from != null && to != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("互换")
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "调课记录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.failed) {
                TextButton(onClick = { attempt++ }) { Text("重新读取") }
            }
        }
        when {
            state.loading -> TimetableLoadingRow("正在读取调课记录…")

            state.failed -> Text(
                "无法读取调课记录，请稍后重试",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error,
            )

            state.swaps.isEmpty() -> Text(
                "还没有调课记录。\n在周视图长按日期或在上方选择两天互换。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )

            else -> state.swaps.forEach { swap ->
                Surface(color = scheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            swap.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        )
                        TextButton(onClick = { scope.launch { viewModel.removeDaySwap(swap.id) } }) {
                            Text("恢复")
                        }
                    }
                }
            }
        }
    }

    if (confirming && from != null && to != null) {
        val a = from!!
        val b = to!!
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("整天调课") },
            text = {
                Text("${formatDay(a)}和${formatDay(b)}的课程将整天互换，可随时在下方「调课记录」里恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        scope.launch {
                            // 换成功后清掉目标日期，和参考实现一致。
                            if (viewModel.addDaySwap(a, b)) to = null
                        }
                    },
                ) {
                    Text("与${formatDay(b)}互换")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("取消") }
            },
        )
    }

    val slot = picking
    if (slot != null) {
        val current = if (slot == SwapDateSlot.From) from else to
        val first = state.semesterFirst
        val last = state.semesterLast
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (current ?: state.today ?: first)?.toUtcMillis(),
            selectableDates = remember(first, last) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date = utcMillisToDate(utcTimeMillis) ?: return false
                        return (first == null || !date.isBefore(first)) &&
                            (last == null || !date.isAfter(last))
                    }

                    override fun isSelectableYear(year: Int): Boolean =
                        (first == null || year >= first.year) &&
                            (last == null || year <= last.year)
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = pickerState.selectedDateMillis?.let(::utcMillisToDate)
                        if (date != null) {
                            if (slot == SwapDateSlot.From) from = date else to = date
                        }
                        picking = null
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** 一格日期：没有选时显示「选择日期」。 */
@Composable
private fun SwapDateBlock(
    modifier: Modifier = Modifier,
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                date?.let(::formatDay) ?: "选择日期",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}