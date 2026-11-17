package dev.duohuo.nuist.sta.compose.features.timetable

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

@Preview(name = "课表", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun TimetablePreview() {
    Md3eTheme(dark = true) { Surface { TimetableScreen() } }
}
