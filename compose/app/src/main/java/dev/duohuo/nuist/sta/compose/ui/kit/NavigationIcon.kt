package dev.duohuo.nuist.sta.compose.ui.kit

import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveBackButton(
    modifier: Modifier = Modifier,
    icon: String = "arrow_back",
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String = "返回",
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes(shape = CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        SymbolIcon(name = icon, contentDescription = contentDescription)
    }
}

/**
 * 小程序页的返回按钮：只在被当作小程序打开（[dev.duohuo.nuist.sta.compose.ui.kit.LocalAppBack]
 * 不为 null）时显示，作为 TopAppBar 的 `navigationIcon` 使用。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniAppBackButton(
    onBack: (() -> Unit)? = dev.duohuo.nuist.sta.compose.ui.kit.LocalAppBack.current,
) {
    if (onBack == null) return
    // 圆形按钮的左边与页面里分段标题（32.dp 缩进）对齐，视觉上收在标题列上。
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier.padding(start = 16.dp),
    ) {
        ExpressiveBackButton { onBack() }
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.size(16.dp))
    }
}
