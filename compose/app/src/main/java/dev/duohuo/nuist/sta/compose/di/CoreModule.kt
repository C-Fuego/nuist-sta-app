// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.di

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.notification.TimetableNotificationScheduler
import dev.duohuo.nuist.sta.compose.data.notification.TimetableReminderChannel
import dev.duohuo.nuist.sta.compose.data.notification.TimetableReminderSync
import dev.duohuo.nuist.sta.compose.data.bridge.DartBridge
import dev.duohuo.nuist.sta.compose.data.bulletin.BulletinApi
import dev.duohuo.nuist.sta.compose.data.jwxt.FreeClassroomApi
import dev.duohuo.nuist.sta.compose.data.credits.InnovationCreditApi
import dev.duohuo.nuist.sta.compose.data.credits.LaborScoreApi
import dev.duohuo.nuist.sta.compose.data.exam.ExamApi
import dev.duohuo.nuist.sta.compose.data.score.ScoreApi
import dev.duohuo.nuist.sta.compose.data.electricity.ElectricityApi
import dev.duohuo.nuist.sta.compose.data.electricity.ElectricityStore
import dev.duohuo.nuist.sta.compose.data.academic.AcademicApi
import dev.duohuo.nuist.sta.compose.data.student.StudentInfoApi
import dev.duohuo.nuist.sta.compose.data.student.StudentInfoCache
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableApi
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettingsRepository
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableCache
import dev.duohuo.nuist.sta.compose.data.timetable.TimetablePreferences
import org.koin.android.ext.koin.androidContext
import dev.duohuo.nuist.sta.compose.data.bulletin.BulletinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule = module {
    // Provide a global coroutine scope for application-level background tasks
    single<CoroutineScope>(named("AppScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // 提醒排程只读写本机缓存，先于 Dart 引擎创建，保证通道注册时已可用。
    single { TimetableNotificationScheduler(androidContext()) }
    single { TimetableReminderChannel(get()) }
    single { TimetableReminderSync(get(), get()) }

    // 校园网 Dart 核心：无界面引擎，按需启动（惰性单例）
    single { DartBridge(androidContext(), get()) }
    single { CampusBridge(get()) }

    // 学校信息公告栏：无状态 HTTP 客户端 + 带缓存的仓库
    single { BulletinApi(get()) }
    single { BulletinRepository(get()) }

    single { TimetablePreferences(androidContext()) }
    single { TimetableApi(get()) }
    single { TimetableSettingsRepository(get(), get()) }
    single { TimetableCache(androidContext()) }
    single { StudentInfoCache(androidContext()) }
    single { ScoreApi(get()) }
    single { ExamApi(get()) }
    single { InnovationCreditApi(get()) }
    single { LaborScoreApi(get()) }
    single { FreeClassroomApi(get()) }
    single { ElectricityApi(get()) }
    single { ElectricityStore(androidContext()) }
    single { StudentInfoApi(get()) }
    single { AcademicApi(get()) }

}
