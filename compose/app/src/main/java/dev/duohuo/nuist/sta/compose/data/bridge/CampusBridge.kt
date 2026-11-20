// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.bridge

import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.IOException

/**
 * 校园网能力的统一入口：所有校园接口都经由 Dart 核心，前端只负责取数据与渲染。
 *
 * 方法表在 `dart_bridge/lib/bridge/router.dart`；这里只做参数装箱与错误归类。
 */
class CampusBridge(private val bridge: DartBridge) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ping(): Boolean = bridge.call("ping").ok

    suspend fun campus(): JsonObject? = bridge.call("campus").dataOrThrow()

    /** 把 WebView 绑定页产出的凭据 JSON 交给 Dart 侧保存。 */
    suspend fun bindCredential(bundle: Map<String, Any?>) {
        bridge.call("portal.bind", mapOf("bundle" to bundle)).dataOrThrow()
    }

    /** 当前绑定状态；未绑定时 [PortalStatus.bound] 为 false。 */
    suspend fun portalStatus(): PortalStatus {
        val data = bridge.call("portal.status").dataOrThrow()
        val bundle = data["bundle"]?.let { if (it is JsonObject) it else null }
        return PortalStatus(
            bound = data["bound"]?.jsonPrimitive?.booleanOrNull == true,
            studentId = data["studentId"]?.jsonPrimitive?.contentOrNull,
            maskedStudentId = data["maskedStudentId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            deviceName = bundle?.get("deviceName")?.jsonPrimitive?.contentOrNull.orEmpty(),
            createdAt = bundle?.get("createdAt")?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }

    /** 预览导入内容（不解密到本机存储）。 */
    suspend fun previewCredential(text: String, pin: String): PortalPreview {
        val data = bridge.call(
            "portal.preview",
            mapOf("text" to text, "pin" to pin),
        ).dataOrThrow()
        val ok = data["ok"]?.jsonPrimitive?.booleanOrNull == true
        return PortalPreview(
            ok = ok,
            reason = data["reason"]?.jsonPrimitive?.contentOrNull,
            maskedStudentId = data["maskedStudentId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            deviceName = data["deviceName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            createdAt = data["createdAt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            existing = data["existing"]?.jsonPrimitive?.booleanOrNull == true,
        )
    }

    /** 导入凭据；失败时返回原因（`wrong_format` / `bad_pin` 等）。 */
    suspend fun importCredential(text: String, pin: String): String? {
        val data = bridge.call(
            "portal.import",
            mapOf("text" to text, "pin" to pin),
        ).dataOrThrow()
        val ok = data["ok"]?.jsonPrimitive?.booleanOrNull == true
        return if (ok) null else data["reason"]?.jsonPrimitive?.contentOrNull ?: "unknown"
    }

    /** 加密导出当前凭据文本。 */
    suspend fun exportCredential(pin: String): String? {
        val data = bridge.call("portal.export", mapOf("pin" to pin)).dataOrThrow()
        val ok = data["ok"]?.jsonPrimitive?.booleanOrNull == true
        return if (ok) data["text"]?.jsonPrimitive?.contentOrNull else null
    }

    /** 预连接隧道（校园网接口在首次请求时也会自己准备，这里用于设置页手动触发）。 */
    suspend fun vpnPrepare(): Int? =
        bridge.call("vpn.prepare").dataOrThrow()["sessionId"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

    suspend fun vpnReset() {
        bridge.call("vpn.reset").dataOrThrow()
    }

    /** 隧道状态；未建立时返回 null。 */
    suspend fun vpnStatus(): JsonObject? =
        bridge.call("vpn.status").dataOrThrow()["status"]?.let { if (it is JsonObject) it else null }

    suspend fun forgetCredential() {
        bridge.call("portal.forget").dataOrThrow()
    }

    /** 强制重新登录一次，返回落地页 URL。 */
    suspend fun verifyCredential(): String {
        val data = bridge.call("portal.verify").dataOrThrow()
        return data["landing"]?.jsonPrimitive?.content ?: ""
    }

    suspend fun call(method: String, args: Map<String, Any?> = emptyMap()): JsonObject =
        bridge.call(method, args).dataOrThrow()

    /** 把通道里的 `{type, message}` 转成前端统一的门户异常。 */
    private fun BridgeReply.dataOrThrow(): JsonObject {
        if (ok) return data ?: JsonObject(emptyMap())
        val type = error?.get("type")?.jsonPrimitive?.content.orEmpty()
        val message = error?.get("message")?.jsonPrimitive?.content.orEmpty()
        Timber.w("bridge 调用失败 type=%s message=%s", type, message)
        val readable = message.ifBlank { "Dart 核心没有返回结果" }
        throw when {
            type.contains("Network", ignoreCase = true) ||
                type.contains("Timeout", ignoreCase = true) ||
                type.contains("Socket", ignoreCase = true) ->
                PortalException.Network(readable)

            type.contains("Credential", ignoreCase = true) ||
                type.contains("Unauthorized", ignoreCase = true) ->
                PortalException.Credential(readable)

            type.contains("Format", ignoreCase = true) ||
                type.contains("Parse", ignoreCase = true) ->
                IOException(readable)

            else -> PortalException.Login(readable)
        }
    }
}

/** Dart 侧的 JSON 转成通道可传的普通 Kotlin 结构。 */
internal fun JsonElement.toChannelValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonObject -> entries.associate { (key, value) -> key to value.toChannelValue() }
    is JsonArray -> map { it.toChannelValue() }
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: doubleOrNull ?: content
    else -> toString()
}

/** 通道参数里嵌套的 JSON 文本（例如 querySetting）直接当字符串传。 */
internal fun JsonElement.toJsonText(): String = jsonText(this)

private fun jsonText(element: JsonElement): String = element.toString()

/** 「统一门户」页需要的绑定信息，全部来自 Dart 侧凭据库。 */
data class PortalStatus(
    val bound: Boolean,
    val studentId: String?,
    val maskedStudentId: String,
    val deviceName: String,
    val createdAt: String,
)

data class PortalPreview(
    val ok: Boolean,
    val reason: String?,
    val maskedStudentId: String,
    val deviceName: String,
    val createdAt: String,
    val existing: Boolean,
)
