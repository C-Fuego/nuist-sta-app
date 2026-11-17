package dev.duohuo.nuist.sta.compose.core.theme

/**
 * 预测性返回动画类型（与 InstallerX 完全一致的五档，代码原样移植）。
 * - [AOSP]：系统 Cross-Activity 风格——缩放 + 贴边 + 淡出。
 * - [Scale]：缩放到 0.85 并以滑动侧为轴心滑出。
 * - [Classic]：经典缩放过渡（缩放 + 遮罩）。
 * - [MIUIX]：Miuix 默认转场。
 * - [None]：不做预测动画，仅标准返回。
 */
enum class PredictiveBackAnimation(val label: String) {
    AOSP("AOSP 跟随"),
    Scale("缩放"),
    Classic("经典缩放"),
    MIUIX("Miuix"),
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
