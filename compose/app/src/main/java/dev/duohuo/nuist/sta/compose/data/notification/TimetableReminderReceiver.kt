// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber

/**
 * 闹钟到点后的唯一入口。
 *
 * 发通知本身很快，但「滚动 7 天」要重新拉起 Dart 引擎，所以用 goAsync 把工作挪到
 * 后台协程，避免在主线程阻塞接收器。
 */
class TimetableReminderReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = get<TimetableNotificationScheduler>()
        when (intent.action) {
            TimetableNotificationScheduler.ACTION_CLASS -> {
                scheduler.notifyClass(intent.getIntExtra(TimetableNotificationScheduler.EXTRA_INDEX, -1))
                refreshSoon()
            }

            TimetableNotificationScheduler.ACTION_NEXT_DAY -> {
                scheduler.notifyNextDay(intent.getStringExtra(TimetableNotificationScheduler.EXTRA_DATE_KEY).orEmpty())
                // 缓存里还有未来的日期，先排下一条，不依赖这次刷新是否成功。
                scheduler.scheduleNextDayFromCache()
                refreshSoon()
            }

            TimetableNotificationScheduler.ACTION_REFRESH -> refreshSoon()
        }
    }

    private fun refreshSoon() {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                get<TimetableReminderSync>().refreshPlan()
            } catch (error: Throwable) {
                // 拉计划失败不能让前瞻断掉：六小时后再试，旧计划继续保持不动。
                Timber.w(error, "课表提醒计划刷新失败")
                get<TimetableNotificationScheduler>().scheduleRefreshRetry()
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }

}