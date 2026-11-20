// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import org.koin.androidx.compose.koinViewModel

/** 各品牌后台保活说明：只做引导，不检测机型。 */
private val brandTips = listOf(
    "小米 / 红米（HyperOS / MIUI）" to
        "设置 → 应用设置 → 应用管理 → 本应用：打开「自启动」；「省电策略」选「无限制」。",
    "华为 / 荣耀（HarmonyOS / MagicOS）" to
        "设置 → 电池 → 启动管理（应用启动管理）→ 本应用：关闭「自动管理」，在弹框中打开「允许自启动」「允许后台活动」。",
    "OPPO / 一加 / realme（ColorOS）" to
        "设置 → 应用 → 应用管理 → 本应用：打开「允许自启动」；「耗电管理」中打开「允许后台运行」。",
    "vivo / iQOO（OriginOS）" to
        "设置 → 电池 → 后台耗电管理 → 本应用选「允许后台高耗电」；i 管家 → 应用管理 → 自启动 中打开本应用。",
    "三星（One UI）" to
        "设置 → 电池 → 后台使用限制：确保本应用不在「休眠应用」「深度休眠应用」中；应用详情 → 电池 选「不受限制」。",
    "其他品牌" to "在系统设置中搜索「自启动」「后台运行」「电池优化」，对本应用选择允许 / 不优化。",
)

/** 提醒卡片里「注意」下面的四条说明。 */
private val warnings = listOf(
    "放假期间也会提醒",
    "在系统设置里「强行停止」App 后，提醒会失效，需要重新打开一次 App 才能恢复。",
    "提醒最多提前安排 7 天：超过 7 天不打开 App，之后可能不再提醒。",
    "清理后台（划掉最近任务）一般不影响提醒，但部分机型会连带停止，建议锁定本应用。",
)

/**
 * 后台运行设置：说明为什么需要放行，并直接跳到系统的电池优化设置。
 *
 * 从课表设置的「提醒」卡片进入；只做引导，不改任何提醒设置。
 */
@Composable
internal fun TimetableKeepAlivePage(
    onDismiss: () -> Unit,
    viewModel: TimetableKeepAliveViewModel = koinViewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    val ignoring by viewModel.ignoringBatteryOptimizations.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 用户去系统设置放行后回到页面，重新查一次让状态跟上。
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    TimetableDialogPage(title = "后台运行设置", onDismiss = onDismiss, snackbar = snackbar) {
        KeepAliveCard {
            Text(
                "部分手机会限制后台应用，导致上课提醒延迟或不弹出。建议允许本应用自启动，并关闭电池优化。",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (ignoring == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SymbolIcon(
                        name = "check_circle",
                        tint = scheme.primary,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "已允许后台运行，提醒不会被系统限制",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.primary,
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.openBatteryOptimizationSettings() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("关闭电池优化")
                }
            }
        }

        KeepAliveCard {
            Text(
                "注意",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            warnings.forEach { warning ->
                Text(
                    "· $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        brandTips.forEach { (brand, tip) ->
            KeepAliveCard {
                Text(
                    brand,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Text(
            "不同系统版本菜单名称可能略有不同。",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun KeepAliveCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}