// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.data.notification.TimetableNotificationScheduler
import dev.duohuo.nuist.sta.compose.data.notification.openExactAlarmSettings
import dev.duohuo.nuist.sta.compose.data.notification.openNotificationSettings
import dev.duohuo.nuist.sta.compose.data.notification.readReminderPermissionState
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettings
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.ColorPickerSheet
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import dev.duohuo.nuist.sta.compose.ui.kit.SwitchWidget
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** 需要弹单选弹窗的设置项。 */
private enum class ChoiceDialog {
    WeekStart,
    CellHeight,
    FontScale,
    Opacity,
    HeaderTextSize,
    ListMode,
    DayLabel,
    ClassReminder,
    NextDayTime,
    HomeMaxItems,
}

/** 「提醒」卡片提示行反映的两种权限缺失：通知没开，或精确闹钟被系统收回。 */
private enum class ReminderPermissionProblem {
    Notifications,
    ExactAlarm,
}

/** 课表管理里打开的二级页；都用页内整页窗口，不占导航栈。 */
private enum class ManagePage {
    DaySwap,
    ChangeLog,
    KeepAlive,
}

/** 与 Dart `TimetableSettings` 一致的取值域：行高 52–90、字号三档、不透明度 30–100、表头 10–14。 */
private val cellHeightChoices = listOf(52, 56, 64, 72, 80, 90)
private val fontScaleChoices = listOf(0.9f, 1f, 1.15f)
private val opacityChoices = listOf(30, 40, 50, 60, 70, 80, 90, 100)
private val headerTextSizeChoices = listOf(10, 11, 12, 13, 14)

/**
 * 课表设置页，只放与「课表怎么显示」有关的选项。
 *
 * 分组与参考实现的外观页一致，但按当前客户端的能力收敛：不做文字颜色与配色方案，
 * 其余选项改动后立刻生效并持久化，课表页无需刷新数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSettingsScreen(
    onBack: () -> Unit,
    viewModel: TimetableSettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var dialog by remember { mutableStateOf<ChoiceDialog?>(null) }
    var colorPickerOpen by remember { mutableStateOf(false) }
    var managePage by remember { mutableStateOf<ManagePage?>(null) }
    var restoreConfirm by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    val manageViewModel: TimetableManagementViewModel = koinViewModel()
    val manageMessage by manageViewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 拒绝后不再反复弹，提醒仍会按计划保存，只是系统不会展示。 */ }

    // 只在真正打开提醒时问一次；Android 13 以下没有这个运行时权限。
    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val prefs = context.getSharedPreferences(
            TimetableNotificationScheduler.PREFS_NAME,
            Context.MODE_PRIVATE,
        )
        if (prefs.getBoolean(TimetableNotificationScheduler.KEY_PERMISSION_ASKED, false)) return
        prefs.edit().putBoolean(TimetableNotificationScheduler.KEY_PERMISSION_ASKED, true).apply()
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // 通知权限与精确闹钟都可能被用户在系统设置里收回，回到前台就重读一次，
    // 从系统设置返回时提示行要立刻跟着变。
    var reminderPermission by remember { mutableStateOf(readReminderPermissionState(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        reminderPermission = readReminderPermissionState(context)
    }
    // 上课提醒在这里恒开（没有单独开关），所以只要系统状态不对就提示。
    val reminderPermissionProblem = when {
        !reminderPermission.notificationsGranted -> ReminderPermissionProblem.Notifications
        !reminderPermission.exactAlarmsGranted -> ReminderPermissionProblem.ExactAlarm
        else -> null
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = scheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("课表设置", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton(onBack = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surfaceContainer,
                    titleContentColor = scheme.onBackground,
                    scrolledContainerColor = scheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SegmentedColumn(title = "显示方式") {
                    item {
                        BaseWidget(
                            icon = if (settings.listMode) "list" else "calendar_month",
                            title = "课表展示",
                            description = if (settings.listMode) "按天列出课程" else "按节次排成网格",
                            onClick = { dialog = ChoiceDialog.ListMode },
                            trailingContent = { ValueTrailing(if (settings.listMode) "列表" else "周视图") },
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = settings.listMode,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
                ) {
                    SegmentedColumn(title = "列表视图") {
                        item {
                            BaseWidget(
                                icon = "date_range",
                                title = "星期显示",
                                description = "按天分组时星期几的写法",
                                onClick = { dialog = ChoiceDialog.DayLabel },
                                trailingContent = {
                                    ValueTrailing(
                                        dayLabelStyles[settings.dayLabelStyle.coerceIn(dayLabelStyles.indices)],
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = !settings.listMode,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
                ) {
                SegmentedColumn(title = "周视图") {
                    item {
                        BaseWidget(
                            icon = "date_range",
                            title = "一周起始",
                            description = if (settings.weekStartsMonday) "周一排在最左" else "周日排在最左",
                            onClick = { dialog = ChoiceDialog.WeekStart },
                            trailingContent = { ValueTrailing(if (settings.weekStartsMonday) "周一" else "周日") },
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = "date_range",
                            title = "显示周六",
                            checked = settings.showSaturday,
                            onCheckedChange = viewModel::setShowSaturday,
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = "date_range",
                            title = "显示周日",
                            checked = settings.showSunday,
                            onCheckedChange = viewModel::setShowSunday,
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = "visibility",
                            title = "显示非本周课程",
                            description = "关闭时只显示当前周的课，开启后半透明显示",
                            checked = settings.showNonCurrentWeek,
                            onCheckedChange = viewModel::setShowNonCurrentWeek,
                        )
                    }
                }
                }
            }
            item {
                AnimatedVisibility(
                    visible = !settings.listMode,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
                ) {
                SegmentedColumn(title = "格子信息") {
                    item {
                        SwitchWidget(
                            icon = "location_on",
                            title = "显示地点",
                            checked = settings.showLocation,
                            onCheckedChange = viewModel::setShowLocation,
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = "person",
                            title = "显示教师",
                            checked = settings.showTeacher,
                            onCheckedChange = viewModel::setShowTeacher,
                        )
                    }
                }
                }
            }
            // 角标在周视图与列表都生效，所以不跟随视图切换隐藏。
            item {
                    SegmentedColumn(title = "课块角标") {
                        item {
                            SwitchWidget(
                                icon = "science",
                                title = "标出实验",
                                description = "教务标记为实验课时的格子标「实验」",
                                checked = settings.markExperimental,
                                onCheckedChange = viewModel::setMarkExperimental,
                            )
                        }
                        item {
                            SwitchWidget(
                                icon = "date_range",
                                title = "标出单双周",
                                description = "只排单周或只排双周的课标上对应胶囊",
                                checked = settings.markParity,
                                onCheckedChange = viewModel::setMarkParity,
                            )
                        }
                        item {
                            SwitchWidget(
                                icon = "edit",
                                title = "标出考试",
                                description = "有考试安排的格子标「考试」",
                                checked = settings.markExam,
                                onCheckedChange = viewModel::setMarkExam,
                            )
                        }
                    }
            }
            item {
                AnimatedVisibility(
                    visible = !settings.listMode,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
                ) {
                SegmentedColumn(title = "外观") {
                    item {
                        SwitchWidget(
                            icon = "schedule",
                            title = "节次显示时间",
                            description = "左侧节次栏显示每节的开始时间",
                            checked = settings.showPeriodTimes,
                            onCheckedChange = viewModel::setShowPeriodTimes,
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = "grid_on",
                            title = "显示网格线",
                            checked = settings.showGridLines,
                            onCheckedChange = viewModel::setShowGridLines,
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "format_size",
                            title = "格子字号",
                            description = "课程块里文字的缩放档位",
                            onClick = { dialog = ChoiceDialog.FontScale },
                            trailingContent = { ValueTrailing(fontScaleLabel(settings.fontScale)) },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "height",
                            title = "格子高度",
                            description = "周视图每一节的行高",
                            onClick = { dialog = ChoiceDialog.CellHeight },
                            trailingContent = { ValueTrailing(cellHeightLabel(settings.cellHeightDp)) },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "invert_colors",
                            title = "格子不透明度",
                            description = "只影响课块底色，文字始终不透明",
                            onClick = { dialog = ChoiceDialog.Opacity },
                            trailingContent = { ValueTrailing("${settings.cellOpacity}%") },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "format_size",
                            title = "表头字号",
                            description = "周视图日期一栏的字号",
                            onClick = { dialog = ChoiceDialog.HeaderTextSize },
                            trailingContent = { ValueTrailing("${settings.headerTextSize}sp") },
                        )
                    }
                }
                }
            }
            item {
                SegmentedColumn(title = "考试") {
                    item {
                        SwitchWidget(
                            icon = "edit",
                            title = "显示考试",
                            description = "在周视图与列表里显示考试安排",
                            checked = settings.showExams,
                            onCheckedChange = viewModel::setShowExams,
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "palette",
                            title = "考试颜色",
                            description = "考试卡片与周视图里的考试块用这个颜色",
                            onClick = { colorPickerOpen = true },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(settings.examColorArgb)),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    SymbolIcon(
                                        "chevron_right",
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                        fontSize = 20.sp,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            item {
                SegmentedColumn(title = "提醒") {
                    item {
                        BaseWidget(
                            icon = "schedule",
                            title = "上课提醒",
                            description = "课前 ${settings.reminderLeadMinutes} 分钟",
                            onClick = { dialog = ChoiceDialog.ClassReminder },
                        )
                    }
                    reminderPermissionProblem?.let { problem ->
                        // 权限没给全时，「上课提醒」下面直接给出原因与去处，不再让用户猜为什么没有通知。
                        item(key = "reminder_permission") {
                            BaseWidget(
                                icon = "info",
                                iconColor = scheme.error,
                                title = reminderPermissionHint(problem),
                                titleStyle = MaterialTheme.typography.bodySmall,
                                onClick = { openReminderPermissionSettings(context, problem) },
                                trailingContent = {
                                    TextButton(onClick = { openReminderPermissionSettings(context, problem) }) {
                                        Text("去设置")
                                    }
                                },
                            )
                        }
                    }
                    item {
                        SwitchWidget(
                            icon = "event",
                            title = "明日课程提醒",
                            description = "每晚按第二天的课表推送一次",
                            checked = settings.nextDayReminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setNextDayReminderEnabled(enabled)
                                if (enabled) ensureNotificationPermission()
                            },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "schedule",
                            title = "提醒时间",
                            description = "明日课程提醒每天推送的时刻",
                            onClick = { dialog = ChoiceDialog.NextDayTime },
                            trailingContent = { ValueTrailing(formatMinuteOfDay(settings.nextDayReminderMinute)) },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "bolt",
                            title = "后台运行设置",
                            description = "允许后台运行，避免提醒被系统延迟",
                            onClick = { managePage = ManagePage.KeepAlive },
                        )
                    }
                }
            }
            // 首页「今日概览」的课表卡片读这两项，改完回到首页立刻生效。
            item {
                SegmentedColumn(title = "首页与桌面") {
                    item {
                        SwitchWidget(
                            icon = "event",
                            title = "首页显示明天的课",
                            description = "今天课上完后，首页卡片接着显示明天的课",
                            checked = settings.showTomorrowOnHome,
                            onCheckedChange = viewModel::setShowTomorrowOnHome,
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "list",
                            title = "首页最多显示几门课",
                            description = "超出的课到课表页查看",
                            onClick = { dialog = ChoiceDialog.HomeMaxItems },
                            trailingContent = {
                                ValueTrailing("${settings.homeMaxItems} 门")
                            },
                        )
                    }
                }
            }
            item {
                SegmentedColumn(title = "课表管理") {
                    item {
                        BaseWidget(
                            icon = "date_range",
                            title = "整天调课",
                            description = "两天课程整天互换，可随时恢复",
                            onClick = { managePage = ManagePage.DaySwap },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "description",
                            title = "变动记录",
                            description = "同步后的改动",
                            onClick = { managePage = ManagePage.ChangeLog },
                        )
                    }
                    item {
                        BaseWidget(
                            icon = "restore",
                            title = "恢复教务课程",
                            description = "清掉你的修改、删除和调课",
                            isError = true,
                            onClick = { restoreConfirm = true },
                        )
                    }
                }
            }
            item {
                Text(
                    "设置只保存在本机，不影响教务处下发的课表数据。提醒需要系统通知权限，未授权时只保存设置，不会弹出通知。",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 36.dp),
                )
            }
        }
    }

    TimetableBridgeMessageEffect(manageMessage, snackbar, manageViewModel::clearMessage)

    when (managePage) {
        ManagePage.DaySwap -> TimetableDaySwapPage(onDismiss = { managePage = null })
        ManagePage.ChangeLog -> TimetableChangeLogPage(onDismiss = { managePage = null })
        ManagePage.KeepAlive -> TimetableKeepAlivePage(onDismiss = { managePage = null })
        null -> Unit
    }

    if (restoreConfirm) {
        AlertDialog(
            onDismissRequest = { if (!restoring) restoreConfirm = false },
            title = { Text("恢复教务课程") },
            text = { Text("恢复所有教务课程和考试？你的修改、删除和调课都会清除。") },
            confirmButton = {
                TextButton(
                    enabled = !restoring,
                    onClick = {
                        restoring = true
                        scope.launch {
                            val changed = manageViewModel.restoreAllJwxt()
                            restoring = false
                            restoreConfirm = false
                            snackbar.showSnackbar(
                                when (changed) {
                                    null -> "恢复失败，请稍后重试"
                                    true -> "已恢复教务课程"
                                    false -> "没有需要恢复的修改"
                                },
                            )
                        }
                    },
                ) {
                    Text("恢复", color = scheme.error)
                }
            },
            dismissButton = {
                TextButton(enabled = !restoring, onClick = { restoreConfirm = false }) { Text("取消") }
            },
        )
    }

    dialog?.let { kind ->
        when (kind) {
            ChoiceDialog.WeekStart -> SingleChoiceDialog(
                title = "一周起始",
                items = listOf("周一", "周日"),
                selectedIndex = if (settings.weekStartsMonday) 0 else 1,
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setWeekStartsMonday(index == 0)
                    dialog = null
                },
            )
            ChoiceDialog.CellHeight -> SingleChoiceDialog(
                title = "格子高度",
                items = cellHeightChoices.map(::cellHeightLabel),
                selectedIndex = cellHeightChoices
                    .indexOf(settings.cellHeightDp).coerceAtLeast(0),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setCellHeightDp(cellHeightChoices[index])
                    dialog = null
                },
            )
            ChoiceDialog.ListMode -> SingleChoiceDialog(
                title = "课表展示",
                items = listOf("周视图", "列表"),
                selectedIndex = if (settings.listMode) 1 else 0,
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setListMode(index == 1)
                    dialog = null
                },
            )
            ChoiceDialog.DayLabel -> SingleChoiceDialog(
                title = "星期显示",
                items = dayLabelStyles,
                selectedIndex = settings.dayLabelStyle.coerceIn(dayLabelStyles.indices),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setDayLabelStyle(index)
                    dialog = null
                },
            )
            ChoiceDialog.Opacity -> SingleChoiceDialog(
                title = "格子不透明度",
                items = opacityChoices.map { "$it%" },
                selectedIndex = opacityChoices.indexOf(settings.cellOpacity).coerceAtLeast(0),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setCellOpacity(opacityChoices[index])
                    dialog = null
                },
            )
            ChoiceDialog.HeaderTextSize -> SingleChoiceDialog(
                title = "表头字号",
                items = headerTextSizeChoices.map { "${it}sp" },
                selectedIndex = headerTextSizeChoices.indexOf(settings.headerTextSize).coerceAtLeast(0),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setHeaderTextSize(headerTextSizeChoices[index])
                    dialog = null
                },
            )
            ChoiceDialog.FontScale -> SingleChoiceDialog(
                title = "格子字号",
                items = fontScaleChoices.map(::fontScaleLabel),
                selectedIndex = fontScaleChoices
                    .indexOf(settings.fontScale).coerceAtLeast(0),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setFontScale(fontScaleChoices[index])
                    dialog = null
                },
            )
            ChoiceDialog.ClassReminder -> SingleChoiceDialog(
                title = "上课提醒",
                items = TimetableSettings.REMINDER_LEAD_OPTIONS.map { "课前 $it 分钟" },
                selectedIndex = TimetableSettings.REMINDER_LEAD_OPTIONS
                    .indexOf(settings.reminderLeadMinutes).coerceAtLeast(0),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setReminderLeadMinutes(TimetableSettings.REMINDER_LEAD_OPTIONS[index])
                    ensureNotificationPermission()
                    dialog = null
                },
            )
            ChoiceDialog.NextDayTime -> SingleChoiceDialog(
                title = "提醒时间",
                items = TimetableSettings.NEXT_DAY_MINUTE_OPTIONS.map(::formatMinuteOfDay),
                selectedIndex = TimetableSettings.NEXT_DAY_MINUTE_OPTIONS
                    .indexOf(settings.nextDayReminderMinute).coerceAtLeast(0),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setNextDayReminderMinute(TimetableSettings.NEXT_DAY_MINUTE_OPTIONS[index])
                    if (settings.nextDayReminderEnabled) ensureNotificationPermission()
                    dialog = null
                },
            )
            ChoiceDialog.HomeMaxItems -> SingleChoiceDialog(
                title = "首页最多显示几门课",
                items = TimetableSettings.HOME_MAX_ITEMS_OPTIONS.map { "$it 门" },
                selectedIndex = TimetableSettings.HOME_MAX_ITEMS_OPTIONS
                    .indexOf(settings.homeMaxItems).coerceAtLeast(0),
                onDismiss = { dialog = null },
                onSelect = { index ->
                    viewModel.setHomeMaxItems(TimetableSettings.HOME_MAX_ITEMS_OPTIONS[index])
                    dialog = null
                },
            )
        }
    }

    if (colorPickerOpen) {
        ColorPickerSheet(
            title = "考试颜色",
            initialColorArgb = settings.examColorArgb,
            onDismiss = { colorPickerOpen = false },
            onConfirm = { argb ->
                viewModel.setExamColorArgb(argb)
                colorPickerOpen = false
            },
        )
    }
}

@Composable
private fun ValueTrailing(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        SymbolIcon(
            "chevron_right",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
            fontSize = 20.sp,
        )
    }
}

private fun cellHeightLabel(dp: Int): String = "$dp dp"

/** 提示行文案：只说清「提醒会怎样」，不出现权限名与系统版本。 */
private fun reminderPermissionHint(problem: ReminderPermissionProblem): String = when (problem) {
    ReminderPermissionProblem.Notifications -> "通知权限未开启，提醒不会显示"
    ReminderPermissionProblem.ExactAlarm -> "精确闹钟未开启，提醒可能延迟"
}

private fun openReminderPermissionSettings(context: Context, problem: ReminderPermissionProblem) {
    when (problem) {
        ReminderPermissionProblem.Notifications -> openNotificationSettings(context)
        ReminderPermissionProblem.ExactAlarm -> openExactAlarmSettings(context)
    }
}

private val dayLabelStyles = listOf("简略", "周几", "星期几")

/** 21:00 这类时刻，分钟数来自 Dart 的设置值。 */
private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour = (minuteOfDay / 60).coerceIn(0, 23)
    val minute = (minuteOfDay % 60).coerceIn(0, 59)
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun fontScaleLabel(scale: Float): String = when {
    scale < 0.95f -> "小"
    scale > 1.05f -> "大"
    else -> "标准"
}