// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.bridge

import android.content.Context
import dev.duohuo.nuist.sta.compose.data.notification.TimetableReminderChannel
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import timber.log.Timber
import kotlin.coroutines.resume

/** 通道调用结果。成功时 [data] 为返回体，失败时 [error] 带 `type` / `message` 等诊断字段。 */
class BridgeReply(
    val ok: Boolean,
    val data: JsonObject?,
    val error: JsonObject?,
) {
    fun errorMessage(): String {
        val error = error ?: return "Dart 核心调用失败"
        val message = error["message"]?.let { (it as? JsonPrimitive)?.content }?.takeIf { it.isNotBlank() }
        val type = error["type"]?.let { (it as? JsonPrimitive)?.content }?.takeIf { it.isNotBlank() }
        return when {
            message != null && type != null -> "$message（$type）"
            message != null -> message
            type != null -> type
            else -> "Dart 核心调用失败"
        }
    }
}

/**
 * 校园网 Dart 核心的进程内桥。
 *
 * 引擎无界面：不创建 FlutterView、不渲染 widget、不注册 Flutter 插件之外的任何 UI，
 * 只用来跑 AOT 编译好的 Dart 代码。协议实现（CAS、passkey、隧道、各接口解析）
 * 全部在 Dart 侧，这里只是把方法名字符串转发过去。
 */
class DartBridge(
    context: Context,
    private val reminders: TimetableReminderChannel,
) {

    private val channel: MethodChannel

    init {
        FlutterInjector.instance().flutterLoader().startInitialization(context)
        val engine = FlutterEngine(context)
        // 先注册提醒通道再启动 Dart：Dart 启动后会立刻推送一次明日提醒与上课计划，
        // 处理器晚一步就收不到这次推送。
        reminders.register(engine)
        engine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault())
        channel = MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
    }

    /**
     * 调用 Dart 侧方法。方法表见 `dart_bridge/lib/bridge/router.dart`。
     *
     * 引擎是异步启动的：`executeDartEntrypoint` 返回时 Dart 侧的通道处理器未必已经注册，
     * 这时平台侧会收到 `notImplemented`。这里按小步长重试，等处理器就绪后再判失败。
     */
    suspend fun call(method: String, args: Map<String, Any?> = emptyMap()): BridgeReply {
        var reply = invokeOnce(method, args)
        var waited = 0L
        while (isNotImplemented(reply) && waited < READY_TIMEOUT_MS) {
            delay(READY_STEP_MS)
            waited += READY_STEP_MS
            reply = invokeOnce(method, args)
        }
        return reply
    }

    private fun isNotImplemented(reply: BridgeReply): Boolean =
        (reply.error?.get("type") as? JsonPrimitive)?.content == "not_implemented"

    private suspend fun invokeOnce(method: String, args: Map<String, Any?>): BridgeReply {
        val raw = try {
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Any?> { continuation ->
                    channel.invokeMethod(
                        method,
                        args,
                        object : MethodChannel.Result {
                            override fun success(result: Any?) {
                                if (continuation.isActive) continuation.resume(result)
                            }

                            override fun error(code: String, message: String?, details: Any?) {
                                if (!continuation.isActive) return
                                continuation.resume(
                                    mapOf(
                                        "ok" to false,
                                        "error" to mapOf(
                                            "type" to code,
                                            "message" to (message ?: "Dart 通道调用失败"),
                                        ),
                                    ),
                                )
                            }

                            override fun notImplemented() {
                                if (!continuation.isActive) return
                                continuation.resume(
                                    mapOf(
                                        "ok" to false,
                                        "error" to mapOf(
                                            "type" to "not_implemented",
                                            "message" to "Dart 侧没有实现 $method",
                                        ),
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        } catch (error: Throwable) {
            Timber.w(error, "bridge %s 调用失败", method)
            return BridgeReply(
                ok = false,
                data = null,
                error = JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("channel_error"),
                        "message" to JsonPrimitive("${error.javaClass.simpleName}: ${error.message}"),
                    ),
                ),
            )
        }

        val root = toJson(raw) as? JsonObject
            ?: return BridgeReply(ok = false, data = null, error = null)
        val ok = (root["ok"] as? JsonPrimitive)?.booleanOrNull ?: false
        return BridgeReply(
            ok = ok,
            data = (root["data"] as? JsonObject) ?: if (ok) JsonObject(emptyMap()) else null,
            error = (root["error"] as? JsonObject),
        )
    }

    private fun toJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Map<*, *> -> JsonObject(value.entries.associate { (key, item) -> key.toString() to toJson(item) })
        is Iterable<*> -> JsonArray(value.map { toJson(it) })
        is Array<*> -> JsonArray(value.map { toJson(it) })
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value.toDouble())
        is String -> JsonPrimitive(value)
        is Char -> JsonPrimitive(value.toString())
        else -> JsonPrimitive(value.toString())
    }

    private companion object {
        const val CHANNEL = "nuist.bridge"
        const val READY_STEP_MS = 200L
        const val READY_TIMEOUT_MS = 5_000L
    }
}
