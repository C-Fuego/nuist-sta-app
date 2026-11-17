package dev.duohuo.nuist.sta.compose.core.theme.material


/** 调色板样式（原样取自 参考实现 PaletteStyle，去掉 MiuiX 无关项，保留 CMF）。 */
enum class PaletteStyle(val displayName: String) {
    TonalSpot("Tonal Spot"),
    Neutral("Neutral"),
    Vibrant("Vibrant"),
    Expressive("Expressive"),
    Rainbow("Rainbow"),
    FruitSalad("FruitSalad"),
    Monochrome("Monochrome"),
    Fidelity("Fidelity"),
    Content("Content"),
    Cmf("CMF"),
    ;

    val supportedSpecs: List<ThemeColorSpec>
        get() = when (this) {
            TonalSpot, Neutral, Vibrant, Expressive -> listOf(ThemeColorSpec.SPEC_2021, ThemeColorSpec.SPEC_2025)
            Cmf -> listOf(ThemeColorSpec.SPEC_2026)
            else -> listOf(ThemeColorSpec.SPEC_2021)
        }

    fun effectiveSpec(preferredSpec: ThemeColorSpec): ThemeColorSpec =
        preferredSpec.takeIf { it in supportedSpecs } ?: supportedSpecs.last()

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: TonalSpot
    }
}

/** 颜色规格（原样取自 参考实现 ThemeColorSpec）。 */
enum class ThemeColorSpec(val displayName: String) {
    SPEC_2021("Material 3 (2021)"),
    SPEC_2025("Expressive (2025)"),
    SPEC_2026("Spec 2026"),
    ;

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: SPEC_2025
    }
}
