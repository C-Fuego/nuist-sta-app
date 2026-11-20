// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.notification

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * 提醒真正能准时弹出所依赖的两项系统状态。
 *
 * 这里只读系统状态、提供「去设置」的跳转，不碰本地排程：排程仍归
 * [TimetableNotificationScheduler]，权限缺失时它会退化成普通闹钟或干脆不发通知。
 */
data class ReminderPermissionState(
    /** 通知权限：Android 13 起是运行时权限，更低版本看应用级通知总开关。 */
    val notificationsGranted: Boolean,
    /** 精确闹钟：Android 12 起可被系统收回，更低版本恒为可用。 */
    val exactAlarmsGranted: Boolean,
)

/**
 * 读一次当前权限状态。
 *
 * 权限会被用户在系统设置里随时改掉，调用方应在回到前台时重读，而不是只在页面创建时读一次。
 */
fun readReminderPermissionState(context: Context): ReminderPermissionState =
    ReminderPermissionState(
        notificationsGranted = notificationsGranted(context),
        exactAlarmsGranted = exactAlarmsGranted(context),
    )

private fun notificationsGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun exactAlarmsGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
    return alarmManager.canScheduleExactAlarms()
}

/** 打开本应用的通知设置；系统没有这个页面时退回应用详情页。 */
fun openNotificationSettings(context: Context) {
    val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        appDetails(context)
    }
    startSettings(context, target) || startSettings(context, appDetails(context))
}

/** 打开系统的精确闹钟授权页；跳转失败时退回应用详情页。 */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val target = Intent(
        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        Uri.parse("package:${context.packageName}"),
    )
    startSettings(context, target) || startSettings(context, appDetails(context))
}

private fun appDetails(context: Context): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.parse("package:${context.packageName}"),
)

private fun startSettings(context: Context, intent: Intent): Boolean {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent) }.isSuccess
}