// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys for Navigation.
 * Each destination is a NavKey (data object/data class) and can be saved/restored in the back stack.
 */
@Serializable
sealed interface Route : NavKey {
    /** 主页面：首页 / 学习 / 我的 三个 tab 的外壳。 */
    @Serializable
    data object Main : Route

    /** 主题设置。 */
    @Serializable
    data object Theme : Route

    @Serializable
    data object FreeClassroom : Route

    @Serializable
    data object Electricity : Route

    @Serializable
    data object Score : Route

    @Serializable
    data object Timetable : Route

    @Serializable
    data object TimetableSettings : Route

    /** 考试安排。 */
    @Serializable
    data object Exams : Route

    /** 从考试安排跳到课表：直接定位到某周某天的某节。 */
    @Serializable
    data class TimetableFocus(
    val week: Int,
    val day: Int,
    val startNode: Int,
    /** 要临时显示并描边的考试（`examKey`）；为空表示只是定位到某节次。 */
    val examKey: String? = null,
    /** 要描边的课程格（`day|startNode|endNode`）；首页点某节课跳过来时用。 */
    val courseKey: String? = null,
) : Route

    @Serializable
    data object BulletinList : Route

    @Serializable
    data object PortalAccount : Route

    @Serializable
    data object PortalBind : Route

    @Serializable
    data object PortalImport : Route

    @Serializable
    data object PortalExport : Route

    @Serializable
    data class BulletinDetail(val url: String) : Route

    @Serializable
    data object CampusMap : Route

    @Serializable
    data object InnovationCredit : Route

    @Serializable
    data object LaborScore : Route
}