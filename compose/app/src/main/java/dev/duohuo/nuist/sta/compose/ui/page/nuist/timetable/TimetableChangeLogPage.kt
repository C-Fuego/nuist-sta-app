// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableChangeItem
import org.koin.androidx.compose.koinViewModel

/**
 * 变动记录：每次同步后课表的改动，按同步批次分组。
 *
 * 六类变更与「时间变动」的归并都在 Dart 侧完成，这里只按组标题与行文案排版。
 */
@Composable
internal fun TimetableChangeLogPage(
    onDismiss: () -> Unit,
    viewModel: TimetableManagementViewModel = koinViewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    val log by viewModel.changeLog.collectAsStateWithLifecycle()
    val loading by viewModel.changeLogLoading.collectAsStateWithLifecycle()
    // 重新读取时 +1，LaunchedEffect 跟着重跑。
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) { viewModel.loadChangeLog() }

    TimetableDialogPage(title = "变动记录", onDismiss = onDismiss, snackbar = snackbar) {
        val current = log
        when {
            loading && current == null -> TimetableLoadingRow("正在读取变动记录…")

            current == null -> {
                Text(
                    "无法读取变动记录，请稍后重试",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
                TextButton(onClick = { attempt++ }) { Text("重新读取") }
            }

            current.groups.isEmpty() -> Text(
                "还没有变动",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )

            else -> current.groups.forEach { group ->
                Text(
                    group.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                group.items.forEach { item -> ChangeLogRow(item) }
            }
        }
    }
}

/** 一条变动：课程名 + 类型胶囊 + 影响范围 + 明细。 */
@Composable
private fun ChangeLogRow(item: TimetableChangeItem) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.courseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = scheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        item.typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            if (item.scope.isNotEmpty()) {
                Text(
                    item.scope,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (item.detail.isNotEmpty()) {
                Text(
                    item.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}