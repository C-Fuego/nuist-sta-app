// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.domain.settings.model.preferences.ThemeState
import dev.duohuo.nuist.sta.compose.ui.icons.AppIcons
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.EntryCard
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SingleChoiceDialog
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import dev.duohuo.nuist.sta.compose.ui.library.FloatingBottomBar
import dev.duohuo.nuist.sta.compose.ui.library.FloatingBottomBarDefaults
import dev.duohuo.nuist.sta.compose.ui.library.FloatingBottomBarMode
import dev.duohuo.nuist.sta.compose.ui.library.LocalFloatingBottomBarActiveLayer
import dev.duohuo.nuist.sta.compose.ui.navigation.MainPagerState
import dev.duohuo.nuist.sta.compose.ui.page.nuist.home.HomeBulletinSection
import dev.duohuo.nuist.sta.compose.ui.page.nuist.home.HomeEntrySection
import dev.duohuo.nuist.sta.compose.ui.page.nuist.home.HomeTodaySection
import dev.duohuo.nuist.sta.compose.ui.page.nuist.study.AcademicSection
import dev.duohuo.nuist.sta.compose.ui.page.nuist.study.SemesterSection
import dev.duohuo.nuist.sta.compose.ui.page.nuist.study.AcademicViewModel
import dev.duohuo.nuist.sta.compose.ui.theme.LocalWindowLayoutInfo
import dev.duohuo.nuist.sta.compose.ui.theme.installerMaterial3BlurEffect
import dev.duohuo.nuist.sta.compose.ui.theme.rememberMaterial3BlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.size
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableViewModel
import androidx.compose.runtime.LaunchedEffect

/** 底部导航的一个 tab。 */
private data class NuistTab(val label: String, val icon: String)

private val nuistTabs = listOf(
    NuistTab("首页", "home"),
    NuistTab("学习", "school"),
    NuistTab("我的", "person"),
)

/**
 * 主页面外壳。
 *
 * 结构照搬参考实现的 `Material3SettingsCompactLayout` / `Material3SettingsWideScreenLayout`：
 * 先按 [LocalWindowLayoutInfo] 静态分支（窄屏底栏 / 宽屏侧栏），再决定悬浮胶囊还是常驻导航；
 * 悬浮底栏挂在 `Scaffold.bottomBar`，其 padding 作为 `outerPadding` 一路传给页面内容，
 * 因此最后一行可以真正滚到胶囊底下而不被遮挡。
 */
@Composable
fun NuistMainScreen(
    uiState: ThemeState,
    mainPagerState: MainPagerState,
    onOpenPage: (String) -> Unit,
    onOpenThemeSettings: () -> Unit,
) {
    val layoutInfo = LocalWindowLayoutInfo.current
    val useBlur = uiState.useBlur
    val useFloatingBottomBar = uiState.useAppleFloatingBar
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    if (layoutInfo.showNavigationRail) {
        NuistMainWideLayout(
            mainPagerState = mainPagerState,
            useBlur = useBlur,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop,
            onOpenPage = onOpenPage,
            onOpenThemeSettings = onOpenThemeSettings,
        )
    } else {
        NuistMainCompactLayout(
            mainPagerState = mainPagerState,
            useBlur = useBlur,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop,
            isMedium = layoutInfo.isMediumPortrait,
            onOpenPage = onOpenPage,
            onOpenThemeSettings = onOpenThemeSettings,
        )
    }
}

/** Compact Screen Layout (Portrait/Phone) */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NuistMainCompactLayout(
    mainPagerState: MainPagerState,
    useBlur: Boolean,
    useFloatingBottomBar: Boolean,
    backdrop: LayerBackdrop?,
    isMedium: Boolean,
    onOpenPage: (String) -> Unit,
    onOpenThemeSettings: () -> Unit,
) {
    val navigationWindowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (useFloatingBottomBar) {
                NuistFloatingBottomBar(
                    mainPagerState = mainPagerState,
                    backdrop = backdrop,
                )
            } else {
                RowNavigation(
                    modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                    windowInsets = navigationWindowInsets,
                    currentPage = mainPagerState.selectedPage,
                    onPageChanged = { mainPagerState.animateToPage(it) },
                    containerColor = if (useBlur) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
                    isMedium = isMedium,
                )
            }
        },
    ) { paddingValues ->
        NuistPagerContent(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            mainPagerState = mainPagerState,
            outerPadding = paddingValues,
            onOpenPage = onOpenPage,
            onOpenThemeSettings = onOpenThemeSettings,
        )
    }
}

/** Wide Screen Layout (Tablet/Landscape) */
@Composable
private fun NuistMainWideLayout(
    mainPagerState: MainPagerState,
    useBlur: Boolean,
    useFloatingBottomBar: Boolean,
    backdrop: LayerBackdrop?,
    onOpenPage: (String) -> Unit,
    onOpenThemeSettings: () -> Unit,
) {
    val navigationWindowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Vertical + WindowInsetsSides.Start,
    )

    if (useFloatingBottomBar) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NuistFloatingBottomBar(
                    mainPagerState = mainPagerState,
                    backdrop = backdrop,
                )
            },
        ) { paddingValues ->
            NuistPagerContent(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(paddingValues)
                    .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                mainPagerState = mainPagerState,
                outerPadding = paddingValues,
                onOpenPage = onOpenPage,
                onOpenThemeSettings = onOpenThemeSettings,
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            ColumnNavigation(
                windowInsets = navigationWindowInsets,
                currentPage = mainPagerState.selectedPage,
                onPageChanged = { mainPagerState.animateToPage(it) },
            )

            NuistPagerContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .consumeWindowInsets(navigationWindowInsets.only(WindowInsetsSides.Start))
                    .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                mainPagerState = mainPagerState,
                outerPadding = PaddingValues(0.dp),
                onOpenPage = onOpenPage,
                onOpenThemeSettings = onOpenThemeSettings,
            )
        }
    }
}

@Composable
private fun NuistFloatingBottomBar(
    mainPagerState: MainPagerState,
    backdrop: LayerBackdrop?,
) {
    val fallbackBackdrop = rememberLayerBackdrop()
    val floatingBackdrop = backdrop ?: fallbackBackdrop

    Box(modifier = Modifier.fillMaxWidth()) {
        FloatingBottomBar(
            items = nuistTabs,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            selectedIndex = { mainPagerState.selectedPage },
            onSelected = { index -> mainPagerState.animateToPage(index) },
            backdrop = floatingBackdrop,
            mode = if (backdrop != null) FloatingBottomBarMode.Blur else FloatingBottomBarMode.None,
            colors = FloatingBottomBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                indicatorColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            iconContent = { tab, _ ->
                SymbolIcon(
                    name = tab.icon,
                    // 选中层实心、底色层描边，拖动时随指示器即时切换
                    filled = LocalFloatingBottomBarActiveLayer.current,
                    contentDescription = tab.label,
                )
            },
            labelContent = { tab, _ ->
                Text(
                    text = tab.label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            },
        )
    }
}

/** 三个 tab 的分页内容。 */
@Composable
private fun NuistPagerContent(
    modifier: Modifier = Modifier,
    mainPagerState: MainPagerState,
    outerPadding: PaddingValues,
    onOpenPage: (String) -> Unit,
    onOpenThemeSettings: () -> Unit,
) {
    HorizontalPager(
        state = mainPagerState.pagerState,
        userScrollEnabled = true,
        modifier = modifier,
        beyondViewportPageCount = 2,
    ) { page ->
        when (page) {
            0 -> HomeTab(outerPadding = outerPadding, onOpenPage = onOpenPage)
            1 -> StudyTab(outerPadding = outerPadding, onOpenPage = onOpenPage)
            2 -> ProfileTab(
                outerPadding = outerPadding,
                onOpenThemeSettings = onOpenThemeSettings,
                onOpenPortal = { onOpenPage("portal") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTab(outerPadding: PaddingValues, onOpenPage: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("NUIST++", modifier = Modifier.padding(start = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(
                bottom = 16.dp + outerPadding.calculateBottomPadding(),
            ),
        ) {
            item {
                HomeTodaySection(
                    onOpenTimetable = { onOpenPage("timetable") },
                    onOpenElectricity = { onOpenPage("electricity") },
                    onOpenExams = { onOpenPage("exams") },
                )
            }
            item { HomeBulletinSection(onOpenAll = { onOpenPage("bulletin") }) }
            item {
                Text(
                    "校园导览",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 12.dp),
                )
                HomeEntrySection { id -> onOpenPage(id) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudyTab(
    outerPadding: PaddingValues,
    onOpenPage: (String) -> Unit,
    timetableViewModel: TimetableViewModel = koinInject(),
    academicViewModel: AcademicViewModel = koinViewModel(),
) {
    val timetableState by timetableViewModel.state.collectAsStateWithLifecycle()
    val academic by academicViewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LaunchedEffect(Unit) {
        // 课表状态是全应用共享的一份，进学习页只复用现有数据（不发起同步）；
        // 强制同步走课表页的下拉刷新，避免进页面时把桥的串行队列堵住。
        timetableViewModel.load()
        academicViewModel.refresh()
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("学习", modifier = Modifier.padding(start = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp + outerPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val snapshot = timetableState.snapshot
            // 学期与周次来自课表快照；学生身份不在这里重复，统一在「统一门户」。
            SemesterSection(
                semester = snapshot?.termLabel ?: "学期未同步",
                week = snapshot?.currentWeek ?: 0,
                totalWeeks = snapshot?.totalWeeks ?: 0,
            )
            if (academic.summary != null) {
                AcademicSection(academic.display)
            } else if (academic.error != null) {
                SegmentedColumn(title = "学业进度") {
                    item {
                        BaseWidget(
                            icon = "error",
                            title = "学业概览加载失败",
                            isError = true,
                            onClick = academicViewModel::refresh,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "常用功能",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    EntryCard("analytics", "成绩查询", Modifier.weight(1f)) { onOpenPage("score") }
                    EntryCard("lightbulb", "双创学分", Modifier.weight(1f)) { onOpenPage("credits") }
                    EntryCard("work", "劳动积分", Modifier.weight(1f)) { onOpenPage("labor") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileTab(
    outerPadding: PaddingValues,
    onOpenThemeSettings: () -> Unit,
    onOpenPortal: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("我的", modifier = Modifier.padding(start = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp + outerPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegmentedColumn(title = "账号") {
                item {
                    BaseWidget(
                        icon = "link",
                        title = "统一门户",
                        description = "绑定后无感使用校级服务",
                        onClick = onOpenPortal,
                    ) {
                        SymbolIcon("chevron_right", contentDescription = null, tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(
                        icon = "palette",
                        title = "主题设置",
                        description = "主题模式、配色、预测性返回",
                        onClick = onOpenThemeSettings,
                    ) {
                        SymbolIcon("chevron_right", contentDescription = null, tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(
                        icon = "bug_report",
                        title = "诊断与日志",
                        description = "导出 / 清除本机诊断日志",
                        onClick = {},
                    ) {
                        SymbolIcon("chevron_right", contentDescription = null, tint = scheme.onSurfaceVariant)
                    }
                }
            }
            SegmentedColumn(title = "关于") {
                item {
                    BaseWidget(
                        icon = "info",
                        title = "关于 NUIST++",
                        description = "版本 0.1.0-compose",
                        onClick = {},
                    ) {
                        SymbolIcon("chevron_right", contentDescription = null, tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowNavigation(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    isMedium: Boolean = false,
) {
    ShortNavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
        windowInsets = windowInsets,
        containerColor = containerColor,
        arrangement = if (isMedium) {
            ShortNavigationBarArrangement.Centered
        } else {
            ShortNavigationBarArrangement.EqualWeight
        },
    ) {
        nuistTabs.forEachIndexed { index, tab ->
            ShortNavigationBarItem(
                selected = currentPage == index,
                onClick = { onPageChanged(index) },
                iconPosition = if (isMedium) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                icon = {
                    SymbolIcon(
                        name = tab.icon,
                        filled = currentPage == index,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(text = tab.label) },
            )
        }
    }
}

@Composable
private fun ColumnNavigation(
    windowInsets: WindowInsets,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
) {
    val state = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()

    WideNavigationRail(
        state = state,
        windowInsets = windowInsets,
        colors = WideNavigationRailColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modalContainerColor = WideNavigationRailDefaults.colors().modalContainerColor,
            modalScrimColor = WideNavigationRailDefaults.colors().modalScrimColor,
            modalContentColor = WideNavigationRailDefaults.colors().modalContentColor,
        ),
        header = {
            IconButton(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .semantics {
                        stateDescription =
                            if (state.currentValue == WideNavigationRailValue.Expanded) {
                                "Expanded"
                            } else {
                                "Collapsed"
                            }
                    },
                onClick = {
                    scope.launch {
                        if (state.targetValue == WideNavigationRailValue.Expanded) {
                            state.collapse()
                        } else {
                            state.expand()
                        }
                    }
                },
            ) {
                if (state.targetValue == WideNavigationRailValue.Expanded) {
                    Icon(AppIcons.MenuOpen, "Collapse rail")
                } else {
                    Icon(AppIcons.Menu, "Expand rail")
                }
            }
        },
    ) {
        nuistTabs.forEachIndexed { index, tab ->
            WideNavigationRailItem(
                railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                selected = currentPage == index,
                onClick = { onPageChanged(index) },
                icon = {
                    SymbolIcon(
                        name = tab.icon,
                        filled = currentPage == index,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(text = tab.label) },
            )
        }
    }
}
