// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.notification

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge

/**
 * 从 Dart 侧拉取未来 7 天的上课提醒并整表重排。
 *
 * 触发时机只有「闹钟响过」和「手动刷新」，日常路径由 Dart 在同步、改设置后主动推送，
 * 两边共用同一份 plan，避免各自计算导致文案或时间不一致。
 */
class TimetableReminderSync(
    private val bridge: CampusBridge,
    private val scheduler: TimetableNotificationScheduler,
) {

    suspend fun refreshPlan() {
        val data = bridge.call("timetable.notifyPlan", mapOf("horizonDays" to HORIZON_DAYS))
        scheduler.applyPlanFromBridge(data)
    }

    private companion object {
        const val HORIZON_DAYS = 7
    }
}