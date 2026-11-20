package dev.duohuo.nuist.sta.compose.ui.page.nuist.credits

/** 双创学分的一个认定项。 */
data class InnovationItem(
    val name: String,
    val date: String,
    val batch: String,
    val categoryPath: String,
    val score: String,
    val standardScore: String,
    val status: String,
) {
    val isDiscounted: Boolean get() = standardScore.isNotEmpty() && standardScore != score
}

/** 双创学分汇总。 */
data class InnovationCredit(
    val total: String,
    val grade: String,
    val items: List<InnovationItem>,
)

/** 劳动积分的官方核算分项。 */
data class LaborResult(
    val theory: String,
    val life: String,
    val service: String,
    val majorCourse: String,
    val majorContest: String,
    val majorTotal: String,
    val total: String,
    val confirmed: String,
)

object CreditsMock {
    val innovation = InnovationCredit(
        total = "18.5",
        grade = "优秀",
        items = listOf(
            InnovationItem("全国大学生数学建模竞赛 省级一等奖", "2026-09-12", "2026 第一批", "学科竞赛 · 省级", "6", "6", "已认定"),
            InnovationItem("软件著作权（校园服务 App）", "2026-07-03", "2026 第一批", "创新创业训练 · 校级", "3", "3", "已认定"),
            InnovationItem("校级创新创业训练计划 结题", "2026-05-20", "2026 第一批", "创新创业训练 · 校级", "4", "5", "已认定"),
            InnovationItem("大学英语六级", "2025-12-14", "2025 第二批", "技能证书 · 国家级", "2", "2", "已认定"),
            InnovationItem("社会实践优秀个人", "2025-08-30", "2025 第一批", "社会实践 · 校级", "3.5", "3.5", "已认定"),
        ),
    )

    val labor = LaborResult(
        theory = "20",
        life = "25",
        service = "30",
        majorCourse = "12",
        majorContest = "8",
        majorTotal = "20",
        total = "95",
        confirmed = "已确认",
    )
}
