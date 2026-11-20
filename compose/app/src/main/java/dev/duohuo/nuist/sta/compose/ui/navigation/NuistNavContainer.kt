// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.PredictiveBackAnimation
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.ThemeState
import dev.duohuo.nuist.sta.compose.ui.animation.predictiveback.appNavTransition
import dev.duohuo.nuist.sta.compose.ui.activity.TimetableLaunchRouteEffect
import dev.duohuo.nuist.sta.compose.ui.kit.LocalAppBack
import dev.duohuo.nuist.sta.compose.ui.page.theme.ThemeSettingsPage
import dev.duohuo.nuist.sta.compose.ui.page.nuist.NuistMainScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinDetailScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinListScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.campus_map.CampusMapScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.credits.InnovationCreditScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.credits.LaborScoreScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.electricity.ElectricityScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom.FreeClassroomScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.score.ScoreScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.portal.PortalAccountScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.portal.PortalBindScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.portal.PortalExportScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.portal.PortalImportScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.exam.ExamScreen
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableSettingsScreen
import dev.duohuo.nuist.sta.compose.ui.util.rememberDeviceCornerRadius
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

/**
 * 应用的路由容器。外壳与参考实现保持一致：
 * 返回栈来自 Miuix 的 `rememberNavBackStack`，转场走整包移植的预测性返回工厂，
 * 每一页都包一层 [NuistNavEntry] 处理系统返回。
 */
@Composable
fun NuistNavContainer(uiState: ThemeState) {
    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        val navCornerRadius = rememberDeviceCornerRadius(defaultRadius = 0.dp)
        val roundAllCorners = uiState.predictiveBackAnimation == PredictiveBackAnimation.AOSP ||
            uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale ||
            uiState.predictiveBackAnimation == PredictiveBackAnimation.Classic
        val backdropColor = MaterialTheme.colorScheme.surfaceContainer
        val effects = remember(navCornerRadius, roundAllCorners, backdropColor) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
                cornerClipMode = if (roundAllCorners) NavCornerClipMode.All else NavCornerClipMode.Leading,
                dimAmount = 0.5f,
                backdropColor = backdropColor,
                blockInputDuringTransition = false,
            )
        }
        val transition = remember(uiState.predictiveBackAnimation, uiState.predictiveBackExitDirection) {
            appNavTransition(
                animation = uiState.predictiveBackAnimation,
                exitDirection = uiState.predictiveBackExitDirection,
            )
        }
        val swipeBackDirection = when (LocalLayoutDirection.current) {
            LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
            LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
        }
        val interceptPredictiveBack =
            uiState.predictiveBackAnimation == PredictiveBackAnimation.None && backStack.size > 1

        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            transition = transition,
            effects = effects,
        ) {
            entry<Route.Main> {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    val pagerState = rememberPagerState(initialPage = 0) { 3 }
                    val mainPagerState = rememberMainPagerState(pagerState)
                    NuistMainScreen(
                        uiState = uiState,
                        mainPagerState = mainPagerState,
                        onOpenPage = { id ->
                            when (id) {
                                "free-classroom" -> navigator.push(Route.FreeClassroom)
                                "electricity" -> navigator.push(Route.Electricity)
                                "score" -> navigator.push(Route.Score)
                                "timetable" -> navigator.push(Route.Timetable)
                                "bulletin" -> navigator.push(Route.BulletinList)
                                "map" -> navigator.push(Route.CampusMap)
                                "credits" -> navigator.push(Route.InnovationCredit)
                                "labor" -> navigator.push(Route.LaborScore)
                                "portal" -> navigator.push(Route.PortalAccount)
                                "exams" -> navigator.push(Route.Exams)
                            }
                        },
                        onOpenThemeSettings = { navigator.push(Route.Theme) },
                    )
                }
            }
            entry<Route.Theme>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    ThemeSettingsPage()
                }
            }
            entry<Route.FreeClassroom>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) { FreeClassroomScreen() }
                }
            }
            entry<Route.Electricity>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) { ElectricityScreen() }
                }
            }
            entry<Route.Score>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) { ScoreScreen() }
                }
            }
            // 课表页左右滑动属于「翻周」，不能再挂返回手势，否则右滑直接退出页面。
            entry<Route.TimetableFocus> { key ->
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) {
                        TimetableScreen(
                            onOpenPortal = { navigator.push(Route.PortalAccount) },
                            onOpenSettings = { navigator.push(Route.TimetableSettings) },
                            initialWeek = key.week,
                            scrollToNode = key.startNode,
                            focusExamKey = key.examKey,
                            focusCourseKey = key.courseKey,
                        )
                    }
                }
            }
            entry<Route.Timetable> {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) {
                        TimetableScreen(
                            onOpenPortal = { navigator.push(Route.PortalAccount) },
                            onOpenSettings = { navigator.push(Route.TimetableSettings) },
                        )
                    }
                }
            }
            entry<Route.TimetableSettings>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    TimetableSettingsScreen(onBack = onBack)
                }
            }
            entry<Route.Exams>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) {
                        ExamScreen(
                            onBack = onBack,
                            onOpenPortal = { navigator.push(Route.PortalAccount) },
                            onOpenTimetable = { week, day, node, examKey ->
                                // 只按周次与节次定位：星期由课表页自己的列集合决定。
                                navigator.push(
                                    Route.TimetableFocus(
                                        week = week,
                                        day = day,
                                        startNode = node,
                                        examKey = examKey,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            entry<Route.PortalAccount>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PortalAccountScreen(
                        onBack = onBack,
                        onOpenBind = { navigator.push(Route.PortalBind) },
                        onOpenImport = { navigator.push(Route.PortalImport) },
                        onOpenExport = { navigator.push(Route.PortalExport) },
                    )
                }
            }
            entry<Route.PortalBind> {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PortalBindScreen(onBack = onBack, onDone = onBack)
                }
            }
            entry<Route.PortalImport>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PortalImportScreen(onBack = onBack, onDone = onBack)
                }
            }
            entry<Route.PortalExport>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PortalExportScreen(onBack = onBack)
                }
            }
            entry<Route.CampusMap>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) { CampusMapScreen() }
                }
            }
            entry<Route.InnovationCredit>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) { InnovationCreditScreen() }
                }
            }
            entry<Route.LaborScore>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) { LaborScoreScreen() }
                }
            }
            entry<Route.BulletinList>(swipeDismiss = swipeBackDirection) {
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    PageWithBack(onBack) {
                        BulletinListScreen(
                            onOpen = { bulletin -> navigator.push(Route.BulletinDetail(bulletin.url)) },
                        )
                    }
                }
            }
            entry<Route.BulletinDetail>(swipeDismiss = swipeBackDirection) { key ->
                NuistNavEntry(interceptPredictiveBack, onBack) {
                    BulletinDetailScreen(
                        url = key.url,
                        onBack = onBack,
                    )
                }
            }
        }
        // 通知点击带来的课表跳转：navigator 只在这个作用域里可见。
        TimetableLaunchRouteEffect()
    }
}

/** 给二级页提供返回回调（页内 `MiniAppBackButton` 依赖它显示返回按钮）。 */
@Composable
private fun PageWithBack(onBack: () -> Unit, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppBack provides onBack) { content() }
}

@Composable
private fun NuistNavEntry(interceptPredictiveBack: Boolean, onBack: () -> Unit, content: @Composable () -> Unit) {
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    // 所有二级页都从这里拿到返回回调，页内 `MiniAppBackButton()` 才不会因为
    // LocalAppBack 为 null 而悄悄不画按钮。
    CompositionLocalProvider(LocalAppBack provides onBack) { content() }
}