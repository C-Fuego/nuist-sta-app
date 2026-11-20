package dev.duohuo.nuist.sta.compose.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BaseItemContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val baseShape = LocalSegmentedItemShape.current
    val backgroundColor = MaterialTheme.colorScheme.surfaceBright
    Surface(
        modifier = modifier.fillMaxWidth().let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        },
        color = backgroundColor,
        shape = baseShape,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}
