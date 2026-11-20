// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.portal

import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.InlineLoadingIndicator
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import kotlinx.coroutines.launch
import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.PortalPreview
import org.koin.compose.koinInject

/**
 * 导入通行密钥，对齐原项目 `portal_import_page.dart`：
 * 三种来源拿到导出文本 → 输 PIN 解密 → 预览确认 → 覆盖本机凭据。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PortalImportScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    val bridge = koinInject<CampusBridge>()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme

    var busy by remember { mutableStateOf(false) }
    var pasteOpen by rememberSaveable { mutableStateOf(false) }
    var pinOpen by rememberSaveable { mutableStateOf(false) }
    var pendingText by rememberSaveable { mutableStateOf("") }
    var lastPin by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<PortalPreview?>(null) }
    var scanOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun takeText(text: String) {
        pendingText = text.trim()
        if (!pendingText.startsWith("nuistkey1:")) {
            error = "这不是通行密钥导出文本（应以 nuistkey1: 开头）"
            return
        }
        error = null
        pinOpen = true
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            busy = false
            if (text.isNullOrBlank()) {
                error = "读取文件失败，请确认选择的是导出的 .nuistkey 文件"
            } else {
                takeText(text)
            }
        }
    }

    if (pasteOpen) {
        PortalPasteDialog(
            initial = clipboardText(context),
            onDismiss = { pasteOpen = false },
            onConfirm = { text ->
                pasteOpen = false
                scope.launch { takeText(text) }
            },
        )
    }
    if (pinOpen) {
        PortalPinDialog(
            onDismiss = { pinOpen = false },
            onConfirm = { pin ->
                pinOpen = false
                lastPin = pin
                busy = true
                scope.launch {
                    val result = runCatching { bridge.previewCredential(pendingText, pin) }
                    busy = false
                    result.onSuccess { value ->
                        when {
                            value.ok -> preview = value
                            value.reason == "wrong_format" ->
                                error = "这不是通行密钥导出文本（应以 nuistkey1: 开头）"
                            else -> error = "PIN 不正确，或导出内容已损坏"
                        }
                    }.onFailure { e -> error = e.toUserMessage() }
                }
            },
        )
    }
    preview?.let { value ->
        PortalImportPreviewDialog(
            preview = value,
            onDismiss = { preview = null },
            onConfirm = {
                busy = true
                scope.launch {
                    val failure = runCatching { bridge.importCredential(pendingText, lastPin) }
                        .getOrElse { e -> error = e.toUserMessage(); null }
                    busy = false
                    preview = null
                    if (failure == null) {
                        snackbar.showSnackbar("凭据已导入")
                        onDone()
                    }
                }
            },
        )
    }
    if (scanOpen) {
        PortalScanDialog(
            onDismiss = { scanOpen = false },
            onResult = { text ->
                scanOpen = false
                scope.launch { takeText(text) }
            },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("导入通行密钥", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton(onBack = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surfaceContainer,
                    titleContentColor = scheme.onBackground,
                    scrolledContainerColor = scheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "在另一台设备的「导出通行密钥」页生成内容后，选择一种方式读入。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                SegmentedColumn {
                    item {
                        BaseWidget(
                            icon = "qr_code_scanner",
                            title = "扫描二维码",
                            description = "对准另一台设备屏幕上的二维码",
                            onClick = { scanOpen = true },
                            enabled = !busy,
                            trailingContent = { Chevron() },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "content_paste",
                            title = "粘贴文本",
                            description = "粘贴以 nuistkey1: 开头的内容",
                            onClick = { pasteOpen = true },
                            enabled = !busy,
                            trailingContent = { Chevron() },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "folder_open",
                            title = "选择文件",
                            description = "读取 .nuistkey 文件",
                            onClick = { runCatching { filePicker.launch(arrayOf("*/*")) } },
                            enabled = !busy,
                            trailingContent = { Chevron() },
                        )
                    }
                }
                error?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            if (busy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) { InlineLoadingIndicator(size = 32.dp) }
            }
        }
    }
}

private fun clipboardText(context: android.content.Context): String {
    val manager = context.getSystemService(ClipboardManager::class.java) ?: return ""
    val clip = manager.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(context).toString()
}

@Composable
private fun PortalPasteDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("粘贴文本") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 6,
                maxLines = 10,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("下一步") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PortalPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var pin by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("输入 PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                singleLine = true,
                label = { Text("六位数字 PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = pin.length == 6) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PortalImportPreviewDialog(
    preview: PortalPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val replacing = preview.existing
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认导入") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("学号：${preview.maskedStudentId.ifEmpty { "未知" }}")
                Text("凭据名称：${preview.deviceName.ifEmpty { "未记录" }}")
                Text("绑定时间：${preview.createdAt.take(16).replace('T', ' ')}")
                if (replacing) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "本机已有一份通行密钥，导入后会被替换。",
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(if (replacing) "替换" else "导入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}


@Composable
private fun Chevron() {
    val scheme = MaterialTheme.colorScheme
    dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon(
        "chevron_right",
        fontSize = 20.sp,
        tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}