package dev.duohuo.nuist.sta.compose.features.score

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

private val mockState = ScoreUiState()

@Composable
private fun PreviewContent() {
    ScoreContent(state = mockState, onSelectTerm = {}, onRefresh = {})
}

@Preview(name = "成绩 · 浅色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun ScoreLightPreview() {
    Md3eTheme(dark = false) { Surface { PreviewContent() } }
}

@Preview(name = "成绩 · 深色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun ScoreDarkPreview() {
    Md3eTheme(dark = true) { Surface { PreviewContent() } }
}
