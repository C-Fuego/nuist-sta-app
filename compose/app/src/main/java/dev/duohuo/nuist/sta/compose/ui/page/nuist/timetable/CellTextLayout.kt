// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 格内文字排版，按参考实现的规则移植：
 *
 * 1. 先按格宽把课名字号缩到「每行至少三个汉字」能放下（不放大，最小 10sp）；
 * 2. 再按格高逐级缩小字号（每级 1sp，下限 10sp）：
 *    放不下时先把课名截到能放下的行数（至少 1 行、末行省略），仍放不下才缩字号；
 * 3. 缩到下限仍放不下：地点和教师完整排版、课名 1 行省略号截断，超出部分由格子裁掉。
 *
 * 地点与教师永远不截断（不限行数、无省略号）；只有课名会用省略号。
 */
internal object CellTextLayout {

    /** 格内基准课名字号（sp），与参考实现的课名基准一致。 */
    const val NAME_SP = 12f

    /** 格内基准副文字（地点 / 教师）字号（sp）。 */
    const val SUB_SP = 11f

    /** 格内最小实际字号（sp）。 */
    const val MIN_SP = 10f

    /** 行高倍数，与参考实现一致（1.1 时中文上下笔画不被切）。 */
    const val LINE_HEIGHT_FACTOR = 1.1f

    /** 宽格锚点：一行至少要放得下这么多汉字。 */
    private const val WIDTH_ANCHOR = "课课课"

    private const val EPS = 0.01f

    data class Plan(
        val nameSizeSp: Float,
        val subSizeSp: Float,
        /** 课名最多行数（至少 1，末行省略）。 */
        val nameLines: Int,
        /** 是否在最小字号内放下了；false 时课名只留 1 行，其余靠裁。 */
        val fits: Boolean,
    )

    /** 测量助手：Compose 侧用 [TextMeasurer] 量文本，坐标单位是像素。 */
    class Metrics(private val measurer: TextMeasurer) {

        fun widthOf(text: String, style: TextStyle): Float = measurer.measure(
            text = text,
            style = style,
            constraints = Constraints(),
            skipCache = true,
        ).size.width.toFloat()

        fun heightOf(text: String, style: TextStyle, maxWidthPx: Int): Float = measurer.measure(
            text = text,
            style = style,
            constraints = Constraints(maxWidth = maxWidthPx),
            skipCache = true,
        ).size.height.toFloat()

        fun linesOf(text: String, style: TextStyle, maxWidthPx: Int): Int = measurer.measure(
            text = text,
            style = style,
            constraints = Constraints(maxWidth = maxWidthPx),
            skipCache = true,
        ).lineCount.coerceAtLeast(1)
    }

    fun styleOf(base: TextStyle, sizeSp: Float): TextStyle = base.copy(
        fontSize = sizeSp.sp,
        lineHeight = (sizeSp * LINE_HEIGHT_FACTOR).sp,
    )

    /**
     * 算出这一格该用什么字号、课名几行。
     *
     * @param maxWidthPx 内容区宽度（像素，已扣掉格子内边距）
     * @param maxHeightPx 内容区可用高度（像素，已扣掉底部角标预留）
     */
    /** 排版结果按（文字 + 样式 + 可用尺寸）缓存：重组、滑动回看时不必再量一遍。 */
    private val planCache = object : LinkedHashMap<String, Plan>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Plan>?): Boolean =
            size > 512
    }

    fun plan(
        metrics: Metrics,
        name: String,
        location: String,
        teacher: String,
        nameBase: TextStyle,
        subBase: TextStyle,
        maxWidthPx: Int,
        maxHeightPx: Float,
        fontScale: Float,
    ): Plan {
        val key = buildString(64) {
            append(name).append('\u0001').append(location).append('\u0001').append(teacher)
            append('\u0001').append(maxWidthPx).append('\u0001').append(maxHeightPx.toInt())
            append('\u0001').append(fontScale)
            append('\u0001').append(nameBase.fontWeight?.weight ?: 0)
            append('\u0001').append(subBase.fontWeight?.weight ?: 0)
        }
        planCache[key]?.let { return it }
        return computePlan(metrics, name, location, teacher, nameBase, subBase, maxWidthPx, maxHeightPx, fontScale)
            .also { planCache[key] = it }
    }

    private fun computePlan(
        metrics: Metrics,
        name: String,
        location: String,
        teacher: String,
        nameBase: TextStyle,
        subBase: TextStyle,
        maxWidthPx: Int,
        maxHeightPx: Float,
        fontScale: Float,
    ): Plan {
        val width = maxWidthPx.coerceAtLeast(1)
        val initialName = snap(NAME_SP * fontScale).coerceAtLeast(MIN_SP)
        val initialSub = snap(SUB_SP * fontScale).coerceAtLeast(MIN_SP)

        // ① 按格宽缩到「一行三个汉字」放得下，给高度拟合一个不荒唐的起点。
        var widthFitSize = initialName
        while (widthFitSize > MIN_SP &&
            metrics.widthOf(WIDTH_ANCHOR, styleOf(nameBase, widthFitSize)) > width + EPS
        ) {
            widthFitSize -= 1f
        }
        widthFitSize = widthFitSize.coerceAtLeast(MIN_SP)
        val startSub = snap(initialSub * widthFitSize / initialName).coerceAtLeast(MIN_SP)

        val laidName = layoutCourseName(metrics, name, nameBase, widthFitSize, width)
        val hasTeacher = teacher.isNotEmpty()

        // ② 以「宽格锚点」得到的字号为起点，再按高度逐级缩。
        var step = 0
        while (true) {
            val ns = snap(max(MIN_SP, widthFitSize - step)).coerceAtLeast(MIN_SP)
            val ss = snap(max(MIN_SP, startSub - step)).coerceAtLeast(MIN_SP)
            val nameStyle = styleOf(nameBase, ns)
            val subStyle = styleOf(subBase, ss)

            val nameLineH = metrics.heightOf("课", nameStyle, width)
            val subLineH = metrics.heightOf("课", subStyle, width)
            val nameLines = metrics.linesOf(laidName, nameStyle, width)
            val locH = if (location.isEmpty()) 0f else metrics.heightOf(location, subStyle, width)
            val teacherH = if (hasTeacher) metrics.heightOf(teacher, subStyle, width) else 0f
            val teacherGap = if (hasTeacher) subLineH else 0f
            val secondaryH = locH + teacherH + teacherGap
            val fullH = nameLines * nameLineH

            if (fullH + secondaryH <= maxHeightPx + EPS) {
                return Plan(ns, ss, nameLines, fits = true)
            }
            // 课名截到能放下的行数（至少 1 行），地点与教师保持完整。
            if (nameLineH + secondaryH <= maxHeightPx + EPS) {
                val lines = floor((maxHeightPx - secondaryH + EPS) / nameLineH)
                    .toInt()
                    .coerceIn(1, nameLines)
                return Plan(ns, ss, lines, fits = true)
            }
            if (ns <= MIN_SP + EPS && ss <= MIN_SP + EPS) {
                // 到下限还是放不下：地点与教师完整排、课名 1 行，其余交给 ClipRect。
                return Plan(ns, ss, 1, fits = false)
            }
            step++
        }
    }

    /** 逻辑 sp 取整，避免 11.4 这类半像素字号发虚。 */
    private fun snap(sp: Float): Float = sp.roundToInt().toFloat()

    /**
     * 课名换行预处理：尾部括注（「（上）」「（C语言）」）整体断到下一行，
     * 不让折行把括注拆成两半。与参考实现一致，不在这里加省略号。
     */
    private fun layoutCourseName(
        metrics: Metrics,
        name: String,
        style: TextStyle,
        sizeSp: Float,
        maxWidthPx: Int,
    ): String {
        val text = name.trim()
        if (text.length <= 3) return text
        val openIndex = text.lastIndexOfAny(charArrayOf('（', '('))
        if (openIndex <= 2 || openIndex >= text.length - 1) return text
        val head = text.substring(0, openIndex)
        val tail = text.substring(openIndex)
        if (metrics.widthOf(tail, styleOf(style, sizeSp)) > maxWidthPx) return text
        return "$head\n$tail"
    }

    /**
     * 地点文字：与参考实现一致，前面加一个「@」并在相邻两个非汉字（字母、数字）
     * 之间插入 U+200B，让「A101」这类串也能逐字换行；只插不可见字符，可见文字只有 @。
     */
    fun breakableLocation(location: String): String {
        val out = StringBuilder()
        var prev: Char? = null
        for (ch in "@$location") {
            if (prev != null && isBreakable(prev) && isBreakable(ch)) out.append('\u200B')
            out.append(ch)
            prev = ch
        }
        return out.toString()
    }

    /** 教师名单：按分隔符一行一位教师，避免两位教师的名字被连在一起拆行。 */
    fun teacherText(teacher: String): String = teacher
        .trim()
        .split(Regex("[,，、;；]\\s*"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

    private fun isBreakable(ch: Char): Boolean = ch.isLetterOrDigit() && ch.code < 0x2E80

    /** 底部角标行预留高度（像素）。 */
    fun badgeReservePx(subSizeSp: Float, density: Float, lineHeightFactor: Float = LINE_HEIGHT_FACTOR): Float =
        ceil(16f * density)
}