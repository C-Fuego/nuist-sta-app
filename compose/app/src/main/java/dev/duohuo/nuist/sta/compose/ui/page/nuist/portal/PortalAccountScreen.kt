// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.portal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.data.bridge.PortalStatus
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.InlineLoadingIndicator
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import org.koin.androidx.compose.koinViewModel

/**
 * 「统一门户」状态页：状态 → 凭据信息 → 操作 → 测试登录。
 * 布局与行式分组同本项目其它页面（SegmentedColumn + BaseWidget）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PortalAccountScreen(
    onBack: () -> Unit,
    onOpenBind: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    viewModel: PortalAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeMessage()
    }

    var confirmUnbind by rememberSaveable { mutableStateOf(false) }
    var confirmRebind by rememberSaveable { mutableStateOf(false) }
    var confirmExport by rememberSaveable { mutableStateOf(false) }

    if (confirmUnbind) {
        PortalConfirmDialog(
            title = "解除绑定",
            message = "将删除本机保存的通行密钥与登录状态，APP 内需要门户身份的功能会停止工作。\n\n" +
                "注意：这只会删除本机凭据，门户上那条通行密钥记录依然存在；" +
                "如需彻底移除，请到门户的「账户安全 → 通行密钥」页面删除。",
            confirmText = "解除绑定",
            destructive = true,
            onDismiss = { confirmUnbind = false },
            onConfirm = {
                confirmUnbind = false
                viewModel.unbind()
            },
        )
    }
    if (confirmRebind) {
        PortalConfirmDialog(
            title = "重新绑定",
            message = "将在门户上注册一个新的通行密钥，并替换本机现有凭据。\n\n" +
                "旧的通行密钥不会被自动删除，会作为一条记录留在门户的" +
                "「账户安全 → 通行密钥」列表里，需要的话请自行到那里清理。",
            confirmText = "继续",
            onDismiss = { confirmRebind = false },
            onConfirm = {
                confirmRebind = false
                onOpenBind()
            },
        )
    }
    if (confirmExport) {
        PortalConfirmDialog(
            title = "导出通行密钥",
            message = "导出内容包含本机私钥，任何拿到它并猜中六位 PIN 的人都能以你的身份登录统一门户。\n\n" +
                "请只通过可信渠道传到自己的另一台设备，不要发到群聊、云盘或公共场所；传完就删除。",
            confirmText = "我知道了",
            onDismiss = { confirmExport = false },
            onConfirm = {
                confirmExport = false
                onOpenExport()
            },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("统一门户", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surfaceContainer,
                    titleContentColor = scheme.onBackground,
                    scrolledContainerColor = scheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PortalStatusSection(
                    bundle = state.bound,
                    revokedMessage = state.revokedMessage,
                )
            }
            if (state.bound != null) {
                item {
                    PortalInfoSection(
                        bundle = state.bound!!,
                        bindingTime = state.bindingTime,
                        name = state.studentName,
                        org = state.studentOrg,
                        loading = state.studentLoading,
                        error = state.studentError,
                    )
                }
            }
            if (state.bound == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onOpenBind, modifier = Modifier.fillMaxWidth()) {
                            Text("立即绑定")
                        }
                        OutlinedButton(onClick = onOpenImport, modifier = Modifier.fillMaxWidth()) {
                            Text("从其他设备导入")
                        }
                    }
                }
            } else {
                item {
                    PortalActionsSection(
                        onRebind = { confirmRebind = true },
                        onExport = { confirmExport = true },
                        onImport = onOpenImport,
                        onUnbind = { confirmUnbind = true },
                    )
                }
                item {
                    PortalDebugSection(
                        testing = state.testing,
                        stage = state.testStage,
                        result = state.testResult,
                        onTest = viewModel::testLogin,
                    )
                }
            }
            item {
                Text(
                    "提示：凭据是通行密钥（WebAuthn passkey），不含密码；" +
                        "本机只保存加密后的副本，所有校级服务都靠它无感登录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

/** 状态分组：未绑定 / 已绑定 / 已失效三态。 */
@Composable
private fun PortalStatusSection(bundle: PortalStatus?, revokedMessage: String?) {
    val scheme = MaterialTheme.colorScheme
    val unbound = bundle == null
    val revoked = !unbound && revokedMessage != null
    val accent = when {
        unbound -> scheme.onSurfaceVariant
        revoked -> scheme.error
        else -> scheme.primary
    }
    val icon = when {
        unbound -> "link_off"
        revoked -> "error"
        else -> "verified_user"
    }
    val title = when {
        unbound -> "未绑定"
        revoked -> "通行密钥已失效"
        else -> "已绑定"
    }
    val subtitle = when {
        unbound -> "绑定后即可在 APP 内直接使用教务、电费等需要门户身份的功能。"
        revoked -> revokedMessage ?: "门户拒绝了本机凭据，请重新绑定。"
        else -> "本机已保存通行密钥，APP 内需要门户身份时会自动登录。"
    }

    SegmentedColumn {
        item {
            BaseWidget(
                icon = icon,
                iconColor = accent,
                title = title,
                titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                description = subtitle,
                isError = revoked,
            )
        }
    }
}

/** 凭据信息分组（学号 / 凭据名称 / 绑定时间），行式呈现。 */
@Composable
private fun PortalInfoSection(
    bundle: PortalStatus,
    bindingTime: String,
    name: String,
    org: String,
    loading: Boolean,
    error: String?,
) {
    SegmentedColumn {
        item {
            PortalInfoRow("姓名", name.ifEmpty { if (loading) "正在读取…" else "未记录" })
        }
        item {
            PortalInfoRow("学号", bundle.studentId ?: "未知")
        }
        item {
            PortalInfoRow(
                "院系班级",
                org.ifEmpty { if (loading) "正在读取…" else (error ?: "未记录") },
            )
        }
        item {
            PortalInfoRow("凭据名称", bundle.deviceName.ifEmpty { "未记录" })
        }
        item {
            PortalInfoRow("绑定时间", bindingTime)
        }
    }
}

@Composable
private fun PortalInfoRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    val shape = dev.duohuo.nuist.sta.compose.ui.kit.LocalSegmentedItemShape.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceBright)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 操作分组：重新绑定 / 导出 / 导入 / 解除绑定。 */
@Composable
private fun PortalActionsSection(
    onRebind: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onUnbind: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    SegmentedColumn {
        item {
            BaseWidget(
                icon = "refresh",
                title = "重新绑定",
                description = "注册新的通行密钥",
                onClick = onRebind,
                trailingContent = { Chevron() },
            )
        }
        item {
            BaseWidget(
                icon = "ios_share",
                title = "导出通行密钥",
                description = "加密后传到另一台设备",
                onClick = onExport,
                trailingContent = { Chevron() },
            )
        }
        item {
            BaseWidget(
                icon = "download",
                title = "导入通行密钥",
                description = "用其他设备导出的数据替换本机凭据",
                onClick = onImport,
                trailingContent = { Chevron() },
            )
        }
        item {
            BaseWidget(
                icon = "link_off",
                title = "解除绑定",
                description = "删除本机凭据",
                onClick = onUnbind,
                descriptionColor = scheme.error,
                trailingContent = { Chevron() },
            )
        }
    }
}

/** 调试分组：测试登录 + 结果（对应原项目 `_debugSection`）。 */
@Composable
private fun PortalDebugSection(
    testing: Boolean,
    stage: String?,
    result: PortalTestResult?,
    onTest: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "调试",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onTest,
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
        ) {
            if (testing) {
                InlineLoadingIndicator(size = 16.dp, color = scheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stage ?: "登录中…")
            } else {
                SymbolIcon("login", fontSize = 18.sp, tint = scheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("测试登录")
            }
        }
        result?.let { report ->
            SegmentedColumn {
                item {
                    BaseWidget(
                        icon = if (report.ok) "check_circle" else "error",
                        title = report.title,
                        description = report.details.joinToString("\n").ifBlank { null },
                        isError = !report.ok,
                    )
                }
            }
        }
    }
}

@Composable
private fun Chevron() {
    val scheme = MaterialTheme.colorScheme
    SymbolIcon(
        "chevron_right",
        fontSize = 20.sp,
        tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

@Composable
private fun PortalConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (destructive) scheme.error else scheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}