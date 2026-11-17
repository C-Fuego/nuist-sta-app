package dev.duohuo.nuist.sta.compose.core.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前页面的"返回"动作。
 *
 * 被当作小程序从首页宫格打开时由 AppShell 提供；作为 Tab 页（首页/学习/我的）渲染时为 null，
 * 此时页面不显示返回按钮。
 */
val LocalAppBack = staticCompositionLocalOf<(() -> Unit)?> { null }
