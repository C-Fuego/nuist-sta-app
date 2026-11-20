// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.portal

/**
 * 门户认证的异常分类，与根项目 `lib/core/auth/portal_exceptions.dart` 对齐。
 *
 * - [Network]：连不上、超时、非 2xx 这类可重试的问题
 * - [Credential]：设备凭据被门户吊销/不认识（典型是「未查询到设备信息」）
 * - [Login]：协议层面失败（表单结构变了、登录后仍停在登录页）
 */
sealed class PortalException(
    message: String,
    val code: String,
    val stage: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class Network(
        message: String,
        code: String = "network_failed",
        stage: String? = null,
        cause: Throwable? = null,
    ) : PortalException(message, code, stage, cause)

    class Credential(
        message: String,
        code: String = "credential_invalid",
        stage: String? = null,
        cause: Throwable? = null,
    ) : PortalException(message, code, stage, cause)

    class Login(
        message: String,
        code: String = "login_failed",
        stage: String? = null,
        cause: Throwable? = null,
    ) : PortalException(message, code, stage, cause)
}

/**
 * 把任意异常转成可以直接显示给用户的一句中文。
 *
 * 全应用唯一的错误文案来源：网络类、凭据类、其它各页都走这里，避免同一类失败在不同页面
 * 显示成不同措辞。**原始异常文本（主机名、端口、栈）不上屏**，只写进 logcat，给后续的
 * 「诊断与日志」页收集。
 */
fun Throwable.toUserMessage(): String {
    timber.log.Timber.tag("Portal").w(this, "%s: %s", this::class.java.simpleName, message.orEmpty())
    return when (this) {
        is PortalException.Credential -> "需要登录统一门户"
        is PortalException.Network -> "网络不可用"
        is PortalException.Login -> "统一门户登录失败"
        is PortalException -> "服务暂时不可用"
        is java.io.IOException -> "网络不可用"
        else -> "服务暂时不可用"
    }
}
