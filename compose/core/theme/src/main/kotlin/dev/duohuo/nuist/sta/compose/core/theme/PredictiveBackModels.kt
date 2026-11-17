package dev.duohuo.nuist.sta.compose.core.theme

/**
 * 预测性返回动画类型（借鉴 InstallerX，去掉 MiuiX 一档）。
 * - [AOSP]：系统 AOSP 风格——页面跟随手势退场（位移 + 圆角/缩放 + 淡出）。
 * - [Scale]：缩放退场（缩小 + 淡出）。
 * - [Classic]：经典缩放 + 背景遮罩淡入。
 * - [None]：不做预测动画，仅标准返回。
 */
enum class PredictiveBackAnimation(val label: String) {
    AOSP("AOSP 跟随"),
    Scale("缩放"),
    Classic("经典缩放"),
    None("无动画"),
}

/** 页面退场方向（借鉴 InstallerX）。 */
enum class PredictiveBackExitDirection(val label: String) {
    /** 跟随手势方向（左滑 → 向右退出）。 */
    FOLLOW_GESTURE("跟随手势"),

    /** 始终向右退出。 */
    ALWAYS_RIGHT("始终向右"),

    /** 始终向左退出。 */
    ALWAYS_LEFT("始终向左"),
}
