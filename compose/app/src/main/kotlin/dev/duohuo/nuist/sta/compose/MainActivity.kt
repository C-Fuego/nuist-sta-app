package dev.duohuo.nuist.sta.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import dev.duohuo.nuist.sta.compose.ui.SymbolIcon
import dev.duohuo.nuist.sta.compose.ui.library.FloatingBottomBar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.features.bulletin.Bulletin
import dev.duohuo.nuist.sta.compose.features.bulletin.BulletinDetailScreen
import dev.duohuo.nuist.sta.compose.features.bulletin.BulletinListScreen
import dev.duohuo.nuist.sta.compose.features.bulletin.BulletinMock
import dev.duohuo.nuist.sta.compose.features.campus_map.CampusMapScreen
import dev.duohuo.nuist.sta.compose.features.credits.InnovationCreditScreen
import dev.duohuo.nuist.sta.compose.features.credits.LaborScoreScreen
import dev.duohuo.nuist.sta.compose.features.electricity.ElectricityScreen
import dev.duohuo.nuist.sta.compose.features.free_classroom.FreeClassroomScreen
import dev.duohuo.nuist.sta.compose.features.score.ScoreScreen
import dev.duohuo.nuist.sta.compose.features.study.AcademicSection
import dev.duohuo.nuist.sta.compose.features.study.StudentInfoSection
import dev.duohuo.nuist.sta.compose.features.study.StudyMock
import dev.duohuo.nuist.sta.compose.features.timetable.TimetableScreen

/**
 * Compose 影子入口（A2 双入口起步）。
 *
 * 导航结构对齐原 App：底部三页签（首页 / 学习 / 我的）；首页宫格进入各小程序，
 * 小程序为全屏页面栈（带返回）。UI 优先阶段数据均为 mock。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // 主题设置持久化：开机从 SharedPreferences 恢复，每次改动整包落盘
            val context = androidx.compose.ui.platform.LocalContext.current
            val initial = remember {
                dev.duohuo.nuist.sta.compose.core.storage.ThemePrefs.load(
                    context,
                    defaults = dev.duohuo.nuist.sta.compose.core.storage.ThemePrefsSnapshot(
                        seedArgb = dev.duohuo.nuist.sta.compose.core.theme.material.PresetColors
                            .first().color.toArgb(),
                        paletteStyleName = dev.duohuo.nuist.sta.compose.core.theme.material.PaletteStyle.TonalSpot.name,
                        colorSpecName = dev.duohuo.nuist.sta.compose.core.theme.material.ThemeColorSpec.SPEC_2025.name,
                        dynamicColor = false,
                        darkModeIndex = 0,
                        useAmoled = false,
                        bottomBarFloating = false,
                        pbAnimName = dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackAnimation.AOSP.name,
                        pbDirName = dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackExitDirection.FOLLOW_GESTURE.name,
                    ),
                )
            }
            // 存储的种子色若已不在预设里（预设调整过），回退到首个预设
            var seedColor by remember {
                val preset = dev.duohuo.nuist.sta.compose.core.theme.material.PresetColors
                    .firstOrNull { it.color.toArgb() == initial.seedArgb }
                mutableStateOf(preset?.color ?: dev.duohuo.nuist.sta.compose.core.theme.material.PresetColors.first().color)
            }
            var paletteStyle by remember {
                mutableStateOf(
                    dev.duohuo.nuist.sta.compose.core.theme.material.PaletteStyle.fromValueOrDefault(
                        initial.paletteStyleName,
                    ),
                )
            }
            var colorSpec by remember {
                mutableStateOf(
                    dev.duohuo.nuist.sta.compose.core.theme.material.ThemeColorSpec.fromValueOrDefault(
                        initial.colorSpecName,
                    ),
                )
            }
            var dynamicColor by remember { mutableStateOf(initial.dynamicColor) }
            var useAmoled by remember { mutableStateOf(initial.useAmoled) }
            var bottomBarFloating by remember { mutableStateOf(initial.bottomBarFloating) }
            var darkModeIndex by remember { mutableIntStateOf(initial.darkModeIndex) }
            var pbAnim by remember {
                mutableStateOf(
                    dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackAnimation
                        .valueOf(initial.pbAnimName),
                )
            }
            var pbDir by remember {
                mutableStateOf(
                    dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackExitDirection
                        .valueOf(initial.pbDirName),
                )
            }
            var showThemeSettings by remember { mutableStateOf(false) }

            fun persist() {
                dev.duohuo.nuist.sta.compose.core.storage.ThemePrefs.save(
                    context,
                    seedArgb = seedColor.toArgb(),
                    paletteStyleName = paletteStyle.name,
                    colorSpecName = colorSpec.name,
                    dynamicColor = dynamicColor,
                    darkModeIndex = darkModeIndex,
                    useAmoled = useAmoled,
                    bottomBarFloating = bottomBarFloating,
                    pbAnimName = pbAnim.name,
                    pbDirName = pbDir.name,
                )
            }
            val dark = when (darkModeIndex) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            dev.duohuo.nuist.sta.compose.core.theme.Md3eTheme(
                dark = dark,
                seedColor = seedColor,
                paletteStyle = paletteStyle,
                colorSpec = colorSpec,
                dynamicColor = dynamicColor,
                useAmoled = useAmoled,
            ) {
                AppShell(
                    pbAnim = pbAnim,
                    pbDir = pbDir,
                    bottomBarFloating = bottomBarFloating,
                    onOpenThemeSettings = { showThemeSettings = true },
                )
                if (showThemeSettings) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        dev.duohuo.nuist.sta.compose.core.theme.ThemeSettingsScreen(
                            seedColor = seedColor,
                            onSeedColorChange = { seedColor = it; persist() },
                            paletteStyle = paletteStyle,
                            onPaletteStyleChange = { paletteStyle = it; persist() },
                            colorSpec = colorSpec,
                            onColorSpecChange = { colorSpec = it; persist() },
                            darkModeIndex = darkModeIndex,
                            onDarkModeChange = { darkModeIndex = it; persist() },
                            dynamicColor = dynamicColor,
                            onDynamicColorChange = { dynamicColor = it; persist() },
                            useAmoled = useAmoled,
                            onUseAmoledChange = { useAmoled = it; persist() },
                            bottomBarFloating = bottomBarFloating,
                            onBottomBarFloatingChange = { bottomBarFloating = it; persist() },
                            pbAnim = pbAnim,
                            onPbAnimChange = { pbAnim = it; persist() },
                            pbDir = pbDir,
                            onPbDirChange = { pbDir = it; persist() },
                            onBack = { showThemeSettings = false },
                        )
                    }
                }
            }
        }
    }
}

/** 小程序入口（宫格项）。 */
private data class AppEntry(val id: String, val label: String, val icon: ImageVector)

private val appEntries = listOf(
    AppEntry("free-classroom", "空教室", Icons.Filled.MeetingRoom),
    AppEntry("electricity", "电费", Icons.Filled.Bolt),
    AppEntry("score", "成绩", Icons.Filled.Assessment),
    AppEntry("credits", "双创", Icons.Filled.Lightbulb),
    AppEntry("labor", "劳动", Icons.Filled.Work),
    AppEntry("bulletin", "公告", Icons.Filled.Campaign),
    AppEntry("timetable", "课表", Icons.Filled.CalendarMonth),
    AppEntry("map", "地图", Icons.Filled.Map),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(
    pbAnim: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackAnimation,
    pbDir: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackExitDirection,
    bottomBarFloating: Boolean,
    onOpenThemeSettings: () -> Unit,
) {
    var openedApp by remember { mutableStateOf<AppEntry?>(null) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 3 }

    // 主界面 ↔ 全屏小程序：进入自下而上滑入+淡入（M3E spring）；
    // 退出动画由 PredictiveBackPage 的预测性返回手势承担，此处瞬时切换。
    AnimatedContent(
        targetState = openedApp,
        transitionSpec = {
            if (targetState != null) {
                // 与 InstallerX 页面进入一致：纯淡入
                fadeIn(spring(dampingRatio = 1f, stiffness = 380f)) togetherWith ExitTransition.None
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "app-shell",
    ) { opened ->
        if (opened != null) {
            // 全屏小程序：预测性返回 + 大屏限宽。
            dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackPage(
                animation = pbAnim,
                direction = pbDir,
                onBack = { openedApp = null },
            ) {
                ContentMaxWidth {
                androidx.compose.runtime.CompositionLocalProvider(
                    dev.duohuo.nuist.sta.compose.core.theme.LocalAppBack provides { openedApp = null },
                ) {
                AnimatedContent(
                    targetState = opened.id,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "app-open",
                ) { id ->
                    when (id) {
                        "free-classroom" -> FreeClassroomScreen()
                        "electricity" -> ElectricityScreen()
                        "score" -> ScoreScreen()
                        "credits" -> InnovationCreditScreen()
                        "labor" -> LaborScoreScreen()
                        "bulletin" -> BulletinTab(onBack = { openedApp = null })
                        "timetable" -> TimetableScreen()
                        else -> CampusMapScreen()
                    }
                }
                }
                }
            }
        } else {
            val destinations = listOf(
        TabDest("首页", "home"),
        TabDest("学习", "school"),
        TabDest("我的", "person"),
    )
    val select: (Int) -> Unit = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } }
    val openApp: (AppEntry) -> Unit = { openedApp = it }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            // 宽屏：左侧 WideNavigationRail（可展开，照搬 InstallerX 的 header 展开按钮）
            val railState = rememberWideNavigationRailState()
            Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                WideNavigationRail(
                    state = railState,
                    header = {
                        IconButton(
                            modifier = Modifier.padding(start = 24.dp),
                            onClick = {
                                scope.launch {
                                    if (railState.targetValue == WideNavigationRailValue.Expanded) {
                                        railState.collapse()
                                    } else {
                                        railState.expand()
                                    }
                                }
                            },
                        ) {
                            if (railState.targetValue == WideNavigationRailValue.Expanded) {
                                Icon(Icons.Filled.MenuOpen, contentDescription = "收起导航栏")
                            } else {
                                Icon(Icons.Filled.Menu, contentDescription = "展开导航栏")
                            }
                        }
                    },
                ) {
                    destinations.forEachIndexed { idx, d ->
                        val selected = pagerState.currentPage == idx
                        WideNavigationRailItem(
                            railExpanded = railState.targetValue == WideNavigationRailValue.Expanded,
                            selected = selected,
                            onClick = { select(idx) },
                            icon = { SymbolIcon(d.symbol, filled = selected) },
                            label = { Text(d.label) },
                        )
                    }
                }
                ContentMaxWidth(Modifier.weight(1f)) {
                    TabPager(pagerState, openApp, pbAnim, pbDir, onOpenThemeSettings)
                }
            }
        } else if (bottomBarFloating) {
            // 页面底色与 InstallerX 一致：surfaceContainer
            // 紧凑 + 悬浮底栏：内容全尺寸铺满、从胶囊两侧穿过（真悬浮），
            // 底栏作为 overlay 浮在其上（组件为移植的 FloatingBottomBar，None 纯色模式）
            Box(Modifier.fillMaxSize()) {
                TabPager(pagerState, openApp, pbAnim, pbDir, onOpenThemeSettings)
                FloatingBottomBar(
                    items = destinations,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                    selectedIndex = { pagerState.settledPage },
                    onSelected = { index -> select(index) },
                    iconContent = { tab, index, isActiveLayer ->
                        SymbolIcon(tab.symbol, filled = isActiveLayer)
                    },
                    labelContent = { tab, _ ->
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    },
                )
            }
            } else {
                // 紧凑：底部 ShortNavigationBar，可横向滑动切页。
                // contentWindowInsets 清零（照搬 InstallerX）：状态栏 inset 由内层
                // 页面自己的顶栏处理，否则双重叠加导致顶部空隙过大
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        ShortNavigationBar {
                            destinations.forEachIndexed { idx, d ->
                                val selected = pagerState.currentPage == idx
                                ShortNavigationBarItem(
                                    selected = selected,
                                    onClick = { select(idx) },
                                    icon = { SymbolIcon(d.symbol, filled = selected) },
                                    label = { Text(d.label) },
                                )
                            }
                        }
                    },
                ) { inner ->
                    Box(Modifier.fillMaxSize().padding(inner)) {
                        TabPager(pagerState, openApp, pbAnim, pbDir, onOpenThemeSettings)
                    }
                }
            }
        }
    }
}

}

private data class TabDest(val label: String, val symbol: String)

/** 三页签横向分页：支持左右滑动切换 + 点击导航滚动到对应页。 */
@Composable
private fun TabPager(
    pagerState: PagerState,
    onOpen: (AppEntry) -> Unit,
    pbAnim: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackAnimation,
    pbDir: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackExitDirection,
    onOpenThemeSettings: () -> Unit,
    outerPadding: PaddingValues = PaddingValues(0.dp),
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
        // 保留全部页签组合：切换 tab 不触发整页重组（消除悬浮底栏点击切换卡顿）
        beyondViewportPageCount = 2,
    ) { page ->
        when (page) {
            0 -> HomeTab(onOpen = onOpen, outerPadding = outerPadding)
            1 -> StudyTab(
                onOpen = { id -> appEntries.find { it.id == id }?.let { onOpen(it) } },
                outerPadding = outerPadding,
            )
            else -> ProfileTab(onOpenThemeSettings = onOpenThemeSettings, outerPadding = outerPadding)
        }
    }
}

/**
 * 大屏内容居中限宽：避免卡片/列表在宽屏上拉成超长横条（M3E 规范要求）。
 *
 * 自适应限宽：内容宽 = 可用宽度 − 96dp，上限 1200dp——
 * 手机/平板窄屏不受影响，宽屏两侧只留窄边，不再「缩在中间」。
 */
@Composable
private fun ContentMaxWidth(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (maxWidth > 840.dp) {
            val contentWidth = (maxWidth - 96.dp).coerceAtMost(1200.dp)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.width(contentWidth).fillMaxHeight()) { content() }
            }
        } else {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTab(onOpen: (AppEntry) -> Unit, outerPadding: PaddingValues = PaddingValues(0.dp)) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
            // 追加悬浮底栏净空（参照 InstallerX outerPadding 模式）
            contentPadding = PaddingValues(bottom = 24.dp + outerPadding.calculateBottomPadding()),
        ) {
            item { HomeBulletinCard(onOpen = { onOpen(entryById("bulletin")) }) }
            item { HomeElectricityCard(onOpen = { onOpen(entryById("electricity")) }) }
            item { HomeTimetableCard(onOpen = { onOpen(entryById("timetable")) }) }
            item { HomeExamCard() }
            // 快捷入口：与学习页并排卡同款形态，带「校园导览」分组标题
            item {
                Text(
                    "校园导览",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    // 与 SegmentedColumn 分组标题的节奏一致：上方 16dp、下方 16dp
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 16.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    StudyEntry("空教室", Icons.Filled.MeetingRoom, Modifier.weight(1f)) {
                        onOpen(entryById("free-classroom"))
                    }
                    StudyEntry("校园地图", Icons.Filled.Map, Modifier.weight(1f)) {
                        onOpen(entryById("map"))
                    }
                }
            }
        }
    }
}

private fun entryById(id: String): AppEntry = appEntries.first { it.id == id }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyTab(onOpen: (String) -> Unit, outerPadding: PaddingValues = PaddingValues(0.dp)) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
            StudentInfoSection(StudyMock.student)
            AcademicSection(StudyMock.academic)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                StudyEntry("成绩", Icons.Filled.Assessment, Modifier.weight(1f)) { onOpen("score") }
                StudyEntry("双创", Icons.Filled.Lightbulb, Modifier.weight(1f)) { onOpen("credits") }
                StudyEntry("劳动", Icons.Filled.Work, Modifier.weight(1f)) { onOpen("labor") }
            }
        }
    }
}

@Composable
private fun StudyEntry(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.height(72.dp).clip(MaterialTheme.shapes.large)
            .background(scheme.surfaceBright)
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = scheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTab(onOpenThemeSettings: () -> Unit, outerPadding: PaddingValues = PaddingValues(0.dp)) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                .padding(bottom = 24.dp + outerPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegmentedColumn(title = "账号") {
                item {
                    BaseWidget(icon = Icons.Filled.Link, title = "统一门户",
                        description = "绑定后无感使用校级服务", onClick = {}) {
                        _ -> Icon(Icons.Filled.ChevronRight, contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(icon = Icons.Filled.Badge, title = "学生信息",
                        description = "学号、院系与班级", onClick = {}) {
                        _ -> Icon(Icons.Filled.ChevronRight, contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
            }

            SegmentedColumn(title = "设置") {
                item {
                    BaseWidget(icon = Icons.Filled.Palette, title = "主题设置",
                        description = "主题模式、配色、预测性返回", onClick = onOpenThemeSettings) {
                        _ -> Icon(Icons.Filled.ChevronRight, contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(icon = Icons.Filled.BugReport, title = "诊断与日志",
                        description = "导出 / 清除本机诊断日志", onClick = {}) {
                        _ -> Icon(Icons.Filled.ChevronRight, contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
            }

            SegmentedColumn(title = "关于") {
                item {
                    BaseWidget(icon = Icons.Filled.Info, title = "关于 NUIST++",
                        description = "版本 0.1.0-compose", onClick = {}) {
                        _ -> Icon(Icons.Filled.ChevronRight, contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(icon = Icons.Filled.Description, title = "开源许可",
                        description = "第三方库许可列表", onClick = {}) {
                        _ -> Icon(Icons.Filled.ChevronRight, contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BulletinTab(onBack: () -> Unit) {
    var opened by remember { mutableStateOf<Bulletin?>(null) }
    val current = opened
    if (current == null) {
        BulletinListScreen(onOpen = { opened = it })
    } else {
        BulletinDetailScreen(BulletinMock.detail(current), onBack = { opened = null })
    }
}

/** 包一层返回处理：单页小程序用系统返回键回到宫格。 */
@Composable
private fun BackHandlerWrapper(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = true, onBack = onBack)
}
