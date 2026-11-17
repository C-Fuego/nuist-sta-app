package dev.duohuo.nuist.sta.compose.features.credits

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme

@Preview(name = "双创学分", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun InnovationPreview() {
    Md3eTheme(dark = true) { Surface { InnovationCreditScreen() } }
}

@Preview(name = "劳动积分", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun LaborPreview() {
    Md3eTheme(dark = true) { Surface { LaborScoreScreen() } }
}
