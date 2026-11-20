package dev.duohuo.nuist.sta.compose.ui.page.nuist.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableCell
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettings
import dev.duohuo.nuist.sta.compose.data.timetable.TimetableSettingsRepository
import dev.duohuo.nuist.sta.compose.ui.kit.BaseWidget
import dev.duohuo.nuist.sta.compose.ui.kit.EntryCard
import dev.duohuo.nuist.sta.compose.ui.kit.SegmentedColumn
import dev.duohuo.nuist.sta.compose.ui.kit.SymbolIcon
import dev.duohuo.nuist.sta.compose.ui.navigation.LocalNavigator
import dev.duohuo.nuist.sta.compose.ui.navigation.Route
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.HomeBulletinViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.electricity.ElectricityViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.exam.ExamViewModel
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.CourseBlock
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableMock
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 首页三段式信息区（方案 A）。
 *
 * 视觉统一规则：
 * 1. 页面只允许「SegmentedColumn 分组 + BaseWidget 行」这一种信息形态，
 *    与学习页 / 我的 / 主题设置完全同构；
 * 2. 分组内每行等高（56/72dp 自适应），左侧图标统一 Material Symbols；
 * 3. 数字指标走 titleMedium 强调，正文说明走 bodyMedium / onSurfaceVariant；
 * 4. 并排入口统一用共享 [EntryCard]（与学习页三入口同款）。
 */

/**
 * 组 1：今日概览 —— 今天的课（真实课表）/ 宿舍电费（真实余额）/ 待考科目。
 *
 * 课表与电费都取自各自页面的 ViewModel，未登录或未绑定时按空态显示。
 */
@Composable
internal fun HomeTodaySection(
    onOpenTimetable: () -> Unit,
    onOpenElectricity: () -> Unit,
    onOpenExams: () -> Unit,
    timetableViewModel: TimetableViewModel = koinInject(),
    settingsRepository: TimetableSettingsRepository = koinInject(),
    electricityViewModel: ElectricityViewModel = koinViewModel(),
    examViewModel: ExamViewModel = koinViewModel(),
) {
    val timetable by timetableViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsRepository.state.collectAsStateWithLifecycle()
    val electricity by electricityViewModel.state.collectAsStateWithLifecycle()
    val examState by examViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        // 课表状态全应用共享：有新鲜数据就直接用，不再每次进首页都发起一次同步。
        timetableViewModel.load()
        electricityViewModel.refresh()
    }
    // 考试安排跟学期走：课表快照拿到学期码后才有得查。
    LaunchedEffect(timetable.snapshot?.termCode) {
        timetable.snapshot?.termCode?.let { examViewModel.refresh(it) }
    }

    val snapshot = timetable.snapshot
    val week = snapshot?.currentWeek ?: 0
    val times = snapshot?.periods ?: TimetableMock.periodTimes
    // 倒计时与「距下课」按分钟走：每分钟自己重算一次，不依赖别处碰巧重组。
    var clock by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            clock = LocalDateTime.now()
        }
    }
    val today = clock.toLocalDate()
    val now = clock.toLocalTime()
    /** 某一天的课：优先用桥里已缓存的周格子（自带真实时刻），退回快照 + 作息表。 */
    fun lessonsOf(date: LocalDate): List<HomeLesson> {
        val dayOfWeek = date.dayOfWeek.value
        // 只有「周日的明天」会跨到下一教学周，其余情况明天仍在同一周。
        val weekNo = if (dayOfWeek < today.dayOfWeek.value) week + 1 else week
        val cells = timetable.cachedWeeks[weekNo]?.takeIf { it.week == weekNo }?.cells
            ?: timetable.week?.takeIf { it.week == weekNo }?.cells
        if (cells != null) {
            return cells.filter { it.day == dayOfWeek && it.activeThisWeek }
                .sortedBy { it.startNode }
                .map { it.toLesson(times) }
        }
        return snapshot?.courses.orEmpty()
            .filter { it.day == dayOfWeek && (it.weeks.isEmpty() || weekNo in it.weeks) }
            .sortedBy { it.startNode }
            .map { it.toLesson(times) }
    }
    val todayLessons = lessonsOf(today)
    val tomorrowLessons = lessonsOf(today.plusDays(1))
    val inProgress = todayLessons.firstOrNull { lesson ->
        val start = lesson.start
        val end = lesson.end
        start != null && end != null && !start.isAfter(now) && end.isAfter(now)
    }
    val showLocation = settings.showLocation
    val showTeacher = settings.showTeacher
    /** 首页最多列出几节课：设置页与 Dart 侧都约束在 1–4。 */
    val homeMaxItems = settings.homeMaxItems.coerceIn(
        TimetableSettings.HOME_MAX_ITEMS_OPTIONS.first(),
        TimetableSettings.HOME_MAX_ITEMS_OPTIONS.last(),
    )
    /**
     * 某一节课属于哪一教学周：只有「周日看明天的课」会跨到下一周，其余仍在本周。
     * 首页点某节课跳课表，要靠它把课表页直接定到那一周。
     */
    fun weekNoOf(date: LocalDate): Int {
        val dayOfWeek = date.dayOfWeek.value
        return if (dayOfWeek < today.dayOfWeek.value) week + 1 else week
    }
    // 今天的课按「正在上的 + 还没开始的」排，上过的不再出现；都上完了才切到明天。
    val pendingToday = listOfNotNull(inProgress) +
        todayLessons.filter { it.start?.isAfter(now) == true }.filter { it != inProgress }
    val showingTomorrow = pendingToday.isEmpty() && settings.showTomorrowOnHome
    val upcomingDate = if (showingTomorrow) today.plusDays(1) else today
    val upcoming = (if (showingTomorrow) tomorrowLessons else pendingToday)
        .take(homeMaxItems)
        .map { it to upcomingDate }
    val navigator = LocalNavigator.current
    /** 点一节课：跳到课表页对应的周 / 那天 / 那一节，并像考试跳转一样描一圈。 */
    fun openAt(lesson: HomeLesson, date: LocalDate) {
        navigator.push(
            Route.TimetableFocus(
                week = weekNoOf(date),
                day = lesson.day,
                startNode = lesson.startNode,
                courseKey = "${lesson.day}|${lesson.startNode}|${lesson.endNode}",
            ),
        )
    }
    // 没有课可列时的说明：来源不同，说法也跟着变。
    val emptyCoursesTitle = when {
        snapshot == null -> when {
            timetable.isLoading -> "正在加载课表…"
            timetable.error != null -> "课表未加载"
            else -> "课表还没同步"
        }
        showingTomorrow -> "明天没有课程"
        todayLessons.isEmpty() -> "今天也没有课"
        else -> "今天的课已上完"
    }
    val emptyCoursesDesc = when {
        snapshot == null -> when {
            timetable.isLoading -> "稍后显示今天的课"
            timetable.error != null -> "到课表页重新同步一次"
            else -> "同步课表后这里显示今天的课"
        }
        showingTomorrow && todayLessons.isNotEmpty() -> "今天的课已上完"
        else -> null
    }

    val balance = electricity.latest
    val balanceTitle = when {
        balance != null -> String.format("%.1f 度", balance.kwh)
        electricity.loading -> "正在查询…"
        else -> "--"
    }
    val exam = examState.next
    val termCode = snapshot?.termCode
    val examTitle = when {
        exam != null -> exam.title
        examState.loading -> "正在获取考试安排…"
        examState.error != null -> "考试安排未加载"
        termCode == null -> "考试安排未加载"
        else -> "暂无考试安排"
    }
    val examDesc = when {
        exam != null -> listOf(
            exam.timeLabel,
            exam.location.ifEmpty { null },
            "共 ${examState.upcoming.size} 场待考",
        ).filterNotNull().joinToString(" · ")
        examState.error != null -> examState.error.orEmpty()
        termCode == null -> "课表未同步，暂时无法获取考试安排"
        else -> "教务处发布的考试都会出现在这里"
    }
    val balanceDesc = buildString {
        append(electricity.room?.displayName ?: "未绑定宿舍")
        if (balance != null) {
            append(" · 更新于 ")
            append(String.format("%d月%d日 %02d:%02d",
                balance.time.monthValue, balance.time.dayOfMonth, balance.time.hour, balance.time.minute))
        }
    }
    // 三个入口拆成三张卡：今天的课按「一节课一张卡」列出来（条数受首页设置约束），
    // 点某节课直接定到课表里那一格并描边；电费与待考各自单独一张卡。
    SegmentedColumn(title = if (showingTomorrow) "明日课程" else "今日课程") {
        if (upcoming.isEmpty()) {
            item {
                BaseWidget(
                    icon = "calendar_month",
                    title = emptyCoursesTitle,
                    description = emptyCoursesDesc,
                    onClick = onOpenTimetable,
                    trailingContent = { Chevron() },
                )
            }
        } else {
            upcoming.forEach { (lesson, date) ->
                item {
                    BaseWidget(
                        icon = "schedule",
                        title = lesson.name,
                        description = lesson.lessonLine(showLocation, showTeacher),
                        onClick = { openAt(lesson, date) },
                        trailingContent = { Chevron() },
                    )
                }
            }
        }
    }
    SegmentedColumn(title = "宿舍电费") {
        item {
            BaseWidget(
                icon = "bolt",
                title = balanceTitle,
                description = balanceDesc,
                onClick = onOpenElectricity,
                trailingContent = { Chevron() },
            )
        }
    }
    SegmentedColumn(title = "待考科目") {
        item {
            BaseWidget(
                icon = "fact_check",
                title = examTitle,
                description = examDesc,
                isError = examState.error != null,
                onClick = onOpenExams,
                trailingContent = { Chevron() },
            )
        }
    }
}

/** 组 2：信息公告 —— 最新三条（学校信息公告栏真实数据）+ 查看全部。 */
@Composable
internal fun HomeBulletinSection(
    onOpenAll: () -> Unit,
    viewModel: HomeBulletinViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    SegmentedColumn(title = "信息公告") {
        if (state.error != null && state.items.isEmpty()) {
            item {
                BaseWidget(
                    icon = "error",
                    title = "公告加载失败",
                    onClick = viewModel::refresh,
                    trailingContent = { Chevron() },
                )
            }
        } else if (state.isLoading && state.items.isEmpty()) {
            item {
                BaseWidget(
                    icon = "progress_activity",
                    title = "正在加载公告…",
                    description = "南京信息工程大学信息公告栏",
                )
            }
        } else {
            state.items.forEach { b ->
                item {
                    BaseWidget(
                        icon = "campaign",
                        title = b.title,
                        titleStyle = MaterialTheme.typography.titleMedium,
                        description = "${b.publisher.ifEmpty { "学校信息公告栏" }} · ${b.publishedAt}",
                        onClick = onOpenAll,
                        trailingContent = { Chevron() },
                    )
                }
            }
        }
        item {
            BaseWidget(
                icon = "inbox",
                title = "查看全部公告",
                description = if (state.totalCount > 0) {
                    "共 ${state.totalCount} 条 · 按栏目筛选"
                } else {
                    "学校信息公告栏"
                },
                onClick = onOpenAll,
                trailingContent = { Chevron() },
            )
        }
    }
}

/** 组 3：校园导览 —— 并排入口（与学习页入口同款）。 */
@Composable
internal fun HomeEntrySection(onOpen: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EntryCard("meeting_room", "空教室", Modifier.weight(1f)) { onOpen("free-classroom") }
        EntryCard("map", "校园地图", Modifier.weight(1f)) { onOpen("map") }
    }
}

@Composable
private fun Chevron() {
    SymbolIcon(
        name = "chevron_right",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
        fontSize = 20.sp,
    )
}

// ---- 今日课表卡片的时间与文案 ----

/**
 * 卡片要展示的一节课。
 *
 * 起止时刻来自格子自带的真实时间（含「只改这一次」），取不到时退回作息表；
 * 两者都没有就只报节次。
 */
private data class HomeLesson(
    /** 星期几（1 = 周一），跳课表时定位用。 */
    val day: Int,
    val name: String,
    val location: String,
    val teacher: String,
    val startNode: Int,
    val endNode: Int,
    val start: LocalTime?,
    val end: LocalTime?,
)

private fun TimetableCell.toLesson(times: List<Pair<String, String>>): HomeLesson = HomeLesson(
    day = day,
    name = name,
    location = location,
    teacher = teacher,
    startNode = startNode,
    endNode = endNode,
    start = clockOf(start) ?: clockOf(times.getOrNull(startNode - 1)?.first),
    end = clockOf(end) ?: clockOf(times.getOrNull(endNode - 1)?.second),
)

private fun CourseBlock.toLesson(times: List<Pair<String, String>>): HomeLesson = HomeLesson(
    day = day,
    name = name,
    location = location,
    teacher = teacher,
    startNode = startNode,
    endNode = endNode,
    start = clockOf(times.getOrNull(startNode - 1)?.first),
    end = clockOf(times.getOrNull(endNode - 1)?.second),
)

/** 第二行：课名 · 时间 · 地点（按设置取舍）· 老师（按设置取舍）。 */
private fun HomeLesson.lessonLine(showLocation: Boolean, showTeacher: Boolean): String {
    val startTime = start
    val endTime = end
    val time = when {
        startTime != null && endTime != null -> "${hhmm(startTime)} – ${hhmm(endTime)}"
        startTime != null -> hhmm(startTime)
        endTime != null -> "至 ${hhmm(endTime)}"
        else -> "第 $startNode-$endNode 节"
    }
    return listOfNotNull(
        name.ifEmpty { null },
        time,
        location.takeIf { showLocation && it.isNotEmpty() },
        teacher.takeIf { showTeacher && it.isNotEmpty() },
    ).joinToString(" · ")
}

/** 「12 分钟后」「1 小时 5 分后」「即将上课」。 */
private fun untilLabel(now: LocalTime, start: LocalTime): String {
    val minutes = ChronoUnit.MINUTES.between(now, start)
    return when {
        minutes <= 0L -> "即将上课"
        minutes < 60L -> "$minutes 分钟后"
        else -> {
            val hours = minutes / 60
            val rest = minutes % 60
            if (rest == 0L) "$hours 小时后" else "$hours 小时 $rest 分后"
        }
    }
}

/** 「距下课 25 分钟」「距下课 1 小时 5 分钟」，不足一分钟按一分钟算。 */
private fun remainingLabel(now: LocalTime, end: LocalTime): String {
    val minutes = ChronoUnit.MINUTES.between(now, end).coerceAtLeast(1L)
    return when {
        minutes < 60L -> "距下课 $minutes 分钟"
        else -> {
            val hours = minutes / 60
            val rest = minutes % 60
            if (rest == 0L) "距下课 $hours 小时" else "距下课 $hours 小时 $rest 分钟"
        }
    }
}

/** 解析「08:00」「08:00:00」这类时刻，解析不了就交给调用方换别的来源。 */
private fun clockOf(raw: String?): LocalTime? {
    val text = raw?.trim().orEmpty()
    if (text.length < 5) return null
    return runCatching { LocalTime.parse(text.take(5)) }.getOrNull()
}

private fun hhmm(t: LocalTime): String = String.format("%02d:%02d", t.hour, t.minute)
