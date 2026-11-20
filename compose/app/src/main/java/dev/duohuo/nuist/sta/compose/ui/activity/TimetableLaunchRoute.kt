// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.activity

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.data.notification.TimetableNotificationScheduler
import dev.duohuo.nuist.sta.compose.ui.navigation.LocalNavigator
import dev.duohuo.nuist.sta.compose.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 通知点击带进来的「待跳转路由」。
 *
 * 通知的 contentIntent 只附带一个路由字符串（见 `TimetableNotificationScheduler.post`），
 * 活动在 [MainActivity.onCreate] 与 [MainActivity.onNewIntent] 里把它搬到内存并立刻抹掉
 * Intent 上的 extra，再由 [TimetableLaunchRouteEffect] 在导航容器内部推成课表页：
 * 冷启动（活动新建）与热启动（活动已存在、收到新的 Intent）走的是同一条路。
 */
object TimetableLaunchRoute {

    private val _pending = MutableStateFlow<String?>(null)

    /** 尚未消费的路由；为 null 表示没有等待处理的跳转。 */
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /**
     * 把 Intent 上的一次性路由搬进内存。
     *
     * 读走之后立刻 `removeExtra`：同一份 Intent 被活动重建（例如进程被回收后再回到前台）
     * 时不会重复跳转一次。
     */
    fun takeFrom(intent: Intent?) {
        val route = intent?.getStringExtra(TimetableNotificationScheduler.EXTRA_ROUTE)
        intent?.removeExtra(TimetableNotificationScheduler.EXTRA_ROUTE)
        if (route.isNullOrBlank()) return
        _pending.value = route
    }

    /** 取走并清空，避免回前台或重建时重复跳转。 */
    fun consume(): String? {
        val route = _pending.value
        _pending.value = null
        return route
    }
}

/**
 * 在导航容器内部执行通知带来的跳转。
 *
 * 必须放在提供了 [LocalNavigator] 的作用域里（导航容器内）；跳转用 `Navigator.push`
 * 完成，已经在课表页时 push 会被忽略，不会连压两页。
 */
@Composable
fun TimetableLaunchRouteEffect() {
    val navigator = LocalNavigator.current
    val route by TimetableLaunchRoute.pending.collectAsStateWithLifecycle()
    LaunchedEffect(route) {
        val target = TimetableLaunchRoute.consume() ?: return@LaunchedEffect
        if (target == TimetableNotificationScheduler.ROUTE_TIMETABLE) {
            navigator.push(Route.Timetable)
        }
    }
}