// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableExportPreview
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import kotlinx.coroutines.launch

/** 跳过原因 → 一句话；与参考实现同一套文案。 */
private fun skipReasonLabel(reason: String): String = when (reason) {
    "noFixedTime" -> "没有固定时间"
    "emptyWeeks" -> "没排周次"
    else -> reason
}

/**
 * 导出到 WakeUp（整页）。课表快照的组装、去身份化、格式校验全在 Dart 侧，
 * 这里只展示预览、触发口令生成并把口令放进剪贴板。
 */
@Composable
internal fun TimetableExportPage(
    viewModel: TimetableViewModel,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 整页是独立窗口，父级 Scaffold 的 Snackbar 会被它挡住，提示只能在这里弹。
    val snackbar = remember { SnackbarHostState() }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    var preview by remember { mutableStateOf<TimetableExportPreview?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingError by remember { mutableStateOf<String?>(null) }
    var generating by remember { mutableStateOf(false) }
    var shareText by remember { mutableStateOf<String?>(null) }
    var showSkipped by remember { mutableStateOf(false) }
    // 重新读取时 +1，LaunchedEffect 跟着重跑。
    var attempt by remember { mutableStateOf(0) }

    fun copyToClipboard(text: String): Boolean {
        val manager = context.getSystemService(ClipboardManager::class.java) ?: return false
        return runCatching {
            manager.setPrimaryClip(ClipData.newPlainText("WakeUp 口令", text))
            true
        }.getOrDefault(false)
    }

    LaunchedEffect(attempt) {
        loading = true
        loadingError = null
        val result = runCatching { viewModel.exportPreview() }.getOrNull()
        preview = result
        loadingError = when {
            result == null -> "无法读取课表，请稍后重试"
            !result.ok -> result.message.ifEmpty { "无法读取课表，请稍后重试" }
            else -> null
        }
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = scheme.surfaceContainer) {
            Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // 标题栏：关闭在左，标题居中。
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    Spacer(Modifier.weight(1f))
                    Text("导出到 WakeUp", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(72.dp))
                }

                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        loading -> Row(
                            Modifier.fillMaxWidth().padding(top = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("正在读取课表…", style = MaterialTheme.typography.bodyMedium)
                        }

                        loadingError != null -> Column {
                            Text(
                                loadingError.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.error,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                            TextButton(onClick = { attempt++ }) { Text("重新读取") }
                        }

                        preview != null -> {
                            val p = preview!!
                            Surface(
                                color = scheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        p.name.ifEmpty { "当前课表" },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        "${p.courseCount} 门课程、${p.examCount} 场考试、" +
                                            "${p.otherCount} 个其他日程",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                            p.warnings.forEach { warning ->
                                Row(verticalAlignment = Alignment.Top) {
                                    SymbolIcon(
                                        name = "info",
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                        tint = scheme.tertiary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        warning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (p.skipped.isNotEmpty()) {
                                TextButton(onClick = { showSkipped = true }) {
                                    Text("有 ${p.skipped.size} 门课未导出")
                                }
                            }

                            if (shareText == null) {
                                Button(
                                    onClick = {
                                        if (generating) return@Button
                                        generating = true
                                        scope.launch {
                                            val result = runCatching { viewModel.exportWakeup() }.getOrNull()
                                            generating = false
                                            when {
                                                result == null -> notify("无法生成口令，请检查网络或稍后重试")
                                                !result.ok -> notify(
                                                    result.message.ifEmpty { "无法生成口令，请检查网络或稍后重试" },
                                                )
                                                else -> {
                                                    shareText = result.shareText
                                                    notify(
                                                        if (copyToClipboard(result.shareText)) "已复制口令"
                                                        else "复制失败，请长按口令手动复制",
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    enabled = !generating,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (generating) "正在生成…" else "生成 WakeUp 口令")
                                }
                            } else {
                                Surface(
                                    color = scheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    SelectionContainer {
                                        Text(
                                            shareText.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        notify(
                                            if (copyToClipboard(shareText.orEmpty())) "已复制口令"
                                            else "复制失败，请长按口令手动复制",
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("复制口令")
                                }
                            }
                            Box(Modifier.height(8.dp))
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    if (showSkipped) {
        val skipped = preview?.skipped.orEmpty()
        AlertDialog(
            onDismissRequest = { showSkipped = false },
            title = { Text("未导出的课") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    skipped.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                item.courseName.ifEmpty { "未命名课程" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                skipReasonLabel(item.reason),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "WakeUp 里需要手动补上",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSkipped = false }) { Text("知道了") }
            },
        )
    }
}