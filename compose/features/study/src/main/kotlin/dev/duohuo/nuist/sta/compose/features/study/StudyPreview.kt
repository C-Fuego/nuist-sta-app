package dev.duohuo.nuist.sta.compose.features.study

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

@Composable
private fun PreviewContent() { StudyScreen() }

@Preview(name = "学习 · 浅色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun StudyLightPreview() {
    Md3eTheme(dark = false) { Surface { PreviewContent() } }
}

@Preview(name = "学习 · 深色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun StudyDarkPreview() {
    Md3eTheme(dark = true) { Surface { PreviewContent() } }
}
