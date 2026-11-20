package dev.duohuo.nuist.sta.compose.ui.kit

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前页面若以「二级页」形式打开，这里提供返回回调；一级 tab 页为 null。
 * 供 [MiniAppBackButton] 决定是否渲染返回按钮。
 */
val LocalAppBack = staticCompositionLocalOf<(() -> Unit)?> { null }
