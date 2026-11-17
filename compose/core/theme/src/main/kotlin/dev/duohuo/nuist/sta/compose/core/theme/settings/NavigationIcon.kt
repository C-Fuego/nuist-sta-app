package dev.duohuo.nuist.sta.compose.core.theme.settings

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveBackButton(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

/**
 * 小程序页的返回按钮：只在被当作小程序打开（[dev.duohuo.nuist.sta.compose.core.theme.LocalAppBack]
 * 不为 null）时显示，作为 TopAppBar 的 `navigationIcon` 使用。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniAppBackButton(
    onBack: (() -> Unit)? = dev.duohuo.nuist.sta.compose.core.theme.LocalAppBack.current,
) {
    if (onBack == null) return
    androidx.compose.foundation.layout.Row {
        ExpressiveBackButton { onBack() }
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.size(16.dp))
    }
}
