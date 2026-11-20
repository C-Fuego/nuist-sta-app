// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.di

import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinDetailViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinListViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.HomeBulletinViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.electricity.ElectricityViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.exam.ExamViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.credits.InnovationCreditViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.credits.LaborScoreViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.score.ScoreViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.student.StudentInfoViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.study.AcademicViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom.FreeClassroomViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.portal.PortalAccountViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableKeepAliveViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableManagementViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableSettingsViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableViewModel
import dev.duohuo.nuist.sta.compose.ui.page.theme.ThemeSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::ThemeSettingsViewModel)
    viewModelOf(::FreeClassroomViewModel)
    viewModelOf(::ElectricityViewModel)
    viewModelOf(::StudentInfoViewModel)
    viewModelOf(::AcademicViewModel)
    viewModelOf(::ScoreViewModel)
    viewModelOf(::ExamViewModel)
    viewModelOf(::InnovationCreditViewModel)
    viewModelOf(::LaborScoreViewModel)
    viewModelOf(::PortalAccountViewModel)
    // 课表状态是全应用共享的一份：首页卡片、学习页、课表页、考试页都读它。
    // 用 single 而不是 viewModelOf，避免每个导航页各建一份实例、每进一次页面就重新走桥。
    single { TimetableViewModel(get(), get()) }
    viewModelOf(::TimetableSettingsViewModel)
    viewModelOf(::TimetableManagementViewModel)
    viewModelOf(::TimetableKeepAliveViewModel)
    viewModelOf(::BulletinListViewModel)
    viewModelOf(::BulletinDetailViewModel)
    viewModelOf(::HomeBulletinViewModel)
}
