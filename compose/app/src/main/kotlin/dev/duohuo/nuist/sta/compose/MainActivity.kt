package dev.duohuo.nuist.sta.compose

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import dev.duohuo.nuist.sta.compose.ui.library.FloatingBottomBar
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
                var openedTarget by remember { mutableStateOf<OpenTarget?>(null) }
                AnimatedContent(
                    targetState = openedTarget,
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
                        // 全屏页面：预测性返回（遵循设置的动画档位）+ 大屏限宽。
                        dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackPage(
                            animation = pbAnim,
                            direction = pbDir,
                            onBack = { openedTarget = null },
                        ) {
                            ContentMaxWidth {
                            androidx.compose.runtime.CompositionLocalProvider(
                                dev.duohuo.nuist.sta.compose.core.theme.LocalAppBack provides { openedTarget = null },
                            ) {
                                when (opened) {
                                    is OpenTarget.App -> when (opened.entry.id) {
                                        "free-classroom" -> FreeClassroomScreen()
                                        "electricity" -> ElectricityScreen()
                                        "score" -> ScoreScreen()
                                        "credits" -> InnovationCreditScreen()
                                        "labor" -> LaborScoreScreen()
                                        "bulletin" -> BulletinTab(onBack = { openedTarget = null })
                                        "timetable" -> TimetableScreen()
                                        else -> CampusMapScreen()
                                    }
                                    OpenTarget.ThemeSettings -> androidx.compose.material3.Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
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
                                            onPbAnimChange = { pbAnim = it },
                                            pbDir = pbDir,
                                            onPbDirChange = { pbDir = it },
                                            onBack = { openedTarget = null },
                                        )
                                    }
                                }
                            }
                            }
                        }
                    } else {
                        AppShell(
                            pbAnim = pbAnim,
                            pbDir = pbDir,
                            bottomBarFloating = bottomBarFloating,
                            onOpen = { openedTarget = OpenTarget.App(it) },
                            onOpenThemeSettings = { openedTarget = OpenTarget.ThemeSettings },
                        )
                    }
                }
            }
        }
    }
}

/** 从主界面打开的目标：小程序或主题设置（共享同一预测性返回通道）。 */
private sealed interface OpenTarget {
    data class App(val entry: AppEntry) : OpenTarget
    data object ThemeSettings : OpenTarget
}

/** 小程序入口（宫格项）。 */
private data class AppEntry(val id: String, val label: String, val icon: String)

private val appEntries = listOf(
    AppEntry("free-classroom", "空教室", "meeting_room"),
    AppEntry("electricity", "电费", "bolt"),
    AppEntry("score", "成绩", "analytics"),
    AppEntry("credits", "双创", "lightbulb"),
    AppEntry("labor", "劳动", "work"),
    AppEntry("bulletin", "公告", "campaign"),
    AppEntry("timetable", "课表", "calendar_month"),
    AppEntry("map", "地图", "map"),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(
    pbAnim: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackAnimation,
    pbDir: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackExitDirection,
    bottomBarFloating: Boolean,
    onOpen: (AppEntry) -> Unit,
    onOpenThemeSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 3 }

    val destinations = listOf(
        TabDest("首页", "home"),
        TabDest("学习", "school"),
        TabDest("我的", "person"),
    )
    val select: (Int) -> Unit = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } }

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
                                SymbolIcon("menu_open", contentDescription = "收起导航栏")
                            } else {
                                SymbolIcon("menu", contentDescription = "展开导航栏")
                            }
                        }
                    },
                ) {
                    destinations.forEachIndexed { idx, d ->
                        val selected = pagerState.targetPage == idx
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
                    TabPager(pagerState, onOpen, pbAnim, pbDir, onOpenThemeSettings)
                }
            }
        } else if (bottomBarFloating) {
            // 页面底色与 InstallerX 一致：surfaceContainer
            // 紧凑 + 悬浮底栏：内容全尺寸铺满、从胶囊两侧穿过（真悬浮），
            // 底栏作为 overlay 浮在其上（组件为移植的 FloatingBottomBar，None 纯色模式）
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Box(Modifier.fillMaxWidth()) {
                        FloatingBottomBar(
                            items = destinations,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                            selectedIndex = { pagerState.targetPage },
                            onSelected = { index -> select(index) },
                            iconContent = { tab, index, isActiveLayer ->
                                SymbolIcon(
                                    tab.symbol,
                                    filled = isActiveLayer || index == pagerState.targetPage,
                                )
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
                },
            ) { inner ->
                TabPager(
                    pagerState, onOpen, pbAnim, pbDir, onOpenThemeSettings,
                    outerPadding = inner,
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
                                val selected = pagerState.targetPage == idx
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
                        TabPager(pagerState, onOpen, pbAnim, pbDir, onOpenThemeSettings)
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
private fun ContentMaxWidth(
    modifier: Modifier = Modifier,
    background: Color? = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().let { m ->
        if (background != null) m.background(background) else m
    }) {
        if (maxWidth > 840.dp) {
            val contentWidth = (maxWidth - 32.dp).coerceAtMost(1600.dp)
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
        // 首次进入/刷新时的骨架屏（数据层接入后换成真实 loading）
        var loading by rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(500)
            loading = false
        }
        if (loading) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = maxOf(24.dp, outerPadding.calculateBottomPadding()),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(4) { HomeCardSkeleton() }
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            // 悬浮底栏净空取 max：底栏高度替代页面固定留白，避免叠加过大
            contentPadding = PaddingValues(
                bottom = maxOf(24.dp, outerPadding.calculateBottomPadding()),
            ),
        ) {
            // 组 1：今日概览（下一节课 / 电费 / 待考）
            item {
                HomeTodaySection(
                    onOpenTimetable = { onOpen(entryById("timetable")) },
                    onOpenElectricity = { onOpen(entryById("electricity")) },
                )
            }
            // 组 2：信息公告（最新三条 + 查看全部）
            item {
                HomeBulletinSection(onOpenAll = { onOpen(entryById("bulletin")) })
            }
            // 组 3：校园导览（并排入口，与学习页入口同款）
            item {
                Text(
                    "校园导览",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 12.dp),
                )
                HomeEntrySection { id -> onOpen(entryById(id)) }
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
        var termIndex by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
        var showTermDialog by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        if (showTermDialog) {
            dev.duohuo.nuist.sta.compose.core.theme.settings.SingleChoiceDialog(
                title = "切换学期",
                items = StudyMock.terms.map { it.label },
                selectedIndex = termIndex,
                onDismiss = { showTermDialog = false },
                onSelect = { index ->
                    showTermDialog = false
                    termIndex = index
                },
            )
        }
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(bottom = maxOf(16.dp, outerPadding.calculateBottomPadding())),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val term = StudyMock.terms[termIndex.coerceIn(0, StudyMock.terms.lastIndex)]
            StudentInfoSection(
                info = StudyMock.student.copy(semester = term.label),
                onSemesterClick = { showTermDialog = true },
            )
            AcademicSection(term.academic)
            // 常用功能：与首页「校园导览」共用 EntryCard，分组标题节奏一致
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
                    dev.duohuo.nuist.sta.compose.core.theme.settings.EntryCard(
                        "analytics", "成绩", Modifier.weight(1f),
                    ) { onOpen("score") }
                    dev.duohuo.nuist.sta.compose.core.theme.settings.EntryCard(
                        "lightbulb", "双创", Modifier.weight(1f),
                    ) { onOpen("credits") }
                    dev.duohuo.nuist.sta.compose.core.theme.settings.EntryCard(
                        "work", "劳动", Modifier.weight(1f),
                    ) { onOpen("labor") }
                }
            }
        }
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
                .padding(bottom = maxOf(24.dp, outerPadding.calculateBottomPadding())),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegmentedColumn(title = "账号") {
                item {
                    BaseWidget(icon = "link", title = "统一门户",
                        description = "绑定后无感使用校级服务", onClick = {}) {
                        _ -> SymbolIcon("chevron_right", contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(icon = "badge", title = "学生信息",
                        description = "学号、院系与班级", onClick = {}) {
                        _ -> SymbolIcon("chevron_right", contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
            }

            SegmentedColumn(title = "设置") {
                item {
                    BaseWidget(icon = "palette", title = "主题设置",
                        description = "主题模式、配色、预测性返回", onClick = onOpenThemeSettings) {
                        _ -> SymbolIcon("chevron_right", contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(icon = "bug_report", title = "诊断与日志",
                        description = "导出 / 清除本机诊断日志", onClick = {}) {
                        _ -> SymbolIcon("chevron_right", contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
            }

            SegmentedColumn(title = "关于") {
                item {
                    BaseWidget(icon = "info", title = "关于 NUIST++",
                        description = "版本 0.1.0-compose", onClick = {}) {
                        _ -> SymbolIcon("chevron_right", contentDescription = null,
                            tint = scheme.onSurfaceVariant)
                    }
                }
                item {
                    BaseWidget(icon = "description", title = "开源许可",
                        description = "第三方库许可列表", onClick = {}) {
                        _ -> SymbolIcon("chevron_right", contentDescription = null,
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

/** 首页卡骨架（数据层未接入时的加载占位；M3 无 skeleton 组件，用色调块 + 呼吸动画）。 */
@Composable
private fun HomeCardSkeleton() {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    Column(Modifier.padding(horizontal = 16.dp)) {
        Box(
            Modifier
                .padding(start = 12.dp, bottom = 8.dp)
                .size(width = 72.dp, height = 12.dp)
                .clip(MaterialTheme.shapes.small)
                .background(scheme.surfaceContainerHighest.copy(alpha = alpha)),
        )
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            color = scheme.surfaceBright,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(scheme.surfaceContainerHighest.copy(alpha = alpha)),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.75f)
                            .height(14.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(scheme.surfaceContainerHighest.copy(alpha = alpha)),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(11.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(scheme.surfaceContainerHighest.copy(alpha = alpha)),
                    )
                }
            }
        }
    }
}
