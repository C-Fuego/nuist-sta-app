// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.portal

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.PortalStatus
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 导出通行密钥，对齐原项目 `portal_export_page.dart`：
 * 设置六位 PIN → 本地加密成 `nuistkey1:` 文本 → 复制或存成文件。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PortalExportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bridge = koinInject<CampusBridge>()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme

    var bundle by remember { mutableStateOf<PortalStatus?>(null) }
    LaunchedEffect(Unit) {
        bundle = runCatching { bridge.portalStatus() }.getOrNull()?.takeIf { it.bound }
    }

    var pin by rememberSaveable { mutableStateOf("") }
    var pinAgain by rememberSaveable { mutableStateOf("") }
    var exportText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val text = exportText ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
            }.isSuccess
            snackbar.showSnackbar(if (ok) "已保存到文件" else "保存失败，请换一个位置再试")
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("导出通行密钥", modifier = Modifier.padding(start = 12.dp)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val current = bundle
            if (current == null) {
                SegmentedColumn {
                    item {
                        BaseWidget(
                            icon = "link_off",
                            title = "还没有绑定通行密钥",
                            description = "先在「统一门户」页绑定或导入一份凭据",
                        )
                    }
                }
            } else {
                Text(
                    "导出内容包含本机私钥，任何拿到它并猜中六位 PIN 的人都能以你的身份登录统一门户。\n\n请只通过可信渠道把内容传到自己的另一台设备。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                SegmentedColumn(title = "凭据") {
                    item {
                        BaseWidget(
                            icon = "verified_user",
                            title = current.maskedStudentId.ifEmpty { "未知学号" },
                            description = current.deviceName.ifEmpty { "未记录设备名" } + " · " +
                                current.createdAt.take(10),
                        )
                    }
                }
                SegmentedColumn(title = "设置导出 PIN") {
                    item {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                            singleLine = true,
                            label = { Text("六位数字 PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = pinAgain,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinAgain = it },
                            singleLine = true,
                            label = { Text("再输入一次") },
                            isError = pinAgain.isNotEmpty() && pinAgain != pin,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }
                }
                error?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                Button(
                    onClick = {
                        error = null
                        scope.launch {
                            runCatching { bridge.exportCredential(pin) }
                                .onSuccess { text ->
                                    if (text.isNullOrBlank()) {
                                        error = "生成导出内容失败"
                                    } else {
                                        exportText = text
                                    }
                                }
                                .onFailure { e -> error = e.message ?: "生成导出内容失败" }
                        }
                    },
                    enabled = pin.length == 6 && pinAgain == pin && exportText == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) { Text("生成导出内容") }

                exportText?.let { text ->
                    Spacer(Modifier.height(8.dp))
                    SegmentedColumn(title = "导出内容") {
                        item {
                            SelectionContainer {
                                Text(
                                    text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val manager = context.getSystemService(ClipboardManager::class.java)
                                manager?.setPrimaryClip(ClipData.newPlainText("NUIST++ 通行密钥", text))
                                scope.launch { snackbar.showSnackbar("已复制到剪贴板") }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("复制到剪贴板") }
                        Button(
                            onClick = {
                                val name = "nuist-" + (current.studentId ?: "key") + "-" +
                                    current.createdAt.take(10) + ".nuistkey"
                                runCatching { saveLauncher.launch(name) }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("保存为文件") }
                    }
                    QRContentSection(text = text)
                }
            }
        }
    }
}


/**
 * 导出内容的二维码：另一台设备用「导入通行密钥 → 扫描二维码」直接读，省手输。
 */
@Composable
private fun QRContentSection(text: String) {
    val scheme = MaterialTheme.colorScheme
    val bitmap = remember(text) { PortalQrCode.encode(text) }
    SegmentedColumn(title = "二维码") {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "通行密钥二维码",
                        modifier = Modifier.size(240.dp),
                    )
                } else {
                    Text(
                        "二维码生成失败，请改用复制或文件导出。",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                    )
                }
                Text(
                    "在另一台设备上打开「导入通行密钥 → 扫描二维码」读取。",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
