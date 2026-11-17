package dev.duohuo.nuist.sta.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                    pbAnimName = pbAnim.name,
                    pbDirName = pbDir.name,
                )
            }
            // 任一设置变化后落盘
            LaunchedEffect(
                seedColor, paletteStyle, colorSpec, dynamicColor, darkModeIndex, useAmoled, pbAnim, pbDir,
            ) { persist() }

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
                    onOpenThemeSettings = { showThemeSettings = true },
                )
                if (showThemeSettings) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        dev.duohuo.nuist.sta.compose.core.theme.ThemeSettingsScreen(
                            seedColor = seedColor,
                            onSeedColorChange = { seedColor = it },
                            paletteStyle = paletteStyle,
                            onPaletteStyleChange = { paletteStyle = it },
                            colorSpec = colorSpec,
                            onColorSpecChange = { colorSpec = it },
                            darkModeIndex = darkModeIndex,
                            onDarkModeChange = { darkModeIndex = it },
                            dynamicColor = dynamicColor,
                            onDynamicColorChange = { dynamicColor = it },
                            useAmoled = useAmoled,
                            onUseAmoledChange = { useAmoled = it },
                            pbAnim = pbAnim,
                            onPbAnimChange = { pbAnim = it },
                            pbDir = pbDir,
                            onPbDirChange = { pbDir = it },
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
    onOpenThemeSettings: () -> Unit,
) {
    var openedApp by remember { mutableStateOf<AppEntry?>(null) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 3 }

    val opened = openedApp
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
        return
    }

    val destinations = listOf(
        TabDest("首页", Icons.Filled.Home),
        TabDest("学习", Icons.Filled.School),
        TabDest("我的", Icons.Filled.Person),
    )
    val select: (Int) -> Unit = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } }
    val openApp: (AppEntry) -> Unit = { openedApp = it }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 600.dp
        if (wide) {
            // 宽屏：左侧 WideNavigationRail，内容居中限宽，可横向滑动切页
            Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                WideNavigationRail {
                    destinations.forEachIndexed { idx, d ->
                        WideNavigationRailItem(
                            railExpanded = false,
                            selected = pagerState.currentPage == idx,
                            onClick = { select(idx) },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label) },
                        )
                    }
                }
                ContentMaxWidth(Modifier.weight(1f)) {
                    TabPager(pagerState, openApp, pbAnim, pbDir, onOpenThemeSettings)
                }
            }
        } else {
            // 紧凑：底部 ShortNavigationBar，可横向滑动切页
            Scaffold(
                bottomBar = {
                    ShortNavigationBar {
                        destinations.forEachIndexed { idx, d ->
                            ShortNavigationBarItem(
                                selected = pagerState.currentPage == idx,
                                onClick = { select(idx) },
                                icon = { Icon(d.icon, contentDescription = d.label) },
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

private data class TabDest(val label: String, val icon: ImageVector)

/** 三页签横向分页：支持左右滑动切换 + 点击导航滚动到对应页。 */
@Composable
private fun TabPager(
    pagerState: PagerState,
    onOpen: (AppEntry) -> Unit,
    pbAnim: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackAnimation,
    pbDir: dev.duohuo.nuist.sta.compose.core.theme.PredictiveBackExitDirection,
    onOpenThemeSettings: () -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
    ) { page ->
        when (page) {
            0 -> HomeTab(onOpen = onOpen)
            1 -> StudyTab(onOpen = { id -> appEntries.find { it.id == id }?.let { onOpen(it) } })
            else -> ProfileTab(onOpenThemeSettings = onOpenThemeSettings)
        }
    }
}

/** 大屏内容居中限宽：避免卡片/列表在宽屏上拉成超长横条（M3E 规范要求）。 */
@Composable
private fun ContentMaxWidth(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (maxWidth > 840.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.width(840.dp).fillMaxHeight()) { content() }
            }
        } else {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTab(onOpen: (AppEntry) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("NUIST++") },
                subtitle = { Text("查空教室、看成绩、查电费、逛校园") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(appEntries, key = { it.id }) { entry ->
                AppGridItem(entry) { onOpen(entry) }
            }
        }
    }
}

@Composable
private fun AppGridItem(entry: AppEntry, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.large)
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(entry.icon, contentDescription = entry.label, tint = scheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(entry.label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyTab(onOpen: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("学习") },
                subtitle = { Text("学业进度与学习卡片") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
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
        modifier = modifier.height(72.dp).clip(MaterialTheme.shapes.medium)
            .background(scheme.surfaceContainerLow)
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
private fun ProfileTab(onOpenThemeSettings: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("我的") },
                subtitle = { Text("账号、设置与关于") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StudentInfoSection(StudyMock.student)

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
