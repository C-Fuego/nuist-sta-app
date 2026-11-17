package dev.duohuo.nuist.sta.compose.core.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import dev.duohuo.nuist.sta.compose.core.theme.material.PaletteStyle
import dev.duohuo.nuist.sta.compose.core.theme.material.PresetColors
import dev.duohuo.nuist.sta.compose.core.theme.material.ThemeColorSpec
import dev.duohuo.nuist.sta.compose.core.theme.material.animateAsState
import dev.duohuo.nuist.sta.compose.core.theme.material.dynamicColorScheme

// 照搬参考实现`ui/theme/InstallerTheme.kt` 的 Material 路径（去掉 MiuiX 分支）。
private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalPaletteStyle = staticCompositionLocalOf { PaletteStyle.TonalSpot }
private val LocalThemeColorSpec = staticCompositionLocalOf { ThemeColorSpec.SPEC_2025 }
private val LocalSeedColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalUseDynamicColor = staticCompositionLocalOf { false }

val LocalNuistColorScheme = staticCompositionLocalOf<ColorScheme> {
    error("No ColorScheme provided")
}

/** 与 参考实现的 `InstallerTheme` 对象等价的主题读取入口。 */
object NuistTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalNuistColorScheme.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDark.current

    val seedColor: Color
        @Composable @ReadOnlyComposable
        get() = LocalSeedColor.current

    val paletteStyle: PaletteStyle
        @Composable @ReadOnlyComposable
        get() = LocalPaletteStyle.current

    val colorSpec: ThemeColorSpec
        @Composable @ReadOnlyComposable
        get() = LocalThemeColorSpec.current

    val useDynamicColor: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseDynamicColor.current
}

/**
 * NUIST++ 的 MD3E 主题 —— 照搬参考实现`InstallerTheme` 的 Material 路径：
 *
 * 1. `keyColor`：动态取色且 Android 12+ 时用系统壁纸强调色，否则用用户选的种子色；
 * 2. `dynamicColorScheme(keyColor, isDark, style, colorSpec)` 生成 base scheme；
 * 3. AMOLED 时把 background/surface/surfaceContainer 覆盖成纯黑；
 * 4. `animateAsState()` 把整套角色包成平滑过渡；
 * 5. 通过 CompositionLocal 下发 scheme / seedColor / 样式，最后交给 [MaterialExpressiveTheme]。
 */
@Composable
fun Md3eTheme(
    dark: Boolean = isSystemInDarkTheme(),
    seedColor: Color = PresetColors.first().color,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    dynamicColor: Boolean = false,
    useAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val keyColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(id = android.R.color.system_accent1_500)
    } else {
        seedColor
    }

    // 1. 配色方案计算较重（HCT 量化），放到后台线程；
    //    计算完成前沿用上一套 scheme，配合 animateAsState 平滑过渡，避免切换卡顿
    var baseColorScheme by remember {
        mutableStateOf(
            dynamicColorScheme(
                keyColor = keyColor,
                isDark = dark,
                style = paletteStyle,
                colorSpec = colorSpec,
            ),
        )
    }
    LaunchedEffect(keyColor, dark, paletteStyle, colorSpec) {
        val target = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            dynamicColorScheme(
                keyColor = keyColor,
                isDark = dark,
                style = paletteStyle,
                colorSpec = colorSpec,
            )
        }
        baseColorScheme = target
    }

    // 仅暗色 + AMOLED 时覆盖纯黑背景
    val colorScheme = if (useAmoled && dark) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
        )
    } else {
        baseColorScheme
    }

    // 2. Wrap it with smooth transitions
    val animatedColorScheme = colorScheme.animateAsState()

    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalPaletteStyle provides paletteStyle,
        LocalSeedColor provides seedColor,
        LocalNuistColorScheme provides animatedColorScheme,
        LocalUseDynamicColor provides dynamicColor,
        LocalThemeColorSpec provides colorSpec,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NavigationBarContrastHandler()
        }
        StatusBarAppearance(dark)

        // Applies Material 3 Expressive defaults including the expressive MotionScheme
        MaterialExpressiveTheme(
            colorScheme = animatedColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            content = content,
        )
    }
}

@Composable
private fun StatusBarAppearance(dark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? ComponentActivity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun NavigationBarContrastHandler() {
    val configuration = LocalConfiguration.current
    val view = LocalView.current

    DisposableEffect(configuration) {
        val window = (view.context as? ComponentActivity)?.window
        window?.isNavigationBarContrastEnforced = false
        onDispose { /** Keep empty as we want this behavior to persist **/ }
    }
}
