package dev.duohuo.nuist.sta.compose.features.campus_map

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

@Preview(name = "校园地图", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun CampusMapPreview() {
    Md3eTheme(dark = true) { Surface { CampusMapScreen() } }
}
