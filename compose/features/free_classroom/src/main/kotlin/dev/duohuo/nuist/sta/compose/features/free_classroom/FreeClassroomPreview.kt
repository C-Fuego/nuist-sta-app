package dev.duohuo.nuist.sta.compose.features.free_classroom

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

private val mockState = FreeClassroomUiState(
    day = FreeClassroomMock.day(FreeClassroomMock.buildings.first()),
)

@Composable
private fun PreviewContent() {
    FreeClassroomContent(
        state = mockState,
        onRefresh = {},
        onSelectBuilding = {},
        onToggleGroup = {},
        onFloor = {},
        onZone = {},
    )
}

@Preview(name = "空教室 · 浅色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun FreeClassroomLightPreview() {
    Md3eTheme(dark = false) { Surface { PreviewContent() } }
}

@Preview(name = "空教室 · 深色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun FreeClassroomDarkPreview() {
    Md3eTheme(dark = true) { Surface { PreviewContent() } }
}
