// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.duohuo.nuist.sta.compose.R
import dev.duohuo.nuist.sta.compose.data.bridge.arr
import dev.duohuo.nuist.sta.compose.data.bridge.bool
import dev.duohuo.nuist.sta.compose.data.bridge.int
import dev.duohuo.nuist.sta.compose.data.bridge.objects
import dev.duohuo.nuist.sta.compose.data.bridge.str
import dev.duohuo.nuist.sta.compose.ui.activity.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

/**
 * 课表提醒的排程与落盘。
 *
 * 只保存「已经算好的触发时刻与文案」，不在闹钟响的时候重新计算课表：闹钟响时进程可能
 * 已经被回收，重新拉起 Dart 引擎既慢又容易失败，提前缓存才能保证通知准时。
 *
 * 两类提醒各自整表替换：
 * - 上课提醒用固定 requestCode 段 2000..2127，一条提醒一个编号；
 * - 明日提醒固定 1000，只排最近的一个日期。
 * 另有 3000 的「补充前瞻」闹钟，用来在无人打开 App 时滚动拉取新的 7 天计划。
 */
class TimetableNotificationScheduler(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    init {
        ensureChannel()
    }

    /** 替换明日提醒：保留未来 400 天里最近的一条，到点后由接收器再排下一条。 */
    fun applyNextDay(configuration: Map<*, *>?) {
        val enabled = configuration?.get("enabled") as? Boolean ?: false
        val minute = (configuration?.get("minuteOfDay") as? Number)?.toInt() ?: DEFAULT_MINUTE
        val payloads = LinkedHashMap<String, NextDayPayload>()
        (configuration?.get("payloads") as? Map<*, *>)?.forEach { (key, value) ->
            val item = value as? Map<*, *> ?: return@forEach
            payloads[key.toString()] = NextDayPayload(
                title = item["title"] as? String ?: "明日课程提醒",
                body = item["body"] as? String ?: "",
                warmLine = item["warmLine"] as? String,
                footer = item["footer"] as? String ?: DEFAULT_FOOTER,
            )
        }
        val config = NextDayConfig(
            enabled = enabled,
            minuteOfDay = minute.coerceIn(0, 1439),
            payloads = payloads,
        )
        prefs.edit().putString(KEY_NEXT_DAY, encodeNextDay(config)).apply()
        cancelPending(NEXT_DAY_REQUEST, ACTION_NEXT_DAY)
        if (enabled) scheduleNextDay(config)
    }

    /** 接收原生通道传来的整表计划。 */
    fun applyPlanFromChannel(plan: List<*>) {
        val entries = plan.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            val at = parseWallClock(item["at"] as? String) ?: return@mapNotNull null
            ReminderEntry(
                atMillis = at,
                title = item["title"] as? String ?: return@mapNotNull null,
                body = item["body"] as? String ?: "",
            )
        }
        applyPlan(entries)
    }

    /** 接收桥返回的 `timetable.notifyPlan` 结果。 */
    fun applyPlanFromBridge(data: JsonObject) {
        val entries = data.arr("reminders").objects().mapNotNull { item ->
            val at = parseWallClock(item.str("at").ifEmpty { null }) ?: return@mapNotNull null
            ReminderEntry(
                atMillis = at,
                title = item.str("title").ifEmpty { return@mapNotNull null },
                body = item.str("body"),
            )
        }
        applyPlan(entries)
    }

    private fun applyPlan(rawEntries: List<ReminderEntry>) {
        val now = System.currentTimeMillis()
        val entries = rawEntries
            .filter { it.atMillis > now + 1_000L }
            .sortedBy { it.atMillis }
            .take(MAX_CLASS_ALARMS)
        prefs.edit().putString(KEY_PLAN, encodePlan(entries)).apply()

        for (index in 0 until MAX_CLASS_ALARMS) {
            cancelPending(CLASS_REQUEST_BASE + index, ACTION_CLASS)
        }
        entries.forEachIndexed { index, entry ->
            scheduleAlarm(entry.atMillis, classPending(index))
        }
        // 最后一条响过一分钟后再向 Dart 要一份计划；没有课可排时每天上午补一次。
        val refreshAt = entries.lastOrNull()?.atMillis?.plus(60_000L)
            ?: nextMorningMillis()
        scheduleInexact(refreshAt, refreshPending())
    }

    /** 开机或升级后从缓存重新登记闹钟，不依赖进程内状态。 */
    fun rearmFromCache() {
        val entries = readPlan()
        // 冷启动时进程内闹钟可能还在，先清掉整段再按缓存重排，避免旧计划多出的编号继续触发。
        for (index in 0 until MAX_CLASS_ALARMS) {
            cancelPending(CLASS_REQUEST_BASE + index, ACTION_CLASS)
        }
        entries.forEachIndexed { index, entry ->
            if (entry.atMillis > System.currentTimeMillis()) {
                scheduleAlarm(entry.atMillis, classPending(index))
            }
        }
        readNextDay()?.takeIf { it.enabled }?.let(::scheduleNextDay)
        // 缓存可能已经过期（例如关机跨过了最后一条），补一次前瞻。
        scheduleInexact(
            System.currentTimeMillis() + 60_000L,
            refreshPending(),
        )
    }

    /** 明日提醒到点后排下一条；缓存里的 400 天足够用到下次打开 App。 */
    fun scheduleNextDayFromCache() {
        readNextDay()?.takeIf { it.enabled }?.let(::scheduleNextDay)
    }

    fun notifyClass(index: Int) {
        val entry = readPlan().getOrNull(index) ?: return
        post(CLASS_REQUEST_BASE + index, entry.title, entry.body)
    }

    fun notifyNextDay(dateKey: String) {
        val config = readNextDay() ?: return
        val payload = config.payloads[dateKey] ?: return
        val body = buildString {
            append(payload.body)
            payload.warmLine?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append('\n')
                append(it)
            }
            payload.footer.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append('\n')
                append(it)
            }
        }
        post(NEXT_DAY_REQUEST, payload.title, body)
    }

    fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun scheduleNextDay(config: NextDayConfig) {
        if (!config.enabled) {
            cancelPending(NEXT_DAY_REQUEST, ACTION_NEXT_DAY)
            return
        }
        val now = System.currentTimeMillis()
        val next = config.payloads.keys
            .mapNotNull { key ->
                val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@mapNotNull null
                val time = LocalTime.of(config.minuteOfDay / 60, config.minuteOfDay % 60)
                val at = date.atTime(time)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (at > now + 1_000L) key to at else null
            }
            .minByOrNull { it.second }
            ?: return
        scheduleAlarm(next.second, nextDayPending(next.first))
    }

    private fun classPending(index: Int): PendingIntent {
        val intent = Intent(context, TimetableReminderReceiver::class.java).apply {
            action = ACTION_CLASS
            putExtra(EXTRA_INDEX, index)
        }
        return PendingIntent.getBroadcast(
            context,
            CLASS_REQUEST_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextDayPending(dateKey: String): PendingIntent {
        val intent = Intent(context, TimetableReminderReceiver::class.java).apply {
            action = ACTION_NEXT_DAY
            putExtra(EXTRA_DATE_KEY, dateKey)
        }
        return PendingIntent.getBroadcast(
            context,
            NEXT_DAY_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun refreshPending(): PendingIntent {
        val intent = Intent(context, TimetableReminderReceiver::class.java).apply {
            action = ACTION_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            REFRESH_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPending(requestCode: Int, action: String) {
        val intent = Intent(context, TimetableReminderReceiver::class.java).apply {
            this.action = action
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    /** 精确闹钟权限被系统收回时退化到普通 idle 闹钟，保证提醒不会完全不响。 */
    private fun scheduleAlarm(triggerAtMillis: Long, pending: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending,
                )
            }
        } catch (error: SecurityException) {
            Timber.w(error, "精确闹钟不可用，改用普通闹钟")
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending,
                )
            }.onFailure { Timber.w(it, "闹钟排程失败") }
        }
    }

    /** 补充前瞻不需要精确，避免占用精确闹钟额度。 */
    fun scheduleInexact(triggerAtMillis: Long, pending: PendingIntent) {
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }.onFailure { Timber.w(it, "补充前瞻闹钟排程失败") }
    }

    fun scheduleRefreshRetry() {
        scheduleInexact(System.currentTimeMillis() + 6 * 60 * 60 * 1000L, refreshPending())
    }

    private fun post(id: Int, title: String, body: String) {
        if (!notificationsAllowed()) return
        ensureChannel()
        val launch = Intent(context, MainActivity::class.java).apply {
            // 点通知直接落在课表页；路由字符串由活动侧解析，通知本身不认识导航结构。
            putExtra(EXTRA_ROUTE, ROUTE_TIMETABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }.onFailure { Timber.w(it, "通知发送失败") }
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "课表提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "上课前的提醒与每晚的明日课程提醒"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun readPlan(): List<ReminderEntry> {
        val raw = prefs.getString(KEY_PLAN, null) ?: return emptyList()
        return runCatching {
            Json.parseToJsonElement(raw).let { element ->
                (element as? JsonArray)?.mapNotNull { item ->
                    val obj = item as? JsonObject ?: return@mapNotNull null
                    ReminderEntry(
                        atMillis = (obj["at"] as? JsonPrimitive)?.content?.toLongOrNull()
                            ?: return@mapNotNull null,
                        title = obj.str("title"),
                        body = obj.str("body"),
                    )
                } ?: emptyList()
            }
        }.getOrElse {
            Timber.w(it, "提醒计划缓存损坏")
            emptyList()
        }
    }

    private fun encodePlan(entries: List<ReminderEntry>): String = JsonArray(
        entries.map { entry ->
            JsonObject(
                mapOf(
                    "at" to JsonPrimitive(entry.atMillis),
                    "title" to JsonPrimitive(entry.title),
                    "body" to JsonPrimitive(entry.body),
                ),
            )
        },
    ).toString()

    private fun readNextDay(): NextDayConfig? {
        val raw = prefs.getString(KEY_NEXT_DAY, null) ?: return null
        return runCatching {
            val root = Json.parseToJsonElement(raw).jsonObject
            val payloads = LinkedHashMap<String, NextDayPayload>()
            root["payloads"]?.jsonObject?.forEach { (key, value) ->
                val item = value.jsonObject
                payloads[key] = NextDayPayload(
                    title = item.str("title"),
                    body = item.str("body"),
                    warmLine = item.str("warmLine").ifEmpty { null },
                    footer = item.str("footer"),
                )
            }
            NextDayConfig(
                enabled = root.bool("enabled"),
                minuteOfDay = root.int("minuteOfDay", DEFAULT_MINUTE),
                payloads = payloads,
            )
        }.getOrElse {
            Timber.w(it, "明日提醒缓存损坏")
            null
        }
    }

    private fun encodeNextDay(config: NextDayConfig): String {
        val payloads = JsonObject(
            config.payloads.mapValues { (_, payload) ->
                JsonObject(
                    mapOf(
                        "title" to JsonPrimitive(payload.title),
                        "body" to JsonPrimitive(payload.body),
                        "warmLine" to JsonPrimitive(payload.warmLine.orEmpty()),
                        "footer" to JsonPrimitive(payload.footer),
                    ),
                )
            },
        )
        return JsonObject(
            mapOf(
                "enabled" to JsonPrimitive(config.enabled),
                "minuteOfDay" to JsonPrimitive(config.minuteOfDay),
                "payloads" to payloads,
            ),
        ).toString()
    }

    /** Dart 的 `toIso8601String()` 是不带偏移的本地墙钟，按设备时区解释。 */
    private fun parseWallClock(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            LocalDateTime.parse(value)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    private fun nextMorningMillis(): Long {
        val now = ZonedDateTime.now()
        var at = now.toLocalDate().atTime(9, 0).atZone(now.zone)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    private data class ReminderEntry(val atMillis: Long, val title: String, val body: String)

    private data class NextDayPayload(
        val title: String,
        val body: String,
        val warmLine: String?,
        val footer: String,
    )

    private data class NextDayConfig(
        val enabled: Boolean,
        val minuteOfDay: Int,
        val payloads: Map<String, NextDayPayload>,
    )

    companion object {
        const val CHANNEL_ID = "timetable_reminder"
        const val PREFS_NAME = "timetable_reminders"
        const val KEY_PERMISSION_ASKED = "notification_permission_asked"
        const val ACTION_CLASS = "dev.duohuo.nuist.sta.compose.action.CLASS_REMINDER"
        const val ACTION_NEXT_DAY = "dev.duohuo.nuist.sta.compose.action.NEXT_DAY_REMINDER"
        const val ACTION_REFRESH = "dev.duohuo.nuist.sta.compose.action.REFRESH_REMINDERS"
        const val EXTRA_INDEX = "index"
        const val EXTRA_DATE_KEY = "dateKey"

        /** 通知点击时带的路由；活动侧读走并立刻抹掉，避免回前台重复跳转。 */
        const val EXTRA_ROUTE = "route"
        const val ROUTE_TIMETABLE = "/apps/timetable"

        private const val KEY_PLAN = "class_plan"
        private const val KEY_NEXT_DAY = "next_day"
        private const val CLASS_REQUEST_BASE = 2000
        private const val MAX_CLASS_ALARMS = 128
        private const val NEXT_DAY_REQUEST = 1000
        private const val REFRESH_REQUEST = 3000
        private const val DEFAULT_MINUTE = 22 * 60
        private const val DEFAULT_FOOTER = "按本机已保存课表"
    }
}