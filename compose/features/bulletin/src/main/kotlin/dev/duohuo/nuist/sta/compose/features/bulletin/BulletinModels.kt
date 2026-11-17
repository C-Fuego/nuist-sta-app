package dev.duohuo.nuist.sta.compose.features.bulletin

/** 一条公告（列表项）。对应 Flutter `Bulletin`。 */
data class Bulletin(
    val id: String,
    val title: String,
    val officialCategory: String,
    val publisher: String,
    val publishedAt: String,
    val topics: List<String>,
    val actions: List<String> = emptyList(),
)

/** 公告正文的一个块。对应 Flutter `BulletinDetailBlock`（kind: 标题/段落/列表）。 */
sealed interface DetailBlock {
    data class Heading(val text: String, val level: Int) : DetailBlock
    data class Paragraph(val text: String) : DetailBlock
    data class Bullets(val items: List<String>) : DetailBlock
}

data class BulletinDetail(
    val bulletin: Bulletin,
    val meta: String,
    val blocks: List<DetailBlock>,
)

object BulletinMock {
    val topics = listOf("全部", "教务", "学工", "就业", "讲座", "活动")
    val categories = listOf("全部", "通知公告", "教学信息", "学生工作")

    val items = listOf(
        Bulletin("1", "关于 2026-2027 学年第一学期期中教学检查的通知", "教学信息", "教务处", "2026-11-14", listOf("教务"), listOf("置顶")),
        Bulletin("2", "关于开展 2026 年度校园歌手大赛报名的通知", "通知公告", "校团委", "2026-11-12", listOf("活动")),
        Bulletin("3", "2027 届毕业生就业双选会（第一场）邀请函", "学生工作", "就业指导中心", "2026-11-10", listOf("就业")),
        Bulletin("4", "学术讲座：人工智能在气象领域的应用", "通知公告", "大气科学学院", "2026-11-08", listOf("讲座")),
        Bulletin("5", "关于启动本学期国家奖学金评审工作的通知", "学生工作", "学生工作处", "2026-11-05", listOf("学工")),
    )

    fun detail(b: Bulletin) = BulletinDetail(
        bulletin = b,
        meta = "${b.publisher} · ${b.publishedAt} · ${b.officialCategory}",
        blocks = listOf(
            DetailBlock.Heading("一、工作安排", 2),
            DetailBlock.Paragraph("本次检查覆盖全校本科教学单位，重点检查课堂教学秩序、教学文件规范性与学生到课情况。各学院须在检查前完成自查并提交报告。"),
            DetailBlock.Heading("二、时间节点", 2),
            DetailBlock.Bullets(listOf(
                "11 月 17 日—11 月 21 日：学院自查阶段",
                "11 月 24 日—11 月 28 日：学校抽查阶段",
                "12 月 1 日：汇总反馈",
            )),
            DetailBlock.Heading("三、注意事项", 2),
            DetailBlock.Paragraph("请各单位高度重视，指定专人负责，确保检查工作顺利开展。如有疑问请联系教务处教学运行科。"),
        ),
    )
}
