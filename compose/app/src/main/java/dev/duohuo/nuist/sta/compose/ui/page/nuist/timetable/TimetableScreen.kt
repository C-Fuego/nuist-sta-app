package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import dev.duohuo.nuist.sta.compose.data.exam.ExamItem
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.ui.kit.ExpressiveBackButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.ui.kit.BaseItemContainer
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.BlankStateArea
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorBanner
import dev.duohuo.nuist.sta.compose.ui.kit.ErrorState
import dev.duohuo.nuist.sta.compose.ui.kit.LocalAppBack
import dev.duohuo.nuist.sta.compose.ui.kit.LocalRefreshFromPull
import dev.duohuo.nuist.sta.compose.ui.kit.NuistPullToRefreshBox
import dev.duohuo.nuist.sta.compose.data.timetable.CourseFormInput
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettings
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableCellDetail
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableEvent
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableWeek
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import dev.duohuo.nuist.sta.compose.ui.kit.TimetableSkeleton
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * 课程块配色：直接用种子色派生出的 `colorScheme` 容器角色（7 组循环）。
 *
 * 与参考实现一致——它只用 materialkolor 生成 ColorScheme，不手搓色调板；
 * 这里也只取 role，保证跟用户选的色板完全一致、不过饱和。
 * 不留 `errorContainer`：错误色是语义色，跟其它课程块差异过大，混在课表里很刺眼。
 */
@Composable
private fun courseColors(): List<Pair<Color, Color>> {
    val s = MaterialTheme.colorScheme
    // Dart 侧色号是 0–15（kCoursePaletteSize = 16），这里也准备 16 组，做到一一对应、互不撞色。
    // 三个 container 色相各取 5 个明度档（更浅 → 更深，深的往 onContainer 混），再加一个中性灰。
    val anchors = listOf(
        Triple(s.primaryContainer, s.onPrimaryContainer, s.surface),
        Triple(s.secondaryContainer, s.onSecondaryContainer, s.surface),
        Triple(s.tertiaryContainer, s.onTertiaryContainer, s.surface),
    )
    val tones = listOf(0.55f, 0.25f, 0f).map { toSurface ->
        anchors.map { (container, _) -> androidx.compose.ui.graphics.lerp(container, s.surface, toSurface) }
    } + listOf(0.22f, 0.45f).map { toDeep ->
        anchors.map { (container, onContainer) -> androidx.compose.ui.graphics.lerp(container, onContainer, toDeep) }
    }
    // 按「色相交错」铺开：浅蓝、浅紫、浅绿、再深一档……相邻色号尽量不同色相。
    val hues = mutableListOf<Color>()
    tones.forEach { row -> hues.addAll(row) }
    val palette = (hues + s.surfaceContainerHighest).map { bg -> onColorFor(bg) to bg }
    return palette
}
private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")

/** 列表视图里「星期几」的写法，由设置 `dayLabelStyle` 决定。 */
private fun dayLabel(day: Int, style: Int): String {
    val name = dayNames.getOrElse(day - 1) { "" }
    return when (style) {
        1 -> "周$name"
        2 -> "星期$name"
        else -> name
    }
}

/** 按背景亮度自动选白字或深字：考试色可以自定义，浅底必须换深色字才看得清。 */
private fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF1B1B1B) else Color.White

/**
 * 课程卡片的前景 / 背景色。
 *
 * 自选色（[CourseBlock.userColor]）是课程级设置，原样使用；
 * 否则用 Dart 控制器算好的全学期色号 [CourseBlock.colorIndex]，
 * 同一门课在每一周都是同一个颜色，不会因为翻周重新分配。
 */
private fun blockColors(
    block: CourseBlock,
    palette: List<Pair<Color, Color>>,
): Pair<Color, Color> {
    val user = block.userColor
    if (user != null) {
        val background = Color(user)
        return onColorFor(background) to background
    }
    return palette[block.colorIndex.mod(palette.size)]
}

/**
 * 课表（Compose + MD3E，只读周视图）。对应 Flutter `WeekGrid` 的只读呈现。
 *
 * 数据来自教务处（`TimetableApi`），加载失败时给出原因与重试；
 * 周次/列表切换都在内存里做，不会重新请求。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetableScreen(
    onOpenPortal: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    /** 从考试安排页跳进来时直接定位到的周次；为空则停在当前周。 */
    initialWeek: Int? = null,
    /** 从考试安排页跳进来时要滚动到的节次（1 起）。 */
    scrollToNode: Int? = null,
    /** 从考试安排页跳进来时要临时显示并描边的考试；为空则只做定位。 */
    focusExamKey: String? = null,
    /** 从首页点某节课跳进来时要描边的课程格（`day|startNode|endNode`）。 */
    focusCourseKey: String? = null,
    viewModel: TimetableViewModel = koinInject(),
    settingsViewModel: TimetableSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    // enterAlways：往下滑收起大标题后，一往上滑就立刻展开（exitUntilCollapsed 要滑回顶部才展开）
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbar = remember { SnackbarHostState() }
    // 非空时打开「新建课程」表单；值是要预选的星期（0 = 交给内容区按可见列挑）。
    var createRequest by remember { mutableStateOf<Int?>(null) }
    // 点「＋」先选要添加的东西：课程 / 考试 / 其他日程。
    var addSheet by remember { mutableStateOf(false) }
    // 非空时打开「新建考试 / 日程」表单；值是类型（"exam" / "other"）。
    var eventRequest by remember { mutableStateOf<String?>(null) }
    // 打开「导出到 WakeUp」整页。
    var showExport by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        val message = state.actionMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeActionMessage()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("课表", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = {
                    Row(
                        modifier = Modifier.padding(start = 16.dp).height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val back = LocalAppBack.current
                        if (back != null) ExpressiveBackButton(onClick = back)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp).height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExpressiveBackButton(
                            icon = "add",
                            contentDescription = "添加",
                            onClick = { addSheet = true },
                        )
                        ExpressiveBackButton(
                            icon = "ios_share",
                            contentDescription = "导出到 WakeUp",
                            onClick = { showExport = true },
                        )
                        ExpressiveBackButton(
                            icon = "settings",
                            contentDescription = "课表设置",
                            onClick = onOpenSettings,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        val snapshot = state.snapshot
        NuistPullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
        Column(Modifier.fillMaxSize()) {
            when {
                snapshot == null && state.isLoading && !LocalRefreshFromPull.current -> TimetableSkeleton()
                snapshot == null -> BlankStateArea {
                    ErrorState(
                        message = state.error ?: "课表加载失败",
                        title = if (state.needsCredential) "需要登录统一门户" else "课表加载失败",
                        needsCredential = state.needsCredential,
                        onRetry = { viewModel.refresh() },
                        onOpenPortal = onOpenPortal,
                    )
                }
                else -> {
                    state.error?.let { failure ->
                        ErrorBanner(
                            message = failure,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    TimetableContent(
                        snapshot = snapshot,
                        dataMonthLabel = state.week?.monthLabel ?: 0,
                        listMode = settings.listMode,
                        settings = settings,
                        exams = state.exams,
                        initialWeek = initialWeek,
                        scrollToNode = scrollToNode,
                        focusExamKey = focusExamKey,
                        focusCourseKey = focusCourseKey,
                        dataWeek = state.dataWeek,
                        dataDates = state.week?.dateOfDay ?: emptyMap(),
                        dataUnplaced = state.week?.unplaced.orEmpty(),
                        weekPending = state.state?.weekPending == true,
                        cachedWeeks = state.cachedWeeks,
                        onWeekChange = viewModel::selectWeek,
                        viewModel = viewModel,
                        createRequest = createRequest,
                        onCreateHandled = { createRequest = null },
                        eventRequest = eventRequest,
                        onEventHandled = { eventRequest = null },
                    )
                }
            }
        }
        }
    }

    if (addSheet) {
        ModalBottomSheet(
            onDismissRequest = { addSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    "添加到课表",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                StackPickRow("课程", "按星期和节次排在格子里") {
                    addSheet = false
                    createRequest = 0
                }
                StackPickRow("考试", "填日期和时间，按时间落进格子里") {
                    addSheet = false
                    eventRequest = "exam"
                }
                StackPickRow("其他日程", "讲座、活动之类，不在课表格子里") {
                    addSheet = false
                    eventRequest = "other"
                }
            }
        }
    }

    if (showExport) {
        TimetableExportPage(
            viewModel = viewModel,
            onDismiss = { showExport = false },
        )
    }
}

@Composable
private fun TimetableContent(
    snapshot: TimetableSnapshot,
    listMode: Boolean,
    settings: TimetableSettings,
    exams: List<ExamItem>,
    initialWeek: Int?,
    scrollToNode: Int?,
    focusExamKey: String? = null,
    focusCourseKey: String? = null,
    onWeekChange: (Int) -> Unit = {},
    /** 当前这批格子属于哪一周；与选中周短暂不同步时不要拿它过滤。 */
    dataWeek: Int = 0,
    /** 预取好的相邻周，滑动时铺在当前网格两侧，形成连续（伪无限）的网格。 */
    cachedWeeks: Map<Int, TimetableWeek> = emptyMap(),
    /** 当前这批格子所属周的月份，表头交叉格显示它。 */
    dataMonthLabel: Int = 0,
    /** 当前这批格子所属周里「星期几 → 日期」，长按表头整天调课要用它。 */
    dataDates: Map<Int, String> = emptyMap(),
    dataUnplaced: List<TimetableEvent> = emptyList(),
    weekPending: Boolean = false,
    syncFailed: Boolean = false,
    viewModel: TimetableViewModel,
    createRequest: Int? = null,
    onCreateHandled: () -> Unit = {},
    /** 非空时打开「新建考试 / 日程」表单；值是类型（"exam" / "other"）。 */
    eventRequest: String? = null,
    onEventHandled: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val periodW = 52.dp
    val cellH = settings.cellHeightDp.dp
    val headerH = 44.dp
    val periods = snapshot.periods.ifEmpty { TimetableMock.periodTimes }
    var week by rememberSaveable(snapshot.termCode) {
        mutableIntStateOf((initialWeek ?: snapshot.currentWeek).coerceIn(1, snapshot.totalWeeks))
    }
    val today = remember { java.time.LocalDate.now() }
    // 从考试安排页点进来的那场考试：教务不给 id，用「课程码 + 日期 + 开始时间」认出来。
    val focusedExam = remember(exams, focusExamKey) {
        focusExamKey?.let { key -> exams.firstOrNull { examKey(it) == key } }
    }
    fun examWeekOf(exam: dev.duohuo.nuist.sta.compose.data.exam.ExamItem): Int =
        if (exam.week > 0) {
            exam.week
        } else {
            ExamGrid.weekOf(exam.date, today, snapshot.currentWeek, settings.weekStartsMonday)
        }
    // 跳转聚焦的那场考试只在它自己的周里生效（翻到别的周就应该消失）。
    val focusedExamWeek = remember(focusedExam, today, snapshot.currentWeek, settings.weekStartsMonday) {
        focusedExam?.let { examWeekOf(it) }
    }
    // 这一周有考试落在周末时，把那一列临时补上（参考实现：周内有日程就显示对应列）。
    val examWeekDays = remember(exams, week, snapshot.currentWeek, settings.showExams, focusedExam, focusExamKey) {
        val browsable = settings.showExams || focusedExam != null
        if (!browsable) {
            emptySet()
        } else {
            exams.mapNotNull { exam ->
                val isFocused = focusExamKey != null && examKey(exam) == focusExamKey
                val examWeek = examWeekOf(exam)
                if (examWeek == week && (settings.showExams || isFocused)) {
                    exam.date.dayOfWeek.value
                } else {
                    null
                }
            }.toSet()
        }
    }
    // 列集合按「一周起始」排序，再按「显示周六 / 周日」过滤，周末有考试的那列自动补上。
    val days = remember(settings.weekStartsMonday, settings.showSaturday, settings.showSunday, examWeekDays) {
        val order = if (settings.weekStartsMonday) listOf(1, 2, 3, 4, 5, 6, 7) else listOf(7, 1, 2, 3, 4, 5, 6)
        order.filter { day ->
            when (day) {
                6 -> settings.showSaturday || 6 in examWeekDays
                7 -> settings.showSunday || 7 in examWeekDays
                else -> true
            }
        }
    }
    // 浏览周由 Dart 控制器记住（编辑、调课都以它为「本周」），切周时同步过去。
    LaunchedEffect(week) { onWeekChange(week) }
    // 从考试安排页跳进来时把视图滚到考试所在节次：先量出周次卡的实高，再按节次换算。
    val weekScroll = rememberScrollState()
    val cellPx = with(LocalDensity.current) { cellH.toPx() }
    // 只在「从考试页跳进来」时滚一次；翻周不应该把视图再拉回跳转时的位置。
    LaunchedEffect(scrollToNode) {
        if (scrollToNode != null) {
            val target = cellPx * (scrollToNode - 1)
            weekScroll.scrollTo(target.toInt().coerceAtLeast(0))
        }
    }
    val monthLabel = if (week == dataWeek) dataMonthLabel else cachedWeeks[week]?.monthLabel ?: dataMonthLabel
    // 长按表头整天调课要带出这一列的真实日期；翻到相邻周时用那一周的日期。
    val weekDates = if (week == dataWeek) dataDates else cachedWeeks[week]?.dateOfDay ?: dataDates
    // 不落在任何节次内的日程/考试（课间、晚上）：网格里画不下，表头日期下点出清单。
    val weekUnplaced = if (week == dataWeek) dataUnplaced else cachedWeeks[week]?.unplaced.orEmpty()
    val unplacedDays = remember(weekUnplaced) {
        weekUnplaced.mapNotNull { event ->
            runCatching { java.time.LocalDate.parse(event.date).dayOfWeek.value }.getOrNull()
        }.toSet()
    }
    var unplacedDay by remember { mutableStateOf<Int?>(null) }
    // 被整天调课换过的日期：表头标「调」，点一下打开调课页看记录/撤销。
    val swappedDates = remember(snapshot.daySwaps) {
        snapshot.daySwaps.flatMap { swap -> listOf(swap.dateA, swap.dateB) }.toSet()
    }
    val weekSwappedDays = remember(weekDates, swappedDates) {
        weekDates.filterValues { it in swappedDates }.keys
    }
    var daySwapDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    val isCurrentWeek = week == snapshot.currentWeek
    var selectedBlock by remember { mutableStateOf<CourseBlock?>(null) }
    var selectedExam by remember { mutableStateOf<ExamGridItem?>(null) }
    // 同一时段叠了多门课/多场考试时的选择卡：先选要看哪一个，再打开它的详情。
    var stackPick by remember {
        mutableStateOf<Pair<List<CourseBlock>, List<ExamGridItem>>?>(null)
    }
    var showWeekPicker by remember { mutableStateOf(false) }
    // 拖动调课：先记住目标格，问清作用范围再交给 Dart。
    var pendingMove by remember { mutableStateOf<Triple<CourseBlock, Int, Int>?>(null) }
    // 周次集合为空视为「未排课」，不进入周视图；非本周课是否保留由设置决定。
    val weekBlocks = remember(snapshot, week, settings.showNonCurrentWeek, dataWeek) {
        val sourceWeek = if (dataWeek in 1..snapshot.totalWeeks) dataWeek else week
        snapshot.courses.filter {
            it.weeks.isEmpty() || sourceWeek in it.weeks || settings.showNonCurrentWeek
        }
    }

    // 考试不是固定节次：按时间与节次时间的重叠落到覆盖节次上，规则见 ExamGrid。
    fun examsFor(pageWeek: Int) = ExamGrid.itemsFor(
        week = pageWeek,
        currentWeek = snapshot.currentWeek,
        today = today,
        weekStartsMonday = settings.weekStartsMonday,
        days = days,
        exams = exams,
        periods = periods,
        showExams = settings.showExams,
        showNonCurrentWeek = settings.showNonCurrentWeek,
        focusedKey = focusExamKey,
    )

    // 每一页的格子直接用 Dart 给的这一周：翻页中途不会出现「周次变了内容还是上一周」的闪动，
    // 「非本周」灰格也由 Dart 的 activeThisWeek 决定，不再靠名字猜。
    fun blocksFor(pageWeek: Int): List<CourseBlock> {
        cachedWeeks[pageWeek]?.let { return cellsOfWeek(it) }
        return if (pageWeek == dataWeek) weekBlocks else emptyList()
    }

    // 跳转过来的考试描一圈：先亮起，停 1.8 秒后淡出（参考实现同节奏）。
    val highlightAlpha = remember { Animatable(0f) }
    // 数据是异步来的：格子还没出现时先亮着，等考试真的画出来再开始 1.8 秒的计时，
    // 否则计时会在加载期间就走完，跳过来看不到描边。
    LaunchedEffect(focusExamKey, focusedExam, week, focusedExamWeek) {
        if (focusExamKey == null || focusedExamWeek != week) {
            highlightAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        highlightAlpha.snapTo(1f)
        if (focusedExam != null) {
            kotlinx.coroutines.delay(1800)
            highlightAlpha.animateTo(0f, tween(350))
        }
    }

    // 首页点某节课跳过来时同样描一圈，节奏与考试一致。
    val courseFocusTarget = remember(focusCourseKey) {
        focusCourseKey?.split("|")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }
    }
    val courseHighlight = remember { Animatable(0f) }
    LaunchedEffect(focusCourseKey, week, initialWeek, blocksFor(week)) {
        val target = courseFocusTarget
        if (target == null || initialWeek == null || week != initialWeek) {
            courseHighlight.snapTo(0f)
            return@LaunchedEffect
        }
        // 数据是异步来的：格子还没出现时先亮着，等这节课真的画出来再开始计时。
        val drawn = blocksFor(week).any { it.day == target[0] && it.startNode == target[1] }
        courseHighlight.snapTo(1f)
        if (drawn) {
            kotlinx.coroutines.delay(1800)
            courseHighlight.animateTo(0f, tween(350))
        }
    }

    // 学期已经走完（本周超出总周数）时不再提示「非本周」，改为说明课程已结束。
    val endedCurrentWeek = snapshot.currentWeek.takeIf { it > snapshot.totalWeeks }
    val dateLabel = remember(weekDates) {
        val dates = weekDates.values.sorted()
        if (dates.isEmpty()) {
            ""
        } else {
            fun pretty(key: String): String {
                val date = runCatching { java.time.LocalDate.parse(key) }.getOrNull() ?: return key
                return "${date.monthValue}月${date.dayOfMonth}日"
            }
            if (dates.size == 1) pretty(dates.first()) else "${pretty(dates.first())} – ${pretty(dates.last())}"
        }
    }
    val weekCard: @Composable () -> Unit = {
        WeekCard(
            week = week,
            totalWeeks = snapshot.totalWeeks,
            termLabel = snapshot.termLabel,
            currentWeek = snapshot.currentWeek,
            endedCurrentWeek = endedCurrentWeek,
            weekPending = weekPending,
            dateLabel = dateLabel,
            syncFailed = syncFailed,
            onOpenPicker = { showWeekPicker = true },
        )
    }

    if (listMode) {
        Column(Modifier.fillMaxSize()) {
            weekCard()
            // 列表模式同样左右翻周：每一页是一周的教学日列表，相邻页已预取。
            WeekSwipeBox(
                week = week,
                totalWeeks = snapshot.totalWeeks,
                onWeekChange = { week = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { pageWeek, isCurrent, offset ->
                Box(Modifier.fillMaxSize().graphicsLayer { this.translationX = offset() }) {
                    CourseListView(
                        week = pageWeek,
                        blocks = blocksFor(pageWeek),
                        exams = examsFor(pageWeek),
                        dayOrder = days,
                        settings = settings,
                        header = null,
                        swipeModifier = Modifier,
                        onBlockClick = { if (isCurrent) selectedBlock = it },
                        onExamClick = { if (isCurrent) selectedExam = it },
                    )
                }
            }
        }
    } else {
        // 网格一直画到屏幕最底端（盖住三点导航条上方那条留白），所以这里不加导航栏内边距。
        Column(Modifier.fillMaxSize()) {
            weekCard()
            // 表头固定：切周时只有数字变，星期几不跟着平移。
            Row(Modifier.fillMaxWidth().height(headerH)) {
                // 交叉格放月份，且这里不再画星期数左侧的竖线。
                Box(Modifier.width(periodW).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (monthLabel in 1..12) {
                        Text(
                            "${monthLabel}月",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
                WeekHeader(
                    height = headerH,
                    days = days,
                    highlightToday = isCurrentWeek,
                    currentDay = snapshot.currentDay,
                    headerTextSize = settings.headerTextSize,
                    dateOfDay = weekDates,
                    onDayLongPress = { daySwapDate = it },
                    unplacedDays = unplacedDays,
                    onUnplacedClick = { unplacedDay = it },
                    swappedDays = weekSwappedDays,
                    onSwappedClick = { day ->
                        weekDates[day]?.let { key ->
                            runCatching { java.time.LocalDate.parse(key) }.getOrNull()
                        }?.let { daySwapDate = it }
                    },
                )
            }
            // 纵向一起滚：左侧节次栏与右侧网格共用同一个滚动位置。
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                // 视口比课程节次更高时，把网格补到视口底部，表格不会在半截停住。
                val minRows = ceil(maxHeight / cellH).toInt()
                val gridRows = maxOf(periods.size, minRows)
                Column(Modifier.fillMaxSize().verticalScroll(weekScroll)) {
                    Row(Modifier.fillMaxWidth()) {
                        // 节次栏固定：翻周时只有右侧网格平移，它不跟着动。
                        PeriodColumn(
                            periodW = periodW,
                            cellH = cellH,
                            periods = periods,
                            rows = gridRows,
                            showTimes = settings.showPeriodTimes,
                            showGridLines = settings.showGridLines,
                        )
                        VerticalDivider(color = scheme.outlineVariant)
                        // 左右翻周：本周与前后两周三张网格并排（相邻周数据由 ViewModel 预取），
                        // 拖动时三张一起平移、接缝处网格线连续，看起来像一整张网格。
                        WeekSwipeBox(
                            week = week,
                            totalWeeks = snapshot.totalWeeks,
                            onWeekChange = { week = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(cellH * gridRows),
                        ) { pageWeek, isCurrent, offset ->
                            Box(Modifier.fillMaxSize().graphicsLayer { this.translationX = offset() }) {
                                WeekBody(
                                    cellH = cellH,
                                    nodeCount = gridRows,
                                    days = days,
                                    blocks = blocksFor(pageWeek),
                                    exams = examsFor(pageWeek),
                                    settings = settings,
                                    currentWeek = pageWeek,
                                    focusExamKey = focusExamKey,
                                    highlightAlpha = highlightAlpha.value,
                                    focusCourseKey = focusCourseKey,
                                    courseHighlightAlpha = courseHighlight.value,
                                    onExamClick = { if (isCurrent) selectedExam = it },
                                    onBlockClick = { if (isCurrent) selectedBlock = it },
                                    onMove = if (isCurrent) {
                                        { block, day, node -> pendingMove = Triple(block, day, node) }
                                    } else {
                                        { _, _, _ -> }
                                    },
                                    onStackClick = { blocks, events ->
                                        if (isCurrent) stackPick = blocks to events
                                    },
                                    onExamMove = { exam, day, node ->
                                        if (isCurrent) {
                                            viewModel.moveEvent(
                                                id = exam.exam.eventId,
                                                week = pageWeek,
                                                day = day,
                                                startNode = node,
                                                span = exam.endNode - exam.startNode + 1,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                    if (snapshot.unscheduled.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        UnscheduledSection(items = snapshot.unscheduled)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    stackPick?.let { (blocks, events) ->
        StackPickSheet(
            blocks = blocks,
            events = events,
            onDismiss = { stackPick = null },
            onBlock = { block ->
                stackPick = null
                selectedBlock = block
            },
            onEvent = { item ->
                stackPick = null
                selectedExam = item
            },
        )
    }

    // 详情页里点「修改」切到表单：先关掉详情，表单拿同一份格子数据。
    var editing by remember { mutableStateOf<TimetableCellDetail?>(null) }
    selectedBlock?.let { block ->
        if (editing == null) {
            CourseDetailSheet(
                block = block,
                week = week,
                viewModel = viewModel,
                onDismiss = { selectedBlock = null },
                onEdit = { editing = it },
            )
        }
    }
    editing?.let { detail ->
        CourseFormPage(
            initial = CourseFormInput(
                courseId = detail.cell.courseId,
                name = detail.cell.name,
                teacher = detail.cell.teacher,
                location = detail.cell.location,
                note = detail.cell.note,
                kind = detail.cell.kind,
                // 「其他」栏的课没有节次（day = 0），表单从周一的第 1 节起，用户自己指定。
                day = detail.cell.day.takeIf { it in 1..7 } ?: 1,
                startNode = detail.cell.startNode.takeIf { it > 0 } ?: 1,
                endNode = detail.cell.endNode.takeIf { it >= detail.cell.startNode }?.takeIf { it > 0 } ?: 1,
                weeks = detail.cell.weeks.toSet(),
                userColor = detail.cell.userColor,
                // 颜色不在这张表单里改（详情卡的「换个颜色」负责），保存时不带颜色意图。
                colorChanged = false,
            ),
            title = "编辑课程",
            totalWeeks = snapshot.totalWeeks,
            periods = snapshot.periods,
            isEdit = true,
            currentWeek = week,
            conflictsProvider = { value -> viewModel.courseConflicts(value) },
            onDismiss = { editing = null },
            onSubmit = { value, applyWeeks ->
                viewModel.saveCourse(value, week, "all", applyWeeks)
                editing = null
                selectedBlock = null
            },
        )
    }
    if (createRequest != null) {
        // 新建课程：星期默认取当前可见列的第一天，节次落在第一个空格。
        val defaultDay = createRequest.takeIf { it in 1..7 } ?: days.first()
        val taken = weekBlocks.filter { it.day == defaultDay }.map { it.startNode..it.endNode }
        val freeNode = (1..periods.size).firstOrNull { node -> taken.none { node in it } } ?: 1
        CourseFormPage(
            initial = CourseFormInput(
                day = defaultDay,
                startNode = freeNode,
                endNode = freeNode,
                weeks = setOf(week),
            ),
            title = "添加课程",
            totalWeeks = snapshot.totalWeeks,
            periods = snapshot.periods,
            isEdit = false,
            currentWeek = week,
            conflictsProvider = { value -> viewModel.courseConflicts(value) },
            onDismiss = onCreateHandled,
            onSubmit = { value, applyWeeks ->
                viewModel.saveCourse(value, week, "all", applyWeeks)
                onCreateHandled()
            },
        )
    }
    eventRequest?.let { kind ->
        EventFormPage(
            initialKind = kind,
            initialDate = today,
            today = today,
            currentWeek = snapshot.currentWeek,
            totalWeeks = snapshot.totalWeeks,
            weekStartsMonday = settings.weekStartsMonday,
            periods = snapshot.periods,
            onDismiss = onEventHandled,
            onSubmit = { input ->
                viewModel.addEvent(
                    kind = input.kind,
                    title = input.title,
                    date = input.date,
                    start = input.start,
                    end = input.end,
                    location = input.location,
                    note = input.note,
                    courseCode = input.courseCode,
                )
                onEventHandled()
            },
        )
    }
    pendingMove?.let { (block, day, node) ->
        MoveScopeDialog(
            block = block,
            day = day,
            startNode = node,
            onDismiss = { pendingMove = null },
            onPick = { scope ->
                pendingMove = null
                viewModel.moveCourse(block, week, day, node, scope)
            },
        )
    }
    selectedExam?.let { item ->
        ExamDetailPage(
            item = item,
            viewModel = viewModel,
            onDismiss = { selectedExam = null },
        )
    }
    if (showWeekPicker) {
        WeekPickerDialog(
            selected = week,
            totalWeeks = snapshot.totalWeeks,
            currentWeek = snapshot.currentWeek,
            onDismiss = { showWeekPicker = false },
            onSelect = { week = it; showWeekPicker = false },
        )
    }
    unplacedDay?.let { day ->
        UnplacedDayDialog(
            day = day,
            items = weekUnplaced.filter { event ->
                runCatching {
                    java.time.LocalDate.parse(event.date).dayOfWeek.value
                }.getOrNull() == day
            },
            onDismiss = { unplacedDay = null },
        )
    }
    daySwapDate?.let { date ->
        TimetableDaySwapPage(
            initialFrom = date,
            onDismiss = {
                daySwapDate = null
                // 调课后这一天挂的课换到了另一天，把当前这批格子重新取一次。
                viewModel.reloadWeek()
            },
        )
    }
}

/** 把某一周的桥数据转成渲染用的课程块（与 ViewModel 里 snapshot 的映射一致）。 */
private fun cellsOfWeek(week: TimetableWeek): List<CourseBlock> =
    // 「只改本周」把课挪走以后，原位置在本周已经没有课了，不该再画一格灰影
    // （参考实现按 slotOrigin 的「原时段本周是否还有课」过滤）。
    week.cells.filterNot { !it.activeThisWeek && it.movedAway }.map { cell ->
    CourseBlock(
        courseId = cell.courseId,
        name = cell.name,
        day = cell.day,
        startNode = cell.startNode,
        endNode = cell.endNode,
        location = cell.location,
        teacher = cell.teacher,
        colorIndex = cell.colorIndex,
        weeks = cell.weeks,
        weeksLabel = cell.weeksLabel,
        isExperimental = cell.isLab,
        kind = cell.kind,
        activeThisWeek = cell.activeThisWeek,
        userColor = cell.userColor,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
    }
}

/** 课程列表视图：按天分组的行式列表，与设置页同构；考试也混排在同一天的组里。 */
@Composable
private fun CourseListView(
    week: Int,
    blocks: List<CourseBlock>,
    exams: List<ExamGridItem>,
    dayOrder: List<Int>,
    settings: TimetableSettings,
    header: (@Composable () -> Unit)? = null,
    swipeModifier: Modifier,
    onBlockClick: (CourseBlock) -> Unit,
    onExamClick: (ExamGridItem) -> Unit,
) {
    val sorted = blocks.sortedWith(compareBy({ dayOrder.indexOf(it.day) }, { it.startNode }))
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().then(swipeModifier),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 12.dp, bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header != null) item { header() }
        dayOrder.forEach { day ->
            val dayBlocks = sorted.filter { it.day == day }
            val dayExams = exams.filter { it.day == day }.sortedBy { it.startNode }
            if (dayBlocks.isEmpty() && dayExams.isEmpty()) return@forEach
            item {
                SegmentedColumn(title = dayLabel(day, settings.dayLabelStyle)) {
                    dayBlocks.forEach { b ->
                        item { CourseListRow(b, settings, onBlockClick) }
                    }
                    dayExams.forEach { item2 ->
                        item { ExamListRow(item2, onExamClick) }
                    }
                }
            }
        }
    }
}

/** 列表里的一行课程：课名 / 节次·地点·教师 / 角标。 */
@Composable
private fun CourseListRow(
    block: CourseBlock,
    settings: TimetableSettings,
    onBlockClick: (CourseBlock) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    BaseItemContainer(onClick = { onBlockClick(block) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    block.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append("第 ${block.startNode}-${block.endNode} 节")
                        if (block.location.isNotEmpty()) append(" · ${block.location}")
                        if (block.teacher.isNotEmpty()) append(" · ${block.teacher}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                val pills = buildLabelPills(block, settings)
                if (pills.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 列表底色是 surfaceBright，课程格里的前景色在它上面几乎看不见，角标统一用主题色。
                        pills.forEach { BlockPill(it, scheme.primary, 1f) }
                    }
                }
            }
            SymbolIcon(
                "chevron_right",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
                fontSize = 20.sp,
            )
        }
    }
}

/** 列表里的一行考试：与课程行同构，角标用「考试」（更早周加「非本周」）。 */
@Composable
private fun ExamListRow(item: ExamGridItem, onExamClick: (ExamGridItem) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val time = "%02d:%02d – %02d:%02d".format(
        item.exam.start.hour,
        item.exam.start.minute,
        item.exam.end.hour,
        item.exam.end.minute,
    )
    BaseItemContainer(onClick = { onExamClick(item) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.exam.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(time)
                        if (item.exam.location.isNotEmpty()) append(" · ${item.exam.location}")
                        append(" · 第 ${item.startNode}-${item.endNode} 节")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BlockPill("考试", scheme.primary, 1f)
                    if (!item.activeThisWeek) BlockPill("非本周", scheme.onSurfaceVariant, 1f)
                }
            }
            SymbolIcon(
                "chevron_right",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
                fontSize = 20.sp,
            )
        }
    }
}

/** 左右翻周容器：本周与前后两周并排，拖动时三页一起平移，松手吸附到相邻一周。 */
@Composable
private fun WeekSwipeBox(
    week: Int,
    totalWeeks: Int,
    onWeekChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (pageWeek: Int, isCurrent: Boolean, offset: () -> Float) -> Unit,
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var swipeWidth by remember { mutableFloatStateOf(1f) }
    var swipeAnim by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    // 相邻周等进场转场动画跑完再铺：转场途中再组合两张网格会让动画掉帧
    // （实测每张要 40–60 ms）。开始拖动时立即补上，保证滑动不空白。
    var neighboursReady by remember { mutableStateOf(false) }
    LaunchedEffect(week, totalWeeks) {
        delay(420)
        withFrameNanos { }
        neighboursReady = true
    }
    val scope = rememberCoroutineScope()
    // 首周/末周不能再往外翻：手势到边界只给一点点跟手位移（橡皮筋），松手弹回。
    val canPrev = week > 1
    val canNext = week < totalWeeks
    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { swipeWidth = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(week, totalWeeks) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        swipeAnim?.cancel()
                        neighboursReady = true
                    },
                    onHorizontalDrag = { change, drag ->
                        change.consume()
                        val raw = swipeOffset + drag
                        val blocked = (raw > 0f && !canPrev) || (raw < 0f && !canNext)
                        swipeOffset = if (blocked) {
                            val slack = swipeWidth * 0.12f
                            (raw * 0.18f).coerceIn(-slack, slack)
                        } else {
                            raw.coerceIn(-swipeWidth, swipeWidth)
                        }
                    },
                    onDragEnd = {
                        // 拖过 1/5 屏才翻页，不足则弹回；到了边界方向一律作废。
                        val wanted = when {
                            swipeOffset <= -swipeWidth * 0.2f -> 1
                            swipeOffset >= swipeWidth * 0.2f -> -1
                            else -> 0
                        }
                        val direction = when {
                            wanted > 0 && !canNext -> 0
                            wanted < 0 && !canPrev -> 0
                            else -> wanted
                        }
                        swipeAnim?.cancel()
                        swipeAnim = scope.launch {
                            Animatable(swipeOffset).animateTo(
                                targetValue = -direction * swipeWidth,
                                animationSpec = tween(durationMillis = 200),
                            ) { swipeOffset = value }
                            if (direction != 0) onWeekChange((week + direction).coerceIn(1, totalWeeks))
                            swipeOffset = 0f
                        }
                    },
                    onDragCancel = {
                        swipeAnim?.cancel()
                        swipeAnim = scope.launch {
                            Animatable(swipeOffset).animateTo(0f, tween(160)) { swipeOffset = value }
                            swipeOffset = 0f
                        }
                    },
                )
            },
    ) {
        for (copy in -1..1) {
            val pageWeek = week + copy
            if (pageWeek !in 1..totalWeeks) continue
            if (copy != 0 && !neighboursReady) continue
            // 偏移用 lambda 传给 graphicsLayer：拖动时只改图层，不触发整页重组
            // （否则每一帧都会重算颜色、分组与文字布局，视觉上就是「闪」）。
            page(pageWeek, copy == 0) { swipeOffset + copy * swipeWidth }
        }
    }
}

/** 左侧节次栏：固定在网格之外，翻周时不动，只跟随纵向滚动。 */
@Composable
private fun PeriodColumn(
    periodW: Dp,
    cellH: Dp,
    periods: List<Pair<String, String>>,
    rows: Int,
    showTimes: Boolean,
    showGridLines: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.width(periodW).height(cellH * rows)) {
        repeat(rows) { i ->
            val start = periods.getOrNull(i)?.first ?: ""
            val end = periods.getOrNull(i)?.second ?: ""
            val tinted = i % 2 != 0 && showGridLines
            Column(
                modifier = Modifier
                    .height(cellH)
                    .fillMaxWidth()
                    .background(if (tinted) scheme.surfaceContainerLow.copy(alpha = 0.4f) else Color.Transparent),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (i < periods.size) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                    )
                }
                if (showTimes && i < periods.size) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        start,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        end,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(
    height: Dp,
    days: List<Int>,
    highlightToday: Boolean,
    currentDay: Int,
    headerTextSize: Int = 11,
    /** 星期几 → 该日日期（`yyyy-MM-dd`）；拿不到日期的列不长按。 */
    dateOfDay: Map<Int, String> = emptyMap(),
    onDayLongPress: ((java.time.LocalDate) -> Unit)? = null,
    /** 这一天有「不落在任何节次内」的日程（含考试）：日期下画小圆点，点了看清单。 */
    unplacedDays: Set<Int> = emptySet(),
    onUnplacedClick: ((Int) -> Unit)? = null,
    /** 这一天被整天调课换过：日期下画「调」提示，点一下看/撤销调课。 */
    swappedDays: Set<Int> = emptySet(),
    onSwappedClick: ((Int) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(height)) {
        days.forEach { day ->
            val d = dayNames[day - 1]
            // 长按日期表头进入整天调课，原日期预填为该日（与参考实现一致）。
            val date = dateOfDay[day]?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .then(
                        if (date != null && (onDayLongPress != null || (onSwappedClick != null && day in swappedDays))) {
                            Modifier.pointerInput(day, date, day in swappedDays) {
                                detectTapGestures(
                                    onLongPress = { onDayLongPress?.invoke(date) },
                                    onTap = {
                                        if (day in swappedDays) onSwappedClick?.invoke(day)
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val isToday = day == currentDay && highlightToday
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isToday) scheme.primary else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        d,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = headerTextSize.sp,
                        lineHeight = (headerTextSize + 2).sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) scheme.onPrimary else scheme.onSurfaceVariant,
                    )
                }
                if (day in swappedDays) {
                    Text(
                        "调",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        color = scheme.tertiary,
                    )
                }
                if (day in unplacedDays) {
                    Box(
                        Modifier
                            .padding(top = 1.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isToday) scheme.primary else scheme.onSurfaceVariant)
                            .then(
                                if (onUnplacedClick != null) {
                                    Modifier.clickable { onUnplacedClick(day) }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekBody(
    cellH: Dp,
    nodeCount: Int,
    days: List<Int>,
    blocks: List<CourseBlock>,
    exams: List<ExamGridItem>,
    settings: TimetableSettings,
    currentWeek: Int,
    focusExamKey: String? = null,
    highlightAlpha: Float = 0f,
    focusCourseKey: String? = null,
    courseHighlightAlpha: Float = 0f,
    onExamClick: (ExamGridItem) -> Unit,
    onBlockClick: (CourseBlock) -> Unit,
    onMove: (CourseBlock, Int, Int) -> Unit = { _, _, _ -> },
    onExamMove: (ExamGridItem, Int, Int) -> Unit = { _, _, _ -> },
    onStackClick: (List<CourseBlock>, List<ExamGridItem>) -> Unit = { _, _ -> },
) {
    val scheme = MaterialTheme.colorScheme
    val totalH = cellH * nodeCount
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(totalH)) {
        if (settings.showGridLines) {
            // 网格线画到边界上：左右两侧都要有，相邻周共用同一条接缝线；上下同理。
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val color = scheme.outlineVariant.copy(alpha = 0.5f)
                        val stroke = 1.dp.toPx()
                        val dayW = size.width / days.size
                        for (i in 0..days.size) {
                            val x = (i * dayW).coerceIn(0f, size.width)
                            drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
                        }
                        val cellPx = cellH.toPx()
                        for (i in 0..nodeCount) {
                            val y = (i * cellPx).coerceIn(0f, size.height)
                            drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
                        }
                    },
            )
        }
        BoxWithBlocks(
            cellH = cellH,
            nodeCount = nodeCount,
            days = days,
            blocks = blocks,
            exams = exams,
            settings = settings,
            currentWeek = currentWeek,
            onExamClick = onExamClick,
            onBlockClick = onBlockClick,
            onMove = onMove,
            onExamMove = onExamMove,
            onStackClick = onStackClick,
        )
        // 从首页点某节课跳过来时给那一格描一圈（与考试同一套描边）。
        val courseTarget = focusCourseKey?.split("|")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }
        if (courseTarget != null && courseHighlightAlpha > 0f &&
            blocks.any { it.day == courseTarget[0] && it.startNode == courseTarget[1] }
        ) {
            val column = days.indexOf(courseTarget[0])
            if (column >= 0) {
                val colW = maxWidth / days.size
                val start = courseTarget[1]
                val end = courseTarget[2].coerceAtLeast(start)
                Box(
                    Modifier
                        .offset(x = colW * column + 1.dp, y = cellH * (start - 1) + 1.dp)
                        .width(colW - 2.dp)
                        .height(cellH * (end - start + 1) - 2.dp)
                        .border(
                            width = 2.dp,
                            color = scheme.primary.copy(alpha = courseHighlightAlpha),
                            shape = RoundedCornerShape(8.dp),
                        ),
                )
            }
        }
        // 从考试安排页跳过来时给那场考试描一圈（2dp accent），1.8 秒后淡出。
        val target = focusExamKey?.let { key -> exams.firstOrNull { examKey(it.exam) == key } }
        if (target != null && highlightAlpha > 0f) {
            val column = days.indexOf(target.day)
            if (column >= 0) {
                val colW = maxWidth / days.size
                Box(
                    Modifier
                        .offset(x = colW * column + 1.dp, y = cellH * (target.startNode - 1) + 1.dp)
                        .width(colW - 2.dp)
                        .height(cellH * (target.endNode - target.startNode + 1) - 2.dp)
                        .border(
                            width = 2.dp,
                            color = scheme.primary.copy(alpha = highlightAlpha),
                            shape = RoundedCornerShape(8.dp),
                        ),
                )
            }
        }
    }
}

/** 一列里合并到同一格的课：起止节次按「传递闭包」相连的都算一组，取名字最小的那门当主课。 */
private data class BlockGroup(
    val primary: CourseBlock,
    val startNode: Int,
    val endNode: Int,
    val activeCount: Int,
    val active: Boolean,
    /** 组内全部格子（本周课的组按课名排序），点开时列出这一格的所有课。 */
    val members: List<CourseBlock> = listOf(primary),
)

private fun groupBlocks(blocks: List<CourseBlock>, currentWeek: Int): List<BlockGroup> {
    // 分组只在「同一列（同一天）」内做：跨列合并会把周二的课塞进周一的格子。
    fun merge(items: List<CourseBlock>, active: Boolean): List<BlockGroup> {
        val groups = mutableListOf<MutableList<CourseBlock>>()
        items.sortedBy { it.startNode }.forEach { block ->
            val last = groups.lastOrNull()
            if (last != null && block.startNode <= last.maxOf { it.endNode }) {
                last.add(block)
            } else {
                groups.add(mutableListOf(block))
            }
        }
        return groups.map { group ->
            val sorted = group.sortedWith(
                compareBy({ normalizeCourseName(it.name) }, { it.courseId }),
            )
            BlockGroup(
                primary = sorted.first(),
                startNode = group.minOf { it.startNode },
                endNode = group.maxOf { it.endNode },
                activeCount = if (active) group.size else 0,
                active = active,
                members = sorted,
            )
        }
    }

    val active = blocks.filter { it.activeThisWeek && (it.weeks.isEmpty() || currentWeek in it.weeks) }
    val activeByDay = active.groupBy { it.day }
    val activeGroups = activeByDay.values.flatMap { merge(it, true) }
    // 非本周的课让位：逐格判断，与本周课重叠的整格不画（参考实现里灰格不计冲突，也不挤进同一格）。
    val ghosts = blocks.filterNot { it in active.toSet() }.filterNot { ghost ->
        activeGroups.any { group ->
            group.primary.day == ghost.day &&
                group.startNode <= ghost.endNode &&
                group.endNode >= ghost.startNode
        }
    }
    val ghostGroups = ghosts.groupBy { it.day }.values.flatMap { merge(it, false) }
    return activeGroups + ghostGroups
}

/** 参考实现的课名归一：去空白 + 半角括号换全角，只用于排序/配色键。 */
private fun normalizeCourseName(name: String): String =
    name.trim().replace('(', '（').replace(')', '）')

/** 一格要画的内容：本周课 + 并进这一格的考试（叠放成「N 门」）。 */
private data class BlockDraw(
    val group: BlockGroup,
    val extras: List<ExamGridItem>,
    val startNode: Int,
    val endNode: Int,
    val stack: Int,
)

/** 没被课程组吸收的考试：同一天里节次重叠的聚成一格。 */
private data class ExamDraw(
    val items: List<ExamGridItem>,
    val startNode: Int,
    val endNode: Int,
)

/** 考试的稳定身份：优先事件 id，没有就退回考试 key。 */
private fun examIdentity(item: ExamGridItem): String =
    item.exam.eventId.ifEmpty { examKey(item.exam) }

/**
 * 一页要画什么（与参考实现的绘制顺序一致）：
 * 1. 本周课按节次重叠成组，压在这一组上的考试并进同一格（范围外扩、stack 计数 +1）；
 * 2. 非本周灰格让位给重叠的考试，整格不画；
 * 3. 没被吸收的考试再按天聚成叠放格，一天里叠着几场就只画一格「N 门」。
 */
private fun buildDrawPlan(
    blocks: List<CourseBlock>,
    currentWeek: Int,
    exams: List<ExamGridItem>,
): Pair<List<BlockDraw>, List<ExamDraw>> {
    val groups = groupBlocks(blocks, currentWeek)
    val absorbed = HashSet<String>()
    val blockDraws = ArrayList<BlockDraw>(groups.size)
    groups.forEach { group ->
        val overlapping = exams.filter { exam ->
            exam.day == group.primary.day &&
                exam.startNode <= group.endNode &&
                exam.endNode >= group.startNode
        }
        if (!group.active) {
            if (overlapping.isEmpty()) {
                blockDraws += BlockDraw(group, emptyList(), group.startNode, group.endNode, 0)
            }
            return@forEach
        }
        overlapping.forEach { absorbed += examIdentity(it) }
        blockDraws += BlockDraw(
            group = group,
            extras = overlapping,
            startNode = minOf(group.startNode, overlapping.minOfOrNull { it.startNode } ?: group.startNode),
            endNode = maxOf(group.endNode, overlapping.maxOfOrNull { it.endNode } ?: group.endNode),
            stack = group.activeCount + overlapping.size,
        )
    }
    val examDraws =
        exams.filterNot { examIdentity(it) in absorbed }
            .groupBy { it.day to it.activeThisWeek }
            .values
            .flatMap { sameDay ->
                val clusters = mutableListOf<MutableList<ExamGridItem>>()
                sameDay.sortedWith(compareBy({ it.startNode }, { it.exam.start }, { it.exam.title }))
                    .forEach { item ->
                        val last = clusters.lastOrNull()
                        if (last != null && item.startNode <= last.maxOf { it.endNode }) {
                            last.add(item)
                        } else {
                            clusters.add(mutableListOf(item))
                        }
                    }
                clusters.map { cluster ->
                    ExamDraw(
                        items = cluster,
                        startNode = cluster.minOf { it.startNode },
                        endNode = cluster.maxOf { it.endNode },
                    )
                }
            }
    return blockDraws to examDraws
}

@Composable
private fun BoxWithBlocks(
    cellH: Dp,
    nodeCount: Int,
    days: List<Int>,
    blocks: List<CourseBlock>,
    exams: List<ExamGridItem>,
    settings: TimetableSettings,
    currentWeek: Int,
    onExamClick: (ExamGridItem) -> Unit,
    onBlockClick: (CourseBlock) -> Unit,
    onMove: (CourseBlock, Int, Int) -> Unit = { _, _, _ -> },
    onExamMove: (ExamGridItem, Int, Int) -> Unit = { _, _, _ -> },
    onStackClick: (List<CourseBlock>, List<ExamGridItem>) -> Unit = { _, _ -> },
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val colW = maxWidth / days.size
        val cellDensity = androidx.compose.ui.platform.LocalDensity.current
        val cellMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
        val cellMetrics = androidx.compose.runtime.remember(cellMeasurer) { CellTextLayout.Metrics(cellMeasurer) }
        val colors = courseColors()
        val scheme = MaterialTheme.colorScheme
        // 非本周格用固定的中性色（白底 + 1px #DADDE3 + #6B7280 字，深色各有一档），
        // 不取任何课程配色，所以 A/B/C 三套色板都不会和它撞色。
        val darkTheme = scheme.surface.luminance() < 0.5f
        val ghostFg = if (darkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
        val ghostBg = if (darkTheme) Color(0xFF22242A) else Color(0xFFFFFFFF)
        val ghostBorder = if (darkTheme) Color(0xFF3A3D45) else Color(0xFFDADDE3)
        // 绘制计划按「这一页的格子」缓存：拖动翻页时不重新分组，只移动图层。
        val (blockDraws, examDraws) = remember(blocks, currentWeek, exams) {
            buildDrawPlan(blocks, currentWeek, exams)
        }
        blockDraws.forEach { draw ->
            val g = draw.group
            val b = g.primary
            val column = days.indexOf(b.day)
            if (column < 0) return@forEach
            // 非本周灰格：白底 + 1px 描边 + 弱化文字，不取课程色、不受格子不透明度影响
            // （参考实现 §13.8 的画法，和本周课一眼能分开）。
            val ghost = !g.active
            val (fg, bg) = if (ghost) ghostFg to ghostBg else blockColors(b, colors)
            val startNode = draw.startNode
            val endNode = draw.endNode
            val span = endNode - startNode + 1
            val top = cellH * (startNode - 1)
            val height = cellH * span
            val blockAlpha = 1f
            // 长按拖动调课：拖动时块跟手，松手才换算成目标「星期 + 起始节」。
            var dragOffset by remember(b.courseId, startNode) {
                mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
            }
            val columnWidthPx = colW
            Box(
                modifier = Modifier
                    .offset(x = colW * column + 1.dp, y = top + 1.dp)
                    .width(colW - 2.dp)
                    .height(height - 2.dp)
                    .graphicsLayer {
                        translationX = dragOffset.x
                        translationY = dragOffset.y
                        alpha = if (dragOffset == androidx.compose.ui.geometry.Offset.Zero) 1f else 0.85f
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (ghost) bg else bg.copy(alpha = settings.cellOpacity / 100f))
                    .then(
                        if (ghost) {
                            Modifier.border(1.dp, ghostBorder, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (ghost) {
                            Modifier
                        } else {
                            Modifier.pointerInput(b.courseId, startNode, endNode, days.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { _ -> dragOffset = androidx.compose.ui.geometry.Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                            },
                            onDragEnd = {
                                val colPx = columnWidthPx.toPx()
                                val cellPx = cellH.toPx()
                                val centerX = colPx * column + dragOffset.x + colPx / 2f
                                val newColumn = (centerX / colPx).toInt().coerceIn(0, days.size - 1)
                                val dragSpan = endNode - startNode + 1
                                val targetNode = ((cellH.toPx() * (startNode - 1) + dragOffset.y) / cellPx)
                                    .roundToInt() + 1
                                val newStart = targetNode.coerceIn(1, (nodeCount - dragSpan + 1).coerceAtLeast(1))
                                val newDay = days[newColumn]
                                dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                if (newDay != b.day || newStart != startNode) {
                                    onMove(b, newDay, newStart)
                                }
                            },
                            onDragCancel = { dragOffset = androidx.compose.ui.geometry.Offset.Zero },
                            )
                            }
                        },
                    )
                    .clickable {
                        // 同一格叠了多门课（或压着考试/日程）时先把它们列出来，
                        // 让用户选要看的那个，而不是只打开课名排序第一的那门。
                        if (draw.stack > 1) {
                            onStackClick(g.members, draw.extras)
                        } else {
                            onBlockClick(b)
                        }
                    }
                    .padding(4.dp),
            ) {
                // 灰格只标「非本周」，不再同时显示单/双周（也不会叠「N 门」：灰格不参与叠放）。
                val pills = when {
                    ghost -> listOf("非本周")
                    draw.stack > 1 -> listOf("${draw.stack} 门")
                    span < 2 -> emptyList()
                    else -> buildLabelPills(b, settings)
                }
                val nameBase = androidx.compose.ui.text.TextStyle(
                    fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = fg.copy(alpha = blockAlpha),
                )
                val subBase = androidx.compose.ui.text.TextStyle(
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    color = fg.copy(alpha = 0.8f * blockAlpha),
                )
                val locText = if (settings.showLocation && b.location.isNotEmpty()) {
                    CellTextLayout.breakableLocation(b.location)
                } else {
                    ""
                }
                val teacher = if (settings.showTeacher) CellTextLayout.teacherText(b.teacher) else ""
                val innerWidth = (colW - 2.dp - 8.dp).coerceAtLeast(1.dp)
                val badgeReserve = if (pills.isNotEmpty()) 16.dp else 0.dp
                val innerHeight = (height - 2.dp - 8.dp - badgeReserve).coerceAtLeast(1.dp)
                val subLineHeight = (CellTextLayout.SUB_SP * CellTextLayout.LINE_HEIGHT_FACTOR * settings.fontScale).dp
                val plan = CellTextLayout.plan(
                    metrics = cellMetrics,
                    name = b.name,
                    location = locText,
                    teacher = teacher,
                    nameBase = nameBase,
                    subBase = subBase,
                    maxWidthPx = with(cellDensity) { innerWidth.roundToPx() },
                    maxHeightPx = with(cellDensity) { innerHeight.toPx() },
                    fontScale = settings.fontScale,
                )
                Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().clipToBounds()) {
                    // 课名：先换行、再缩字号，放不下才截行并以省略号收尾（与参考实现一致）。
                    Text(
                        b.name,
                        style = CellTextLayout.styleOf(nameBase, plan.nameSizeSp),
                        maxLines = plan.nameLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (locText.isNotEmpty()) {
                        // 地点永不截断：不限行数、无省略号，窄列里换行显示完整。
                        Text(
                            locText,
                            style = CellTextLayout.styleOf(subBase, plan.subSizeSp),
                            softWrap = true,
                        )
                    }
                    if (teacher.isNotEmpty()) {
                        Spacer(Modifier.height(subLineHeight))
                        Text(
                            teacher,
                            style = CellTextLayout.styleOf(subBase, plan.subSizeSp),
                            softWrap = true,
                        )
                    }
                    // 底部胶囊：贴底、水平居中；两个标签竖着排，1 节高的格子放不下就省略。
                    Spacer(Modifier.weight(1f))
                    if (pills.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            pills.forEach { BlockPill(it, fg, blockAlpha) }
                        }
                    }
                }
                }
            }
        }

        // 考试块：与课程块同级同构——同样的圆角、内边距、字号缩放规则与胶囊位，
        // 只是底色换成设置里的考试色（更早周的幽灵考试用非本周灰底）。
        val examColor = Color(settings.examColorArgb)
        examDraws.forEach { draw ->
            // 一天里叠着几场考试时只画一格，主项取节次最早的那场。
            val item = draw.items.first()
            val column = days.indexOf(item.day)
            if (column < 0) return@forEach
            val stacked = draw.items.size > 1
            // 叠放格只要有一场落在本周就按本周色画，否则按非本周灰格。
            val active = draw.items.any { it.activeThisWeek }
            val startNode = draw.startNode
            val endNode = draw.endNode
            val span = endNode - startNode + 1
            val top = cellH * (startNode - 1)
            val height = cellH * span
            // 考试的非本周幽灵格和课程灰格同一套中性色（白底 + 1px 描边 + 灰字 + 「非本周」）。
            val cellBg = if (active) examColor else ghostBg
            val cellFg = if (active) onColorFor(examColor) else ghostFg
            val blockAlpha = 1f
            // 考试块与课程块一样可长按拖动：松手把「星期 + 起始节」交给 Dart 换算成新的日期与时间。
            var examOffset by remember(examIdentity(item), startNode) {
                mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
            }
            val examColumnWidthPx = colW
            Box(
                modifier = Modifier
                    .offset(x = colW * column + 1.dp, y = top + 1.dp)
                    .width(colW - 2.dp)
                    .height(height - 2.dp)
                    .graphicsLayer {
                        translationX = examOffset.x
                        translationY = examOffset.y
                        alpha = if (examOffset == androidx.compose.ui.geometry.Offset.Zero) 1f else 0.85f
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(cellBg.copy(alpha = blockAlpha))
                    .then(
                        if (active) {
                            Modifier
                        } else {
                            Modifier.border(1.dp, ghostBorder, RoundedCornerShape(8.dp))
                        },
                    )
                    .then(
                        if (active) {
                            Modifier.pointerInput(examIdentity(item), startNode, endNode, days.size) {
                                detectDragGesturesAfterLongPress(
                            onDragStart = { _ -> examOffset = androidx.compose.ui.geometry.Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                examOffset += amount
                            },
                            onDragEnd = {
                                val colPx = examColumnWidthPx.toPx()
                                val cellPx = cellH.toPx()
                                val centerX = colPx * column + examOffset.x + colPx / 2f
                                val newColumn = (centerX / colPx).toInt().coerceIn(0, days.size - 1)
                                val targetNode = ((cellH.toPx() * (startNode - 1) + examOffset.y) / cellPx)
                                    .roundToInt() + 1
                                val newStart = targetNode.coerceIn(1, (nodeCount - span + 1).coerceAtLeast(1))
                                val newDay = days[newColumn]
                                examOffset = androidx.compose.ui.geometry.Offset.Zero
                                if (newDay != item.day || newStart != startNode) {
                                    onExamMove(item, newDay, newStart)
                                }
                            },
                                onDragCancel = { examOffset = androidx.compose.ui.geometry.Offset.Zero },
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                    .clickable { if (stacked) onStackClick(emptyList(), draw.items) else onExamClick(item) }
                    .padding(4.dp),
            ) {
                val pills = when {
                    stacked -> listOf("${draw.items.size} 门")
                    // 非本周的考试是双胶囊：先标「考试」，再标「非本周」。
                    !active -> listOf("考试", "非本周")
                    span >= 2 -> listOf("考试")
                    else -> emptyList()
                }
                val nameBase = androidx.compose.ui.text.TextStyle(
                    fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = cellFg,
                )
                val subBase = androidx.compose.ui.text.TextStyle(
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    color = cellFg.copy(alpha = 0.8f),
                )
                val timeText = "%02d:%02d – %02d:%02d".format(
                    item.exam.start.hour,
                    item.exam.start.minute,
                    item.exam.end.hour,
                    item.exam.end.minute,
                )
                val locText = if (item.exam.location.isNotEmpty()) {
                    CellTextLayout.breakableLocation(item.exam.location)
                } else {
                    ""
                }
                val innerWidth = (colW - 2.dp - 8.dp).coerceAtLeast(1.dp)
                val badgeReserve = if (pills.isNotEmpty()) 16.dp else 0.dp
                val innerHeight = (height - 2.dp - 8.dp - badgeReserve).coerceAtLeast(1.dp)
                val plan = CellTextLayout.plan(
                    metrics = cellMetrics,
                    name = item.exam.title,
                    location = locText,
                    teacher = "",
                    nameBase = nameBase,
                    subBase = subBase,
                    maxWidthPx = with(cellDensity) { innerWidth.roundToPx() },
                    maxHeightPx = with(cellDensity) { innerHeight.toPx() },
                    fontScale = settings.fontScale,
                )
                Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().clipToBounds()) {
                    Text(
                        item.exam.title,
                        style = CellTextLayout.styleOf(nameBase, plan.nameSizeSp),
                        maxLines = plan.nameLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        timeText,
                        style = CellTextLayout.styleOf(subBase, plan.subSizeSp),
                        softWrap = true,
                    )
                    if (locText.isNotEmpty()) {
                        Text(
                            locText,
                            style = CellTextLayout.styleOf(subBase, plan.subSizeSp),
                            softWrap = true,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (pills.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            pills.forEach { BlockPill(it, cellFg, 1f) }
                        }
                    }
                }
                }
            }
        }
    }
}

/**
 * 同一时段有多门课（或课与考试重叠）时的小卡：列出这一格里的全部内容，
 * 点哪一项就打开哪一项的详情。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StackPickSheet(
    blocks: List<CourseBlock>,
    events: List<ExamGridItem>,
    onDismiss: () -> Unit,
    onBlock: (CourseBlock) -> Unit,
    onEvent: (ExamGridItem) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                if (events.isEmpty()) "本时段 ${blocks.size} 门课" else "本时段 ${blocks.size + events.size} 项",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            blocks.forEach { block ->
                StackPickRow(
                    title = block.name,
                    subtitle = buildString {
                        append("第 ${block.startNode}-${block.endNode} 节")
                        if (block.location.isNotEmpty()) append(" · ${block.location}")
                    },
                    onClick = { onBlock(block) },
                )
            }
            events.forEach { item ->
                StackPickRow(
                    title = item.exam.title,
                    subtitle = buildString {
                        append("考试 · ${item.exam.start} – ${item.exam.end}")
                        if (item.exam.location.isNotEmpty()) append(" · ${item.exam.location}")
                    },
                    onClick = { onEvent(item) },
                )
            }
        }
    }
}

@Composable
private fun StackPickRow(title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        SymbolIcon("chevron_right", tint = scheme.onSurfaceVariant)
    }
}

/** 底部胶囊文字：实验 / 单周期双周仅当周次齐全且全单或全双时标出，考试按课块标记。 */
private fun buildLabelPills(block: CourseBlock, settings: TimetableSettings): List<String> = buildList {
    // 一格高的格子放不下角标：实验标记直接省略（参考实现同样处理）。
    val tallEnough = block.endNode - block.startNode + 1 >= 2
    if (settings.markExperimental && block.isExperimental && tallEnough) add("实验")
    // 单/双周按教务原始周次判断：只改本周后时段位串只剩一周，不能据此判断单双周。
    val parityWeeks = block.origWeeks.ifEmpty { block.weeks }
    if (settings.markParity && parityWeeks.size >= 2) {
        if (parityWeeks.all { it % 2 == 1 }) {
            add("单周")
        } else if (parityWeeks.all { it % 2 == 0 }) {
            add("双周")
        }
    }
    if (settings.markExam && block.isExam) add("考试")
}

/** 格内胶囊：描边与文字同色（60% 透明），高度由字号撑开。 */
@Composable
private fun BlockPill(text: String, color: Color, alpha: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        color = color.copy(alpha = 0.9f * alpha),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .border(0.5.dp, color.copy(alpha = 0.6f * alpha), CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

/** 「其他」栏：教务处下发但还没有固定节次的课程（原版课表底部的其他一栏）。 */
@Composable
private fun UnscheduledSection(items: List<CourseBlock>) {
    val scheme = MaterialTheme.colorScheme
    SegmentedColumn(title = "其他") {
        items.forEach { block ->
            item {
                BaseWidget(
                    icon = "schedule",
                    title = block.name,
                    // 「其他」栏没有固定节次，只给教师与周次。
                    description = listOf(block.teacher, block.weeksLabel)
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                    // 「其他」栏的课没有固定节次，改时间也保存不了，所以这里只给看、不给操作。
                )
            }
        }
    }
}

/** 周次卡片：点开选择周次，左侧一个下拉箭头提示可展开。 */
@Composable
private fun WeekCard(
    week: Int,
    totalWeeks: Int,
    termLabel: String,
    currentWeek: Int = 0,
    /** 非空表示学期已走完，它的值是「本周」的周次。 */
    endedCurrentWeek: Int? = null,
    weekPending: Boolean = false,
    dateLabel: String = "",
    syncFailed: Boolean = false,
    onOpenPicker: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val notCurrent = currentWeek > 0 && week != currentWeek && endedCurrentWeek == null
    val subtitle = when {
        endedCurrentWeek != null && !weekPending -> "本周第 $endedCurrentWeek 周 · 课程已结束"
        weekPending -> "周次待同步 · 点此选择"
        else -> listOf(dateLabel, termLabel).filter { it.isNotEmpty() }.joinToString(" · ")
    }
    SegmentedColumn {
        item {
            BaseWidget(
                icon = "calendar_month",
                title = if (notCurrent) "第 $week 周（非本周）" else "第 $week 周",
                description = if (weekPending) subtitle else "$subtitle · 共 $totalWeeks 周",
                onClick = onOpenPicker,
                trailingContent = {
                    SymbolIcon(
                        "chevron_right",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        // 上次同步失败：一行说明放在周次卡下方，和设置页的提示行同一种写法。
        if (syncFailed) {
            item {
                BaseWidget(
                    icon = "info",
                    iconColor = MaterialTheme.colorScheme.error,
                    title = "上次同步没有成功",
                    titleStyle = MaterialTheme.typography.bodySmall,
                    description = "课程可能不是最新的，下拉课表可以再试一次",
                    descriptionStyle = MaterialTheme.typography.bodySmall,
                    descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 周次选择：只在周次之间选，不牵涉具体日期。 */
@Composable
private fun WeekPickerDialog(
    selected: Int,
    totalWeeks: Int,
    currentWeek: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("选择周次") },
        text = {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                modifier = Modifier.heightIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(totalWeeks) { index ->
                    val w = index + 1
                    val active = w == selected
                    Box(
                        Modifier
                            .aspectRatio(1.6f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) scheme.primary else scheme.surfaceContainerHighest)
                            .clickable { onSelect(w) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$w",
                                style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
                                color = if (active) scheme.onPrimary else scheme.onSurface,
                            )
                            if (w == currentWeek) {
                                Text(
                                    "本周",
                                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 10.sp),
                                    color = if (active) scheme.onPrimary.copy(alpha = 0.85f) else scheme.onSurfaceVariant,
                                    modifier = Modifier.offset(y = (-2).dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

/**
 * 表头圆点清单：这一天「不落在任何节次内」的日程（考试、活动等）。
 * 与参考实现一致——这类安排不画进网格，只在日期下提示。
 */
@Composable
private fun UnplacedDayDialog(
    day: Int,
    items: List<TimetableEvent>,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("周${dayNames.getOrElse(day - 1) { "" }} · 未排进课表的时间") },
        text = {
            Column {
                items.sortedBy { it.start }.forEach { item ->
                    Text(
                        listOf(item.start, item.end).filter { it.isNotEmpty() }.joinToString(" – "),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                    if (item.location.isNotEmpty()) {
                        Text(
                            item.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
    )
}
