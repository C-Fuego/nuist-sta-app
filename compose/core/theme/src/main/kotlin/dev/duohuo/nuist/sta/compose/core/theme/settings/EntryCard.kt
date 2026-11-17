package dev.duohuo.nuist.sta.compose.core.theme.settings

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon

/**
 * 全局统一的并排入口卡（首页「校园导览」与学习页快捷入口共用）。
 *
 * 高 72dp、surfaceBright 底、大圆角、图标 primary 24dp、文字 labelLarge，
 * 保证两处入口视觉完全一致。图标为 Material Symbols 名称。
 */
@Composable
fun EntryCard(
    symbol: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceBright)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SymbolIcon(
            name = symbol,
            tint = scheme.primary,
            modifier = Modifier.size(24.dp),
            fontSize = 24.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface,
        )
    }
}
