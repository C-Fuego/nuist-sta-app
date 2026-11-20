// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.credits

/** 双创学分明细页（`XueFen/Summary/Index`）表格里的一行，全是单元格原始文本。 */
data class CreditItem(
    val name: String,
    val date: String,
    val category1: String,
    val category2: String,
    val level: String,
    val standardScore: String,
    val score: String,
    val batch: String,
    val status: String,
) {
    /** 「竞赛实验班 › 竞赛实验班期末成绩 › 60分及以上」，空段自动省略。 */
    val categoryPath: String
        get() = listOf(category1, category2, level).filter { it.isNotEmpty() }.joinToString(" › ")

    /** 实际分值与参照分值不一致（被打了折）。 */
    val isDiscounted: Boolean
        get() = standardScore.isNotEmpty() && score.isNotEmpty() && standardScore != score

    /** 是否已被学校审核通过。 */
    val approved: Boolean get() = status.contains("通过")
    val rejected: Boolean
        get() = status.contains("不通过") || status.contains("驳回") || status.contains("退回")

    companion object {
        fun fromRow(row: Map<String, String>): CreditItem {
            fun s(key: String) = (row[key] ?: "").trim()
            return CreditItem(
                name = s("项目名称"),
                date = s("取得成果日期"),
                category1 = s("认定大类"),
                category2 = s("认定小类"),
                level = s("获奖等级或排名"),
                standardScore = s("参照分值"),
                score = s("实际分值"),
                batch = s("所属批次"),
                status = s("状态"),
            )
        }
    }
}

/** 双创学分：汇总页总分/成绩 + 明细页全部条目。 */
data class InnovationCreditData(
    val total: String,
    val grade: String,
    val items: List<CreditItem>,
    val fetchedAtMillis: Long,
) {
    /** 成绩评定是否合格；页面没给时返回 null。 */
    val gradeApproved: Boolean?
        get() = when {
            grade.isEmpty() -> null
            grade.contains("不及格") || grade.contains("不合格") -> false
            grade.contains("及格") || grade.contains("合格") || grade.contains("通过") -> true
            else -> null
        }
}

/** 劳动教育平台「学生成绩」页里本人的一行（官方核算结果）。 */
data class LaborResultData(
    val theory: String,
    val life: String,
    val service: String,
    val majorCourse: String,
    val majorContest: String,
    val majorTotal: String,
    val total: String,
    val confirmed: String,
    val updatedAt: String,
    val filed: String,
    val filedAt: String,
) {
    val isConfirmed: Boolean get() = confirmed == "是"
    val isFiled: Boolean get() = filed == "是"

    /** 核算日期只留到「月-日」，解析不了就原样返回。 */
    val updatedShort: String
        get() = Regex("^\\d{4}-(\\d{2}-\\d{2})").find(updatedAt)?.groupValues?.get(1) ?: updatedAt

    companion object {
        fun fromRow(row: Map<String, String>): LaborResultData {
            fun s(key: String) = (row[key] ?: "").trim()
            return LaborResultData(
                theory = s("理论积分"),
                life = s("生活劳动"),
                service = s("服务劳动"),
                majorCourse = s("专业劳动课程积分"),
                majorContest = s("竞赛积分"),
                majorTotal = s("专业劳动"),
                total = s("总积分"),
                confirmed = s("是否确认"),
                updatedAt = s("更新日期"),
                filed = s("是否归档"),
                filedAt = s("归档时间"),
            )
        }
    }
}

/** 专业劳动积分汇总页本人的一行（实时统计）。 */
data class LaborMajorLive(
    val course: String,
    val contest: String,
    val total: String,
) {
    companion object {
        fun fromRow(row: Map<String, String>): LaborMajorLive {
            fun s(key: String) = (row[key] ?: "").trim()
            return LaborMajorLive(
                course = s("专业劳动课程积分"),
                contest = s("竞赛项目积分"),
                total = s("专业劳动累计积分"),
            )
        }
    }
}

/**
 * 劳动积分：官方核算结果 + 三个分项页面的实时统计。
 *
 * 核算结果是按批次生成的快照（带更新日期），实时统计是各分项页面当下的数字，
 * 两者可能不一致——卡片只看核算结果，详情页把两者并列。
 */
data class LaborScoreData(
    val official: LaborResultData?,
    val liveLife: String?,
    val liveService: String?,
    val liveMajor: LaborMajorLive?,
    val fetchedAtMillis: Long,
)
