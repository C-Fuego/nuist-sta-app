// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.ThemeState
import dev.duohuo.nuist.sta.compose.domain.settings.provider.ThemeStateProvider
import dev.duohuo.nuist.sta.compose.ui.navigation.NuistNavContainer
import dev.duohuo.nuist.sta.compose.ui.theme.InstallerTheme
import dev.duohuo.nuist.sta.compose.ui.theme.LocalWindowLayoutInfo
import dev.duohuo.nuist.sta.compose.ui.theme.rememberWindowLayoutInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 应用唯一入口。
 *
 * 与参考实现同构：先等主题状态加载完（splash 保持），再套 [InstallerTheme]，
 * 最后交给 [NuistNavContainer] 处理路由；背景由这里的 Box 统一绘制。
 */
class MainActivity : ComponentActivity(), KoinComponent {

    private val themeStateProvider by inject<ThemeStateProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

        var isThemeLoaded = false
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)
        // 冷启动：通知点击启动活动时，Intent 上带着要打开的路由。
        // 带 savedInstanceState 说明活动是被重建的（导航栈已恢复），此时不再补跳一次。
        if (savedInstanceState == null) TimetableLaunchRoute.takeFrom(intent)
        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(
                initialValue = ThemeState(),
            )
            isThemeLoaded = uiState.isLoaded
            if (!isThemeLoaded) return@setContent

            val layoutInfo = rememberWindowLayoutInfo()
            CompositionLocalProvider(LocalWindowLayoutInfo provides layoutInfo) {
            InstallerTheme(
                themeMode = uiState.themeMode,
                paletteStyle = uiState.paletteStyle,
                colorSpec = uiState.colorSpec,
                useDynamicColor = uiState.useDynamicColor,
                seedColor = Color(uiState.seedColor),
                useAmoled = uiState.useAmoled,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    NuistNavContainer(uiState)
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 热启动：应用在后台时点通知，活动被复用到栈顶，只补一次跳转。
        TimetableLaunchRoute.takeFrom(intent)
    }
}
