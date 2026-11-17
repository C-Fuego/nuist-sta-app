package dev.duohuo.nuist.sta.compose.features.electricity

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

private val mockState = ElectricityUiState()

@Composable
private fun PreviewContent() {
    ElectricityContent(
        state = mockState,
        history = ElecMock.history(mockState.rangeDays),
        onTogglePicker = {},
        onRange = {},
    )
}

@Preview(name = "电费 · 浅色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun ElectricityLightPreview() {
    Md3eTheme(dark = false) { Surface { PreviewContent() } }
}

@Preview(name = "电费 · 深色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun ElectricityDarkPreview() {
    Md3eTheme(dark = true) { Surface { PreviewContent() } }
}
