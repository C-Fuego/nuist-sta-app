// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin

/**
 * 一条公告（列表项）。对应 Flutter `Bulletin`。
 *
 * [url] 是详情页绝对地址，也是这条公告的唯一标识；[pinned] 表示栏目置顶。
 */
data class Bulletin(
    val id: String,
    val title: String,
    val officialCategory: String,
    val publisher: String,
    val publishedAt: String,
    val topics: List<String>,
    val actions: List<String> = emptyList(),
    val url: String = id,
    val pinned: Boolean = actions.contains("置顶"),
)

/** 公告附件（正文里的下载链接或 PDF 阅读器链接）。 */
data class BulletinAttachment(
    val name: String,
    val url: String,
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
    val attachments: List<BulletinAttachment> = emptyList(),
    val sourceUrl: String = bulletin.url,
)

/** 公告列表的一页。 */
data class BulletinPage(
    val items: List<Bulletin>,
    val pageNumber: Int,
    val totalPages: Int,
    val totalCount: Int,
    val nextPath: String?,
)

/** 公告栏的栏目（服务端按栏目分页，`path` 为空表示全部）。 */
data class BulletinCategory(val label: String, val path: String)

/** 学校信息公告栏的栏目清单，与站点栏目页一一对应。 */
val BulletinCategories: List<BulletinCategory> = listOf(
    BulletinCategory("全部", ""),
    BulletinCategory("文件公告", "wjgg.htm"),
    BulletinCategory("学术报告", "xsbg.htm"),
    BulletinCategory("招标信息", "zbxx.htm"),
    BulletinCategory("会议通知", "hytz2.htm"),
    BulletinCategory("党政事务", "dzsw.htm"),
    BulletinCategory("组织人事", "zzrs.htm"),
    BulletinCategory("科研信息", "kyxx.htm"),
    BulletinCategory("招生就业", "zsjy.htm"),
    BulletinCategory("教学考试", "jxks.htm"),
    BulletinCategory("创新创业", "cxcy.htm"),
    BulletinCategory("学术研讨", "xsyt.htm"),
    BulletinCategory("专题讲座", "ztjz.htm"),
    BulletinCategory("校园活动", "xyhd.htm"),
    BulletinCategory("学院动态", "xydt.htm"),
    BulletinCategory("其他", "qt.htm"),
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