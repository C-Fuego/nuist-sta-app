package dev.duohuo.nuist.sta.compose.core.storage

import android.content.Context

/** 主题设置快照（开机恢复用）。种子色为 ARGB int，避免本模块依赖 Compose。 */
data class ThemePrefsSnapshot(
    val seedArgb: Int,
    val paletteStyleName: String,
    val colorSpecName: String,
    val dynamicColor: Boolean,
    val darkModeIndex: Int,
    val useAmoled: Boolean,
    val bottomBarFloating: Boolean,
    val pbAnimName: String,
    val pbDirName: String,
)

/**
 * 主题设置本地存储。
 *
 * 初开发阶段用框架自带的 [SharedPreferences]（不走 DataStore，避免为几十个
 * 布尔/整型引入新依赖）：每次改动整包覆写，量小且低频，主线程读写可接受。
 */
object ThemePrefs {

    private const val FILE = "theme_prefs"

    fun load(
        context: Context,
        defaults: ThemePrefsSnapshot,
    ): ThemePrefsSnapshot {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return ThemePrefsSnapshot(
            seedArgb = sp.getInt("seed_color", defaults.seedArgb),
            paletteStyleName = sp.getString("palette_style", defaults.paletteStyleName)
                ?: defaults.paletteStyleName,
            colorSpecName = sp.getString("color_spec", defaults.colorSpecName)
                ?: defaults.colorSpecName,
            dynamicColor = sp.getBoolean("dynamic_color", defaults.dynamicColor),
            darkModeIndex = sp.getInt("dark_mode_index", defaults.darkModeIndex),
            useAmoled = sp.getBoolean("use_amoled", defaults.useAmoled),
            bottomBarFloating = sp.getBoolean("bottom_bar_floating", defaults.bottomBarFloating),
            pbAnimName = sp.getString("pb_anim", defaults.pbAnimName) ?: defaults.pbAnimName,
            pbDirName = sp.getString("pb_dir", defaults.pbDirName) ?: defaults.pbDirName,
        )
    }

    fun save(
        context: Context,
        seedArgb: Int,
        paletteStyleName: String,
        colorSpecName: String,
        dynamicColor: Boolean,
        darkModeIndex: Int,
        useAmoled: Boolean,
        bottomBarFloating: Boolean,
        pbAnimName: String,
        pbDirName: String,
    ) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt("seed_color", seedArgb)
            .putString("palette_style", paletteStyleName)
            .putString("color_spec", colorSpecName)
            .putBoolean("dynamic_color", dynamicColor)
            .putInt("dark_mode_index", darkModeIndex)
            .putBoolean("use_amoled", useAmoled)
            .putBoolean("bottom_bar_floating", bottomBarFloating)
            .putString("pb_anim", pbAnimName)
            .putString("pb_dir", pbDirName)
            .apply()
    }
}
