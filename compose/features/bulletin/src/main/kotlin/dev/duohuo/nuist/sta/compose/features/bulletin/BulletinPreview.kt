package dev.duohuo.nuist.sta.compose.features.bulletin

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

@Preview(name = "公告列表", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun BulletinListPreview() {
    Md3eTheme(dark = true) { Surface { BulletinListScreen() } }
}

@Preview(name = "公告详情", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun BulletinDetailPreview() {
    Md3eTheme(dark = true) {
        Surface { BulletinDetailScreen(BulletinMock.detail(BulletinMock.items.first()), onBack = {}) }
    }
}
