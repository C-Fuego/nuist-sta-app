// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.notification

import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * Dart 侧提醒通道的原生处理器。
 *
 * 通道与 Dart 的 `NextDayReminderChannel` 对齐：`update` 是明日提醒配置，
 * `updatePlan` 是未来几天的上课提醒整表；两者都只落盘并重排，不做网络请求。
 */
class TimetableReminderChannel(
    private val scheduler: TimetableNotificationScheduler,
) {

    fun register(engine: FlutterEngine) {
        MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "update" -> {
                    scheduler.applyNextDay(call.arguments as? Map<*, *>)
                    result.success(diagnostics())
                }

                "updatePlan" -> {
                    val plan = (call.arguments as? Map<*, *>)?.get("plan") as? List<*>
                    scheduler.applyPlanFromChannel(plan.orEmpty())
                    result.success(diagnostics())
                }

                // Dart 设置页目前不直接调这个；保留给后续入口查询当前授权状态。
                "requestPermission" -> result.success(scheduler.notificationsAllowed())

                else -> result.notImplemented()
            }
        }
    }

    private fun diagnostics(): Map<String, Any?> = mapOf("diagnostics" to emptyList<Any?>())

    private companion object {
        const val CHANNEL = "nuist/timetable_next_day"
    }
}